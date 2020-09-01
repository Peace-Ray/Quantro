package com.peaceray.quantro.main;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import com.peaceray.quantro.QuantroActivity;
import com.peaceray.quantro.QuantroApplication;
import com.peaceray.quantro.QuantroPreferences;
import com.peaceray.quantro.R;
import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.content.Music;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.premium.PremiumLibrary;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.view.button.strip.CustomButtonStrip;
import com.peaceray.quantro.view.button.strip.MainMenuButtonStrip;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.colors.GameModeColors;
import com.peaceray.quantro.view.generic.PinnedHeaderListView;
import com.peaceray.quantro.view.options.OptionAvailability;
import com.peaceray.quantro.view.options.OptionsBackgroundButtonView;
import com.peaceray.quantro.view.options.OptionsBackgroundView;
import com.peaceray.quantro.view.options.OptionsControlsStripView;
import com.peaceray.quantro.view.options.OptionsControlsView;
import com.peaceray.quantro.view.options.OptionsSkinButtonView;
import com.peaceray.quantro.view.options.OptionsSkinView;
import com.peaceray.quantro.view.options.OptionsSoundMusicButtonView;
import com.peaceray.quantro.view.options.OptionsSoundMusicView;
import com.peaceray.quantro.view.options.OptionsView;
import com.velosmobile.utils.SectionableAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;

import androidx.fragment.app.Fragment;

