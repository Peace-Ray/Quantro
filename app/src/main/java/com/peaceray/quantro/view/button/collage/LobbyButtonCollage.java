package com.peaceray.quantro.view.button.collage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import com.peaceray.quantro.R;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.lobby.WebConsts;
import com.peaceray.quantro.lobby.WiFiLobbyDetails;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton.SupportsLongClickOracle;
import com.peaceray.quantro.view.button.content.QuantroButtonAccess;
import com.peaceray.quantro.view.button.content.QuantroButtonDirectAccess;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;


public class LobbyButtonCollage extends QuantroButtonCollage implements OnClickListener, OnLongClickListener, SupportsLongClickOracle {
	
	public static final String TAG = "LobbyButtonCollage" ;
	
	public interface Delegate {
		
		/**
		 * The user wants to join this lobby.
		 * @param collage
		 * @param lobby
		 * @param lobbyDetails
		 * @return Should we play the appropriate sound effect for this button press?
		 */
		public boolean lbc_join( LobbyButtonCollage collage, Lobby lobby, WiFiLobbyDetails lobbyDetails, int lobbyType ) ;
		
		/**
		 * The user wants to examine this lobby.
		 * @param collage
		 * @param lobby
		 * @param lobbyDetails
		 * @return Should we play the appropriate sound effect for this button press?
		 */
		public boolean lbc_examine( LobbyButtonCollage collage, Lobby lobby, WiFiLobbyDetails lobbyDetails, int lobbyType ) ;
		
		/**
		 * The user wants to hide this lobby.
		 * @param collage
		 * @param lobby
		 * @param lobbyDetails
		 * @return Should we play the appropriate sound effect for this button press?
		 */
		public boolean lbc_hide( LobbyButtonCollage collage, Lobby lobby, WiFiLobbyDetails lobbyDetails, int lobbyType ) ;
	}
	

