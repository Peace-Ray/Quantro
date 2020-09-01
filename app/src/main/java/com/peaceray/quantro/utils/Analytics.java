package com.peaceray.quantro.utils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

// TODO import analytics agent
import com.peaceray.quantro.QuantroPreferences;
import com.peaceray.quantro.R;
import com.peaceray.quantro.keys.Key;
import com.peaceray.quantro.keys.KeyRing;
import com.peaceray.quantro.keys.PremiumContentKey;
import com.peaceray.quantro.keys.PromotionalKey;
import com.peaceray.quantro.keys.QuantroXLKey;
import com.peaceray.quantro.model.game.GameSettings;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.premium.PremiumLibrary;
import com.peaceray.quantro.premium.PremiumSKU;
import com.peaceray.quantro.view.game.DrawSettings;
import com.peaceray.quantro.view.game.GameViewMemoryCapabilities;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;

/**
 * A paper-thin wrapper class. Its only purpose is to provide an easy way to
 * potentially change analytics methods later. Only this class directly touches
 * analytics suites (as of this writing, no such suite is used and no analytics
 * are logged, but one could be easily added).
 * 
 * @author Jake
 * 
 */
public class Analytics {

	private static final String TAG = "Analytics";

	/**
	 * Starts a session. The Activity or Service should pass itself as an
	 * argument.
	 *
	 * Follow the appropriate conventions for the underlying analytics suite used.
	 * For example, for Flurry, this method should be called in every onStart
	 * (for full-screened Activities) and when Services launch.
	 * 
	 * For example, it's important that a multiplayer game counts as an ongoing
	 * session even when not fullscreened. Therefore, don't stop the session
	 * when the Activity stops (or rather, stop the Activity session, but not
	 * the Service session.
	 * 
	 * POSTCONDITION: This method will start an analytics session. It also
	 * performs whatever configuration is necessary to begin the session,
	 * including reading from QuantroPreferences. However, it does NOT read the
	 * 'analytics_active' setting, which determines whether to start sessions
	 * and log data.
	 * 
	 * Example: this method WILL read 'analytics_aggregated' and will set user
	 * ID if 'false.'
	 * 
	 * @param context
	 */
	public final static void startSession(Context context) {
		/* TODO start Analytics
			Start an analytics session based on the provided context.
			If not aggregated (QuantroPreferences.getAnalyticsAggregated(context)),
			set the deviceID as the user's identity.
		 */
	}

	/**
	 * Stops a session. The Activity or Service should pass itself as an
	 * argument.
	 *
	 * Follow the convention of the underlying analytics suite. For example, for
	 * Flurry, this method should be called in every onStop (for full-screened Activities)
	 * and when Services terminate.
	 * 
	 * For example, it's important that a mulitplayer game counts as an ongoing
	 * session even when not fullscreened. Therefore, don't stop the session
	 * when the Activity stops (or rather, stop the Activity session, but not
	 * the Service session).
	 * 
	 * @param context
	 */
	public final static void stopSession(Context context) {
		// TODO stop analytics session
	}

	// //////////////////////////////////////////////////////////////////////////
	//
	// LOGGING EVENTS
	//
	// The main purpose of analytics is to log user events; these are probably
	// much
	// more useful than session start-stop time.
	//
	// We provide direct (wrapped) access to event logging through 'log', which
	// allows arbitrary event logging. We also provide special log*() methods
	// which allow convenient logging of common events without having to
	// carefully
	// track parameter and event names; these methods do it for you.
	//
	// One warning: to prevent overloading of built-in events, log() will Log.e
	// an error if a built-in eventID is provided; it will also return false
	// and NOT log the event.
	//
	// //////////////////////////////////////////////////////////////////////////

	// CONVENIENCE STRINGS FOR CUSTOM EVENT PARAMATERS
	public static final String YES = "Yes";
	public static final String NO = "No";

	public static final String HIGH = "High";
	public static final String MID = "Mid";
	public static final String LOW = "Low";
	
	public static final String NONE = "None" ;
	
	public static final String PURCHASED = "Purchased" ;
	public static final String PROMO = "Promo" ;
	public static final String XL = "XL" ;

	public static final String REASON_FULL = "Full";
	public static final String REASON_NONCE_INVALID = "NonceInvalid";
	public static final String REASON_NONCE_IN_USE = "NonceInUse";
	public static final String REASON_PORT_RANDOMIZATION = "PortRandomization";
	public static final String REASON_UNSPECIFIED = "Unspecified";

	public static final String REASON_ISSUED = "Issued";
	public static final String REASON_INVALID = "Invalid";
	public static final String REASON_BLANK = "Blank";
	public static final String REASON_CANNOT_FORM_QUERY = "CannotFormQuery";
	public static final String REASON_FAILED = "Failed";
	public static final String REASON_MALFORMED = "Malformed";
	public static final String REASON_NONE = "None";
	public static final String REASON_REFUSED = "Refused";
	public static final String REASON_TIMEOUT = "Timeout";
	public static final String REASON_WAITING = "Waiting";

	public static final String RESPONSE_OK = "OK";
	public static final String RESPONSE_USER_CANCELED = "UserCanceled";
	public static final String RESPONSE_SERVICE_UNAVAILABLE = "ServiceUnavailable";
	public static final String RESPONSE_BILLING_UNAVAILABLE = "BillingUnavailable";
	public static final String RESPONSE_ITEM_UNAVAILABLE = "ItemUnavailable";
	public static final String RESPONSE_DEVELOPER_ERROR = "DeveloperError";
	public static final String RESPONSE_ERROR = "Error";

	public static final String STATE_PURCHASED = "Purchased";
	public static final String STATE_CANCELED = "Canceled";
	public static final String STATE_REFUNDED = "Refunded";

	public static final String RESULT_SUCCESS = "Success";
	public static final String RESULT_FAILED_NO_SERVER = "NoServer";
	public static final String RESULT_FAILED_INVALID = "Invalid";
	public static final String RESULT_FAILED_ISSUED = "Issued";
	public static final String RESULT_FAILED_NOT_AVAILABLE = "NotAvailable";
	public static final String RESULT_FAILED_REFUSED = "Refused";
	public static final String RESULT_FAILED_WAITING = "Waiting";
	public static final String RESULT_FAILED_STORAGE = "Storage";
	public static final String RESULT_FAILED_UNSPECIFIED = "Unspecified";

	public static final String OTHER = "Other";

	/**
	 * Logs an arbitrary event, optionally with parameters and and/or a 'timed'
	 * value.
	 * 
	 * At present, it is the responsibility of the caller to ensure usefulness
	 * and consistency in the eventID and parameters used.
	 * 
	 * PRECONDITION: The specified eventID is NOT a reserved event ID. If it is,
	 * false is returned, an error logged, and no event sent.
	 * 
	 * @param eventID
	 * @param params
	 * @param timed
	 * @return Whether the event was logged, to the best of our knowledge.
	 */
	public final static boolean log(String eventID, Map<String, String> params,
			Boolean timed) {


		if (isReservedEvent(eventID)) {
			Log.e(TAG, "Reserved eventID " + eventID + " specified to log()");
			return false;
		}

		return sendLog(eventID, params, timed);
	}

	private final static boolean sendLog(String eventID, Map<String, String> params) {
		return sendLog(eventID, params, null);
	}

	private final static boolean sendLog(String eventID, Map<String, String> params, Boolean timed) {
		if (eventID == null)
			throw new NullPointerException("Must provide a non-null eventID");

		// TODO send a log of this event to the analytics suite
		Log.e(TAG, "Would have logged " + eventID + " but Analytics is disabled");
		return true;
	}

	// RESERVED EVENT IDS
	private static final String EVENT_ID_DEVICE = "Device";

	private static final String EVENT_ID_SETTINGS_GAME = "SettingsGame";
	private static final String EVENT_ID_SETTINGS_APP = "SettingsApp";
	
	private static final String EVENT_ID_GAME_VIEW_MEMORY_CAPABILITIES = "GameViewMemoryCapabilities" ;
	
	private static final String EVENT_ID_PREMIUM_LIBRARY = "PremiumLibrary" ;

	private static final String EVENT_ID_GAME_NEW = "GameNew";
	private static final String EVENT_ID_GAME_LOAD = "GameLoad";
	private static final String EVENT_ID_GAME_DELETE = "GameDelete";
	private static final String EVENT_ID_GAME_CUSTOM_MODE = "GameCustomMode" ;

