package com.peaceray.quantro.view.button.strip;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.peaceray.quantro.R;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.lobby.WebConsts;
import com.peaceray.quantro.lobby.WiFiLobbyDetails;
import com.peaceray.quantro.view.button.content.QuantroButtonAccess;

public class LobbyButtonStrip extends QuantroButtonStrip implements QuantroButtonStrip.Controller {

	public interface Delegate {
		/**
		 * The user has short-clicked a button we for game launches.  The
		 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
		 * the right action.
		 * 
		 * Provides either a lobby description or a lobby (never both).  Check which is
		 * 'null' before proceeding.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param buttonType
		 * @param gameMode
		 * @param saveKey
		 */
		public boolean lbs_onButtonClick(
				LobbyButtonStrip strip,
				int buttonNum, int buttonType, int lobbyType, WiFiLobbyDetails lobbyDescription, Lobby lobby ) ;
		
		/**
		 * The user has long-clicked a button we for game launches.  The
		 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
		 * the right action.
		 * 
		 * Provides either a lobby description or a lobby (never both).  Check which is
		 * 'null' before proceeding.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param buttonType
		 * @param gameMode
		 * @param saveKey
		 */
		public boolean lbs_onButtonLongClick(
				LobbyButtonStrip strip,
				int buttonNum, int buttonType, int lobbyType, WiFiLobbyDetails lobbyDescription, Lobby lobby ) ;
		
		
		
		/**
		 * Do we support long-clicks on this button?
		 * 
		 * This is usually called when the user starts a press, and should be considered
		 * as (basically): if the user keeps pressing, with a long-press activate?
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param buttonType
		 * @param gameMode
		 * @param saveKey
		 */
		public boolean lbs_supportsLongClick(
				LobbyButtonStrip strip,
				int buttonNum, int buttonType, int lobbyType, WiFiLobbyDetails lobbyDescription, Lobby lobby ) ;
	}
	
	
	public LobbyButtonStrip(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
	}
	
	public LobbyButtonStrip(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
	}
	
	
	// we need at least 3 buttons, in the most extreme case:
	// a generic "lobby" button, and a "hide" button.
	private static final int MIN_BUTTONS = 2 ;
	
	public static final int BUTTON_TYPE_NO_LOBBY = 0 ;
	public static final int BUTTON_TYPE_LOBBY_SEARCHING_MAIN = 1 ;
	public static final int BUTTON_TYPE_LOBBY_OPEN_MAIN = 2 ;		// Generic "lobby" button
	public static final int BUTTON_TYPE_LOBBY_FULL_MAIN = 3 ;
	public static final int BUTTON_TYPE_LOBBY_CLOSED_MAIN = 4 ;
	
	public static final int BUTTON_TYPE_SEARCHING_HIDE = 5 ;
	public static final int BUTTON_TYPE_OPEN_HIDE = 6 ;
	public static final int BUTTON_TYPE_FULL_HIDE = 7 ;
	public static final int BUTTON_TYPE_CLOSED_HIDE = 8 ;
	
	public static final int NUM_BUTTON_TYPES = 9 ;
	
	
	public static final int LOBBY_TYPE_WIFI = 0 ;
	public static final int LOBBY_TYPE_INTERNET_PUBLIC = 1 ;
	public static final int LOBBY_TYPE_INTERNET_PRIVATE = 2 ;
	public static final int LOBBY_TYPE_INTERNET_ITINERANT = 3 ;
	
	public static final int NUM_LOBBY_TYPES = 4 ;
	
	
	public static final int LOBBY_STATUS_NONE = -1 ;
	public static final int LOBBY_STATUS_SEARCHING = 0 ;
	public static final int LOBBY_STATUS_OPEN = 1 ;
	public static final int LOBBY_STATUS_FULL = 2 ;
	public static final int LOBBY_STATUS_CLOSED = 3 ;
	
	
	
