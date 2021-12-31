package com.peaceray.quantro.view.button.strip;

import java.lang.ref.WeakReference;

import com.peaceray.quantro.R;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.button.content.QuantroButtonAccess;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;


public class LobbyTitleBarButtonStrip extends QuantroButtonStrip 
		implements QuantroButtonStrip.Controller {

	public interface Delegate {
		/**
		 * The user has short-clicked a button on this strip.  If buttonNum == 0,
		 * be assured that it is the main button.  If > 0, it is some other button,
		 * and as mentioned in the class description, the content and effect are
		 * completely up to the outside world to determine.  If you need to check
		 * content, ID, or whatnot, you can use strip.getButton( buttonNum ).
		 * 
		 * The menuItemNumber provided is the currently displayed menu item
		 * according to the strip's records.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param menuItemNumber
		 */
		public boolean ltbbs_onButtonClick(
				LobbyTitleBarButtonStrip strip, int buttonType, boolean asOverflow ) ;
		
		/**
		 * The user has long-clicked a button on this strip.  If buttonNum == 0,
		 * be assured that it is the main button.  If > 0, it is some other button,
		 * and as mentioned in the class description, the content and effect are
		 * completely up to the outside world to determine.  If you need to check
		 * content, ID, or whatnot, you can use strip.getButton( buttonNum ).
		 * 
		 * The menuItemNumber provided is the currently displayed menu item
		 * according to the strip's records.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param menuItemNumber
		 */
		public boolean ltbbs_onButtonLongClick(
				LobbyTitleBarButtonStrip strip, int buttonType ) ;
		
		
		/**
		 * The user has opened the overflow menu.
		 * @param strip
		 */
		public void ltbbs_onOverflowClicked( LobbyTitleBarButtonStrip strip ) ;
		
	}
	
	
	public static final int BUTTON_MAIN = 0 ;
	public static final int BUTTON_TAB_VOTE = 1 ;
	public static final int BUTTON_TAB_CHAT = 2 ;
	public static final int BUTTON_SHARE = 3 ;
	public static final int BUTTON_INFO = 4 ;
	public static final int BUTTON_HELP = 5 ;
	public static final int BUTTON_SETTINGS = 6 ;
	
	
	public static final int NUM_BUTTONS = 7 ;
	
	
	@SuppressWarnings("unused")
	private static final String TAG = "LTBButtonStrip" ;
	
	
	public LobbyTitleBarButtonStrip(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
	}
	
	public LobbyTitleBarButtonStrip(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
	}
	
	
	private static final int TYPE_WIFI = 0 ;
	private static final int TYPE_INTERNET = 1 ;
	private static final int TYPE_INTERNET_PUBLIC = 2 ;
	private static final int TYPE_INTERNET_ITINERANT = 3 ;
	
	
	private int mMainColor ;
	private int mColorWifi ;
	private int mColorInternet ;
	private int mColorInternetPublic ;
	private int mColorInternetItinerant ;
	private int mType ;
	private boolean mIsHost ;
	private WeakReference<Delegate> mwrDelegate ;
	private Lobby mLobby ;
	
	// drawables
	private Drawable [] mImageDrawable ;
	private String [] mTitle ;
	private int [] mColor ;
	
	
	private void constructor_setDefaultValues(Context context) {
		// Self-control
		this.setController(this) ;
		
		Resources res = context.getResources() ;
		
		mwrDelegate = new WeakReference<Delegate>(null) ;
		mLobby = null ;
		
		mMainColor = 0xff888888 ;
		mColorWifi = res.getColor(R.color.lobby_wifi) ;
		mColorInternet = res.getColor(R.color.lobby_internet) ;
		mColorInternetPublic = res.getColor(R.color.internet_lobby_public) ;
		mColorInternetItinerant = res.getColor(R.color.internet_lobby_itinerant) ;
		
		
		mType = TYPE_WIFI ;
		mIsHost = false ;
		
		mImageDrawable = new Drawable[NUM_BUTTONS] ;
		mImageDrawable[BUTTON_TAB_CHAT] = res.getDrawable(R.drawable.action_chat_baked) ;
		mImageDrawable[BUTTON_TAB_VOTE] = res.getDrawable(R.drawable.action_play_baked) ;
		mImageDrawable[BUTTON_SHARE] = res.getDrawable(R.drawable.action_share_baked) ;
		mImageDrawable[BUTTON_INFO] = res.getDrawable(R.drawable.action_about_baked) ;
		mImageDrawable[BUTTON_HELP] = res.getDrawable(R.drawable.action_help_baked) ;
		mImageDrawable[BUTTON_SETTINGS] = res.getDrawable(R.drawable.action_settings_baked) ;
		
		
		mTitle = new String[NUM_BUTTONS] ;
		mTitle[BUTTON_TAB_CHAT] = res.getString(R.string.lobby_action_bar_chat_title) ;
		mTitle[BUTTON_TAB_VOTE] = res.getString(R.string.lobby_action_bar_vote_title) ;
		mTitle[BUTTON_SHARE] = res.getString(R.string.lobby_action_bar_share_title) ;
		mTitle[BUTTON_INFO] = res.getString(R.string.lobby_action_bar_info_title) ;
		mTitle[BUTTON_HELP] = res.getString(R.string.lobby_action_bar_help_title) ;
		mTitle[BUTTON_SETTINGS] = res.getString(R.string.lobby_action_bar_settings_title) ;
	
		mColor = new int[NUM_BUTTONS] ;
		mColor[BUTTON_TAB_CHAT] = mMainColor ;
		mColor[BUTTON_TAB_VOTE] = mMainColor ;
		mColor[BUTTON_SHARE] = res.getColor(R.color.lobby_share) ;
		mColor[BUTTON_INFO] = res.getColor(R.color.lobby_info) ;
		mColor[BUTTON_HELP] = res.getColor(R.color.lobby_help) ;
		mColor[BUTTON_SETTINGS] = res.getColor(R.color.lobby_settings) ;
	
	}
	
	
	private void constructor_readAttributeSet(Context context, AttributeSet attrs) {
		// TODO: Maybe allow configuration when loaded?
	}
	
	
	synchronized public void setDelegate( LobbyTitleBarButtonStrip.Delegate delegate ) {
		this.mwrDelegate = new WeakReference<Delegate>(delegate) ;
	}

	synchronized public void setIsWifi() {
		mType = TYPE_WIFI ;
		mMainColor = mColor[BUTTON_TAB_CHAT] = mColor[BUTTON_TAB_VOTE] = mColorWifi ;
	}
	
	synchronized public void setIsInternetPrivate() {
		mType = TYPE_INTERNET ;
		mMainColor = mColor[BUTTON_TAB_CHAT] = mColor[BUTTON_TAB_VOTE] = mColorInternet ;
	}
	
	synchronized public void setIsInternetPublic() {
		mType = TYPE_INTERNET_PUBLIC ;
		mMainColor = mColor[BUTTON_TAB_CHAT] = mColor[BUTTON_TAB_VOTE] = mColorInternetPublic ;
	}
	
	synchronized public void setIsInternetItinerant() {
		mType = TYPE_INTERNET_ITINERANT ;
		mMainColor = mColor[BUTTON_TAB_CHAT] = mColor[BUTTON_TAB_VOTE] = mColorInternetItinerant ;
	}
	
	
	
	synchronized public void setIsHost( boolean isHost ) {
		mIsHost = isHost ;
	}
	
	synchronized public void setLobby( Lobby lobby ) {
		mLobby = lobby ;
		if ( lobby instanceof InternetLobby ) {
			InternetLobby il = (InternetLobby)lobby ;
			if ( il.isPublic() ) {
				mMainColor = mColor[BUTTON_TAB_CHAT] = mColor[BUTTON_TAB_VOTE] = mColorInternetPublic ;
			} else if ( il.isItinerant() ) {
				mMainColor = mColor[BUTTON_TAB_CHAT] = mColor[BUTTON_TAB_VOTE] = mColorInternetItinerant ;
			}
		}
	}
	
	synchronized public void setActiveTab( int buttonNum ) {
		if ( this.getButtonCount() < NUM_BUTTONS )
			refresh() ;
		
		for ( int i = 0; i < NUM_BUTTONS; i++ )
			getButtonAccess(i).setContentState(buttonNum == i
					? QuantroContentWrappingButton.ContentState.EXCLUSIVE
					: QuantroContentWrappingButton.ContentState.OPTIONAL ) ;
	}
	
	synchronized public void refresh() {
		boolean changed = false ;
		
		// Make sure we have enough buttons!
		while ( this.getButtonCount() < NUM_BUTTONS ) {
			addButton() ;
			changed = true ;
		}
		setButtonVisible(0, true) ;
		
		// set extra buttons invisible 
		for ( int i = NUM_BUTTONS; i < getButtonCount(); i++ ) {
			setButtonVisible(i, false) ;
		}
		
		// Simple enough.
		changed = setButtonContent( BUTTON_MAIN, BUTTON_MAIN ) 			|| changed ;
		changed = setButtonContent( BUTTON_TAB_CHAT, BUTTON_TAB_CHAT ) 	|| changed ;
		changed = setButtonContent( BUTTON_TAB_VOTE, BUTTON_TAB_VOTE ) 	|| changed ;
		changed = setButtonContent( BUTTON_INFO, BUTTON_INFO ) 	|| changed ;
		changed = setButtonContent( BUTTON_HELP, BUTTON_HELP ) 			|| changed ;
		changed = setButtonContent( BUTTON_SETTINGS, BUTTON_SETTINGS ) 	|| changed ;
		
		
		if ( mType == TYPE_WIFI ) {
			if ( getButtonIsVisible( BUTTON_SHARE ) ) {
				setButtonVisible( BUTTON_SHARE, false ) ;
				changed = true ;
			}
		} else {
			changed = setButtonContent( BUTTON_SHARE, BUTTON_SHARE )		|| changed ;
		}
		
		if ( changed )
			super.refresh() ;
	}
	
	
	private boolean setButtonContent( int buttonNum, int buttonType ) {
		
		boolean changed = false ;

		// Title is lobby name, description is something like
		// "WiFi lobby" or "Internet lobby"
		
		String title = null, description = null ;
		int color = 0xff888888 ;
		Drawable imageDrawable = null ;
		
		int type ;
		int role = mIsHost ? TextFormatting.ROLE_HOST : TextFormatting.ROLE_CLIENT ;
		
		// Right now, we only allow control of 1 button.  When we add more, this
		// method should change.
		
		switch( buttonType ) {
		case BUTTON_MAIN:
			if ( mLobby != null ) {
				title = mLobby.getLobbyName() == null ? "Connecting" : mLobby.getLobbyName() ;
				int playerSlot = mLobby.getLocalPlayerSlot() ;
				if ( playerSlot < 0 ) {
					type = mType == TYPE_WIFI
							? TextFormatting.TYPE_LOBBY_WIFI_DESCRIPTION_SHORT
							: TextFormatting.TYPE_LOBBY_INTERNET_DESCRIPTION_SHORT ;
					description = TextFormatting.format(
							getContext(),
							null,
							TextFormatting.DISPLAY_MENU,
							type,
							role,
							-1, mLobby ) ;		// no GameMode associated with this description
				} else {
					// personal launch status.
					int countdownNum = -1 ;
					int countdownGameMode = -1 ;
					allocate() ;
					int num = mLobby.getCountdowns(temp_countdownNumber, temp_gameMode, temp_delay, temp_participant, temp_status, temp_tags) ;
					// we prioritize a countdown in which we are a participant.
					int bestType = 0 ;
					// use: 0 --> 3
					// 3: Active and we participate.
					// 2: Inactive and we participate.
					// 1: Have vote but no launch
					// 0: No personal votes
					for ( int i = 0; i < num && bestType < 3; i++ ) {
						if ( bestType < 3 && temp_status[i] == Lobby.COUNTDOWN_STATUS_ACTIVE && temp_participant[i][playerSlot] ) {
							countdownNum = temp_countdownNumber[i] ;
							countdownGameMode = temp_gameMode[i] ;
							bestType = 3 ;
						} else if ( bestType < 2 && temp_status[i] == Lobby.COUNTDOWN_STATUS_HALTED && temp_participant[i][playerSlot] ) {
							countdownNum = temp_countdownNumber[i] ;
							countdownGameMode = temp_gameMode[i] ;
							bestType = 2 ;
						}
					}
					if ( bestType == 0 ) {
						int [] gameModes = mLobby.getGameModes() ;
						for ( int i = 0; i < gameModes.length; i++ ) {
							if ( mLobby.getLocalVote(gameModes[i]) ) {
								countdownGameMode = gameModes[i] ;
								bestType = 1 ;
							}
						}
					}

					description = TextFormatting.format(
							getContext(),
							null,
							TextFormatting.DISPLAY_MENU,
							TextFormatting.TYPE_LOBBY_PERSONAL_LAUNCH_STATUS,
							role,
							countdownGameMode,
							countdownNum,
							mLobby) ;
				}
			} else {
				title = getResources().getString(R.string.menu_lobby_empty_title) ;
				description = getResources().getString(R.string.menu_lobby_empty_description) ;
			}
			color = mMainColor ;
			break ;
			
		case BUTTON_TAB_CHAT:
		case BUTTON_TAB_VOTE:
		case BUTTON_SHARE:
		case BUTTON_INFO:
		case BUTTON_HELP:
		case BUTTON_SETTINGS:	
			title = mTitle[buttonType] ;
			imageDrawable = mImageDrawable[buttonType] ;
			color = mColor[buttonType] ;
			break ;
		}
		
		// Now set these values in the appropriate content fields.
		QuantroButtonAccess qbs = getButtonAccess(buttonNum) ;
		
		// set text
		changed = qbs.setTitle(title)  					|| changed ;
		changed = qbs.setDescription(description) 		|| changed ;
		// set background drawable
		changed = qbs.setImage(true, imageDrawable) 	|| changed ;
		// button base color
		changed = qbs.setColor(color) 					|| changed ;
		
		if ( buttonType == BUTTON_MAIN )
			setButtonEnabled( buttonNum, false ) ;
		
		return changed ;
	}
	
	int [] temp_countdownNumber = null ;
	int [] temp_gameMode = null ;
	long [] temp_delay = null ;
	boolean [][] temp_participant = null ;
	int [] temp_status = null ;
	Object [] temp_tags = null ;
	
	private void allocate() {
		int num = mLobby.getMaxPeople() ;
		if ( temp_countdownNumber == null || temp_countdownNumber.length != num ) {
			// allocate these arrays
			temp_countdownNumber = new int[num] ;
			temp_gameMode = new int[num] ;
			temp_delay = new long[num] ;
			temp_participant = new boolean[num][num] ;
			temp_status = new int[num] ;
			temp_tags = new Object[num] ;
		}
	}

	@Override
	public void qbsc_onClick(QuantroButtonStrip strip, int buttonNum, boolean asOverflow) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;		// we did not handle it
		
		if ( buttonNum > -1 )
			delegate.ltbbs_onButtonClick(this, buttonNum, asOverflow) ;
	}

	@Override
	public boolean qbsc_onLongClick(QuantroButtonStrip strip, int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;		// we did not handle it
		
		if ( buttonNum > -1 )
			return delegate.ltbbs_onButtonLongClick(this, buttonNum) ;
		
		// Didn't handle it.
		return false ;
	}

	@Override
	public boolean qbsc_supportsLongClick(QuantroButtonStrip strip,
			int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;		// we did not handle it
		
		if ( buttonNum == BUTTON_SHARE )
			return true ;
		
		return false ;
	}
	
	public void qbsc_onOverflowClicked( QuantroButtonStrip strip ) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;
		delegate.ltbbs_onOverflowClicked(this) ;
	}
	
}