	private static final String EVENT_ID_SERVICED_LOBBY_ACTIVITY_START = "ServicedLobbyActivityStart";
	private static final String EVENT_ID_SERVICED_LOBBY_ACTIVITY_STOP = "ServicedLobbyActivityStop";

	private static final String EVENT_ID_SERVICED_GAME_ACTIVITY_START = "ServicedGameActivityStart";
	private static final String EVENT_ID_SERVICED_GAME_ACTIVITY_STOP = "ServicedGameActivityStop";

	private static final String EVENT_ID_IN_GAME_OUT_OF_MEMORY = "InGameOutOfMemory";
	private static final String EVENT_ID_IN_GAME_PAUSE = "InGamePauseOn";
	private static final String EVENT_ID_IN_GAME_UNPAUSE = "InGamePauseOff";
	private static final String EVENT_ID_IN_GAME_EXAMINE_RESULT = "InGameExamineResult";
	private static final String EVENT_ID_IN_GAME_EXAMINE_RESOLVED_TO_CONTINUE = "InGameExamineResolvedToContinue";
	private static final String EVENT_ID_IN_GAME_EXAMINE_RESOLVED_TO_QUIT = "InGameExamineResolvedToQuit";
	private static final String EVENT_ID_IN_GAME_EXAMINE_RESOLVED_TO_REPLAY = "InGameExamineResolvedToReplay";
	private static final String EVENT_ID_IN_GAME_EXAMINE_RESOLVED_TO_REWIND = "InGameExamineResolvedToRewind";
	private static final String EVENT_ID_IN_GAME_LOAD = "InGameLoad";
	private static final String EVENT_ID_IN_GAME_SAVE = "InGameSave";
	private static final String EVENT_ID_IN_GAME_LOAD_CHECKPOINT = "InGameLoadCheckpoint";
	private static final String EVENT_ID_IN_GAME_SAVE_CHECKPOINT = "InGameSaveCheckpoint";

	private static final String EVENT_ID_LOBBY_CREATED = "LobbyCreated";
	private static final String EVENT_ID_LOBBY_JOINED = "LobbyJoined";
	private static final String EVENT_ID_LOBBY_LEFT = "LobbyLeft";
	private static final String EVENT_ID_LOBBY_GUEST_JOINED = "LobbyGuestJoined";
	private static final String EVENT_ID_LOBBY_GUEST_LEFT = "LobbyGuestLeft";
	private static final String EVENT_ID_LOBBY_CLOSED = "LobbyClosed";

	private static final String EVENT_ID_IN_LOBBY_LOG_EVENT = "InLobbyLogEvent";
	private static final String EVENT_ID_IN_LOBBY_COUNTDOWN_START = "InLobbyCountdownStart";
	private static final String EVENT_ID_IN_LOBBY_COUNTDOWN_ABORTED = "InLobbyCountdownAborted";
	private static final String EVENT_ID_IN_LOBBY_COUNTDOWN_HALTED = "InLobbyCountdownHalted";
	private static final String EVENT_ID_IN_LOBBY_LAUNCH = "InLobbyLaunch";

	private static final String EVENT_ID_CHALLENGE_ACTION_RECEIVED = "ChallengeActionReceived";
	private static final String EVENT_ID_CHALLENGE_ACTION_INITIATED = "ChallengeActionInitiated";
	private static final String EVENT_ID_CHALLENGE_ACTION_MANUAL_ENTRY = "ChallengeManualEntry";

	private static final String EVENT_ID_MATCHMAKING_REQUEST_TICKET = "MatchmakingRequestTicket" ;
	private static final String EVENT_ID_MATCHMAKING_NO_TICKET_NO_RESPONSE = "MatchmakingNoTicketNoResponse" ;
	private static final String EVENT_ID_MATCHMAKING_REQUEST = "MatchmakingRequest";
	private static final String EVENT_ID_MATCHMAKING_PROMISE = "MatchmakingPromise";
	private static final String EVENT_ID_MATCHMAKING_MATCH = "MatchmakingMatch";
	private static final String EVENT_ID_MATCHMAKING_NO_RESPONSE = "MatchmakingNoResponse";
	private static final String EVENT_ID_MATCHMAKING_REJECTED = "MatchmakingRejected";
	private static final String EVENT_ID_MATCHMAKING_UDP_HOLE_PUNCH = "UDPHolePunch";
	private static final String EVENT_ID_MATCHMAKING_INFORMATION_EXCHANGE = "InformationExchange";
	
	private static final String EVENT_ID_KEY_IN_APP_PURCHASE_ACTIVATION = "XLInAppPurchaseActivation";
	private static final String EVENT_ID_KEY_IN_APP_PURCHASE_RESPONSE = "XLInAppPurchaseResponse";
	private static final String EVENT_ID_KEY_IN_APP_PURCHASE_STATE = "XLInAppPurchaseState";
	private static final String EVENT_ID_KEY_IN_APP_PURCHASE_BILLING_AVAILABLE = "XLInAppPurchaseBillingNotAvailable";

	private static final String EVENT_ID_KEY_MANUAL_ENTRY = "XLKeyManualEntry";
	private static final String EVENT_ID_KEY_REACTIVATION = "XLKeyReactivationSuccess";

	private static final String EVENT_ID_AD_CLICK = "AdClick";
	private static final String EVENT_ID_AD_CLICK_CUSTOM_XL = "AdClickCustomXL";
	private static final String EVENT_ID_AD_LOAD = "AdLoad";

	private static final ArrayList<String> EVENT_IDS;
	static {
		EVENT_IDS = new ArrayList<String>();
		EVENT_IDS.add(EVENT_ID_DEVICE);

		EVENT_IDS.add(EVENT_ID_SETTINGS_GAME);
		EVENT_IDS.add(EVENT_ID_SETTINGS_APP);
		EVENT_IDS.add(EVENT_ID_GAME_VIEW_MEMORY_CAPABILITIES) ;
		EVENT_IDS.add(EVENT_ID_PREMIUM_LIBRARY) ;

		EVENT_IDS.add(EVENT_ID_GAME_NEW);
		EVENT_IDS.add(EVENT_ID_GAME_LOAD);
		EVENT_IDS.add(EVENT_ID_GAME_DELETE);
		EVENT_IDS.add(EVENT_ID_GAME_CUSTOM_MODE) ;

		EVENT_IDS.add(EVENT_ID_SERVICED_LOBBY_ACTIVITY_START);
		EVENT_IDS.add(EVENT_ID_SERVICED_LOBBY_ACTIVITY_STOP);

		EVENT_IDS.add(EVENT_ID_SERVICED_GAME_ACTIVITY_START);
		EVENT_IDS.add(EVENT_ID_SERVICED_GAME_ACTIVITY_STOP);

		EVENT_IDS.add(EVENT_ID_IN_GAME_OUT_OF_MEMORY) ;
		EVENT_IDS.add(EVENT_ID_IN_GAME_PAUSE);
		EVENT_IDS.add(EVENT_ID_IN_GAME_UNPAUSE);
		EVENT_IDS.add(EVENT_ID_IN_GAME_EXAMINE_RESULT);
		EVENT_IDS.add(EVENT_ID_IN_GAME_EXAMINE_RESOLVED_TO_CONTINUE);
		EVENT_IDS.add(EVENT_ID_IN_GAME_EXAMINE_RESOLVED_TO_QUIT);
		EVENT_IDS.add(EVENT_ID_IN_GAME_EXAMINE_RESOLVED_TO_REPLAY);
		EVENT_IDS.add(EVENT_ID_IN_GAME_EXAMINE_RESOLVED_TO_REWIND);
		EVENT_IDS.add(EVENT_ID_IN_GAME_LOAD);
		EVENT_IDS.add(EVENT_ID_IN_GAME_SAVE);
		EVENT_IDS.add(EVENT_ID_IN_GAME_LOAD_CHECKPOINT);
		EVENT_IDS.add(EVENT_ID_IN_GAME_SAVE_CHECKPOINT);

		EVENT_IDS.add(EVENT_ID_LOBBY_CREATED);
		EVENT_IDS.add(EVENT_ID_LOBBY_JOINED);
		EVENT_IDS.add(EVENT_ID_LOBBY_LEFT);
		EVENT_IDS.add(EVENT_ID_LOBBY_GUEST_JOINED);
		EVENT_IDS.add(EVENT_ID_LOBBY_GUEST_LEFT);
		EVENT_IDS.add(EVENT_ID_LOBBY_CLOSED);

		EVENT_IDS.add(EVENT_ID_IN_LOBBY_LOG_EVENT);
		EVENT_IDS.add(EVENT_ID_IN_LOBBY_COUNTDOWN_START);
		EVENT_IDS.add(EVENT_ID_IN_LOBBY_COUNTDOWN_ABORTED);
		EVENT_IDS.add(EVENT_ID_IN_LOBBY_COUNTDOWN_HALTED);
		EVENT_IDS.add(EVENT_ID_IN_LOBBY_LAUNCH);

		EVENT_IDS.add(EVENT_ID_CHALLENGE_ACTION_RECEIVED);
		EVENT_IDS.add(EVENT_ID_CHALLENGE_ACTION_INITIATED);
		EVENT_IDS.add(EVENT_ID_CHALLENGE_ACTION_MANUAL_ENTRY);

		EVENT_IDS.add(EVENT_ID_MATCHMAKING_REQUEST_TICKET) ;
		EVENT_IDS.add(EVENT_ID_MATCHMAKING_NO_TICKET_NO_RESPONSE) ;
		EVENT_IDS.add(EVENT_ID_MATCHMAKING_REQUEST);
		EVENT_IDS.add(EVENT_ID_MATCHMAKING_PROMISE);
		EVENT_IDS.add(EVENT_ID_MATCHMAKING_MATCH);
		EVENT_IDS.add(EVENT_ID_MATCHMAKING_NO_RESPONSE);
		EVENT_IDS.add(EVENT_ID_MATCHMAKING_REJECTED);
		EVENT_IDS.add(EVENT_ID_MATCHMAKING_UDP_HOLE_PUNCH);
		EVENT_IDS.add(EVENT_ID_MATCHMAKING_INFORMATION_EXCHANGE);

		EVENT_IDS.add(EVENT_ID_KEY_IN_APP_PURCHASE_ACTIVATION);
		EVENT_IDS.add(EVENT_ID_KEY_IN_APP_PURCHASE_RESPONSE);
		EVENT_IDS.add(EVENT_ID_KEY_IN_APP_PURCHASE_STATE);
		EVENT_IDS.add(EVENT_ID_KEY_IN_APP_PURCHASE_BILLING_AVAILABLE);

		EVENT_IDS.add(EVENT_ID_KEY_MANUAL_ENTRY);
		EVENT_IDS.add(EVENT_ID_KEY_REACTIVATION);

		EVENT_IDS.add(EVENT_ID_AD_CLICK);
		EVENT_IDS.add(EVENT_ID_AD_CLICK_CUSTOM_XL);
		EVENT_IDS.add(EVENT_ID_AD_LOAD);
	}