	// Our strip is configured by a Lobby, given as a description.
	protected WiFiLobbyDetails mWiFiLobbyDetails ;
	protected boolean mWiFiLobbySearching ;
	protected Lobby mLobby ;
	protected int mLobbyStatusOnLastRefresh ;
	
	protected int mLobbyType ;
	
	// Our listener
	protected WeakReference<Delegate> mwrDelegate ;
	
	// Colors!
	protected int [][] mColorButton ;
	// Do we have these colors?
	protected boolean [][] mHasColorButton ;
	// We use the TextFormatting class to configure our text,
	// but our drawable may be given in XML.  We set these to the
	// background of whichever View responds to mTagBackgroundDrawable.
	protected Drawable [][] mBackgroundDrawableButton ;
	// ...and these are for mTagImageDrawable
	protected Drawable [][] mImageDrawableButton ; 
	
	
	private void constructor_setDefaultValues(Context context) {
		// self control
		this.setController(this) ;
		
		mWiFiLobbyDetails = null ;
		mWiFiLobbySearching = false ;
		mwrDelegate = new WeakReference<Delegate>(null) ;
		
		// Initialize our colors and drawables.
		mColorButton = new int [NUM_LOBBY_TYPES][NUM_BUTTON_TYPES] ;
		mHasColorButton = new boolean [NUM_LOBBY_TYPES][NUM_BUTTON_TYPES] ;
		mBackgroundDrawableButton = new Drawable[NUM_LOBBY_TYPES][NUM_BUTTON_TYPES] ;
		mImageDrawableButton = new Drawable[NUM_LOBBY_TYPES][NUM_BUTTON_TYPES] ;
		
		for ( int i = 0; i < NUM_LOBBY_TYPES; i++ ) {
			for ( int j = 0; j < NUM_BUTTON_TYPES; j++ ) {
				mColorButton[i][j] = 0 ;
				mHasColorButton[i][j] = false ;
				mBackgroundDrawableButton[i][j] = null ;
				mImageDrawableButton[i][j] = null ;
			}
		}
	}
	
	private void constructor_readAttributeSet(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.LobbyButtonStrip);
		
		int lobbyType ;
		int buttonType ;
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			////////////////////////////////////////////////////////////////////
			// COLOR! //////////////////////////////////////////////////////////
			case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_main_searching:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_main_open:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_main_full:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_main_closed:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_hide_searching:	
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_hide_open:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_hide_full:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_hide_closed:
		  	
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_main_open:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_main_full:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_main_closed:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_hide_open:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_hide_full:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_hide_closed:
		  	
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_main_open:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_main_full:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_main_closed:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_hide_open:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_hide_full:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_hide_closed:
		  	
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_main_open:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_main_full:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_main_closed:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_hide_open:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_hide_full:
		  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_hide_closed:
		  		lobbyType = attributeToLobbyType(attr) ;
		  		buttonType = attributeToButtonType(attr) ;
		  		mColorButton[lobbyType][buttonType] = a.getColor(attr, 0) ;
		  		mHasColorButton[lobbyType][buttonType] = true ;
		  		break ;
		  		
		  	
			
			////////////////////////////////////////////////////////////////////
			// BACKGROUND DRAWABLE! ////////////////////////////////////////////
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_main_searching:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_main_open:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_main_full:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_main_closed:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_hide_searching:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_hide_open:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_hide_full:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_hide_closed:
		  	
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_main_open:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_main_full:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_main_closed:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_hide_open:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_hide_full:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_hide_closed:
		  	
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_main_open:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_main_full:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_main_closed:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_hide_open:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_hide_full:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_hide_closed:
		  	
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_main_open:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_main_full:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_main_closed:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_hide_open:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_hide_full:
		  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_hide_closed:
		  		lobbyType = attributeToLobbyType(attr) ;
		  		buttonType = attributeToButtonType(attr) ;
		  		mBackgroundDrawableButton[lobbyType][buttonType] = a.getDrawable(attr) ;
		  		break ;
		  		
		  	
				
