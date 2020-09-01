package com.peaceray.quantro.dialog;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.peaceray.quantro.R;
import com.peaceray.quantro.keys.Key;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.lobby.WebConsts;
import com.peaceray.quantro.lobby.WiFiLobbyDetails;
import com.peaceray.quantro.lobby.WiFiLobbyFinder;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.modes.GameModes;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateFormat;
import android.util.Log;

/**
 * A static utility class, meant for formatting in-game dialogs and
 * notifications.  This formatting usually requires reading our Resources and
 * inserting player name(s) as appropriate.
 * 
 * Since this functionality is used by two Activities and two Services, we
 * split it off into its own class.
 * 
 * @author Jake
 *
 */
public class TextFormatting {
	
	public static final String TAG = "TextFormatting" ;
	
	public static final int DISPLAY_DIALOG = 100 ;
	public static final int DISPLAY_NOTIFICATION = 101 ;
	public static final int DISPLAY_TOAST = 102 ;
	public static final int DISPLAY_MENU = 103 ;
	
	public static final int TYPE_LOBBY_CONNECTING 				= 200 ;
	public static final int TYPE_LOBBY_NEGOTIATING 				= 201 ;
	public static final int TYPE_LOBBY_LAUNCHING 				= 202 ;
	public static final int TYPE_LOBBY_LAUNCHING_ABSENT			= 203 ;
	public static final int TYPE_LOBBY_READY					= 204 ;
	public static final int TYPE_LOBBY_QUIT						= 205 ;
	public static final int TYPE_LOBBY_QUIT_YES_BUTTON			= 206 ;
	public static final int TYPE_LOBBY_QUIT_NO_BUTTON			= 207 ;
	public static final int TYPE_LOBBY_CLOSED_FOREVER			= 208 ;
	public static final int TYPE_LOBBY_CLOSED_FOREVER_BUTTON	= 209 ;
	public static final int TYPE_LOBBY_NO_WIFI 					= 210 ;
	private static final int RANGE_TYPE_LOBBY_MIN = 200 ;
	private static final int RANGE_TYPE_LOBBY_MAX = 299 ;
	
	
	public static final int TYPE_GAME_CONNECTING 				= 300 ;
	public static final int TYPE_GAME_NEGOTIATING 				= 301 ;
	public static final int TYPE_GAME_WAITING 					= 302 ;
	public static final int TYPE_GAME_PAUSED 					= 303 ;
	public static final int TYPE_GAME_READY						= 304 ;
	public static final int TYPE_GAME_OVER 						= 305 ;
	public static final int TYPE_GAME_QUIT						= 306 ;
	public static final int TYPE_GAME_QUIT_YES 					= 307 ;
	public static final int TYPE_GAME_QUIT_NO 					= 308 ;
	public static final int TYPE_GAME_OPPONENT_QUIT 			= 309 ;
	public static final int TYPE_GAME_OPPONENT_QUIT_BUTTON		= 310 ;
	public static final int TYPE_GAME_NO_WIFI 					= 311 ;
	private static final int RANGE_TYPE_GAME_MIN = 300 ;
	private static final int RANGE_TYPE_GAME_MAX = 399 ;
	
	
	
	// LOBBIES /////////////////////////////////////////////////////////////////
	// Confirm actions!														////
	public static final int TYPE_LOBBY_CONFIRM_JOIN					= 655 ;
	public static final int TYPE_LOBBY_CONFIRM_JOIN_YES_BUTTON	 	= 656 ;
	public static final int TYPE_LOBBY_CONFIRM_JOIN_NO_BUTTON 		= 657 ;
	public static final int TYPE_LOBBY_CONFIRM_HIDE					= 660 ;
	public static final int TYPE_LOBBY_CONFIRM_HIDE_YES_BUTTON 		= 661 ;
	public static final int TYPE_LOBBY_CONFIRM_HIDE_NO_BUTTON 		= 662 ;
	// Examine lobbies! 													////
	public static final int TYPE_LOBBY_EXAMINE_NAME 				= 665 ;
	public static final int TYPE_LOBBY_EXAMINE_POPULATION			= 666 ;
	public static final int TYPE_LOBBY_EXAMINE_INFO 				= 667 ;
	public static final int TYPE_LOBBY_EXAMINE_QUERY 				= 668 ;
	public static final int TYPE_LOBBY_EXAMINE_NO_WIFI 				= 669 ;
	public static final int TYPE_LOBBY_EXAMINE_JOIN_BUTTON			= 670 ;
	public static final int TYPE_LOBBY_EXAMINE_CANCEL_BUTTON		= 671 ;
	// Examine (searching) lobbies!											////
	public static final int TYPE_LOBBY_EXAMINE_NAME_SEARCHING 			= 672 ;
	public static final int TYPE_LOBBY_EXAMINE_POPULATION_SEARCHING		= 673 ;
	public static final int TYPE_LOBBY_EXAMINE_INFO_SEARCHING 			= 674 ;
	public static final int TYPE_LOBBY_EXAMINE_QUERY_SEARCHING 			= 675 ;
	// WiFi warning!
	public static final int TYPE_LOBBY_MANAGER_NO_WIFI				 = 680 ;
	public static final int TYPE_LOBBY_MANAGER_NO_WIFI_CANCEL_BUTTON = 681 ;
	// 																		////
	////////////////////////////////////////////////////////////////////////////
	
	
															

	// MENU GAME BUTTONS!
	public static final int TYPE_MENU_SINGLE_PLAYER_FIRST_GAME_TITLE 		= 800 ;
	public static final int TYPE_MENU_SINGLE_PLAYER_FIRST_GAME_DESCRIPTION 	= 801 ;
	public static final int TYPE_MENU_SINGLE_PLAYER_RESUME_GAME_TITLE 		= 802 ;
	public static final int TYPE_MENU_SINGLE_PLAYER_RESUME_GAME_DESCRIPTION = 803 ;
	public static final int TYPE_MENU_SINGLE_PLAYER_RESUME_GAME_LONG_DESCRIPTION = 804 ;
	public static final int TYPE_MENU_SINGLE_PLAYER_NEW_GAME_TITLE 			= 805 ;
	public static final int TYPE_MENU_SINGLE_PLAYER_NEW_GAME_DESCRIPTION 	= 806 ;
	public static final int TYPE_MENU_SINGLE_PLAYER_NEW_GAME_MODE_TITLE 		= 807 ;
	public static final int TYPE_MENU_SINGLE_PLAYER_NEW_GAME_MODE_DESCRIPTION 	= 808 ;
	public static final int TYPE_MENU_SINGLE_PLAYER_EDIT_GAME_MODE_TITLE 		= 809 ;
	public static final int TYPE_MENU_SINGLE_PLAYER_EDIT_GAME_MODE_DESCRIPTION 	= 810 ;
	
	
	
	public static final int TYPE_MENU_MULTI_PLAYER_VOTE_GAME_TITLE 				= 820 ;
	public static final int TYPE_MENU_MULTI_PLAYER_VOTE_GAME_DESCRIPTION 		= 821 ;
	
	
	
	
	// /////////////////////////////////////////////////////////////////////////
	// NEW LOBBY ACTIVITY
	public static final int TYPE_NEW_LOBBY_ADDRESS						= 970 ;
	
	// /////////////////////////////////////////////////////////////////////////
	// MANUAL KEY ACTIVATION ACTIVITY
	public static final int TYPE_MANUAL_KEY_ACTIVATION_XL_FAILURE_WAIT		= 975 ;
	public static final int TYPE_MANUAL_KEY_ACTIVATION_XL_SUCCESS 			= 976 ;
	
	public static final int TYPE_MANUAL_KEY_ACTIVATION_PROMO_FAILURE_WAIT		= 977 ;
	public static final int TYPE_MANUAL_KEY_ACTIVATION_PROMO_SUCCESS 			= 978 ;
	
	
	
	
	
	// /////////////////////////////////////////////////////////////////////////
	// LOBBY ACTIVITY
	public static final int TYPE_LOBBY_NAME 							= 1000 ;		// not used right now
	public static final int TYPE_LOBBY_INTERNET_DESCRIPTION 			= 1005 ;
	public static final int TYPE_LOBBY_WIFI_DESCRIPTION 				= 1006 ;
	public static final int TYPE_LOBBY_INTERNET_DESCRIPTION_SHORT 		= 1007 ;
	public static final int TYPE_LOBBY_WIFI_DESCRIPTION_SHORT 			= 1008 ;
	public static final int TYPE_LOBBY_AVAILABILITY_DESCRIPTION			= 1009 ;
	public static final int TYPE_LOBBY_INVITATION_DESCRIPTION_BEAM_ENABLED		= 1010 ;
	public static final int TYPE_LOBBY_INVITATION_DESCRIPTION_BEAM_DISABLED		= 1011 ;
	public static final int TYPE_LOBBY_DETAILS							= 1012 ;
	
	
	public static final int TYPE_LOBBY_LAUNCH_STATUS					= 1015 ;
	public static final int TYPE_LOBBY_PERSONAL_LAUNCH_STATUS			= 1016 ;
	
	
	// /////////////////////////////////////////////////////////////////////////
	// LOBBY MANAGER ACTIVITY
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_TITLE				= 1050 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_TITLE_SEARCHING		= 1051 ;
	
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_DESCRIPTION 				= 1055 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_DESCRIPTION_SEARCHING		= 1056 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_DESCRIPTION_LONG			= 1057 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_DESCRIPTION_LONG_SEARCHING 	= 1058 ;
	
	public static final int TYPE_MENU_LOBBY_MANAGER_HIDE_BUTTON_TITLE				= 1060 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_HIDE_BUTTON_DESCRIPTION 		= 1065 ;
	// DETAILED INFORMATION
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_POPULATION			= 1070 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_CREATION_TIME		= 1071 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_HOST				= 1072 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_ADDRESS_OR_TYPE		= 1073 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_SESSION_ID			= 1074 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_POPULATION_SEARCHING			= 1075 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_CREATION_TIME_SEARCHING			= 1076 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_HOST_SEARCHING					= 1077 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_ADDRESS_OR_TYPE_SEARCHING		= 1078 ;
	public static final int TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_SESSION_ID_SEARCHING			= 1079 ;
	
	
	
	// /////////////////////////////////////////////////////////////////////////
	// EXAMINE GAME RESULT ACTIVITY
	public static final int TYPE_EXAMINE_GAME_RESULT_TITLE_SAVED_GAME 		= 1100 ;
	public static final int TYPE_EXAMINE_GAME_RESULT_TITLE_SP_GAME_OVER		= 1101 ;
	public static final int TYPE_EXAMINE_GAME_RESULT_TITLE_SP_LEVEL_OVER	= 1102 ;
	public static final int TYPE_EXAMINE_GAME_RESULT_TITLE_MP_GAME_OVER		= 1103 ;
	
	public static final int TYPE_EXAMINE_GAME_RESULT_WON_LOST				= 1110 ;
	public static final int TYPE_EXAMINE_GAME_RESULT_PLAYER_NAME 			= 1111 ;
	public static final int TYPE_EXAMINE_GAME_RESULT_LEVEL_FULL 			= 1112 ;
	public static final int TYPE_EXAMINE_GAME_RESULT_LEVEL_NUMBER 			= 1113 ;
	public static final int TYPE_EXAMINE_GAME_RESULT_SCORE_FULL 			= 1114 ;
	public static final int TYPE_EXAMINE_GAME_RESULT_SCORE_NUMBER 			= 1115 ;
	public static final int TYPE_EXAMINE_GAME_RESULT_TIME_FULL 				= 1118 ;
	public static final int TYPE_EXAMINE_GAME_RESULT_TIME_NUMBER 			= 1119 ;
	
	
	// /////////////////////////////////////////////////////////////////////////
	// EXAMINE GAME RESULT ACTIVITY
	public static final int TYPE_NEW_GAME_TITLE_NEW_GAME 					= 1120 ;
	
	
	// /////////////////////////////////////////////////////////////////////////
	// PAUSE OVERLAY
	public static final int TYPE_GAME_PAUSE_OVERLAY_STATE_LOADING		= 1150 ;
	
