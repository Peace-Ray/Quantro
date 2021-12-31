package com.peaceray.quantro.view.lobby;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.peaceray.quantro.R;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.lobby.LobbyLog;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.view.button.collage.MultiPlayerGameLaunchButtonCollage;
import com.peaceray.quantro.view.button.strip.LobbyTitleBarButtonStrip;
import com.peaceray.quantro.view.button.strip.MultiPlayerGameLaunchButtonStrip;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.colors.GameModeColors;
import com.peaceray.quantro.view.drawable.ColorTabsDrawable;
import com.peaceray.quantro.view.generic.PinnedHeaderListView;
import com.peaceray.quantro.view.options.OptionAvailability;
import com.velosmobile.utils.SectionableAdapter;


/**
 * The LobbyViewComponentAdapter does not contain a "LobbyView" in the strictest
 * sense; rather, it allows a set of lobby-relevant content objects to be set,
 * and it coordinates communication between them.  The purpose is to allow a
 * paper-thin View to be wrapped around this functional skeleton, but to be equally
 * useful in a tabbed design where the relevant views do not even exist in the
 * same layout, and thus cannot be contained inside a custom ViewGroup (the way
 * that ChallengeManagerStripView works, for example).
 * 
 * The following Views may or may not appear in the layout, their IDs being set
 * from outside:
 * 
 * Lobby Name: if present, we set its contents to the name of this lobby.
 * 
 * Lobby Description: if present, we set its contents to a basic desscription
 * 		of the lobby, including e.g. "Quantro WiFi lobby" and "Open since 6:25 pm"
 * 
 * Lobby Membership: if present, displays a color-coded list of current lobby members.
 * 
 * Lobby Launch Status: if present, gives a general description of the current launch
 * 		status of the lobby: waiting for players, waiting for votes, etc.
 * 
 * Lobby Population: a simple number giving how populated the lobby is at the moment.
 * 
 * Lobby Instructions General: very general instructions, probably regarding the
 * 		the behavior of "Back" and "Home."
 * 
 * Lobby Chat Log: A View for displaying the chat log and other events, such as
 * 		people entering or leaving the lobby.
 * 
 * Lobby Chat Edit Text: a place for people to enter text they want to send to
 * 		the chat.  We read this text upon click of the "post" button.
 * 
 * Lobby Chat Post Button: we set ourself as the onClickListener for this button;
 * 		when pressed, we take text from (and empty) our Edit Text view, and post it
 * 		as a message to the lobby.
 * 
 * Lobby Game Mode List: a ListView for storing game modes.  We store
 * 		MultiplayerGameLaunchButtonStrips as items in this list.
 * 
 * Lobby Instructions Chat: instructions on how to chat.
 * 
 * Lobby Instructions Vote: instructions on how to vote.
 * 
 * @author Jake
 *
 */