			////////////////////////////////////////////////////////////////////
			// IMAGE DRAWABLE! /////////////////////////////////////////////////
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_main_searching:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_main_open:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_main_full:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_main_closed:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_hide_searching:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_hide_open:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_hide_full:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_hide_closed:
		  	
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_main_open:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_main_full:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_main_closed:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_hide_open:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_hide_full:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_hide_closed:
		  	
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_main_open:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_main_full:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_main_closed:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_hide_open:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_hide_full:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_hide_closed:
		  	
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_main_open:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_main_full:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_main_closed:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_hide_open:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_hide_full:
		  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_hide_closed:
		  		lobbyType = attributeToLobbyType(attr) ;
		  		buttonType = attributeToButtonType(attr) ;
		  		mImageDrawableButton[lobbyType][buttonType] = a.getDrawable(attr) ;
		  		break ;
			}
		}
		
		// recycle the array; we don't need it anymore.
		a.recycle() ;
	}
	
	private int attributeToLobbyType( int attribute ) {
		switch( attribute ) {
		// BASE COLOR
		case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_main_searching:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_hide_searching:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_hide_closed:
	  		return LOBBY_TYPE_WIFI ;
	  	
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_hide_closed:
	  		return LOBBY_TYPE_INTERNET_PUBLIC ;
	  	
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_hide_closed:
	  		return LOBBY_TYPE_INTERNET_PRIVATE ;
	  	
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_hide_closed:
	  		return LOBBY_TYPE_INTERNET_ITINERANT ;
	  		
	  	// BACKGROUND DRAWABLE
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_main_searching:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_hide_searching:	
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_hide_closed:
	  		return LOBBY_TYPE_WIFI ;
	  	
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_hide_closed:
	  		return LOBBY_TYPE_INTERNET_PUBLIC ;
	  	
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_hide_closed:
	  		return LOBBY_TYPE_INTERNET_PRIVATE ;
	  	
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_hide_closed:
	  		return LOBBY_TYPE_INTERNET_ITINERANT ;
	  		
	  	// IMAGE DRAWABLE
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_main_searching:	
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_hide_searching:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_hide_closed:
	  		return LOBBY_TYPE_WIFI ;
	  	
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_hide_closed:
	  		return LOBBY_TYPE_INTERNET_PUBLIC ;
	  	
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_hide_closed:
	  		return LOBBY_TYPE_INTERNET_PRIVATE ;
	  	
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_hide_closed:
	  		return LOBBY_TYPE_INTERNET_ITINERANT ;
	  		
  		default:
  			return -1 ;
		}
	}

	private int attributeToButtonType( int attribute ) {
		switch( attribute ) {
		// BASE COLOR
		case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_main_searching:
			return BUTTON_TYPE_LOBBY_SEARCHING_MAIN ;
		
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_main_open:
	  		return BUTTON_TYPE_LOBBY_OPEN_MAIN ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_main_full:
	  		return BUTTON_TYPE_LOBBY_FULL_MAIN ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_main_closed:
	  		return BUTTON_TYPE_LOBBY_CLOSED_MAIN ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_hide_searching:
			return BUTTON_TYPE_SEARCHING_HIDE ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_hide_open:
	  		return BUTTON_TYPE_OPEN_HIDE ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_hide_full:
	  		return BUTTON_TYPE_FULL_HIDE ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_wifi_hide_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_public_hide_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_private_hide_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_base_color_button_internet_itinerant_hide_closed:
	  		return BUTTON_TYPE_CLOSED_HIDE ;
	  		
	  	// BACKGROUND DRAWABLE
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_main_searching:
			return BUTTON_TYPE_LOBBY_SEARCHING_MAIN ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_main_open:
	  		return BUTTON_TYPE_LOBBY_OPEN_MAIN ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_main_full:
	  		return BUTTON_TYPE_LOBBY_FULL_MAIN ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_main_closed:
	  		return BUTTON_TYPE_LOBBY_CLOSED_MAIN ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_hide_searching:
			return BUTTON_TYPE_SEARCHING_HIDE ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_hide_open:
	  		return BUTTON_TYPE_OPEN_HIDE ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_hide_full:
	  		return BUTTON_TYPE_FULL_HIDE ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_wifi_hide_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_public_hide_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_private_hide_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_background_drawable_button_internet_itinerant_hide_closed:
	  		return BUTTON_TYPE_CLOSED_HIDE ;
	  		
	  	// IMAGE DRAWABLE
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_main_searching:
			return BUTTON_TYPE_LOBBY_SEARCHING_MAIN ;
			
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_main_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_main_open:
	  		return BUTTON_TYPE_LOBBY_OPEN_MAIN ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_main_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_main_full:
	  		return BUTTON_TYPE_LOBBY_FULL_MAIN ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_main_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_main_closed:
	  		return BUTTON_TYPE_LOBBY_CLOSED_MAIN ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_hide_searching:
			return BUTTON_TYPE_SEARCHING_HIDE ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_hide_open:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_hide_open:
	  		return BUTTON_TYPE_OPEN_HIDE ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_hide_full:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_hide_full:
	  		return BUTTON_TYPE_FULL_HIDE ;
	  		
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_wifi_hide_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_public_hide_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_private_hide_closed:
	  	case R.styleable.LobbyButtonStrip_lbs_image_drawable_button_internet_itinerant_hide_closed:
	  		return BUTTON_TYPE_CLOSED_HIDE ;
	  		
	  	
	  	
		default:
			return -1 ;
		}
	}
	
	synchronized public void setDelegate( LobbyButtonStrip.Delegate delegate ) {
		this.mwrDelegate = new WeakReference<Delegate>(delegate) ;
	}
	
	synchronized public void setWiFiLobbyDetails( WiFiLobbyDetails lobbyDescription, boolean searching ) {
		// We refresh after this.
		this.mWiFiLobbyDetails = lobbyDescription ;
		this.mWiFiLobbySearching = searching ;
		this.mLobby = mWiFiLobbyDetails == null ? mLobby : null ;
		this.mLobbyType = LOBBY_TYPE_WIFI ;
		this.refresh() ;
	}
	
	synchronized public WiFiLobbyDetails getWiFiLobbyDetails() {
		return this.mWiFiLobbyDetails ;
	}
	
	synchronized public void setWiFiLobbySearching( boolean searching ) {
		this.mWiFiLobbySearching = searching ;
		this.refresh() ;
	}
	
	synchronized public boolean getWiFiLobbySearching() {
		return this.mWiFiLobbySearching ;
	}
	
	synchronized public void setLobby( Lobby lobby ) {
		// We refresh after this.
		this.mLobby = lobby ;
		this.mWiFiLobbyDetails = mLobby == null ? mWiFiLobbyDetails : null ;
		
		if ( !(lobby instanceof InternetLobby) )
			this.mLobbyType = LOBBY_TYPE_WIFI ;
		else {
			InternetLobby il = (InternetLobby)lobby ;
			if ( il.isPublic() )
				this.mLobbyType = LOBBY_TYPE_INTERNET_PUBLIC ;
			else if ( il.isItinerant() )
				this.mLobbyType = LOBBY_TYPE_INTERNET_ITINERANT ;
			else
				this.mLobbyType = LOBBY_TYPE_INTERNET_PRIVATE ;
		}
		
		this.refresh() ;
	}
	
	synchronized public Lobby getLobby() {
		return this.mLobby ;
	}
	
	
	
	
	@Override
	synchronized public void refresh() {
		boolean changed = false ;
		
		// Make sure we have enough buttons!
		while ( this.getButtonCount() < MIN_BUTTONS ) {
			changed = true ;
			addButton() ;
		}
		
		int lobbyStatus = LOBBY_STATUS_NONE ;
		
		// There are 3 basic styles: no lobby, an open lobby, and a full lobby.
		
		if ( mWiFiLobbyDetails == null && mLobby == null )
			changed = setStripContentToNone() || changed ;
		else {
			boolean searching = false, open = false, full = false, closed = false ;
			if ( mWiFiLobbyDetails != null ) {
				searching = mWiFiLobbySearching ;
				if ( !searching ) {
					open = mWiFiLobbyDetails.getReceivedStatus().getNumPeople() < mWiFiLobbyDetails.getReceivedStatus().getMaxPeople() ;
					full = !open ;
				}
			} else if ( !( mLobby instanceof InternetLobby ) ){
				open = mLobby.getNumPeople() < mLobby.getMaxPeople() ;
				full = !open ;
			} else {
				InternetLobby il = (InternetLobby) mLobby ;
				int status = il.getStatus() ;
				open = ( status == WebConsts.STATUS_EMPTY || status == WebConsts.STATUS_OPEN )
						&& il.getHostedPlayers() < il.getMaxPeople() ;
				full = ( status == WebConsts.STATUS_EMPTY || status == WebConsts.STATUS_OPEN )
						&& il.getHostedPlayers() >= il.getMaxPeople() ;
				closed = !open && !full ;
			}
			
			if ( searching ) {
				changed = setStripContentToSearchingLobby() || changed ;
				lobbyStatus = LOBBY_STATUS_SEARCHING ;
			} else if ( open ) {
				changed = setStripContentToOpenLobby() || changed ;
				lobbyStatus = LOBBY_STATUS_OPEN ;
			} else if ( full ) {
				changed = setStripContentToFullLobby() || changed ;
				lobbyStatus = LOBBY_STATUS_FULL ;
			} else if ( closed ) {
				changed = setStripContentToClosedLobby() || changed ;
				lobbyStatus = LOBBY_STATUS_CLOSED ;
			} else
				changed = setStripContentToNone() || changed ;
		}
		
		changed = lobbyStatus != mLobbyStatusOnLastRefresh ;
		mLobbyStatusOnLastRefresh = lobbyStatus ;
		
		if ( changed )
			super.refresh() ;
	}
	
	
	private boolean setStripContentToNone() {
		for ( int i = 1; i < getButtonCount(); i++ )
			setButtonVisible( i, false ) ;
		boolean changed = false ;
		changed = setButtonContent( 0, -1 ) || changed ;
		
		return changed ;
	}
	
	private boolean setStripContentToSearchingLobby() {
		for ( int i = 2; i < getButtonCount(); i++ )
			setButtonVisible( i, false ) ;
		boolean changed = false ;
		changed = setButtonContent( 0, BUTTON_TYPE_LOBBY_SEARCHING_MAIN ) || changed ;
		changed = setButtonContent( 1, BUTTON_TYPE_SEARCHING_HIDE ) || changed ;
		
		return changed ;
	}
	
	private boolean setStripContentToOpenLobby() {
		for ( int i = 2; i < getButtonCount(); i++ )
			setButtonVisible( i, false ) ;
		boolean changed = false ;
		changed = setButtonContent( 0, BUTTON_TYPE_LOBBY_OPEN_MAIN ) || changed ;
		changed = setButtonContent( 1, BUTTON_TYPE_OPEN_HIDE ) || changed ;
		
		return changed ;
	}
	
	private boolean setStripContentToFullLobby() {
		for ( int i = 2; i < getButtonCount(); i++ )
			setButtonVisible( i, false ) ;
		boolean changed = false ;
		changed = setButtonContent( 0, BUTTON_TYPE_LOBBY_FULL_MAIN ) || changed ;
		changed = setButtonContent( 1, BUTTON_TYPE_FULL_HIDE ) || changed ;
		
		return changed ;
	}
	
	private boolean setStripContentToClosedLobby() {
		for ( int i = 2; i < getButtonCount(); i++ )
			setButtonVisible( i, false ) ;
		boolean changed = false ;
		changed = setButtonContent( 0, BUTTON_TYPE_LOBBY_CLOSED_MAIN ) || changed ;
		changed = setButtonContent( 1, BUTTON_TYPE_CLOSED_HIDE ) || changed ;
		
		return changed ;
	}
	
	
	private boolean setButtonContent( int buttonIndex, int buttonType ) {
		boolean changed = false ;
		
		int typeTitle, typeDescription, typeLongDescription ;
		int display = TextFormatting.DISPLAY_MENU ;
		int role = TextFormatting.ROLE_HOST ;
		
		// Color and background drawable
		boolean hasColor = false ;
		int baseColor = 0 ;
		Drawable backgroundDrawable = null ;
		Drawable imageDrawable = null ;
		
		int alpha = 255 ;
		
		if ( buttonType == -1 ) {
			hasColor = true ;
			baseColor = 0xffffffff ;
			backgroundDrawable = null ;
			imageDrawable = null ;
		} else {
			hasColor = mHasColorButton[mLobbyType][buttonType] ;
			baseColor = mColorButton[mLobbyType][buttonType] ;
			backgroundDrawable = mBackgroundDrawableButton[mLobbyType][buttonType] ;
			imageDrawable = mImageDrawableButton[mLobbyType][buttonType] ;
		}
		
		switch( buttonType ) {
		case BUTTON_TYPE_LOBBY_SEARCHING_MAIN:
			typeTitle = TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_TITLE_SEARCHING ;
			typeDescription = TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_DESCRIPTION_SEARCHING ;
			typeLongDescription = TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_DESCRIPTION_LONG_SEARCHING ;
			break ;
			
		// TODO: special case handling for CLOSED_MAIN?
		case BUTTON_TYPE_LOBBY_OPEN_MAIN:
		case BUTTON_TYPE_LOBBY_FULL_MAIN:
		case BUTTON_TYPE_LOBBY_CLOSED_MAIN:
			typeTitle = TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_TITLE ;
			typeDescription = TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_DESCRIPTION ;
			typeLongDescription = TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_DESCRIPTION_LONG ;
			break ;
			
		case BUTTON_TYPE_SEARCHING_HIDE:
		case BUTTON_TYPE_OPEN_HIDE:
		case BUTTON_TYPE_FULL_HIDE:
		case BUTTON_TYPE_CLOSED_HIDE:
			typeTitle = TextFormatting.TYPE_MENU_LOBBY_MANAGER_HIDE_BUTTON_TITLE ;
			typeDescription = TextFormatting.TYPE_MENU_LOBBY_MANAGER_HIDE_BUTTON_DESCRIPTION ;
			typeLongDescription = typeDescription ;
			break ;
			
		default:
			typeTitle = -1 ;
			typeDescription = -1 ;
			typeLongDescription = -1 ;
		}
		
		Context context = getContext() ;
		Resources res = context.getResources() ;
		String textTitle, textDescription, textLongDescription ;
		if ( mWiFiLobbyDetails != null ) {
			textTitle = TextFormatting.format(context, res, display, typeTitle, role, mWiFiLobbyDetails) ;
			textDescription = TextFormatting.format(context, res, display, typeDescription, role, mWiFiLobbyDetails) ;
			textLongDescription = TextFormatting.format(context, res, display, typeLongDescription, role, mWiFiLobbyDetails) ;
		} else if ( mLobby != null ) {
			textTitle = TextFormatting.format(context, res, display, typeTitle, role, mLobby) ;
			textDescription = TextFormatting.format(context, res, display, typeDescription, role, mLobby) ;
			textLongDescription = TextFormatting.format(context, res, display, typeLongDescription, role, mLobby) ;
		} else {
			textTitle = "NONE" ;
			textDescription = "NONE" ;
			textLongDescription = "NONE" ;
		}
		
		QuantroButtonAccess qbs = getButtonAccess(buttonIndex) ;
		
		// Now set these values in the appropriate content fields.
		changed = qbs.setTitle(textTitle) || changed ;
		changed = qbs.setDescription(textDescription) || changed ;
		changed = qbs.setLongDescription(textLongDescription) || changed ;
		
		// background and image drawable
		changed = qbs.setBackground(backgroundDrawable) || changed ;
		changed = qbs.setImage(true, imageDrawable) || changed ;
		
		// set the button base color
		if ( hasColor )
			changed = qbs.setColor(baseColor) || changed ;
		
		// closed: set alpha.
		if ( buttonType == BUTTON_TYPE_LOBBY_CLOSED_MAIN || buttonType == BUTTON_TYPE_LOBBY_SEARCHING_MAIN )
			alpha = 128 ;
		changed = qbs.setContentAlpha(alpha) || changed ;
		
		if ( !this.getButtonIsVisible(buttonIndex) ) {
			this.setButtonVisible(buttonIndex, true) ;
			changed = true ;
		}
		
		if ( !this.getButtonIsEnabled(buttonIndex) ) {
			this.setButtonEnabled(buttonIndex, true) ;
			changed = true ;
		}
		
		return changed ;
	}
	
	private int getButtonType( int buttonNum ) {
		if ( buttonNum == 0 ) {
			if ( mWiFiLobbyDetails == null && mLobby == null )
				return BUTTON_TYPE_NO_LOBBY ;
			else {
				switch( mLobbyStatusOnLastRefresh ) {
				case LOBBY_STATUS_NONE:
					return BUTTON_TYPE_NO_LOBBY ;
				case LOBBY_STATUS_SEARCHING:
					return BUTTON_TYPE_LOBBY_SEARCHING_MAIN ;
				case LOBBY_STATUS_OPEN:
					return BUTTON_TYPE_LOBBY_OPEN_MAIN ;
				case LOBBY_STATUS_FULL:
					return BUTTON_TYPE_LOBBY_FULL_MAIN ;
				case LOBBY_STATUS_CLOSED:
					return BUTTON_TYPE_LOBBY_CLOSED_MAIN ;
				}
			}
		} else if ( buttonNum == 1 ) {
			// Second button may have a number of specialized functions,
			// based on lobby description, but for right now it's all "hiding."
			if ( mWiFiLobbyDetails != null || mLobby != null  ) {
				switch( mLobbyStatusOnLastRefresh ) {
				case LOBBY_STATUS_NONE:
					return BUTTON_TYPE_NO_LOBBY ;
				case LOBBY_STATUS_SEARCHING:
					return BUTTON_TYPE_SEARCHING_HIDE ;
				case LOBBY_STATUS_OPEN:
					return BUTTON_TYPE_OPEN_HIDE ;
				case LOBBY_STATUS_FULL:
					return BUTTON_TYPE_FULL_HIDE ;
				case LOBBY_STATUS_CLOSED:
					return BUTTON_TYPE_CLOSED_HIDE ;
				}
			}
		}
		
		return -1 ;
	}

	@Override
	public void qbsc_onClick(QuantroButtonStrip strip, int buttonNum, boolean asOverflow) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;			// we did not handle it
		
		int buttonType = getButtonType( buttonNum ) ;
		
		if ( buttonNum >= 0 && buttonType >= 0 )
			delegate.lbs_onButtonClick(this, buttonNum, buttonType, mLobbyType, mWiFiLobbyDetails, mLobby) ;
		return ;
	}

	@Override
	public boolean qbsc_onLongClick(QuantroButtonStrip strip, int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;			// we did not handle it
		
		int buttonType = getButtonType( buttonNum ) ;
		
		if ( buttonNum >= 0 && buttonType >= 0 )
			return delegate.lbs_onButtonLongClick(this, buttonNum, buttonType, mLobbyType, mWiFiLobbyDetails, mLobby) ;
		return false ;
	}

	@Override
	public boolean qbsc_supportsLongClick(QuantroButtonStrip strip,
			int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;			// we did not handle it
		
		int buttonType = getButtonType( buttonNum ) ;
		
		if ( buttonNum >= 0 && buttonType >= 0 )
			return delegate.lbs_supportsLongClick(this, buttonNum, buttonType, mLobbyType, mWiFiLobbyDetails, mLobby) ;
		return false ;
	}
	
	public void qbsc_onOverflowClicked( QuantroButtonStrip strip ) {
		// nothing
	}
	
}
