package com.peaceray.quantro;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.content.Music;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.dialog.DialogManager;
import com.peaceray.quantro.premium.PremiumLibrary;
import com.peaceray.quantro.utils.Analytics;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.view.dialog.SeekBarPreference;
import com.peaceray.quantro.view.game.DrawSettings;
import com.peaceray.quantro.view.game.GameViewMemoryCapabilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.util.Log;
import android.widget.Toast;

public class QuantroPreferences extends PreferenceActivity implements
		OnPreferenceChangeListener {
	
	public static final String TAG = "QuantroPreferences";
	
	public static final String INTENT_EXTRA_SECTION = "Section" ;

	private static final float MAX_VOLUME_PERCENT = 200.0f;
	
	private static final int DIALOG_ID_PRIORITIZE_FRAMERATE_ON = 0 ;
	private static final int DIALOG_ID_PRIORITIZE_FRAMERATE_OFF = 1 ;
	

	private static final int GRAPHICS_GRAPHICAL_DETAIL_MID_ANDROID_VERSION_MINIMUM = VersionCapabilities.VERSION_2_0;
	private static final int GRAPHICS_GRAPHICAL_DETAIL_MID_HEAP_SIZE_MINIMUM = 17;

	Hashtable<String, String> mOriginalSummary;
	DialogManager mDialogManager;
	PremiumLibrary mPremiumLibrary ;
	
	Skin mPremiumOnlySkin ;
	Background mPremiumOnlyBackground ;
	
	// set in onStart. Because analyticsActive can change during this
	// Activity, we don't want to mis-apply a stopSession call.
	boolean mAnalyticsOn = false;

	boolean mSettingsChanged = false;

	// Here are some preferences.
	// Storing them this way is much faster than retrieving them using their key
	// each time
	// they are needed.
	private static int enm = 0;
	private static final int PREFERENCE_GAME_PIECE_TIPS = enm++;
	private static final int PREFERENCE_GAME_REMEMBER_SETUP = enm++;
	private static final int PREFERENCE_GAME_BACK_BUTTON = enm++;
	private static final int PREFERENCE_GAME_MULTIPLAYER_NAME = enm++;
	private static final int PREFERENCE_GAME_IN_GAME_CHAT = enm++ ;

	private static final int PREFERENCE_GRAPHICS_GRAPHICAL_DETAIL = enm++;
	private static final int PREFERENCE_GRAPHICS_SKIP_ANIMATIONS = enm++ ;
	private static final int PREFERENCE_GRAPHICS_PIECE_PREVIEW_MINIMUM_PROFILE = enm++;
	private static final int PREFERENCE_GRAPHICS_PIECE_PREVIEW_SWAP = enm++;
	private static final int PREFERENCE_GRAPHICS_ADVANCED = enm++ ;
	private static final int PREFERENCE_GRAPHICS_SCALE = enm++ ;
	private static final int PREFERENCE_GRAPHICS_PRIORITIZE_FRAMERATE = enm++ ;

	private static final int PREFERENCE_SKIN_QUANTRO_TEMPLATE 			= enm++ ;
	private static final int PREFERENCE_SKIN_QUANTRO_COLOR 				= enm++ ;
	private static final int PREFERENCE_SKIN_RETRO_TEMPLATE 			= enm++ ;
	private static final int PREFERENCE_SKIN_RETRO_COLOR 				= enm++ ;

	private static final int PREFERENCE_BACKGROUND_CURRENT_TEMPLATE = enm++;
	private static final int PREFERENCE_BACKGROUND_CURRENT_SHADE = enm++;
	private static final int PREFERENCE_BACKGROUND_SHUFFLE = enm++ ;
	private static final int PREFERENCE_BACKGROUND_MENU_USE_CURRENT = enm++ ;
	private static final int PREFERENCE_BACKGROUND_MENU_TEMPLATE = enm++;
	private static final int PREFERENCE_BACKGROUND_MENU_SHADE = enm++;

	private static final int PREFERENCE_SOUND_MUTE_WITH_RINGER = enm++;
	private static final int PREFERENCE_SOUND_PLAY = enm++;
	private static final int PREFERENCE_SOUND_VOLUME_MUSIC = enm++;
	private static final int PREFERENCE_SOUND_VOLUME_SOUND = enm++;
	private static final int PREFERENCE_SOUND_CONTROLS = enm++;
	private static final int PREFERENCE_SOUND_MUSIC_IN_MENU = enm++ ;
	private static final int PREFERENCE_SOUND_MUSIC_IN_LOBBY = enm++ ;

	private static final int PREFERENCE_CONTROLS_SCREEN = enm++;
	private static final int PREFERENCE_CONTROLS_CATEGORY_TEMPLATE = enm++;
	private static final int PREFERENCE_CONTROLS_TEMPLATE = enm++;
	private static final int PREFERENCE_CONTROLS_SHOW_BUTTONS = enm++ ;
	private static final int PREFERENCE_CONTROLS_CATEGORY_GAMEPAD = enm++;
	private static final int PREFERENCE_CONTROLS_CATEGORY_GAMEPAD_ADVANCED = enm++ ;
	private static final int PREFERENCE_CONTROLS_CATEGORY_GESTURE = enm++;
	private static final int PREFERENCE_CONTROLS_CATEGORY_GESTURE_ADVANCED = enm++ ;
	
	private static final int PREFERENCE_CONTROLS_GAMEPAD_QUICK_SLIDE = enm++ ;
	private static final int PREFERENCE_CONTROLS_GAMEPAD_DOWN = enm++;
	private static final int PREFERENCE_CONTROLS_GAMEPAD_DOWN_AUTOLOCK = enm++;
	private static final int PREFERENCE_CONTROLS_GAMEPAD_USE_ADVANCED = enm++;
	private static final int PREFERENCE_CONTROLS_GAMEPAD_DOUBLE_DOWN = enm++;
	private static final int PREFERENCE_CONTROLS_GAMEPAD_SWAP_TURN_MOVE = enm++;
	private static final int PREFERENCE_CONTROLS_GAMEPAD_CENTER_BUTTON_WIDTH = enm++;
	private static final int PREFERENCE_CONTROLS_GAMEPAD_CENTER_BUTTON_WIDTH_SCALE = enm++;
	private static final int PREFERENCE_CONTROLS_GAMEPAD_BUTTON_HEIGHT = enm++ ;
	private static final int PREFERENCE_CONTROLS_GAMEPAD_BUTTON_HEIGHT_SCALE = enm++ ;
	
	private static final int PREFERENCE_CONTROLS_GESTURE_QUICK_SLIDE = enm++ ;
	private static final int PREFERENCE_CONTROLS_GESTURE_DRAG_DOWN_AUTOLOCK = enm++;
	private static final int PREFERENCE_CONTROLS_GESTURE_TURN_BUTTONS = enm++;
	private static final int PREFERENCE_CONTROLS_GESTURE_USE_ADVANCED = enm++;
	private static final int PREFERENCE_CONTROLS_GESTURE_DRAG_EXAGGERATION = enm++ ;
	private static final int PREFERENCE_CONTROLS_GESTURE_FLING_SENSITIVITY = enm++ ;

	private static final int PREFERENCE_XL_SIZE_GAME = enm++;
	private static final int PREFERENCE_XL_SIZE_LOBBY = enm++;
	private static final int PREFERENCE_XL_SIZE_MENU = enm++;
	
	private static final int PREFERENCE_IMMERSIVE_FULL_SCREEN_GAME = enm++ ;
	private static final int PREFERENCE_IMMERSIVE_FULL_SCREEN_LOBBY = enm++ ;
	private static final int PREFERENCE_IMMERSIVE_FULL_SCREEN_MENU = enm++ ;
	
	
	private static final int PREFERENCE_NETWORK_WIFI_LOCK = enm++ ;
	
	private static final int PREFERENCE_ANALYTICS_ACTIVE = enm++;
	private static final int PREFERENCE_ANALYTICS_AGGREGATED = enm++;
	private static final int NUM_PREFERENCES = enm;

	private Preference[] mPreferences;
	
	
	public enum Section {
		/**
		 * The base-level setting section.
		 */
		ROOT,
		
		
		/**
		 * Setting the current Skin(s) and related config.
		 */
		SKIN,
		
		
		/**
		 * Setting current Background and advanced config (e.g.
		 * menu backgrounds)
		 */
		BACKGROUND,
		
		
		/**
		 * Controls for music, sound, volume, etc.
		 */
		SOUND,
		
		/**
		 * The level for setting GameControls.
		 */
		CONTROLS,
	}

	public static void launchActivity( QuantroActivity launchingActivity, Section section ) {
		Intent intent = new Intent( launchingActivity, QuantroPreferences.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	
    	if ( section != null ) {
    		intent.putExtra(INTENT_EXTRA_SECTION, section) ;
    	}
    	
    	launchingActivity.startActivity(intent) ;
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// There appears to be a crash-bug when applying screen size settings to
		// this Activity,
		// possibly because it inherits from PreferenceActivity and not
		// Activity.
		
		// Some of our preference hiding/showing and enabling/disabling
		// breaks when the orientation changes.  Prevent this from happening.
		// Force portrait layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) ;

		addPreferencesFromResource(R.xml.settings);
		PreferenceManager.setDefaultValues(QuantroPreferences.this,
				R.xml.settings, false);

		mOriginalSummary = new Hashtable<String, String>();
		mDialogManager = new DialogManager(this);
		
		mPremiumLibrary = ((QuantroApplication)getApplication()).getPremiumLibrary() ;

		// Load preferences
		Resources res = getResources();
		mPreferences = new Preference[NUM_PREFERENCES];
		mPreferences[PREFERENCE_GRAPHICS_GRAPHICAL_DETAIL] = findPreference(res.getString(R.string.setting_key_graphics_graphical_detail));
		mPreferences[PREFERENCE_GRAPHICS_SKIP_ANIMATIONS] = findPreference(res.getString(R.string.setting_key_graphics_skip_animations)) ;
		mPreferences[PREFERENCE_GRAPHICS_PIECE_PREVIEW_MINIMUM_PROFILE] = findPreference(res.getString(R.string.setting_key_graphics_rotated_preview));
		mPreferences[PREFERENCE_GRAPHICS_PIECE_PREVIEW_SWAP] = findPreference(res.getString(R.string.setting_key_graphics_swap_previews));
		mPreferences[PREFERENCE_GRAPHICS_ADVANCED] = findPreference(res.getString(R.string.setting_category_key_graphics_advanced)) ;
		mPreferences[PREFERENCE_GRAPHICS_SCALE] = findPreference(res.getString(R.string.setting_key_graphics_scale)) ;
		mPreferences[PREFERENCE_GRAPHICS_PRIORITIZE_FRAMERATE] = findPreference(res.getString(R.string.setting_key_graphics_prioritize_framerate)) ;
		
		mPreferences[PREFERENCE_SKIN_QUANTRO_TEMPLATE]			= findPreference(res.getString(R.string.setting_key_skin_quantro_template)) ;
		mPreferences[PREFERENCE_SKIN_QUANTRO_COLOR]				= findPreference(res.getString(R.string.setting_key_skin_quantro_color)) ;
		mPreferences[PREFERENCE_SKIN_RETRO_TEMPLATE]			= findPreference(res.getString(R.string.setting_key_skin_retro_template)) ;
		mPreferences[PREFERENCE_SKIN_RETRO_COLOR]				= findPreference(res.getString(R.string.setting_key_skin_retro_color)) ;

		mPreferences[PREFERENCE_BACKGROUND_CURRENT_TEMPLATE] = findPreference(res
				.getString(R.string.setting_key_background_current_template));
		mPreferences[PREFERENCE_BACKGROUND_CURRENT_SHADE] = findPreference(res
				.getString(R.string.setting_key_background_current_shade));
		mPreferences[PREFERENCE_BACKGROUND_SHUFFLE] = findPreference(res
				.getString(R.string.setting_key_background_shuffle));
		mPreferences[PREFERENCE_BACKGROUND_MENU_USE_CURRENT] = findPreference(res
				.getString(R.string.setting_key_background_menu_use_current));
		mPreferences[PREFERENCE_BACKGROUND_MENU_TEMPLATE] = findPreference(res
				.getString(R.string.setting_key_background_menu_template));
		mPreferences[PREFERENCE_BACKGROUND_MENU_SHADE] = findPreference(res
				.getString(R.string.setting_key_background_menu_shade));

		mPreferences[PREFERENCE_SOUND_MUTE_WITH_RINGER] = findPreference(res
				.getString(R.string.setting_key_sound_mute_on_ringer_silent));
		mPreferences[PREFERENCE_SOUND_PLAY] = findPreference(res
				.getString(R.string.setting_key_sound_play));
		mPreferences[PREFERENCE_SOUND_VOLUME_MUSIC] = findPreference(res
				.getString(R.string.setting_key_sound_music_volume));
		mPreferences[PREFERENCE_SOUND_VOLUME_SOUND] = findPreference(res
				.getString(R.string.setting_key_sound_sound_volume));
		mPreferences[PREFERENCE_SOUND_CONTROLS] = findPreference(res
				.getString(R.string.setting_key_sound_controls));
		mPreferences[PREFERENCE_SOUND_MUSIC_IN_MENU] = findPreference(res.getString(R.string.setting_key_sound_music_in_menu)) ;
		mPreferences[PREFERENCE_SOUND_MUSIC_IN_LOBBY] = findPreference(res.getString(R.string.setting_key_sound_music_in_lobby)) ;
		

		mPreferences[PREFERENCE_GAME_PIECE_TIPS] = findPreference(res
				.getString(R.string.setting_key_game_piece_tips));
		mPreferences[PREFERENCE_GAME_REMEMBER_SETUP] = findPreference(res
				.getString(R.string.setting_key_game_new_game_custom));
		mPreferences[PREFERENCE_GAME_BACK_BUTTON] = findPreference(res
				.getString(R.string.setting_key_game_back_button));
		mPreferences[PREFERENCE_GAME_MULTIPLAYER_NAME] = findPreference(res
				.getString(R.string.setting_key_game_multiplayer_name));
		mPreferences[PREFERENCE_GAME_IN_GAME_CHAT] = findPreference(res.
				getString(R.string.setting_key_game_in_game_chat)) ;

		mPreferences[PREFERENCE_CONTROLS_SCREEN] = findPreference(res
				.getString(R.string.setting_screen_key_controls));
		mPreferences[PREFERENCE_CONTROLS_CATEGORY_TEMPLATE] = findPreference(res
				.getString(R.string.setting_category_key_controls_style));
		mPreferences[PREFERENCE_CONTROLS_TEMPLATE] = findPreference(res
				.getString(R.string.setting_key_controls_template));
		mPreferences[PREFERENCE_CONTROLS_SHOW_BUTTONS] = findPreference(res
				.getString(R.string.setting_key_controls_show)) ;
		mPreferences[PREFERENCE_CONTROLS_CATEGORY_GAMEPAD] = findPreference(res
				.getString(R.string.setting_category_key_controls_gamepad_config));
		mPreferences[PREFERENCE_CONTROLS_CATEGORY_GAMEPAD_ADVANCED] = findPreference(res
				.getString(R.string.setting_category_key_controls_gamepad_customization)) ;
		mPreferences[PREFERENCE_CONTROLS_CATEGORY_GESTURE] = findPreference(res
				.getString(R.string.setting_category_key_controls_gesture_config));
		mPreferences[PREFERENCE_CONTROLS_CATEGORY_GESTURE_ADVANCED] = findPreference(res
				.getString(R.string.setting_category_key_controls_gesture_customization)) ;
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_QUICK_SLIDE] = findPreference(res
				.getString(R.string.setting_key_controls_gamepad_quick_slide)) ;
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_DOWN] = findPreference(res
				.getString(R.string.setting_key_controls_gamepad_down));
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_DOWN_AUTOLOCK] = findPreference(res
				.getString(R.string.setting_key_controls_gamepad_down_autolock));
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_DOUBLE_DOWN] = findPreference(res
				.getString(R.string.setting_key_controls_gamepad_double_down_drop));
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_USE_ADVANCED] = findPreference(res
				.getString(R.string.setting_key_controls_gamepad_use_custom));
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_SWAP_TURN_MOVE] = findPreference(res
				.getString(R.string.setting_key_controls_gamepad_swap_turn_move));
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_CENTER_BUTTON_WIDTH] = findPreference(res
				.getString(R.string.setting_key_controls_gamepad_center_button_width));
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_CENTER_BUTTON_WIDTH_SCALE] = findPreference(res
				.getString(R.string.setting_key_controls_gamepad_center_button_width_factor));
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_BUTTON_HEIGHT] = findPreference(res
				.getString(R.string.setting_key_controls_gamepad_custom_height));
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_BUTTON_HEIGHT_SCALE] = findPreference(res
				.getString(R.string.setting_key_controls_gamepad_custom_height_factor));
		
		mPreferences[PREFERENCE_CONTROLS_GESTURE_QUICK_SLIDE] = findPreference(res
				.getString(R.string.setting_key_controls_gesture_quick_slide)) ;
		mPreferences[PREFERENCE_CONTROLS_GESTURE_DRAG_DOWN_AUTOLOCK] = findPreference(res
				.getString(R.string.setting_key_controls_gesture_drag_down_autolock));
		mPreferences[PREFERENCE_CONTROLS_GESTURE_TURN_BUTTONS] = findPreference(res
				.getString(R.string.setting_key_controls_gesture_turn_buttons));
		mPreferences[PREFERENCE_CONTROLS_GESTURE_USE_ADVANCED] = findPreference(res
				.getString(R.string.setting_key_controls_gesture_use_custom)) ;
		mPreferences[PREFERENCE_CONTROLS_GESTURE_DRAG_EXAGGERATION] = findPreference(res
				.getString(R.string.setting_key_controls_gesture_drag_exaggeration)) ;
		mPreferences[PREFERENCE_CONTROLS_GESTURE_FLING_SENSITIVITY] = findPreference(res
				.getString(R.string.setting_key_controls_gesture_fling_sensitivity)) ;
		

		mPreferences[PREFERENCE_XL_SIZE_GAME] = findPreference(res
				.getString(R.string.setting_key_xl_fullscreen_game));
		mPreferences[PREFERENCE_XL_SIZE_LOBBY] = findPreference(res
				.getString(R.string.setting_key_xl_fullscreen_lobby));
		mPreferences[PREFERENCE_XL_SIZE_MENU] = findPreference(res
				.getString(R.string.setting_key_xl_fullscreen_menu));
		mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_GAME] = findPreference(res
				.getString(R.string.setting_key_full_screen_immersive_game));
		mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_LOBBY] = findPreference(res
				.getString(R.string.setting_key_full_screen_immersive_lobby));
		mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_MENU] = findPreference(res
				.getString(R.string.setting_key_full_screen_immersive_menu));
		
		
		mPreferences[PREFERENCE_NETWORK_WIFI_LOCK] = findPreference(res
				.getString(R.string.setting_key_network_wifi_lock)) ;

		mPreferences[PREFERENCE_ANALYTICS_ACTIVE] = findPreference(res
				.getString(R.string.setting_key_analytics_active));
		mPreferences[PREFERENCE_ANALYTICS_AGGREGATED] = findPreference(res
				.getString(R.string.setting_key_analytics_aggregated));

		// Set ourselves as the listener for everything, just in case.
		if ( mPreferences[PREFERENCE_GAME_BACK_BUTTON] != null )
			mPreferences[PREFERENCE_GAME_BACK_BUTTON].setOnPreferenceChangeListener(this) ;

		// GRAPHICS: we need to know when the graphical detail or
		// color scheme changes, so we can update the summary.
		mPreferences[PREFERENCE_GRAPHICS_GRAPHICAL_DETAIL].setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_GRAPHICS_PRIORITIZE_FRAMERATE].setOnPreferenceChangeListener(this) ;
		
		mPreferences[PREFERENCE_SKIN_QUANTRO_TEMPLATE].setOnPreferenceChangeListener(this) ;
		mPreferences[PREFERENCE_SKIN_QUANTRO_COLOR].setOnPreferenceChangeListener(this) ;
		mPreferences[PREFERENCE_SKIN_RETRO_TEMPLATE].setOnPreferenceChangeListener(this) ;
		mPreferences[PREFERENCE_SKIN_RETRO_COLOR].setOnPreferenceChangeListener(this) ;

		// BACKGROUND: we need to know when the background template or
		// shade changes, so we can update the summary.  We also need to
		// know if 'use current' changes, so we can activate / deactivate the
		// associated Preference.
		mPreferences[PREFERENCE_BACKGROUND_SHUFFLE]
		         .setOnPreferenceChangeListener(this) ;
		mPreferences[PREFERENCE_BACKGROUND_CURRENT_TEMPLATE]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_BACKGROUND_CURRENT_SHADE]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_BACKGROUND_MENU_USE_CURRENT]
		             .setOnPreferenceChangeListener(this);     
		mPreferences[PREFERENCE_BACKGROUND_MENU_TEMPLATE]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_BACKGROUND_MENU_SHADE]
				.setOnPreferenceChangeListener(this);
		
		// Shade Sliders are enabled based on their template NOT being NONE.
		// Menu Template / Shade are disabled if we use current background.
		mPreferences[PREFERENCE_BACKGROUND_CURRENT_SHADE]
				.setEnabled(getBackgroundCurrentTemplate(this) != Background.Template.NONE);
		boolean menuEnabled = !getBackgroundMenuUseCurrent(this) ;
		mPreferences[PREFERENCE_BACKGROUND_MENU_TEMPLATE]
						.setEnabled(menuEnabled);
		mPreferences[PREFERENCE_BACKGROUND_MENU_SHADE]
						.setEnabled(menuEnabled && getBackgroundMenuTemplate(this) != Background.Template.NONE);

		// SOUND: we need to know when volume changes, so we can change the
		// application's QuantroSoundPool.
		// Volume updates also require a summary refresh.
		mPreferences[PREFERENCE_SOUND_MUTE_WITH_RINGER]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_SOUND_PLAY].setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_SOUND_VOLUME_MUSIC]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_SOUND_VOLUME_SOUND]
				.setOnPreferenceChangeListener(this);
		// disable some based on muted or not
		boolean playSound = !getMuted(this);
		mPreferences[PREFERENCE_SOUND_CONTROLS].setEnabled(playSound);
		mPreferences[PREFERENCE_SOUND_VOLUME_SOUND].setEnabled(playSound);
		mPreferences[PREFERENCE_SOUND_VOLUME_MUSIC].setEnabled(playSound);
		mPreferences[PREFERENCE_SOUND_MUSIC_IN_MENU].setOnPreferenceChangeListener(this) ;
		mPreferences[PREFERENCE_SOUND_MUSIC_IN_LOBBY].setOnPreferenceChangeListener(this) ;
		

		// INTERNET: we need to know when internet multiplayer is turned on, so
		// we can activate our ChallengeRefresher. The various other components
		// should smoothly deactivate if this is turned off, but going
		// from off-on we need to start things in motion ourselves.
		// Lobby and MP names require a summary refresh, as do piece tips.
		mPreferences[PREFERENCE_GAME_MULTIPLAYER_NAME]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_GAME_PIECE_TIPS]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_GAME_IN_GAME_CHAT]
		             .setOnPreferenceChangeListener(this) ;

		// CONTROLS: we update the template and drop button summaries when
		// changed.
		// Additionally, when the template changes, we hide / reveal certain
		// components.
		mPreferences[PREFERENCE_CONTROLS_TEMPLATE]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_DOWN]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_USE_ADVANCED]
		        .setOnPreferenceChangeListener(this) ;
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_CENTER_BUTTON_WIDTH]
				.setOnPreferenceChangeListener(this) ;
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_CENTER_BUTTON_WIDTH_SCALE]
		         .setEnabled(getControlsGamepadCenterButtonWidthIgnoreUseAdvanced(this) == CENTER_BUTTON_WIDTH_CUSTOM) ;
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_BUTTON_HEIGHT]
		         .setOnPreferenceChangeListener(this) ;
		mPreferences[PREFERENCE_CONTROLS_GAMEPAD_BUTTON_HEIGHT_SCALE]
		         .setEnabled(getControlsGamepadCustomButtonHeightIgnoreUseAdvanced(this)) ;
				
		mPreferences[PREFERENCE_CONTROLS_GESTURE_USE_ADVANCED]
				 .setOnPreferenceChangeListener(this) ;

		// XL Features: these also need summary updates.
		mPreferences[PREFERENCE_XL_SIZE_GAME]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_XL_SIZE_LOBBY]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_XL_SIZE_MENU]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_GAME]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_LOBBY]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_MENU]
				.setOnPreferenceChangeListener(this);
		
				

		// ANALYTICS
		mPreferences[PREFERENCE_ANALYTICS_ACTIVE]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_ANALYTICS_AGGREGATED]
				.setOnPreferenceChangeListener(this);
		mPreferences[PREFERENCE_ANALYTICS_AGGREGATED]
				.setEnabled(QuantroPreferences.getAnalyticsActive(this));
		
		
		// Set the back button summary.
		if ( mPreferences[PREFERENCE_GAME_BACK_BUTTON] != null ) {
			int backButton = getBackButtonBehavior( this ) ;
			int backButtonValue = 0 ;
			switch ( backButton ) {
			case BACK_BUTTON_QUIT:
				backButtonValue = R.string.setting_value_key_game_back_button_quit ;
				break ;
			case BACK_BUTTON_OPTIONS:
				backButtonValue = R.string.setting_value_key_game_back_button_menu ;
				break ;
			case BACK_BUTTON_ASK:
				backButtonValue = R.string.setting_value_key_game_back_button_ask ;
				break ;
			}
			((ListPreference) mPreferences[PREFERENCE_GAME_BACK_BUTTON])
					.setValue(res.getString(backButtonValue));
		}
		

		// Now set an input filter for Player Name, to keep it under the maximum
		// length.
		((EditTextPreference) (mPreferences[PREFERENCE_GAME_MULTIPLAYER_NAME]))
				.getEditText()
				.setFilters(
						new InputFilter[] { new InputFilter.LengthFilter(
								res.getInteger(R.integer.player_maximum_name_length)) });

		// set current scale dimensions


		// special case: low-end devices only get LOW graphics.
		if (!supportsMidDetailGraphics(this)) {
			((ListPreference) mPreferences[PREFERENCE_GRAPHICS_GRAPHICAL_DETAIL])
					.setEnabled(false);
			((ListPreference) mPreferences[PREFERENCE_GRAPHICS_GRAPHICAL_DETAIL])
					.setValue(res
							.getString(R.string.setting_value_key_graphics_graphical_detail_low));
			((ListPreference) mPreferences[PREFERENCE_GRAPHICS_GRAPHICAL_DETAIL])
					.setSummary(R.string.setting_summary_graphics_graphical_detail_low_required);
		} else {
			// set the default... if necessary. We need to do this because the
			// "default" draw
			// detail may differ from device to device.
			int detail = getGraphicsGraphicalDetail(this);
			int value = 0, summary = 0;
			switch (detail) {
			case DrawSettings.DRAW_DETAIL_LOW:
				value = R.string.setting_value_key_graphics_graphical_detail_low;
				summary = R.string.setting_summary_graphics_graphical_detail_low;
				break;
			case DrawSettings.DRAW_DETAIL_MID:
				value = R.string.setting_value_key_graphics_graphical_detail_mid;
				summary = R.string.setting_summary_graphics_graphical_detail_mid;
				break;
			case DrawSettings.DRAW_DETAIL_HIGH:
				value = R.string.setting_value_key_graphics_graphical_detail_high;
				summary = R.string.setting_summary_graphics_graphical_detail_high;
				break;
			}
			((ListPreference) mPreferences[PREFERENCE_GRAPHICS_GRAPHICAL_DETAIL])
					.setValue(res.getString(value));
			((ListPreference) mPreferences[PREFERENCE_GRAPHICS_GRAPHICAL_DETAIL])
					.setSummary(summary);
		}

		// set the controls template
		if (QuantroPreferences.getControls(this) == CONTROLS_GAMEPAD) {
			ListPreference lp = ((ListPreference) mPreferences[PREFERENCE_CONTROLS_TEMPLATE]) ;
			lp.setSummary(res.getString(R.string.setting_summary_controls_template_gamepad));
			for ( int i = 0; i < lp.getEntryValues().length; i++ )
				if ( lp.getEntryValues()[i].equals( res.getString(R.string.setting_value_key_controls_template_gamepad ) ) ) {
					lp.setValueIndex(i) ;
				}
					
		} else {
			ListPreference lp = ((ListPreference) mPreferences[PREFERENCE_CONTROLS_TEMPLATE]) ;
			lp.setSummary(res.getString(R.string.setting_summary_controls_template_gesture));
			for ( int i = 0; i < lp.getEntryValues().length; i++ )
				if ( lp.getEntryValues()[i].equals( res.getString(R.string.setting_value_key_controls_template_gesture ) ) ) {
					lp.setValueIndex(i) ;
				}
		}
		
		// Set background shuffle
		if ( !supportsBackgroundShuffle(this) ) {
			CheckBoxPreference cbp = ((CheckBoxPreference) mPreferences[PREFERENCE_BACKGROUND_SHUFFLE]) ;
			cbp.setEnabled(false) ;
			cbp.setChecked(false) ;
			cbp.setSummary(R.string.setting_summary_background_shuffle_unsupported) ;
		}
		
		// We have changed the displays and keys for quantro/retro skins.  Our
		// new formulation uses legacy skins as its default value, and does not preload
		// its entry arrays.
		refreshQuantroSkinTemplateEntriesAndSetting() ;
		Skin.Template template = getSkinQuantro( this ).getTemplate() ;
		refreshQuantroSkinColorEntriesAndSetting(template) ;
		
		refreshRetroSkinTemplateEntriesAndSetting() ;
		template = getSkinRetro( this ).getTemplate() ;
		refreshRetroSkinColorEntriesAndSetting(template) ;

		// Set resolution summary
		if (((QuantroApplication) getApplication()).getGameViewMemoryCapabilities(this).getBlit() == DrawSettings.BLIT_NONE) {
			((com.robobunny.SeekBarPreference)mPreferences[PREFERENCE_GRAPHICS_SCALE]).setEnabled(false);
			mPreferences[PREFERENCE_GRAPHICS_SCALE].setSummary(R.string.setting_summary_graphics_scale_disabled);
		} else {
			int scaleDefault = ((QuantroApplication) getApplication()).getGameViewMemoryCapabilities(this).getScale();
			int scaleSettingMin = ((com.robobunny.SeekBarPreference)mPreferences[PREFERENCE_GRAPHICS_SCALE]).getMinValue();
			int scaleSettingMax = ((com.robobunny.SeekBarPreference)mPreferences[PREFERENCE_GRAPHICS_SCALE]).getMaxValue();
			int scaleRange = scaleSettingMax - scaleSettingMin;
			int scaleUnit = ((com.robobunny.SeekBarPreference)mPreferences[PREFERENCE_GRAPHICS_SCALE]).getInterval();
			int count = 1 + (scaleRange) / scaleUnit;
			String[] scaleLabels = new String[count];
			for (int i = 0; i < count; i++) {
				int scaleStep = i * scaleUnit;
				int scaleAdditive = scaleSettingMax - scaleStep;
				float percentResolution = (100.0f * scaleDefault) / (scaleDefault + scaleAdditive);
				scaleLabels[i] = res.getString(R.string.setting_label_graphics_scale, percentResolution);
			}
			((com.robobunny.SeekBarPreference)mPreferences[PREFERENCE_GRAPHICS_SCALE]).setLabels(scaleLabels);
		}
		
		// Remove the "Advanced" controls if it wouldn't do anything.
		if ( !((QuantroApplication)getApplication()).getGameViewMemoryCapabilitiesCanChangeWithPriority(this) ) {
			mPreferences[PREFERENCE_GRAPHICS_PRIORITIZE_FRAMERATE].setSummary(R.string.setting_summary_graphics_prioritize_framerate_no_effect) ;
			mPreferences[PREFERENCE_GRAPHICS_PRIORITIZE_FRAMERATE].setEnabled(false) ;
		}
		
		
		// We support one of two full-screen styles: XL or Immersive.
		// Remove the other.
		PreferenceScreen fullScreenScreen = findPreferenceScreenForPreference(
				getResources().getString(R.string.setting_key_full_screen_immersive_game), null) ;
		if ( VersionCapabilities.supportsImmersiveFullScreen() ) {
			fullScreenScreen.removePreference(mPreferences[PREFERENCE_XL_SIZE_GAME]);
			fullScreenScreen.removePreference(mPreferences[PREFERENCE_XL_SIZE_LOBBY]);
			fullScreenScreen.removePreference(mPreferences[PREFERENCE_XL_SIZE_MENU]);
			// now set the value of the remaining appropriately.
			((ListPreference)mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_GAME]).setValue(
					xlIntToFullScreenImmersiveValueKey(getScreenSizeGame(this), res)) ;
			((ListPreference)mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_LOBBY]).setValue(
					xlIntToFullScreenImmersiveValueKey(getScreenSizeLobby(this), res)) ;
			((ListPreference)mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_MENU]).setValue(
					xlIntToFullScreenImmersiveValueKey(getScreenSizeMenu(this), res)) ;
		} else {
			fullScreenScreen.removePreference(mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_GAME]);
			fullScreenScreen.removePreference(mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_LOBBY]);
			fullScreenScreen.removePreference(mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_MENU]);
		}
		
		
		// Refresh the back button summary
		refreshPreferenceSummary(mPreferences[PREFERENCE_GAME_BACK_BUTTON]);

		// hide/reveal controls PreferenceCategory according to current setting.
		refreshPreferenceSummary(mPreferences[PREFERENCE_CONTROLS_TEMPLATE]);
		refreshPreferenceSummary(mPreferences[PREFERENCE_CONTROLS_GAMEPAD_DOWN]);
		refreshPreferenceSummary(mPreferences[PREFERENCE_CONTROLS_GAMEPAD_CENTER_BUTTON_WIDTH]) ;

		// Refresh the summaries of any setting whose value is not immediately
		// apparent
		// (i.e., those which are not CheckboxPreferences).
		refreshPreferenceSummary( mPreferences[PREFERENCE_GRAPHICS_GRAPHICAL_DETAIL]);
		refreshPreferenceSummary( mPreferences[PREFERENCE_SKIN_QUANTRO_TEMPLATE] ) ;
		refreshPreferenceSummary( mPreferences[PREFERENCE_SKIN_QUANTRO_COLOR] ) ;
		refreshPreferenceSummary( mPreferences[PREFERENCE_SKIN_RETRO_TEMPLATE] ) ;
		refreshPreferenceSummary( mPreferences[PREFERENCE_SKIN_RETRO_COLOR] ) ;
		
		refreshPreferenceSummary(mPreferences[PREFERENCE_BACKGROUND_CURRENT_TEMPLATE]);
		refreshPreferenceSummary(mPreferences[PREFERENCE_BACKGROUND_CURRENT_SHADE]);
		refreshPreferenceSummary(mPreferences[PREFERENCE_BACKGROUND_MENU_TEMPLATE]);
		refreshPreferenceSummary(mPreferences[PREFERENCE_BACKGROUND_MENU_SHADE]);
		refreshPreferenceSummary(mPreferences[PREFERENCE_GAME_MULTIPLAYER_NAME]);
		refreshPreferenceSummary(mPreferences[PREFERENCE_GAME_PIECE_TIPS]);
		refreshPreferenceSummary(mPreferences[PREFERENCE_SOUND_MUSIC_IN_MENU]) ;
		refreshPreferenceSummary(mPreferences[PREFERENCE_SOUND_MUSIC_IN_LOBBY]) ;
		if ( VersionCapabilities.supportsImmersiveFullScreen() ) {
			refreshPreferenceSummary(mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_GAME]);
			refreshPreferenceSummary(mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_LOBBY]);
			refreshPreferenceSummary(mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_MENU]);
		} else {
			refreshPreferenceSummary(mPreferences[PREFERENCE_XL_SIZE_GAME]);
			refreshPreferenceSummary(mPreferences[PREFERENCE_XL_SIZE_LOBBY]);
			refreshPreferenceSummary(mPreferences[PREFERENCE_XL_SIZE_MENU]);
		}
		
		
        ////////////////////////////////////////////////////////////////////////
		// DISABLE ANDROID BEAM
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		if ( nfcAdapter != null ) {
			nfcAdapter.setNdefPushMessage(null, this) ;
		}
		
		// Everything is configured.  See if we're opening to a
		// particular section.
		if ( getIntent().hasExtra(INTENT_EXTRA_SECTION) ) {
			Section section = (Section)getIntent().getSerializableExtra(INTENT_EXTRA_SECTION) ;
			switch( section ) {
			case ROOT:
				// nothing
				break ;
				
			case SKIN:
				openPreference( res.getString(R.string.setting_screen_key_graphics) ) ;
				openPreference( res.getString(R.string.setting_screen_key_skin) ) ;
				break ;
				
			case BACKGROUND:
				openPreference( res.getString(R.string.setting_screen_key_graphics) ) ;
				openPreference( res.getString(R.string.setting_screen_key_background) ) ;
				break ;
				
			case SOUND:
				openPreference( res.getString(R.string.setting_screen_key_sound) ) ;
				break ;
				
			case CONTROLS:
				openPreference( res.getString(R.string.setting_screen_key_controls) ) ;
				break ;
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (getAnalyticsActive(this)) {
			Analytics.startSession(this);
			mAnalyticsOn = true;
		}

		mSettingsChanged = false;
	}

	@Override
	public void onResume() {
		super.onResume();
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mPremiumLibrary = ((QuantroApplication)getApplication()).getPremiumLibrary() ;
		mDialogManager.revealDialogs();
	}

	@Override
	public void onPause() {
		super.onPause();
		
		mDialogManager.hideDialogs();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mAnalyticsOn) {

			if (mSettingsChanged) {
				boolean hasAnyPremium = getPremiumLibrary(this).hasAnyPremium() ;
				Analytics.logSettingsApp(this, hasAnyPremium);
				Analytics.logSettingsGame(this, hasAnyPremium);
			}

			Analytics.stopSession(this);
		}
	}
	
	private PreferenceScreen findPreferenceScreenForPreference( String key, PreferenceScreen screen ) {
	    if( screen == null ) {
	        screen = getPreferenceScreen();
	    }

	    PreferenceScreen result = null;

	    android.widget.Adapter ada = screen.getRootAdapter();
	    for( int i = 0; i < ada.getCount(); i++ ) {
	        String prefKey = ((Preference)ada.getItem(i)).getKey();
	        if( prefKey != null && prefKey.equals( key ) ) {
	            return screen;
	        }
	        if( ada.getItem(i).getClass().equals(android.preference.PreferenceScreen.class) ) {
	            result = findPreferenceScreenForPreference( key, (PreferenceScreen) ada.getItem(i) );
	            if( result != null ) {
	                return result;
	            }
	        }
	    }

	    return null;
	}

	private void openPreference( String key ) {
	    PreferenceScreen screen = findPreferenceScreenForPreference( key, null );
	    if( screen != null ) {
	        screen.onItemClick(null, null, findPreference(key).getOrder(), 0);
	    }
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder;

		switch (id) {
		// A dialog box with choices: confirm the accept, or don't.
			
		case DIALOG_ID_PRIORITIZE_FRAMERATE_ON:
			builder = new AlertDialog.Builder(this) ;
			builder.setTitle(R.string.setting_dialog_prioritize_framerate_on_title) ;
			builder.setMessage(R.string.setting_dialog_prioritize_framerate_on_message) ;
			builder.setCancelable(true);
			builder.setPositiveButton(
					R.string.setting_dialog_prioritize_framerate_on_button_yes,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int arg1) {
							// Set the preference and restart the app.
							QuantroPreferences.setGraphicsPrioritizeFrameRate(QuantroPreferences.this, true) ;
							// Restart this application immediately!
							Intent i = getBaseContext().getPackageManager()
										.getLaunchIntentForPackage(getBaseContext().getPackageName() );
								 
							i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK );
							startActivity(i);
							dialog.cancel() ;
						}
					});
			builder.setNegativeButton(
					R.string.setting_dialog_prioritize_framerate_on_button_no,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int arg1) {
							// dismiss dialog; no change to settings.
							dialog.cancel() ;
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_PRIORITIZE_FRAMERATE_ON);
				}
			});
			return builder.create();
			
			
		case DIALOG_ID_PRIORITIZE_FRAMERATE_OFF:
			builder = new AlertDialog.Builder(this) ;
			builder.setTitle(R.string.setting_dialog_prioritize_framerate_off_title) ;
			builder.setMessage(R.string.setting_dialog_prioritize_framerate_off_message) ;
			builder.setCancelable(true);
			builder.setPositiveButton(
					R.string.setting_dialog_prioritize_framerate_off_button_yes,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int arg1) {
							// Set the preference and restart the app.
							QuantroPreferences.setGraphicsPrioritizeFrameRate(QuantroPreferences.this, false) ;
							// Restart this application immediately!
							Intent i = getBaseContext().getPackageManager()
										.getLaunchIntentForPackage(getBaseContext().getPackageName() );
								 
							i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK );
							startActivity(i);
							dialog.cancel() ;
						}
					});
			builder.setNegativeButton(
					R.string.setting_dialog_prioritize_framerate_off_button_no,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int arg1) {
							// dismiss dialog; no change to settings.
							dialog.cancel() ;
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_PRIORITIZE_FRAMERATE_ON);
				}
			});
			return builder.create();
			
		}

		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		// THIS METHOD IS DEPRECIATED
		// (and I don't care! the DialogFragment class will call through
		// to this method for compatibility)

		// nothing to prepare
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		boolean changeOK = true;
		Resources res = getResources();
		
		// ADVANCED GRAPHICS
		if ( preference == mPreferences[PREFERENCE_GRAPHICS_PRIORITIZE_FRAMERATE] ) {
			// we don't make this change.  Instead, we display a dialog.
			// The dialog confirms the change and starts a restart if
			// the user wants to go through with it.
			if ( Boolean.TRUE.equals(newValue) ) {
				mDialogManager.showDialog(DIALOG_ID_PRIORITIZE_FRAMERATE_ON) ;
			} else {
				mDialogManager.showDialog(DIALOG_ID_PRIORITIZE_FRAMERATE_OFF) ;
			}
			
			changeOK = false ;
		}
		
		// SKIN Template
		// If a skin template changes, we need to set a new collection of values 
		// for the available colors.  That means setting both the value / key arrays
		// and setting the current value in place.
		if (preference == mPreferences[PREFERENCE_SKIN_QUANTRO_TEMPLATE]) {
			// verify that this skin template is currently allowed.
			Skin.Template template = QuantroPreferences.skinTemplateValueKeyToEnum(this, (String)newValue) ;
			Skin sampleSkin = Skin.getSkins(Skin.Game.QUANTRO, template)[0] ;
			if ( mPremiumLibrary.has( sampleSkin ) ) {
				// set QUANTRO_COLOR's current key/title arrays, and set its
				// current value.  Current value is its previous value, IF appropriate
				// for 'newValue' (check Skin. methods), keep the current setting;
				// otherwise, take the first available Color.
				refreshQuantroSkinColorEntriesAndSetting( skinTemplateValueKeyToEnum(this, (String)newValue) ) ;
				
				// After this, revise its summary.
				refreshPreferenceSummary( mPreferences[PREFERENCE_SKIN_QUANTRO_COLOR] ) ;
				// the 'template' summary gets revised below.
			}
		}
		
		if (preference == mPreferences[PREFERENCE_SKIN_QUANTRO_COLOR]) {
			Skin.Template template = QuantroPreferences.getSkinQuantroTemplate(this) ;
			Skin.Color color = QuantroPreferences.skinColorValueKeyToEnum(this, (String)newValue) ;
			Skin skinSet = Skin.get(Skin.Game.QUANTRO, template, color) ;
		}
		
		if (preference == mPreferences[PREFERENCE_SKIN_RETRO_TEMPLATE]) {
			// verify that this skin template is currently allowed.
			Skin.Template template = QuantroPreferences.skinTemplateValueKeyToEnum(this, (String)newValue) ;
			Skin sampleSkin = Skin.getSkins(Skin.Game.RETRO, template)[0] ;
			if ( mPremiumLibrary.has( sampleSkin ) ) {
				// set QUANTRO_COLOR's current key/title arrays, and set its
				// current value.  Current value is its previous value, IF appropriate
				// for 'newValue' (check Skin. methods), keep the current setting;
				// otherwise, take the first available Color.
				refreshRetroSkinColorEntriesAndSetting( skinTemplateValueKeyToEnum(this, (String)newValue) ) ;
				
				// After this, revise its summary.
				refreshPreferenceSummary( mPreferences[PREFERENCE_SKIN_RETRO_COLOR] ) ;
				// the 'template' summary gets revised below.
				
			}
		}
		
		
		if (preference == mPreferences[PREFERENCE_SKIN_RETRO_COLOR]) {
			Skin.Template template = QuantroPreferences.getSkinRetroTemplate(this) ;
			Skin.Color color = QuantroPreferences.skinColorValueKeyToEnum(this, (String)newValue) ;
			Skin skinSet = Skin.get(Skin.Game.RETRO, template, color) ;
		}
		
		
		
		// Background shuffle.
		if (preference == mPreferences[PREFERENCE_BACKGROUND_SHUFFLE]) {
			if ( newValue instanceof Boolean && ((Boolean)newValue) )
				setBackgroundCurrentInShuffleIfNecessary( this ) ;
		}

		// BACKGROUND (in-game)
		if (preference == mPreferences[PREFERENCE_BACKGROUND_CURRENT_TEMPLATE]) {			
			// verify that this skin template is currently allowed.
			Background.Template template = QuantroPreferences.backgroundTemplateKeyToBackgroundTemplate(getResources(), (String)newValue) ;
			Background sampleBG = Background.get(template, Background.Shade.BLACK) ;
			if ( mPremiumLibrary.has( sampleBG ) ) {
				mPreferences[PREFERENCE_BACKGROUND_CURRENT_SHADE]
								.setEnabled(!res
										.getString(
												R.string.setting_value_key_background_template_none)
										.equals(newValue));
			}
		}
		
		// BACKGROUND (menu)
		if (preference == mPreferences[PREFERENCE_BACKGROUND_MENU_TEMPLATE]) {
			// verify that this skin template is currently allowed.
			Background.Template template = QuantroPreferences.backgroundTemplateKeyToBackgroundTemplate(getResources(), (String)newValue) ;
			Background sampleBG = Background.get(template, Background.Shade.BLACK) ;
			if ( mPremiumLibrary.has( sampleBG ) ) {
				mPreferences[PREFERENCE_BACKGROUND_MENU_SHADE]
								.setEnabled(!res
										.getString(
												R.string.setting_value_key_background_template_none)
										.equals(newValue));
			}
		}
		
		// BACKGROUND (use current in menus)
		if ( preference == mPreferences[PREFERENCE_BACKGROUND_MENU_USE_CURRENT]) {
			boolean useCurrent = ((Boolean)newValue).booleanValue() ;
			mPreferences[PREFERENCE_BACKGROUND_MENU_TEMPLATE]
							.setEnabled(!useCurrent);
			mPreferences[PREFERENCE_BACKGROUND_MENU_SHADE]
							.setEnabled(!useCurrent && getBackgroundMenuTemplate(this) != Background.Template.NONE);

		}

		// MUTE w/ Ringer.
		else if (preference == mPreferences[PREFERENCE_SOUND_MUTE_WITH_RINGER]) {
			boolean mute = (Boolean) newValue;
			if (mute)
				((QuantroApplication) getApplication()).getSoundPool(this)
						.muteWithRinger();
			else
				((QuantroApplication) getApplication()).getSoundPool(this)
						.unmuteWithRinger();
		}

		// SOUND mute
		else if (preference == mPreferences[PREFERENCE_SOUND_PLAY]) {
			boolean play = (Boolean) newValue;
			if (!play)
				((QuantroApplication) getApplication()).getSoundPool(this)
						.mute();
			else
				((QuantroApplication) getApplication()).getSoundPool(this)
						.unmute();

			// enable / disable other sound controls.
			mPreferences[PREFERENCE_SOUND_CONTROLS].setEnabled(play);
			mPreferences[PREFERENCE_SOUND_VOLUME_SOUND].setEnabled(play);
			mPreferences[PREFERENCE_SOUND_VOLUME_MUSIC].setEnabled(play);
		}

		// SOUND music volume
		else if (preference == mPreferences[PREFERENCE_SOUND_VOLUME_MUSIC]) {
			// value is an integer from 0 to 200. Scale to float.
			int vol = (Integer) newValue;
			float fVol = vol / MAX_VOLUME_PERCENT;
			((QuantroApplication) getApplication()).getSoundPool(this)
					.setMusicVolume(fVol);
		}

		// SOUND sound volume
		else if (preference == mPreferences[PREFERENCE_SOUND_VOLUME_SOUND]) {
			// value is an integer from 0 to 200. Scale to float.
			int vol = (Integer) newValue;
			float fVol = vol / MAX_VOLUME_PERCENT;
			((QuantroApplication) getApplication()).getSoundPool(this)
					.setInGameSoundVolume(fVol);
		}
		
		// CONTROLS center button width!
		else if ( preference == mPreferences[PREFERENCE_CONTROLS_GAMEPAD_CENTER_BUTTON_WIDTH] ) {
			mPreferences[PREFERENCE_CONTROLS_GAMEPAD_CENTER_BUTTON_WIDTH_SCALE]
			             .setEnabled(res
							.getString(
									R.string.setting_value_key_controls_gamepad_center_button_width_custom)
							.equals(newValue));
		}
		
		// CONTROLS button height!
		else if ( preference == mPreferences[PREFERENCE_CONTROLS_GAMEPAD_BUTTON_HEIGHT] ) {
			mPreferences[PREFERENCE_CONTROLS_GAMEPAD_BUTTON_HEIGHT_SCALE]
				         .setEnabled((Boolean)newValue) ;
		}
		

		// Analytics! If this changes, we IMMEDIATELY apply the change.
		else if (preference == mPreferences[PREFERENCE_ANALYTICS_ACTIVE]) {
			boolean value = ((Boolean) newValue).booleanValue();
			if (value && !mAnalyticsOn) {
				Analytics.startSession(this);
				mAnalyticsOn = true;
			} else if (value && mAnalyticsOn) {
				Analytics.stopSession(this);
				mAnalyticsOn = false;
			}

			// The setting also affects the enabled...ness of
			// analytics_aggregated.
			// If disabled, aggregated should be disabled too.
			mPreferences[PREFERENCE_ANALYTICS_AGGREGATED].setEnabled(value);
		}

		// Perform a refresh!
		if (changeOK) {
			String strValue = null;
			int intValue = 0;
			if (preference instanceof ListPreference) {
				strValue = null;

				CharSequence[] values = ((ListPreference) preference)
						.getEntryValues();
				for (int i = 0; i < values.length; i++) {
					if (values[i].equals(newValue)) {
						strValue = ((ListPreference) preference).getEntries()[i]
								.toString();
						intValue = i;
						break;
					}
				}
			} else if (preference instanceof EditTextPreference) {
				strValue = newValue.toString();
				try {
					intValue = Integer.parseInt(strValue);
				} catch (NumberFormatException nfe) {
				} // do nothing
			} else if (preference instanceof SeekBarPreference) {
				intValue = (Integer) newValue;
				strValue = "" + intValue;
			} else if (preference instanceof CheckBoxPreference) {
				intValue = (Boolean) newValue ? 1 : 0 ;
				strValue = intValue == 1 ? "1" : "0" ;
			}

			if (strValue != null)
				refreshPreferenceSummary(preference, strValue, intValue);
		}

		// Toast!
		// Some settings don't take effect immediately. Inform the user of them.
		if (changeOK) {
			// if we change this setting in onResume, it will happen immediately.
			// Otherwise the user needs to restart the activity.  Note that for now,
			// only games change this setting during without an Activity restart.
			if ( !VersionSafe.setupUIImmersiveOnResume() ) {
				if (preference == mPreferences[PREFERENCE_XL_SIZE_GAME] ||
						preference == mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_GAME])
					Toast.makeText(this,
							R.string.setting_toast_fullscreen_game_changed,
							Toast.LENGTH_SHORT).show();
			}
			if (preference == mPreferences[PREFERENCE_XL_SIZE_LOBBY] ||
					preference == mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_LOBBY])
				Toast.makeText(this,
						R.string.setting_toast_fullscreen_lobby_changed,
						Toast.LENGTH_SHORT).show();
			if (preference == mPreferences[PREFERENCE_XL_SIZE_MENU] ||
					preference == mPreferences[PREFERENCE_IMMERSIVE_FULL_SCREEN_MENU])
				Toast.makeText(this,
						R.string.setting_toast_fullscreen_menu_changed,
						Toast.LENGTH_SHORT).show();
			
			// Mute?
			if (preference == mPreferences[PREFERENCE_SOUND_PLAY] && !((Boolean) newValue))
				Toast.makeText(this,
						R.string.setting_toast_sound_muted,
						Toast.LENGTH_SHORT).show();
				

			// thank the user for non-aggregated analytics.
			boolean prefIsAnalyticsActive = preference == mPreferences[PREFERENCE_ANALYTICS_ACTIVE];
			boolean prefIsAnalyticsAggreg = preference == mPreferences[PREFERENCE_ANALYTICS_AGGREGATED];

			if (prefIsAnalyticsActive || prefIsAnalyticsAggreg) {
				boolean active, aggreg;
				// set these considering the new value
				active = ((prefIsAnalyticsActive && (Boolean) newValue) || (!prefIsAnalyticsActive && getAnalyticsActive(this)));
				aggreg = ((prefIsAnalyticsAggreg && (Boolean) newValue) || (!prefIsAnalyticsAggreg && getAnalyticsAggregated(this)));

				if (active && !aggreg)
					Toast.makeText(this,
							R.string.setting_toast_analytics_activated,
							Toast.LENGTH_SHORT).show();
			}
			
		}

		// should we change?
		mSettingsChanged = mSettingsChanged || changeOK;
		return changeOK;
	}

	// //////////////////////////////////////////////////////////////////////////
	//
	// PREFERENCE SUMMARY
	//
	// Some preferences have summaries which change according to their value.
	//
	// //////////////////////////////////////////////////////////////////////////

	public void refreshPreferenceSummary(Preference p) {
		if ( p == null )
			return ;
		
		String key = p.getKey();

		// our basic goal is to substitute the value of this preference - the
		// value_title, current integer value, current string value, etc. -
		// into the corresponding summary.
		String strValue = null;
		int intValue = 0;

		// List preferences are easy - int value is the index of the current
		// selection,
		// string value the current selection (as displayed to the user).
		if (p instanceof ListPreference) {
			ListPreference lp = (ListPreference) p;
			if ( lp.getEntry() == null ) {
				lp.setValueIndex(0) ;
			}
			strValue = lp.getEntry().toString();
			CharSequence[] entries = lp.getEntries();
			for (int i = 0; i < entries.length; i++) {
				if (entries[i].toString().equals(strValue))
					intValue = i;
			}
		}

		// EditText preferences are similarly easy - string value is the current
		// (trimmed) string, int value is that string parsed as an integer (if
		// possible).
		else if (p instanceof EditTextPreference) {
			EditTextPreference etp = (EditTextPreference) p;
			strValue = etp.getText().trim();
			try {
				intValue = Integer.parseInt(strValue);
			} catch (NumberFormatException nfe) {
			} // do nothing
		}

		// Seek bar preferences have integer values.
		else if (p instanceof SeekBarPreference) {
			// We can't use getProgress(); direct query only works once the
			// setting has been changed by the user. Instead, load this setting.
			Resources res = getResources();
			if (key.equals(res
					.getString(R.string.setting_key_sound_music_volume)))
				intValue = (int) (getVolumeMusic(this) * 10);
			else if (key.equals(res
					.getString(R.string.setting_key_sound_sound_volume)))
				intValue = (int) (getVolumeSound(this) * 10);
			strValue = "" + intValue;
		}
		
		// Check box preferences: 0 false, 1 true.
		else if (p instanceof CheckBoxPreference) {
			intValue = ((CheckBoxPreference)p).isChecked() ? 1 : 0 ;
			strValue = intValue == 1 ? "Yes" : "No" ;
		}

		else
			return;

		refreshPreferenceSummary(p, strValue, intValue);
	}

	public void refreshPreferenceSummary(Preference p, String strValue,
			int intValue) {
		if ( p == null )
			return ;
		
		Resources res = getResources();
		
		if (p == mPreferences[PREFERENCE_GAME_BACK_BUTTON]) {
			CharSequence value = intValue >= 0
					&& intValue < ((ListPreference) p).getEntryValues().length ? ((ListPreference) p)
					.getEntryValues()[intValue] : ((ListPreference) p)
					.getValue();
					
			String summaryTemplate = res.getString(R.string.setting_summary_game_back_button) ;
			String summaryInsert = "?" ;
			if ( value.equals(res.getString(R.string.setting_value_key_game_back_button_quit)) )
				summaryInsert = res.getString(R.string.setting_value_title_game_back_button_quit) ;
			if ( value.equals(res.getString(R.string.setting_value_key_game_back_button_menu)) )
				summaryInsert = res.getString(R.string.setting_value_title_game_back_button_menu) ;
			if ( value.equals(res.getString(R.string.setting_value_key_game_back_button_ask)) )
				summaryInsert = res.getString(R.string.setting_value_title_game_back_button_ask) ;
			
			String summary = summaryTemplate.replace(
					res.getString(R.string.placeholder_setting_value_string), summaryInsert) ;
			
			p.setSummary(summary);
			return;
		}

		// special cases?
		if (p == mPreferences[PREFERENCE_GRAPHICS_GRAPHICAL_DETAIL]) {
			CharSequence value = intValue >= 0
					&& intValue < ((ListPreference) p).getEntryValues().length ? ((ListPreference) p)
					.getEntryValues()[intValue] : ((ListPreference) p)
					.getValue();
			String strSummary = null;
			if (value
					.equals(res
							.getString(R.string.setting_value_key_graphics_graphical_detail_low)))
				strSummary = res
						.getString(R.string.setting_summary_graphics_graphical_detail_low);
			else if (value
					.equals(res
							.getString(R.string.setting_value_key_graphics_graphical_detail_mid)))
				strSummary = res
						.getString(R.string.setting_summary_graphics_graphical_detail_mid);
			else if (value
					.equals(res
							.getString(R.string.setting_value_key_graphics_graphical_detail_high)))
				strSummary = res
						.getString(R.string.setting_summary_graphics_graphical_detail_high);

			p.setSummary(strSummary);
			return;
		}

		if ( p == mPreferences[PREFERENCE_BACKGROUND_CURRENT_TEMPLATE]
			                      || p == mPreferences[PREFERENCE_BACKGROUND_MENU_TEMPLATE] ) {
				CharSequence value = intValue >= 0
					&& intValue < ((ListPreference) p).getEntryValues().length ? ((ListPreference) p)
					.getEntryValues()[intValue] : ((ListPreference) p)
					.getValue();
			String strSummary = null;
			if (value
					.equals(res
							.getString(R.string.setting_value_key_background_template_none)))
				strSummary = res
						.getString(R.string.setting_summary_background_template_none);
			else if (value
					.equals(res
							.getString(R.string.setting_value_key_background_template_pieces)))
				strSummary = res
						.getString(R.string.setting_summary_background_template_pieces);
			else if (value
					.equals(res
							.getString(R.string.setting_value_key_background_template_spin)))
				strSummary = res
						.getString(R.string.setting_summary_background_template_spin);
			else if (value
					.equals(res
							.getString(R.string.setting_value_key_background_template_argyle)))
				strSummary = res
						.getString(R.string.setting_summary_background_template_argyle);
			else if (value
					.equals(res
							.getString(R.string.setting_value_key_background_template_rhombi)))
				strSummary = res
						.getString(R.string.setting_summary_background_template_rhombi);
			else if (value
					.equals(res
							.getString(R.string.setting_value_key_background_template_tartan)))
				strSummary = res
						.getString(R.string.setting_summary_background_template_tartan);
			else if (value
					.equals(res
							.getString(R.string.setting_value_key_background_template_tilted_tartan)))
				strSummary = res
						.getString(R.string.setting_summary_background_template_tilted_tartan);
			

			p.setSummary(strSummary);
			return;
		}

		if ( p == mPreferences[PREFERENCE_BACKGROUND_CURRENT_SHADE]
		                      || p == mPreferences[PREFERENCE_BACKGROUND_MENU_SHADE] ) {
			// TODO: If we get ways to set color other than "brightness", make
			// the change here.
			return;
		}

		if (p == mPreferences[PREFERENCE_CONTROLS_TEMPLATE]) {
			// update the summary, the extended summary, and hide/show
			// preference categories.
			CharSequence value = intValue >= 0
					&& intValue < ((ListPreference) p).getEntryValues().length ? ((ListPreference) p)
					.getEntryValues()[intValue] : ((ListPreference) p)
					.getValue();

			((PreferenceScreen) mPreferences[PREFERENCE_CONTROLS_SCREEN])
					.removeAll();
			((PreferenceScreen) mPreferences[PREFERENCE_CONTROLS_SCREEN])
					.addPreference(mPreferences[PREFERENCE_CONTROLS_CATEGORY_TEMPLATE]);

			if (value
					.equals(res
							.getString(R.string.setting_value_key_controls_template_gamepad))) {
				p.setSummary(res
						.getString(R.string.setting_summary_controls_template_gamepad));
				((PreferenceScreen) mPreferences[PREFERENCE_CONTROLS_SCREEN])
						.addPreference(mPreferences[PREFERENCE_CONTROLS_CATEGORY_GAMEPAD]);

				if ( getControlsGamepadUseAdvanced(this) )
					((PreferenceScreen) mPreferences[PREFERENCE_CONTROLS_SCREEN])
							.addPreference(mPreferences[PREFERENCE_CONTROLS_CATEGORY_GAMEPAD_ADVANCED]);
			} else {
				p.setSummary(res
						.getString(R.string.setting_summary_controls_template_gesture));
				((PreferenceScreen) mPreferences[PREFERENCE_CONTROLS_SCREEN])
						.addPreference(mPreferences[PREFERENCE_CONTROLS_CATEGORY_GESTURE]);
				
				if ( getControlsGestureUseAdvanced(this) )
					((PreferenceScreen) mPreferences[PREFERENCE_CONTROLS_SCREEN])
							.addPreference(mPreferences[PREFERENCE_CONTROLS_CATEGORY_GESTURE_ADVANCED]);
			}
			
			return;
		}
		
		if (p == mPreferences[PREFERENCE_SOUND_MUSIC_IN_MENU] || p == mPreferences[PREFERENCE_SOUND_MUSIC_IN_LOBBY] ) {
			// The summary should be the current value (strValue)
			// inserted into the appropriate template.
			String template = p == mPreferences[PREFERENCE_SOUND_MUSIC_IN_MENU]
			        ? res.getString(R.string.setting_summary_sound_music_in_menu)
					: res.getString(R.string.setting_summary_sound_music_in_lobby) ;
			        
			String summary = template.replace(res.getString(R.string.placeholder_setting_value_string), strValue) ;
			p.setSummary(summary) ;
			Log.d(TAG, "setting preference " + p + " summary to " + summary) ;
			
			return ;
		}
		
		if (p == mPreferences[PREFERENCE_CONTROLS_GAMEPAD_DOWN]) {
			// update the summary, the extended summary, and hide/show
			// preference categories.
			CharSequence value = intValue >= 0
					&& intValue < ((ListPreference) p).getEntryValues().length ? ((ListPreference) p)
					.getEntryValues()[intValue] : ((ListPreference) p)
					.getValue();
					
			if (value.equals(res.getString(R.string.setting_value_key_controls_gamepad_down_fall))) {
				p.setSummary(res.getString(R.string.setting_summary_controls_gamepad_down_fall)) ;
			} else {
				p.setSummary(res.getString(R.string.setting_summary_controls_gamepad_down_drop)) ;
			}
			
			return ;
		}

		if (p == mPreferences[PREFERENCE_CONTROLS_GAMEPAD_USE_ADVANCED]) {
			// hide or show the 'advanced options' category.
			((PreferenceScreen) mPreferences[PREFERENCE_CONTROLS_SCREEN])
					.removePreference(mPreferences[PREFERENCE_CONTROLS_CATEGORY_GAMEPAD_ADVANCED]) ;
			if ( intValue == 1 && getControls(this) == QuantroPreferences.CONTROLS_GAMEPAD ) {
				((PreferenceScreen) mPreferences[PREFERENCE_CONTROLS_SCREEN])
						.addPreference(mPreferences[PREFERENCE_CONTROLS_CATEGORY_GAMEPAD_ADVANCED]);
			}
		}
		
		if (p == mPreferences[PREFERENCE_CONTROLS_GAMEPAD_CENTER_BUTTON_WIDTH]) {
			// update the summary, the extended summary, and hide/show
			// preference categories.
			CharSequence value = intValue >= 0
					&& intValue < ((ListPreference) p).getEntryValues().length ? ((ListPreference) p)
					.getEntryValues()[intValue] : ((ListPreference) p)
					.getValue();
					
			if (value.equals(res.getString(R.string.setting_value_key_controls_gamepad_center_button_width_standard))) {
				p.setSummary(res.getString(R.string.setting_summary_controls_gamepad_center_button_width_standard)) ;
			} else if (value.equals(res.getString(R.string.setting_value_key_controls_gamepad_center_button_width_panel_to_panel))) {
				p.setSummary(res.getString(R.string.setting_summary_controls_gamepad_center_button_width_panel_to_panel)) ;
			} else {
				p.setSummary(res.getString(R.string.setting_summary_controls_gamepad_center_button_width_custom)) ;
			}
			
			return ;
		}
		
		if (p == mPreferences[PREFERENCE_CONTROLS_GESTURE_USE_ADVANCED]) {
			// hide or show the 'advanced options' category.
			((PreferenceScreen) mPreferences[PREFERENCE_CONTROLS_SCREEN])
					.removePreference(mPreferences[PREFERENCE_CONTROLS_CATEGORY_GESTURE_ADVANCED]) ;
			if ( intValue == 1 && getControls(this) == QuantroPreferences.CONTROLS_GESTURE ) {
				((PreferenceScreen) mPreferences[PREFERENCE_CONTROLS_SCREEN])
						.addPreference(mPreferences[PREFERENCE_CONTROLS_CATEGORY_GESTURE_ADVANCED]);
			}
		}

		String key = p.getKey();

		// Now that we have those values, set the summary appropriately.
		String summary;
		if (mOriginalSummary.containsKey(key))
			summary = mOriginalSummary.get(key);
		else {
			summary = p.getSummary().toString();
			mOriginalSummary.put(key, summary);
		}

		String strPlaceholder = res
				.getString(R.string.placeholder_setting_value_string);
		String intPlaceholder = res
				.getString(R.string.placeholder_setting_value_int);

		if (strValue != null)
			summary = summary.replace(strPlaceholder, strValue);
		summary = summary.replace(intPlaceholder, "" + intValue);

		p.setSummary(summary);
	}
	
	
	
	// //////////////////////////////////////////////////////////////////////////
	//
	// DYNAMICALLY SET LIST VALUES / KEYS
	//
	// //////////////////////////////////////////////////////////////////////////
	
	private void refreshQuantroSkinTemplateEntriesAndSetting() {
		// construct arrays of value_keys and value_titles.
		Skin.Template [] templates = Skin.getTemplates(Skin.Game.QUANTRO) ;
		String [] entries = new String[templates.length] ;
		String [] entryValues = new String[templates.length] ;
		
		for ( int i = 0; i < templates.length; i++ ) {
			entries[i] = skinTemplateEnumToValueTitle(this, templates[i]) ;
			entryValues[i] = skinTemplateEnumToValueKey(this, templates[i]) ;
		}
		
		// set the list entries...
		((ListPreference)mPreferences[PREFERENCE_SKIN_QUANTRO_TEMPLATE]).setEntryValues(entryValues) ;
		((ListPreference)mPreferences[PREFERENCE_SKIN_QUANTRO_TEMPLATE]).setEntries(entries) ;
		
		// set the current setting.
		Skin.Template template = getSkinQuantro(this).getTemplate() ;
		for ( int i = 0; i < templates.length; i++ ) {
			if ( templates[i] == template ) {
				((ListPreference)mPreferences[PREFERENCE_SKIN_QUANTRO_TEMPLATE]).setValueIndex(i) ;
			}
		}
	}
	
	
	private void refreshQuantroSkinColorEntriesAndSetting( Skin.Template quantroTemplate ) {
		// construct arrays of value_keys and value_titles.
		Skin [] templateSkins = Skin.getSkins(Skin.Game.QUANTRO, quantroTemplate) ;
		String [] entries = new String[templateSkins.length] ;
		String [] entryValues = new String[templateSkins.length] ;
		
		for ( int i = 0; i < templateSkins.length; i++ ) {
			Skin.Color color = templateSkins[i].getColor() ;
			entries[i] = skinColorEnumToValueTitle(this, color) ;
			entryValues[i] = skinColorEnumToValueKey(this, color) ;
		}
		
		// set the list entries...
		((ListPreference)mPreferences[PREFERENCE_SKIN_QUANTRO_COLOR]).setEntryValues(entryValues) ;
		((ListPreference)mPreferences[PREFERENCE_SKIN_QUANTRO_COLOR]).setEntries(entries) ;
		
		// set the current setting...
		Skin.Color color = getSkinQuantro(this).getColor() ;
		if ( Skin.isSkin(Skin.Game.QUANTRO, quantroTemplate, color) ) {
			// set by this index.
			for ( int i = 0; i < templateSkins.length; i++ ) {
				if ( templateSkins[i].getColor() == color ) {
					((ListPreference)mPreferences[PREFERENCE_SKIN_QUANTRO_COLOR]).setValueIndex(i) ;
				}
			}
		} else {
			// just set to the first, whatever it is.
			((ListPreference)mPreferences[PREFERENCE_SKIN_QUANTRO_COLOR]).setValueIndex(0) ;
			setSkinQuantroColor(this, templateSkins[0].getColor()) ;
		}
	}
	
	private void refreshRetroSkinTemplateEntriesAndSetting() {
		// construct arrays of value_keys and value_titles.
		Skin.Template [] templates = Skin.getTemplates(Skin.Game.RETRO) ;
		String [] entries = new String[templates.length] ;
		String [] entryValues = new String[templates.length] ;
		
		for ( int i = 0; i < templates.length; i++ ) {
			entries[i] = skinTemplateEnumToValueTitle(this, templates[i]) ;
			entryValues[i] = skinTemplateEnumToValueKey(this, templates[i]) ;
		}
		
		// set the list entries...
		((ListPreference)mPreferences[PREFERENCE_SKIN_RETRO_TEMPLATE]).setEntryValues(entryValues) ;
		((ListPreference)mPreferences[PREFERENCE_SKIN_RETRO_TEMPLATE]).setEntries(entries) ;
		
		// set the current setting.
		Skin.Template template = getSkinRetro(this).getTemplate() ;
		for ( int i = 0; i < templates.length; i++ ) {
			if ( templates[i] == template ) {
				((ListPreference)mPreferences[PREFERENCE_SKIN_RETRO_TEMPLATE]).setValueIndex(i) ;
			}
		}
	}
	
	private void refreshRetroSkinColorEntriesAndSetting( Skin.Template quantroTemplate ) {
		// construct arrays of value_keys and value_titles.
		Skin [] templateSkins = Skin.getSkins(Skin.Game.RETRO, quantroTemplate) ;
		String [] entries = new String[templateSkins.length] ;
		String [] entryValues = new String[templateSkins.length] ;
		
		for ( int i = 0; i < templateSkins.length; i++ ) {
			Skin.Color color = templateSkins[i].getColor() ;
			entries[i] = skinColorEnumToValueTitle(this, color) ;
			entryValues[i] = skinColorEnumToValueKey(this, color) ;
		}
		
		// set the list entries...
		((ListPreference)mPreferences[PREFERENCE_SKIN_RETRO_COLOR]).setEntryValues(entryValues) ;
		((ListPreference)mPreferences[PREFERENCE_SKIN_RETRO_COLOR]).setEntries(entries) ;
		
		// set the current setting...
		Skin.Color color = getSkinRetro(this).getColor() ;
		if ( Skin.isSkin(Skin.Game.RETRO, quantroTemplate, color) ) {
			// set by this index.
			for ( int i = 0; i < templateSkins.length; i++ ) {
				if ( templateSkins[i].getColor() == color ) {
					((ListPreference)mPreferences[PREFERENCE_SKIN_RETRO_COLOR]).setValueIndex(i) ;
				}
			}
		} else {
			// just set to the first, whatever it is.
			((ListPreference)mPreferences[PREFERENCE_SKIN_RETRO_COLOR]).setValueIndex(0) ;
			setSkinRetroColor(this, templateSkins[0].getColor()) ;
		}
	}
	
	
	
	private static PremiumLibrary getPremiumLibrary( Context context ) {
		if ( context instanceof QuantroActivity )
			return ((QuantroActivity)context).getPremiumLibrary() ;
		if ( context instanceof QuantroPreferences )
			return ((QuantroPreferences)context).mPremiumLibrary ;
		return ((QuantroApplication)context.getApplicationContext()).getPremiumLibrary() ;
	}
	

	// //////////////////////////////////////////////////////////////////////////
	//
	// STATIC PREFERENCE ACCESSORS
	//
	// //////////////////////////////////////////////////////////////////////////
	
	
	public static boolean supportsBackgroundShuffle(Activity activity) {
		return ((QuantroApplication)activity.getApplication()).getGameViewMemoryCapabilities(activity).getShuffleSupported() ;
	}

	public static boolean supportsMidDetailGraphics(Context context) {
		return VersionCapabilities
				.versionAtLeast(GRAPHICS_GRAPHICAL_DETAIL_MID_ANDROID_VERSION_MINIMUM)
				&& VersionSafe.getMemoryClass(context) >= GRAPHICS_GRAPHICAL_DETAIL_MID_HEAP_SIZE_MINIMUM;
	}

	public static int defaultDetailGraphics(Activity activity) {
		// Some phones only support LOW.
		if (!supportsMidDetailGraphics(activity))
			return DrawSettings.DRAW_DETAIL_LOW;

		// If we can manage Septuple Blit, we want HIGH as the default; we
		// assume
		// we have the speed to handle it.
		GameViewMemoryCapabilities gvmc = ((QuantroApplication)activity.getApplication()).getGameViewMemoryCapabilities(activity) ;
		if (gvmc.getBlit() == DrawSettings.BLIT_SEPTUPLE)
			return DrawSettings.DRAW_DETAIL_HIGH;

		return DrawSettings.DRAW_DETAIL_MID;
	}

	// //////////////////////////////////////////////////////////////////////////
	// GAME PREFERENCES

	public static final int PIECE_TIPS_NEVER = 0;
	public static final int PIECE_TIPS_OCCASIONALLY = 1;
	public static final int PIECE_TIPS_OFTEN = 2;

	public static int getPieceTips(Context context) {
		Resources res = context.getResources();
		String value_key = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getString(
						res.getString(R.string.setting_key_game_piece_tips),
						res.getString(R.string.setting_value_key_game_piece_tips_occasionally));

		if (value_key.equals(res
				.getString(R.string.setting_value_key_game_piece_tips_never)))
			return PIECE_TIPS_NEVER;
		if (value_key.equals(res
				.getString(R.string.setting_value_key_game_piece_tips_often)))
			return PIECE_TIPS_OFTEN;

		return PIECE_TIPS_OCCASIONALLY;
	}

	public static boolean getRememberCustomSetup(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources().getString(
								R.string.setting_key_game_new_game_custom),
						true);
	}
	
	
	
	/**
	 * Should we warn the player before quitting the game?  This
	 * is a single-player only setting.
	 * 
	 * @param context
	 * @return
	 */
	public static boolean getGameWarnBeforeQuitLegacy( Context context ) {
		return PreferenceManager.getDefaultSharedPreferences(context)
		.getBoolean(
				context.getResources().getString(
						R.string.setting_key_game_warn_before_quit),
				true);
	}
	
	
	
	public static final int BACK_BUTTON_QUIT = 0 ;
	public static final int BACK_BUTTON_OPTIONS = 1 ;
	public static final int BACK_BUTTON_ASK = 2 ;
	
	
	public static int getBackButtonBehavior( Activity activity ) {
		int defaultBehavior = ((QuantroApplication)activity.getApplication()).getGameViewMemoryCapabilities(activity).getGameOverlaySupported()
				? BACK_BUTTON_OPTIONS : BACK_BUTTON_ASK ;
		
		/*
		Resources res = activity.getResources() ;
		
		String defaultKey = null ;
		switch( defaultBehavior ) {
		case BACK_BUTTON_QUIT:
			defaultKey = res.getString(R.string.setting_value_key_game_back_button_quit) ;
			break ;
		case BACK_BUTTON_OPTIONS:
			defaultKey = res.getString(R.string.setting_value_key_game_back_button_menu) ;
			break ;
		case BACK_BUTTON_ASK:
			defaultKey = res.getString(R.string.setting_value_key_game_back_button_ask) ;
			break ;
		}
		
		String key = PreferenceManager.getDefaultSharedPreferences(activity)
				.getString(res.getString(R.string.setting_key_game_back_button), defaultKey) ;
		
		if ( res.getString(R.string.setting_value_key_game_back_button_quit).equals(key) )
			return BACK_BUTTON_QUIT ;
		if ( res.getString(R.string.setting_value_key_game_back_button_menu).equals(key) )
			return BACK_BUTTON_OPTIONS ;
		if ( res.getString(R.string.setting_value_key_game_back_button_ask).equals(key) )
			return BACK_BUTTON_ASK ;
			*/
		
		return defaultBehavior ;
	}
	

	public static boolean getInGameChat(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources().getString(
								R.string.setting_key_game_in_game_chat),
						true);
	}

	public static String getMultiplayerName(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(
						context.getResources().getString(
								R.string.setting_key_game_multiplayer_name),
						"Player");
	}

	public static String getDefaultWiFiLobbyName(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(
						context.getResources().getString(
								R.string.setting_key_game_default_lobby_name),
						"WiFi Lobby");
	}

	public static void setDefaultWiFiLobbyName(Context context, String name) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putString(
				context.getResources().getString(
						R.string.setting_key_game_default_lobby_name), name);
		editor.commit();
	}
	
	public static boolean getDefaultWiFiLobbyLarge(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources().getString(
								R.string.setting_key_game_default_lobby_large),
						true);
	}

	public static void setDefaultWiFiLobbyLarge(Context context, boolean large) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(
				context.getResources().getString(
						R.string.setting_key_game_default_lobby_large), large);
		editor.commit();
	}
	
	public static int getDefaultWiFiLobbySize(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getInt(
						context.getResources().getString(
								R.string.setting_key_game_default_lobby_size),
						4);
	}

	public static void setDefaultWiFiLobbySize(Context context, int size) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putInt(
				context.getResources().getString(
						R.string.setting_key_game_default_lobby_size), size);
		editor.commit();
	}
	
	
	public static String getDefaultInternetLobbyName(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(
						context.getResources().getString(
								R.string.setting_key_game_default_internet_lobby_name),
						"Internet Lobby");
	}

	public static void setDefaultInternetLobbyName(Context context, String name) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putString(
				context.getResources().getString(
						R.string.setting_key_game_default_internet_lobby_name), name);
		editor.commit();
	}
	
	
	public static boolean getDefaultInternetLobbyLarge(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources().getString(
								R.string.setting_key_game_default_internet_lobby_large),
						true);
	}

	public static void setDefaultInternetLobbyLarge(Context context, boolean large) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(
				context.getResources().getString(
						R.string.setting_key_game_default_internet_lobby_large), large);
		editor.commit();
	}
	
	
	public static int getDefaultInternetLobbySize(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getInt(
						context.getResources().getString(
								R.string.setting_key_game_default_internet_lobby_size),
						4);
	}

	public static void setDefaultInternetLobbySize(Context context, int size) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putInt(
				context.getResources().getString(
						R.string.setting_key_game_default_internet_lobby_size), size);
		editor.commit();
	}
	
	
	
	public static final int INTERNET_LOBBY_TYPE_PUBLIC = 0 ;
	public static final int INTERNET_LOBBY_TYPE_PRIVATE = 1 ;
	public static final int INTERNET_LOBBY_TYPE_ITINERANT = 2 ;
	
	
	
	public static int getDefaultInternetLobbyType( Context context ) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getInt(
						context.getResources().getString(
								R.string.setting_key_game_default_internet_lobby_type),
								INTERNET_LOBBY_TYPE_PUBLIC );
	}
	
	
	
	public static void setDefaultInternetLobbyType( Context context, int type ) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putInt(
				context.getResources().getString(
						R.string.setting_key_game_default_internet_lobby_type), type);
		editor.commit();
	}
	
	
	
	
	

	// //////////////////////////////////////////////////////////////////////////
	// GRAPHICS PREFERENCES

	/**
	 * Returns, as a DrawSettings.DRAW_DETAIL_* constant, the currently set
	 * level of graphical detail.
	 *
	 * @return
	 */
	public static int getGraphicsGraphicalDetail(Activity activity) {
		// now-capability devices get LOW detail, always.
		if (!supportsMidDetailGraphics(activity))
			return DrawSettings.DRAW_DETAIL_LOW;

		Resources res = activity.getResources();
		String value_key = PreferenceManager.getDefaultSharedPreferences(
				activity).getString(
				activity.getResources().getString(
						R.string.setting_key_graphics_graphical_detail), null);

		if (value_key == null)
			return defaultDetailGraphics(activity);

		if (value_key
				.equals(res
						.getString(R.string.setting_value_key_graphics_graphical_detail_mid)))
			return DrawSettings.DRAW_DETAIL_MID;
		else if (value_key
				.equals(res
						.getString(R.string.setting_value_key_graphics_graphical_detail_high)))
			return DrawSettings.DRAW_DETAIL_HIGH;

		return DrawSettings.DRAW_DETAIL_LOW;
	}
	
	
	public static boolean getGraphicsSkipAnimations(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources().getString(
								R.string.setting_key_graphics_skip_animations),
						false);
	}
	

	public static boolean getGraphicsPiecePreviewMinimumProfile(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources().getString(
								R.string.setting_key_graphics_rotated_preview),
						false);
	}
	
	public static boolean getGraphicsPiecePreviewsSwapped(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources().getString(
								R.string.setting_key_graphics_swap_previews),
						false);
	}
	
	public static int getGraphicsScaleAdditive( Context context ) {
		// Extremely hacky. Assume max = 19. Don't change without comparing against settings.xml
		return Math.max(0, 19 - PreferenceManager.getDefaultSharedPreferences(context)
				.getInt(
						context.getResources().getString(
								R.string.setting_key_graphics_scale),
						19));
	}
	
	/**
	 * Are we willing to sacrifice features and graphical quality to get the best
	 * framerate?
	 * @param context
	 * @return
	 */
	public static boolean getGraphicsPrioritizeFrameRate( Context context ) {
		return PreferenceManager.getDefaultSharedPreferences(context)
		.getBoolean(
				context.getResources().getString(
						R.string.setting_key_graphics_prioritize_framerate),
				false);
	}
	
	
	public static void setGraphicsPrioritizeFrameRate( Context context, boolean prioritize ) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(
				context.getResources().getString(
						R.string.setting_key_graphics_prioritize_framerate), prioritize);
		editor.commit();
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// SKIN GETTERS / SETTERS
	
	public static Skin getSkinQuantro( Activity activity ) {
		if ( !hasSkinQuantroSet(activity) && hasLegacyGraphicsQuantroSkinSet(activity) )
			return getLegacyGraphicsQuantroSkin(activity) ;
		
		// Default is the standard skin.
		Skin.Template t = getSkinQuantroTemplate( activity ) ;
		Skin.Color c = getSkinQuantroColor( activity ) ;
		
		if ( Skin.isSkin( Skin.Game.QUANTRO, t, c ) ) {
			Skin skin = Skin.get( Skin.Game.QUANTRO, t, c) ;
			if ( getPremiumLibrary(activity).has(skin) )
				return skin ;
		}
		return Skin.get( Skin.Game.QUANTRO, Skin.Template.STANDARD, Skin.Color.QUANTRO ) ;
	}
	
	public static Skin getSkinRetro( Activity activity ) {
		if ( !hasSkinRetroSet(activity) && hasLegacyGraphicsRetroSkinSet(activity) )
			return getLegacyGraphicsRetroSkin(activity) ;
		
		// Default is the standard skin.
		Skin.Template t = getSkinRetroTemplate( activity ) ;
		Skin.Color c = getSkinRetroColor( activity ) ;
		
		if ( Skin.isSkin( Skin.Game.RETRO, t, c ) ) {
			Skin skin = Skin.get( Skin.Game.RETRO, t, c) ;
			if ( getPremiumLibrary(activity).has(skin) )
				return skin ;
		}
		return Skin.get( Skin.Game.RETRO, Skin.Template.STANDARD, Skin.Color.RETRO ) ;
	}
	
	public static void setSkinGame( Context context, Skin skin ) {
		if ( skin.getGame() == Skin.Game.QUANTRO )
			setSkinQuantro( context, skin ) ;
		else
			setSkinRetro( context, skin ) ;
	}
	
	public static void setSkinQuantro( Context context, Skin skin ) {
		if ( skin.getGame() != Skin.Game.QUANTRO )
			throw new IllegalArgumentException("Not a Quantro skin.") ;
		
		String templateValue = skinTemplateEnumToValueKey( context, skin.getTemplate() ) ;
		String colorValue = skinColorEnumToValueKey( context, skin.getColor() ) ;
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putString(
				context.getResources().getString(
						R.string.setting_key_skin_quantro_template), templateValue);
		editor.putString(
				context.getResources().getString(
						R.string.setting_key_skin_quantro_color), colorValue);
		
		editor.commit();
	}
	
	public static void setSkinRetro( Context context, Skin skin ) {
		if ( skin.getGame() != Skin.Game.RETRO )
			throw new IllegalArgumentException("Not a Retro skin.") ;
		
		String templateValue = skinTemplateEnumToValueKey( context, skin.getTemplate() ) ;
		String colorValue = skinColorEnumToValueKey( context, skin.getColor() ) ;
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putString(
				context.getResources().getString(
						R.string.setting_key_skin_retro_template), templateValue);
		editor.putString(
				context.getResources().getString(
						R.string.setting_key_skin_retro_color), colorValue);
		
		editor.commit();
	}
	
	private static Skin.Template getSkinQuantroTemplate( Context context ) {
		Resources res = context.getResources();
		String default_key = res.getString(R.string.setting_value_key_skin_template_standard) ;

		String value_key = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(res.getString(R.string.setting_key_skin_quantro_template),
						default_key) ;
		
		return skinTemplateValueKeyToEnum( context, value_key ) ;
	}
	
	private static Skin.Color getSkinQuantroColor( Context context ) {
		try {
			Resources res = context.getResources();
			String default_key = res.getString(R.string.setting_value_key_skin_color_quantro) ;
	
			String value_key = PreferenceManager.getDefaultSharedPreferences(context)
					.getString(res.getString(R.string.setting_key_skin_quantro_color),
							default_key) ;
			
			return skinColorValueKeyToEnum( context, value_key ) ;
		} catch ( Exception e ) {
			return Skin.Color.QUANTRO ;
		}
	}
	
	private static void setSkinQuantroColor( Context context, Skin.Color color ) {
		String colorValue = skinColorEnumToValueKey( context, color ) ;
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putString(
				context.getResources().getString(
						R.string.setting_key_skin_quantro_color), colorValue);
		
		editor.commit();
	}
	
	private static Skin.Template getSkinRetroTemplate( Context context ) {
		Resources res = context.getResources();
		String default_key = res.getString(R.string.setting_value_key_skin_template_standard) ;

		String value_key = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(res.getString(R.string.setting_key_skin_retro_template),
						default_key) ;
		
		return skinTemplateValueKeyToEnum( context, value_key ) ;
	}
	
	private static Skin.Color getSkinRetroColor( Context context ) {
		try {
			Resources res = context.getResources();
			String default_key = res.getString(R.string.setting_value_key_skin_color_retro) ;
	
			String value_key = PreferenceManager.getDefaultSharedPreferences(context)
					.getString(res.getString(R.string.setting_key_skin_retro_color),
							default_key) ;
			
			return skinColorValueKeyToEnum( context, value_key ) ;
		} catch ( Exception e ) {
			return Skin.Color.RETRO ;
		}
	}
	
	private static void setSkinRetroColor( Context context, Skin.Color color ) {
		String colorValue = skinColorEnumToValueKey( context, color ) ;
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putString(
				context.getResources().getString(
						R.string.setting_key_skin_retro_color), colorValue);
		
		editor.commit();
	}
	
	
	private static Skin.Template skinTemplateValueKeyToEnum( Context context, String value_key ) {
		Resources res = context.getResources() ;
		if ( res.getString(R.string.setting_value_key_skin_template_standard).equals(value_key) )
			return Skin.Template.STANDARD ;
		if ( res.getString(R.string.setting_value_key_skin_template_colorblind).equals(value_key) )
			return Skin.Template.COLORBLIND ;
		if ( res.getString(R.string.setting_value_key_skin_template_ne0n).equals(value_key) )
			return Skin.Template.NEON ;
		
		throw new IllegalArgumentException("Don't recognize value_key " + value_key) ;
	}
	
	private static Skin.Color skinColorValueKeyToEnum( Context context, String value_key ) {
		Resources res = context.getResources() ;
		if ( res.getString(R.string.setting_value_key_skin_color_retro).equals(value_key) )
			return Skin.Color.RETRO ;
		if ( res.getString(R.string.setting_value_key_skin_color_quantro).equals(value_key) )
			return Skin.Color.QUANTRO ;
		if ( res.getString(R.string.setting_value_key_skin_color_clean).equals(value_key) )
			return Skin.Color.CLEAN ;
		if ( res.getString(R.string.setting_value_key_skin_color_red).equals(value_key) )
			return Skin.Color.RED ;
		if ( res.getString(R.string.setting_value_key_skin_color_green).equals(value_key) )
			return Skin.Color.GREEN ;
		if ( res.getString(R.string.setting_value_key_skin_color_blue).equals(value_key) )
			return Skin.Color.BLUE ;
		if ( res.getString(R.string.setting_value_key_skin_color_primary).equals(value_key) )
			return Skin.Color.PRIMARY ;
		if ( res.getString(R.string.setting_value_key_skin_color_austere).equals(value_key) )
			return Skin.Color.AUSTERE ;
		if ( res.getString(R.string.setting_value_key_skin_color_severe).equals(value_key) )
			return Skin.Color.SEVERE ;
		if ( res.getString(R.string.setting_value_key_skin_color_ne0n).equals(value_key) )
			return Skin.Color.NEON ;
		if ( res.getString(R.string.setting_value_key_skin_color_limbo).equals(value_key) )
			return Skin.Color.LIMBO ;
		if ( res.getString(R.string.setting_value_key_skin_color_nile).equals(value_key) )
			return Skin.Color.NILE ;
		if ( res.getString(R.string.setting_value_key_skin_color_natural).equals(value_key) )
			return Skin.Color.NATURAL ;
		if ( res.getString(R.string.setting_value_key_skin_color_zen).equals(value_key) )
			return Skin.Color.ZEN ;
		if ( res.getString(R.string.setting_value_key_skin_color_dawn).equals(value_key) )
			return Skin.Color.DAWN ;
		if ( res.getString(R.string.setting_value_key_skin_color_decadence).equals(value_key) )
			return Skin.Color.DECADENCE ;
		if ( res.getString(R.string.setting_value_key_skin_color_protanopia).equals(value_key) )
			return Skin.Color.PROTANOPIA ;
		if ( res.getString(R.string.setting_value_key_skin_color_deuteranopia).equals(value_key) )
			return Skin.Color.DEUTERANOPIA ;
		if ( res.getString(R.string.setting_value_key_skin_color_tritanopia).equals(value_key) )
			return Skin.Color.TRITANOPIA ;
		
		throw new IllegalArgumentException("Don't recognize value_key " + value_key) ;
	}
	
	private static String skinTemplateEnumToValueKey( Context context, Skin.Template template ) {
		Resources res = context.getResources() ;
		
		switch( template ) {
		case STANDARD:
			return res.getString(R.string.setting_value_key_skin_template_standard) ;
		case COLORBLIND:
			return res.getString(R.string.setting_value_key_skin_template_colorblind) ;
		case NEON:
			return res.getString(R.string.setting_value_key_skin_template_ne0n) ;
		}
		
		throw new IllegalArgumentException("Don't recognize Template " + template) ;
	}
	
	private static String skinTemplateEnumToValueTitle( Context context, Skin.Template template ) {
		Resources res = context.getResources() ;
		
		switch( template ) {
		case STANDARD:
			return res.getString(R.string.setting_value_title_skin_template_standard) ;
		case COLORBLIND:
			return res.getString(R.string.setting_value_title_skin_template_colorblind) ;
		case NEON:
			return res.getString(R.string.setting_value_title_skin_template_ne0n) ;
		}
		
		throw new IllegalArgumentException("Don't recognize Template " + template) ;
	}
	
	private static String skinColorEnumToValueKey( Context context, Skin.Color color ) {
		Resources res = context.getResources() ;
		
		switch( color ) {
		case RETRO:
			return res.getString(R.string.setting_value_key_skin_color_retro) ;
		case QUANTRO:
			return res.getString(R.string.setting_value_key_skin_color_quantro) ;
		case CLEAN:
			return res.getString(R.string.setting_value_key_skin_color_clean) ;
		case RED:
			return res.getString(R.string.setting_value_key_skin_color_red) ;
		case GREEN:
			return res.getString(R.string.setting_value_key_skin_color_green) ;
		case BLUE:
			return res.getString(R.string.setting_value_key_skin_color_blue) ;
		case PRIMARY:
			return res.getString(R.string.setting_value_key_skin_color_primary) ;
		case AUSTERE:
			return res.getString(R.string.setting_value_key_skin_color_austere) ;
		case SEVERE:
			return res.getString(R.string.setting_value_key_skin_color_severe) ;
		case NEON:
			return res.getString(R.string.setting_value_key_skin_color_ne0n) ;
		case LIMBO:
			return res.getString(R.string.setting_value_key_skin_color_limbo) ;
		case NILE:
			return res.getString(R.string.setting_value_key_skin_color_nile) ;
		case NATURAL:
			return res.getString(R.string.setting_value_key_skin_color_natural) ;
		case ZEN:
			return res.getString(R.string.setting_value_key_skin_color_zen) ;
		case DAWN:
			return res.getString(R.string.setting_value_key_skin_color_dawn) ;	
		case DECADENCE:
			return res.getString(R.string.setting_value_key_skin_color_decadence) ;
		case PROTANOPIA:
			return res.getString(R.string.setting_value_key_skin_color_protanopia) ;
		case DEUTERANOPIA:
			return res.getString(R.string.setting_value_key_skin_color_deuteranopia) ;
		case TRITANOPIA:
			return res.getString(R.string.setting_value_key_skin_color_tritanopia) ;
		}
		
		throw new IllegalArgumentException("Don't recognize Color " + color) ;
	}
	
	private static String skinColorEnumToValueTitle( Context context, Skin.Color color ) {
		Resources res = context.getResources() ;
		
		switch( color ) {
		case RETRO:
			return res.getString(R.string.setting_value_title_skin_color_retro) ;
		case QUANTRO:
			return res.getString(R.string.setting_value_title_skin_color_quantro) ;
		case CLEAN:
			return res.getString(R.string.setting_value_title_skin_color_clean) ;
		case RED:
			return res.getString(R.string.setting_value_title_skin_color_red) ;
		case GREEN:
			return res.getString(R.string.setting_value_title_skin_color_green) ;
		case BLUE:
			return res.getString(R.string.setting_value_title_skin_color_blue) ;
		case PRIMARY:
			return res.getString(R.string.setting_value_title_skin_color_primary) ;
		case AUSTERE:
			return res.getString(R.string.setting_value_title_skin_color_austere) ;
		case SEVERE:
			return res.getString(R.string.setting_value_title_skin_color_severe) ;
		case NEON:
			return res.getString(R.string.setting_value_title_skin_color_ne0n) ;
		case LIMBO:
			return res.getString(R.string.setting_value_title_skin_color_limbo) ;
		case NILE:
			return res.getString(R.string.setting_value_title_skin_color_nile) ;
		case NATURAL:
			return res.getString(R.string.setting_value_title_skin_color_natural) ;
		case ZEN:
			return res.getString(R.string.setting_value_title_skin_color_zen) ;
		case DAWN:
			return res.getString(R.string.setting_value_title_skin_color_dawn) ;
		case DECADENCE:
			return res.getString(R.string.setting_value_title_skin_color_decadence) ;
		case PROTANOPIA:
			return res.getString(R.string.setting_value_title_skin_color_protanopia) ;
		case DEUTERANOPIA:
			return res.getString(R.string.setting_value_title_skin_color_deuteranopia) ;
		case TRITANOPIA:
			return res.getString(R.string.setting_value_title_skin_color_tritanopia) ;
		}
		
		throw new IllegalArgumentException("Don't recognize Color " + color) ;
	}
	
	

	private static boolean getLegacyGraphicsColorblindHelp(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources().getString(
								R.string.setting_key_graphics_colorblind_help),
						false);
	}
	
	private static void setLegacyGraphicsColorblindHelp(Context context, boolean help) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(
				context.getResources().getString(
						R.string.setting_key_graphics_colorblind_help), help);
		editor.commit();
	}
	
	
	private static boolean hasLegacyGraphicsQuantroSkinSet( Context context ) {
		Resources res = context.getResources() ;
		String value_key = PreferenceManager.getDefaultSharedPreferences(context)
			.getString(res.getString(R.string.setting_key_graphics_color_scheme_quantro),
					null) ;
		
		return value_key != null ;
	}
	
	private static boolean hasLegacyGraphicsRetroSkinSet( Context context ) {
		Resources res = context.getResources() ;
		String value_key = PreferenceManager.getDefaultSharedPreferences(context)
			.getString(res.getString(R.string.setting_key_graphics_color_scheme_retro),
					null) ;
		
		return value_key != null ;
	}
	
	private static boolean hasSkinQuantroSet( Context context ) {
		Resources res = context.getResources() ;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context) ;
		String template_key = 
			sharedPreferences.getString(res.getString(R.string.setting_key_skin_quantro_template),
					null) ;
		String color_key = 
			sharedPreferences.getString(res.getString(R.string.setting_key_skin_quantro_color),
					null) ;
		
		return template_key != null && color_key != null ;
	}
	
	private static boolean hasSkinRetroSet( Context context ) {
		Resources res = context.getResources() ;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context) ;
		String template_key = 
			sharedPreferences.getString(res.getString(R.string.setting_key_skin_retro_template),
					null) ;
		String color_key = 
			sharedPreferences.getString(res.getString(R.string.setting_key_skin_retro_color),
					null) ;
		
		return template_key != null && color_key != null ;
	}
	
	
	
	
	private static Skin getLegacyGraphicsQuantroSkin( Context context ) {
		// TODO: Store this as its own value.
		int scheme = getLegacyGraphicsQuantroColorScheme(context) ;
		Skin.Color color = legacyColorSchemeToColor( Skin.Game.QUANTRO, scheme ) ;
		
		Skin.Template template = getLegacyGraphicsColorblindHelp(context)
				? Skin.Template.COLORBLIND
				: Skin.Template.STANDARD ;
		
		return Skin.get( Skin.Game.QUANTRO, template, color ) ;
	}
	
	private static Skin getLegacyGraphicsRetroSkin( Context context ) {
		// TODO: Store this as its own value.
		int scheme = getLegacyGraphicsRetroColorScheme(context) ;
		Skin.Color color = legacyColorSchemeToColor( Skin.Game.RETRO, scheme ) ;
		
		Skin.Template template = color == Skin.Color.NEON
				? Skin.Template.NEON
				: Skin.Template.STANDARD ;
		
		return Skin.get( Skin.Game.RETRO, template, color ) ;
	}
	
	private static void setLegacyGraphicsGameSkin( Context context, Skin skin ) {
		if ( skin.getGame() == Skin.Game.QUANTRO )
			setLegacyGraphicsQuantroSkin( context, skin ) ;
		else if ( skin.getGame() == Skin.Game.RETRO )
			setLegacyGraphicsRetroSkin( context, skin ) ;
	}
	
	private static void setLegacyGraphicsQuantroSkin( Context context, Skin skin ) {
		// store as a color scheme and 'colorblind help.'
		int scheme = legacyColorToColorScheme( Skin.Game.QUANTRO, skin.getColor() ) ;
		setLegacyGraphicsQuantroColorScheme( context, scheme ) ;
		
		boolean colorblind = skin.getTemplate() == Skin.Template.COLORBLIND ;
		setLegacyGraphicsColorblindHelp( context, colorblind ) ;
	}
	
	private static void setLegacyGraphicsRetroSkin( Context context, Skin skin ) {
		// store as a color scheme.
		int scheme = legacyColorToColorScheme( Skin.Game.RETRO, skin.getColor() ) ;
		setLegacyGraphicsRetroColorScheme( context, scheme ) ;
	}
	

	private static int getLegacyGraphicsQuantroColorScheme(Context context) {
		Resources res = context.getResources();
		String default_key = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(res.getString(R.string.setting_key_graphics_color_scheme),
						res.getString(R.string.setting_value_key_graphics_color_scheme_standard)) ;

		String value_key = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(res.getString(R.string.setting_key_graphics_color_scheme_quantro),
						default_key) ;
		
		return legacyColorSchemeKeyToColorScheme( res, value_key ) ;
	}
	
	private static int getLegacyGraphicsRetroColorScheme(Context context) {
		Resources res = context.getResources() ;
		String default_key = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(res.getString(R.string.setting_key_graphics_color_scheme), 
						res.getString(R.string.setting_value_key_graphics_color_scheme_standard));

		String value_key = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(res.getString(R.string.setting_key_graphics_color_scheme_retro),
						default_key) ;
		
		return legacyColorSchemeKeyToColorScheme( res, value_key ) ;
	}
	
	private static void setLegacyGraphicsQuantroColorScheme(Context context, int scheme) {
		Resources res = context.getResources();
		// scheme and colorblind?
		String schemeKey = legacyColorSchemeToColorSchemeKey( res, scheme ) ;
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putString(
				context.getResources().getString(
						R.string.setting_key_graphics_color_scheme_quantro), schemeKey);
		
		editor.commit();
	}
	
	private static void setLegacyGraphicsRetroColorScheme(Context context, int scheme) {
		Resources res = context.getResources();
		// scheme and colorblind?
		String schemeKey = legacyColorSchemeToColorSchemeKey( res, scheme ) ;
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putString(
				context.getResources().getString(
						R.string.setting_key_graphics_color_scheme_retro), schemeKey);
		
		editor.commit();
	}
	
	private static final int COLOR_SCHEME_STANDARD 		= 1 ;
	private static final int COLOR_SCHEME_PROTANOPIA 	= 2 ;
	private static final int COLOR_SCHEME_DEUTERANOPIA 	= 3 ;
	private static final int COLOR_SCHEME_TRITANOPIA 	= 4 ;
	private static final int COLOR_SCHEME_NE0N			= 5 ;
	private static final int COLOR_SCHEME_NILE			= 6 ;

	private static Skin.Color legacyColorSchemeToColor( Skin.Game game, int scheme ) {
		switch ( scheme ) {
		case COLOR_SCHEME_STANDARD:
			return ( game == Skin.Game.QUANTRO ) ? Skin.Color.QUANTRO : Skin.Color.RETRO ;
		case COLOR_SCHEME_PROTANOPIA:
			return Skin.Color.PROTANOPIA ;
		case COLOR_SCHEME_DEUTERANOPIA:
			return Skin.Color.DEUTERANOPIA ;
		case COLOR_SCHEME_TRITANOPIA:
			return Skin.Color.TRITANOPIA ;
		case COLOR_SCHEME_NE0N:
			return Skin.Color.NEON ;
		case COLOR_SCHEME_NILE:
			return Skin.Color.NILE ;
		}
		
		return null ;
	}
	
	private static int legacyColorToColorScheme( Skin.Game game, Skin.Color color ) {
		switch( color ) {
		case QUANTRO:
		case RETRO:
			return COLOR_SCHEME_STANDARD ;
		case PROTANOPIA:
			return COLOR_SCHEME_PROTANOPIA ;
		case DEUTERANOPIA:
			return COLOR_SCHEME_DEUTERANOPIA ;
		case TRITANOPIA:
			return COLOR_SCHEME_TRITANOPIA ;
		case NEON:
			return COLOR_SCHEME_NE0N ;
		case NILE:
			return COLOR_SCHEME_NILE ;
		}
		
		throw new IllegalArgumentException("Don't know what to do with color "+ color) ;
	}

	private static int legacyColorSchemeKeyToColorScheme( Resources res, String value_key ) {
		if (value_key.equals(res.getString(R.string.setting_value_key_graphics_color_scheme_protanopia)))
			return COLOR_SCHEME_PROTANOPIA;
		if (value_key.equals(res.getString(R.string.setting_value_key_graphics_color_scheme_deuteranopia)))
			return COLOR_SCHEME_DEUTERANOPIA;
		if (value_key.equals(res.getString(R.string.setting_value_key_graphics_color_scheme_tritanopia)))
			return COLOR_SCHEME_TRITANOPIA;
		if ( value_key.equals(res.getString(R.string.setting_value_key_graphics_color_scheme_ne0n) ) )
			return COLOR_SCHEME_NE0N ;
		if ( value_key.equals(res.getString(R.string.setting_value_key_graphics_color_scheme_nile) ) )
			return COLOR_SCHEME_NILE ;

		return COLOR_SCHEME_STANDARD;
	}
	
	private static String legacyColorSchemeToColorSchemeKey( Resources res, int scheme ) {
		switch( scheme ) {
		case COLOR_SCHEME_PROTANOPIA:
			return res.getString(R.string.setting_value_key_graphics_color_scheme_protanopia) ;
		case COLOR_SCHEME_DEUTERANOPIA:
			return res.getString(R.string.setting_value_key_graphics_color_scheme_deuteranopia) ;
		case COLOR_SCHEME_TRITANOPIA:
			return res.getString(R.string.setting_value_key_graphics_color_scheme_tritanopia) ;
		case COLOR_SCHEME_NE0N:
			return res.getString(R.string.setting_value_key_graphics_color_scheme_ne0n) ;
		case COLOR_SCHEME_NILE:
			return res.getString(R.string.setting_value_key_graphics_color_scheme_nile) ;
			
		default:
			return res.getString(R.string.setting_value_key_graphics_color_scheme_standard) ;
		}
	}
	

	private static final int GRAPHICS_BACKGROUND_COLOR_BLACK = 0;
	private static final int GRAPHICS_BACKGROUND_COLOR_DARK = 1;
	private static final int GRAPHICS_BACKGROUND_COLOR_LIGHT = 2;
	private static final int GRAPHICS_BACKGROUND_COLOR_WHITE = 3;
	
	public static Background getBackgroundCurrent( Context context ) {
		Background.Template t = getBackgroundCurrentTemplate(context) ;
		Background.Shade s = getBackgroundCurrentShade(context) ;
		
		Background bg = Background.get(t, s) ;
		if ( getPremiumLibrary(context).has(bg) )
			return bg ;
		return Background.get( Background.Template.PIECES, Background.Shade.DARK ) ;
	}
	
	/**
	 * Returns the currently set Menu background.  If our settings
	 * indicate that the current background should be used, we return
	 * it; otherwise, we return the currently set menu background.
	 * 
	 * @param context
	 * @return
	 */
	public static Background getBackgroundMenu( Context context ) {
		if ( getBackgroundMenuUseCurrent( context ) )
			return getBackgroundCurrent( context ) ;
		
		Background.Template t = getBackgroundMenuTemplate(context) ;
		Background.Shade s = getBackgroundMenuShade(context) ;
		
		Background bg = Background.get(t, s) ;
		if ( getPremiumLibrary(context).has(bg) )
			return bg ;
		return Background.get( Background.Template.PIECES, Background.Shade.DARK ) ;
	}
	
	public static void setBackgroundCurrent( Context context, Background background ) {
		setBackgroundCurrentTemplate( context, background.getTemplate() ) ;
		setBackgroundCurrentShade( context, background.getShade() ) ;
	}

	private static Background.Template getBackgroundCurrentTemplate(Context context) {
		Resources res = context.getResources();
		String value_key = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getString(
						res.getString(R.string.setting_key_background_current_template),
						res.getString(R.string.setting_value_key_background_template_pieces));

		return backgroundTemplateKeyToBackgroundTemplate( res, value_key ) ;
	}
	
	private static Background.Template getBackgroundMenuTemplate( Context context ) {
		Resources res = context.getResources();
		String value_key = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getString(
						res.getString(R.string.setting_key_background_menu_template),
						res.getString(R.string.setting_value_key_background_template_pieces));

		return backgroundTemplateKeyToBackgroundTemplate( res, value_key ) ;
	}
	
	private static Background.Template backgroundTemplateKeyToBackgroundTemplate( Resources res, String value_key ) {
		if (value_key
				.equals(res
						.getString(R.string.setting_value_key_background_template_none)))
			return Background.Template.NONE;
		if (value_key
				.equals(res
						.getString(R.string.setting_value_key_background_template_pieces)))
			return Background.Template.PIECES;
		if (value_key
				.equals(res
						.getString(R.string.setting_value_key_background_template_spin)))
			return Background.Template.SPIN;
		if (value_key
				.equals(res
						.getString(R.string.setting_value_key_background_template_argyle)))
			return Background.Template.ARGYLE;
		if (value_key
				.equals(res
						.getString(R.string.setting_value_key_background_template_rhombi)))
			return Background.Template.RHOMBI;
		if (value_key
				.equals(res
						.getString(R.string.setting_value_key_background_template_tartan)))
			return Background.Template.TARTAN;
		if (value_key
				.equals(res
						.getString(R.string.setting_value_key_background_template_tilted_tartan)))
			return Background.Template.TILTED_TARTAN;
		
		return Background.Template.NONE;
	}
	
	private static void setBackgroundCurrentTemplate(Context context, Background.Template template) {
		Resources res = context.getResources();
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putString(
				context.getResources().getString(
						R.string.setting_key_background_current_template),
						backgroundTemplateToBackgroundTemplateKey( res, template ) );
		editor.commit();
	}
	
	private static void setBackgroundMenuTemplate(Context context, Background.Template template) {
		Resources res = context.getResources();
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putString(
				context.getResources().getString(
						R.string.setting_key_background_menu_template),
						backgroundTemplateToBackgroundTemplateKey( res, template ) );
		editor.commit();
	}
	
	private static String backgroundTemplateToBackgroundTemplateKey( Resources res, Background.Template template ) {
		switch( template ) {
		case NONE:
			return res.getString(R.string.setting_value_key_background_template_none) ;
		case PIECES:
			return res.getString(R.string.setting_value_key_background_template_pieces) ;
		case SPIN:
			return res.getString(R.string.setting_value_key_background_template_spin) ;
		case ARGYLE:
			return res.getString(R.string.setting_value_key_background_template_argyle) ;
		case RHOMBI:
			return res.getString(R.string.setting_value_key_background_template_rhombi) ;
		case TARTAN:
			return res.getString(R.string.setting_value_key_background_template_tartan) ;
		case TILTED_TARTAN:
			return res.getString(R.string.setting_value_key_background_template_tilted_tartan) ;
		}
		
		return res.getString(R.string.setting_value_key_background_template_none) ;
	}

	private static Background.Shade getBackgroundCurrentShade(Context context) {
		Resources res = context.getResources();

		int val = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getInt(res
						.getString(R.string.setting_key_background_current_shade),
						GRAPHICS_BACKGROUND_COLOR_DARK);

		return backgroundColorIntToBackgroundShade( val ) ;
	}
	
	private static Background.Shade getBackgroundMenuShade(Context context) {
		Resources res = context.getResources();

		int val = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getInt(res
						.getString(R.string.setting_key_background_menu_shade),
						GRAPHICS_BACKGROUND_COLOR_DARK);

		return backgroundColorIntToBackgroundShade( val ) ;
	}
	
	private static Background.Shade backgroundColorIntToBackgroundShade( int val ) {
		switch( val ) {
		case GRAPHICS_BACKGROUND_COLOR_WHITE:
			return Background.Shade.WHITE ;
		case GRAPHICS_BACKGROUND_COLOR_LIGHT:
			return Background.Shade.LIGHT ;
		case GRAPHICS_BACKGROUND_COLOR_DARK:
			return Background.Shade.DARK ;
		case GRAPHICS_BACKGROUND_COLOR_BLACK:
		default:
			return Background.Shade.BLACK ;
		}
	}

	private static void setBackgroundCurrentShade(Context context, Background.Shade shade ) {
		int val = backgroundShadeToBackgroundColorInt( shade ) ;
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putInt(
				context.getResources().getString(
						R.string.setting_key_background_current_shade),
						val );
		editor.commit();
	}
	
	private static int backgroundShadeToBackgroundColorInt( Background.Shade shade ) {
		switch( shade ) {
		case WHITE:
			return GRAPHICS_BACKGROUND_COLOR_WHITE ;
		case LIGHT:
			return GRAPHICS_BACKGROUND_COLOR_LIGHT ;
		case DARK:
			return GRAPHICS_BACKGROUND_COLOR_DARK ;
		case BLACK:
		default:
			return GRAPHICS_BACKGROUND_COLOR_BLACK ;
		}
	}
	
	
	/**
	 * Do we shuffle through backgrounds?
	 * @return
	 */
	public static boolean getBackgroundShuffles( Activity activity ) {
		if ( !supportsBackgroundShuffle(activity) ) {
			return false ;
		}
		
		Resources res = activity.getResources();
		return PreferenceManager
				.getDefaultSharedPreferences(activity)
				.getBoolean(
						res.getString(R.string.setting_key_background_shuffle),
						true);
	}
	
	
	/**
	 * Do we shuffle through backgrounds?
	 * @param context
	 * @return
	 */
	public static void setBackgroundShuffles( Context context, boolean shuffles ) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(
				context.getResources().getString(
						R.string.setting_key_background_shuffle),
						shuffles );
		editor.commit();
		
		// make sure our current background is in the shuffle.
		if ( shuffles )
			setBackgroundCurrentInShuffleIfNecessary( context ) ;
	}
	
	
	/**
	 * Returns the backgrounds we will shuffle through, as an ArrayList.
	 * 
	 * This method accounts for CurrentBackground and manually set shuffles,
	 * as well as whether 'backgroundShuffles' is set.
	 * 
	 * It will also takes current Content Keys into account -- but that's
	 * future functionality.
	 *
	 * @return
	 */
	public static Set<Background> getBackgroundsInShuffle( Activity activity ) {
		Set<Background> shuffle = new HashSet<Background>() ;
		
		if ( !getBackgroundShuffles( activity ) ) {
			shuffle.add( getBackgroundCurrent(activity) ) ;
			return shuffle ;
		}
		
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(activity) ;
		
		PremiumLibrary premiumLibrary = getPremiumLibrary(activity) ;
		
		// iterate through the image backgrounds, checking each in turn.
		// By default -- i.e. if there is no stored value for whether the
		// background is shuffled -- we include it.
		Background [] image_bgs = Background.getBackgroundsWithImage() ;
		String in_shuffle_key_prefix = activity.getResources().getString(R.string.setting_key_background_in_shuffle_prefix) ;
		for ( int i = 0; i < image_bgs.length; i++ ) {
			Background bg = image_bgs[i] ;
			String key = in_shuffle_key_prefix + Background.toStringEncoding(bg) ;
			boolean include = sharedPrefs.getBoolean(key, true) && premiumLibrary.has(bg) ;
			if ( include )
				shuffle.add(bg) ;
		}
		// also check NONE / Black -- unlike images, we do NOT include this by default.
		Background bg = Background.get(
				Background.Template.NONE, Background.Shade.BLACK) ;
		String key = in_shuffle_key_prefix + Background.toStringEncoding(bg) ;
		boolean include = sharedPrefs.getBoolean(key, false) ;
		if ( include )
			shuffle.add(bg) ;
		
		return shuffle ;
	}
	
	
	/**
	 * Returns whether the provided background is included in the shuffle.  Equivalent
	 * to getBackgroundsInShuffle().contains( background ), although faster and with
	 * lower memory usage.
	 *
	 * @param background
	 * @return
	 */
	public static boolean getBackgroundInShuffle( Activity activity, Background background ) {
		
		// if not shuffling, it only included if current.
		if ( !getBackgroundShuffles( activity ) ) {
			Background current = getBackgroundCurrent(activity) ;
			return current.equals(background) ;
		}
		
		String in_shuffle_key_prefix = activity.getResources().getString(R.string.setting_key_background_in_shuffle_prefix) ;
		String key = in_shuffle_key_prefix + Background.toStringEncoding(background) ;
		
		boolean includedByDefault = background.hasImage() ;
		
		boolean included = PreferenceManager.getDefaultSharedPreferences(activity)
				.getBoolean(key, includedByDefault) ;
		
		included = included && getPremiumLibrary(activity).has(background) ;
		
		return included ;
	}
	
	/**
	 * Sets whether the specified background is included in our shuffle.
	 * @param context
	 * @param background
	 * @param inShuffle
	 */
	public static void setBackgroundInShuffle( Context context, Background background, boolean inShuffle ) {
		String in_shuffle_key_prefix = context.getResources().getString(R.string.setting_key_background_in_shuffle_prefix) ;
		String key = in_shuffle_key_prefix + Background.toStringEncoding(background) ;
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(key, inShuffle);
		editor.commit();
	}
	
	private static boolean getBackgroundMenuUseCurrent( Context context ) {
		Resources res = context.getResources();
		return PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(
						res.getString(R.string.setting_key_background_menu_use_current),
						true);
	}
	
	
	/**
	 * We don't ever want background shuffles to not include the current background.
	 * 
	 * This method checks whether the current background is among those shuffling,
	 * and if not, sets it to be.
	 * 
	 * This should be called whenever shuffling is turned on.
	 * 
	 * @param context
	 */
	private static void setBackgroundCurrentInShuffleIfNecessary( Context context ) {
		Background current = getBackgroundCurrent(context) ;
		String in_shuffle_key_prefix = context.getResources().getString(R.string.setting_key_background_in_shuffle_prefix) ;
		String key = in_shuffle_key_prefix + Background.toStringEncoding(current) ;
		
		boolean includedByDefault = current.hasImage() ;
		boolean included = PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(key, includedByDefault) ;
		
		if ( !included )
			setBackgroundInShuffle( context, current, true ) ;
	}
	

	// //////////////////////////////////////////////////////////////////////////
	// SOUND AND MUSIC PREFERENCES

	/**
	 * Sets that we mute all sounds when the phone's ringer is silent.  This
	 * change will be immediately reflected in the SoundPool.
	 */
	public static void setMuteWithRinger( Activity activity, boolean mute ) {
		if (mute)
			((QuantroApplication) activity.getApplication()).getSoundPool(activity)
					.muteWithRinger();
		else
			((QuantroApplication) activity.getApplication()).getSoundPool(activity)
					.unmuteWithRinger();
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(activity).edit();
		editor.putBoolean(
				activity.getResources().getString(
						R.string.setting_key_sound_mute_on_ringer_silent), mute);
		editor.commit();
	}
	
	public static boolean getMuteWithRinger(Context context) {
		return PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources()
								.getString(
										R.string.setting_key_sound_mute_on_ringer_silent),
						false);
	}
	


	/**
	 * Sets whether our sounds and music are muted.  The change will be immediately
	 * reflected in the SoundPool. 
	 * 
	 * @param activity
	 * @param muted
	 */
	public static void setMuted(Activity activity, boolean muted) {
		if (muted)
			((QuantroApplication) activity.getApplication()).getSoundPool(activity).mute() ;
		else
			((QuantroApplication) activity.getApplication()).getSoundPool(activity).unmute() ;
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(activity).edit();
		editor.putBoolean(
				activity.getResources().getString(
						R.string.setting_key_sound_play), !muted);
		editor.commit();
	}

	public static boolean getMuted(Context context) {
		return !PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources().getString(
								R.string.setting_key_sound_play), true);
	}

	public static float getVolumeSound(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(
				context.getResources().getString(
						R.string.setting_key_sound_sound_volume), 100)
				/ MAX_VOLUME_PERCENT;
	}

	public static int getVolumeSoundPercent(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(
				context.getResources().getString(
						R.string.setting_key_sound_sound_volume), 100);
	}
	
	/**
	 * Sets the current sound volume as a percent.  The change will be immediate
	 * reflected in the SoundPool.
	 * 
	 */
	public static void setVolumeSoundPercent(Activity activity, int volPercent) {
		volPercent = Math.max(0, volPercent) ;
		volPercent = (int)Math.min(volPercent, MAX_VOLUME_PERCENT) ;
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(activity).edit();
		editor.putInt(
				activity.getResources().getString(
						R.string.setting_key_sound_sound_volume), volPercent);
		editor.commit();
		
		float vol = ((float)volPercent) / MAX_VOLUME_PERCENT ;
		((QuantroApplication) activity.getApplication()).getSoundPool(activity).setInGameSoundVolume(vol) ;
	}

	public static float getVolumeMusic(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(
				context.getResources().getString(
						R.string.setting_key_sound_music_volume), 100)
				/ MAX_VOLUME_PERCENT;
	}

	public static int getVolumeMusicPercent(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(
				context.getResources().getString(
						R.string.setting_key_sound_music_volume), 100);
	}
	
	/**
	 * Sets the current music volume as a percent.  The change will be immediate
	 * reflected in the SoundPool.
	 * 
	 */
	public static void setVolumeMusicPercent(Activity activity, int volPercent) {
		volPercent = Math.max(0, volPercent) ;
		volPercent = (int)Math.min(volPercent, MAX_VOLUME_PERCENT) ;
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(activity).edit();
		editor.putInt(
				activity.getResources().getString(
						R.string.setting_key_sound_music_volume), volPercent);
		editor.commit();
		
		float vol = ((float)volPercent) / MAX_VOLUME_PERCENT ;
		((QuantroApplication) activity.getApplication()).getSoundPool(activity).setMusicVolume(vol) ;
	}
	
	

	public static boolean getSoundControls(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources().getString(
								R.string.setting_key_sound_controls), true);
	}
	
	
	public static Music getMusicInMenu( Context context ) {
		Resources res = context.getResources();

		String val = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getString(res.getString(R.string.setting_key_sound_music_in_menu),
						res.getString(R.string.setting_value_key_sound_music_main_theme));

		return musicValueKeyToMusicInstance( res, val ) ;
	}
	
	public static Music getMusicInLobby( Context context ) {
		Resources res = context.getResources();

		String val = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getString(res.getString(R.string.setting_key_sound_music_in_lobby),
						res.getString(R.string.setting_value_key_sound_music_waiting));

		return musicValueKeyToMusicInstance( res, val ) ;
	}
	
	
	private static Music musicValueKeyToMusicInstance( Resources res, String valKey ) {
		if ( valKey == null )
			return null ;
		
		if ( valKey.equals( res.getString(R.string.setting_value_key_sound_music_quantro_1) ) )
			return Music.get(Music.Track.QUANTRO_1) ;
		if ( valKey.equals( res.getString(R.string.setting_value_key_sound_music_quantro_2) ) )
			return Music.get(Music.Track.QUANTRO_2) ;
		if ( valKey.equals( res.getString(R.string.setting_value_key_sound_music_retro_1) ) )
			return Music.get(Music.Track.RETRO_1) ;
		if ( valKey.equals( res.getString(R.string.setting_value_key_sound_music_retro_2) ) )
			return Music.get(Music.Track.RETRO_2) ;
		if ( valKey.equals( res.getString(R.string.setting_value_key_sound_music_main_theme) ) )
			return Music.get(Music.Track.MAIN_THEME) ;
		if ( valKey.equals( res.getString(R.string.setting_value_key_sound_music_waiting) ) )
			return Music.get(Music.Track.WAITING) ;
		
		return null ;
	}
	
	

	// //////////////////////////////////////////////////////////////////////////
	// CONTROLS
	
	
	public static void setControlsReleaseVersionDefaults( Context context ) {
		setControls( context, CONTROLS_GAMEPAD ) ;
		setControlsGamepadDropButton( context, CONTROLS_DROP_DROP ) ;
		setControlsGamepadDropAutolocks( context, false ) ;
		setControlsGamepadDoubleDownDrop( context, false ) ;
	}
	
	
	public static void setControlsDefaults( Context context ) {
		// DEFAULTS:
		//
		setControls(context, getControlsDeviceDefault(context) ) ;
		
		setControlsGamepadDropButton( context, CONTROLS_DROP_FALL ) ;
		setControlsGamepadDropAutolocks( context, false ) ;
		setControlsGamepadDoubleDownDrop( context, true ) ;
		//
		setControlsGestureDragDownAutolock( context, false ) ;
		setControlsGestureTurnButtons( context, true ) ;
	}
	

	public static final int CONTROLS_GAMEPAD = 0;
	public static final int CONTROLS_GESTURE = 1;
	

	public static int getControlsDeviceDefault( Context context ) {
		Resources res = context.getResources();
		int screenLayout = res.getConfiguration().screenLayout;
		int size = screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

		if (size == Configuration.SCREENLAYOUT_SIZE_SMALL
				|| size == Configuration.SCREENLAYOUT_SIZE_NORMAL)
			return CONTROLS_GAMEPAD ;
		else
			return CONTROLS_GESTURE ;
	}

	public static int getControls(Context context) {
		Resources res = context.getResources() ;
		int controlsDefault = getControlsDeviceDefault( context ) ;
		String def = null ;
		switch( controlsDefault ) {
		case CONTROLS_GAMEPAD:
			def = res.getString(R.string.setting_value_key_controls_template_gamepad);
			break ;
		case CONTROLS_GESTURE:
			def = res.getString(R.string.setting_value_key_controls_template_gesture);
			break ;
		}

		String key = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(
						res.getString(R.string.setting_key_controls_template),
						def);

		if (key.equals(res
				.getString(R.string.setting_value_key_controls_template_gamepad)))
			return CONTROLS_GAMEPAD;
		else
			return CONTROLS_GESTURE;
	}
	
	
	
	public static void setControls(Context context, int controls) {
		Resources res = context.getResources() ;
		String entry = null ;
		switch( controls ) {
		case CONTROLS_GAMEPAD:
			entry = res.getString(R.string.setting_value_key_controls_template_gamepad);
			break ;
		case CONTROLS_GESTURE:
			entry = res.getString(R.string.setting_value_key_controls_template_gesture);
			break ;
		}
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putString(
				res.getString(R.string.setting_key_controls_template),
				entry);
		editor.commit();
	}
	
	
	public static boolean getControlsShowButtons(Context context) {
		Resources res = context.getResources();
		return PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(
						res.getString(R.string.setting_key_controls_show),
						true);
	}
	
	
	public static boolean getControlsGamepadQuickSlide(Context context) {
		Resources res = context.getResources();
		return PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(
						res.getString(R.string.setting_key_controls_gamepad_quick_slide),
						true);
	}

	public static final int CONTROLS_DROP_FALL = 0;
	public static final int CONTROLS_DROP_DROP = 1;

	public static int getControlsGamepadDropButton(Context context) {
		Resources res = context.getResources();
		String key = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getString(
						res.getString(R.string.setting_key_controls_gamepad_down),
						res.getString(R.string.setting_value_key_controls_gamepad_down_fall));
		
		if (key.equals(res
				.getString(R.string.setting_value_key_controls_gamepad_down_fall))) {
			return CONTROLS_DROP_FALL;
		}
		else {
			return CONTROLS_DROP_DROP;
		}
	}
	
	
	public static void setControlsGamepadDropButton(Context context, int drop) {
		Resources res = context.getResources() ;
		String entry = null ;
		switch( drop ) {
		case CONTROLS_DROP_FALL:
			entry = res.getString(R.string.setting_value_key_controls_gamepad_down_fall);
			break ;
		case CONTROLS_DROP_DROP:
			entry = res.getString(R.string.setting_value_key_controls_gamepad_down_drop);
			break ;
		}
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putString(
				res.getString(R.string.setting_key_controls_gamepad_down),
				entry);
		editor.commit();
	}

	public static boolean getControlsGamepadDropAutolocks(Context context) {
		Resources res = context.getResources();
		return PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(
						res.getString(R.string.setting_key_controls_gamepad_down_autolock),
						false);
	}
	
	public static void setControlsGamepadDropAutolocks(Context context, boolean autolock) {
		Resources res = context.getResources() ;
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(
				res.getString(R.string.setting_key_controls_gamepad_down_autolock),
				autolock);
		editor.commit();
	}

	public static boolean getControlsGamepadDoubleDownDrop(Context context) {
		Resources res = context.getResources();
		return PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(
						res.getString(R.string.setting_key_controls_gamepad_double_down_drop),
						true);
	}
	
	public static void setControlsGamepadDoubleDownDrop(Context context, boolean doubleDrops ) {
		Resources res = context.getResources() ;
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(
				res.getString(R.string.setting_key_controls_gamepad_double_down_drop),
				doubleDrops);
		editor.commit();
	}
	
	
	public static boolean getControlsGamepadUseAdvanced(Context context) {
		Resources res = context.getResources();
		return PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(
						res.getString(R.string.setting_key_controls_gamepad_use_custom),
						false);
	}
	
	
	public static boolean getControlsGamepadSwapTurnAndMove(Context context) {
		if ( getControlsGamepadUseAdvanced(context) ) {
			Resources res = context.getResources();
			return PreferenceManager
					.getDefaultSharedPreferences(context)
					.getBoolean(
							res.getString(R.string.setting_key_controls_gamepad_swap_turn_move),
							false);
		}
		
		return false ;
	}
	
	public static final int CENTER_BUTTON_WIDTH_STANDARD = 0 ;
	public static final int CENTER_BUTTON_WIDTH_PANEL_TO_PANEL = 1 ;
	public static final int CENTER_BUTTON_WIDTH_CUSTOM = 2 ;
	
	
	public static int getControlsGamepadCenterButtonWidth(Context context) {
		if ( getControlsGamepadUseAdvanced(context) )
			return getControlsGamepadCenterButtonWidthIgnoreUseAdvanced(context) ;
		
		return CENTER_BUTTON_WIDTH_STANDARD ;
	}
	
	public static int getControlsGamepadCenterButtonWidthIgnoreUseAdvanced(Context context) {
		Resources res = context.getResources() ;
		String def = res.getString(R.string.setting_value_key_controls_gamepad_center_button_width_standard);

		String key = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(
						res.getString(R.string.setting_key_controls_gamepad_center_button_width),
						def);

		if (key.equals(res
				.getString(R.string.setting_value_key_controls_gamepad_center_button_width_standard)))
			return CENTER_BUTTON_WIDTH_STANDARD ;
		else if (key.equals(res
				.getString(R.string.setting_value_key_controls_gamepad_center_button_width_panel_to_panel)))
			return CENTER_BUTTON_WIDTH_PANEL_TO_PANEL ;
		else if (key.equals(res
				.getString(R.string.setting_value_key_controls_gamepad_center_button_width_custom)))
			return CENTER_BUTTON_WIDTH_CUSTOM ;
		
		return CENTER_BUTTON_WIDTH_STANDARD ;
	}
	
	public static float getControlsGamepadCenterButtonWidthScaleFactor( Context context ) {
		if ( getControlsGamepadUseAdvanced(context) ) {
			return PreferenceManager.getDefaultSharedPreferences(context).getInt(
					context.getResources().getString(
							R.string.setting_key_controls_gamepad_center_button_width_factor), 100) / 100.f ;
		}
		
		return 1.0f ;
	}
	
	
	public static boolean getControlsGamepadCustomButtonHeight(Context context) {
		if ( getControlsGamepadUseAdvanced(context) )
			getControlsGamepadCustomButtonHeightIgnoreUseAdvanced(context) ;
		
		return false ;
	}
	
	public static boolean getControlsGamepadCustomButtonHeightIgnoreUseAdvanced(Context context) {
		Resources res = context.getResources();
		return PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(
						res.getString(R.string.setting_key_controls_gamepad_custom_height),
						false);
	}
	
	
	public static float getControlsGamepadButtonHeightScaleFactor( Context context ) {
		if ( getControlsGamepadUseAdvanced(context) ) {
			return PreferenceManager.getDefaultSharedPreferences(context).getInt(
					context.getResources().getString(
							R.string.setting_key_controls_gamepad_custom_height_factor), 100) / 100.f ;
		}
		
		return 1.0f ;
	}
	
	
	public static boolean getControlsGestureQuickSlide(Context context) {
		Resources res = context.getResources();
		return PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(
						res.getString(R.string.setting_key_controls_gesture_quick_slide),
						false);
	}
	

	public static boolean getControlsGestureDragDownAutolock(Context context) {
		Resources res = context.getResources();
		return PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(
						res.getString(R.string.setting_key_controls_gesture_drag_down_autolock),
						false);
	}
	
	
	public static void setControlsGestureDragDownAutolock(Context context, boolean autolock ) {
		Resources res = context.getResources() ;
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(
				res.getString(R.string.setting_key_controls_gesture_drag_down_autolock),
				autolock);
		editor.commit();
	}

	public static boolean getControlsGestureTurnButtons(Context context) {
		Resources res = context.getResources();
		return PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(
						res.getString(R.string.setting_key_controls_gesture_turn_buttons),
						false);
	}
	
	public static void setControlsGestureTurnButtons(Context context, boolean show ) {
		Resources res = context.getResources() ;
		
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(
				res.getString(R.string.setting_key_controls_gesture_turn_buttons),
				show);
		editor.commit();
	}
	
	public static boolean getControlsGestureUseAdvanced(Context context) {
		Resources res = context.getResources();
		return PreferenceManager
				.getDefaultSharedPreferences(context)
				.getBoolean(
						res.getString(R.string.setting_key_controls_gesture_use_custom),
						false);
	}
	
	public static float getControlsGestureDragExaggeration(Context context) {
		if ( getControlsGestureUseAdvanced(context) ) {
			return PreferenceManager.getDefaultSharedPreferences(context).getInt(
					context.getResources().getString(
							R.string.setting_key_controls_gesture_drag_exaggeration), 100) / 100.f ;
		}
		
		return 1.0f ;
	}
	
	public static float getControlsGestureFlingSensitivity(Context context) {
		if ( getControlsGestureUseAdvanced(context) ) {
			return PreferenceManager.getDefaultSharedPreferences(context).getInt(
					context.getResources().getString(
							R.string.setting_key_controls_gesture_fling_sensitivity), 100) / 100.f ;
		}
		
		return 1.0f ;
	}

	////////////////////////////////////////////////////////////////////////////
	// XL PREFERENCES 
	
	// Numbered from 1 -- not from 0 -- for legacy reasons.
	public static final int XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION = 1;
			// normal display: status bar up top, navigation below (if software)
	public static final int XL_SIZE_SHOW_NAVIGATION = 2;
			// previously "full screen": hide the status bar, but show navigation buttons.
	public static final int XL_SIZE_IMMERSIVE = 3 ;
			// available only on KitKat (4.4 API 19).  Hide both status bar and navigation.
			// Can show either with a swipe.
	
	
	public static int fullScreenImmersiveValueKeyToXLInt( String key, Resources res ) {
		if ( res.getString(R.string.setting_value_key_full_screen_immersive_off).equals(key) )
			return XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION ;
		if ( res.getString(R.string.setting_value_key_full_screen_immersive_nav_only).equals(key) )
			return XL_SIZE_SHOW_NAVIGATION ;
		if ( res.getString(R.string.setting_value_key_full_screen_immersive_on).equals(key) )
			return XL_SIZE_IMMERSIVE ;
		return XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION ;
	}
	
	public static String xlIntToFullScreenImmersiveValueKey( int xl, Resources res ) {
		switch( xl ) {
		case XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION:
			return res.getString(R.string.setting_value_key_full_screen_immersive_off) ;
		case XL_SIZE_SHOW_NAVIGATION:
			return res.getString(R.string.setting_value_key_full_screen_immersive_nav_only) ;
		case XL_SIZE_IMMERSIVE:
			return res.getString(R.string.setting_value_key_full_screen_immersive_on) ;
		}
		return res.getString(R.string.setting_value_key_full_screen_immersive_off) ;
	}
	
	
	
	public static int getScreenSizeGame(Activity activity) {
		PremiumLibrary pl = null ;
		if ( activity instanceof QuantroActivity )
			pl = ((QuantroActivity)activity).getPremiumLibrary() ;
		else if ( activity instanceof QuantroPreferences )
			pl = ((QuantroPreferences)activity).mPremiumLibrary ;
		if ( pl == null || !pl.hasHideStatusBar() )
			return XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION;
		
		Resources res = activity.getResources();
		boolean hide_status = PreferenceManager.getDefaultSharedPreferences(
				activity).getBoolean(
				res.getString(R.string.setting_key_xl_fullscreen_game), false);

		if ( VersionCapabilities.supportsImmersiveFullScreen() ) {
			String settingValueKey = PreferenceManager.getDefaultSharedPreferences(
					activity).getString(res.getString(R.string.setting_key_full_screen_immersive_game),
							res.getString( hide_status
									? R.string.setting_value_key_full_screen_immersive_on
									: R.string.setting_value_key_full_screen_immersive_off)) ;
			return fullScreenImmersiveValueKeyToXLInt(settingValueKey, res) ;
		}
		
		return hide_status ? XL_SIZE_SHOW_NAVIGATION : XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION;
	}

	public static void getAndApplyScreenSizeGame(Activity activity) {
		int size = getScreenSizeGame(activity);
		applyScreenSize(size, activity, false);
	}

	public static void getAndApplyScreenSizeGameDialog(Activity activity) {
		int size = getScreenSizeGame(activity);
		applyScreenSizeDialog(size, activity, false);
	}

	public static int getScreenSizeLobby(Activity activity) {
		PremiumLibrary pl = null ;
		if ( activity instanceof QuantroActivity )
			pl = ((QuantroActivity)activity).getPremiumLibrary() ;
		else if ( activity instanceof QuantroPreferences )
			pl = ((QuantroPreferences)activity).mPremiumLibrary ;
		if ( pl == null || !pl.hasHideStatusBar() )
			return XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION;

		Resources res = activity.getResources();
		boolean hide_status = PreferenceManager.getDefaultSharedPreferences(
				activity).getBoolean(
				res.getString(R.string.setting_key_xl_fullscreen_lobby), false);
		
		if ( VersionCapabilities.supportsImmersiveFullScreen() ) {
			String settingValueKey = PreferenceManager.getDefaultSharedPreferences(
					activity).getString(res.getString(R.string.setting_key_full_screen_immersive_lobby),
							res.getString( hide_status
									? R.string.setting_value_key_full_screen_immersive_nav_only
									: R.string.setting_value_key_full_screen_immersive_off)) ;
			return fullScreenImmersiveValueKeyToXLInt(settingValueKey, res) ;
		}

		return hide_status ? XL_SIZE_SHOW_NAVIGATION : XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION;
	}

	public static void getAndApplyScreenSizeLobby(Activity activity) {
		int size = getScreenSizeLobby(activity);
		applyScreenSize(size, activity, false);
	}

	public static void getAndApplyScreenSizeLobbyDialog(Activity activity) {
		int size = getScreenSizeLobby(activity);
		applyScreenSizeDialog(size, activity, false);
	}

	public static int getScreenSizeMenu(Activity activity) {
		PremiumLibrary pl = null ;
		if ( activity instanceof QuantroActivity )
			pl = ((QuantroActivity)activity).getPremiumLibrary() ;
		else if ( activity instanceof QuantroPreferences )
			pl = ((QuantroPreferences)activity).mPremiumLibrary ;
		if ( pl == null || !pl.hasHideStatusBar() )
			return XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION;

		Resources res = activity.getResources();
		boolean hide_status = PreferenceManager.getDefaultSharedPreferences(
				activity).getBoolean(
				res.getString(R.string.setting_key_xl_fullscreen_menu), false);
		
		if ( VersionCapabilities.supportsImmersiveFullScreen() ) {
			String settingValueKey = PreferenceManager.getDefaultSharedPreferences(
					activity).getString(res.getString(R.string.setting_key_full_screen_immersive_menu),
							res.getString( hide_status
									? R.string.setting_value_key_full_screen_immersive_nav_only
									: R.string.setting_value_key_full_screen_immersive_off)) ;
			return fullScreenImmersiveValueKeyToXLInt(settingValueKey, res) ;
		}

		return hide_status ? XL_SIZE_SHOW_NAVIGATION : XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION;
	}

	public static void getAndApplyScreenSizeMenu(Activity activity) {
		int size = getScreenSizeMenu(activity);
		applyScreenSize(size, activity, false);
	}

	public static void getAndApplyScreenSizeMenuDialog(Activity activity) {
		int size = getScreenSizeMenu(activity);
		applyScreenSizeDialog(size, activity, false);
	}

	private static void applyScreenSize(int size, Activity activity, boolean isGame) {
		QuantroActivity qa = null;
		if (activity instanceof QuantroActivity)
			qa = (QuantroActivity) activity;
		
		if ( size == 0 )
			size = XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION ;

		switch (size) {
		case XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION:
			if (qa != null) {
				qa.setFlagShowTitle(false);
				qa.setFlagShowNotification(true);
				qa.setFlagShowNavigation(true) ;
			}
			break;
		case XL_SIZE_SHOW_NAVIGATION:
			// We only allow sticky immersive settings in games.  For everything
			// else, it does not play nice with dialogs, popups, etc., and therefore
			// we use a permanent window flag instead.
			VersionSafe.setupUIShowNavigationBar(activity, isGame) ;
			if (qa != null) {
				qa.setFlagShowTitle(false);
				qa.setFlagShowNotification(false);
				qa.setFlagShowNavigation(true) ;
			}
			break;
		case XL_SIZE_IMMERSIVE:
			// We only allow sticky immersive settings in games.  For everything
			// else, it does not play nice with dialogs, popups, etc.
			boolean supportsImmersive = VersionCapabilities.supportsImmersiveFullScreen() ;
			if ( supportsImmersive )
				VersionSafe.setupUIImmersive(activity) ;
			else
				VersionSafe.setupUIShowNavigationBar(activity, isGame) ;
			if (qa != null) {
				qa.setFlagShowTitle(false);
				qa.setFlagShowNotification(false);
				qa.setFlagShowNavigation( !supportsImmersive ) ;
			}
			break;
		}
	}

	/**
	 * We like to pass activities the "previous screen size" upon launching.
	 * 
	 * @param size
	 * @param activity
	 */
	private static void applyScreenSizeDialog(int size, Activity activity, boolean isGame) {
		QuantroActivity qa = null;
		if (activity instanceof QuantroActivity)
			qa = (QuantroActivity) activity;

		switch (size) {
		case XL_SIZE_SHOW_NOTIFICATION_AND_NAVIGATION:
			if (qa != null) {
				qa.setFlagShowTitle(false);
				qa.setFlagShowNotification(true);
				qa.setFlagShowNavigation(true) ;
			}
			break;
		case XL_SIZE_SHOW_NAVIGATION:
			VersionSafe.setupUIShowNavigationBar(activity, isGame) ;
			if (qa != null) {
				qa.setFlagShowTitle(false);
				qa.setFlagShowNotification(false);
				qa.setFlagShowNavigation(true) ;
			}
			break;
		case XL_SIZE_IMMERSIVE:
			boolean supportsImmersive = VersionCapabilities.supportsImmersiveFullScreen() ;
			if ( supportsImmersive )
				VersionSafe.setupUIImmersive(activity) ;
			else
				VersionSafe.setupUIShowNavigationBar(activity, isGame) ;
			if (qa != null) {
				qa.setFlagShowTitle(false);
				qa.setFlagShowNotification(false);
				qa.setFlagShowNavigation( !supportsImmersive ) ;
			}
			break;
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// NETWORK
	
	public static boolean getNetworkWifiLock(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources().getString(
								R.string.setting_key_network_wifi_lock), true);
	}
	

	// /////////////////////////////////////////////////////////////////////////
	// ANALYTICS

	public static boolean getAnalyticsActive(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources().getString(
								R.string.setting_key_analytics_active), true);
	}

	public static boolean getAnalyticsAggregated(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(
						context.getResources().getString(
								R.string.setting_key_analytics_aggregated),
						false);
	}

	// //////////////////////////////////////////////////////////////////////////
	// PRIVATE ACTIVITY SETTINGS
	//
	// Semi-permanent stuff we want to keep a convenient record of, but that the
	// user should never directly access.
	//
	// USAGE NOTE: Activities should NEVER assume that these preferences exist;
	// they
	// may be cleared at any time.

	private static String getPrivateSettingName(Context context, String name) {
		return context.getResources().getString(
				R.string.setting_key_private_prefix)
				+ name;
	}

	public static long getPrivateSettingLong(Context context, String name,
			long defaultValue) {
		String prefName = getPrivateSettingName(context, name);
		return PreferenceManager.getDefaultSharedPreferences(context).getLong(
				prefName, defaultValue);
	}

	public static void setPrivateSettingLong(Context context, String name,
			long value) {
		String prefName = getPrivateSettingName(context, name);
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putLong(prefName, value);
		editor.commit();
	}

	public static String getPrivateSettingString(Context context, String name,
			String defValue) {
		String prefName = getPrivateSettingName(context, name);
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(prefName, defValue);
	}

	public static void setPrivateSettingString(Context context, String name,
			String value) {
		String prefName = getPrivateSettingName(context, name);
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putString(prefName, value);
		editor.commit();
	}

	public static boolean getPrivateSettingBoolean(Context context,
			String name, boolean defValue) {
		String prefName = getPrivateSettingName(context, name);
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(prefName, defValue);
	}

	public static void setPrivateSettingBoolean(Context context, String name,
			boolean value) {
		String prefName = getPrivateSettingName(context, name);
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(prefName, value);
		editor.commit();
	}
	
	public static int getPrivateSettingInt(Context context,
			String name, int defValue) {
		String prefName = getPrivateSettingName(context, name);
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getInt(prefName, defValue);
	}

	public static void setPrivateSettingInt(Context context, String name,
			int value) {
		String prefName = getPrivateSettingName(context, name);
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putInt(prefName, value);
		editor.commit();
	}

}