	// //
	// Variables for logDevice(), logSettings*()
	private static final String VAR_SDK = "SDK";
	private static final String VAR_RELEASE = "Release";
	private static final String VAR_SCREEN_SIZE = "ScreenSize";

	private static final String VAR_BRAND = "Brand";
	private static final String VAR_MANUFACTURER = "Manufacturer";
	private static final String VAR_MODEL = "Model";

	private static final String VAR_NAVIGATION = "Navigation";
	private static final String VAR_ORIENTATION = "Orientation";
	private static final String VAR_TOUCHSCREEN = "Touchscreen";

	private static final String VAR_PREMIUM = "Premium";
	
	private static final String VAR_XL = "XL" ;

	private static final String VAR_DETAIL = "Detail";
	private static final String VAR_SKIN_QUANTRO = "SkinQuantro";
	private static final String VAR_SKIN_RETRO = "SkinRetro";

	private static final String VAR_CONTROLS_SOUND = "ControlsSound";

	private static final String VAR_TOOLTIPS = "Tooltips";

	private static final String VAR_ANALYTICS_AGGREGATED = "AnalyticsAggregated";

	private static final String VAR_APP_SIZE_GAME = "AppSizeGame";
	private static final String VAR_APP_SIZE_LOBBY = "AppSizeGame";
	private static final String VAR_APP_SIZE_MENU = "AppSizeMenu";
	
	private static final String VAR_SCREEN_WIDTH = "ScreenWidth" ;
	private static final String VAR_SCREEN_HEIGHT = "ScreenHeight" ;
	private static final String VAR_HEAP_SIZE = "HeapSize" ;
	private static final String VAR_LOAD_IMAGE_SIZE = "LoadImageSize" ;
	private static final String VAR_BACKGROUND_IMAGE_SIZE = "BackgroundImageSize" ;
	private static final String VAR_BACKGROUND_SHUFFLE = "BackgroundShuffle" ;
	private static final String VAR_BLIT = "Blit" ;
	private static final String VAR_RECYCLE_TO_VEIL = "RecycleToVeil" ;
	private static final String VAR_DRAW_ANIMATIONS = "DrawAnimations" ;
	private static final String VAR_OPTIONS_OVERLAY = "OptionsOverlay" ;
	
	private static final String VAR_LOAD_AND_BACKGROUND_SIZE_AND_BLIT = "LoadAndBackgroundSizeAndBlit" ;
	private static final String VAR_ASSET_NAME = "AssetName" ;
	private static final String VAR_ATTEMPT = "Attempt" ;

	// //
	// Variables for games.
	private static final String VAR_GAME_MODE = "GameMode";
	private static final String VAR_LEVEL = "Level";
	private static final String VAR_CLEARS = "Clears";
	private static final String VAR_GARBAGE = "Garbage";
	private static final String VAR_GARBAGE_PER_LEVEL = "GarbagePerLevel";
	private static final String VAR_QUICK_PLAY = "QuickPlay" ;
	private static final String VAR_CGMS_ID = "CgmsId" ;
	private static final String VAR_IS_EDIT = "IsEdit" ;
	private static final String VAR_HAS_SAVES= "HasSaves" ;
	

	private static final String VAR_TIME_SINCE_STARTED = "TimeSinceStarted";
	private static final String VAR_TIME_SINCE_LAST_PLAYED = "TimeSinceLastPlayed";
	private static final String VAR_TIME_SPENT = "TimeSpent";

	private static final String VAR_USER_ACTION = "UserAction";
	private static final String VAR_RESPONSE_TO_USER_ACTION = "ResponseToUserAction";

	private static final String VAR_GAME_OVER = "GameOver";

	private static final String VAR_SERVICE_STOPPED = "ServiceStopped";

	private static final String VAR_EPHEMERAL = "Ephemeral";
	private static final String VAR_ASYNCHRONOUS = "Asynchronous";

	// //
	// Variables for lobbies.
	private static final String VAR_TIME_OPEN = "TimeOpen";
	private static final String VAR_NUM_LAUNCHES = "Launches";

	private static final String VAR_IS_WIFI = "IsWifi";
	private static final String VAR_IS_GAME = "IsGame";

	private static final String VAR_EVENT_TYPE = "EventType";

	// //
	// Variables for communication
	private static final String VAR_SUCCESS = "Success";
	private static final String VAR_REASON = "Reason";

	// //
	// Variables for Challenges
	private static final String VAR_ACTION_SOURCE = "Source";
	private static final String VAR_ACTION = "Action";

	// //
	// Variables for keys / tickets
	private static final String VAR_RESPONSE = "Response";
	private static final String VAR_STATE = "State";
	private static final String VAR_RESULT = "Result";
	private static final String VAR_KEY = "Key" ;

	// //
	// Variables for custom ads
	private static final String VAR_TAG = "Tag";

	/**
	 * Sanity check your custom event name to make sure it doesn't match a
	 * reserved event. If this method returns 'true', then log() will return
	 * 'false' and fail to log.
	 * 
	 * @param event
	 * @return
	 */
	public final static boolean isReservedEvent(String event) {
		return EVENT_IDS.contains(event);
	}