	public static final int TYPE_GAME_PAUSE_OVERLAY_LOCAL_STATE_PAUSED = 1155 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_LOCAL_STATE_READY		= 1156 ;
	
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_CONNECTING = 1160 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_NEGOTIATING = 1161 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_WAITING = 1162 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_PAUSED_BY_PLAYER = 1163 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_PAUSED_BY_OTHER = 1164 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_READY = 1165 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_STARTING = 1166 ;
	
	public static final int TYPE_GAME_PAUSE_OVERLAY_STATE_DESCRIPTION_LOADING		= 1170 ;
	
	public static final int TYPE_GAME_PAUSE_OVERLAY_LOCAL_STATE_DESCRIPTION_PAUSED		= 1175 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_LOCAL_STATE_DESCRIPTION_READY		= 1176 ;
	
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_CONNECTING = 1180 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_NEGOTIATING = 1181 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_WAITING = 1182 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_WAITING_TO_DROP = 1183 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_PAUSED_BY_PLAYER = 1184 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_PAUSED_BY_PLAYER_REMOTE_ONLY = 1185 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_PAUSED_BY_OTHER = 1186 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_READY = 1187 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_STARTING = 1188 ;
	
	public static final int TYPE_GAME_PAUSE_OVERLAY_COLORS = 1190 ;
	
	public static final int TYPE_GAME_PAUSE_OVERLAY_SOUND_ON = 1191 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_SOUND_OFF = 1192 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_SOUND_OFF_BY_RINGER = 1193 ;
	
	
	public static final int TYPE_GAME_PAUSE_OVERLAY_MUSIC_ON = 1194 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MUSIC_OFF = 1195 ;
	public static final int TYPE_GAME_PAUSE_OVERLAY_MUSIC_OFF_BY_RINGER = 1196 ;
	
	
	public static final int RANGE_TYPE_GAME_PAUSE_MIN = 1150 ;
	public static final int RANGE_TYPE_GAME_PAUSE_MAX = 1196 ;
	
	
	
	private static final int RANGE_TYPE_MENU_MIN = 800 ;
	private static final int RANGE_TYPE_MENU_MAX = 1200 ;
	
	
	
	
	public static final int ROLE_CLIENT = 10000 ;
	public static final int ROLE_HOST 	= 10001 ;
	
	
	/**
	 * Formats and returns the appropriate dialog text for the situation.
	 * Loads resources from 'res' according to R.strings.* and the specified
	 * display, type and role (which must match the DISPLAY_*, TYPE_*, ROLE_*
	 * constants provided).
	 * 
	 * If the string from 'res' includes a names placeholder, either formatted or
	 * unformatted, it will be replaced as-appropriate for the dialog display, type
	 * and role.
	 * 
	 * We include all those names in the provided array which are not 'null'.
	 * 
	 * @param res		The Resources object to use.
	 * @param display	One of DISPLAY_*
	 * @param type		One of TYPE_*
	 * @param role		One of ROLE_*
	 * @param names		An array of names to include, if a placeholder exists in the string.
	 * @return			The formatted message for display.  Returns 'null' if an invalid
	 * 					combination of display, type, role was used.
	 */
	public static String format( Context context, Resources res, int display, int type, int role,
			String [] names ) throws IllegalArgumentException {
		
		return format( context, res, display, type, role, names, null ) ;
	}
	
	
	/**
	 * Formats and returns the appropriate dialog text for the situation.
	 * Loads resources from 'res' according to R.strings.* and the specified
	 * display, type and role (which must match the DISPLAY_*, TYPE_*, ROLE_*
	 * constants provided).
	 * 
	 * If the string from 'res' includes a names placeholder, either formatted or
	 * unformatted, it will be replaced as-appropriate for the dialog display, type
	 * and role.
	 * 
	 * We include all those names in the provided array which are not 'null'.
	 * 
	 * @param res		The Resources object to use.
	 * @param display	One of DISPLAY_*
	 * @param type		One of TYPE_*
	 * @param role		One of ROLE_*
	 * @param name		The name to include; 'null' if no names.
	 * @return			The formatted message for display.  Returns 'null' if an invalid
	 * 					combination of display, type, role was used.
	 */
	public static String format( Context context, Resources res, int display, int type, int role, String name ) {
		
		String [] nameArray = new String[1] ;
		nameArray[0] = name ;
		return format( context, res, display, type, role, nameArray, null ) ;
	}
	
	
	/**
	 * Formats and returns the appropriate dialog text for the situation.
	 * Loads resources from 'res' according to R.strings.* and the specified
	 * display, type and role (which must match the DISPLAY_*, TYPE_*, ROLE_*
	 * constants provided).
	 * 
	 * If the string from 'res' includes a names placeholder, either formatted or
	 * unformatted, it will be replaced as-appropriate for the dialog display, type
	 * and role.
	 * 
	 * We include all those names in the provided array which are not 'null'.
	 * 
	 * @param res		The Resources object to use.
	 * @param display	One of DISPLAY_*
	 * @param type		One of TYPE_*
	 * @param role		One of ROLE_*
	 * @param names		A list of names to include.
	 * @param useNames	We will use those names that are non-null, and for which 'true' appears here.
	 * @return			The formatted message for display.  Returns 'null' if an invalid
	 * 					combination of display, type, role was used.
	 */
	public static String format( Context context, Resources res, int display, int type, int role,
			String [] names, boolean [] useNames ) {
		
		if ( res == null )
			res = context.getResources() ;
		
		// Load the text with placeholders for this display, type, role combo.
		String phText = loadResourceString( res, display, type, role, names ) ;
		
		// Do the placeholdering.
		int numNamesToUse = 0 ;
		if ( names != null ) {
			for ( int i = 0; i < names.length; i++ ) {
				if ( names[i] != null && ( useNames == null || useNames[i] ) )
					numNamesToUse++ ;
			}
		}
		String [] namesToUse = new String[numNamesToUse] ;
		int index = 0 ;
		if ( numNamesToUse > 0 ) {
			for ( int i = 0; i < names.length; i++ ) {
				if ( names[i] != null && ( useNames == null || useNames[i] ) ) {
					namesToUse[index] = names[i] ;
					index++ ;
				}
			}
		}
		return phText == null
			? null
			: replacePlaceholdersWithNames( context, res, display, type, role, namesToUse, phText ) ;
	}
	
	
	public static String format( Context context, Resources res, int display, int type, int role, int gameMode ) {
		return format(context, res, display, type, role, gameMode, (GameResult)null, 0) ;
	}
	
	public static String format( Context context, Resources res, int display, int type, int role, GameResult gameResult ) {
		return format(context, res, display, type, role, gameResult.getGameInformation(0).mode, gameResult, 0) ;
	}
	
	public static String format( Context context, Resources res, int display, int type, int role, GameResult gameResult, int slot ) {
		return format(context, res, display, type, role, gameResult.getGameInformation(0).mode, gameResult, slot) ;
	}
	
	
	
	public static String format( Context context, Resources res, int display, int type, int role, int gameMode, GameResult gameResult ) {
		return format(context, res, display, type, role, gameMode, gameResult, 0) ;
	}
	
	public static String format( Context context, Resources res, int display, int type, int role, int gameMode, GameResult gameResult, int slot ) {
		if ( res == null )
			res = context.getResources() ;
		
		// Load the text with placeholders for this display, type, role combo.
		String phText = loadResourceString( res, display, type, role, new Object[]{gameResult, new Integer(slot)} ) ;
		
		return phText == null
			? null
			: replacePlaceholdersWithGameInformation( context, res, display, type, role, gameMode, gameResult, slot, phText ) ;
	}
	
	public static String format( Context context, Resources res, int display, int type, int role, long longValue ) {
		if ( res == null )
			res = context.getResources() ;
		
		// Load the text with placeholders for this display, type, role combo.
		String phText = loadResourceString( res, display, type, role, new Long(longValue) ) ;
		
		return phText == null
			? null
			: replacePlaceholdersWithLongValue( context, res, display, type, role, longValue, phText ) ;
	}
	
	public static String format( Context context, Resources res, int display, int type, int role, float floatValue) {
		if ( res == null )
			res = context.getResources() ;
		
		// Load the text with placeholders
		String phText = loadResourceString( res, display, type, role, new Float(floatValue) ) ;
		
		return phText == null
				? null
				: replacePlaceholdersWithFloatValue( context, res, display, type, role, floatValue, phText ) ;
	}
	
	public static String formatInt( Context context, Resources res, int display, int type, int role, int intValue) {
		if ( res == null )
			res = context.getResources() ;
		
		// Load the text with placeholders
		String phText = loadResourceString( res, display, type, role, new Integer(intValue) ) ;
		
		return phText == null
				? null
				: replacePlaceholdersWithIntValue( context, res, display, type, role, intValue, phText ) ;
	}
	
	public static String format( Context context, Resources res, int display, int type, int role, int gameMode, Lobby lobby ) {
		if ( res == null )
			res = context.getResources() ;
		
		// Load the text with placeholders for this display, type, role combo.
		String phText = loadResourceString( res, display, type, role, new Object[]{lobby, new Integer(gameMode)} ) ;
		
		return phText == null
			? null
			: replacePlaceholdersWithLobby( context, res, display, type, role, gameMode, lobby, phText ) ;
	}

	
	public static String format( Context context, Resources res, int display, int type, int role, int gameMode, int countdownNumber, Lobby lobby ) {
		if ( res == null )
			res = context.getResources() ;
		
		// Load the text with placeholders for this display, type, role combo.
		String phText = loadResourceString( res, display, type, role, new Object[]{lobby, Integer.valueOf(gameMode), Integer.valueOf(countdownNumber)} ) ;
		
		if ( phText == null )
			return null ;
		
		phText = replacePlaceholdersWithLobby( context, res, display, type, role, gameMode, lobby, phText ) ;
		phText = replacePlaceholdersWithLobbyCountdown( context, res, display, type, role, gameMode, countdownNumber, lobby, phText ) ;
		return phText ;
	}
	
	
	public static String format( Context context, Resources res, int display, int type, int role, WiFiLobbyDetails lobbyDescription ) {
		if ( res == null )
			res = context.getResources() ;
		
		// Load the text with placeholders for this display, type, role combo.
		String phText = loadResourceString( res, display, type, role, new Object[]{lobbyDescription} ) ;
		
		if ( phText == null )
			return null ;
		
		phText = replacePlaceholdersWithLobbyDescription( context, res, display, type, role, lobbyDescription, phText ) ;
		return phText ;
	}
	
	public static String format( Context context, Resources res, int display, int type, int role, Lobby lobby ) {
		if ( res == null )
			res = context.getResources() ;
		
		// Load the text with placeholders for this display, type, role combo.
		String phText = loadResourceString( res, display, type, role, new Object[]{lobby} ) ;
		
		if ( phText == null )
			return null ;
		
		phText = replacePlaceholdersWithLobby( context, res, lobby, phText ) ;
		return phText ;
	}
	
	
	
	
	public static String format( Context context, Resources res, int display, int type, int role, Key key ) {
		if ( res == null )
			res = context.getResources() ;
		
		// Load the text with placeholders for this display, type, role combo.
		String phText = loadResourceString( res, display, type, role, new Object[]{key} ) ;
		
		if ( phText == null )
			return null ;
		
		phText = replacePlaceholdersWithKey( context, res, display, type, role, key, phText ) ;
		return phText ;
	}
	
	
	/**
	 * Loads the specified resource string.
	 * 
	 * @param res
	 * @param display
	 * @param type
	 * @param role
	 * @return
	 */
	protected static String loadResourceString( Resources res, int display, int type, int role ) {
		return loadResourceString( res, display, type, role, null ) ;
	}
	