public class LobbyViewComponentAdapter
		implements LobbyViewAdapter, View.OnClickListener, View.OnLongClickListener, MultiPlayerGameLaunchButtonStrip.Delegate, MultiPlayerGameLaunchButtonCollage.Delegate {
	
	
	private static final String TAG = "LVComponentAdapter" ;
	
	
	// COMPONENTS.  These ints let us select various components.  We
	// assume that each component is a View (most are actually TextViews); subclassing
	// these is perfectly fine, as long as they respond to the appropriate
	// View methods, but unfortunately we can't make a fully generic implicit 
	// interface for these components (this isn't Go).  That makes this 
	// ComponentAdapter somewhat limited in application.
	
	// TODO: If we want to retain the functionality of this ComponentAdapter
	// while allowing structures other than Views (maybe custom ViewGroups
	// instead?), put in some Interfaces for components and allow either
	// Views or implementations of those interfaces to be set as components.
	public static final int COMPONENT_LOBBY_NAME		= 0 ;		// TextView
	public static final int COMPONENT_LOBBY_DESCRIPTION = 1 ;		// TextView
	public static final int COMPONENT_LOBBY_AVAILABILITY_DESCRIPTION = 2 ;		// TextView
	public static final int COMPONENT_LOBBY_INVITATION_DESCRIPTION  = 3 ;		// TextView
	public static final int COMPONENT_LOBBY_DETAILS  = 4 ;		// TextView
	
	
	public static final int COMPONENT_LOBBY_MEMBERSHIP 	= 5 ;		// TextView.  We apply colored text to this.
	public static final int COMPONENT_LOBBY_LAUNCH_STATUS	= 6 ;	// TextView.  A global "launch" status which should be always visible.
	public static final int COMPONENT_LOBBY_POPULATION	= 7 ;		// TextView.  A very simple place for "1/2", etc.
	
	public static final int COMPONENT_LOG_LIST			= 8 ;		// ListView.  A list for storing LobbyLog events, including chat dialog.
	public static final int COMPONENT_CHAT_EDIT_TEXT	= 9 ;		// EditText.  A place for the user to type chat comments.
	public static final int COMPONENT_CHAT_POST_BUTTON 	= 10 ;		// A View.  We set ourself as onClickListener.
	
	public static final int COMPONENT_GAME_MODE_LIST 	= 11 ;		// A ListView.  We populate it with MultiPlayerGameLaunchButtonStrips.
	
	public static final int COMPONENT_INSTRUCTIONS_GENERAL 	= 12 ;	// TextView.  A place for general instructions, such as "press 'Back' to leave the lobby."
	public static final int COMPONENT_INSTRUCTIONS_VOTE 	= 13;	// TextView.  Instructions on voting and its purpose.
	public static final int COMPONENT_INSTRUCTIONS_CHAT		= 14; 	// TextView.  Instructions for chatting.
	
	// Special membership views to divide the membership.
	public static final int COMPONENT_LOBBY_MEMBERSHIP_PAIR_1			= 15 ;
	public static final int COMPONENT_LOBBY_MEMBERSHIP_PAIR_2			= 16 ;
	public static final int COMPONENT_LOBBY_MEMBERSHIP_PAIR_3			= 17 ;
	public static final int COMPONENT_LOBBY_MEMBERSHIP_PAIR_4			= 18 ;
	
	
	public static final int NUM_COMPONENTS = 19 ;
	
	
	public static final long MAX_DELAY_LOBBY_LOG_SOUND = 3000 ;		// If the log entry has been sitting idle for this long,
																	// we do NOT play a sound for it.
	
	
	// Component IDs.  These are IDs for the views above.  We use -1 for unset IDs.
	protected int [] mComponentID ;
	// Views.  References to the components.  We load them on 'start', and
	// whenever a component is set while we are running.
	protected View [] mComponentView ;
	
	protected LobbyTitleBarButtonStrip mLobbyTitleBarButtonStrip ;
	
	// Our context.
	protected WeakReference<Context> mwrContext ;
	protected Resources mResources ;
	// A root view from which components may be located by Id.
	protected View mRootView ;
	// Our delegate.
	protected WeakReference<Delegate> mwrDelegate ;
	// Our data: the lobby, and its log.
	protected Lobby mLobby ;
	protected int [] mCachedPlayerStatuses ;		// used for comparisons to determine which sounds to play.
	protected Lobby mLobbyUpdateSnapshot ;	// used for updates: take a snapshot of the current lobby and alter our content based on it.
	protected LobbyLog mLobbyLog ;
	// Additional data.  We keep records of data that isn't explicitly
	// present in the Lobby object, and that can't be inferred from
	// lobby updates and other state change methods.
	protected int [] mPlayerColor ;			// the color assigned to each player, by player slot.
	protected Integer [] mPlayerColorInteger ;
	// Game Mode availability.  If unspecified, we assume ENABLED.
	protected Hashtable<Integer, OptionAvailability> mGameModeAvailability ;
	
	// ArrayAdapters for handling our lists: multiplayer votes and the log.
	protected MultiPlayerGameLaunchButtonCollageAdapter mGameVoteAdapter ;
	protected LobbyLogEventArrayAdapter mLobbyLogEventArrayAdapter ;
	
	// A handler for our runnables, and those runnables themselves!
	protected Handler mHandler ;
	protected Runnable mUpdateBasicsRunnable ;
	protected Runnable mUpdateVotesRunnable ;
	protected Runnable mUpdateLaunchesRunnable ;
	protected Runnable mUpdatePlayersRunnable ;
	protected Runnable mUpdateLogRunnable ;
	
	// Are we currently going?
	protected boolean mStarted = false ;
	// Wifi? (or internet?)
	protected boolean mWifi = false ;
	protected boolean mHost = false ;
	protected boolean mHasAndroidBeam = false ;
	
	protected ColorScheme mColorScheme ;
	protected QuantroSoundPool mSoundPool ;
	protected boolean mSoundControls ;
	
	// Background for our vote list.
	protected ColorTabsDrawable mVoteTabs ;
	
	/**
	 * Constructor!  Allocates a lot of stuff.  The LobbyViewComponentAdapter
	 * is not ready to be started; you should set delegate, lobby, etc.
	 */
	public LobbyViewComponentAdapter( Context context ) {
		mComponentID = new int[NUM_COMPONENTS] ;
		mComponentView = new View[NUM_COMPONENTS] ;
		for ( int i = 0; i < NUM_COMPONENTS; i++ ) {
			mComponentID[i] = -1 ;
			mComponentView[i] = null ;
		}
		
		// Links to outside info - context, rootview, delegate, etc.
		mwrContext = new WeakReference<Context>(context) ;
		mResources = context.getResources() ;
		mRootView = null ;
		mwrDelegate = new WeakReference<Delegate>(null) ;
		mLobby = null ;
		mCachedPlayerStatuses = null ;
		mLobbyUpdateSnapshot = new Lobby() ;
		mLobbyLog = null ;
		
		mGameModeAvailability = new Hashtable<Integer, OptionAvailability>() ;
		
		// Color tabs!
		mVoteTabs = new ColorTabsDrawable(context) ;
		mVoteTabs.setNumberOfTabs(8) ;
		
		// Player color is set large enough to hold information for all lobby members.
		// We do this preemptively, to avoid race conditions with access / allocation
		// of this array.  Remember that we might get player color information BEFORE
		// we have updated ourselves for lobby membership.
		mPlayerColor = new int[8] ;
		mPlayerColorInteger = new Integer[8] ;
		for ( int i = 0; i < 8; i++ ) {
			mPlayerColor[i] = 0xffffffff ;
			mPlayerColorInteger[i] = new Integer( mPlayerColor[i] ) ;
		}
		
		mGameVoteAdapter = 
			new MultiPlayerGameLaunchButtonCollageAdapter(
					((Activity)context).getLayoutInflater(),
					R.layout.multiplayer_button_collage_list_item,
					0,
					R.id.multiplayer_game_mode_list_row) ;
		mLobbyLogEventArrayAdapter = new LobbyLogEventArrayAdapter(context, -1) ;
		mLobbyLogEventArrayAdapter.setNotifyOnChange(true) ;
		
		// A handler for our runnables, and those runnables themselves!
		mHandler = new Handler() ;
		mUpdateBasicsRunnable = new UpdateBasicsRunnable() ;
		mUpdateVotesRunnable = new UpdateVotesRunnable() ;
		mUpdateLaunchesRunnable = new UpdateLaunchesRunnable() ;
		mUpdatePlayersRunnable = new UpdatePlayersRunnable() ;
		mUpdateLogRunnable = new UpdateLogRunnable() ;
		
		// Are we currently going?
		mStarted = false ;
		// Wifi? (or internet?)
		mWifi = false ;
		mHost = false ;
	}
	

	/**
	 * Sets the controller for this view.
	 */
	synchronized public void setDelegate( LobbyViewAdapter.Delegate delegate ) {
		mwrDelegate = new WeakReference<Delegate>(delegate) ;
	}
	
	/**
	 * Sets half the "model" for this view; the Lobby object.  This is intended
	 * to be a one-way information stream, with the LobbyView displaying what's
	 * in the LobbyClient, but using the Controller to actually make updates.
	 * @param lc
	 */
	synchronized public void setLobby( Lobby lobby ) {
		mLobby = lobby ;
		// Log.d(TAG, "given lobby: " + mLobby) ;
		if ( mStarted )
			update_lobby() ;
	}
	
	
	/**
	 * Sets the other half of the "model" for this view: the LobbyLog.  This, like
	 * Lobby, is intended as a one-way information stream, with the LobbyView
	 * displaying either the LobbyLog or a recent (perhaps partial) snapshot of
	 * it, using the Controller to make updates.
	 * @param lobbyLog
	 */
	synchronized public void setLobbyLog( LobbyLog lobbyLog ) {
		mLobbyLog = lobbyLog ;
		if ( mStarted ) 
			update_log() ;
	}
	
	
	/**
	 * Sets the availability of the specified game mode.  Note that the actual game
	 * modes are read from a lobby object; this method only specifies the
	 * button availability of an existing mode.
	 * 
	 * Note also that, by default, we follow "black list" behavior of "DEFAULT ALLOW."
	 * That is, a particular game mode is assumed to be OptionAvailability.ENABLED
	 * unless this method is called with a different parameter.
	 * 
	 * @param gameMode
	 * @param availability
	 */
	synchronized public void setGameModeAvailability( int gameMode, OptionAvailability availability ) {
		mGameModeAvailability.put(gameMode, availability) ;
		if ( mGameVoteAdapter != null ) {
			mGameVoteAdapter.updateAvailability(Integer.valueOf(gameMode), availability) ;
		}
		if ( mStarted )
			this.update_votes() ;
	}
	
	
	/**
	 * Sets whether this lobby is local Wifi only, the alternative being
	 * internet.
	 * 
	 * @param wifi
	 */
	@Override
	synchronized public void setIsWifiOnly( boolean wifi ) {
		mWifi = wifi ;
		if ( mStarted )
			update_basics() ;
	}
	
	
	/**
	 * Sets whether this lobby user is acting as the host for the lobby,
	 * the alternative being client-only.
	 * 
	 * @param host
	 */
	@Override
	public void setIsHost( boolean host ) {
		mHost = host ;
		if ( mStarted )
			update_basics() ;
	}
	
	/**
	 * Sets whether the user has Android Beam available at this moment.
	 * @param enabled
	 */
	@Override
	public void setAndroidBeamIsEnabled( boolean enabled ) {
		mHasAndroidBeam = enabled ;
		if ( mStarted )
			update_basics() ;
	}
	
	/**
	 * Sets the color scheme used by this lobby.  It might be ignored.
	 * @param cs
	 */
	public void setColorScheme( ColorScheme cs ) {
		mColorScheme = cs ;
		if ( mStarted ) {
			update_basics() ;
			update_myVotes() ;
			update_votes() ;
			update_launches() ;
			update_players() ;
			update_lobby() ;
			update_log() ;
		}
	}
	
	/**
	 * Sets the sound pool for this lobby view adapter.
	 * @param soundPool
	 */
	public void setSoundPool( QuantroSoundPool soundPool ) {
		mSoundPool = soundPool ;
	}
	
	public void setSoundControls( boolean soundControls ) {
		mSoundControls = soundControls ;
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// UPDATES
	//
	// LobbyView will receive update messages when LobbyClient changes.  These
	// updates are sent by the Controller.  It's up to the LobbyView to handle
	// retrieval of the actual info from LobbyClient and update its own display.
	//
	///////////////////////////////////////////////////////////////////////////	
	
	
	// Updates regarding the lobby object.
	
	@Override
	/**
	 * The most basic update: player slot and the like.  This should (probably)
	 * only be called once; however, it is called in 'update', so make sure performing
	 * this function doesn't trample other data.
	 */
	synchronized public void update_basics() {
		if ( !mStarted )
			return ;
		
		// post the appropriate runnable.
		mHandler.post(mUpdateBasicsRunnable) ;
		// that's it.  we done!
	}
	
	@Override
	/**
	 * This player's votes have updated.  These updates reflect
	 * local changes - i.e., the LobbyClient knows we voted / unvoted
	 * for something, but the LobbyServer may not know yet.
	 */
	synchronized public void update_myVotes() {
		// We don't make a strong distinction between our votes
		// and general lobby votes.  Call update_votes immediately.
		update_votes() ;
	}
	
	@Override
	/**
	 * Votes have been updated!
	 */
	synchronized public void update_votes() {
		if ( !mStarted )
			return ;
		
		// post the appropriate runnable.
		mHandler.post(mUpdateVotesRunnable) ;
		// that's it.  we done!
	}
	
	@Override
	/**
	 * Game launches have updated.  Get new information.
	 */
	synchronized public void update_launches() {
		if ( !mStarted )
			return ;
		
		// post the appropriate runnable.
		mHandler.post(mUpdateLaunchesRunnable) ;
		// that's it.  we done!
	}
	
	
	synchronized public void update_launches(long delay) {
		if ( !mStarted )
			return ;
		
		// post the appropriate runnable.
		mHandler.postDelayed(mUpdateLaunchesRunnable, delay) ;
		// that's it.
	}
	
	@Override
	/**
	 * The players in the lobby have changed, either by who's
	 * present, or their names.  Perform an update.
	 */
	synchronized public void update_players() {
		if ( !mStarted )
			return ;
		
		// post the appropriate runnable.
		mHandler.post(mUpdatePlayersRunnable) ;
		// that's it.  we done!
	}
	
	@Override
	/**
	 * Performs all above updates.
	 */
	synchronized public void update_lobby() {
		update_basics() ;
		update_votes() ;
		update_launches() ;
		update_players() ;
	}
	
	@Override
	/**
	 * A generic method, called whenever LobbyLog changes.
	 */
	synchronized public void update_log() {
		if ( !mStarted )
			return ;
		
		// post the appropriate runnable.
		mHandler.post(mUpdateLogRunnable) ;
		// that's it.  we done!
	}
	
	
	@Override
	/**
	 * Starts the thread for a LobbyView
	 */
	synchronized public void start() {
		
		// First, one helpful thing to do is retrieve references to any Views whose Ids we've
		// been given.
		if ( mRootView != null )
			getViewReferences() ;

		mStarted = true ;
		
		// We use the cache to avoid replaying sound effects.  Update it now; we don't want to play
		// sounds as we're first getting up-to-date.
		if ( mCachedPlayerStatuses == null )
			mCachedPlayerStatuses = new int[mLobby.getMaxPeople()] ;
		for ( int i = 0; i < mCachedPlayerStatuses.length; i++ )
			mCachedPlayerStatuses[i] = mLobby.getPlayerStatus(i) ;
		
		// nows a good time to update
		update_lobby() ;
		update_log() ;
	}

	
	
	@Override
	/**
	 * Stops the lobby view thread, if one exists.  This will be 
	 * called before the activity is suspended.  The LobbyView should
	 * stop updating after this call, but should still respond to 'start()'
	 * or 'resume' without loss of functionality.
	 */
	synchronized public void stop() {
		mHandler.removeCallbacks(mUpdateBasicsRunnable) ;
		mHandler.removeCallbacks(mUpdateVotesRunnable) ;
		mHandler.removeCallbacks(mUpdatePlayersRunnable) ;
		mHandler.removeCallbacks(mUpdateLaunchesRunnable) ;
		mHandler.removeCallbacks(mUpdateLogRunnable) ;
 		mStarted = false ;
	}

	@Override
	/**
	 * Blocks until the LobbyView thread is finished.  If the LobbyView
	 * has no dedicated thread, this method should immediately return.
	 */
	synchronized public void join() {
		// TODO Auto-generated method stub

	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// SETTING DISPLAY CONFIGURATION
	// 
	// Some important display info other than components may come in from outside.
	// For example, we allow servers (and some users, e.g. XL customers) to set
	// the color representing them in lobbies.  This information is not relevant
	// to a Lobby instance, so it is not stored there.  Instead it is passed
	// directly in.
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Sets the color for the specified player slot.
	 */
	synchronized public void setPlayerColor( int slot, int color ) {
		// colors MUST be visible against black!
		if ( ( Color.red(color) < 64 && Color.green(color) < 64 && Color.blue(color) < 64 )
				|| Color.alpha(color) < 64 ) {
			color = Color.argb(
					255,
					Math.max(64, Color.red(color)),
					Math.max(64, Color.green(color)),
					Math.max(64, Color.blue(color))) ;
		}
		mPlayerColor[slot] = color ;
		mPlayerColorInteger[slot] = new Integer(color) ;
		mVoteTabs.setTabColor(slot, color) ;
		
		// This update will automatically affect most things, but we may
		// want to update the player color on all vote bars.
		update_votes() ;
	}
	
	
	/**
	 * Gets the color for the specified player slot.
	 * @param slot
	 * @return
	 */
	synchronized public int getPlayerColor( int slot ) {
		return mPlayerColor[slot] ;
	}
	
	
	/**
	 * Unsets the player color for this player, returning it to the default
	 * (white).
	 * @param slot
	 */
	synchronized public void unsetPlayerColor( int slot ) {
		mPlayerColor[slot] = 0xffffffff ;
		mPlayerColorInteger[slot] = new Integer(mPlayerColor[slot]) ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// SETTING COMPONENTS
	// 
	// Because this Adapter is not a View, it is not inflated from XML.  Instead,
	// components must be programmatically set from outside.  These components
	// may be set either as View IDs (integers), or as references to the Views
	// themselves.  If provided as IDs, they should be somewhere in the view
	// hierarchy rooted at the provided root view.
	//
	// Views will be retrieved by a call to findViewById, at the first available
	// moment when all three of the following conditions are true:
	//
	// 1. a root view is set
	// 2. a component view ID is set
	// 3. the LobbyViewComponentAdapter has "started" (i.e., a call to start has been made
	//			without a corresponding call to "stop").
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	synchronized public void setLobbyTitleBarButtonStrip( LobbyTitleBarButtonStrip ltbbs ) {
		mLobbyTitleBarButtonStrip = ltbbs ;
	}
	
	/**
	 * Sets the root view for any components set, or to be set,
	 * using their ID rather than a direct View reference.
	 * 
	 * @v The root view
	 */
	synchronized public void setComponentRootView( View v ) {
		mRootView = v ;
		if ( mRootView != null && mStarted )
			getViewReferences() ;
	}
	
	
	synchronized public void setComponent( int componentName, int id ) {
		mComponentID[componentName] = id ;
		mComponentView[componentName] = null ; 
		// Load this, if appropriate at this time.
		if ( mStarted && mRootView != null ) {
			mComponentView[componentName] = mRootView.findViewById(id) ;
			setUpComponent( componentName ) ;
		}
	}
	
	synchronized public void setComponent( int componentName, View v ) {
		mComponentView[componentName] = v ;
		mComponentID[componentName] = v.getId() ;
		setUpComponent( componentName ) ;
	}
	
	synchronized public void unsetComponent( int componentName ) {
		mComponentView[componentName] = null ;
		mComponentID[componentName] = -1 ;
	}
	
	
	/**
	 * Iterates through view component IDs, and for each ID != -1 where
	 * the corresponding view is null, gets a reference using findViewById
	 * rooted at mRootView.
	 */
	synchronized protected void getViewReferences() {
		for ( int i = 0; i < NUM_COMPONENTS; i++ ) {
			if ( mComponentID[i] != -1 && mComponentView[i] == null ) {
				mComponentView[i] = mRootView.findViewById( mComponentID[i] ) ;
				setUpComponent( i ) ;
			}
		}
	}
	
	/**
	 * For certain components, it is appropriate to perform some specialized setup
	 * or bookkeeping at the time they are loaded.  For example, we may want
	 * to associate an ArrayAdapter with a ListView component.  This is the place
	 * to do it.
	 * 
	 * @param componentName The component we should "set up."
	 */
	synchronized protected void setUpComponent( int componentName ) {
		
		View v = mComponentView[componentName] ;
		EditText et ;
		ListView lv ;
		
		switch ( componentName ) {
		case COMPONENT_CHAT_POST_BUTTON:
			if ( v != null ) {
				// register ourself as the on click listener,
				// and on long-click listener.
				v.setOnClickListener(this) ;
				v.setOnLongClickListener(this) ;
				
				// by the way, we only enable this button when there
				// is content in the chat edit text view.
				v.setEnabled(hasPostableText()) ;
			}
			break ;
		
		case COMPONENT_GAME_MODE_LIST:
			if ( v != null ) {
				lv = (ListView)v ;
				// link it to the array adapter for votes!
				lv.setAdapter(mGameVoteAdapter) ;
				lv.setOnScrollListener(mGameVoteAdapter) ;
				lv.setItemsCanFocus(true) ;
				if ( lv instanceof PinnedHeaderListView ) {
	    			View pinnedHeaderView = ((Activity)mwrContext.get()).getLayoutInflater().inflate(R.layout.quantro_list_item_header, lv, false) ;
	    			pinnedHeaderView.setTag( new MultiPlayerGameLaunchButtonRowTag(pinnedHeaderView ) ) ;
	    			((PinnedHeaderListView)lv).setPinnedHeaderView( pinnedHeaderView ) ;
	    		}
				// no dividers; don't need them with our button format
				lv.setDivider(null) ;
				lv.setDividerHeight(0) ;
				// use the ColorTabs as background for this view.
				Log.d(TAG, "setting background for list view") ;
				VersionSafe.setBackground(lv, mVoteTabs) ;
			}
			break ;
		
		case COMPONENT_LOG_LIST:
			if ( v != null ) {
				lv = (ListView)v ;
				// list it to the array adapter for log events!
				lv.setAdapter(mLobbyLogEventArrayAdapter) ;
				mLobbyLogEventArrayAdapter.setListView(lv) ;
				// no dividers; don't need them for text paragraphs
				lv.setDivider(null) ;
				lv.setDividerHeight(0) ;
			}
			break ;
			
		case COMPONENT_CHAT_EDIT_TEXT:
			// Set up an update listener to change the enabled...ness
			// of the chat post button.
			if ( v != null ) {
				et = (EditText)v ;
				et.addTextChangedListener( new TextWatcher() {
		        	public void beforeTextChanged( CharSequence s, int start, int count, int after ) { }
		        	public void onTextChanged( CharSequence s, int start, int before, int count ) { }
		        	public void afterTextChanged( Editable s ) {
		        		View v = mComponentView[COMPONENT_CHAT_POST_BUTTON] ;
		        		if ( v != null )
		        			v.setEnabled(s.length() > 0) ;
		        	}
		        }) ;
			}
			break ;	
		}
	}
	
	
	
	/**
	 * Returns true if a chat post component has been set,
	 * it is non-null, and it has text of length > 0.
	 * 
	 * @return
	 */
	private boolean hasPostableText() {
		View v = mComponentView[COMPONENT_CHAT_EDIT_TEXT] ;
		if ( v != null && v instanceof TextView )
			return ((TextView)v).getText().length() > 0 ;
		return false ;
	}
	
	
	/**
	 * Returns, as a CharSequence, the text the user has entered
	 * into the CHAT_EDIT_TEXT.  Returns 'null' if no such component
	 * has been set, or it is not a TextView.  In any case, if
	 * a CharSequence is returned, the text of the chat edit
	 * text component has been set to the empty string.
	 * @return
	 */
	private CharSequence getAndEmptyPostableText() {
		CharSequence cs = null ;
		View v = mComponentView[COMPONENT_CHAT_EDIT_TEXT] ;
		if ( v != null && v instanceof TextView ) {
			cs = ((TextView)v).getText() ;
			((TextView)v).setText("") ;
		}
		return cs ;
	}
	
	
	
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// ON CLICK LISTENER, ON LONG CLICK LISTENER DELEGATES
	// 
	// Some things are generic clickable views, such as the chat post button.
	//
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * On click!
	 */
	@Override
	synchronized public void onClick( View v ) {
		Delegate delegate = mwrDelegate.get() ;
		if ( v == mComponentView[COMPONENT_CHAT_POST_BUTTON] ) {
			// someone wants to post some text.  Do it if we can.
			if ( delegate != null && hasPostableText() ) {
				CharSequence cs = getAndEmptyPostableText() ;
				v.setEnabled(false) ;
				delegate.lvad_userSentTextMessage(new StringBuilder().append(cs).toString()) ;
				if ( mSoundControls && mSoundPool != null )
					mSoundPool.menuButtonClick() ;
				// Can we maybe do this without allocating new objects?
			}
		}
	}
	
	/**
	 * On longClick!
	 */
	@Override
	synchronized public boolean onLongClick( View v ) {
		// nope, nothing yet
		return false ;
	}
	
	////////////////////////////////////////////////////////////////////////////
	//
	// Multiplayer Game Launch Button Collage Delegate
	//
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * The player wants to vote for the specified game type.
	 * @param collage
	 * @param mGameMode
	 */
	public boolean mpglbcd_vote( MultiPlayerGameLaunchButtonCollage collage, int gameMode ) {
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return false ;
		
		delegate.lvad_userSetVote(gameMode, true) ;
		return mSoundControls ;
	}
	
	
	/**
	 * The player wants to rescind their vote for the specified game type.
	 * @param collage
	 * @param mGameMode
	 */
	public boolean mpglbcd_unvote( MultiPlayerGameLaunchButtonCollage collage, int gameMode ) {
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return false ;
		
		delegate.lvad_userSetVote(gameMode, false) ;
		return mSoundControls ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// MULTIPLAYER_GAME_LAUNCH_BUTTON_STRIP LISTENER
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * The user has short-clicked a button we for game launches.  The
	 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
	 * the right action.
	 * 
	 * @param strip
	 * @param buttonNum
	 * @param buttonType
	 * @param gameMode
	 * @param saveKey
	 */
	public boolean mpglbs_onButtonClick(
			MultiPlayerGameLaunchButtonStrip strip,
			int buttonNum, int buttonType, int gameMode ) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return false ;
		
		// This is how players vote.
		switch( buttonType ) {
		case MultiPlayerGameLaunchButtonStrip.BUTTON_TYPE_VOTE:
			// set our vote to what it WASN'T before.
			delegate.lvad_userSetVote(gameMode, !mLobby.getLocalVote(gameMode)) ;
			if ( mSoundControls )
				mSoundPool.menuButtonClick() ;
			return true ;
			
		default:
			return false ;
		}
	}
	
	/**
	 * The user has long-clicked a button we for game launches.  The
	 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
	 * the right action.
	 * 
	 * @param strip
	 * @param buttonNum
	 * @param buttonType
	 * @param gameMode
	 * @param saveKey
	 */
	public boolean mpglbs_onButtonLongClick(
			MultiPlayerGameLaunchButtonStrip strip,
			int buttonNum, int buttonType, int gameMode ) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return false ;
		
		// This is how players vote.
		switch( buttonType ) {
		case MultiPlayerGameLaunchButtonStrip.BUTTON_TYPE_VOTE:
			// TODO: show a description of this game type.
			return false ;
			
		default:
			return false ;
		}
		
	}
	
	

	public boolean mpglbs_supportsLongClick(
			MultiPlayerGameLaunchButtonStrip strip,
			int buttonNum, int buttonType, int gameMode ) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return false ;
		
		// This is how players vote.
		switch( buttonType ) {
		case MultiPlayerGameLaunchButtonStrip.BUTTON_TYPE_VOTE:
			// TODO: show a description of this game type.
			return false ;
			
		default:
			return false ;
		}
		
	}
	
	
	
	private static class MultiPlayerGameModeIndexer implements SectionIndexer {
		// when only VS was allowed, we had 2 sections: QUANTRO and RETRO.
		// We're changing it to VS for each (quantro / retro) and 3+ for each
		// (Quantro / Retro).
		protected static final int SECTION_QUANTRO_VS = 0 ;
		protected static final int SECTION_RETRO_VS = 1 ;
		protected static final int SECTION_QUANTRO_FREE4ALL = 2 ;
		protected static final int SECTION_RETRO_FREE4ALL = 3 ;
		protected static final int NUM_SECTIONS = 4 ;
		
		protected String [] mSections = new String[]{ "Quantro 2-Player", "Retro 2-Player", "Quantro 3+ Players", "Retro 3+ Players" } ;
		protected Integer [] mGameModes ;
		
		public MultiPlayerGameModeIndexer( Integer [] gameModes ) {
			mGameModes = gameModes ;
		}
		
		synchronized public void setGameModes( Integer [] gameModes ) {
			mGameModes = gameModes ;
		}
		
		synchronized public Object[] getSections() {
	        return mSections ;
	    }
	    
	    /**
	     * Performs a binary search or cache lookup to find the first row that
	     * matches a given section.
	     * @param sectionIndex the section to search for
	     * @return the row index of the first occurrence, or the nearest next index.
	     * For instance, if section is '1', returns the first index of a received
	     * challenge.  If there are no received challenges, returns the length
	     * of the array.
	     */
		synchronized public int getPositionForSection(int sectionIndex) {
	    	
			// QUANTRO VS
	    	// If section index is 0, then no matter what, we return 0.
	    	if ( sectionIndex == SECTION_QUANTRO_VS )
	    		return 0 ;
	    	
	    	// RETRO VS
	    	// If section index is 1, then return the first game which is NOT 3d,
	    	// or is custom.
	    	if ( sectionIndex == SECTION_RETRO_VS ) {
	    		for ( int i = 0; i < mGameModes.length; i++ ) {
	    			if ( GameModes.isCustom(mGameModes[i]) || GameModes.numberQPanes(mGameModes[i]) == 1 ) {
	    				return i ;
	    			}
	    		}
	    	}
	    	
	    	// QUANTRO FREE 4 ALL
	    	// If section index is 2, we return the first game mode with more than 2 players.
	    	if ( sectionIndex == SECTION_QUANTRO_FREE4ALL ) {
	    		for ( int i = 0; i < mGameModes.length; i++ ) {
	    			if ( GameModes.minPlayers(mGameModes[i]) > 2 ) {
	    				return i ;
	    			}
	    		}
	    	}
	    	
	    	// RETRO FREE 4 ALL
	    	// If section index is 3, we return the first game mode with 1 qpane and more
	    	// than 2 players.
	    	if ( sectionIndex == SECTION_RETRO_FREE4ALL ) {
	    		for ( int i = 0; i < mGameModes.length; i++ ) {
	    			if ( GameModes.minPlayers(mGameModes[i]) > 2 && GameModes.numberQPanes(mGameModes[i]) == 1 ) {
	    				return i ;
	    			}
	    		}
	    	}
	    	
	    	// huh?
	    	return mGameModes.length ;
	    }
	    
	    /**
	     * Returns the section index for a given position in the list by
	     * examining the item.
	     */
		synchronized public int getSectionForPosition(int position) {
			if ( position < 0 || position >= mGameModes.length )
				return -1 ;
			boolean is3d = GameModes.numberQPanes(mGameModes[position]) == 2 ;
			boolean is2p = GameModes.minPlayers(mGameModes[position]) == 2 ;
			if ( is3d && is2p )
				return SECTION_QUANTRO_VS ;
			if ( !is3d && is2p )
				return SECTION_RETRO_VS ;
			if ( is3d && !is2p )
				return SECTION_QUANTRO_FREE4ALL ;
			if ( !is3d && !is2p )
				return SECTION_RETRO_FREE4ALL ;
			
	    	return 0 ;
	    }
		
		synchronized public boolean isFirstInSection( int position ) {
			if ( position < 0 || position >= mGameModes.length )
				return false ;
			if ( position == 0 )
				return true ;
			boolean is3d = GameModes.numberQPanes(mGameModes[position]) == 2 ;
			boolean is3d_pre = GameModes.numberQPanes(mGameModes[position-1]) == 2 ;
			boolean is2p = GameModes.minPlayers(mGameModes[position]) == 2 ;
			boolean is2p_pre = GameModes.minPlayers(mGameModes[position-1]) == 2 ;
			
			
			// a difference indicates change-of-section.
			return is3d != is3d_pre || is2p != is2p_pre ;
		}
	}
	
	
	
	private static class MultiPlayerGameLaunchButtonCellTag {
		int mGameMode ;
		MultiPlayerGameLaunchButtonStrip mMPGLBS ;
		MultiPlayerGameLaunchButtonCollage mMPGLBC ;
		
		public MultiPlayerGameLaunchButtonCellTag( View v ) {
			mGameMode = -1 ;
			mMPGLBS = (MultiPlayerGameLaunchButtonStrip) v.findViewById( R.id.multiplayer_game_mode_list_item_button_strip ) ;
			mMPGLBC = (MultiPlayerGameLaunchButtonCollage) v.findViewById( R.id.multiplayer_game_mode_list_item_button_collage ) ;
		}
	}
	
	private static class MultiPlayerGameLaunchButtonRowTag {
		View mHeaderView ;
		View mHeaderViewTopSpacer ;
		TextView mHeaderTextView ;
		
		ViewGroup mButtonRow ;
		
		public MultiPlayerGameLaunchButtonRowTag( View v ) {
			mHeaderView = v.findViewById(R.id.quantro_list_item_header) ;
			mHeaderViewTopSpacer = v.findViewById(R.id.quantro_list_item_header_top_spacer) ;
			mHeaderTextView = (TextView)v.findViewById(R.id.quantro_list_item_header_text) ;
			mButtonRow = (ViewGroup)v.findViewById(R.id.multiplayer_game_mode_list_row) ;
		}
	}
	
	
	private class MultiPlayerGameLaunchButtonCollageAdapter extends SectionableAdapter
			implements OnScrollListener, PinnedHeaderListView.PinnedHeaderAdapter, SectionIndexer {

		
		
		////////////////////////////////////////////////////////////////////////
		// LIST MANIPULATION
		// Changing the content - game modes - and their results / save data.
		/**
		 * Adds the specified object at the end of the array.
		 */
		synchronized public void add(Integer obj, OptionAvailability availability) {
			
			gameModes.add(obj) ;
			availabilities.put(obj, availability) ;
			
			Integer [] array = new Integer[gameModes.size()] ;
			array = gameModes.toArray(array) ;
			mIndexer.setGameModes(array) ;
		}
		
		synchronized public void clear() {
			
			gameModes.clear() ;
			availabilities.clear() ;
			mIndexer.setGameModes(new Integer[0]) ;
		}
		
		
		synchronized public void insert( Integer obj, OptionAvailability availability, int index ) {
			gameModes.add(index, obj) ;
			availabilities.put(obj, availability) ;
			
			Integer [] array = new Integer[gameModes.size()] ;
			array = gameModes.toArray(array) ;
			mIndexer.setGameModes(array) ;
		}
		
		synchronized public boolean updateAvailability( Integer obj, OptionAvailability availability ) {
			if ( availabilities.containsKey(obj) ) {
				availabilities.put(obj, availability) ;
				return true ;
			}
			return false ;
		}
		
		synchronized public void remove(Integer obj) {
			// find the index
			int index = gameModes.indexOf(obj) ;
			if ( index > -1 ) {
				gameModes.remove(index) ;
				availabilities.remove(obj) ;
				
				Integer [] array = new Integer[gameModes.size()] ;
				array = gameModes.toArray(array) ;
				mIndexer.setGameModes(array) ;
			}
		}
		
		synchronized public OptionAvailability getAvailability( int gameMode ) {
			return availabilities.get(gameMode) ;
		}

		ArrayList<Integer> gameModes = new ArrayList<Integer>() ;
		Hashtable<Integer, OptionAvailability> availabilities = new Hashtable<Integer, OptionAvailability> () ;
		private MultiPlayerGameModeIndexer mIndexer ;

    	boolean mScrolledToTop ;
    	
    	int [] mFirstGameModeVoteForPlayer = null ;
    	int [] mLastGameModeVoteForPlayer = null ;
    	
		public MultiPlayerGameLaunchButtonCollageAdapter(LayoutInflater inflater,
				int rowLayoutID, int headerID, int itemHolderID) {
			
			super(inflater, rowLayoutID, headerID, itemHolderID);
			
			mIndexer = new MultiPlayerGameModeIndexer(new Integer[0]) ;
			mScrolledToTop = true ;
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
		
		int firstVisibleItem_lastCall = -1 ;
		int visibleItemCount_lastCall = -1 ;
		boolean mScrolledToTop_lastCall = false ;
		
		@Override
		synchronized public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			View topChild = view.getChildAt(0) ;
			MultiPlayerGameLaunchButtonRowTag tag = null ;
			if ( topChild != null )
				tag = ((MultiPlayerGameLaunchButtonRowTag)topChild.getTag()) ;
			
			mScrolledToTop = firstVisibleItem == 0 && visibleItemCount > 0 && topChild.getTop() == 0 ;
			boolean topHasHeader = mIndexer.isFirstInSection( getRealPosition(firstVisibleItem) ) ;
			if (view instanceof PinnedHeaderListView) {
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
			
			if ( firstVisibleItem_lastCall != firstVisibleItem
					|| visibleItemCount_lastCall != visibleItemCount
					|| mScrolledToTop_lastCall != mScrolledToTop ) {
				// the first visible item has changed.  Update our above/below
				// votes.
				for ( int player = 0; player < mVoteTabs.getNumberOfTabs(); player++ ) {
					mVoteTabs.setTabOnTop(player, mGameVoteAdapter.getPlayerHasVotesAbove(player, view)) ;
					mVoteTabs.setTabOnBottom(player, mGameVoteAdapter.getPlayerHasVotesBelow(player, view)) ;
				}
				mVoteTabs.set() ;
				
				firstVisibleItem_lastCall = firstVisibleItem ;
				visibleItemCount_lastCall = visibleItemCount ;
				mScrolledToTop_lastCall = mScrolledToTop ;
			}
        }

		@Override
		synchronized public void onScrollStateChanged(AbsListView arg0, int arg1) {
		}
		

		@Override
		synchronized public int getPinnedHeaderState(int position) {
			if (mIndexer == null || getDataCount() == 0) {
                return PINNED_HEADER_STATE_GONE;
            }

            if (position < 0) {
                return PINNED_HEADER_STATE_GONE;
            }
            
            if ( position == 0 && mScrolledToTop )
            	return PINNED_HEADER_STATE_GONE ;

            // The header should get pushed up if the top item shown
            // is the last item in a section for a particular letter.
            int section = getSectionForPosition( position );
            int nextSectionPosition = getPositionForSection( section + 1 ) ;
            
            if (nextSectionPosition != -1 && position == nextSectionPosition - 1) {
                return PINNED_HEADER_STATE_PUSHED_UP;
            }

            return PINNED_HEADER_STATE_VISIBLE;
		}
		
		@Override
        public int pha_getPinnedHeaderFadeAlphaStyle() {
			return PinnedHeaderListView.PinnedHeaderAdapter.PINNED_HEADER_FADE_ALPHA_HEADER_NONCONTENT_OVERLAP ;
			// return PinnedHeaderListView.PHA_DEFAULTS.pha_getPinnedHeaderFadeAlphaStyle() ;
		}
        
		Rect tempPaddingRect = new Rect() ;
        
		@Override
        public boolean copyPinnedHeaderOccludingBounds(View header, int position, Rect r) {
			MultiPlayerGameLaunchButtonRowTag tag = (MultiPlayerGameLaunchButtonRowTag)header.getTag() ;
        	Drawable bg = tag.mHeaderTextView.getBackground() ;
        	bg.getPadding(tempPaddingRect) ;
        	r.set(0, tag.mHeaderTextView.getTop() + tempPaddingRect.top,
        			header.getWidth(), tag.mHeaderTextView.getBottom() - tempPaddingRect.bottom) ;
        	return true ;
        }
        
        @Override
        public boolean copyPinnedHeaderVisibleBounds(View header, int position, Rect r) {
        	MultiPlayerGameLaunchButtonRowTag tag = (MultiPlayerGameLaunchButtonRowTag)header.getTag() ;
        	r.set(0, tag.mHeaderTextView.getTop(),
        			header.getWidth(), tag.mHeaderTextView.getBottom()) ;
        	return true ;
        }
        
        
        @Override
        public boolean copyHeaderOccludingBounds(View view, int position, Rect r) {
        	MultiPlayerGameLaunchButtonRowTag tag = (MultiPlayerGameLaunchButtonRowTag)view.getTag() ;
        	if ( tag.mHeaderView.getVisibility() == View.GONE ) {
        		r.set(0, 0, 0, 0) ;
        		return true ;
        	}
        	
        	return copyPinnedHeaderOccludingBounds( tag.mHeaderView, position, r ) ;
        }
        
        @Override
        public boolean copyHeaderVisibleBounds(View view, int position, Rect r) {
        	MultiPlayerGameLaunchButtonRowTag tag = (MultiPlayerGameLaunchButtonRowTag)view.getTag() ;
        	if ( tag.mHeaderView.getVisibility() == View.GONE ) {
        		r.set(0, 0, 0, 0) ;
        		return true ;
        	}
        	
        	return copyPinnedHeaderVisibleBounds( tag.mHeaderView, position, r ) ;
        }
		
		@Override
		synchronized public void configurePinnedHeader(View v, int position, int alpha) {
			final int section = getSectionForPosition( position );
			final String title = (String) getSections()[section];
			
			MultiPlayerGameLaunchButtonRowTag tag = (MultiPlayerGameLaunchButtonRowTag)v.getTag() ;
			tag.mHeaderTextView.setText(title) ;
			tag.mHeaderViewTopSpacer.setVisibility(View.GONE) ;
			VersionSafe.setAlpha(v, alpha / 255f) ;
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
			return gameModes.get(position) ;
		}


		@Override
		protected int getDataCount() {
			return gameModes.size() ;
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
			return MultiPlayerGameModeIndexer.NUM_SECTIONS ;
		}


		@Override
		protected int getCountInSection(int index) {
			if ( mIndexer == null )
				return 0 ;
			
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
				return -1 ;
			
			return mIndexer.getSectionForPosition(position) ;
		}


		@Override
		protected String getHeaderForSection(int section) {
			return null ;
		}


		@Override
		protected void bindView(View cell, int position) {
			Integer gameModeInteger = gameModes.get(position) ;
			int gameMode = gameModeInteger == null ? -1 : gameModeInteger.intValue() ;
			
    		MultiPlayerGameLaunchButtonCellTag tag = (MultiPlayerGameLaunchButtonCellTag)cell.getTag() ;
    		if ( tag == null ) {
				tag = new MultiPlayerGameLaunchButtonCellTag(cell) ;
				if ( tag.mMPGLBS != null ) {
					tag.mMPGLBS.setDelegate(LobbyViewComponentAdapter.this) ;
					tag.mMPGLBS.setColorScheme(mColorScheme) ;
					tag.mMPGLBS.setLobby(mLobby) ;
				} 
				if ( tag.mMPGLBC != null ) {
					tag.mMPGLBC.setDelegate(LobbyViewComponentAdapter.this) ;
					tag.mMPGLBC.setColorScheme(mColorScheme) ;
					tag.mMPGLBC.setSoundPool(mSoundPool) ;
					tag.mMPGLBC.setLobby( mLobby ) ;
				}
				cell.setTag(tag) ;
			}
    		
    		// Set the game mode and player colors.  This will automatically refresh
			// if things have changed.  TODO: this might be a good place to optimize.
    		tag.mGameMode = gameMode ;
			if ( tag.mMPGLBS != null ) {
	    		tag.mMPGLBS.setGameMode(gameMode, availabilities.get(gameMode)) ;
				for ( int i = 0; i < mLobby.getMaxPeople(); i++ )
					tag.mMPGLBS.setPlayerColor(i, mPlayerColor[i]) ;
			}
			
			if ( tag.mMPGLBC != null ) {
	    		tag.mMPGLBC.setGameMode(gameMode, availabilities.get(gameMode)) ;
				for ( int i = 0; i < mLobby.getMaxPeople(); i++ )
					tag.mMPGLBC.setPlayerColor(i, mPlayerColor[i]) ;
			}
			
			// manual refresh.  Many list view items do not refresh themselves
			// when votes change.  This can be a problem if votes are "offscreen"
			// and then scrolled back.
			if ( tag.mMPGLBS != null ) {
	    		tag.mMPGLBS.refresh() ;
			}
			
			if ( tag.mMPGLBC != null ) {
	    		tag.mMPGLBC.refresh() ;
			}
			
			// Setting height causes a measure.  If this happens before we refresh() we get weird results.
			if ( tag.mMPGLBS != null )
				tag.mMPGLBS.getLayoutParams().height = tag.mMPGLBS.getIdealHeight() ;
		}
		
		
		/**
		 * Perform any row-specific customization your grid requires. For example, you could add a header to the
		 * first row or a footer to the last row.
		 * @param row the 0-based index of the row to customize.
		 * @param convertView the inflated row View.
		 */
		@Override
		protected void customizeRow(int row, int firstPosition, View rowView) {
			// This is where we perform necessary header configuration.
			
			MultiPlayerGameLaunchButtonRowTag tag = ((MultiPlayerGameLaunchButtonRowTag)rowView.getTag()) ;
			if ( tag == null ) {
				tag = new MultiPlayerGameLaunchButtonRowTag( rowView ) ;
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
		
		public void setListItemLobby( View v, Lobby lobby ) {
			if ( v == null )
				return ;
			Object tag = v.getTag() ;
			if ( tag != null && tag instanceof MultiPlayerGameLaunchButtonCellTag ) {
				MultiPlayerGameLaunchButtonCellTag mpglbct = ((MultiPlayerGameLaunchButtonCellTag)tag) ;
				if ( mpglbct.mMPGLBS != null )
					mpglbct.mMPGLBS.setLobby(lobby) ;
				if ( mpglbct.mMPGLBC != null )
					mpglbct.mMPGLBC.setLobby(lobby) ;
			} else if ( tag != null && tag instanceof MultiPlayerGameLaunchButtonRowTag ) {
				MultiPlayerGameLaunchButtonRowTag mpglbrt = ((MultiPlayerGameLaunchButtonRowTag)tag) ;
				if ( mpglbrt.mButtonRow != null ) {
					for ( int i = 0; i < mpglbrt.mButtonRow.getChildCount(); i++ ) {
						setListItemLobby( mpglbrt.mButtonRow.getChildAt(i), lobby ) ;
					}
				}
			}
		}
		
		
		/**
		 * Refreshes the content of the provided view, which should be an
		 * item in a ListView for which this ArrayAdapter serves as an adapter.
		 * @param v
		 */
		public void refreshListItem( View v ) {
			if ( v == null )
				return ;
			// if v is an instance of whatever our list item is, perform the refresh
			// on the MultiPlayerGameLaunchButtonStrip within the layout (it may be
			// the entire layout, but maybe not?)
			Object tag = v.getTag() ;
			if ( tag != null && tag instanceof MultiPlayerGameLaunchButtonCellTag ) {
				MultiPlayerGameLaunchButtonCellTag mpglbct = ((MultiPlayerGameLaunchButtonCellTag)tag) ;
				int gameMode = mpglbct.mGameMode ;
				if ( mpglbct.mMPGLBS != null ) {
					mpglbct.mMPGLBS.setGameMode( gameMode, availabilities.get(gameMode) ) ;
					mpglbct.mMPGLBS.refresh() ;
				}
				if ( mpglbct.mMPGLBC != null ) {
					mpglbct.mMPGLBC.setGameMode( gameMode, availabilities.get(gameMode) ) ;
					mpglbct.mMPGLBC.refresh() ;
				}
			} else if ( tag != null && tag instanceof MultiPlayerGameLaunchButtonRowTag ) {
				MultiPlayerGameLaunchButtonRowTag mpglbrt = ((MultiPlayerGameLaunchButtonRowTag)tag) ;
				if ( mpglbrt.mButtonRow != null ) {
					for ( int i = 0; i < mpglbrt.mButtonRow.getChildCount(); i++ ) {
						refreshListItem( mpglbrt.mButtonRow.getChildAt(i) ) ;
					}
				}
			}
		}
		
		/**
		 * Refreshes the content of the provided view, which should be an
		 * item in a ListView for which this ArrayAdapter serves as an adapter.
		 * @param v The ListItem view
		 * @param gameModes an array of integers specifying the game modes to be refreshed
		 * @param lim if >= 0, the bounds of gameModes to examine.  We proceed as if
		 * 			gameModes.length = Math.min( gameModes.length, lim ).
		 */
		public void refreshListItemIfGameMode( View v, int [] gameModes, int lim ) {
			if ( v == null )
				return ;
			// if v is an instance of whatever our list item is, perform the refresh
			// on the MultiPlayerGameLaunchButtonStrip within the layout (it may be
			// the entire layout, but maybe not?)
			Object tag = v.getTag() ;
			if ( tag != null && tag instanceof MultiPlayerGameLaunchButtonCellTag ) {
				MultiPlayerGameLaunchButtonCellTag mpglbct = ((MultiPlayerGameLaunchButtonCellTag)tag) ;
				int gameMode = mpglbct.mGameMode ;
				if ( gameModes == null ) {
					if ( mpglbct.mMPGLBS != null ) {
						mpglbct.mMPGLBS.setGameMode( gameMode, availabilities.get(gameMode) ) ;
						mpglbct.mMPGLBS.refresh() ;
					}
					if ( mpglbct.mMPGLBC != null ) {
						mpglbct.mMPGLBC.setGameMode( gameMode, availabilities.get(gameMode) ) ;
						mpglbct.mMPGLBC.refresh() ;
					}
				} else {
					int limit = lim >= 0 ? Math.min(lim, gameModes.length) : gameModes.length ;
					if ( mpglbct.mMPGLBS != null ) {
						for ( int j = 0; j < limit; j++ ) {
							if ( gameMode == gameModes[j] ) {
								mpglbct.mMPGLBS.setGameMode( gameMode, availabilities.get(gameMode) ) ;
								mpglbct.mMPGLBS.refresh() ;
							}
						}
					}
					if ( mpglbct.mMPGLBC != null ) {
						for ( int j = 0; j < limit; j++ ) {
							if ( gameMode == gameModes[j] ) {
								mpglbct.mMPGLBC.setGameMode( gameMode, availabilities.get(gameMode) ) ;
								mpglbct.mMPGLBC.refresh() ;
							}
						}
					}
				}
			} else if ( tag != null && tag instanceof MultiPlayerGameLaunchButtonRowTag ) {
				MultiPlayerGameLaunchButtonRowTag mpglbrt = ((MultiPlayerGameLaunchButtonRowTag)tag) ;
				if ( mpglbrt.mButtonRow != null ) {
					for ( int i = 0; i < mpglbrt.mButtonRow.getChildCount(); i++ ) {
						refreshListItemIfGameMode( mpglbrt.mButtonRow.getChildAt(i), gameModes, lim ) ;
					}
				}
			}
		}
		
		public void setPlayerColor( View v, int playerSlot, int playerColor ) {
			if ( v == null )
				return ;
			// if v is an instance of whatever our list item is, perform the refresh
			// on the MultiPlayerGameLaunchButtonStrip within the layout (it may be
			// the entire layout, but maybe not?)
			Object tag = v.getTag() ;
			if ( tag != null && tag instanceof MultiPlayerGameLaunchButtonCellTag ) {
				MultiPlayerGameLaunchButtonCellTag mpglbct = ((MultiPlayerGameLaunchButtonCellTag)tag) ;
				if ( mpglbct.mMPGLBS != null )
					mpglbct.mMPGLBS.setPlayerColor(playerSlot, playerColor) ;
				if ( mpglbct.mMPGLBC != null )
					mpglbct.mMPGLBC.setPlayerColor(playerSlot, playerColor) ;
			} else if ( tag != null && tag instanceof MultiPlayerGameLaunchButtonRowTag ) {
				MultiPlayerGameLaunchButtonRowTag mpglbrt = ((MultiPlayerGameLaunchButtonRowTag)tag) ;
				if ( mpglbrt.mButtonRow != null ) {
					for ( int i = 0; i < mpglbrt.mButtonRow.getChildCount(); i++ ) {
						setPlayerColor( mpglbrt.mButtonRow.getChildAt(i), playerSlot, playerColor ) ;
					}
				}
			}
		}
		
		
		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged() ;
			
			if ( mLobby != null ) {
				// take this opportunity to determine each player's "first vote in order"
				// and "last vote in order."  We use these to determine quickly whether a particular
				// player should have their top and/or bottom tab displayed.
				if ( mFirstGameModeVoteForPlayer == null || mFirstGameModeVoteForPlayer.length != mLobby.getMaxPeople() ) {
					mFirstGameModeVoteForPlayer = new int[mLobby.getMaxPeople()] ;
					mLastGameModeVoteForPlayer = new int[mLobby.getMaxPeople()] ;
				}
				
				for ( int i = 0; i < mLobby.getMaxPeople(); i++ ) {
					mFirstGameModeVoteForPlayer[i] = -1 ;
					mLastGameModeVoteForPlayer[i] = -1 ;
				}
				
				// step through game modes in order - this is the "displayed" order.
				Iterator<Integer> iter = gameModes.iterator() ;
				for ( ; iter.hasNext() ; ) {
					int mode = iter.next().intValue() ;
					for ( int i = 0; i < mLobby.getMaxPeople(); i++ ) {
						if ( mLobby.getPlayerVote(i, mode) ) {
							if ( mFirstGameModeVoteForPlayer[i] == -1 )
								mFirstGameModeVoteForPlayer[i] = mode ;
							mLastGameModeVoteForPlayer[i] = mode ;
						}
					}
				}
			}
		}
		
		/**
		 * Does the player have votes which are not displayed, because they have
		 * been scrolled off the list and are now ABOVE the displayed items?
		 * @return
		 */
		public boolean getPlayerHasVotesAbove( int playerSlot, AbsListView lv ) {
			if ( mFirstGameModeVoteForPlayer == null
					|| playerSlot >= mFirstGameModeVoteForPlayer.length
					|| mFirstGameModeVoteForPlayer[playerSlot] == -1 )
				return false ;
			
			// check the first list item at child 0.
			if ( lv.getChildCount() < 1 )
				return false ;
			MultiPlayerGameLaunchButtonRowTag rowTag = ((MultiPlayerGameLaunchButtonRowTag)lv.getChildAt(0).getTag()) ;
			if ( rowTag.mButtonRow == null || rowTag.mButtonRow.getChildCount() < 1 )
				return false ;
			// find a list item.
			ViewGroup itemHolder = (ViewGroup)rowTag.mButtonRow.findViewById(getItemHolderID()) ;
			if ( itemHolder == null )
				return false ;
			
			MultiPlayerGameLaunchButtonCellTag cellTag = (MultiPlayerGameLaunchButtonCellTag)itemHolder.getChildAt(0).getTag() ;
			if ( cellTag == null )
				return false ;
			int gameMode = cellTag.mGameMode ;
			
			// if the first game mode occurs before this one, then yes.
			return compareGameModes(mFirstGameModeVoteForPlayer[playerSlot], gameMode) < 0 ;
		}
		
		/**
		 * Does the player have notes which are not displayed, because they
		 * have been scrolled off the list and are now BELOW the displayed items?
		 * @return
		 */
		public boolean getPlayerHasVotesBelow( int playerSlot, AbsListView lv ) {
			if ( mLastGameModeVoteForPlayer == null
					|| playerSlot >= mLastGameModeVoteForPlayer.length
					|| mLastGameModeVoteForPlayer[playerSlot] == -1 )
				return false ;
			
			// check the last list item at the last child
			if ( lv.getChildCount() < 1 )
				return false ;
			MultiPlayerGameLaunchButtonRowTag rowTag = ((MultiPlayerGameLaunchButtonRowTag)lv.getChildAt(lv.getChildCount()-1).getTag()) ;
			if ( rowTag.mButtonRow == null || rowTag.mButtonRow.getChildCount() < 1 )
				return false ;
			// find a list item.
			ViewGroup itemHolder = (ViewGroup)rowTag.mButtonRow.findViewById(getItemHolderID()) ;
			if ( itemHolder == null )
				return false ;
			
			// find the last VISIBLE item.
			MultiPlayerGameLaunchButtonCellTag cellTag = null ;
			for ( int i = itemHolder.getChildCount()-1; i >= 0; i-- ) {
				View v = itemHolder.getChildAt(i) ;
				if ( v.getVisibility() == View.VISIBLE ) {
					cellTag = (MultiPlayerGameLaunchButtonCellTag)v.getTag() ;
					break ;
				}
			}
			
			if ( cellTag == null )
				return false ;
			int gameMode = cellTag.mGameMode ;
			
			// if the last game mode occurs after this one, then yes.
			return compareGameModes(mLastGameModeVoteForPlayer[playerSlot], gameMode) > 0 ;
		}
	}
	
	
	
	
	private class LobbyLogEventArrayAdapter extends ArrayAdapter<LobbyLog.Event> {
		
		ListView lv ;
		
		private class InnerViews {
			WebView wv ;
		}

		public LobbyLogEventArrayAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			// TODO Auto-generated constructor stub
		}
		
		synchronized public void setListView( ListView lv ) {
			this.lv = lv ;
		}
		
		@Override
		synchronized public void add(LobbyLog.Event e) {
			if ( lv == null ) {
				super.add(e) ;
				return ;
			}
			// We want to maintain our current position after the add.
			// If at the bottom, stay at the bottom.  If above the bottom,
			// do not scroll (i.e., do nothing.)
			boolean atBottom = false ;
			atBottom = lv.getLastVisiblePosition() == lv.getCount() -1 ;
			
			super.add(e) ;
			
			if ( atBottom && lv.isInTouchMode() )
				lv.setSelection(lv.getCount()-1) ;
		}
		
		@Override
		synchronized public void remove( LobbyLog.Event e ) {
			if ( getCount() < 2 || lv == null ) {
				super.remove(e) ;
				return ;
			}
			// We want to maintain our current position after the remove.
			// Removing an item may have no effect on our scroll position
			// (if the item was below the last displayed), it may move
			// everything up one (if above the first displayed) or it
			// may remove an on-screen item.
			
			// If past the last displayed, do nothing, we are okay already.
			// If above the first displayed, we want the appearance of no
			// change.  Do this by setting the top item as selected after the
			// remove.
			
			// If within our display, we can't hide the change.  Do not scroll.
			
			boolean aboveDisplay = false ;
			int firstPos = lv.getFirstVisiblePosition() ;
			if ( getItem( firstPos ).id > e.id  ) 
				aboveDisplay = true ;
				// exploit the fact that they are arranged in ascending id order.
			
			int num = getCount() ;
			super.remove(e) ;
			boolean didRemove = num != getCount() ;
			
			if ( aboveDisplay && didRemove && lv.isInTouchMode() )
				lv.setSelection( firstPos-1 ) ;
		}
		
		@Override
		synchronized public View getView(int position, View convertView, ViewGroup parent) {
			LobbyLog.Event e = getItem(position) ;
			InnerViews innerViews ;
			
			// At the end of this block, convertView is the list item,
			// and innerViews is the innerViews object associated with it.
			if ( convertView != null ) {
				// we have set an 'InnerViews' object as the tag.
				innerViews = (InnerViews)convertView.getTag() ;
			}
			else {
				convertView = ((Activity)mwrContext.get()).getLayoutInflater().inflate(R.layout.web_view, parent, false);
				// for now, innerViews.wv IS the list item itself!
				innerViews = new InnerViews() ;
				innerViews.wv = (WebView)convertView ;
				
				convertView.setTag(innerViews) ;
				VersionSafe.disableHardwareAcceleration( innerViews.wv ) ;
			}
			
			// Set the content according to the event.  There should be a prefix establishing
			// CSS style; for instance, we indent paragraph text from the 2nd line on.
			// Additionally, we format a color-tag for the player, a bold tag for player
			// performing chat, and a "lobby text" color tag for lobby-relevant events
			// (such as a player entering or leaving a lobby).  Finally, we create this string
			// from scratch if e.tag is an Integer; if a String, it is the previously
			// created string.
			String cssText ;
			if ( e.tag instanceof String ) {
				cssText = (String)e.tag ;
			}
			else {
				Integer color = (Integer)e.tag ;
				// Ignore the alpha-channnel for all colors.  The easiest way to do this
				// is to construct a color with 0 alpha.
				int lobbyColor = mwrContext.get().getResources().getColor( mWifi ? R.color.lobby_wifi : R.color.lobby_internet ) ;
				int playerColor = color.intValue() ;
				int gameModeColor = ( GameModeColors.hasPrimary(e.arg1) && mColorScheme != null ? GameModeColors.primary(mColorScheme, e.arg1) : 0xffffffff ) ;
				// remove the alpha from each.
				lobbyColor = setAlphaToZero(lobbyColor) ;
				playerColor = setAlphaToZero(playerColor) ;
				gameModeColor = setAlphaToZero(gameModeColor) ;
				// Get the color WITHOUT alpha channel.

				StringBuilder sb = new StringBuilder() ;
				// Format tag styles
				sb.append("<head>\n") ;
				sb.append("<style type=\"text/css\">\n") ;
				sb.append("p {text-indent:-20px; padding-left:20px; color:#ffffff}\n") ;
				// Use a 'name' tag if this event has a name (and thus a color).
				if ( color != null )
					sb.append("name {color:#").append( toHexString( playerColor, 6 ) ).append(";}\n") ;
				sb.append("lobby {color:#").append( toHexString( lobbyColor, 6 ) ).append(";}\n") ;
				sb.append("emph {font-weight:bold;}\n") ;
				// if a launch, add a tag for game mode
				if ( e.type == LobbyLog.Event.LAUNCH )
					sb.append("game {color:#").append( toHexString( gameModeColor, 6 ) ).append(";}\n") ;
				
				sb.append("</style>\n") ;
				sb.append("</head>\n") ;
				
				// That's the header, now the body.
				// It doesn't appear that WebView's 'padding' element works.  Therefore, set a margin.
				sb.append("<body bgcolor=\"black\" marginwidth=\"5\" marginheight=\"5\" topmargin=\"5\" leftmargin=\"5\" rightmargin=\"5\" bottommargin=\"5\">\n") ;
				// content is determined by event type.
				switch( e.type ) {
				case LobbyLog.Event.CONNECTED:
					// Put an emphasized lobby message.
					sb.append("<p><lobby><emph>") ;
					sb.append(
							mResources.getString(
									R.string.menu_lobby_log_event_connected).replace(
											mResources.getString(R.string.placeholder_lobby_name),
											TextUtils.htmlEncode(""+mLobby.getLobbyName()) )) ;
					sb.append("</emph></lobby></p>") ;
					break ;
				case LobbyLog.Event.DISCONNECTED:
					// Put an emphasized lobby message.
					sb.append("<p><lobby><emph>") ;
					sb.append(
							mResources.getString(
									R.string.menu_lobby_log_event_disconnected).replace(
											mResources.getString(R.string.placeholder_lobby_name),
											TextUtils.htmlEncode(""+mLobby.getLobbyName()) )) ;
					sb.append("</emph></lobby></p>") ;
					break ;
				case LobbyLog.Event.PLAYER_JOINED:
					// Put a lobby message, color-coding the player name.
					sb.append("<p><lobby>") ;
					sb.append(
							mResources.getString(
									R.string.menu_lobby_log_event_player_joined)
									.replace(
											mResources.getString(R.string.placeholder_lobby_name),
											TextUtils.htmlEncode(""+mLobby.getLobbyName()) )
									.replace(
											mResources.getString(R.string.placeholder_names_array_name_1),
											"<name>" + TextUtils.htmlEncode(""+e.name) + "</name>")
											
							 ) ;
					sb.append("</lobby></p>") ;
					break ;
				case LobbyLog.Event.PLAYER_LEFT:
					// Put a lobby message, color-coding the player name.
					sb.append("<p><lobby>") ;
					sb.append(
							mResources.getString(
									R.string.menu_lobby_log_event_player_left)
									.replace(
											mResources.getString(R.string.placeholder_lobby_name),
											TextUtils.htmlEncode(""+mLobby.getLobbyName()))
									.replace(
											mResources.getString(R.string.placeholder_names_array_name_1),
											"<name>" + TextUtils.htmlEncode(""+e.name) + "</name>")
											
							 ) ;
					sb.append("</lobby></p>") ;
					break ;
				case LobbyLog.Event.PLAYER_KICKED:
					// Put a lobby message, color-coding the player name.  Follow
					// up with the kick message.
					sb.append("<p><lobby>") ;
					sb.append(
							mResources.getString(
									R.string.menu_lobby_log_event_player_kicked)
									.replace(
											mResources.getString(R.string.placeholder_lobby_name),
											TextUtils.htmlEncode(""+mLobby.getLobbyName()) )
									.replace(
											mResources.getString(R.string.placeholder_names_array_name_1),
											"<name>" + TextUtils.htmlEncode(""+e.name) + "</name>")
											
							 ) ;
					// DO include the colon in lobby-formatting.
					sb.append(":</lobby>") ;
					// follow up with message.
					sb.append(" " + TextUtils.htmlEncode(""+e.text)) ;
					sb.append("</p>") ;
					break ;
				case LobbyLog.Event.CHAT:
					// Put up a chat message.  Emph and color-code the speaker name
					// then normal format everything that follows.
					// Put the colon inside the formatting.
					sb.append("<p>") ;
					sb.append("<name><emph>" + TextUtils.htmlEncode(""+e.name) + ":</emph></name>") ;
					// Replace escape characters using TextUtils.htmlEncode.
					sb.append(" " + TextUtils.htmlEncode(""+e.text)) ;
					sb.append("</p>") ;
					break ;
				case LobbyLog.Event.LAUNCH:
					// A little complicated.  This is obviously lobby-specific, so format
					// with lobby stuff, but we need to format the game mode name with 'game'
					// tags.
					sb.append("<p><lobby>") ;
					sb.append(mResources.getString(
							R.string.menu_lobby_log_event_launch).replace(
									mResources.getString(R.string.placeholder_game_mode_short_name),
									"<game>" + TextUtils.htmlEncode( GameModes.shortName(e.arg1) ) + "</game>" )) ;
					sb.append("</lobby></p>") ;
					break ;
				}
				sb.append("</body>") ;
				
				// sb is now the complete string.  Convert to string,
				// and replace the event tag.
				cssText = sb.toString() ;
				e.tag = cssText ;
			}
			
			// Apply the cssText.
			innerViews.wv.loadDataWithBaseURL("same://ur/l/calling/loadData/does/not/refresh/on/second/call", cssText, "text/html", "utf-8", null) ;
			innerViews.wv.setBackgroundColor(0xff000000) ;
			//System.err.println("setting webview " + innerViews.wv + " to event " + e.id) ;
			//System.err.println("sanity check: " + (innerViews.wv == convertView ? "passed" : "---___FAILED____---")) ;
			
			return convertView ;
		}
		
		private int setAlphaToZero( int color ) {
			int ac = Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)) ;
			return ac ;
		}
		
		/**
		 * Converts the provided value to a hex string of exactly the specified length.
		 * If Integer.toHexString would return a shorter string, leading zeroes are
		 * included.  If longer, the result is truncated.
		 * @param value
		 * @param size
		 * @return
		 */
		private String toHexString( int value, int size ) {
			int mask = (int)Math.pow(2, size*4) ;
			return Integer.toHexString( ( value % mask  ) | mask ).substring(1) ;
		}
		
	}
	
	
	
	/**
	 * Compares two game modes using standard comparator conventions.
	 * 
	 * If gameModeA should occur before gameModeB in a list, returns 'r' < 0.
	 * If gameModeA == gameModeB, returns 0.
	 * If gameModeA should occur after gameModeB in a list, returns 'r' > 0 .
	 * 
	 * @param gameModeA
	 * @param gameModeB
	 * @return
	 */
	public int compareGameModes( int gameModeA, int gameModeB ) {
		if ( gameModeA == gameModeB )
			return 0 ;
		
		boolean is3d = GameModes.numberQPanes(gameModeA) == 2 ;
		boolean is2p = GameModes.minPlayers(gameModeA) == 2 ;
		
		boolean b_is3d = GameModes.numberQPanes(gameModeB) == 2 ;
		boolean b_is2p = GameModes.minPlayers(gameModeB) == 2 ;
		
		// Handle three sets of cases.  first, we have reached the
		// end of a section.  We check the next item to get a sense
		// of whether this game mode should appear before the next section.
		// Second, we are in the correct section.  Check whether the game
		// mode number appears before the one here: if so, put it first.
		// Third, we have found the exact game mode.  Quit out with -1.
		
		// FIRST: just reached the next section.
		
		// if game mode is 2-player quantro, put it before the first
		// non "2-p quantro" mode.
		if ( is3d && is2p && !(b_is3d && b_is2p) )
			return -1 ;
		
		// if game mode is 2-player retro, put it before the first 
		// non 2-p game mode.
		if ( !is3d && is2p && !b_is2p )
			return -1;
		
		// if game mode is free4all quantro, put it before the
		// first free4all retro.
		if ( is3d && !is2p && !b_is3d && !b_is2p )
			return -1 ;
		
		// SECOND: within the right section
		if ( is3d == b_is3d && is2p == b_is2p && gameModeA < gameModeB )
			return -1 ;
		
		// otherwise, it should be after.
		return 1 ;
	}
	
	
	private class UpdateBasicsRunnable implements Runnable {
		
		@Override
		public void run() {
			if ( mLobby == null )
				return ;
			
			int role = mHost ? TextFormatting.ROLE_HOST : TextFormatting.ROLE_CLIENT ;
			int display = TextFormatting.DISPLAY_MENU ;
			int type ;
			
			// get rid of ourself if we're posted later.  Each delayed post
			// indicates that an update is necessary; we are performing a
			// full update right now, so we are handling that event already.
			mHandler.removeCallbacks(this) ;
			// immediately copy mLobby into our update cache.  Since lobby could change
			// during this update, we want a "snapshot view."
			mLobbyUpdateSnapshot.takeVals(mLobby) ;
			// since we already removed callbacks, any future changes to the lobby will
			// produce a new run()ing of this updater.
			
			// Refresh all the general information not covered by the other
			// updaters.  
			TextView tv ;
			
			// Update our lobby name.  No configuration or whatever, just
			// set the lobby name.
			tv = (TextView)mComponentView[COMPONENT_LOBBY_NAME] ;
			if ( tv != null )
				tv.setText( mLobbyUpdateSnapshot.getLobbyName() ) ;
			
			// Update our lobby description.
			tv = (TextView)mComponentView[COMPONENT_LOBBY_DESCRIPTION] ;
			if ( tv != null ) {
				type = mWifi ? TextFormatting.TYPE_LOBBY_WIFI_DESCRIPTION : TextFormatting.TYPE_LOBBY_INTERNET_DESCRIPTION ;
				tv.setText( TextFormatting.format(
						mwrContext.get(),
						null,
						display,
						type,
						role,
						-1, mLobbyUpdateSnapshot) ) ;		// no GameMode associated with this description
			}
			
			// Update availability, invitation, and details.
			tv = (TextView)mComponentView[COMPONENT_LOBBY_AVAILABILITY_DESCRIPTION] ;
			if ( tv != null ) {
				type = TextFormatting.TYPE_LOBBY_AVAILABILITY_DESCRIPTION ;
				tv.setText( TextFormatting.format(
						mwrContext.get(),
						null,
						display,
						type,
						role,
						-1, mLobbyUpdateSnapshot) ) ;		// no GameMode associated with this description
			}
			
			tv = (TextView)mComponentView[COMPONENT_LOBBY_INVITATION_DESCRIPTION] ;
			if ( tv != null ) {
				type = mHasAndroidBeam
						? TextFormatting.TYPE_LOBBY_INVITATION_DESCRIPTION_BEAM_ENABLED
						: TextFormatting.TYPE_LOBBY_INVITATION_DESCRIPTION_BEAM_DISABLED ;
				tv.setText( TextFormatting.format(
						mwrContext.get(),
						null,
						display,
						type,
						role,
						-1, mLobbyUpdateSnapshot) ) ;		// no GameMode associated with this description
			}
			
			tv = (TextView)mComponentView[COMPONENT_LOBBY_DETAILS] ;
			if ( tv != null ) {
				type = TextFormatting.TYPE_LOBBY_DETAILS ;
				tv.setText( TextFormatting.format(
						mwrContext.get(),
						null,
						display,
						type,
						role,
						-1, mLobbyUpdateSnapshot) ) ;		// no GameMode associated with this description
			}
			
			
			// Update the game modes available in this lobby.  We try to minimize 
			// the disruption of the game mode list, so don't just remove and replace;
			// instead, iterate through both lists, removing those game modes
			// which don't exist and adding those which do.
			
			// TODO: Worry about minimizing disruption.  For now, if the 
			// content of the ArrayAdapter is not EXACTLY that of the current
			// game modes, clear it and re-do.  Otherwise, no change.
			int [] gameModes = mLobbyUpdateSnapshot.getGameModes() ;
			boolean matches = true ;
			if ( mGameVoteAdapter.getDataCount() != gameModes.length )
				matches = false ;
			for ( int i = 0; matches && i < gameModes.length; i++ ) {
				OptionAvailability available = mGameModeAvailability.containsKey(gameModes[i])
						? mGameModeAvailability.get(gameModes[i])
						: OptionAvailability.ENABLED ;
				if ( findGameModeInsertionPosition( gameModes[i] ) != -1 )
					matches = false ; 	// a game mode not in our list.
				else if ( mGameVoteAdapter.getAvailability( gameModes[i] ) != available )
					matches = false ;
			}
			
			if ( !matches ) {
				mGameVoteAdapter.clear() ;
				for ( int i = 0; i < gameModes.length; i++ ) {
					int insertionPos = findGameModeInsertionPosition( gameModes[i] ) ;
					Integer gmInteger = GameModes.gameModeIntegerObject(gameModes[i]) ;
					OptionAvailability availability = mGameModeAvailability.containsKey(gmInteger)
							? mGameModeAvailability.get(gmInteger)
							: OptionAvailability.ENABLED ;
					if ( insertionPos > -1 )
						mGameVoteAdapter.insert(gmInteger, availability, insertionPos) ;
				}
				mGameVoteAdapter.notifyDataSetChanged() ;
			}
			
			// There's no need to set any fully generic text, such as
			// instructions; we expect that they will be set in the layout,
			// some outside element (such as the Activity) or earlier in
			// the LobbyViewComponentAdapter's operation.  We never need
			// to "update" them so they certainly shouldn't be altered here.
		}
		
		/**
		 * This is basically our insertion-sort operation.
		 * Returns the position in mGameVoteArrayAdapter where the
		 * game mode should be inserted.
		 * 
		 * Returns -1 if the provided game mode is already present.
		 * 
		 * @param gameMode
		 * @return
		 */
		private int findGameModeInsertionPosition( int gameMode ) {
			
			for ( int i = 0; i < mGameVoteAdapter.getDataCount(); i++ ) {
				int compare = compareGameModes(gameMode, (Integer)mGameVoteAdapter.getItem(i)) ;
				if ( compare < 0 )
					return i ;
				else if ( compare == 0 )
					return -1 ;
			}
			
			return mGameVoteAdapter.getDataCount() ;
		}
	}
	
	
	private class UpdateVotesRunnable implements Runnable {

		@Override
		public void run() {
			if ( mLobby == null )
				return ;
			
			if ( mLobbyTitleBarButtonStrip != null )
				mLobbyTitleBarButtonStrip.refresh() ;
			
			// get rid of ourself if we're posted later.  Each delayed post
			// indicates that an update is necessary; we are performing a
			// full update right now, so we are handling that event already.
			mHandler.removeCallbacks(this) ;
			// Snapshot!
			mLobbyUpdateSnapshot.takeVals(mLobby) ;
			
			// Refresh each list item in our votes list.
			// We display votes with a "game mode" list, which we hopefully have a
			// reference to.  We don't want to make assumptions about the layout
			// of each individual list item - that's the ArrayAdapter's job - so
			// we let the ArrayAdapter perform the refresh.
			
			ListView lv = (ListView)mComponentView[COMPONENT_GAME_MODE_LIST] ;
			if ( lv != null ) {
				// send each child to the adapter to refresh.
				for ( int i = 0; i < lv.getChildCount(); i++ ) {
					mGameVoteAdapter.setListItemLobby(lv.getChildAt(i), mLobbyUpdateSnapshot) ;
					mGameVoteAdapter.refreshListItem(lv.getChildAt(i)) ;
					for ( int slot = 0; slot < mLobbyUpdateSnapshot.getMaxPeople(); slot++ )
						mGameVoteAdapter.setPlayerColor(lv.getChildAt(i), slot, mPlayerColor[slot]) ;
				}
				// These lines were added after votes failed to update
				// (visually) upon another player entering the lobby.
				mGameVoteAdapter.notifyDataSetChanged() ;
				
				// update tab visibility based on which players have
				// votes above / below the displayed items.
				for ( int player = 0; player < mVoteTabs.getNumberOfTabs(); player++ ) {
					mVoteTabs.setTabOnTop(player, mGameVoteAdapter.getPlayerHasVotesAbove(player, lv)) ;
					mVoteTabs.setTabOnBottom(player, mGameVoteAdapter.getPlayerHasVotesBelow(player, lv)) ;
				}
				mVoteTabs.set() ;
			}
			
			// done!
		}
		
	}
	
	
	private class UpdateLaunchesRunnable implements Runnable {
	
		int [] countdownNumber = null ;
		int [] gameMode = null ;
		long [] delay = null ;
		boolean [][] participant = null ;
		int [] status = null ;
		Object [] tags = null ;
		
		@Override
		public void run() {
			if ( mLobby == null )
				return ;
			
			if ( mLobbyTitleBarButtonStrip != null )
				mLobbyTitleBarButtonStrip.refresh() ;
			
			// get rid of ourself if we're posted later.  Each delayed post
			// indicates that an update is necessary; we are performing a
			// full update right now, so we are handling that event already.
			mHandler.removeCallbacks(this) ;
			// Snapshot!
			mLobbyUpdateSnapshot.takeVals(mLobby) ;
			
			allocate() ;
			
			// We update any game vote button currently involved in a launch
			// (note that if a vote has changed, for example from launching
			// to having insufficient votes to launch, this update will
			// not change the display - HOWEVER, such an event will also
			// prompt an update to game votes, and THAT call will refresh
			// the button.
			int num = mLobbyUpdateSnapshot.getCountdowns(countdownNumber, gameMode, delay, participant, status, tags) ;
			// there are 'num' countdowns currently in place.
			
			// Step 1: perform a generic refresh on all displayed game vote
			// buttons IF those buttons have gameMode involved in a launch.
			ListView lv = (ListView)mComponentView[COMPONENT_GAME_MODE_LIST] ;
			if ( lv != null ) {
				// send each child to the adapter to refresh.
				for ( int i = 0; i < lv.getChildCount(); i++ ) {
					mGameVoteAdapter.refreshListItemIfGameMode(lv.getChildAt(i), gameMode, num) ;
				}
			}
			
			// Step 2: perform a refresh of the global launch status dialog.
			TextView tv = (TextView)mComponentView[COMPONENT_LOBBY_LAUNCH_STATUS] ;
			if ( tv != null ) {
				// find the most pressing countdown, i.e., an active one.  If none,
				// a halted one.  Otherwise, none.
				boolean found = false ;
				int countdownNum = -1 ;
				int countdownGameMode = -1 ;
				for ( int i = 0; i < num; i++ ) {
					if ( status[i] == Lobby.COUNTDOWN_STATUS_ACTIVE ) {
						countdownNum = countdownNumber[i] ;
						countdownGameMode = gameMode[i] ;
						found = true ;
					}
				}
				for ( int i = 0; i < num && !found; i++ ) {
					if ( status[i] == Lobby.COUNTDOWN_STATUS_HALTED ) {
						countdownNum = countdownNumber[i] ;
						countdownGameMode = gameMode[i] ;
						found = true ;
					}
				}
				tv.setText( TextFormatting.format(
						mwrContext.get(),
						null,
						TextFormatting.DISPLAY_MENU,
						TextFormatting.TYPE_LOBBY_LAUNCH_STATUS,
						mHost ? TextFormatting.ROLE_HOST : TextFormatting.ROLE_CLIENT,
						countdownGameMode,
						countdownNum,
						mLobby) ) ;
			}
			
			// repost if countdowns exist.
			if ( num > 0 )
				update_launches(100) ;
		}
		
		public void allocate() {
			int num = mLobby.getMaxPeople() ;
			if ( countdownNumber == null || countdownNumber.length != num ) {
				// allocate these arrays
				countdownNumber = new int[num] ;
				gameMode = new int[num] ;
				delay = new long[num] ;
				participant = new boolean[num][num] ;
				status = new int[num] ;
				tags = new Object[num] ;
			}
		}
		
	}
	
	
	private class UpdatePlayersRunnable implements Runnable {
		
		String [] mNamesPresent = null ;
		int [] mStatusPresent = null ;
		int [] mColorPresent = null ;
		
		SpannableStringBuilder ssb = null ;
		SpannableStringBuilder [] ssb_pair = null ;
		
		@Override
		public void run() {
			if ( mLobby == null )
				return ;
			
			// get rid of ourself if we're posted later.  Each delayed post
			// indicates that an update is necessary; we are performing a
			// full update right now, so we are handling that event already.
			mHandler.removeCallbacks(this) ;
			// Snapshot!
			mLobbyUpdateSnapshot.takeVals(mLobby) ;
			
			// Update the lobby population (x/X).
			TextView tv = (TextView)mComponentView[COMPONENT_LOBBY_POPULATION] ;
			if ( tv != null )
				tv.setText(Integer.toString(mLobbyUpdateSnapshot.getNumPeople()) + "/" + Integer.toString(mLobbyUpdateSnapshot.getMaxPeople())) ;
			
			// Update the current lobby membership, including 
			// names and "active" vs. "inactive."
			// We don't bother using TextFormatting for this - applying color
			// codes to certain sections is a little beyond TextFormatting's
			// expertise.  We do this by building a SpannableString, and
			// assigning color information as spans.
			String [] names = mLobbyUpdateSnapshot.getPlayerNames() ;
			int [] statuses = mLobbyUpdateSnapshot.getPlayerStatuses() ;
			if ( mNamesPresent == null || mNamesPresent.length != names.length ) {
				mNamesPresent = new String[names.length] ;
				mStatusPresent = new int[names.length] ;
				mColorPresent = new int[names.length] ;
			}
			
			if ( ssb == null ) {
				ssb = new SpannableStringBuilder() ;
				ssb_pair = new SpannableStringBuilder[4] ;
				for ( int i = 0; i < 4; i++ )
					ssb_pair[i] = new SpannableStringBuilder() ;
			} else {
				ssb.clear() ;
				for ( int i = 0; i < 4; i++ )
					ssb_pair[i].clear() ;
			}

			// compress these so there aren't any gaps.
			int numNames = 0 ;
			for ( int i = 0; i < statuses.length; i++ ) {
				// Log.d(TAG, "setting player name " + i + " to " + names[i] + " status " + statuses[i]) ;
				if ( statuses[i] == Lobby.PLAYER_STATUS_ACTIVE
						|| statuses[i] == Lobby.PLAYER_STATUS_INACTIVE
						|| statuses[i] == Lobby.PLAYER_STATUS_IN_GAME ) {
					mNamesPresent[numNames] = names[i] ;
					mStatusPresent[numNames] = statuses[i] ;
					mColorPresent[numNames] = mPlayerColor[i] ;
					numNames++ ;
				}
			}
			
			// names and status determine the text of the strings; color
			// color (stored in mPlayerColor) determine color of the spans.
			// TODO: Maybe we should get the current 'ssb' displayed, rather
			// than construct a new one?
			for ( int i = 0; i < numNames; i++ ) {
				appendName( ssb, mStatusPresent[i], mNamesPresent[i], mColorPresent[i] ) ;
				appendName( ssb_pair[i / 2], mStatusPresent[i], mNamesPresent[i], mColorPresent[i] ) ;
				// newlines?
				if ( i + 1 < numNames )
					ssb.append("\n") ;
				if ( i % 2 == 0 )
					ssb_pair[i / 2].append("\n") ;
			}
			int room = mLobbyUpdateSnapshot.getMaxPeople() - numNames ;
			if ( room > 0 ) {
				String name = "" + numNames + "/" + mLobbyUpdateSnapshot.getMaxPeople() ;
				appendName( ssb, Lobby.PLAYER_STATUS_NOT_CONNECTED, name, 0xff888888 ) ;
				if ( numNames % 2 == 0 )
					ssb_pair[numNames/2].append("\n") ;
				appendName( ssb_pair[numNames/2], Lobby.PLAYER_STATUS_NOT_CONNECTED, name, 0xff888888 ) ;
			}
			
			// TODO: For kick / ban controls, it is useful to attach onClick to each
			// name as a spannable.
			// now set for the player population view
			tv = (TextView)mComponentView[COMPONENT_LOBBY_MEMBERSHIP] ;
			if ( tv != null )
				tv.setText(ssb) ;
			for ( int i = 0; i < 4; i++ ) {
				tv = (TextView)mComponentView[COMPONENT_LOBBY_MEMBERSHIP_PAIR_1 + i] ;
				if ( tv != null )
					tv.setText(ssb_pair[i]) ;
			}
			
			
			// Why not play some sounds?
			if ( mSoundPool != null ) {
				if ( statuses != null && mCachedPlayerStatuses != null && statuses.length == mCachedPlayerStatuses.length ) {
					for ( int i = 0; i < statuses.length; i++ ) {
						// join: was not previously connected, now is.
						if ( ( mCachedPlayerStatuses[i] == Lobby.PLAYER_STATUS_NOT_CONNECTED
										|| mCachedPlayerStatuses[i] == Lobby.PLAYER_STATUS_UNUSED )
								&& ( statuses[i] == Lobby.PLAYER_STATUS_ACTIVE
										|| statuses[i] == Lobby.PLAYER_STATUS_INACTIVE
										|| statuses[i] == Lobby.PLAYER_STATUS_IN_GAME ) ) {
							if ( i == mLobbyUpdateSnapshot.getLocalPlayerSlot() )
								mSoundPool.lobbyUserJoin() ;
							else
								mSoundPool.lobbyOtherJoin() ;
						}
						// quit: was previously connected, now is not.
						else if ( ( mCachedPlayerStatuses[i] == Lobby.PLAYER_STATUS_ACTIVE
										|| mCachedPlayerStatuses[i] == Lobby.PLAYER_STATUS_INACTIVE
										|| mCachedPlayerStatuses[i] == Lobby.PLAYER_STATUS_IN_GAME )
								&& ( statuses[i] == Lobby.PLAYER_STATUS_NOT_CONNECTED
										|| statuses[i] == Lobby.PLAYER_STATUS_UNUSED ) ) {
							if ( i == mLobbyUpdateSnapshot.getLocalPlayerSlot() )
								mSoundPool.lobbyUserQuit() ;
							else
								mSoundPool.lobbyOtherQuit() ;
						}
						// go active: was inactive, now active.
						else if ( ( mCachedPlayerStatuses[i] == Lobby.PLAYER_STATUS_INACTIVE
										|| mCachedPlayerStatuses[i] == Lobby.PLAYER_STATUS_IN_GAME )
								&& statuses[i] == Lobby.PLAYER_STATUS_ACTIVE ) {
							if ( i == mLobbyUpdateSnapshot.getLocalPlayerSlot() )
								mSoundPool.lobbyUserGoActive() ;
							else
								mSoundPool.lobbyOtherGoActive() ;
						}
						// go inactive: was active, now inactive.
						else if ( mCachedPlayerStatuses[i] == Lobby.PLAYER_STATUS_ACTIVE
								&& ( statuses[i] == Lobby.PLAYER_STATUS_INACTIVE
										|| statuses[i] == Lobby.PLAYER_STATUS_IN_GAME ) ) {
							if ( i == mLobbyUpdateSnapshot.getLocalPlayerSlot() )
								mSoundPool.lobbyUserGoInactive() ;
							else
								mSoundPool.lobbyOtherGoInactive() ;
						}
					}
				}
			}
			
			// refresh our cache so we don't echo sound effects.
			if ( mCachedPlayerStatuses == null || mCachedPlayerStatuses.length != mLobbyUpdateSnapshot.getMaxPeople()) {
				mCachedPlayerStatuses = new int[mLobbyUpdateSnapshot.getMaxPeople()] ;
			} for ( int i = 0; i < mCachedPlayerStatuses.length; i++ ) {
				mCachedPlayerStatuses[i] = mLobbyUpdateSnapshot.getPlayerStatus(i) ;
			}
			
			// That's it.
		}
		
		
		private void appendName( SpannableStringBuilder ssb, int status, String name, int color ) {
			int len = ssb.length() ;
			switch( status ) {
			case Lobby.PLAYER_STATUS_NOT_CONNECTED:
				// Presumably you know what you're doing?
				ssb.append(Html.fromHtml("&#9642;")).append(""+name) ;		// small filled box
				ssb.setSpan( new ForegroundColorSpan(color), len, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) ;
				break ;
			case Lobby.PLAYER_STATUS_ACTIVE:
				// We don't append 'names[i]'; instead, we append ""+names[i],
				// to convert a null pointer into "null".  This is a stopgap
				// for the very initial display, when a user joins (is active or inactive)
				// but we have not set their name yet.
				ssb.append(Html.fromHtml("&#9632;")).append(" "+name) ;		// a filled box
				ssb.setSpan( new ForegroundColorSpan(color), len, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) ;
				break ;
			case Lobby.PLAYER_STATUS_INACTIVE:
				ssb.append(Html.fromHtml("&#9633;")).append(" "+name) ;		// an empty box
				ssb.setSpan( new ForegroundColorSpan(color), len, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) ;
				break ;
			case Lobby.PLAYER_STATUS_IN_GAME:
				ssb.append(Html.fromHtml("&#9654;")).append(" "+name) ;		// a right-facing triangle
				ssb.setSpan( new ForegroundColorSpan(color), len, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) ;
				break ;
			}
		}
		
	}
	
	
	private class UpdateLogRunnable implements Runnable {
		
		private static final int MAX_LOG_EVENTS = 100 ;
		private int lastEventId = -1 ;
		
		@Override
		public void run() {
			if ( mLobbyLog == null )
				return ;
			
			// get rid of ourself if we're posted later.  Each delayed post
			// indicates that an update is necessary; we are performing a
			// full update right now, so we are handling that event already.
			mHandler.removeCallbacks(this) ;
			
			// Extract a slice of events from the log to get the most recent
			// set.  Attach the player color (if relevant) as a tag to each event,
			// then add/remove from the log event array adapter.  Keep it at a 
			// maximum size; if at that max size, make changes by removing the last,
			// and adding the next, iteratively.
			
			// Use a NEW array list.  This guarantees new Event objects, so
			// overwriting the contents of these won't interfere with the display
			// of earlier list items.
			ArrayList<LobbyLog.Event> events = new ArrayList<LobbyLog.Event>() ;
			int num = mLobbyLog.getEventSliceFrom(events, lastEventId+1) ;
			
			if ( num > 0 ) {
				for ( int i = 0; i < num; i++ ) {
					LobbyLog.Event e = events.get(i) ;
					// Attach the associated player color as an Integer tag.
					if ( mPlayerColorInteger != null && e.slot >= 0 && e.slot < mPlayerColorInteger.length )
						e.tag = mPlayerColorInteger[e.slot] ;
					
					// We include all events EXCEPT a status change.
					if ( e.type != LobbyLog.Event.PLAYER_STATUS_CHANGE ) {
						// Play a sound if this is a chat message.
						if ( e.type == LobbyLog.Event.CHAT && mSoundPool != null && e.age() < MAX_DELAY_LOBBY_LOG_SOUND ) {
							if ( e.slot == mLobby.getLocalPlayerSlot() )
								mSoundPool.lobbyUserChat() ;
							else
								mSoundPool.lobbyOtherChat() ;
						}
						if ( mLobbyLogEventArrayAdapter.getCount() >= MAX_LOG_EVENTS ) 
							mLobbyLogEventArrayAdapter.remove( mLobbyLogEventArrayAdapter.getItem(0) ) ;
						mLobbyLogEventArrayAdapter.add( e ) ;
						// This procedure by default keeps the current scroll position
						// relative to the top.  Here's the behavior we want, in four cases:
						
						// At the bottom and added: stay at the bottom (scroll to bottom)
						// At the bottom and replaced: stay at the bottom (scroll to bottom)
						// Above the bottom and added: maintain current scroll (no change)
						// Above the bottom and replaced: keep current selection on-screen (scroll up?)
						
						// We handle these things in the 'add' and 'remove' methosd of
						// the Adapter.
						
					}
				}
				
				// update lastEventId.
				lastEventId = events.get(num-1).id ;
			}
		}
		
	}

}