	/**
	 * Logs information about the device on which Quantro is running. This
	 * method should (probably) be called only once per use of the application.
	 * 
	 * Recommendation: call within QuantroApplication.
	 * 
	 * Information logged:
	 * 
	 * Android SDK Version (1, 7, 13, etc.) Android Version Number (e.g. 2.3.3)
	 * Screen Size in pixels (WIDTHxHEIGHT)
	 * 
	 * Brand (Build.BRAND) Manufacturer (Build.MANUFACTURER) Model (Build.MODEL)
	 * 
	 * Navigation Orientation Touchscreen
	 * 
	 * XL (has Quantro XL?)
	 * 
	 * @return
	 */
	public final static boolean logDevice(Context context, boolean hasAnyPremium) {

		Hashtable<String, String> params = new Hashtable<String, String>();

		Point size = VersionSafe.getScreenSize(context);
		params.put(VAR_SDK, "" + Build.VERSION.SDK_INT);
		params.put(VAR_RELEASE, Build.VERSION.RELEASE);
		params.put(VAR_SCREEN_SIZE, "" + size.x + "x" + size.y);

		params.put(VAR_BRAND, Build.BRAND);
		params.put(VAR_MANUFACTURER, Build.MANUFACTURER);
		params.put(VAR_MODEL, Build.MODEL);

		Configuration config = context.getResources().getConfiguration();

		// VAR_NAVIGATION
		switch (config.navigation) {
		case Configuration.NAVIGATION_NONAV:
			params.put(VAR_NAVIGATION, "NoNav");
			break;
		case Configuration.NAVIGATION_DPAD:
			params.put(VAR_NAVIGATION, "DPad");
			break;
		case Configuration.NAVIGATION_TRACKBALL:
			params.put(VAR_NAVIGATION, "Trackball");
			break;
		case Configuration.NAVIGATION_WHEEL:
			params.put(VAR_NAVIGATION, "Wheel");
			break;
		case Configuration.NAVIGATION_UNDEFINED:
		default:
			params.put(VAR_NAVIGATION, "Undefined");
			break;
		}

		// VAR_ORIENTATION
		switch (config.orientation) {
		case Configuration.ORIENTATION_PORTRAIT:
			params.put(VAR_ORIENTATION, "Portrait");
			break;
		case Configuration.ORIENTATION_LANDSCAPE:
			params.put(VAR_ORIENTATION, "Landscape");
			break;
		case Configuration.ORIENTATION_SQUARE:
			params.put(VAR_ORIENTATION, "Square");
			break;
		case Configuration.ORIENTATION_UNDEFINED:
		default:
			params.put(VAR_ORIENTATION, "Undefined");
			break;
		}

		// VAR_PREMIUM
		params.put(VAR_PREMIUM, hasAnyPremium ? YES : NO);

		// finally, log.
		sendLog(EVENT_ID_DEVICE, params);

		return true;
	}

	public final static boolean logSettingsGame(Activity activity, boolean hasAnyPremium) {
		// Logs the user's game configuration for this device.

		// Again we are limited to 10 variables, so here they are:

		// Has XL Key (might be omitted)
		// Has MP Key (might be omitted)

		// HighDetail (YES / NO)
		// ColorblindHelp ( YES / NO )
		// ColorSchemeQuantro
		// ColorSchemeRetro

		// ControlsHaveSound (YES / NO)

		// Tooltips

		// and 2 parameters left for the future.

		Hashtable<String, String> params = new Hashtable<String, String>();

		params.put(VAR_PREMIUM, hasAnyPremium ? YES : NO);

		String detail = MID;
		switch (QuantroPreferences.getGraphicsGraphicalDetail(activity)) {
		case DrawSettings.DRAW_DETAIL_LOW:
			detail = LOW;
			break;
		case DrawSettings.DRAW_DETAIL_MID:
			detail = MID;
			break;
		case DrawSettings.DRAW_DETAIL_HIGH:
			detail = HIGH;
			break;
		}
		params.put(VAR_DETAIL, detail);
		
		params.put(VAR_SKIN_QUANTRO, QuantroPreferences.getSkinQuantro(activity).getName());
		params.put(VAR_SKIN_RETRO, QuantroPreferences.getSkinRetro(activity).getName());
		

		params.put(VAR_CONTROLS_SOUND,
				QuantroPreferences.getSoundControls(activity) ? YES : NO);

		String tooltips = OTHER;
		switch (QuantroPreferences.getPieceTips(activity)) {
		case QuantroPreferences.PIECE_TIPS_OFTEN:
			tooltips = "Often";
			break;
		case QuantroPreferences.PIECE_TIPS_OCCASIONALLY:
			tooltips = "Occasionally";
			break;
		case QuantroPreferences.PIECE_TIPS_NEVER:
			tooltips = "Never";
			break;
		}
		params.put(VAR_TOOLTIPS, tooltips);

		sendLog(EVENT_ID_SETTINGS_GAME, params);
		return true;
	}

	public final static boolean logSettingsApp(Activity activity, boolean hasAnyPremium ) {
		// Logs the user's app configuration for this device. We are especially
		// interested
		// in analytics settings (can't log No Analytics, obviously, but we CAN
		// log
		// whether we are collecting them aggregated (i.e., w/o an Android ID).

		// Again we are limited to 10 variables, so here they are:

		// Has XL Key (might be omitted)

		// AggregatedAnalytics (YES / NO)

		// InternetMultiplayer (YES / NO)

		// AppSizeGame
		// AppSizeLobby
		// AppSizeMenu

		// and 3 parameters left for the future.

		Hashtable<String, String> params = new Hashtable<String, String>();

		params.put(VAR_PREMIUM, hasAnyPremium ? YES : NO);

		params.put(VAR_ANALYTICS_AGGREGATED,
				QuantroPreferences.getAnalyticsAggregated(activity) ? YES : NO);

		int[] sizes = new int[] {
				QuantroPreferences.getScreenSizeGame(activity),
				QuantroPreferences.getScreenSizeLobby(activity),
				QuantroPreferences.getScreenSizeMenu(activity) };
		String[] sizeStrs = new String[3];
		for (int i = 0; i < 3; i++) {
			switch (sizes[i]) {
			case QuantroPreferences.XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION:
				sizeStrs[i] = "NotificationBar";
				break;
			case QuantroPreferences.XL_SIZE_SHOW_NAVIGATION:
				sizeStrs[i] = "FullScreen";
				break;
			case QuantroPreferences.XL_SIZE_IMMERSIVE:
				sizeStrs[i] = "Immersive" ;
				break ;
			default:
				sizeStrs[i] = OTHER;
			}
		}

		params.put(VAR_APP_SIZE_GAME, sizeStrs[0]);
		params.put(VAR_APP_SIZE_LOBBY, sizeStrs[1]);
		params.put(VAR_APP_SIZE_MENU, sizeStrs[2]);

		sendLog(EVENT_ID_SETTINGS_APP, params);
		return true;
	}
	
	
	public static final boolean logGameViewMemoryCapabilities( Activity a, GameViewMemoryCapabilities gvmc ) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		
		params.put(VAR_SCREEN_WIDTH, gvmc.getScreenWidthString()) ;
		params.put(VAR_SCREEN_HEIGHT, gvmc.getScreenHeightString()) ;
		params.put(VAR_HEAP_SIZE, gvmc.getHeapMBString() + " / " + gvmc.getLargeHeapMBString()) ;
		params.put(VAR_LOAD_IMAGE_SIZE, gvmc.getLoadImagesSizeString()) ;
		params.put(VAR_BACKGROUND_IMAGE_SIZE, gvmc.getBackgroundImageSizeString()) ;
		params.put(VAR_BACKGROUND_SHUFFLE, gvmc.getShuffleSupported() ? YES : NO) ;
		params.put(VAR_BLIT, gvmc.getBlitString()) ;
		params.put(VAR_RECYCLE_TO_VEIL, gvmc.getRecycleToVeil() ? YES : NO) ;
		params.put(VAR_OPTIONS_OVERLAY, gvmc.getGameOverlaySupported() ? YES : NO) ;
		