	protected static String loadResourceString( Resources res, int display, int type, int role, Object referenceObject ) {
		return loadResourceString( res, display, type, role, new Object[]{referenceObject} ) ;
	}
	
	/**
	 * Loads the specified resource string.  Basically a bunch of nested switch
	 * statements to locate a specific resource string ID.
	 * 
	 * A reference object might be needed when the display, type and role do
	 * not completely define a resource string.  For example, with the type
	 * TYPE_MENU_CHALLENGE_DESCRIPTION, the description is partially based on
	 * the status of the challenge itself, so the Challenge is provided as a
	 * reference object.
	 * 
	 * @param res
	 * @param display
	 * @param type
	 * @param role
	 * @param referenceObject
	 * @return
	 */
	protected static String loadResourceString( Resources res, int display, int type, int role, Object [] referenceObjects ) {
		
		// Reference objects.
		String string = null ;
		Long longValue = null ;
		Integer integerValue = null ;
		Integer integerValue2 = null ;
		Lobby lobby = null ;
		WiFiLobbyDetails wifiLobbyDetails = null ; 
		GameResult gameResult = null ;
		if ( referenceObjects != null && referenceObjects[0] != null && referenceObjects[0] instanceof GameResult )
			gameResult = (GameResult)referenceObjects[0] ;
		if ( referenceObjects != null && referenceObjects[0] != null && referenceObjects[0] instanceof String )
			string = (String)referenceObjects[0] ;
		if ( referenceObjects != null && referenceObjects[0] != null && referenceObjects[0] instanceof Long )
			longValue = (Long)referenceObjects[0] ;
		if ( referenceObjects != null && referenceObjects[0] != null && referenceObjects[0] instanceof Lobby ) {
			lobby = (Lobby)referenceObjects[0] ;
		}
		if ( referenceObjects != null && referenceObjects.length >= 2 && referenceObjects[1] instanceof Integer ) {
			integerValue = (Integer)referenceObjects[1] ;
		}
		if ( referenceObjects != null && referenceObjects.length >= 3 && referenceObjects[2] instanceof Integer ) {
			integerValue2 = (Integer)referenceObjects[2] ;
		}
		if ( referenceObjects != null && referenceObjects[0] != null && referenceObjects[0] instanceof WiFiLobbyDetails ) {
			wifiLobbyDetails = (WiFiLobbyDetails)referenceObjects[0] ;
		}
		
		
		int intValue ;
		
		boolean didVoteFor ;
		int countdownStatus ;
		
		int [] countdowns ;
		
		boolean internet = false, open = false, isPublic = true, isItinerant = false ;
		boolean full = false, closed = false ;
		boolean statusReceived = false ;
		if ( wifiLobbyDetails != null || lobby != null ) {
			internet = lobby != null && lobby instanceof InternetLobby ;
			open = ( wifiLobbyDetails != null && wifiLobbyDetails.hasReceivedStatus() && wifiLobbyDetails.getReceivedStatus().getNumPeople() < wifiLobbyDetails.getReceivedStatus().getMaxPeople() )
					|| ( lobby != null && lobby.getNumPeople() < lobby.getMaxPeople() ) ;
			if ( internet ) {
				InternetLobby il = (InternetLobby) lobby ;
				isPublic = il.isPublic() ;
				isItinerant = il.isItinerant() ;
				closed = il.getStatus() == WebConsts.STATUS_CLOSED || il.getStatus() == WebConsts.STATUS_REMOVED ;
				if ( !closed ) {
					open = il.getHostedPlayers() < il.getMaxPeople() ;
					full = !open ;
				} else {
					open = full = false ;
				}
			}
			if ( wifiLobbyDetails != null ) {
				statusReceived = wifiLobbyDetails.hasReceivedStatus() ;
			}
		}
		
		if ( display == TextFormatting.DISPLAY_MENU ) {
			switch( type ) {
			// Game launch buttons
			case TYPE_MENU_SINGLE_PLAYER_FIRST_GAME_TITLE:
				return res.getString( R.string.menu_sp_first_game_title ) ;
			case TYPE_MENU_SINGLE_PLAYER_FIRST_GAME_DESCRIPTION:
				return res.getString( R.string.menu_sp_first_game_description) ;
			case TYPE_MENU_SINGLE_PLAYER_RESUME_GAME_TITLE:
				return res.getString( R.string.menu_sp_resume_game_title ) ;
			case TYPE_MENU_SINGLE_PLAYER_RESUME_GAME_DESCRIPTION:
				switch( GameModes.measurePerformanceBy(gameResult.getGameInformation(0).mode) ) {
				case GameModes.MEASURE_PERFORMANCE_BY_SCORE:
					return res.getString( R.string.menu_sp_resume_game_description) ;
				case GameModes.MEASURE_PERFORMANCE_BY_TIME:
					return res.getString(R.string.menu_sp_resume_game_measure_time_description) ;
				}
				return res.getString( R.string.menu_sp_resume_game_description) ;
			case TYPE_MENU_SINGLE_PLAYER_RESUME_GAME_LONG_DESCRIPTION:
				switch( GameModes.measurePerformanceBy(gameResult.getGameInformation(0).mode) ) {
				case GameModes.MEASURE_PERFORMANCE_BY_SCORE:
					return res.getString( R.string.menu_sp_resume_game_long_description) ;
				case GameModes.MEASURE_PERFORMANCE_BY_TIME:
					return res.getString(R.string.menu_sp_resume_game_measure_time_long_description) ;
				}
				return res.getString( R.string.menu_sp_resume_game_long_description) ;
			case TYPE_MENU_SINGLE_PLAYER_NEW_GAME_TITLE:
				return res.getString( R.string.menu_sp_new_game_title ) ;
			case TYPE_MENU_SINGLE_PLAYER_NEW_GAME_DESCRIPTION:
				return res.getString( R.string.menu_sp_new_game_description) ;
			case TYPE_MENU_SINGLE_PLAYER_NEW_GAME_MODE_TITLE:
				return res.getString( R.string.menu_sp_new_game_mode_title ) ;
			case TYPE_MENU_SINGLE_PLAYER_NEW_GAME_MODE_DESCRIPTION:
				return res.getString( R.string.menu_sp_new_game_mode_description) ;
			case TYPE_MENU_SINGLE_PLAYER_EDIT_GAME_MODE_TITLE:
				return res.getString( R.string.menu_sp_edit_game_mode_title ) ;
			case TYPE_MENU_SINGLE_PLAYER_EDIT_GAME_MODE_DESCRIPTION:
				return res.getString( R.string.menu_sp_edit_game_mode_description) ;
			
			
				
			// Game vote buttons!
			case TYPE_MENU_MULTI_PLAYER_VOTE_GAME_TITLE:
			case TYPE_MENU_MULTI_PLAYER_VOTE_GAME_DESCRIPTION:
				// get whether we have voted for already, whether a launch
				// is in progress, etc.
				intValue = integerValue.intValue() ;
				didVoteFor = lobby.getLocalVote(intValue) ;
				// determine whether the local player is in a countdown for this game mode.
				countdowns = lobby.getCountdownsForGameMode(intValue) ;
				countdownStatus = Lobby.COUNTDOWN_STATUS_NONE ;
				if ( countdowns.length > 0 ) {
					for ( int i = 0; i < countdowns.length; i++ ) {
						if ( countdowns[i] == lobby.getPlayerCountdownNumber(lobby.getLocalPlayerSlot()) ) {
							if ( countdownStatus != Lobby.COUNTDOWN_STATUS_ACTIVE )
								countdownStatus = lobby.getCountdownStatus(countdowns[i]) ;
						}
					}
				}
				// select one.
				if ( type == TYPE_MENU_MULTI_PLAYER_VOTE_GAME_TITLE ) {
					if ( !didVoteFor && countdownStatus == Lobby.COUNTDOWN_STATUS_NONE )
						return res.getString( R.string.menu_mp_game_vote_for_title ) ;
					if ( didVoteFor && countdownStatus == Lobby.COUNTDOWN_STATUS_NONE )
						return res.getString( R.string.menu_mp_game_vote_against_title ) ;
					if ( !didVoteFor && countdownStatus == Lobby.COUNTDOWN_STATUS_ACTIVE )
						return res.getString( R.string.menu_mp_game_launching_without_title ) ;
					if ( didVoteFor && countdownStatus == Lobby.COUNTDOWN_STATUS_ACTIVE )
						return res.getString( R.string.menu_mp_game_launching_with_title ) ;
					if ( !didVoteFor && countdownStatus == Lobby.COUNTDOWN_STATUS_HALTED )
						return res.getString( R.string.menu_mp_game_halted_without_title ) ;
					if ( didVoteFor && countdownStatus == Lobby.COUNTDOWN_STATUS_HALTED )
						return res.getString( R.string.menu_mp_game_halted_with_title ) ;
				}
				else if ( type == TYPE_MENU_MULTI_PLAYER_VOTE_GAME_DESCRIPTION ) {
					if ( !didVoteFor && countdownStatus == Lobby.COUNTDOWN_STATUS_NONE )
						return res.getString( R.string.menu_mp_game_vote_for_description ) ;
					if ( didVoteFor && countdownStatus == Lobby.COUNTDOWN_STATUS_NONE )
						return res.getString( R.string.menu_mp_game_vote_against_description ) ;
					if ( !didVoteFor && countdownStatus == Lobby.COUNTDOWN_STATUS_ACTIVE )
						return res.getString( R.string.menu_mp_game_launching_without_description ) ;
					if ( didVoteFor && countdownStatus == Lobby.COUNTDOWN_STATUS_ACTIVE )
						return res.getString( R.string.menu_mp_game_launching_with_description ) ;
					if ( !didVoteFor && countdownStatus == Lobby.COUNTDOWN_STATUS_HALTED )
						return res.getString( R.string.menu_mp_game_halted_without_description ) ;
					if ( didVoteFor && countdownStatus == Lobby.COUNTDOWN_STATUS_HALTED )
						return res.getString( R.string.menu_mp_game_halted_with_description ) ;
				}
				break ;

				
				
			// NEW LOBBY
			case TYPE_NEW_LOBBY_ADDRESS:
				// address provided as a String, stored in 'string'
				if ( string == null )
					return res.getString(R.string.new_lobby_address_no_wifi) ;
				else
					return res.getString(R.string.new_lobby_address) ;
			
			// LOBBIES!
			case TYPE_LOBBY_NAME:
				Log.e(TAG, "The type TYPE_LOBBY_NAME is not yet supported.") ;
				return null ;
			case TYPE_LOBBY_INTERNET_DESCRIPTION:
				if ( lobby instanceof InternetLobby && ((InternetLobby)lobby).isPublic() )
					return res.getString( R.string.menu_lobby_public_internet_description ) ;
				return res.getString( R.string.menu_lobby_private_internet_description ) ;
			case TYPE_LOBBY_WIFI_DESCRIPTION:
				return res.getString( R.string.menu_lobby_wifi_description ) ;
			case TYPE_LOBBY_INTERNET_DESCRIPTION_SHORT:
				if ( lobby instanceof InternetLobby && ((InternetLobby)lobby).isPublic() )
					return res.getString( R.string.menu_lobby_public_internet_short_description ) ;
				return res.getString( R.string.menu_lobby_private_internet_short_description ) ;
			case TYPE_LOBBY_WIFI_DESCRIPTION_SHORT:
				return res.getString( R.string.menu_lobby_wifi_short_description ) ;
			case TYPE_LOBBY_AVAILABILITY_DESCRIPTION:
				if ( lobby instanceof InternetLobby ) {
					if ( ((InternetLobby)lobby).isPublic() )
						return res.getString( R.string.menu_lobby_public_internet_availability_description) ;
					else if ( ((InternetLobby)lobby).isItinerant() )
						return res.getString( R.string.menu_lobby_itinerant_internet_availability_description) ;
					else
						return res.getString( R.string.menu_lobby_private_internet_availability_description) ;
				}
				return res.getString( R.string.menu_lobby_wifi_availability_description) ;
			case TYPE_LOBBY_INVITATION_DESCRIPTION_BEAM_ENABLED:
				if ( lobby instanceof InternetLobby ) {
					if ( ((InternetLobby)lobby).isPublic() )
						return res.getString( R.string.menu_lobby_public_internet_invitation_description_beam_enabled) ;
					else if ( ((InternetLobby)lobby).isItinerant() )
						return res.getString( R.string.menu_lobby_itinerant_internet_invitation_description_beam_enabled) ;
					else
						return res.getString( R.string.menu_lobby_private_internet_invitation_description_beam_enabled) ;
				}
				return res.getString( R.string.menu_lobby_wifi_invitation_description_beam_enabled) ;
			case TYPE_LOBBY_INVITATION_DESCRIPTION_BEAM_DISABLED:
				if ( lobby instanceof InternetLobby ) {
					if ( ((InternetLobby)lobby).isPublic() )
						return res.getString( R.string.menu_lobby_public_internet_invitation_description_beam_disabled) ;
					else if ( ((InternetLobby)lobby).isItinerant() )
						return res.getString( R.string.menu_lobby_itinerant_internet_invitation_description_beam_disabled) ;
					else
						return res.getString( R.string.menu_lobby_private_internet_invitation_description_beam_disabled) ;
				}
				return res.getString( R.string.menu_lobby_wifi_invitation_description_beam_disabled) ;
			case TYPE_LOBBY_DETAILS:
				if ( lobby instanceof InternetLobby ) {
					if ( ((InternetLobby)lobby).isPublic() )
						return res.getString( R.string.menu_lobby_public_internet_details) ;
					else if ( ((InternetLobby)lobby).isItinerant() )
						return res.getString( R.string.menu_lobby_itinerant_internet_details) ;
					else
						return res.getString( R.string.menu_lobby_private_internet_details) ;
				}
				return res.getString( R.string.menu_lobby_wifi_details) ;
			case TYPE_LOBBY_LAUNCH_STATUS:
				// Get a very general sense of launch status.
				int status = lobby.getCountdownStatus(integerValue2.intValue()) ;
				if ( status == Lobby.COUNTDOWN_STATUS_UNUSED || status == Lobby.COUNTDOWN_STATUS_NONE )
					return res.getString( R.string.menu_lobby_launch_status_none ) ;
				else if ( status == Lobby.COUNTDOWN_STATUS_HALTED )
					return res.getString( R.string.menu_lobby_launch_status_halted ) ;
				else
					return res.getString( R.string.menu_lobby_launch_status_active ) ;
			case TYPE_LOBBY_PERSONAL_LAUNCH_STATUS:
				if ( integerValue.intValue() < 0 )
					return res.getString(R.string.menu_lobby_personal_launch_status_no_game) ;
				status = integerValue2.intValue() < 0 ? Lobby.COUNTDOWN_STATUS_NONE : lobby.getCountdownStatus(integerValue2.intValue()) ;
				if ( status == Lobby.COUNTDOWN_STATUS_NONE || status == Lobby.COUNTDOWN_STATUS_UNUSED )
					return res.getString(R.string.menu_lobby_personal_launch_status_no_launch) ;
				else if ( status == Lobby.COUNTDOWN_STATUS_HALTED )
					return res.getString( R.string.menu_lobby_personal_launch_status_halted ) ;
				else
					return res.getString( R.string.menu_lobby_personal_launch_status_active ) ;
				
				
			// LOBBY MANAGER!
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_TITLE:
				if ( wifiLobbyDetails == null && lobby == null )
					return res.getString(R.string.menu_lobby_manager_button_new_title) ;
				else if ( !internet ) {
					return res.getString(R.string.menu_lobby_manager_button_main_title) ;
				} else {
					if ( isPublic )
						return res.getString( R.string.menu_lobby_manager_button_main_internet_public_title ) ;
					else if ( isItinerant )
						return res.getString( R.string.menu_lobby_manager_button_main_internet_itinerant_title ) ;
					else
						return res.getString( R.string.menu_lobby_manager_button_main_internet_private_title ) ;
				}
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_TITLE_SEARCHING:
				if ( !statusReceived )
					return res.getString(R.string.menu_lobby_manager_button_main_title_searching_initial) ;
				else
					return res.getString(R.string.menu_lobby_manager_button_main_title) ;
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_DESCRIPTION:
				if ( wifiLobbyDetails == null && lobby == null )
					return res.getString(R.string.menu_lobby_manager_button_new_description) ;
				else if ( !internet ) {
					return res.getString(R.string.menu_lobby_manager_button_main_description) ;
				} else {
					if ( open ) {
						if ( isPublic )
							return res.getString( R.string.menu_lobby_manager_button_main_internet_public_open_description ) ;
						else if ( isItinerant )
							return res.getString( R.string.menu_lobby_manager_button_main_internet_itinerant_open_description ) ;
						else
							return res.getString( R.string.menu_lobby_manager_button_main_internet_private_open_description ) ;
					} else if ( full ) {
						if ( isPublic )
							return res.getString( R.string.menu_lobby_manager_button_main_internet_public_full_description ) ;
						else if ( isItinerant )
							return res.getString( R.string.menu_lobby_manager_button_main_internet_itinerant_full_description ) ;
						else
							return res.getString( R.string.menu_lobby_manager_button_main_internet_private_full_description ) ;
					} else {
						// closed
						if ( isPublic )
							return res.getString( R.string.menu_lobby_manager_button_main_internet_public_closed_description ) ;
						else if ( isItinerant )
							return res.getString( R.string.menu_lobby_manager_button_main_internet_itinerant_closed_description ) ;
						else
							return res.getString( R.string.menu_lobby_manager_button_main_internet_private_closed_description ) ;
					}
				}
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_DESCRIPTION_SEARCHING:
				if ( !statusReceived )
					return res.getString(R.string.menu_lobby_manager_button_main_description_searching_initial) ;
				else
					return res.getString(R.string.menu_lobby_manager_button_main_description_searching_subsequent) ;
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_DESCRIPTION_LONG:
				if ( wifiLobbyDetails == null && lobby == null )
					return res.getString(R.string.menu_lobby_manager_button_new_description) ;
				else if ( !internet ) {
					return res.getString(R.string.menu_lobby_manager_button_main_description_long) ;
				} else {
					if ( open ) {
						if ( isPublic )
							return res.getString( R.string.menu_lobby_manager_button_main_internet_public_open_description_long ) ;
						else if ( isItinerant )
							return res.getString( R.string.menu_lobby_manager_button_main_internet_itinerant_open_description_long ) ;
						else
							return res.getString( R.string.menu_lobby_manager_button_main_internet_private_open_description_long ) ;
					} else if ( full ) {
						if ( isPublic )
							return res.getString( R.string.menu_lobby_manager_button_main_internet_public_full_description_long ) ;
						else if ( isItinerant )
							return res.getString( R.string.menu_lobby_manager_button_main_internet_itinerant_full_description_long ) ;
						else
							return res.getString( R.string.menu_lobby_manager_button_main_internet_private_full_description_long ) ;
					} else {
						// closed
						if ( isPublic )
							return res.getString( R.string.menu_lobby_manager_button_main_internet_public_closed_description_long ) ;
						else if ( isItinerant )
							return res.getString( R.string.menu_lobby_manager_button_main_internet_itinerant_closed_description_long ) ;
						else
							return res.getString( R.string.menu_lobby_manager_button_main_internet_private_closed_description_long ) ;
					}
				}
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_DESCRIPTION_LONG_SEARCHING:
				if ( !statusReceived )
					return res.getString(R.string.menu_lobby_manager_button_main_description_long_searching_initial) ;
				else
					return res.getString(R.string.menu_lobby_manager_button_main_description_long_searching_subsequent) ;
			case TYPE_MENU_LOBBY_MANAGER_HIDE_BUTTON_TITLE:
				if ( wifiLobbyDetails == null && lobby == null )
					return res.getString(R.string.menu_lobby_manager_button_new_title) ;
				else if ( !internet ) {
					res.getString(R.string.menu_lobby_manager_button_hide_title) ;
				} else {
					if ( isPublic )
						return res.getString( R.string.menu_lobby_manager_button_hide_internet_public_title ) ;
					else if ( isItinerant )
						return res.getString( R.string.menu_lobby_manager_button_hide_internet_itinerant_title ) ;
					else
						return res.getString( R.string.menu_lobby_manager_button_hide_internet_private_title ) ;
				}
			case TYPE_MENU_LOBBY_MANAGER_HIDE_BUTTON_DESCRIPTION:
				if ( wifiLobbyDetails == null && lobby == null )
					return res.getString(R.string.menu_lobby_manager_button_new_description) ;
				else if ( !internet ) {
					return res.getString(R.string.menu_lobby_manager_button_hide_description) ;
				} else {
					if ( isPublic )
						return res.getString( R.string.menu_lobby_manager_button_hide_internet_public_description ) ;
					else if ( isItinerant )
						return res.getString( R.string.menu_lobby_manager_button_hide_internet_itinerant_description ) ;
					else
						return res.getString( R.string.menu_lobby_manager_button_hide_internet_private_description ) ;
				}
			
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_POPULATION:
				if ( closed && isItinerant )
					return res.getString(R.string.menu_lobby_manager_button_collage_membership_closed_itinerant) ;
				else if ( closed )
					return res.getString(R.string.menu_lobby_manager_button_collage_membership_closed_forever) ;
				else if ( full )
					return res.getString(R.string.menu_lobby_manager_button_collage_membership_full ) ;
				else
					return res.getString(R.string.menu_lobby_manager_button_collage_membership_open) ;
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_CREATION_TIME:
				if ( closed && isItinerant )
					return res.getString(R.string.menu_lobby_manager_button_collage_creation_closed_itinerant) ;
				else if ( closed )
					return res.getString(R.string.menu_lobby_manager_button_collage_creation_closed_forever) ;
				else if ( internet )
					return res.getString(R.string.menu_lobby_manager_button_collage_creation_open_internet) ;
				else
					return res.getString(R.string.menu_lobby_manager_button_collage_creation_open_local) ;
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_HOST:
				if ( closed && isItinerant )
					return res.getString(R.string.menu_lobby_manager_button_collage_host_closed_itinerant) ;
				else if ( closed )
					return res.getString(R.string.menu_lobby_manager_button_collage_host_closed_forever) ;
				else
					return res.getString(R.string.menu_lobby_manager_button_collage_host_open) ;
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_ADDRESS_OR_TYPE:
				if ( isItinerant )
					return res.getString(R.string.menu_lobby_manager_button_collage_type_itinerant) ;
				else if ( internet && isPublic )
					return res.getString(R.string.menu_lobby_manager_button_collage_type_public) ;
				else if ( internet )
					return res.getString(R.string.menu_lobby_manager_button_collage_type_private) ;
				else
					return res.getString(R.string.menu_lobby_manager_button_collage_address_local) ;
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_SESSION_ID:
				return res.getString(R.string.menu_lobby_manager_button_collage_session_id) ;
			
				
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_POPULATION_SEARCHING:
				if ( !statusReceived )
					return res.getString(R.string.menu_lobby_manager_button_collage_membership_searching_initial) ;
				else
					return res.getString(R.string.menu_lobby_manager_button_collage_membership_searching_subsequent) ;
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_CREATION_TIME_SEARCHING:
				if ( !statusReceived )
					return res.getString(R.string.menu_lobby_manager_button_collage_creation_searching_initial) ;
				else
					return res.getString(R.string.menu_lobby_manager_button_collage_creation_searching_subsequent) ;
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_HOST_SEARCHING:
				if ( !statusReceived )
					return res.getString(R.string.menu_lobby_manager_button_collage_host_searching_initial) ;
				else
					return res.getString(R.string.menu_lobby_manager_button_collage_host_open) ;
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_ADDRESS_OR_TYPE_SEARCHING:
				return res.getString(R.string.menu_lobby_manager_button_collage_address_local_searching) ;
			case TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_SESSION_ID_SEARCHING:
				return res.getString(R.string.menu_lobby_manager_button_collage_session_id) ;
				
			// EXAMINE GAME RESULT ACTIVITY
			case TYPE_EXAMINE_GAME_RESULT_TITLE_SAVED_GAME:
				return res.getString(R.string.examine_game_result_title_saved_game) ;
			case TYPE_EXAMINE_GAME_RESULT_TITLE_SP_GAME_OVER:
				return res.getString(R.string.examine_game_result_title_single_player_game_over) ;
			case TYPE_EXAMINE_GAME_RESULT_TITLE_SP_LEVEL_OVER:
				return res.getString(R.string.examine_game_result_title_single_player_level_over) ;
			case TYPE_EXAMINE_GAME_RESULT_TITLE_MP_GAME_OVER:
				if ( gameResult == null )
					return null ;
				else if ( gameResult.getWon( gameResult.getLocalPlayerSlot() ) )
					return res.getString( R.string.examine_game_result_title_multi_player_game_over_player_won ) ;
				else if ( gameResult.getLost( gameResult.getLocalPlayerSlot() ) )
					return res.getString( R.string.examine_game_result_title_multi_player_game_over_player_lost ) ;
				else if ( gameResult.getQuit( gameResult.getLocalPlayerSlot() ) )
					return null ;
				else 
					return null ;
			case TYPE_EXAMINE_GAME_RESULT_WON_LOST:
				if ( gameResult == null )
					return null ;
				else if ( gameResult.getWon( integerValue ) )
					return res.getString( R.string.examine_game_result_winner ) ;
				else if ( gameResult.getLost( integerValue ) )
					return res.getString( R.string.examine_game_result_loser ) ;
				else if ( gameResult.getQuit( integerValue ) )
					return res.getString( R.string.examine_game_result_quit ) ;
				else
					return null ;
			case TYPE_EXAMINE_GAME_RESULT_PLAYER_NAME:
				return res.getString(R.string.examine_game_result_player_name) ;
			case TYPE_EXAMINE_GAME_RESULT_LEVEL_FULL:
				return res.getString(R.string.examine_game_result_level_full_text) ;
			case TYPE_EXAMINE_GAME_RESULT_LEVEL_NUMBER:
				return res.getString(R.string.examine_game_result_level_number) ;
			case TYPE_EXAMINE_GAME_RESULT_SCORE_FULL:
				return res.getString(R.string.examine_game_result_score_full_text) ;
			case TYPE_EXAMINE_GAME_RESULT_SCORE_NUMBER:
				return res.getString(R.string.examine_game_result_score_number) ;
			case TYPE_EXAMINE_GAME_RESULT_TIME_FULL:
				return res.getString(R.string.examine_game_result_time_full_text) ;
			case TYPE_EXAMINE_GAME_RESULT_TIME_NUMBER:
				return res.getString(R.string.examine_game_result_time_number) ;
			case TYPE_NEW_GAME_TITLE_NEW_GAME:
				return res.getString(R.string.new_game_title_new_game) ;
			
			// Pause overlay titles
			case TYPE_GAME_PAUSE_OVERLAY_STATE_LOADING:
				return res.getString(R.string.game_interface_pause_state_loading) ;
			case TYPE_GAME_PAUSE_OVERLAY_LOCAL_STATE_PAUSED:
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_PAUSED_BY_PLAYER:
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_PAUSED_BY_OTHER:
				return res.getString(R.string.game_interface_pause_state_paused) ;
			case TYPE_GAME_PAUSE_OVERLAY_LOCAL_STATE_READY:
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_READY:
				return res.getString(R.string.game_interface_pause_state_ready) ;
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_CONNECTING:
				return res.getString(R.string.game_interface_pause_state_connecting) ;
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_NEGOTIATING:
				return res.getString(R.string.game_interface_pause_state_talking) ;
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_WAITING:
				return res.getString(R.string.game_interface_pause_state_waiting) ;
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_STARTING:
				return res.getString(R.string.game_interface_pause_state_starting) ;
			// Pause overlay descriptions
			case TYPE_GAME_PAUSE_OVERLAY_STATE_DESCRIPTION_LOADING:
				return res.getString(R.string.game_interface_pause_state_description_loading) ;
			case TYPE_GAME_PAUSE_OVERLAY_LOCAL_STATE_DESCRIPTION_PAUSED:
				return res.getString(R.string.game_interface_pause_state_description_local_paused) ;
			case TYPE_GAME_PAUSE_OVERLAY_LOCAL_STATE_DESCRIPTION_READY:
				return res.getString(R.string.game_interface_pause_state_description_local_ready) ;
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_CONNECTING:
				return res.getString( role == TextFormatting.ROLE_CLIENT
						? R.string.game_interface_pause_state_description_client_connecting
						: R.string.game_interface_pause_state_description_host_connecting ) ;
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_NEGOTIATING:
				return res.getString( role == TextFormatting.ROLE_CLIENT
						? R.string.game_interface_pause_state_description_client_talking
						: R.string.game_interface_pause_state_description_host_talking ) ;
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_WAITING:
				return res.getString( role == TextFormatting.ROLE_CLIENT
						? R.string.game_interface_pause_state_description_client_waiting
						: R.string.game_interface_pause_state_description_host_waiting ) ;
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_WAITING_TO_DROP:
				return res.getString(R.string.game_interface_pause_state_description_client_waiting_to_drop) ;
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_PAUSED_BY_PLAYER:
				return res.getString( role == TextFormatting.ROLE_CLIENT
						? R.string.game_interface_pause_state_description_client_you_paused
						: R.string.game_interface_pause_state_description_host_you_paused ) ;
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_PAUSED_BY_PLAYER_REMOTE_ONLY:
				return res.getString( role == TextFormatting.ROLE_CLIENT
						? R.string.game_interface_pause_state_description_client_you_paused_remote_only
						: R.string.game_interface_pause_state_description_host_you_paused_remote_only ) ;
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_PAUSED_BY_OTHER:
				return res.getString( role == TextFormatting.ROLE_CLIENT
						? R.string.game_interface_pause_state_description_client_other_paused
						: R.string.game_interface_pause_state_description_host_other_paused ) ;
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_READY:
				return res.getString( role == TextFormatting.ROLE_CLIENT
						? R.string.game_interface_pause_state_description_client_ready
						: R.string.game_interface_pause_state_description_host_ready ) ;
			case TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_STARTING:
				return res.getString( role == TextFormatting.ROLE_CLIENT
						? R.string.game_interface_pause_state_description_client_starting
						: R.string.game_interface_pause_state_description_host_starting ) ;
				
			case TYPE_GAME_PAUSE_OVERLAY_COLORS:
				return res.getString( R.string.game_interface_pause_colors ) ;
				
			case TYPE_GAME_PAUSE_OVERLAY_SOUND_ON:
				return res.getString( R.string.game_interface_pause_sound_on_yes) ;
			case TYPE_GAME_PAUSE_OVERLAY_SOUND_OFF:
				return res.getString( R.string.game_interface_pause_sound_on_no) ;
			case TYPE_GAME_PAUSE_OVERLAY_SOUND_OFF_BY_RINGER:
				return res.getString( R.string.game_interface_pause_sound_on_no_by_ringer) ;
			case TYPE_GAME_PAUSE_OVERLAY_MUSIC_ON:
				return res.getString( R.string.game_interface_pause_music_on_yes) ;
			case TYPE_GAME_PAUSE_OVERLAY_MUSIC_OFF:
				return res.getString( R.string.game_interface_pause_music_on_no) ;
			case TYPE_GAME_PAUSE_OVERLAY_MUSIC_OFF_BY_RINGER:
				return res.getString( R.string.game_interface_pause_music_on_no_by_ringer) ;
			}
		}
		
		else if ( display == TextFormatting.DISPLAY_DIALOG ) {
			switch( type ) {
			case TYPE_LOBBY_CONNECTING:
				return role == ROLE_CLIENT ? res.getString( R.string.in_lobby_dialog_connecting_as_client ) 
										   : res.getString( R.string.in_lobby_dialog_connecting_as_host ) ;
			case TYPE_LOBBY_NEGOTIATING:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_lobby_dialog_talking_as_client ) 
						   					: res.getString( R.string.in_lobby_dialog_talking_as_host ) ;
			case TYPE_LOBBY_LAUNCHING:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_lobby_dialog_launching_as_client ) 
	   										: res.getString( R.string.in_lobby_dialog_launching_as_host ) ;
			case TYPE_LOBBY_QUIT:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_lobby_dialog_quit_as_client_message )
											: res.getString( R.string.in_lobby_dialog_quit_as_host_message ) ;
			case TYPE_LOBBY_QUIT_YES_BUTTON:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_lobby_dialog_quit_as_client_yes_button )
											: res.getString( R.string.in_lobby_dialog_quit_as_host_yes_button ) ;
			case TYPE_LOBBY_QUIT_NO_BUTTON:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_lobby_dialog_quit_as_client_no_button )
											: res.getString( R.string.in_lobby_dialog_quit_as_host_no_button ) ;
			case TYPE_LOBBY_CLOSED_FOREVER:
				return res.getString( R.string.in_lobby_dialog_lobby_closed_forever ) ;
				
			case TYPE_LOBBY_CLOSED_FOREVER_BUTTON:
				return res.getString( R.string.in_lobby_dialog_lobby_closed_forever_quit_button ) ;
				
			case TYPE_LOBBY_NO_WIFI:
				return res.getString( R.string.in_lobby_dialog_no_wifi ) ;
			
			case TYPE_GAME_CONNECTING:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_game_dialog_connecting_to_server_as_client ) 
						   					: res.getString( R.string.in_game_dialog_connecting_to_server_as_host ) ;
			case TYPE_GAME_NEGOTIATING:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_game_dialog_talking_to_server_as_client ) 
	   										: res.getString( R.string.in_game_dialog_talking_to_server_as_host ) ;
			case TYPE_GAME_WAITING:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_game_dialog_waiting_for_players_as_client ) 
											: res.getString( R.string.in_game_dialog_waiting_for_players_as_host ) ;
			case TYPE_GAME_PAUSED:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_game_dialog_paused_by_players_as_client ) 
											: res.getString( R.string.in_game_dialog_paused_by_players_as_host ) ;
			case TYPE_GAME_READY:
				return null ;			// no "game ready" dialog just now
				
			case TYPE_GAME_OVER:
				return null ;			// no generic "game over" dialog just now
				
			case TYPE_GAME_QUIT:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_game_dialog_quit_multiplayer_as_client ) 
											: res.getString( R.string.in_game_dialog_quit_multiplayer_as_host ) ;
			case TYPE_GAME_QUIT_YES:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_game_dialog_quit_multiplayer_as_client_yes_button ) 
											: res.getString( R.string.in_game_dialog_quit_multiplayer_as_host_yes_button ) ;
			case TYPE_GAME_QUIT_NO:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_game_dialog_quit_multiplayer_as_client_no_button ) 
											: res.getString( R.string.in_game_dialog_quit_multiplayer_as_host_no_button ) ;
			case TYPE_GAME_OPPONENT_QUIT:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_game_dialog_opponent_quit_multiplayer_as_client )
											: res.getString( R.string.in_game_dialog_opponent_quit_multiplayer_as_host ) ;
			case TYPE_GAME_OPPONENT_QUIT_BUTTON :
				return res.getString( R.string.in_game_dialog_opponent_quit_multiplayer_quit_button ) ;
				
			case TYPE_GAME_NO_WIFI:
				return res.getString( R.string.in_game_dialog_no_wifi ) ;
				
				
				
			// LOBBY MANAGER DIALOGS
			// Confirmation
			case TYPE_LOBBY_CONFIRM_JOIN:
				return res.getString( R.string.lobby_confirm_dialog_join ) ;
			case TYPE_LOBBY_CONFIRM_JOIN_YES_BUTTON:
				return res.getString( R.string.lobby_confirm_dialog_join_yes_button ) ;
			case TYPE_LOBBY_CONFIRM_JOIN_NO_BUTTON:
				return res.getString( R.string.lobby_confirm_dialog_join_no_button ) ;
			case TYPE_LOBBY_CONFIRM_HIDE:
				if ( lobby instanceof InternetLobby ) {
					if ( ((InternetLobby)lobby).getOrigin() != InternetLobby.ORIGIN_PUBLIC_LIST ) {
						if ( ((InternetLobby)lobby).isItinerant() )
							return res.getString(R.string.internet_lobby_manager_confirm_dialog_remove_itinerant) ;
						return res.getString(R.string.internet_lobby_manager_confirm_dialog_remove) ;
					}
					return res.getString(R.string.internet_lobby_manager_confirm_dialog_hide) ;
				}
				return res.getString( R.string.lobby_confirm_dialog_hide ) ;
			case TYPE_LOBBY_CONFIRM_HIDE_YES_BUTTON:
				if ( lobby instanceof InternetLobby ) {
					if ( ((InternetLobby)lobby).getOrigin() != InternetLobby.ORIGIN_PUBLIC_LIST )
						return res.getString(R.string.internet_lobby_manager_confirm_dialog_remove_yes_button) ;
					return res.getString(R.string.internet_lobby_manager_confirm_dialog_hide_yes_button) ;
				}
				return res.getString( R.string.lobby_confirm_dialog_hide_yes_button ) ;
			case TYPE_LOBBY_CONFIRM_HIDE_NO_BUTTON:
				if ( lobby instanceof InternetLobby ) {
					if ( ((InternetLobby)lobby).getOrigin() != InternetLobby.ORIGIN_PUBLIC_LIST )
						return res.getString(R.string.internet_lobby_manager_confirm_dialog_remove_no_button) ;
					return res.getString(R.string.internet_lobby_manager_confirm_dialog_hide_no_button) ;
				}
				return res.getString( R.string.lobby_confirm_dialog_hide_no_button ) ;
				
			// Examine lobbies!
			case TYPE_LOBBY_EXAMINE_NAME:
				return res.getString( R.string.lobby_examine_lobby_name ) ;
			case TYPE_LOBBY_EXAMINE_POPULATION:
				return res.getString( R.string.lobby_examine_lobby_population ) ;
			case TYPE_LOBBY_EXAMINE_INFO:
				return res.getString( R.string.lobby_examine_lobby_info ) ;
			case TYPE_LOBBY_EXAMINE_QUERY:
				// Query string is dependent on whether the lobby is full.
				if ( wifiLobbyDetails.getReceivedStatus().getNumPeople() < wifiLobbyDetails.getReceivedStatus().getMaxPeople() )
					return res.getString( R.string.lobby_examine_lobby_open_query ) ;
				else
					return res.getString( R.string.lobby_examine_lobby_full_query ) ;
			case TYPE_LOBBY_EXAMINE_NO_WIFI:
				return res.getString( R.string.lobby_examine_no_wifi ) ;
			case TYPE_LOBBY_EXAMINE_JOIN_BUTTON:
				return res.getString( R.string.lobby_examine_lobby_button_join ) ;
			case TYPE_LOBBY_EXAMINE_CANCEL_BUTTON:
				return res.getString( R.string.lobby_examine_lobby_button_cancel ) ;
			case TYPE_LOBBY_MANAGER_NO_WIFI:
				return res.getString( R.string.lobby_manager_no_wifi ) ;
			case TYPE_LOBBY_MANAGER_NO_WIFI_CANCEL_BUTTON:
				return res.getString( R.string.lobby_manager_no_wifi_button_cancel ) ;
				
			case TYPE_LOBBY_EXAMINE_NAME_SEARCHING:
				if ( statusReceived )
					return res.getString( R.string.lobby_examine_lobby_name ) ;
				return res.getString( R.string.lobby_examine_lobby_name_searching_initial ) ;
			case TYPE_LOBBY_EXAMINE_POPULATION_SEARCHING:
				if ( !statusReceived )
					return res.getString( R.string.lobby_examine_lobby_population_searching_initial ) ;
				else
					return res.getString( R.string.lobby_examine_lobby_population_searching_subsequent ) ;
			case TYPE_LOBBY_EXAMINE_INFO_SEARCHING:
				if ( !statusReceived )
					return res.getString( R.string.lobby_examine_lobby_info_searching_initial ) ;
				else
					return res.getString( R.string.lobby_examine_lobby_info_searching_subsequent ) ;
			case TYPE_LOBBY_EXAMINE_QUERY_SEARCHING:
				return res.getString( R.string.lobby_examine_lobby_searching_query ) ;
			
				
				
			case TYPE_MANUAL_KEY_ACTIVATION_XL_FAILURE_WAIT:
			case TYPE_MANUAL_KEY_ACTIVATION_PROMO_FAILURE_WAIT:
				return res.getString( R.string.manual_key_activation_failed_waiting) ;
				
			case TYPE_MANUAL_KEY_ACTIVATION_XL_SUCCESS:
				return res.getString( R.string.manual_key_activation_xl_success ) ;
			case TYPE_MANUAL_KEY_ACTIVATION_PROMO_SUCCESS:
				return res.getString( R.string.manual_key_activation_promo_success ) ;
				
			}
		}
		
		else if ( display == TextFormatting.DISPLAY_NOTIFICATION ) {
			switch( type ) {
			// LOBBY
			case TYPE_LOBBY_CONNECTING:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_lobby_notification_connecting_to_server_as_client )
											: res.getString( R.string.in_lobby_notification_connecting_to_server_as_host ) ;
			case TYPE_LOBBY_NEGOTIATING:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_lobby_notification_talking_to_server_as_client )
											: res.getString( R.string.in_lobby_notification_talking_to_server_as_host ) ;
			case TYPE_LOBBY_READY:
				if ( lobby.getNumPeople() > 1 )
					return role == ROLE_CLIENT 	? res.getString( R.string.in_lobby_notification_with_in_lobby_as_client )
												: res.getString( R.string.in_lobby_notification_with_in_lobby_as_host ) ;
				else
					return role == ROLE_CLIENT 	? res.getString( R.string.in_lobby_notification_alone_in_lobby_as_client )
												: res.getString( R.string.in_lobby_notification_alone_in_lobby_as_host ) ;	
			case TYPE_LOBBY_CLOSED_FOREVER:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_lobby_notification_closed_as_client )
											: res.getString( R.string.in_lobby_notification_closed_as_host ) ;
				
			// GAMES
			case TYPE_GAME_CONNECTING:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_game_notification_connecting_to_server_as_client ) 
						   					: res.getString( R.string.in_game_notification_connecting_to_server_as_host ) ;
			case TYPE_GAME_NEGOTIATING:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_game_notification_talking_to_server_as_client ) 
	   										: res.getString( R.string.in_game_notification_talking_to_server_as_host ) ;
			case TYPE_GAME_WAITING:
			case TYPE_GAME_PAUSED:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_game_notification_game_paused_as_client ) 
											: res.getString( R.string.in_game_notification_game_paused_as_host ) ;
				
			case TYPE_GAME_READY:
				return role == ROLE_CLIENT 	? res.getString( R.string.in_game_notification_game_ready_as_client ) 
											: res.getString( R.string.in_game_notification_game_ready_as_host ) ;
			case TYPE_GAME_OVER:
				return role == ROLE_CLIENT	? res.getString( R.string.in_game_notification_game_over_as_client )
											: res.getString( R.string.in_game_notification_game_over_as_host ) ;
				
			case TYPE_GAME_QUIT:
			case TYPE_GAME_QUIT_YES:
			case TYPE_GAME_QUIT_NO:
			case TYPE_GAME_OPPONENT_QUIT:
			case TYPE_GAME_OPPONENT_QUIT_BUTTON :
				return null ;			// no support for these as a notification.
				
			
			}
		}
		
		else if ( display == TextFormatting.DISPLAY_TOAST ) {
			switch( type ) {
			case TYPE_LOBBY_LAUNCHING_ABSENT:
				return res.getString(R.string.in_lobby_toast_launching_as_absent) ;
			}
		}
		
		// If not listed, return null.
		return null ;
	}
	
	
	/**
	 * Replaces the "placeholder(s)" in phText with the appropriately formatted
	 * names.  If names is of length 0, uses the "no names" value instead.
	 * 
	 * Returns the new string with the specified formatting applied, or 'null' if
	 * the display, type, role combination is invalid.
	 * 
	 * @param res
	 * @param display
	 * @param type
	 * @param role
	 * @param names	 An array of names, which must not contain nulls, but may be zero-length.
	 * @param phText The placeholder text, which must not be null
	 * @return
	 */
    public static String replacePlaceholdersWithNames(
    		Context context, Resources res,
    		int display, int type, int role,
    		String [] names, String phText ) {
    	
    	String prefix = null ;
    	String suffix = null ;
    	String separa = null ;
    	String noname = null ;	// we check noname later for null
    	
    	if ( display == TextFormatting.DISPLAY_DIALOG ) {
    		if ( TextFormatting.RANGE_TYPE_LOBBY_MIN <= type && type <= TextFormatting.RANGE_TYPE_LOBBY_MAX  ) {
    	    	prefix = res.getString( R.string.in_lobby_dialog_player_names_prefix ) ;
    	    	suffix = res.getString( R.string.in_lobby_dialog_player_names_suffix ) ;
    	    	separa = res.getString( R.string.in_lobby_dialog_player_names_separator ) ;
    	    	noname = res.getString( R.string.in_lobby_dialog_player_names_no_names ) ;
    		}
    		else if ( TextFormatting.RANGE_TYPE_GAME_MIN <= type && type <= TextFormatting.RANGE_TYPE_GAME_MAX  ) {
    			prefix = res.getString( R.string.in_game_dialog_player_names_prefix ) ;
    	    	suffix = res.getString( R.string.in_game_dialog_player_names_suffix ) ;
    	    	separa = res.getString( R.string.in_game_dialog_player_names_separator ) ;
    	    	noname = res.getString( R.string.in_game_dialog_player_names_no_names ) ;
    		}
    	}
    	else if ( display == TextFormatting.DISPLAY_NOTIFICATION ) {
    		if ( TextFormatting.RANGE_TYPE_LOBBY_MIN <= type && type <= TextFormatting.RANGE_TYPE_LOBBY_MAX  ) {
    	    	// TODO: Nothing yet; fill these in later
    		}
    		else if ( TextFormatting.RANGE_TYPE_GAME_MIN <= type && type <= TextFormatting.RANGE_TYPE_GAME_MAX  ) {
    			prefix = res.getString( R.string.in_game_notification_player_names_prefix ) ;
    	    	suffix = res.getString( R.string.in_game_notification_player_names_suffix ) ;
    	    	separa = res.getString( R.string.in_game_notification_player_names_separator ) ;
    	    	noname = res.getString( R.string.in_game_notification_player_names_no_names ) ;
    		}
    	}
    	else if ( display == TextFormatting.DISPLAY_TOAST ) {
    		if ( TextFormatting.RANGE_TYPE_LOBBY_MIN <= type && type <= TextFormatting.RANGE_TYPE_LOBBY_MAX  ) {
    	    	prefix = res.getString( R.string.in_lobby_dialog_player_names_prefix ) ;
    	    	suffix = res.getString( R.string.in_lobby_dialog_player_names_suffix ) ;
    	    	separa = res.getString( R.string.in_lobby_dialog_player_names_separator ) ;
    	    	noname = res.getString( R.string.in_lobby_dialog_player_names_no_names ) ;
    		}
    	}
    	else if ( display == TextFormatting.DISPLAY_MENU ) {
    		// TODO: Add these to strings_menu.xml.
    		if ( TextFormatting.RANGE_TYPE_GAME_PAUSE_MIN <= type && type <= TextFormatting.RANGE_TYPE_GAME_PAUSE_MAX ) {
    			prefix = res.getString( R.string.game_interface_pause_header_player_names_prefix ) ;
    	    	suffix = res.getString( R.string.game_interface_pause_header_player_names_suffix ) ;
    	    	separa = res.getString( R.string.game_interface_pause_header_player_names_separator ) ;
    	    	noname = res.getString( R.string.game_interface_pause_header_player_names_no_names ) ;
    		}
    	}
    	
    	if ( prefix == null ) {
    		prefix = "" ;
    		suffix = "" ;
    		separa = "" ;
    		noname = "" ;
    	}
    	
    	// We have separator strings.  Format the names.
    	String formattedNameText, unformattedNameText ;
    	if ( names.length == 0 )
    		formattedNameText = unformattedNameText = noname ;
    	else {
    		formattedNameText = prefix ;
    		unformattedNameText = "" ;
    		for ( int i = 0; i < names.length; i++ ) {
				String thisName = (i < names.length-1)
						? names[i] + separa
						: names[i] ;
				formattedNameText = formattedNameText + thisName ;
				unformattedNameText = unformattedNameText + thisName ;
    		}
    		formattedNameText = formattedNameText + suffix ;
    	}
    	
    	// Find and replace occurrences of the placeholder.
    	String formattedPlaceholder = res.getString( R.string.placeholder_names_array_formatted ) ;
    	String unformattedPlaceholder = res.getString( R.string.placeholder_names_array_unformatted ) ;
    	String unformattedPlaceholder1 = res.getString( R.string.placeholder_names_array_name_1 ) ;
    	String unformattedPlaceholder2 = res.getString( R.string.placeholder_names_array_name_2 ) ;
    	
    	
    	return phText.replace(formattedPlaceholder, formattedNameText)
    				 .replace(unformattedPlaceholder, unformattedNameText)
    				 .replace(unformattedPlaceholder1, names.length >= 1 && names[0] != null ? names[0] : "?")
    				 .replace(unformattedPlaceholder2, names.length >= 2 && names[1] != null ? names[1] : "?") ;
    }
    
    
    
    
    public static String replacePlaceholdersWithGameModeInformation(
    		Context context, Resources res, int display, int type, int role, int gameMode, String phText ) {
    	
    	if ( gameMode < 0 )
    		return phText; 
    	
    	// Here are the placeholders we might expect to see.
    	String placeholder_gameModeName = res.getString( R.string.placeholder_game_mode_name ) ;
    	String placeholder_gameModeShortName = res.getString( R.string.placeholder_game_mode_short_name ) ;
    	String placeholder_gameModeShortDesc = res.getString( R.string.placeholder_game_mode_short_description ) ;
    	
    	return phText.replace(placeholder_gameModeName, GameModes.name(gameMode))
    				 .replace(placeholder_gameModeShortName, GameModes.shortName(gameMode))
		   			 .replace(placeholder_gameModeShortDesc, GameModes.shortDescription(gameMode)) ;
    	
    }
    
    
    public static String replacePlaceholdersWithGameResultInformation(
    		Context context, Resources res, int display, int type, int role, GameResult gameResult, int slot, String phText ) {
    	
    	String placeholder_gameResultLevel = res.getString( R.string.placeholder_game_result_level ) ;
    	String placeholder_gameResultLevelPrevious = res.getString( R.string.placeholder_game_result_level_previous ) ;
    	String placeholder_gameResultScore = res.getString( R.string.placeholder_game_result_score ) ;
    	String placeholder_gameResultRow = res.getString( R.string.placeholder_game_result_rows ) ;
    	String placeholder_gameResultFirstLevel = res.getString( R.string.placeholder_game_result_first_level) ;
    	String placeholder_gameResultConfig = res.getString( R.string.placeholder_game_result_garbage ) ;
    	String placeholder_gameResultTime = res.getString( R.string.placeholder_game_result_time ) ;
    	
    	String placeholder_empty = res.getString( R.string.menu_placeholder_empty ) ;
    	
    	GameInformation ginfo = null ;
    	DecimalFormat intCommas = new DecimalFormat("###,###,###,###,###");
    	if ( gameResult != null )
    		ginfo = gameResult.getGameInformationImmutable(slot) ;
    	
    	String timeString = placeholder_empty ;
    	StringBuilder sb = new StringBuilder() ;
    	if ( ginfo != null ) {
    		long minutes = ginfo.milliseconds / (60 * 1000) ;
    		long seconds = ( ginfo.milliseconds / 1000 ) % 60 ;
    		long milliseconds = ginfo.milliseconds % 1000 ;
    		sb.append(minutes).append(seconds < 10 ? ":0" : ":").append(seconds).append(".") ;
    		if ( milliseconds < 100 )
    			sb.append("0") ;
    		if ( milliseconds < 10 )
    			sb.append("0") ;
    		sb.append(milliseconds) ;
    		timeString = sb.toString() ;
    	}
    	
    	return phText.replace(placeholder_gameResultLevel, ginfo == null ? placeholder_empty : "" + ginfo.level)
    				 .replace(placeholder_gameResultLevelPrevious, ginfo == null ? placeholder_empty : "" + (ginfo.level-1) )
    				 .replace(placeholder_gameResultScore, ginfo == null ? placeholder_empty : "" + intCommas.format(ginfo.score) )
    				 .replace(placeholder_gameResultRow, ginfo == null ? placeholder_empty : "" + intCommas.format(ginfo.s0clears + ginfo.s1clears + ginfo.sLclears + ginfo.moclears) )
    				 .replace(placeholder_gameResultFirstLevel, ginfo == null ? placeholder_empty : "" + (ginfo.firstLevel) )
    				 .replace(placeholder_gameResultConfig, ginfo == null ? placeholder_empty : "" + (ginfo.firstGarbage) )
    				 .replace(placeholder_gameResultTime, timeString) ;
    }
    
    
    public static String replacePlaceholdersWithGameInformation(
    		Context context, Resources res, int display, int type, int role, int gameMode, GameResult gameResult, int slot, String phText ) {
    	
    	phText = replacePlaceholdersWithGameModeInformation( context, res, display, type, role, gameMode, phText ) ;
    	return replacePlaceholdersWithGameResultInformation( context, res, display, type, role, gameResult, slot, phText ) ;
    }
    
    
    public static String replacePlaceholdersWithLongValue( 
    		Context context, Resources res, int display, int type, int role, long longValue, String phText ) {
    	
    	// Here are the placeholders we might expect to see.
    	String placeholder_dateNear = res.getString( R.string.placeholder_duration_date_near ) ;
    	String placeholder_durationHours = res.getString( R.string.placeholder_duration_hours ) ;
    	String placeholder_durationMinutes = res.getString( R.string.placeholder_duration_minutes ) ;
    	String placeholder_durationSeconds = res.getString( R.string.placeholder_duration_seconds ) ;
    	
    	Date dateAfterMilliseconds = new Date(System.currentTimeMillis() + longValue) ;
    	
    	return phText.replace(placeholder_durationHours, "" + (longValue / (1000 * 60 * 60)))
    				 .replace(placeholder_durationMinutes, "" + ((longValue / (1000 * 60)) % 60))
    				 .replace(placeholder_durationSeconds, "" + ((longValue / (1000)) % 60))
    				 .replace(placeholder_dateNear, formatNearDate(context, res, dateAfterMilliseconds) ) ;

    
    
    
    }
    
    public static String replacePlaceholdersWithFloatValue(
    		Context context, Resources res, int display, int type, int role, float floatValue, String phText ) {
    	
    	// Placeholder for percentage
    	String placeholder_percentage = res.getString(R.string.placeholder_percentage) ;
    	
    	return phText.replace(placeholder_percentage, "" + (int)Math.round( floatValue * 100 )) ;
    }
    
    public static String replacePlaceholdersWithIntValue(
    		Context context, Resources res, int display, int type, int role, int intValue, String phText ) {
    	
    	// Placeholder for percentage
    	String placeholder_percentage = res.getString(R.string.placeholder_percentage) ;
    	
    	return phText.replace(placeholder_percentage, "" + intValue) ;
    }
    
    
    public static String replacePlaceholdersWithLobby(
    		Context context, Resources res, int display, int type, int role, int gameMode, Lobby lobby, String phText ) {
    	
    	// We need the game mode title and short description, any countdowns currently counting in progress
    	// (for now, take the one of greatest priority to this player).
    	
    	// First: apply game mode substitutions.
    	phText = replacePlaceholdersWithGameModeInformation( context, res, display, type, role, gameMode, phText ) ;
    	// Now: apply countdown information.  We actually only use duration for this, at
    	// least for now.
    	
    	if ( lobby == null )
    		return phText ;
    	
    	int [] countdowns = lobby.getCountdownsForGameMode(gameMode) ;
		int mostRelevantCountdown = -1 ;
		if ( countdowns.length > 0 ) {
			for ( int i = 0; i < countdowns.length; i++ ) {
				if ( countdowns[i] == lobby.getPlayerCountdownNumber(lobby.getLocalPlayerSlot()) ) {
					mostRelevantCountdown = countdowns[i] ;
				}
			}
			if ( mostRelevantCountdown == -1 ) {
				// player is not involved in any; take the first we find.
				mostRelevantCountdown = countdowns[0] ;
			}
		}
		
		if ( mostRelevantCountdown > -1 )
			phText = replacePlaceholdersWithLongValue( context, res, display, type, role, lobby.getCountdownDelay(mostRelevantCountdown), phText) ;
		
		return replacePlaceholdersWithLobby( context, res, lobby, phText ) ;
	}
    
    public static String replacePlaceholdersWithLobbyCountdown(
    		Context context, Resources res, int display, int type, int role, int gameMode, int countdownNumber, Lobby lobby, String phText ) {
    	
    	
    	if ( countdownNumber < 0 )
    		return phText ;
    	
    	// This method is called after replacePlaceholdersWithLobby,
    	// so we have already done game mode and lobby substitutions.
    	String placeholder_inactivePlayer = res.getString( R.string.placeholder_lobby_inactive_player_name ) ;
    	
    	// find an inactive player.
    	String name = "" ;
    	for ( int i = 0; i < lobby.getMaxPeople(); i++ ) {
    		if ( lobby.getPlayerCountdownNumber(i) == countdownNumber && lobby.getPlayerStatus(i) == Lobby.PLAYER_STATUS_INACTIVE ) {
    			name = lobby.getPlayerName(i) ;
    			break ;
    		}
    	}
    	
		return phText.replace(placeholder_inactivePlayer, name == null ? "" : name) ;
    }
    
    public static String replacePlaceholdersWithLobbyDescription( Context context, Resources res, int display, int type, int role, WiFiLobbyDetails wifiLobbyDetails, String phText ) {
    	
    	if ( wifiLobbyDetails == null )
    		return phText ;
    	
    	// Substitute other deals.
    	String placeholder_lobbyName = res.getString( R.string.placeholder_lobby_name ) ;
    	String placeholder_lobbyHostName = res.getString( R.string.placeholder_lobby_host_name ) ;
    	String placeholder_lobbyNumPeople = res.getString( R.string.placeholder_lobby_num_people ) ;
    	String placeholder_lobbyHostedPeople = res.getString( R.string.placeholder_lobby_hosted_people ) ;
    	String placeholder_lobbyMaxPeople = res.getString( R.string.placeholder_lobby_max_people ) ;
    	String placeholder_lobbyAgeDateNear = res.getString(R.string.placeholder_lobby_age_date_near) ;
    	String placeholder_lobbyAddress = res.getString(R.string.placeholder_lobby_address) ;
    	String placeholder_lobbyStatusReceived = res.getString(R.string.placeholder_lobby_status_received_date_near) ;
    	
    	String placeholder_nonce = res.getString(R.string.placeholder_nonce) ;
    	String placeholder_lobby_nonce = res.getString(R.string.placeholder_lobby_nonce) ; 
    	
    	String null_lobbyName = res.getString( R.string.placeholder_lobby_name_null ) ;
    	String null_hostName = res.getString( R.string.placeholder_lobby_host_name_null ) ;
    	
    	String address = wifiLobbyDetails.getStatus().getIPAddress() ;
    	if ( address == null )
    		address = "Address Unknown" ;
    				
    	Date dateStarted = wifiLobbyDetails.hasReceivedStatus() ? new Date(System.currentTimeMillis() - wifiLobbyDetails.getReceivedStatus().getAge()) : null ;
    	Date dateLastRefreshed = wifiLobbyDetails.hasReceivedStatus() ? new Date(wifiLobbyDetails.getReceivedStatus().getReceivedAt()) : null ;
    	
    	String lobbyName = wifiLobbyDetails.getStatus().getLobbyName() ;
    	if ( lobbyName != null )
    		lobbyName = lobbyName.trim() ;
    	
    	String hostName = wifiLobbyDetails.getStatus().getHostName() ;
    	if ( hostName != null )
    		hostName = hostName.trim() ;
    	
    	String nonce = wifiLobbyDetails.getNonce() == null
				? "Unknown"
				: wifiLobbyDetails.getNonce().toString() ;
    	
    	int numPeople = wifiLobbyDetails.hasReceivedStatus() ? wifiLobbyDetails.getReceivedStatus().getNumPeople() : 0 ;
    	int maxPeople = wifiLobbyDetails.hasReceivedStatus() ? wifiLobbyDetails.getReceivedStatus().getMaxPeople() : 0 ;
    	
    	return phText
		    	.replace(placeholder_lobby_nonce, nonce == null ? "Unknown" : nonce)
				.replace(placeholder_nonce, nonce == null ? "Unknown" : nonce)
    			.replace(placeholder_lobbyNumPeople, ""+numPeople)
    			.replace(placeholder_lobbyHostedPeople, ""+numPeople)
    			.replace(placeholder_lobbyMaxPeople, ""+maxPeople)
    			.replace(placeholder_lobbyAddress, address == null ? "" : address)
    			.replace(placeholder_lobbyAgeDateNear, formatNearDate( context, res, dateStarted ))
    			.replace(placeholder_lobbyStatusReceived, formatNearDate( context, res, dateLastRefreshed ))
    			.replace(placeholder_lobbyName, ( lobbyName == null || lobbyName.length() == 0 ) ? null_lobbyName : lobbyName)
    			.replace(placeholder_lobbyHostName, ( hostName == null || hostName.length() == 0 ) ? null_hostName : hostName) ;
    }
    
    
    public static String replacePlaceholdersWithLobby( Context context, Resources res, Lobby lobby, String phText ) {
    	
    	String placeholder_lobbyName = res.getString( R.string.placeholder_lobby_name ) ;
    	String placeholder_lobbyHostName = res.getString( R.string.placeholder_lobby_host_name ) ;
    	String placeholder_lobbyDescription = res.getString( R.string.placeholder_lobby_description ) ;
    	String placeholder_lobbyAddress = res.getString(R.string.placeholder_lobby_address) ;
    	String placeholder_lobbyHostedPeople = res.getString(R.string.placeholder_lobby_hosted_people) ;
    	String placeholder_lobbyNumPeople = res.getString(R.string.placeholder_lobby_num_people) ;
    	String placeholder_lobbyMaxPeople = res.getString(R.string.placeholder_lobby_max_people) ;
    	String placeholder_lobbyAgeDateNear = res.getString(R.string.placeholder_lobby_age_date_near) ;
    	String placeholder_lobbyInactivePlayerName = res.getString(R.string.placeholder_lobby_inactive_player_name) ;
    	String placeholder_lobbyOtherPlayersFirst = res.getString(R.string.placeholder_lobby_other_players_first) ;
    	// NOT USED String placeholder_lobbyOtherPlayersFormatted = res.getString(R.string.placeholder_lobby_inactive_player_name) ;
    	String placeholder_lobbyOtherPlayersUnformatted = res.getString(R.string.placeholder_lobby_other_players_unformatted) ;
    	
    	
    	String placeholder_nonce = res.getString(R.string.placeholder_nonce) ;
    	String placeholder_lobby_nonce = res.getString(R.string.placeholder_lobby_nonce) ; 
    	
    	String null_lobbyName = res.getString( R.string.placeholder_lobby_name_null ) ;
    	
    	
    	// We need the game mode title and short description, any countdowns currently counting in progress
    	// (for now, take the one of greatest priority to this player).
    	if ( lobby == null )
    		return phText ;
    	
		String directAddress = lobby.getDirectIPAddress() ;
		if ( directAddress == null )
			directAddress = "Address unknown" ;
    				
    	String nonce = lobby.getSessionNonce() == null
    			? "Unknown"
    			: lobby.getSessionNonce().toString() ;
    	String lobbyNonce = lobby.getLobbyNonce() == null
				? "Unknown"
				: lobby.getLobbyNonce().toString() ;

		
		// Next: apply lobby age and other generic info (not related to gameMode).
		long age = lobby.getAge() ;
		// remember, age is positive but in the past.
		Date dateStarted = new Date( System.currentTimeMillis() - age ) ;
		
		// "Other player" strings
		String defaultSep = ", " ;
		String otherPlayers = "" ;
		String firstOtherPlayer = null ;
		String firstInactivePlayer = null ;
		boolean [] players = lobby.getPlayersInLobby() ;
		for ( int i = 0; i < lobby.getMaxPeople(); i++ ) {
			if ( i != lobby.getLocalPlayerSlot() && players[i] ) {
				String name = lobby.getPlayerName(i) ;
				if ( otherPlayers.length() == 0 )
					otherPlayers = name ;
				else
					otherPlayers = otherPlayers + defaultSep + name ;
				
				if ( firstOtherPlayer == null )
					firstOtherPlayer = name ;
				
				if ( firstInactivePlayer == null && lobby.getPlayerStatus(i) == Lobby.PLAYER_STATUS_INACTIVE)
					firstInactivePlayer = name ;
			}
		}
		
		//Log.d(TAG, "directAddress: " + directAddress) ;
		//Log.d(TAG, "numPeople: " + lobby.getNumPeople()) ;
		//Log.d(TAG, "maxPeople: " + lobby.getMaxPeople()) ;
		//Log.d(TAG, "lobbyAgeDateNear: " + formatNearDate( context, res, dateStarted )) ;
		//Log.d(TAG, "lobbyNonce" + lobbyNonce) ;
		//Log.d(TAG, "nonce " + nonce) ;

		String hostName = lobby instanceof InternetLobby ? ((InternetLobby)lobby).getHostName() : "" ;
		String description = lobby instanceof InternetLobby ? ((InternetLobby)lobby).getDescription() : "" ;
		String hostedPlayers = lobby instanceof InternetLobby ? "" + ((InternetLobby)lobby).getHostedPlayers() : "" ;
		
		// First replace all non-user setable fields
		phText = phText .replace(placeholder_lobbyAddress, directAddress == null ? "" : directAddress )
						.replace(placeholder_lobbyNumPeople, "" + lobby.getNumPeople())
						.replace(placeholder_lobbyMaxPeople, "" + lobby.getMaxPeople())
						.replace(placeholder_lobbyHostedPeople, hostedPlayers)
						.replace(placeholder_lobbyAgeDateNear, formatNearDate( context, res, dateStarted ))
						.replace(placeholder_lobby_nonce, lobbyNonce == null ? "" : lobbyNonce)
						.replace(placeholder_nonce, nonce == null ? "" : nonce) ;
		
		//System.err.println("TextFormatting: replacing session nonce placeholder " + placeholder_nonce + " with " + nonce) ;
		//System.err.println("TextFormatting: replacing lobby nonce placeholder " + placeholder_lobby_nonce + " with " + lobbyNonce) ;
		
		//Log.d(TAG, "lobbyName " + lobby.getLobbyName()) ;
		//Log.d(TAG, "inactivePlayer " + firstInactivePlayer) ;
		//Log.d(TAG, "firstOtherPlayer " + firstOtherPlayer) ;
		//Log.d(TAG, "otherPlayers " + otherPlayers) ;
		
		String lobbyName = lobby.getLobbyName() ;
    	if ( lobbyName != null )
    		lobbyName = lobbyName.trim() ;
    	
		// Now replace user-configurable things, starting with lobby name.
		return phText.replace(placeholder_lobbyName, ( lobbyName == null || lobbyName.length() == 0 ) ? null_lobbyName : lobbyName)
					 .replace(placeholder_lobbyHostName, hostName == null ? "" : hostName)
					 .replace(placeholder_lobbyDescription, description == null ? "" : description)
					 .replace(placeholder_lobbyInactivePlayerName, firstInactivePlayer == null ? "" : firstInactivePlayer)
					 .replace(placeholder_lobbyOtherPlayersFirst, firstOtherPlayer == null ? "" : firstOtherPlayer)
					 .replace(placeholder_lobbyOtherPlayersUnformatted, otherPlayers == null ? "" : otherPlayers) ;
    }
    
    
    
    
    
    public static String replacePlaceholdersWithKey( Context context, Resources res, int display, int type, int role, Key key, String phText ) {
    	if ( key == null )
    		return phText ;
    	
    	// Substitute key values.
    	String placeholder_keyValue = res.getString( R.string.placeholder_key_value ) ;
    	String placeholder_keyPromo = res.getString( R.string.placeholder_key_promo ) ;
    	
    	return phText
    			.replace(placeholder_keyValue, key.getKey() != null ? key.getKey() : "null")
    			.replace(placeholder_keyPromo, key.getKeyPromo() != null ? key.getKeyPromo() : "null" ) ;
    }
    
    
    protected static CharSequence formatNearDate( Context context, Resources res, Date d ) {
    	if ( d == null )
    		return "--:--" ;
    	
    	Date now = new Date() ;
    	Calendar calendarNow = new GregorianCalendar() ;
    	calendarNow.setTime(now) ;
    	Calendar calendarDate = new GregorianCalendar() ;
    	calendarDate.setTime(d) ;
    	
    	String suffix_yesterday = res.getString(R.string.menu_formatting_suffix_yesterday) ;
    	String suffix_tomorrow = res.getString(R.string.menu_formatting_suffix_tomorrow) ;
    	
    	
    	// if today, format by the current time.  If yesterday or tomorrow,
    	// do the same but append the appropriate suffix.  Otherwise,
    	// format as a complete date.
    	int dayNow = calendarNow.get(Calendar.DATE) ;
    	int dayDate = calendarDate.get(Calendar.DATE) ;
    	if ( dayNow == dayDate )
    		return DateFormat.format( DateFormat.is24HourFormat(context) ? "kk:mm" : "h:mm aa" , d ) ;
    	else if ( dayNow - dayDate == 1 )
    		return DateFormat.format( DateFormat.is24HourFormat(context) ? "kk:mm" : "h:mm aa" , d ) + suffix_yesterday ;
    	else if ( dayNow - dayDate == -1 )
    		return DateFormat.format( DateFormat.is24HourFormat(context) ? "kk:mm" : "h:mm aa" , d ) + suffix_tomorrow ;
    	else
    		return DateFormat.format(
    				DateFormat.is24HourFormat(context) ? "EEE, d MMM yyyy kk:mm"
    												   : "EEE, d MMM yyyy h:mm aa",
    		        d) ;
    }
	
}