public class GameOptionsMenuFragment extends Fragment
		implements OptionsSkinView.Delegate,
				OptionsBackgroundView.Delegate,
				OptionsSoundMusicView.Delegate,
				OptionsControlsView.Delegate,
				CustomButtonStrip.Delegate,
				MainMenuButtonStrip.Delegate {
	
	private static final String TAG = "GameOptionsMenuFragment" ;
	
	public interface Listener {
		
		public void gomfl_onAttach( GameOptionsMenuFragment fragment ) ;
		
		////////////////////////////////////////////////////////////////////////
		//
		// OPTIONS CHANGES
		//
		
		/**
		 * The user has changed the skin to the provided skin via the OptionsMenu Fragment.
		 * Load this skin to replace the current one, unless it is currently loaded.
		 * 
		 * Note that the MenuFragment has already made the necessary Settings changes;
		 * all you need to do is change the one currently loaded for the game.
		 * @param skin
		 */
		public void gomfl_setCurrentSkinAndColors( Skin skin, ColorScheme colorScheme ) ;
		
		
		/**
		 * The user has changed the background to the provided Background via
		 * the OptionsMenuFragment.  Load this background to replace the current
		 * one, unless it is currently loaded.
		 * 
		 * Note that the MenuFragment has already made the necessary Settings changes;
		 * all you need to do is change the one currently loaded for the game.
		 * 
		 * @param background
		 */
		public void gomfl_setCurrentBackground( Background background ) ;
		
		/**
		 * The user has changed the set of backgrounds to be shuffled.  Update
		 * your settings appropriately.
		 * 
		 * @param backgrounds
		 */
		public void gomfl_setBackgroundsInShuffle( Collection<Background> backgrounds ) ;
		
		/**
		 * The user has changed whether the background is to shuffle or not.
		 * In all likelihood the current background is to remain the same.
		 * 
		 * Note that the MenuFragment has already made the necessary Settings changes;
		 * all you need to do is change the current in-game shuffle behavior.
		 * 
		 * @param shuffles
		 */
		public void gomfl_setBackgroundShuffles( boolean shuffles ) ;
		
		/**
		 * The user has changed the music to the provided Music via the
		 * OptionsMenuFragment.  Load this music to replace the current one,
		 * unless it is currently loaded.
		 * 
		 * Note that the MenuFragment has already made the necessary internal
		 * changes.  If you want to call 'setCurrentMusic', go ahea, but it
		 * shouldn't be necessary. 
		 * 
		 * @param music
		 */
		public void gomfl_setCurrentMusic( Music music ) ;
		
		/**
		 * The user has switched to Gamepad controls.  Load the current controls
		 * from Preferences (we have already made the necessary changes).
		 */
		public void gomfl_setCurrentControlsGamepad() ;
		
		/**
		 * The user has switched to Gesture controls.  Load the current controls
		 * from Preferences (we have already made the necessary changes).
		 */
		public void gomfl_setCurrentControlsGesture() ;
		
		//
		////////////////////////////////////////////////////////////////////////
		
		////////////////////////////////////////////////////////////////////////
		// 
		// ACTIVITY CHANGES
		//
		
		/**
		 * The user has dismissed this menu.  If you paused just for this,
		 * you may want to unpause.
		 */
		public void gomfl_optionsMenuFragmentDismissed() ;
		
		/**
		 * The user wants to quit.  If you put up a pop-up rather than
		 * quitting, make sure you dismiss this Fragment (as appropriate).
		 */
		public void gomfl_quit() ;
		
		//
		////////////////////////////////////////////////////////////////////////
	}
	
	// Section headings
	private static final int SECTION_MAIN = 0 ;
	private static final int SECTION_SKIN = 1 ;
	private static final int SECTION_BACKGROUND = 2 ;
	private static final int SECTION_MUSIC_SOUND = 3 ;
	private static final int SECTION_CONTROLS = 4 ;
	private static final int NUM_SECTIONS = 5 ;
	
	// Menu information
	private static final int MENU_ITEM_RESUME = 0 ;
	private static final int MENU_ITEM_SKIN = 1 ;
	private static final int MENU_ITEM_BACKGROUND = 2 ;
	private static final int MENU_ITEM_MUSIC_SOUND = 3 ;
	private static final int MENU_ITEM_CONTROLS = 4 ;
	private static final int MENU_ITEM_ADVANCED = 5 ;
	private static final int MENU_ITEM_QUIT = 6 ;
	private static final int NUM_MENU_ITEMS = 7 ;

	
	private boolean [] mMenuItemActive ;
	private boolean [] mMenuItemVisible ;
	private String [] mMenuItemTitle ;
	private String [] mMenuItemDescription ;
	private int [] mMenuItemColor ;
	
	
	private String ACTION_NAME_RESUME ;
	private String ACTION_NAME_SETTINGS ;
	
	// No-Op
	View.OnClickListener mNoOpOnClickListener ;
	
	// Main Content
	View mContent ;
	Listener mListener ;
	
	boolean mShown ;
	
	// Section Views
	View [] mSectionContent ;
	int mSectionActive ;
	
	// Action Bar
	private CustomButtonStrip mActionStrip ;
	
	// Options Menu
	private ListView mMenuListView ;
	private MenuItemArrayAdapter mMenuArrayAdapter ;
	
	// Subsection Views
	private OptionsView [] mSectionOptionsView ;
	private OptionsSkinButtonView mOptionsSkinView ;
	private OptionsBackgroundButtonView mOptionsBackgroundView ;
	private OptionsSoundMusicButtonView mOptionsMusicSoundView ;
	private OptionsControlsStripView mOptionsControlsView ;
	
	// Current game?
	private int mGameMode ;
	
	// sound pool / sound controls
	private QuantroSoundPool mSoundPool ;
	private boolean mSoundControls ;
	
	// Current color / skin?
	private ColorScheme mColorScheme ;
	private Skin mSkin ;
	private Background mBackground ;
	private Set<Background> mBackgroundShuffle ;
	private Music mMusic ;
	private Drawable mControlsThumbnail ;
	
	
	@Override
	public void onAttach( Activity activity ) {
		super.onAttach(activity) ;
		try {
			mListener = (Listener)activity ;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " is not a GameOptionsMenuFragment.Listener.") ;
		}
		
		mShown = false ;
		mListener.gomfl_onAttach( this ) ;
		
		mSoundPool = ((QuantroApplication)activity.getApplication()).getSoundPool(activity) ;
		mSoundControls = QuantroPreferences.getSoundControls(activity) ;

		Skin skinQuantro = QuantroPreferences.getSkinQuantro(activity) ;
		Skin skinRetro = QuantroPreferences.getSkinRetro(activity) ;
		mColorScheme = new ColorScheme( activity, skinQuantro.getColor(), skinRetro.getColor() ) ;
	}

	@Override
	public void onCreate( Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState) ;
		
		mNoOpOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// no effect; return after eating the input.
			}
		} ;
		
		//  load item titles, descriptions, etc.  Set visibility.
		Activity activity = getActivity() ;
		Resources res = activity.getResources() ;
		ACTION_NAME_RESUME = res.getString(R.string.action_strip_name_resume) ;
		ACTION_NAME_SETTINGS = res.getString(R.string.action_strip_name_settings) ;
		
		mMenuItemActive = new boolean[NUM_MENU_ITEMS] ;
		mMenuItemVisible = new boolean[NUM_MENU_ITEMS] ;
		mMenuItemTitle = new String[NUM_MENU_ITEMS] ;
		mMenuItemDescription = new String[NUM_MENU_ITEMS] ;
		mMenuItemColor = new int[NUM_MENU_ITEMS] ;
		
		// load strings (titles and descriptions)
		mMenuItemTitle[MENU_ITEM_RESUME] 		= res.getString(R.string.game_options_item_resume_title) ;
		mMenuItemTitle[MENU_ITEM_SKIN] 			= res.getString(R.string.game_options_item_skin_title) ;
		mMenuItemTitle[MENU_ITEM_BACKGROUND]	= res.getString(R.string.game_options_item_background_title) ;
		mMenuItemTitle[MENU_ITEM_MUSIC_SOUND] 	= res.getString(R.string.game_options_item_music_sound_title) ;
		mMenuItemTitle[MENU_ITEM_CONTROLS] 		= res.getString(R.string.game_options_item_controls_title) ;
		mMenuItemTitle[MENU_ITEM_ADVANCED] 		= res.getString(R.string.game_options_item_advanced_title) ;
		mMenuItemTitle[MENU_ITEM_QUIT] 			= res.getString(R.string.game_options_item_quit_title) ;
		
		mMenuItemDescription[MENU_ITEM_RESUME] 		= res.getString(R.string.game_options_item_resume_description) ;
		mMenuItemDescription[MENU_ITEM_SKIN] 		= res.getString(R.string.game_options_item_skin_description) ;
		mMenuItemDescription[MENU_ITEM_BACKGROUND]	= res.getString(R.string.game_options_item_background_description) ;
		mMenuItemDescription[MENU_ITEM_MUSIC_SOUND] = res.getString(R.string.game_options_item_music_sound_description) ;
		mMenuItemDescription[MENU_ITEM_CONTROLS] 	= res.getString(R.string.game_options_item_controls_description) ;
		mMenuItemDescription[MENU_ITEM_ADVANCED] 	= res.getString(R.string.game_options_item_advanced_description) ;
		mMenuItemDescription[MENU_ITEM_QUIT] 		= res.getString(R.string.game_options_item_quit_description) ;
		
		// Active / Visible
		mMenuItemActive[MENU_ITEM_RESUME]		= false ;
		mMenuItemActive[MENU_ITEM_SKIN]			= true ;
		mMenuItemActive[MENU_ITEM_BACKGROUND]	= true ;
		mMenuItemActive[MENU_ITEM_MUSIC_SOUND]	= true ;
		mMenuItemActive[MENU_ITEM_CONTROLS]		= true ;
		mMenuItemActive[MENU_ITEM_ADVANCED]		= true ;
		mMenuItemActive[MENU_ITEM_QUIT]			= true ;
		
		// everything visible by default, then turn some off.
		for ( int i = 0; i < NUM_MENU_ITEMS; i++ )
			mMenuItemVisible[i] = true ;
		// These are handled by action bar items.
		mMenuItemVisible[MENU_ITEM_RESUME] 		= false ;
		mMenuItemVisible[MENU_ITEM_ADVANCED]	= false ;
		
		// Menu item colors.
		for ( int i = 0; i < NUM_MENU_ITEMS; i++ )
			mMenuItemColor[i] = 0xffffffff ;
		mMenuItemColor[MENU_ITEM_SKIN] = res.getColor(R.color.game_options_skin) ;
		mMenuItemColor[MENU_ITEM_RESUME] = res.getColor(R.color.game_options_main) ;
		mMenuItemColor[MENU_ITEM_BACKGROUND] = res.getColor(R.color.game_options_background) ;
		mMenuItemColor[MENU_ITEM_MUSIC_SOUND] = res.getColor(R.color.game_options_music_sound) ;
		mMenuItemColor[MENU_ITEM_CONTROLS] = res.getColor(R.color.game_options_controls) ;
		mMenuItemColor[MENU_ITEM_QUIT] = res.getColor(R.color.game_options_main) ;
	}
	
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState) ;
		
		Activity activity = getActivity() ;
		
		// Inflate view.
		mContent = inflater.inflate(R.layout.game_options_menu, container, false) ;
		// set this as touchable so we don't accidentally send touches to content behind.
		mContent.setOnClickListener(mNoOpOnClickListener) ;
		
		// Grab references to the subsections.
		mSectionContent = new View[NUM_SECTIONS] ;
		mSectionContent[SECTION_MAIN] = mContent.findViewById(R.id.game_options_menu) ;
		mSectionContent[SECTION_SKIN] = mContent.findViewById(R.id.game_options_skin) ;
		mSectionContent[SECTION_BACKGROUND] = mContent.findViewById(R.id.game_options_background) ;
		mSectionContent[SECTION_MUSIC_SOUND] = mContent.findViewById(R.id.game_options_music_sound) ;
		mSectionContent[SECTION_CONTROLS] = mContent.findViewById(R.id.game_options_controls) ;
		
		for ( int i = 0; i < NUM_SECTIONS; i++ ) {
			if ( mSectionContent[i] != null )
				mSectionContent[i].setOnClickListener(mNoOpOnClickListener) ;
		}
		mSectionOptionsView = new OptionsView[NUM_SECTIONS] ;
		
		// Perform the necessary hook-ups and reference population for the MAIN section.
		mActionStrip = (CustomButtonStrip) mSectionContent[SECTION_MAIN].findViewById(R.id.game_options_menu_action_strip) ;
		if ( mActionStrip != null ) {
			mActionStrip.setDelegate(this) ;
			int playButtonNum = mActionStrip.getButton( ACTION_NAME_RESUME ) ;
			mActionStrip.setEnabled( playButtonNum, true ) ;
			if ( mColorScheme != null && GameModeColors.hasPrimary(mGameMode) )
				mActionStrip.setColor( playButtonNum, GameModeColors.primary(mColorScheme, mGameMode) ) ;
		
			int settingsButtonNum = mActionStrip.getButton( ACTION_NAME_SETTINGS ) ;
			mActionStrip.setEnabled( settingsButtonNum, true ) ;
		}
		
		mMenuListView = (ListView) mSectionContent[SECTION_MAIN].findViewById(R.id.game_options_menu_list) ;
		mMenuListView.setItemsCanFocus(true) ;
		mMenuListView.setDivider(null) ;
		mMenuListView.setDividerHeight(0) ;
		
		mMenuArrayAdapter = new MenuItemArrayAdapter(
				inflater,
				R.layout.main_menu_list_item,
				0,
				R.id.main_menu_list_row) ;
		mMenuListView.setAdapter(mMenuArrayAdapter) ;
		
		// Perform the necessary hook-ups and reference population for the SKIN section.
		mOptionsSkinView = new OptionsSkinButtonView() ;
		mOptionsSkinView.setDelegate(this) ;
		mOptionsSkinView.setColorScheme(mColorScheme) ;
		mOptionsSkinView.setSoundPool(mSoundPool) ;
		mOptionsSkinView.setSoundControls(mSoundControls) ;
		mOptionsSkinView.init(activity, mSectionContent[SECTION_SKIN]) ;
		mOptionsSkinView.setCurrentSkin(mSkin) ;
		mSectionOptionsView[SECTION_SKIN] = mOptionsSkinView ;
		
		// Perform the necessary hook-ups and reference population for the BACKGROUND section.
		mOptionsBackgroundView = new OptionsBackgroundButtonView() ;
		mOptionsBackgroundView.setDelegate(this) ;
		mOptionsBackgroundView.setColorScheme(mColorScheme) ;
		mOptionsBackgroundView.setSoundPool(mSoundPool) ;
		mOptionsBackgroundView.setSoundControls(mSoundControls) ;
		mOptionsBackgroundView.setBackgroundShuffleSupported( QuantroPreferences.supportsBackgroundShuffle(activity) ) ;
		mOptionsBackgroundView.init(activity, mSectionContent[SECTION_BACKGROUND]) ;
		mOptionsBackgroundView.setCurrentBackground(mBackground) ;
		mSectionOptionsView[SECTION_BACKGROUND] = mOptionsBackgroundView ;
		
		
		// Perform the necessary hook-ups and reference population for the MUSIC / SOUND section.
		// Set only and exactly those things which will not change when this
		// Fragment is stopped -- e.g., the things based on GameMode,
		// and those we expect to be necessary for initialization.
		mOptionsMusicSoundView = new OptionsSoundMusicButtonView() ;
		mOptionsMusicSoundView.setDelegate(this) ;
		mOptionsMusicSoundView.setColorScheme(mColorScheme) ;
		mOptionsMusicSoundView.setSoundPool(mSoundPool) ;
		mOptionsMusicSoundView.setSoundControls(mSoundControls) ;
		mOptionsMusicSoundView.init(activity, mSectionContent[SECTION_MUSIC_SOUND]) ;
		mOptionsMusicSoundView.setCurrentMusic(mMusic) ;
		mSectionOptionsView[SECTION_MUSIC_SOUND] = mOptionsMusicSoundView ;
		
		// Perform the necessary hook-ups and reference population for the CONTROLS section.
		// Set only and exactly those things which will not change when this
		// Fragment is stopped -- e.g., the things based on GameMode,
		// and those we expect to be necessary for initialization.
		mOptionsControlsView = new OptionsControlsStripView() ;
		mOptionsControlsView.setDelegate(this) ;
		mOptionsControlsView.setColorScheme(mColorScheme) ;
		mOptionsControlsView.setSoundPool(mSoundPool) ;
		mOptionsControlsView.setSoundControls(mSoundControls) ;
		mOptionsControlsView.init(activity, mSectionContent[SECTION_CONTROLS]) ;
		mOptionsControlsView.setControlsThumbnail(mControlsThumbnail) ;
		mOptionsControlsView.setControlsHas(
				GameModes.hasRotation(mGameMode),
				GameModes.hasReflection(mGameMode) ) ;
		mSectionOptionsView[SECTION_CONTROLS] = mOptionsControlsView ;
		
		
		
		// Currently, the MAIN section is active.
		mSectionActive = SECTION_MAIN ;
		
		// After these calls, at most one section will be visible.
		if ( mShown )
			show() ;
		else
			dismiss() ;
		
		return mContent ;
	}
	
	
	@Override
	public void onActivityCreated( Bundle savedInstanceState ) {
		super.onActivityCreated(savedInstanceState) ;
		// TODO
	}
	
	
	@Override
	public void onStart () {
		super.onStart() ;
		// TODO
		
		Activity activity = getActivity() ;
		
		PremiumLibrary premiumLibrary = ((QuantroActivity)activity).getPremiumLibrary() ;
		
		// refresh sound pool and sound controls
		mSoundControls = QuantroPreferences.getSoundControls(activity) ;

		// refresh color scheme and current skin
		Skin skinQuantro = QuantroPreferences.getSkinQuantro(activity) ;
		Skin skinRetro = QuantroPreferences.getSkinRetro(activity) ;
		mColorScheme = new ColorScheme( activity, skinQuantro.getColor(), skinRetro.getColor() ) ;
		mSkin = GameModes.numberQPanes(mGameMode) == 1
				? skinRetro : skinQuantro ;
		
		do_refreshColors() ;
		
		// refresh menu items
		int numVisible = 0 ;
		for ( int i = 0; i < NUM_MENU_ITEMS; i++ ) {
			if ( mMenuItemVisible[i] )
				numVisible++ ;
		}
		int [] visibleItems = new int[numVisible] ;
		int index = 0;
		for ( int i = 0; i < NUM_MENU_ITEMS; i++ ) {
			if ( mMenuItemVisible[i] )
				visibleItems[index++] = i ;
		}
		mMenuArrayAdapter.setItems(visibleItems) ;
		mMenuArrayAdapter.notifyDataSetChanged() ;
		
		
		// refresh SKIN content
		mOptionsSkinView.setColorScheme(mColorScheme) ;
		mOptionsSkinView.setSoundPool(mSoundPool) ;
		mOptionsSkinView.setSoundControls(mSoundControls) ;
		// what are our backgrounds?  Which are available?
		Skin [] skins = Skin.getSkins( GameModes.numberQPanes(mGameMode) == 1 ? Skin.Game.RETRO : Skin.Game.QUANTRO ) ;
		mOptionsSkinView.clearSkins() ;
		for ( int i = 0; i < skins.length; i++ ) {
			mOptionsSkinView.addSkin(skins[i], getAvailability(premiumLibrary, skins[i])) ;
		}
		mOptionsSkinView.setCurrentSkin(mSkin) ;
		mOptionsSkinView.refresh() ;
		
		
		
		// refresh BACKGROUND content
		mOptionsBackgroundView.setColorScheme(mColorScheme) ;
		mOptionsBackgroundView.setSoundPool(mSoundPool) ;
		mOptionsBackgroundView.setSoundControls(mSoundControls) ;
		// what are our backgrounds?  Which are available?
		Background [] backgrounds = Background.getBackgroundsWithImage() ;
		mOptionsBackgroundView.clearBackgrounds() ;
		for ( int i = 0; i < backgrounds.length; i++ ) {
			mOptionsBackgroundView.addBackground(backgrounds[i], getAvailability(premiumLibrary, backgrounds[i])) ;
		}
		Background blackBackground = Background.get(
				Background.Template.NONE, Background.Shade.BLACK) ;
		mOptionsBackgroundView.addBackground(blackBackground, getAvailability(premiumLibrary, blackBackground)) ;
		mBackgroundShuffle = QuantroPreferences.getBackgroundsInShuffle(activity) ;
		mOptionsBackgroundView.setBackgroundShuffling(QuantroPreferences.getBackgroundShuffles(activity)) ;
		mOptionsBackgroundView.setBackgroundsShuffled(mBackgroundShuffle) ;
		mOptionsBackgroundView.setCurrentBackground(mBackground) ;
		mOptionsBackgroundView.refresh() ;
		
		// refresh MUSIC / SOUND content
		mOptionsMusicSoundView.setColorScheme(mColorScheme) ;
		mOptionsMusicSoundView.setSoundPool(mSoundPool) ;
		mOptionsMusicSoundView.setSoundControls(mSoundControls) ;
		mOptionsMusicSoundView.setSoundVolumePercent(QuantroPreferences.getVolumeSoundPercent(activity)) ;
		mOptionsMusicSoundView.setMusicVolumePercent(QuantroPreferences.getVolumeMusicPercent(activity)) ;
		mOptionsMusicSoundView.setSoundOn( !mSoundPool.isMuted(), mSoundPool.isMutedByRinger() ) ;
		// what are our tracks?  Which are available?
		Music [] musics = Music.getMusics() ;
		mOptionsMusicSoundView.clearMusic() ;
		for ( int i = 0; i < musics.length; i++ ) {
			mOptionsMusicSoundView.addMusic(musics[i], getAvailability(premiumLibrary, musics[i])) ;
		}
		mOptionsMusicSoundView.setCurrentMusic(mMusic) ;
		mOptionsMusicSoundView.refresh() ;
		
		
		
		// refresh CONTROLS content
		mOptionsControlsView.setColorScheme(mColorScheme) ;
		mOptionsControlsView.setSoundPool(mSoundPool) ;
		mOptionsControlsView.setSoundControls(mSoundControls) ;
		if ( QuantroPreferences.getControls(activity) == QuantroPreferences.CONTROLS_GAMEPAD ) {
			mOptionsControlsView.setControlsGamepad(
					QuantroPreferences.getControlsGamepadQuickSlide(activity),
					QuantroPreferences.getControlsGamepadDropButton(activity) == QuantroPreferences.CONTROLS_DROP_FALL,
	        		QuantroPreferences.getControlsGamepadDoubleDownDrop(activity) ) ;
		} else {
			mOptionsControlsView.setControlsGesture(
					QuantroPreferences.getControlsGestureQuickSlide(activity),
					QuantroPreferences.getControlsGestureTurnButtons(activity),
					QuantroPreferences.getControlsGestureDragDownAutolock(activity)) ;
		}
		mOptionsControlsView.refresh() ;
	}
	
	@Override
	public void onResume () {
		super.onResume() ;
		
		Log.d(TAG, "onResume with content " + mContent + " shown " + mShown) ;
		if ( mShown ) {
			show() ;
		}
	}
	
	
	@Override
	public void onPause () {
		super.onPause() ;
		// TODO
	}
	
	@Override
	public void onStop () {
		super.onStop() ;
		// TODO
	}
	
	@Override
	public void onDestroyView () {
		super.onDestroyView() ;
		// TODO
	}
	
	@Override
	public void onDestroy () {
		super.onDestroy() ;
		// TODO
	}
	
	@Override
	public void onDetach () {
		super.onDetach() ;
		// TODO
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// QUICK HELPER METHODS
	//
	
	private void do_refreshColors() {
		if ( mActionStrip != null && mColorScheme != null && GameModeColors.hasPrimary(mGameMode) )
			mActionStrip.setColor(
					mActionStrip.getButton(getActivity().getResources().getString(R.string.action_strip_name_resume)),
					GameModeColors.primary(mColorScheme, mGameMode) ) ;
	}
	
	private void do_resumeGame() {
		dismiss() ;
		mListener.gomfl_optionsMenuFragmentDismissed() ;
	}
	
	private OptionAvailability getAvailability( PremiumLibrary pl, Skin s ) {
		// Does the user own this content?
		if ( pl.has(s) )
			return OptionAvailability.ENABLED ;
		if ( PremiumLibrary.isPremium(s) )
			return OptionAvailability.LOCKED_ENABLED ;
		return OptionAvailability.DISABLED ;
	}
	
	private OptionAvailability getAvailability( PremiumLibrary pl, Background bg ) {
		// Does the user own this content?
		if ( pl.has(bg) )
			return OptionAvailability.ENABLED ;
		if ( PremiumLibrary.isPremium(bg) )
			return OptionAvailability.LOCKED_ENABLED ;
		return OptionAvailability.DISABLED ;
	}
	
	private OptionAvailability getAvailability( PremiumLibrary pl, Music m ) {
		// Does the user own this content?
		if ( pl.has(m) )
			return OptionAvailability.ENABLED ;
		if ( PremiumLibrary.isPremium(m) )
			return OptionAvailability.LOCKED_ENABLED ;
		return OptionAvailability.DISABLED ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////

	
	////////////////////////////////////////////////////////////////////////////
	//
	// GAME ACTIVITY CONTROL METHODS
	//
	
	public void setGameMode( int gameMode ) {
		mGameMode = gameMode ;
		// set color for resume button.
		do_refreshColors() ;
	}
	
	public void setCurrentSkinAndColorScheme( Skin skin, ColorScheme colorScheme ) {
		mSkin = skin ;
		mColorScheme = colorScheme ;
		// Pass to skin view
		if ( this.mOptionsSkinView != null )
			mOptionsSkinView.setCurrentSkin(skin) ;
		
		// Pass color scheme to all views.
		for ( int i = 0; i < NUM_SECTIONS; i++ ) {
			if ( mSectionOptionsView[i] != null )
				mSectionOptionsView[i].setColorScheme(mColorScheme) ;
		}
	}
	
	public void setCurrentBackground( Background background ) {
		mBackground = background ;
		// Pass the BackgroundView
		if ( this.mOptionsBackgroundView != null )
			mOptionsBackgroundView.setCurrentBackground(background) ;
	}
	
	public void setBackgroundsInShuffle( Collection<Background> inShuffle ) {
		mBackgroundShuffle.clear() ;
		Iterator<Background> iterator = inShuffle.iterator() ;
		for ( ; iterator.hasNext() ; )
			mBackgroundShuffle.add(iterator.next()) ;
		if ( this.mOptionsBackgroundView != null )
			mOptionsBackgroundView.setBackgroundsShuffled(mBackgroundShuffle) ;
	}

	public void setCurrentMusic( Music music ) {
		mMusic = music ;
		// Pass to MusicSoundView.
		if ( this.mOptionsMusicSoundView != null )
			mOptionsMusicSoundView.setCurrentMusic(music) ;
	}
	
	public void setControlsThumbnail( Drawable thumbnail ) {
		mControlsThumbnail = thumbnail ;
		// Pass to the ControlsView.
		if ( mOptionsControlsView != null )
			mOptionsControlsView.setControlsThumbnail(thumbnail) ;
	}
	
	public void dismiss() {
		mShown = false ;
		if ( mContent != null )
			mContent.setVisibility( View.GONE ) ;
		if ( mSectionContent != null ) {
			for ( int i = 0; i < NUM_SECTIONS; i++ ) {
				if ( mSectionContent[i] != null )
					mSectionContent[i].setVisibility( View.GONE ) ;
			}
		}
	}
	
	public void show() {
		mShown = true ;
		if ( mContent != null )
			mContent.setVisibility( View.VISIBLE ) ;
		show( mSectionActive ) ;
	}
	
	private void show( int section ) {
		mSectionActive = section ;
		if ( mSectionContent != null ) {
			for ( int i = 0; i < NUM_SECTIONS; i++ ) {
				if ( i == mSectionActive && mShown ) {
					if ( this.mSectionOptionsView[i] != null )
						mSectionOptionsView[i].refreshCache() ;
					if ( mSectionContent[i] != null )
						mSectionContent[i].setVisibility( View.VISIBLE ) ;
				} else {
					if ( this.mSectionOptionsView[i] != null )
						mSectionOptionsView[i].relaxCache() ;
					if ( mSectionContent[i] != null )
						mSectionContent[i].setVisibility( View.GONE ) ;
				}
			}
		}
	}
	
	/**
	 * The 'Back' button has been pressed while this menu is shown.
	 */
	public void backButtonPressed() {
		// TODO: Get this setting.
		boolean backToQuit = false ;
		
		// We have only 1 layer.  If the currently active section is MAIN,
		// then return to game or quit.  Otherwise, return to MAIN.
		if ( mSectionActive == SECTION_MAIN ) {
			if ( backToQuit ) {
				mListener.gomfl_quit() ;
			} else {
				do_resumeGame() ;
			}
		} else {
			show( SECTION_MAIN ) ;
		}
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// OPTIONS VIEW DELEGATE METHODS
	//
	
	////////////////////////////////////////////////////////
	// Skin Options

	@Override
	public void osvd_userSetCurrentSkin(OptionsSkinView osv, Skin skin,
			OptionAvailability availability) {
		
		Activity activity = getActivity() ;

		Skin skinQuantro = skin.getGame() == Skin.Game.QUANTRO ? skin : QuantroPreferences.getSkinQuantro(activity) ;
		Skin skinRetro = skin.getGame() == Skin.Game.RETRO ? skin : QuantroPreferences.getSkinRetro(activity) ;

		mSkin = skin ;
		mColorScheme = new ColorScheme( getActivity(), skinQuantro.getColor(), skinRetro.getColor() ) ;
		
		QuantroPreferences.setSkinGame(getActivity(), skin) ;

		// Not locked!  Go ahead and make the change.
		mListener.gomfl_setCurrentSkinAndColors(mSkin, mColorScheme) ;
		// Change our preference.
		
		// change our 'resume' button...
		do_refreshColors() ;
		
		// change color schemes...
		this.mOptionsSkinView.setColorScheme(mColorScheme) ;
		this.mOptionsBackgroundView.setColorScheme(mColorScheme) ;
		this.mOptionsMusicSoundView.setColorScheme(mColorScheme) ;
		this.mOptionsControlsView.setColorScheme(mColorScheme) ;
		this.mOptionsSkinView.refresh() ;
		this.mOptionsBackgroundView.refresh() ;
		this.mOptionsMusicSoundView.refresh();
		this.mOptionsControlsView.refresh() ;
		
		// make the skin change.
		osv.setCurrentSkin(skin) ;
	}
	
	
	@Override
	public void osvd_userAdvancedConfiguration(OptionsSkinView obv) {
		QuantroPreferences.launchActivity(
				((QuantroActivity)getActivity()),
				QuantroPreferences.Section.SKIN) ;
	}
	
	
	////////////////////////////////////////////////////////
	// Background Options


	@Override
	public boolean obvd_userSetBackgroundShuffling(OptionsBackgroundView obv,
			boolean shuffling) {
		
		QuantroPreferences.setBackgroundShuffles(getActivity(), shuffling) ;
		
		if ( shuffling ) {
			// the call to set QuantroPreferences places the current background
			// in the rotation automatically.  Do that too.
			mBackgroundShuffle.add( mBackground ) ;
			obv.setBackgroundShuffled(mBackground, true) ;
		}
		obv.setBackgroundShuffling(shuffling) ;
		obv.refresh() ;
		
		if ( shuffling )
			mListener.gomfl_setBackgroundsInShuffle(mBackgroundShuffle) ;
		mListener.gomfl_setBackgroundShuffles(shuffling) ;
		
		return true ;
		
	}

	@Override
	public void obvd_userSetCurrentBackground(OptionsBackgroundView obv,
			Background background, OptionAvailability availability) {
		
		Activity activity = getActivity() ;
		
		mBackground = background ;
		mListener.gomfl_setCurrentBackground(background) ;
		// Change our preference.
		QuantroPreferences.setBackgroundCurrent(getActivity(), background) ;
		// return true: we have made the change.
		obv.setCurrentBackground(background) ;
	}
	
	@Override
	public void obvd_userSetCurrentBackgroundAndIncludeInShuffle(OptionsBackgroundView obv,
			Background background, OptionAvailability availability) {
		Activity activity = getActivity() ;

		// Not locked!  Go ahead and make the change.
		mBackground = background ;
		if ( !mBackgroundShuffle.contains(background) ) {
			mBackgroundShuffle.add(background) ;
			mListener.gomfl_setBackgroundsInShuffle(mBackgroundShuffle) ;
		}
		mListener.gomfl_setCurrentBackground(background) ;
		
		// Change our preference.
		QuantroPreferences.setBackgroundCurrent(activity, background) ;
		QuantroPreferences.setBackgroundInShuffle(activity, background, true) ;
		
		// return true: we have made the change.
		obv.setCurrentBackground(background) ;
		obv.setBackgroundShuffled(background, true) ;
	}
	

	@Override
	public void obvd_userSetBackgroundShuffled(OptionsBackgroundView obv,
			Background background, boolean inRotation,
			OptionAvailability availability) {
		
		Activity activity = getActivity() ;
		
		if ( mBackgroundShuffle.contains(background) != inRotation ) {
			
			if ( inRotation )
				mBackgroundShuffle.add(background) ;
			else
				mBackgroundShuffle.remove(background) ;
			
			// change the current background -- if we need to.
			if ( !mBackgroundShuffle.contains(this.mBackground) ) {
				// set to a random element.
				if ( mBackgroundShuffle.size() == 0 )
					mBackgroundShuffle.add(
							Background.get(Background.Template.NONE,
							Background.Shade.BLACK)) ;
				mBackground = mBackgroundShuffle.iterator().next() ;
				
				mListener.gomfl_setCurrentBackground(mBackground) ;
				QuantroPreferences.setBackgroundCurrent(activity, mBackground) ;
				obv.setCurrentBackground(mBackground) ;
			}
			
			mListener.gomfl_setBackgroundsInShuffle(mBackgroundShuffle) ;
			// Change our preference.
			QuantroPreferences.setBackgroundInShuffle(getActivity(), background, inRotation) ;
		}
		
		// this call automatically refreshes the background view(s)
		obv.setBackgroundShuffled(background, mBackgroundShuffle.contains(background)) ;
	}
	
	@Override
	public void obvd_userAdvancedConfiguration(OptionsBackgroundView obv) {
		QuantroPreferences.launchActivity(
				((QuantroActivity)getActivity()),
				QuantroPreferences.Section.BACKGROUND) ;
	}
	
	
	////////////////////////////////////////////////////////
	// MUSIC SOUND VIEW 
	

	@Override
	public void osmv_userSetCurrentMusic(OptionsSoundMusicView osmv,
			Music music, OptionAvailability availability) {
		
		Activity activity = getActivity() ;
		
		mMusic = music ;
		mListener.gomfl_setCurrentMusic(music) ;
		// Now update the osmv.  This call 'refreshes' automatically;
		// at least, the altered sections refresh.
		osmv.setCurrentMusic(mMusic) ;
		
	}
	
	@Override
	public void osmv_userSetDefaultMusic( OptionsSoundMusicView osmv, Music music, OptionAvailability availability ) {
		
		// TODO: Set this music track as the default track for this game mode
		// -- whatever that means.  May generalize to "game style" --
		// i.e., 3D Endurance games, for example.
		
	}
	

	@Override
	public void osmv_userSetSoundOn(OptionsSoundMusicView osmv, boolean on) {
		Activity activity = getActivity() ;
		
		// Behavior:
		//		We want to set the sound behavior to 'on' as simply as possible.
		//		In short, this means that if sounds were previously muted (for ANY REASON),
		//			we perform the minimum number of changes to unmute.
		//			If currently muted by ringer, set muteWithRinger -> false.
		//			Then, if muted, set sound ON.
		//		If sounds were previously playing, and we're setting it false, just
		//			turn sounds off.  Don't affect ringer setting.
		boolean performChange = false ;
		if ( on ) {
			// minimum change
			if ( mSoundPool.isMutedByAnything() ) {
				performChange = true ;
				if ( mSoundPool.isMutedByRinger() ) {
					QuantroPreferences.setMuteWithRinger(activity, false) ;
				}
				if ( mSoundPool.isMuted() ) {
					QuantroPreferences.setMuted(activity, false) ;
				}
			}
		} else {
			if ( !mSoundPool.isMutedByAnything() ) {
				performChange = true ;
				QuantroPreferences.setMuted(activity, true) ;
			}
		}
		
		if ( performChange ) {
			mOptionsMusicSoundView.setSoundOn( !mSoundPool.isMuted(), mSoundPool.isMutedByRinger() ) ;
			mOptionsMusicSoundView.refresh() ;
		}
	}

	@Override
	public boolean osmv_userSetMusicVolumePercent(OptionsSoundMusicView osmv,
			int volPercent) {
		
		Activity activity = getActivity() ;
		QuantroPreferences.setVolumeMusicPercent(activity, volPercent) ;
		return true ;
	}

	@Override
	public boolean osmv_userSetSoundVolumePercent(OptionsSoundMusicView osmv,
			int volPercent) {
		
		Activity activity = getActivity() ;
		QuantroPreferences.setVolumeSoundPercent(activity, volPercent) ;
		return true ;
	}

	@Override
	public void osmv_userAdvancedConfiguration(OptionsSoundMusicView osmv) {
		// open advanced config.
		QuantroPreferences.launchActivity(
				((QuantroActivity)getActivity()),
				QuantroPreferences.Section.SOUND) ;
	}
	
	
	////////////////////////////////////////////////////////
	// CONTROLS VIEW
	

	@Override
	public void ocvd_userSetControlsGamepad(OptionsControlsView ocv) {
		Activity activity = getActivity() ;
		// Inform the Listener, then make the necessary Preferences changes.
		if ( QuantroPreferences.getControls(activity) == QuantroPreferences.CONTROLS_GESTURE ) {
			// switch to gamepad.  Make preference changes, inform the OptionsView,
			// and inform the Listener.
			QuantroPreferences.setControls(activity, QuantroPreferences.CONTROLS_GAMEPAD) ;
			// inform the view...
			ocv.setControlsThumbnail(null) ;
			ocv.setControlsGamepad(
					QuantroPreferences.getControlsGamepadQuickSlide(activity),
					QuantroPreferences.getControlsGamepadDropButton(activity) == QuantroPreferences.CONTROLS_DROP_FALL,
	        		QuantroPreferences.getControlsGamepadDoubleDownDrop(activity) ) ;
			ocv.refresh() ;
			// inform the Listener (this will, eventually, produce a new thumbnail)
			mListener.gomfl_setCurrentControlsGamepad() ;
		}
	}

	@Override
	public void ocvd_userSetControlsGesture(OptionsControlsView ocv) {
		Activity activity = getActivity() ;
		// Inform the Listener, then make the necessary Preferences changes.
		if ( QuantroPreferences.getControls(activity) == QuantroPreferences.CONTROLS_GAMEPAD ) {
			// switch to gamepad.  Make preference changes, inform the OptionsView,
			// and inform the Listener.
			QuantroPreferences.setControls(activity, QuantroPreferences.CONTROLS_GESTURE) ;
			// inform the view...
			ocv.setControlsThumbnail(null) ;
			ocv.setControlsGesture(
					QuantroPreferences.getControlsGestureQuickSlide(activity),
					QuantroPreferences.getControlsGestureTurnButtons(activity),
					QuantroPreferences.getControlsGestureDragDownAutolock(activity)) ;
			ocv.refresh() ;
			// inform the Listener (this will, eventually, produce a new thumbnail)
			mListener.gomfl_setCurrentControlsGesture() ;
		}
	}

	@Override
	public void ocvd_userAdvancedConfiguration(OptionsControlsView ocv) {
		QuantroPreferences.launchActivity(
				((QuantroActivity)getActivity()),
				QuantroPreferences.Section.CONTROLS) ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// PERSONAL VIEW DELEGATE CALLBACKS
	//
	
	@Override
	public boolean customButtonStrip_onButtonClick(CustomButtonStrip strip,
			int buttonNum, String name, boolean asOverflow ) {
		
		if ( ACTION_NAME_RESUME.equals(name) ) {
			if ( mSoundControls && mSoundPool != null && !asOverflow )
				mSoundPool.menuButtonClick() ;
			do_resumeGame() ;
			return true ;
		} else if ( ACTION_NAME_SETTINGS.equals(name) ) {
			if ( mSoundControls && mSoundPool != null && !asOverflow )
				mSoundPool.menuButtonClick() ;
			QuantroPreferences.launchActivity((QuantroActivity)getActivity(), null) ;
			return true ;
		}
		
		return false;
	}

	@Override
	public boolean customButtonStrip_onButtonLongClick(CustomButtonStrip strip,
			int buttonNum, String name) {
		// NO LONG PRESS!
		return false;
	}

	@Override
	public boolean customButtonStrip_supportsLongClick(CustomButtonStrip strip,
			int buttonNum, String name) {
		// NO LONG PRESS!
		return false;
	}
	
	@Override
	public void customButtonStrip_onPopupOpen(
			CustomButtonStrip strip ) {
		if ( mSoundPool != null && mSoundControls )
			mSoundPool.menuButtonClick() ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// MENU ITEM ADAPTER AND DELEGATE METHODS
	
	////////////////////////////////////////////////////////////////////////////
    // ARRAY ADAPTERS
    // Used to populate our ListViews.
	
	private abstract class MenuItemIndexer implements SectionIndexer {
		
		public abstract boolean isFirstInSection(int position) ;
		
		public abstract int numSections() ;
	}
    
	private static class MenuItemListRowTag {
		View mHeaderView ;
		View mHeaderViewTopSpacer ;
		TextView mHeaderTextView ;
		
		public MenuItemListRowTag( View v ) {
			mHeaderView = v.findViewById(R.id.quantro_list_item_header) ;
			mHeaderViewTopSpacer = v.findViewById(R.id.quantro_list_item_header_top_spacer) ;
			mHeaderTextView = (TextView)v.findViewById(R.id.quantro_list_item_header_text) ;
		}
	}
	
	
	private static class MenuItemListItemTag {
		MainMenuButtonStrip mMBS ;
		
		public MenuItemListItemTag( View v ) {
			mMBS = (MainMenuButtonStrip) v.findViewById( R.id.main_menu_button_strip ) ;
		}
	}
	
    
    private class MenuItemArrayAdapter extends SectionableAdapter
    		implements OnScrollListener, PinnedHeaderListView.PinnedHeaderAdapter, SectionIndexer {   	
    	
    	int [] mMenuItems ;
    	MenuItemIndexer mIndexer ;
    	Hashtable<Integer, MainMenuButtonStrip> views = new Hashtable<Integer, MainMenuButtonStrip>() ;
    	
    	boolean mScrolledToTop ;
    	
    	public MenuItemArrayAdapter(LayoutInflater inflater,
				int rowLayoutID, int headerID, int itemHolderID) {
    		super(inflater, rowLayoutID, headerID, itemHolderID,
    				SectionableAdapter.CellVisibility.GONE_IF_FIRST_ROW_IN_SECTION);
			
			mMenuItems = new int[0] ;
			mIndexer = null ;
			views = new Hashtable<Integer, MainMenuButtonStrip>() ;
			
			mScrolledToTop = true ;
    	}
    	
    	////////////////////////////////////////////////////////////////////////
    	// Setting Contents
    	// Our operation is complex: we configure category headers, contents, etc.
    	// using our own calculations.  All we need is the "root item" which we are
    	// representing: we read from mMenuHierarchy and mMenuHierarchyCategory from
    	// that data.
    	
    	synchronized public void setItems( int [] items ) {
    		mMenuItems = items.clone() ;
    		mIndexer = null ;
    		views.clear() ;
    		
    		mScrolledToTop = true ;
    		notifyDataSetChanged() ;
    	}
    	
    	
    	synchronized public void refreshMenuItem( Integer menuItem ) {
    		//Log.d(TAG, "MenuItemArrayAdapter.refreshMenuItem bracket IN") ;
    		if ( views.containsKey(menuItem) ) {
    			// Log.d(TAG, "MenuItemArrayAdapter, contains " + menuItem + " doing refresh.") ;
    			views.get(menuItem).refresh() ;
    			this.notifyDataSetChanged() ;
    		}
    		//Log.d(TAG, "MenuItemArrayAdapter.refreshMenuItem bracket OUT") ;
    	}
    	
    	synchronized public int getPositionForSection(int sectionIndex) {
            if (mIndexer == null) {
                return -1;
            }

            // this method is called by PinnedHeaderListView, which -- as
            // far as it is aware -- is indexing ROWS, not entries.
            // Perform a conversion.
            return this.getRowPosition( mIndexer.getPositionForSection(sectionIndex) );
        }
		
		synchronized public int getSectionForPosition(int position) {
            if (mIndexer == null) {
                return -1;
            }

            // this method is called by PinnedHeaderListView, which -- as
            // far as it is aware -- is indexing ROWS, not entries.
            // Perform a conversion.
            return mIndexer.getSectionForPosition( getRealPosition(position) );
        }
		
		@Override
		synchronized public Object[] getSections() {
            if (mIndexer == null) {
                return new String[] { " " };
            } else {
                return mIndexer.getSections();
            }
		}
		
		
		@Override
		synchronized public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			View topChild = view.getChildAt(0) ;
		    MenuItemListRowTag tag = null ;
			if ( topChild != null )
				tag = ((MenuItemListRowTag)topChild.getTag()) ;
			
			mScrolledToTop = firstVisibleItem == 0 && visibleItemCount > 0 && topChild.getTop() == 0 ;
			
			if (view instanceof PinnedHeaderListView && mIndexer != null) {
				boolean topHasHeader = false ; // mIndexer.isFirstInSection(firstVisibleItem) ;
				// Previously: we assumed that headers have a fixed size, and thus the
				// moment when one header reaches the top of the screen is the moment 
				// when the previous header has vanished.  However, we have started
				// experimenting with variable-height headers: specifically, the pinned
				// header (and top header) is short, w/out much spacing, while in-list
				// headers after the first have a large amount of leading padding.
				// We only present the current position if its EFFECTIVE header has
				// reached the top of the screen.
				// For quantro_list_item_header, this is the case when topChild.y +
				// the in-child y position of the header is <= 0.
				boolean headerNotYetInPosition = ( tag != null ) && topHasHeader && firstVisibleItem != 0
				&& ( topChild.getTop() + tag.mHeaderTextView.getTop() > 0 ) ;
				
                ((PinnedHeaderListView) view).configureHeaderView(
                		headerNotYetInPosition ? firstVisibleItem -1 : firstVisibleItem,
                		!headerNotYetInPosition );
            }	
		}

		@Override
		synchronized public void onScrollStateChanged(AbsListView arg0, int arg1) {
		}

		@Override
		synchronized public int getPinnedHeaderState(int position) {
            if (mIndexer == null || getCount() == 0) {
                return PINNED_HEADER_STATE_GONE;
            }

            if (position < 0) {
                return PINNED_HEADER_STATE_GONE;
            }
            
            if ( position == 0 && mScrolledToTop )
            	return PINNED_HEADER_STATE_GONE ;

            // The header should get pushed up if the top item shown
            // is the last item in a section for a particular letter.
            int section = getSectionForPosition(position);
            if ( section < 0 )
            	return PINNED_HEADER_STATE_GONE ;
            
            int nextSectionPosition = getPositionForSection(section + 1);
            
            if (nextSectionPosition != -1 && position == nextSectionPosition - 1) {
                return PINNED_HEADER_STATE_PUSHED_UP;
            }

            return PINNED_HEADER_STATE_VISIBLE;
		}
		
		@Override
        public int pha_getPinnedHeaderFadeAlphaStyle() {
			return PinnedHeaderListView.PinnedHeaderAdapter.PINNED_HEADER_FADE_ALPHA_HEADER_NONCONTENT_OVERLAP ;
		}
        
        
		Rect tempPaddingRect = new Rect() ;
        
		@Override
        public boolean copyPinnedHeaderOccludingBounds(View header, int position, Rect r) {
			MenuItemListRowTag tag = (MenuItemListRowTag)header.getTag() ;
        	Drawable bg = tag.mHeaderTextView.getBackground() ;
        	bg.getPadding(tempPaddingRect) ;
        	r.set(0, tag.mHeaderTextView.getTop() + tempPaddingRect.top,
        			header.getWidth(), tag.mHeaderTextView.getBottom() - tempPaddingRect.bottom) ;
        	return true ;
        }
        
        @Override
        public boolean copyPinnedHeaderVisibleBounds(View header, int position, Rect r) {
        	MenuItemListRowTag tag = (MenuItemListRowTag)header.getTag() ;
        	r.set(0, tag.mHeaderTextView.getTop(),
        			header.getWidth(), tag.mHeaderTextView.getBottom()) ;
        	return true ;
        }
        
        
        @Override
        public boolean copyHeaderOccludingBounds(View view, int position, Rect r) {
        	MenuItemListRowTag tag = (MenuItemListRowTag)view.getTag() ;
        	if ( tag.mHeaderView.getVisibility() == View.GONE ) {
        		r.set(0, 0, 0, 0) ;
        		return true ;
        	}
        	
        	return copyPinnedHeaderOccludingBounds( tag.mHeaderView, position, r ) ;
        }
        
        @Override
        public boolean copyHeaderVisibleBounds(View view, int position, Rect r) {
        	MenuItemListRowTag tag = (MenuItemListRowTag)view.getTag() ;
        	if ( tag.mHeaderView.getVisibility() == View.GONE ) {
        		r.set(0, 0, 0, 0) ;
        		return true ;
        	}
        	
        	return copyPinnedHeaderVisibleBounds( tag.mHeaderView, position, r ) ;
        }
		
		@Override
		synchronized public void configurePinnedHeader(View v, int position, int alpha) {
			final int section = getSectionForPosition(position);
			if ( section >= 0 ) {
				final String title = (String) getSections()[section];
				
				MenuItemListRowTag tag = (MenuItemListRowTag)v.getTag() ;
				tag.mHeaderTextView.setText(title);
				tag.mHeaderViewTopSpacer.setVisibility(View.GONE) ;
				VersionSafe.setAlpha(v, alpha / 255f) ;
			}
		}
		
		@Override
        synchronized public int nextHeaderAfter(int position) {
			int section = getSectionForPosition( position ) ;
			if ( section == -1 )
				return -1 ;
			
			return getPositionForSection(section+1) ;
		}
		
		
		@Override
		public Object getItem(int position) {
			return this.mMenuItems[position] ;
		}


		@Override
		protected int getDataCount() {
			return this.mMenuItems.length ;
		}


		@Override
		protected int getSectionsCount() {
			// NOTE: the current implementation of SectionableAdapter
			// calls this method exactly once, so we can't adjust
			// the sections over time (e.g. a new section for specific
			// custom game modes).  Consider how to change and/or implement
			// this if we need adaptable section numbers.  We might not,
			// even if we add/remove sections, so long as we can bound the
			// number of sections in advance.
			if ( mIndexer == null )
				return 1 ;
			return mIndexer.numSections() ;
		}


		@Override
		protected int getCountInSection(int index) {
			if ( mIndexer == null )
				return mMenuItems.length ;
			
			// returns the number of items within the specified section.
			// this is the difference between getPositionForSection(index+1)
			// and getPositionForSection(index).  getPositionForSection will
			// return the total number of items if called with a section index
			// that is out of bounds.
			
			// note that our implementation of getPositionForSection works
			// on the View (row) level, whereas our indexer works on the item
			// (real position) level.
			return mIndexer.getPositionForSection(index+1) - mIndexer.getPositionForSection(index) ;
		}
		
		
		@Override
		protected int getTypeFor(int position) {
			// called by SectionableAdapter; uses real-positions.
			if ( mIndexer == null )
				return 0 ;
			
			return mIndexer.getSectionForPosition(position) ;
		}


		@Override
		protected String getHeaderForSection(int section) {
			return null ;
		}
		
		
		@Override
		protected void bindView(View cell, int position) {
			int menuItem = mMenuItems[position] ;
			
			MenuItemListItemTag tag = (MenuItemListItemTag)cell.getTag() ;
    		if ( tag == null ) {
				tag = new MenuItemListItemTag(cell) ;
				if ( tag.mMBS != null ) {
					tag.mMBS.setMenuItemTitles(mMenuItemTitle) ;
					tag.mMBS.setMenuItemDescriptions(mMenuItemDescription) ;
					tag.mMBS.setMenuItemColors(mMenuItemColor) ;
					tag.mMBS.setDelegate(GameOptionsMenuFragment.this) ;
					
				} 
				cell.setTag(tag) ;
			}
    		
			boolean enabled = mMenuItemActive[menuItem] ;
			
			// Set menu item.  This call automatically refreshes.
			if ( tag.mMBS != null ) {
				tag.mMBS.setMenuItemEnabled(enabled) ;
				tag.mMBS.setMenuItem(menuItem) ;
				
				// adjust view in the hashtable
				Set<Entry<Integer, MainMenuButtonStrip>> entrySet = views.entrySet() ;
				Iterator<Entry<Integer, MainMenuButtonStrip>> iter = entrySet.iterator() ;
				for ( ; iter.hasNext() ; ) {
					Entry<Integer, MainMenuButtonStrip> entry = iter.next() ;
					if ( entry.getValue() == tag.mMBS )
						iter.remove() ;
				}
				views.put(menuItem, tag.mMBS) ;
			}
			
			// set height to ideal height.
			if ( tag.mMBS != null )
				tag.mMBS.getLayoutParams().height = tag.mMBS.getIdealHeight() ;
		}
		
		
		
		/**
		 * Perform any row-specific customization your grid requires. For example, you could add a header to the
		 * first row or a footer to the last row.
		 * @param row the 0-based index of the row to customize.
		 * @param rowView the inflated row View.
		 */
		@Override
		protected void customizeRow(int row, int firstPosition, View rowView) {
			// This is where we perform necessary header configuration.
			
			MenuItemListRowTag tag = ((MenuItemListRowTag)rowView.getTag()) ;
			if ( tag == null ) {
				tag = new MenuItemListRowTag( rowView ) ;
				rowView.setTag(tag) ;
				if ( tag.mHeaderView != null )
					tag.mHeaderView.setTag(tag) ;
			}
        	
	        final int section = getSectionForPosition(row);
	        final int sectionPosition = getPositionForSection(section) ;
	        if (section >= 0 && sectionPosition == row) {
	            String title = (String) mIndexer.getSections()[section];
	            tag.mHeaderTextView.setText(title);
	            tag.mHeaderView.setVisibility(View.VISIBLE);
		    	// the first item does not get a spacer; the rest of the headers do.
	            tag.mHeaderViewTopSpacer.setVisibility( firstPosition == 0 ? View.GONE : View.VISIBLE ) ;
	        } else {
	        	tag.mHeaderView.setVisibility(View.GONE);
		    	//dividerView.setVisibility(View.VISIBLE);
	        }
		}
    	
    }

	@Override
	public boolean mmbs_onButtonClick(MainMenuButtonStrip strip, int buttonNum,
			int buttonType, int menuItemNumber, boolean buttonEnabled) {
		
		boolean didAction = false ;
		
		switch( menuItemNumber ) {
		case MENU_ITEM_RESUME:
			do_resumeGame() ;
			didAction = true ;
			break ;
		case MENU_ITEM_SKIN:
			show( SECTION_SKIN ) ;
			didAction = true ;
			break ;
		case MENU_ITEM_BACKGROUND:
			show( SECTION_BACKGROUND ) ;
			didAction = true ;
			break ;
		case MENU_ITEM_MUSIC_SOUND:
			show( SECTION_MUSIC_SOUND ) ;
			didAction = true ;
			break ;
		case MENU_ITEM_CONTROLS:
			show( SECTION_CONTROLS ) ;
			didAction = true ;
			break ;
		case MENU_ITEM_ADVANCED:
			QuantroPreferences.launchActivity((QuantroActivity)getActivity(), null) ;
			didAction = true ;
			break ;
		case MENU_ITEM_QUIT:
			mListener.gomfl_quit() ;
			didAction = true ;
			break ;
		}
		
		if ( didAction && mSoundControls && mSoundPool != null )
			mSoundPool.menuButtonClick() ;
		return didAction;
	}

	@Override
	public boolean mmbs_onButtonLongClick(MainMenuButtonStrip strip,
			int buttonNum, int buttonType, int menuItemNumber,
			boolean buttonEnabled) {
		// No long-press
		return false;
	}

	@Override
	public boolean mmbs_supportsLongClick(MainMenuButtonStrip strip,
			int buttonNum, int buttonType, int menuItemNumber,
			boolean buttonEnabled) {
		// No long-press
		return false;
	}
    
    //
    ////////////////////////////////////////////////////////////////////////////

}