	public LobbyButtonCollage(Context context) {
		super(context);
		
		// Set basic defaults
		
		constructor_setDefaultValues(context) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public LobbyButtonCollage(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
		
		
	}
	
	public LobbyButtonCollage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	private void constructor_setDefaultValues( Context context ) {
		mLobby = null ;
		mWiFiLobbyDetails = null ;
		mWiFiLobbySearching = false ;
		mwrDelegate = new WeakReference<Delegate>(null) ;
		
		Resources res = context.getResources() ;
		
		// Initialize our colors and drawables.
		mLobbyTypeButtonTypeColor = new int [NUM_LOBBY_TYPES][NUM_BUTTON_TYPES] ;
		mLobbyTypeLobbySizeButtonTypeImageDrawable = new Drawable[NUM_LOBBY_TYPES][NUM_LOBBY_SIZES][NUM_BUTTON_TYPES] ;
		// Set our color and image defaults.
		Drawable drawablePopulation2 = res.getDrawable(R.drawable.icon_population_2_baked) ;
		Drawable drawablePopulation3 = res.getDrawable(R.drawable.icon_population_3_baked) ;
		Drawable drawableHide = res.getDrawable(R.drawable.action_cancel_baked) ;
		
		// join wifi
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_WIFI][BUTTON_TYPE_SEARCHING_JOIN] = res.getColor(R.color.lobby_button_strip_searching_main) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_WIFI][BUTTON_TYPE_OPEN_JOIN] = res.getColor(R.color.lobby_button_strip_open_main) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_WIFI][BUTTON_TYPE_FULL_JOIN] = res.getColor(R.color.lobby_button_strip_full_main) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_WIFI][BUTTON_TYPE_CLOSED_JOIN] = res.getColor(R.color.lobby_button_strip_closed_main) ;
		// hide wifi
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_WIFI][BUTTON_TYPE_SEARCHING_HIDE] = res.getColor(R.color.lobby_button_strip_searching_hide) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_WIFI][BUTTON_TYPE_OPEN_HIDE] = res.getColor(R.color.lobby_button_strip_open_hide) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_WIFI][BUTTON_TYPE_FULL_HIDE] = res.getColor(R.color.lobby_button_strip_full_hide) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_WIFI][BUTTON_TYPE_CLOSED_HIDE] = res.getColor(R.color.lobby_button_strip_closed_hide) ;
		// join public
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_PUBLIC][BUTTON_TYPE_OPEN_JOIN] = res.getColor(R.color.internet_lobby_button_strip_public_open_main) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_PUBLIC][BUTTON_TYPE_FULL_JOIN] = res.getColor(R.color.internet_lobby_button_strip_public_full_main) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_PUBLIC][BUTTON_TYPE_CLOSED_JOIN] = res.getColor(R.color.internet_lobby_button_strip_public_closed_main) ;
		// hide public
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_PUBLIC][BUTTON_TYPE_OPEN_HIDE] = res.getColor(R.color.lobby_button_strip_open_hide) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_PUBLIC][BUTTON_TYPE_FULL_HIDE] = res.getColor(R.color.lobby_button_strip_full_hide) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_PUBLIC][BUTTON_TYPE_CLOSED_HIDE] = res.getColor(R.color.lobby_button_strip_closed_hide) ;
		// join private
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_PRIVATE][BUTTON_TYPE_OPEN_JOIN] = res.getColor(R.color.internet_lobby_button_strip_private_open_main) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_PRIVATE][BUTTON_TYPE_FULL_JOIN] = res.getColor(R.color.internet_lobby_button_strip_private_full_main) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_PRIVATE][BUTTON_TYPE_CLOSED_JOIN] = res.getColor(R.color.internet_lobby_button_strip_private_closed_main) ;
		// hide private
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_PRIVATE][BUTTON_TYPE_OPEN_HIDE] = res.getColor(R.color.lobby_button_strip_open_hide) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_PRIVATE][BUTTON_TYPE_FULL_HIDE] = res.getColor(R.color.lobby_button_strip_full_hide) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_PRIVATE][BUTTON_TYPE_CLOSED_HIDE] = res.getColor(R.color.lobby_button_strip_closed_hide) ;
		// join ITINERANT
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_ITINERANT][BUTTON_TYPE_OPEN_JOIN] = res.getColor(R.color.internet_lobby_button_strip_itinerant_open_main) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_ITINERANT][BUTTON_TYPE_FULL_JOIN] = res.getColor(R.color.internet_lobby_button_strip_itinerant_full_main) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_ITINERANT][BUTTON_TYPE_CLOSED_JOIN] = res.getColor(R.color.internet_lobby_button_strip_itinerant_closed_main) ;
		// hide ITINERANT
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_ITINERANT][BUTTON_TYPE_OPEN_HIDE] = res.getColor(R.color.lobby_button_strip_open_hide) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_ITINERANT][BUTTON_TYPE_FULL_HIDE] = res.getColor(R.color.lobby_button_strip_full_hide) ;
		mLobbyTypeButtonTypeColor[LOBBY_TYPE_INTERNET_ITINERANT][BUTTON_TYPE_CLOSED_HIDE] = res.getColor(R.color.lobby_button_strip_closed_hide) ;
		
		// Drawables
		for ( int i = 0; i < NUM_LOBBY_TYPES; i++ ) {
			for ( int j = 0; j < NUM_LOBBY_SIZES; j++ ) {
				Drawable popDraw = j == LOBBY_SIZE_2
						? drawablePopulation2 : drawablePopulation3 ;
				mLobbyTypeLobbySizeButtonTypeImageDrawable[i][j][BUTTON_TYPE_OPEN_JOIN] = popDraw ;
				mLobbyTypeLobbySizeButtonTypeImageDrawable[i][j][BUTTON_TYPE_FULL_JOIN] = popDraw ;
				mLobbyTypeLobbySizeButtonTypeImageDrawable[i][j][BUTTON_TYPE_CLOSED_JOIN] = popDraw ;
				
				mLobbyTypeLobbySizeButtonTypeImageDrawable[i][j][BUTTON_TYPE_SEARCHING_HIDE] = drawableHide ;
				mLobbyTypeLobbySizeButtonTypeImageDrawable[i][j][BUTTON_TYPE_OPEN_HIDE] = drawableHide ;
				mLobbyTypeLobbySizeButtonTypeImageDrawable[i][j][BUTTON_TYPE_FULL_HIDE] = drawableHide ;
				mLobbyTypeLobbySizeButtonTypeImageDrawable[i][j][BUTTON_TYPE_CLOSED_HIDE] = drawableHide ;
			}
		}
		
		mLobbyStatusOnLastRefresh = -1 ;
		
		mSoundPool = null ;
		mSoundControls = true ;
	}
	
	
	private void constructor_readAttributeSet( Context context, AttributeSet attrs ) {
		// Nothing to do here.
	}
	
	
	private void constructor_allocateAndInitialize( Context context ) {
		// Nothing to do here.
	}
	
	
	private static final int BUTTON_TYPE_SEARCHING_JOIN = 0 ;
	private static final int BUTTON_TYPE_OPEN_JOIN = 1 ;
	private static final int BUTTON_TYPE_FULL_JOIN = 2 ;
	private static final int BUTTON_TYPE_CLOSED_JOIN = 3 ;
	
	private static final int BUTTON_TYPE_SEARCHING_HIDE = 4 ;
	private static final int BUTTON_TYPE_OPEN_HIDE = 5 ;
	private static final int BUTTON_TYPE_FULL_HIDE = 6 ;
	private static final int BUTTON_TYPE_CLOSED_HIDE = 7 ;
	
	private static final int NUM_BUTTON_TYPES = 8 ;
	
	public static final int LOBBY_TYPE_WIFI = 0 ;
	public static final int LOBBY_TYPE_INTERNET_PUBLIC = 1 ;
	public static final int LOBBY_TYPE_INTERNET_PRIVATE = 2 ;
	public static final int LOBBY_TYPE_INTERNET_ITINERANT = 3 ;
	
	public static final int NUM_LOBBY_TYPES = 4 ;
	
	public static final int LOBBY_SIZE_2 = 0 ;
	public static final int LOBBY_SIZE_4 = 1 ;
	public static final int LOBBY_SIZE_6 = 2 ;
	public static final int NUM_LOBBY_SIZES = 3 ;
	
	
	private static final int BUTTON_NUM_MAIN = 0 ;
	private static final int BUTTON_NUM_HIDE = 1 ;
	private static final int NUM_BUTTONS = 2 ;
	
	
	public static final int LOBBY_STATUS_NONE = -1 ;
	public static final int LOBBY_STATUS_SEARCHING = 0 ;
	public static final int LOBBY_STATUS_OPEN = 1 ;
	public static final int LOBBY_STATUS_FULL = 2 ;
	public static final int LOBBY_STATUS_CLOSED = 3 ;
	
	// We need references to a bunch of stuff.  There are two, possibly three
	// buttons to be concerned with: our main launch button and a 'new game' button,
	// and possibly a "custom edit" button.
	private QuantroContentWrappingButton [] mButtons ;
	private QuantroButtonDirectAccess [] mButtonContent ;
	private int [][] mLobbyTypeButtonTypeColor ;
	private Drawable [][][] mLobbyTypeLobbySizeButtonTypeImageDrawable ;
	
	// Our collage is configured by a Lobby, given as a lobbyDetails.
	protected WiFiLobbyDetails mWiFiLobbyDetails ;
	protected boolean mWiFiLobbySearching ;
	protected Lobby mLobby ;
	protected int mLobbyType ;
	protected int mLobbyStatusOnLastRefresh ;
	
	private QuantroSoundPool mSoundPool ;
	private boolean mSoundControls ;
	
	// Delegate!
	protected WeakReference<Delegate> mwrDelegate ;
	
	
	public void setDelegate( Delegate delegate ) {
		this.mwrDelegate = new WeakReference<Delegate>(delegate) ;
	}
	
	public void setSoundPool( QuantroSoundPool pool ) {
		this.mSoundPool = pool ;
	}
	
	public void setSoundControls( boolean soundControls ) {
		this.mSoundControls = soundControls ;
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
	
	private boolean refresh_ever = false ;
	@Override
	public void refresh() {
		// Determine if the content changed.  If so, call super.refresh.
		boolean changed = false ;
		
		if ( !refresh_ever ) {
			collectAllContent() ;
			refresh_ever = true ;
			changed = true ;
		}
		
		int lobbyStatus = LOBBY_STATUS_NONE ;
		
		// There are 3 basic styles: no lobby, an open lobby, and a full lobby.
		// However, within those styles there are other options: for example, we might
		// 
		if ( mWiFiLobbyDetails != null || mLobby != null ) {
			boolean searching = false, open = false, full = false, closed = false ;
			if ( mWiFiLobbyDetails != null ) {
				if ( mWiFiLobbySearching ) {
					searching = true ;
				} else {
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
				changed = setContentToSearchingLobby() || changed ;
				lobbyStatus = LOBBY_STATUS_SEARCHING ;
			} else if ( open ) {
				changed = setContentToOpenLobby() || changed ;
				lobbyStatus = LOBBY_STATUS_OPEN ;
			} else if ( full ) {
				changed = setContentToFullLobby() || changed ;
				lobbyStatus = LOBBY_STATUS_FULL ;
			} else if ( closed ) {
				changed = setContentToClosedLobby() || changed ;
				lobbyStatus = LOBBY_STATUS_CLOSED ;
			}
		}
		
		if ( lobbyStatus != this.mLobbyStatusOnLastRefresh ) {
			changed = true ;
			mLobbyStatusOnLastRefresh = lobbyStatus ;
		}
		
		if ( changed )
			super.refresh() ;
	}
	
	
	/**
	 * Collects necessary references, sets listeners, creates 
	 * QuantroButtonContent objects, etc.
	 */
	private void collectAllContent() {
		
		// collect buttons.
		ArrayList<Integer> ids = new ArrayList<Integer>() ;
		
		ids.add( R.id.button_collage_lobby_button_main ) ;
		ids.add( R.id.button_collage_lobby_button_hide ) ;
		
		ArrayList<QuantroContentWrappingButton> buttons = collectButtons(ids) ;
		
		mButtons = new QuantroContentWrappingButton[NUM_BUTTONS] ;
		mButtons[BUTTON_NUM_MAIN] = buttons.get(0) ;
		mButtons[BUTTON_NUM_HIDE] = buttons.get(1) ;
		
		// construct and associate contents.
		mButtonContent = new QuantroButtonDirectAccess[NUM_BUTTONS] ;
		for ( int i = 0; i < NUM_BUTTONS; i++ ) {
			QuantroContentWrappingButton qcwb = mButtons[i] ;
			if ( qcwb != null ) {
				mButtonContent[i] = (QuantroButtonDirectAccess)QuantroButtonDirectAccess.wrap( qcwb ) ;
				qcwb.setTag(mButtonContent[i]) ;
			}
		}
		
		// assign listeners
		for ( int i = 0; i < NUM_BUTTONS; i++ ) {
			QuantroContentWrappingButton qcwb = mButtons[i] ;
			if ( qcwb != null ) {
				qcwb.setOnClickListener(this) ;
				qcwb.setOnLongClickListener(this) ;
				qcwb.setSupportsLongClickOracle(this) ;
			}
		}
	}
	
	
	private boolean setContentToSearchingLobby() {
		boolean changed = false ;
		changed = this.setButtonContent(mButtons[0], mButtonContent[0], BUTTON_TYPE_SEARCHING_JOIN) || changed ;
		changed = this.setButtonContent(mButtons[1], mButtonContent[1], BUTTON_TYPE_SEARCHING_HIDE) || changed ;
		
		return changed ;
	}
	
	private boolean setContentToOpenLobby() {
		boolean changed = false ;
		changed = this.setButtonContent(mButtons[0], mButtonContent[0], BUTTON_TYPE_OPEN_JOIN) || changed ;
		changed = this.setButtonContent(mButtons[1], mButtonContent[1], BUTTON_TYPE_OPEN_HIDE) || changed ;
		
		return changed ;
	}
	
	private boolean setContentToFullLobby() {
		boolean changed = false ;
		changed = this.setButtonContent(mButtons[0], mButtonContent[0], BUTTON_TYPE_FULL_JOIN) || changed ;
		changed = this.setButtonContent(mButtons[1], mButtonContent[1], BUTTON_TYPE_FULL_HIDE) || changed ;
		
		return changed ;
	}
	
	private boolean setContentToClosedLobby() {
		boolean changed = false ;
		changed = this.setButtonContent(mButtons[0], mButtonContent[0], BUTTON_TYPE_CLOSED_JOIN) || changed ;
		changed = this.setButtonContent(mButtons[1], mButtonContent[1], BUTTON_TYPE_CLOSED_HIDE) || changed ;
		
		return changed ;
	}
	
	
	private boolean setButtonContent( QuantroContentWrappingButton button, QuantroButtonDirectAccess content, int buttonType ) {
		boolean changed = false ;
		
		int typeTitle = -1 ;
		
		int display = TextFormatting.DISPLAY_MENU ;
		int role = TextFormatting.ROLE_HOST ;
		
		int maxPlayers = 2 ;
		if ( mLobby != null ) {
			maxPlayers = mLobby.getMaxPeople() ;
		} else if ( mWiFiLobbyDetails != null ) {
			maxPlayers = mWiFiLobbyDetails.hasReceivedStatus() ? mWiFiLobbyDetails.getReceivedStatus().getMaxPeople() : 0 ;
		}
		
		int lobbySize ;
		if ( maxPlayers >= 6 )
			lobbySize = LOBBY_SIZE_6 ;
		else if ( maxPlayers >= 4 )
			lobbySize = LOBBY_SIZE_4 ;
		else
			lobbySize = LOBBY_SIZE_2 ;
		
		// Color and background drawable
		int baseColor = this.mLobbyTypeButtonTypeColor[mLobbyType][buttonType] ;
		Drawable imageDrawable = this.mLobbyTypeLobbySizeButtonTypeImageDrawable[mLobbyType][lobbySize][buttonType] ;
		
		switch( buttonType ) {
		case BUTTON_TYPE_SEARCHING_JOIN:
			typeTitle = TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_TITLE_SEARCHING ;
			break ;
		case BUTTON_TYPE_OPEN_JOIN:
		case BUTTON_TYPE_FULL_JOIN:
		case BUTTON_TYPE_CLOSED_JOIN:
			typeTitle = TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_TITLE ;
			break ;
		case BUTTON_TYPE_SEARCHING_HIDE:
		case BUTTON_TYPE_OPEN_HIDE:
		case BUTTON_TYPE_FULL_HIDE:
		case BUTTON_TYPE_CLOSED_HIDE:
			typeTitle = TextFormatting.TYPE_MENU_LOBBY_MANAGER_HIDE_BUTTON_TITLE ;
			break ;
		}
		
		
		Context context = getContext() ;
		Resources res = context.getResources() ;
		String textTitle = null ;
		if ( mLobby != null )
			textTitle = TextFormatting.format(context, res, display, typeTitle, role, mLobby) ;
		else if ( mWiFiLobbyDetails != null )
			textTitle = TextFormatting.format(context, res, display, typeTitle, role, mWiFiLobbyDetails) ;
		
		// Set values that apply to all buttons
		changed = content.setTitle(textTitle) 		|| changed ;
		changed = content.setImage(imageDrawable != null, imageDrawable) 	|| changed ;
		changed = content.setColor(baseColor) 		|| changed ;
		
		// Special settings for the main button
		if ( buttonType == BUTTON_TYPE_SEARCHING_JOIN || buttonType == BUTTON_TYPE_OPEN_JOIN || buttonType == BUTTON_TYPE_FULL_JOIN || buttonType == BUTTON_TYPE_CLOSED_JOIN ) {
			String textMembership = null ;
			String textCreation = null ;
			String textHost = null ;
			String textAddressOrType = null ;
			String textSessionID = null ;
			if ( mLobby != null ) {
				textMembership = TextFormatting.format(context, res, display,
						TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_POPULATION, role, mLobby) ;
				textCreation = TextFormatting.format(context, res, display,
						TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_CREATION_TIME, role, mLobby) ;
				textHost = TextFormatting.format(context, res, display,
						TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_HOST, role, mLobby) ;
				textAddressOrType = TextFormatting.format(context, res, display,
						TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_ADDRESS_OR_TYPE, role, mLobby) ;
				textSessionID = TextFormatting.format(context, res, display,
						TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_SESSION_ID, role, mLobby) ;
			} else if ( mWiFiLobbyDetails != null ) {
				if ( mWiFiLobbySearching ) {
					textMembership = TextFormatting.format(context, res, display,
							TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_POPULATION_SEARCHING, role, mWiFiLobbyDetails) ;
					textCreation = TextFormatting.format(context, res, display,
							TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_CREATION_TIME_SEARCHING, role, mWiFiLobbyDetails) ;
					textHost = TextFormatting.format(context, res, display,
							TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_HOST_SEARCHING, role, mWiFiLobbyDetails) ;
					textAddressOrType = TextFormatting.format(context, res, display,
							TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_ADDRESS_OR_TYPE_SEARCHING, role, mWiFiLobbyDetails) ;
					textSessionID = TextFormatting.format(context, res, display,
							TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_SESSION_ID_SEARCHING, role, mWiFiLobbyDetails) ;
				} else {
					textMembership = TextFormatting.format(context, res, display,
							TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_POPULATION, role, mWiFiLobbyDetails) ;
					textCreation = TextFormatting.format(context, res, display,
							TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_CREATION_TIME, role, mWiFiLobbyDetails) ;
					textHost = TextFormatting.format(context, res, display,
							TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_HOST, role, mWiFiLobbyDetails) ;
					textAddressOrType = TextFormatting.format(context, res, display,
							TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_ADDRESS_OR_TYPE, role, mWiFiLobbyDetails) ;
					textSessionID = TextFormatting.format(context, res, display,
							TextFormatting.TYPE_MENU_LOBBY_MANAGER_MAIN_BUTTON_SESSION_ID, role, mWiFiLobbyDetails) ;
				}
			}
			
			changed = content.setLobbyDetail(QuantroButtonAccess.LobbyDetail.MEMBERSHIP, 	textMembership) 	|| changed ;
			changed = content.setLobbyDetail(QuantroButtonAccess.LobbyDetail.CREATION, 		textCreation) 		|| changed ;
			changed = content.setLobbyDetail(QuantroButtonAccess.LobbyDetail.HOST, 			textHost) 			|| changed ;
			changed = content.setLobbyDetail(QuantroButtonAccess.LobbyDetail.ADDRESS_OR_TYPE, textAddressOrType) 	|| changed ;
			changed = content.setLobbyDetail(QuantroButtonAccess.LobbyDetail.SESSION_ID, 	textSessionID) 		|| changed ;
		}
		
		
		boolean enabled = true ;
		switch( buttonType ) {
		case BUTTON_TYPE_SEARCHING_JOIN:
		case BUTTON_TYPE_FULL_JOIN:
			enabled = false ;
			break ;
		case BUTTON_TYPE_CLOSED_JOIN:
			enabled = mLobbyType == LOBBY_TYPE_INTERNET_ITINERANT ;
			break ;
		}
		
		changed = content.setContentAlpha(enabled ? 255 : 128) || changed ;
		changed = button.isEnabled() != enabled					|| changed ;
		button.setEnabled(enabled) ;
		
		// pinwheel if searching main
		changed = content.setPinwheel(buttonType == BUTTON_TYPE_SEARCHING_JOIN) || changed ;
		
		return changed ;
	}
	
	

	@Override
	public void onClick(View v) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;
		
		boolean sound = false ;
		
		Log.d(TAG, "onClick " + v) ;
		
		if ( v == mButtons[BUTTON_NUM_MAIN] ) {
			// join!
			sound = delegate.lbc_join(this, mLobby, mWiFiLobbyDetails, mLobbyType) ;
		}
		else if ( v == mButtons[BUTTON_NUM_HIDE] ) {
			// hide!
			sound = delegate.lbc_hide(this, mLobby, mWiFiLobbyDetails, mLobbyType) ;
		}
		
		if ( mSoundPool != null && mSoundControls && sound )
			mSoundPool.menuButtonClick() ;
	}
	

	@Override
	public boolean onLongClick(View v) {
		
		return false ;
	}

	

	@Override
	public boolean supportsLongClick(QuantroContentWrappingButton qcwb) {
		// no long-press.  Normally we would allow "examination" with
		// a long-press, but all the relevant info is already here!
		return false ;
	}
}