		sendLog(EVENT_ID_GAME_VIEW_MEMORY_CAPABILITIES, params);
		return true;
	}
	
	
	public static final boolean logKeyRing( Activity a, KeyRing kr ) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		
		// Premium SKUs?
		String value ;
		for ( int i = 0; i < PremiumSKU.ALL.length; i++ ) {
			String sku = PremiumSKU.ALL[i] ;
			if ( kr.signedPremium(sku) ) {
				value = PURCHASED ;
			} else if ( kr.signedPromotion(sku) ) {
				value = PROMO ;
			} else if ( PremiumLibrary.isUnlockedByQuantroXL(sku) && kr.signedXL() ) {
				value = XL ;
			} else {
				value = NO ;
			}
			
			params.put(sku, value) ;
		}
		
		// XL?
		if ( kr.signedXL() ) {
			QuantroXLKey xlKey = (QuantroXLKey)kr.getXL() ;
			if ( xlKey.getJSON() == null ) {
				value = PROMO ;
			} else {
				value = PURCHASED ;
			}
		} else {
			value = NO ;
		}
		params.put(VAR_XL, value) ;
		
		sendLog(EVENT_ID_PREMIUM_LIBRARY, params);
		return true;
	}
	

	// //////////////////////////////////////////////////////////////////////////
	//
	// GAMES FROM OUTSIDE, i.e., not within a GameActivity
	// e.g. launches, load games, delete saves, etc.
	//

	/**
	 * Logs the launch of a new game, including the initial game settings.
	 * 
	 * Game modes are reported in a high-detail string, e.g. '0001 Quantro
	 * Endurance'. Level, clears, first garbage and garbage are reported
	 * directly.
	 * 
	 * This leaves 5 open parameters. What do we do? Reminders of what these
	 * game modes mean? Probably a waste. User history, e.g. the number of times
	 * they've played this game mode? Terrible idea; don't want to enumerate
	 * strings like that. # played in this lobby? Why is that not a LOBBY event?
	 * 
	 * For now, I can't actually think of any extra parameters. I leave them
	 * open for later inclusion.
	 * 
	 * @param gs
	 * @param quickPlay
	 * @return Success
	 */
	public final static boolean logNewGame(GameSettings gs, boolean quickPlay) {

		Hashtable<String, String> params = new Hashtable<String, String>();

		params.put(VAR_GAME_MODE, gameModeString(gs.getMode()));
		params.put(VAR_LEVEL, "" + gs.getLevel());
		params.put(VAR_CLEARS, "" + gs.getClearsPerLevel());
		params.put(VAR_GARBAGE, "" + gs.getGarbage());
		params.put(VAR_GARBAGE_PER_LEVEL, "" + gs.getGarbagePerLevel());
		params.put(VAR_QUICK_PLAY, quickPlay ? YES : NO) ;

		return sendLog(EVENT_ID_GAME_NEW, params);
	}

	public final static boolean logLoadGame(GameSettings gs,
			long timeSinceStarted, long timeSinceLastPlay, boolean quickPlay) {
		// We don't report time directly; instead we aggregate.
		String[] times = new String[2];
		long[] millis = new long[] { timeSinceStarted, timeSinceLastPlay };
		for (int i = 0; i < 2; i++) {
			int minutes = (int) (millis[i] / (1000 * 60));
			times[i] = binByMaxExclusive(minutes, new int[] { 1, 5, 10, 30, 60,
					60 * 4, 60 * 24, 60 * 48, 60 * 72 }, new String[] {
					"00:00 - 00:01", "00:01 - 00:05", "00:05 - 00:10",
					"00:10 - 00:30", "00:30 - 01:00", "01:00 - 04:00",
					"04:00 - 24:00", "24:00 - 48:00", "48:00 - 72:00" },
					"72:00+");
		}

		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gs.getMode()));
		if (millis[0] >= 0)
			params.put(VAR_TIME_SINCE_STARTED, times[0]);
		if (millis[1] >= 0)
			params.put(VAR_TIME_SINCE_LAST_PLAYED, times[1]);
		params.put(VAR_QUICK_PLAY, quickPlay ? YES : NO) ;

		sendLog(EVENT_ID_GAME_LOAD, params);
		return true;
	}

	public final static boolean logDeleteGame(int gameMode,
			long timeSinceStarted, long timeSinceLastPlayed, long timePlayed,
			boolean userAction) {
		// We don't report time directly; instead we aggregate.

		int minutes = (int) (timeSinceStarted / (1000 * 60));
		String sinceStarted = binByMaxExclusive(minutes, new int[] { 1, 5, 10,
				30, 60, 60 * 4, 60 * 24, 60 * 48, 60 * 72 }, new String[] {
				"00:00 - 00:01", "00:01 - 00:05", "00:05 - 00:10",
				"00:10 - 00:30", "00:30 - 01:00", "01:00 - 04:00",
				"04:00 - 24:00", "24:00 - 48:00", "48:00 - 72:00" }, "72:00+");

		minutes = (int) (timeSinceLastPlayed / (1000 * 60));
		String sinceLast = binByMaxExclusive(minutes, new int[] { 1, 5, 10, 30,
				60, 60 * 4, 60 * 24, 60 * 48, 60 * 72 }, new String[] {
				"00:00 - 00:01", "00:01 - 00:05", "00:05 - 00:10",
				"00:10 - 00:30", "00:30 - 01:00", "01:00 - 04:00",
				"04:00 - 24:00", "24:00 - 48:00", "48:00 - 72:00" }, "72:00+");

		minutes = (int) (timePlayed / (1000 * 60));
		String played = binByMaxExclusive(minutes, new int[] { 1, 5, 10, 15,
				30, 45, 60, 90, 120, 180, 240, 300, 360 }, new String[] {
				"00:00 - 00:01", "00:01 - 00:05", "00:05 - 00:10",
				"00:10 - 00:15", "00:15 - 00:30", "00:30 - 00:45",
				"00:45 - 01:00", "01:00 - 01:30", "01:30 - 02:00",
				"02:00 - 03:00", "03:00 - 04:00", "04:00 - 05:00",
				"05:00 - 06:00" }, "06:00+");

		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		if (timeSinceStarted >= 0)
			params.put(VAR_TIME_SINCE_STARTED, sinceStarted);
		if (timeSinceLastPlayed >= 0)
			params.put(VAR_TIME_SINCE_LAST_PLAYED, sinceLast);
		if (timePlayed >= 0)
			params.put(VAR_TIME_SPENT, played);
		params.put(VAR_USER_ACTION, userAction ? YES : NO);

		sendLog(EVENT_ID_GAME_DELETE, params);
		return true;
	}
	
	public final static boolean logCustomGameMode( int cgms_id, boolean isEdit, boolean hasSaves) {

		Hashtable<String, String> params = new Hashtable<String, String>();

		params.put(VAR_CGMS_ID, "" + cgms_id) ;
		params.put(VAR_IS_EDIT, isEdit ? YES : NO) ;
		params.put(VAR_HAS_SAVES, hasSaves ? YES : NO) ;

		sendLog(EVENT_ID_GAME_CUSTOM_MODE, params);
		return true;
	}

	private final static String gameModeString(int gameMode) {
		return String.format("%04d", gameMode) + " " + GameModes.name(gameMode);
	}

	//
	// GAMES FROM OUTSIDE
	//
	// //////////////////////////////////////////////////////////////////////////

	// //////////////////////////////////////////////////////////////////////////
	//
	// GAMES FROM INSIDE, i.e., within a GameActivity or GameService
	// e.g. pauses, game ends, leave Activity with 'home', etc.
	//
	

	public static final boolean logInGameOutOfMemory(GameViewMemoryCapabilities gvmc, String assetKey, int attemptNumber) {
		Hashtable<String, String> params = new Hashtable<String, String>() ;
		params.put(VAR_SCREEN_SIZE, gvmc == null ? NONE
				: gvmc.getScreenWidthString() + " by " + gvmc.getScreenHeightString()) ;
		params.put(VAR_HEAP_SIZE, gvmc == null ? NONE
				: gvmc.getHeapMBString()) ;
		params.put(VAR_LOAD_AND_BACKGROUND_SIZE_AND_BLIT, gvmc == null ? NONE
				: gvmc.getLoadImagesSizeString() + " and " + gvmc.getBackgroundImageSizeString() + " and " + gvmc.getBlitString() ) ;
		
		params.put(VAR_ASSET_NAME, assetKey == null ? NONE : assetKey) ;
		
		params.put(VAR_ATTEMPT, ""+attemptNumber) ;
		
		params.put(VAR_RELEASE, Build.VERSION.RELEASE);

		params.put(VAR_BRAND, Build.BRAND);
		params.put(VAR_MANUFACTURER, Build.MANUFACTURER);
		params.put(VAR_MODEL, Build.MODEL);
		
		sendLog(EVENT_ID_IN_GAME_OUT_OF_MEMORY, params);
		return true;
	}
	

	public static final boolean logInGamePause(int gameMode, boolean userAction) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		params.put(VAR_USER_ACTION, userAction ? YES : NO);

		sendLog(EVENT_ID_IN_GAME_PAUSE, params);
		return true;
	}

	public static final boolean logInGameUnpause(int gameMode,
			long timeSpentPaused, boolean userAction, boolean pauseWasUserAction) {
		if (timeSpentPaused < 0)
			return false;

		int seconds = (int) (timeSpentPaused / 1000);
		int minutes = seconds / 60;
		String spentPaused = binByMaxExclusive(seconds, new int[] { 1, 10, 30,
				60, 90, 120 }, new String[] { "00:00:00 - 00:00:01",
				"00:00:01 - 00:00:10", "00:00:10 - 00:00:30",
				"00:00:30 - 00:01:00", "00:01:00 - 00:01:30",
				"00:01:30 - 00:02:00" }, null);
		if (spentPaused == null) {
			spentPaused = binByMaxExclusive(minutes, new int[] { 3, 5, 10, 15,
					30, 45, 60 }, new String[] { "00:02:00 - 00:03:00",
					"00:03:00 - 00:05:00", "00:05:00 - 00:10:00",
					"00:10:00 - 00:15:00", "00:15:00 - 00:30:00",
					"00:30:00 - 00:45:00", "00:45:00 - 01:00:00" }, "01:00:00+");
		}

		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		params.put(VAR_TIME_SPENT, spentPaused);
		params.put(VAR_USER_ACTION, userAction ? YES : NO);
		params.put(VAR_RESPONSE_TO_USER_ACTION, userAction ? YES : NO);

		sendLog(EVENT_ID_IN_GAME_UNPAUSE, params);
		return true;
	}

	public static final boolean logInGameActivityStart(int gameMode,
			boolean serviceStopped) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		params.put(VAR_SERVICE_STOPPED, serviceStopped ? YES : NO);

		sendLog(EVENT_ID_SERVICED_GAME_ACTIVITY_START, params);
		return true;
	}

	public static final boolean logInGameActivityStop(int gameMode,
			boolean serviceStopped) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		params.put(VAR_SERVICE_STOPPED, serviceStopped ? YES : NO);

		sendLog(EVENT_ID_SERVICED_GAME_ACTIVITY_STOP, params);
		return true;
	}

	public static final boolean logInGameExamineResult(int gameMode,
			boolean gameOver) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		params.put(VAR_GAME_OVER, gameOver ? YES : NO);

		sendLog(EVENT_ID_IN_GAME_EXAMINE_RESULT, params);
		return true;
	}

	public static final boolean logInGameExamineResolvedToContinue(
			int gameMode, boolean gameOver) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		params.put(VAR_GAME_OVER, gameOver ? YES : NO);

		sendLog(EVENT_ID_IN_GAME_EXAMINE_RESOLVED_TO_CONTINUE,
				params);
		return true;
	}

	public static final boolean logInGameExamineResolvedToQuit(int gameMode,
			boolean gameOver) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		params.put(VAR_GAME_OVER, gameOver ? YES : NO);

		sendLog(EVENT_ID_IN_GAME_EXAMINE_RESOLVED_TO_QUIT, params);
		return true;
	}

	public static final boolean logInGameExamineResolvedToReplay(int gameMode,
			boolean gameOver) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		params.put(VAR_GAME_OVER, gameOver ? YES : NO);

		sendLog(EVENT_ID_IN_GAME_EXAMINE_RESOLVED_TO_REPLAY,
				params);
		return true;
	}

	public static final boolean logInGameExamineResolvedToRewind(int gameMode,
			boolean gameOver) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		params.put(VAR_GAME_OVER, gameOver ? YES : NO);

		sendLog(EVENT_ID_IN_GAME_EXAMINE_RESOLVED_TO_REWIND,
				params);
		return true;
	}

	public static final boolean logInGameLoad(int gameMode, boolean asynchronous) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		params.put(VAR_ASYNCHRONOUS, asynchronous ? YES : NO);

		sendLog(EVENT_ID_IN_GAME_LOAD, params);
		return true;
	}

	public static final boolean logInGameSave(int gameMode, boolean asynchronous) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		params.put(VAR_ASYNCHRONOUS, asynchronous ? YES : NO);

		sendLog(EVENT_ID_IN_GAME_SAVE, params);
		return true;
	}

	public static final boolean logInGameLoadCheckpoint(int gameMode,
			boolean asynchronous) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		params.put(VAR_ASYNCHRONOUS, asynchronous ? YES : NO);

		sendLog(EVENT_ID_IN_GAME_LOAD_CHECKPOINT, params);
		return true;
	}

	public static final boolean logInGameSaveCheckpoint(int gameMode,
			boolean isEphemeral, boolean asynchronous) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_GAME_MODE, gameModeString(gameMode));
		params.put(VAR_EPHEMERAL, isEphemeral ? YES : NO);
		params.put(VAR_ASYNCHRONOUS, asynchronous ? YES : NO);

		sendLog(EVENT_ID_IN_GAME_SAVE_CHECKPOINT, params);
		return true;
	}

	//
	// GAMES FROM INSIDE, i.e., within a GameActivity or GameService
	// e.g. pauses, game ends, leave Activity with 'home', etc.
	//
	// //////////////////////////////////////////////////////////////////////////

	// //////////////////////////////////////////////////////////////////////////
	//
	// MATCHMAKING
	//
	
	
	public static final boolean logMatchmakingRequestTicket(boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);

		sendLog(EVENT_ID_MATCHMAKING_REQUEST_TICKET, params);
		return true;
	}
	
	public static final boolean logMatchmakingNoTicketNoResponse(boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);

		sendLog(EVENT_ID_MATCHMAKING_NO_TICKET_NO_RESPONSE, params);
		return true;
	}
	
	

	public static final boolean logMatchmakingRequest(boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);

		sendLog(EVENT_ID_MATCHMAKING_REQUEST, params);
		return true;
	}

	public static final boolean logMatchmakingPromise(boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);

		sendLog(EVENT_ID_MATCHMAKING_PROMISE, params);
		return true;
	}

	public static final boolean logMatchmakingMatch(boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);

		sendLog(EVENT_ID_MATCHMAKING_MATCH, params);
		return true;
	}

	public static final boolean logMatchmakingUDPHolePunchSucceeded(
			boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);
		params.put(VAR_SUCCESS, YES);

		sendLog(EVENT_ID_MATCHMAKING_UDP_HOLE_PUNCH, params);
		return true;
	}

	public static final boolean logMatchmakingUDPHolePunchFailed(boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);
		params.put(VAR_SUCCESS, NO);

		sendLog(EVENT_ID_MATCHMAKING_UDP_HOLE_PUNCH, params);
		return true;
	}

	public static final boolean logMatchmakingInformationExchangeFailed(
			boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);
		params.put(VAR_SUCCESS, NO);

		sendLog(EVENT_ID_MATCHMAKING_INFORMATION_EXCHANGE, params);
		return true;
	}

	public static final boolean logMatchmakingNoResponse(boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);

		sendLog(EVENT_ID_MATCHMAKING_NO_RESPONSE, params);
		return true;
	}

	public static final boolean logMatchmakingRejectedTooBusy(boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);
		params.put(VAR_REASON, REASON_FULL);

		sendLog(EVENT_ID_MATCHMAKING_REJECTED, params);
		return true;
	}

	public static final boolean logMatchmakingRejectedNonceInvalid(
			boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);
		params.put(VAR_REASON, REASON_NONCE_INVALID);

		sendLog(EVENT_ID_MATCHMAKING_REJECTED, params);
		return true;
	}

	public static final boolean logMatchmakingRejectedNonceInUse(boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);
		params.put(VAR_REASON, REASON_NONCE_IN_USE);

		sendLog(EVENT_ID_MATCHMAKING_REJECTED, params);
		return true;
	}

	public static final boolean logMatchmakingRejectedPortRandomization(
			boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);
		params.put(VAR_REASON, REASON_PORT_RANDOMIZATION);

		sendLog(EVENT_ID_MATCHMAKING_REJECTED, params);
		return true;
	}

	public static final boolean logMatchmakingRejectedUnspecified(boolean isGame) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_GAME, isGame ? YES : NO);
		params.put(VAR_REASON, REASON_UNSPECIFIED);

		sendLog(EVENT_ID_MATCHMAKING_REJECTED, params);
		return true;
	}

	//
	// MATCHMAKING
	//
	// //////////////////////////////////////////////////////////////////////////

	// //////////////////////////////////////////////////////////////////////////
	//
	// IN LOBBY
	//

	public final static boolean logInLobbyCreated(boolean isWifi) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_WIFI, isWifi ? YES : NO);

		sendLog(EVENT_ID_LOBBY_CREATED, params);
		return true;
	}

	public final static boolean logInLobbyClosed(boolean isWifi,
			long millisOpen, int numLaunches) {

		if (millisOpen < 0 || numLaunches < 0) {
			Log.e(TAG, "logWifiLobbyClosed with negative input: " + millisOpen
					+ ", " + numLaunches);
			return false;
		}

		// We don't report raw values; we limit possible values to a range.

		// For time, we like the following:
		// 00:00 - 00:01 under a minute
		// 00:01 - 00:05 one to five minutes
		// 00:05 - 00:10 five to ten
		// 00:10 - 00:30 ten to thirty
		// 00:30 - 01:00 thirty to an hour
		// 01:00 - 02:00 hour to two
		// 02:00 - 03:00
		// 03:00 - 04:00
		// 04:00 - 08:00
		// 08:00 - 24:00
		// 24:00 - 48:00
		// 48:00 - 72:00
		// 72:00+ // more than 3 days!

		// For number of launches:
		// 0, 1, 2, 3, 4, 5, 6-10, 11-15, 16-20, 21-30, 31+

		String timeOpen;
		int minutesOpen = (int) (millisOpen / (1000 * 60));
		int hoursOpen = minutesOpen / 60; // probably 0

		timeOpen = binByMaxExclusive(minutesOpen,
				new int[] { 1, 5, 10, 30, 60 }, new String[] { "00:00 - 00:01",
						"00:01 - 00:05", "00:05 - 00:10", "00:10 - 00:30",
						"00:30 - 01:00" }, null);
		if (timeOpen == null)
			timeOpen = binByMaxExclusive(hoursOpen, new int[] { 2, 3, 4, 8, 24,
					48, 72 }, new String[] { "01:00 - 02:00", "02:00 - 03:00",
					"03:00 - 04:00", "04:00 - 08:00", "08:00 - 24:00",
					"24:00 - 48:00", "48:00 - 72:00" }, "72:00+");

		String launches = binByMaxInclusive(numLaunches, new int[] { 5, 10, 15,
				20, 30 }, new String[] { null, "6-10", "11-15", "16-20",
				"21-30" }, "31+");
		if (launches == null)
			launches = "" + numLaunches; // stringify directly

		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_WIFI, isWifi ? YES : NO);
		params.put(VAR_TIME_OPEN, timeOpen);
		params.put(VAR_NUM_LAUNCHES, launches);

		sendLog(EVENT_ID_LOBBY_CLOSED, params);
		return true;
	}

	public static final boolean logInLobbyActivityStart(boolean serviceStopped) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_SERVICE_STOPPED, serviceStopped ? YES : NO);

		sendLog(EVENT_ID_SERVICED_LOBBY_ACTIVITY_START, params);
		return true;
	}

	public static final boolean logInLobbyActivityStop(boolean serviceStopped) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_SERVICE_STOPPED, serviceStopped ? YES : NO);

		sendLog(EVENT_ID_SERVICED_LOBBY_ACTIVITY_STOP, params);
		return true;
	}

	/**
	 * Event type is not examined. It is the caller's responsibility to ensure
	 * consistency, correctness, and above all a limited number of possible
	 * values.
	 */
	public static final boolean logInLobbyLogEvent(boolean isWifi,
			String eventType) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_WIFI, isWifi ? YES : NO);
		params.put(VAR_EVENT_TYPE, eventType);

		sendLog(EVENT_ID_IN_LOBBY_LOG_EVENT, params);
		return true;
	}

	public static final boolean logInLobbyCountdownStart(boolean isWifi,
			int gameMode) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_WIFI, isWifi ? YES : NO);
		params.put(VAR_GAME_MODE, gameModeString(gameMode));

		sendLog(EVENT_ID_IN_LOBBY_COUNTDOWN_START, params);
		return true;
	}

	public static final boolean logInLobbyCountdownAborted(boolean isWifi,
			int gameMode) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_WIFI, isWifi ? YES : NO);
		params.put(VAR_GAME_MODE, gameModeString(gameMode));

		sendLog(EVENT_ID_IN_LOBBY_COUNTDOWN_ABORTED, params);
		return true;
	}

	public static final boolean logInLobbyCountdownHalted(boolean isWifi,
			int gameMode) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_WIFI, isWifi ? YES : NO);
		params.put(VAR_GAME_MODE, gameModeString(gameMode));

		sendLog(EVENT_ID_IN_LOBBY_COUNTDOWN_HALTED, params);
		return true;
	}

	public static final boolean logInLobbyLaunch(boolean isWifi, GameSettings gs) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_IS_WIFI, isWifi ? YES : NO);
		params.put(VAR_GAME_MODE, gameModeString(gs.getMode()));

		sendLog(EVENT_ID_IN_LOBBY_LAUNCH, params);
		return true;
	}

	//
	// IN LOBBY
	//
	// //////////////////////////////////////////////////////////////////////////

	
	// //////////////////////////////////////////////////////////////////////////
	//
	// XL KEYS
	//

	public static final boolean logKeyInAppPurchaseActivationSuccess( Key key ) {
		return logKeyInAppPurchaseActivation(  keyToValueString(key), RESULT_SUCCESS );
	}

	public static final boolean logKeyInAppPurchaseActivationFailedNoServer( Key key ) {
		return logKeyInAppPurchaseActivation(  keyToValueString(key), RESULT_FAILED_NO_SERVER );
	}

	public static final boolean logKeyInAppPurchaseActivationFailedInvalid( Key key ) {
		return logKeyInAppPurchaseActivation(  keyToValueString(key), RESULT_FAILED_INVALID );
	}

	public static final boolean logKeyInAppPurchaseActivationFailedIssued( Key key ) {
		return logKeyInAppPurchaseActivation(  keyToValueString(key), RESULT_FAILED_ISSUED );
	}

	public static final boolean logKeyInAppPurchaseActivationFailedNotAvailable( Key key ) {
		return logKeyInAppPurchaseActivation(  keyToValueString(key), RESULT_FAILED_NOT_AVAILABLE );
	}

	public static final boolean logKeyInAppPurchaseActivationFailedRefused( Key key ) {
		return logKeyInAppPurchaseActivation(  keyToValueString(key), RESULT_FAILED_REFUSED );
	}

	public static final boolean logKeyInAppPurchaseActivationFailedWaiting( Key key ) {
		return logKeyInAppPurchaseActivation(  keyToValueString(key), RESULT_FAILED_WAITING );
	}

	public static final boolean logKeyInAppPurchaseActivationFailedStorage( Key key ) {
		return logKeyInAppPurchaseActivation(  keyToValueString(key), RESULT_FAILED_STORAGE );
	}

	public static final boolean logKeyInAppPurchaseActivationFailedUnspecified( Key key ) {
		return logKeyInAppPurchaseActivation(  keyToValueString(key), RESULT_FAILED_UNSPECIFIED );
	}

	private static final boolean logKeyInAppPurchaseActivation(String key, String result) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_KEY, key) ;
		params.put(VAR_RESULT, result);

		sendLog(EVENT_ID_KEY_IN_APP_PURCHASE_ACTIVATION, params);
		return true;
	}

	public static final boolean logKeyInAppPurchaseResponseOK( Key key ) {
		return logKeyInAppPurchaseResponse( keyToValueString(key), RESPONSE_OK );
	}

	public static final boolean logKeyInAppPurchaseResponseUserCanceled( Key key ) {
		return logKeyInAppPurchaseResponse( keyToValueString(key), RESPONSE_USER_CANCELED );
	}

	public static final boolean logKeyInAppPurchaseResponseServiceUnavailable( Key key ) {
		return logKeyInAppPurchaseResponse( keyToValueString(key), RESPONSE_SERVICE_UNAVAILABLE );
	}

	public static final boolean logKeyInAppPurchaseResponseBillingUnavailable( Key key ) {
		return logKeyInAppPurchaseResponse( keyToValueString(key), RESPONSE_BILLING_UNAVAILABLE );
	}

	public static final boolean logKeyInAppPurchaseResponseItemUnavailable( Key key ) {
		return logKeyInAppPurchaseResponse( keyToValueString(key), RESPONSE_ITEM_UNAVAILABLE );
	}

	public static final boolean logKeyInAppPurchaseResponseDeveloperError( Key key ) {
		return logKeyInAppPurchaseResponse( keyToValueString(key), RESPONSE_DEVELOPER_ERROR );
	}

	public static final boolean logKeyInAppPurchaseResponseError( Key key ) {
		return logKeyInAppPurchaseResponse( keyToValueString(key), RESPONSE_ERROR );
	}

	private static final boolean logKeyInAppPurchaseResponse(String key, String response) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_KEY, key) ;
		params.put(VAR_RESPONSE, response);

		sendLog(EVENT_ID_KEY_IN_APP_PURCHASE_RESPONSE, params);
		return true;
	}

	public static final boolean logKeyInAppPurchaseStatePurchased( Key key ) {
		return logKeyInAppPurchaseState( keyToValueString(key), STATE_PURCHASED );
	}

	public static final boolean logKeyInAppPurchaseStateCanceled( Key key ) {
		return logKeyInAppPurchaseState( keyToValueString(key), STATE_CANCELED );
	}

	public static final boolean logKeyInAppPurchaseStateRefunded( Key key ) {
		return logKeyInAppPurchaseState( keyToValueString(key), STATE_REFUNDED );
	}

	private static final boolean logKeyInAppPurchaseState(String key, String state) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_KEY, key) ;
		params.put(VAR_STATE, state);
		
		sendLog(EVENT_ID_KEY_IN_APP_PURCHASE_STATE, params);
		return true;
	}

	public static final boolean logKeyInAppPurchaseBillingAvailable(
			boolean available) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_RESPONSE, available ? YES : NO);

		sendLog(EVENT_ID_KEY_IN_APP_PURCHASE_BILLING_AVAILABLE, null);
		return true;
	}

	public static final boolean logKeyManualEntrySuccess( Key key ) {
		return logKeyManualEntry( keyToValueString(key), RESULT_SUCCESS );
	}

	public static final boolean logKeyManualEntryFailedNoServer( Key key ) {
		return logKeyManualEntry( keyToValueString(key), RESULT_FAILED_NO_SERVER );
	}

	public static final boolean logKeyManualEntryFailedInvalid( Key key ) {
		return logKeyManualEntry( keyToValueString(key), RESULT_FAILED_INVALID );
	}

	public static final boolean logKeyManualEntryFailedIssued( Key key ) {
		return logKeyManualEntry( keyToValueString(key), RESULT_FAILED_ISSUED );
	}

	public static final boolean logKeyManualEntryFailedNotAvailable( Key key ) {
		return logKeyManualEntry( keyToValueString(key), RESULT_FAILED_NOT_AVAILABLE );
	}

	public static final boolean logKeyManualEntryFailedRefused( Key key ) {
		return logKeyManualEntry( keyToValueString(key), RESULT_FAILED_REFUSED );
	}

	public static final boolean logKeyManualEntryFailedWaiting( Key key ) {
		return logKeyManualEntry( keyToValueString(key), RESULT_FAILED_WAITING );
	}

	public static final boolean logKeyManualEntryFailedStorage( Key key ) {
		return logKeyManualEntry( keyToValueString(key), RESULT_FAILED_STORAGE );
	}

	public static final boolean logKeyManualEntryFailedUnspecified( Key key ) {
		return logKeyManualEntry( keyToValueString(key), RESULT_FAILED_UNSPECIFIED );
	}

	private static final boolean logKeyManualEntry(String key, String result) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_KEY, key) ;
		params.put(VAR_RESULT, result);

		sendLog(EVENT_ID_KEY_MANUAL_ENTRY, params);
		return true;
	}

	public static final boolean logKeyReactivationSuccess( Key key ) {
		return logKeyReactivation( keyToValueString(key), RESULT_SUCCESS );
	}

	public static final boolean logKeyReactivationFailedNoServer( Key key ) {
		return logKeyReactivation( keyToValueString(key), RESULT_FAILED_NO_SERVER );
	}

	public static final boolean logKeyReactivationFailedInvalid( Key key ) {
		return logKeyReactivation( keyToValueString(key), RESULT_FAILED_INVALID );
	}

	public static final boolean logKeyReactivationFailedIssued( Key key ) {
		return logKeyReactivation( keyToValueString(key), RESULT_FAILED_ISSUED );
	}

	public static final boolean logKeyReactivationFailedNotAvailable( Key key ) {
		return logKeyReactivation( keyToValueString(key), RESULT_FAILED_NOT_AVAILABLE );
	}

	public static final boolean logKeyReactivationFailedRefused( Key key ) {
		return logKeyReactivation( keyToValueString(key), RESULT_FAILED_REFUSED );
	}

	public static final boolean logKeyReactivationFailedWaiting( Key key ) {
		return logKeyReactivation( keyToValueString(key), RESULT_FAILED_WAITING );
	}

	public static final boolean logKeyReactivationFailedStorage( Key key ) {
		return logKeyReactivation( keyToValueString(key), RESULT_FAILED_STORAGE );
	}

	public static final boolean logKeyReactivationFailedUnspecified( Key key ) {
		return logKeyReactivation( keyToValueString(key), RESULT_FAILED_UNSPECIFIED );
	}

	private static final boolean logKeyReactivation(String key, String result) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_KEY, key) ;
		params.put(VAR_RESULT, result);

		sendLog(EVENT_ID_KEY_REACTIVATION, params);
		return true;
	}
	
	private static final String keyToValueString( Key key ) {
		if ( key instanceof QuantroXLKey ) {
			return XL ;
		} else if ( key instanceof PremiumContentKey ) {
			return ((PremiumContentKey) key).getItem() ;
		} else if ( key instanceof PromotionalKey ) {
			return PROMO ;
		}
		
		return NO ;
	}

	//
	// XL KEYS
	//
	// //////////////////////////////////////////////////////////////////////////

	// //////////////////////////////////////////////////////////////////////////
	//
	// AD CLICKS
	//

	public static final boolean logAdClick() {
		sendLog(EVENT_ID_AD_CLICK, null);
		return true;
	}

	public static final boolean logCustomXLAdClick(String tag) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put(VAR_TAG, tag);

		sendLog(EVENT_ID_AD_CLICK_CUSTOM_XL, params);
		return true;
	}

	private static final Hashtable<String, String> LOG_AD_LOAD_PARAMS_YES = new Hashtable<String, String>();
	private static final Hashtable<String, String> LOG_AD_LOAD_PARAMS_NO = new Hashtable<String, String>();

	public static final boolean logAdLoad(boolean success) {
		if (success && LOG_AD_LOAD_PARAMS_YES.size() == 0)
			LOG_AD_LOAD_PARAMS_YES.put(VAR_SUCCESS, YES);
		if (!success && LOG_AD_LOAD_PARAMS_NO.size() == 0)
			LOG_AD_LOAD_PARAMS_NO.put(VAR_SUCCESS, NO);

		sendLog(EVENT_ID_AD_LOAD, success ? LOG_AD_LOAD_PARAMS_YES
				: LOG_AD_LOAD_PARAMS_NO);
		return true;
	}

	//
	// AD CLICKS
	//
	// //////////////////////////////////////////////////////////////////////////

	// //////////////////////////////////////////////////////////////////////////
	//
	// HELPERS
	//

	/**
	 * Given an integer value, returns as a string the appropriate bin label.
	 * Returns maxLabel if the value is beyond the last bin max.
	 * 
	 * PRECONDITIONS: binMax.length == binLabel.length binMax[i] < binMax[j] for
	 * all i < j
	 * 
	 * @param value
	 *            The value to bin
	 * @param binMax
	 *            The maximum value for each bin, inclusive.
	 * @param binLabel
	 *            The label to apply for each bin
	 * @param maxLabel
	 *            The label to return if the value is greater than the final max
	 */
	private static final String binByMaxInclusive(int value, int[] binMax,
			String[] binLabel, String maxLabel) {
		for (int i = 0; i < binMax.length; i++) {
			if (value <= binMax[i])
				return binLabel[i];
		}

		return maxLabel;
	}

	/**
	 * Given an integer value, returns as a string the appropriate bin label.
	 * Returns maxLabel if the value is beyond the last bin max.
	 * 
	 * PRECONDITIONS: binMax.length == binLabel.length binMax[i] < binMax[j] for
	 * all i < j
	 * 
	 * @param value
	 *            The value to bin
	 * @param binMax
	 *            The maximum value for each bin, exclusive.
	 * @param binLabel
	 *            The label to apply for each bin
	 * @param maxLabel
	 *            The label to return if the value is greater than the final max
	 */
	private static final String binByMaxExclusive(int value, int[] binMax,
			String[] binLabel, String maxLabel) {
		for (int i = 0; i < binMax.length; i++) {
			if (value < binMax[i])
				return binLabel[i];
		}

		return maxLabel;
	}

	//
	// HELPERS
	//
	// //////////////////////////////////////////////////////////////////////////

}
