package com.peaceray.quantro.view.lobby;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import com.peaceray.quantro.R;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.lobby.WiFiLobbyDetails;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.utils.WifiMonitor;
import com.peaceray.quantro.view.button.collage.LobbyButtonCollage;
import com.peaceray.quantro.view.button.strip.CustomButtonStrip;
import com.peaceray.quantro.view.button.strip.LobbyButtonStrip;
import com.peaceray.quantro.view.generic.PinnedHeaderListView;
import com.velosmobile.utils.SectionableAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

public class WiFiLobbyManagerStripView extends RelativeLayout implements
		WifiLobbyManagerView, LobbyButtonStrip.Delegate, WifiMonitor.Listener, CustomButtonStrip.Delegate, com.peaceray.quantro.view.button.collage.LobbyButtonCollage.Delegate {
	
	
	private static final String TAG = "WFLMStripView" ;
	
	
	
	// Inflate from XML
	public WiFiLobbyManagerStripView( Context context, AttributeSet attrs ) {
		super(context, attrs) ;
		
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public WiFiLobbyManagerStripView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	private void constructor_setDefaultValues(Context context) {
		mActionStripID = 0 ;
		mCreateLobbyStripID = 0 ;
		mLobbyListID = 0 ;
		mInstructionsTextViewID = 0 ;
		
		// Our delegate!
		mwrDelegate = new WeakReference<Delegate>(null) ;
		// Have we started?
		mStarted = false ;
		
		ACTION_NAME_NEW = context.getResources().getString(R.string.action_strip_name_new) ;
		ACTION_NAME_FIND = context.getResources().getString(R.string.action_strip_name_find) ;
		ACTION_NAME_UNHIDE = context.getResources().getString(R.string.action_strip_name_unhide) ;
		ACTION_NAME_HELP = context.getResources().getString(R.string.action_strip_name_help) ;
		ACTION_NAME_SETTINGS = context.getResources().getString(R.string.action_strip_name_settings) ;
	}
	
	private void constructor_readAttributeSet(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.WifiLobbyManagerStripView);
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			case R.styleable.WifiLobbyManagerStripView_wlmsv_action_strip:
				mActionStripID = a.getResourceId(attr, mActionStripID) ;
				break ;
			case R.styleable.WifiLobbyManagerStripView_wlmsv_create_lobby_strip:
				mCreateLobbyStripID = a.getResourceId(attr, mCreateLobbyStripID) ;
				break ;
			case R.styleable.WifiLobbyManagerStripView_wlmsv_lobby_list:
				mLobbyListID = a.getResourceId(attr, mLobbyListID) ;
				break ;
			case R.styleable.WifiLobbyManagerStripView_wlmsv_instructions_text_view:
				mInstructionsTextViewID = a.getResourceId(attr, mInstructionsTextViewID) ;
				break ;
			}
		}
		
		// recycle; we done.
		a.recycle() ;
	}
	
	private void constructor_allocateAndInitialize(Context context) {
		// First: verify that we have the necessary ID fields.
		if ( mActionStripID == 0 && mCreateLobbyStripID == 0 )
			throw new RuntimeException("Must specify an a Strip ID for creating lobbies or an Action Strip ID.") ;
		else if ( mLobbyListID == 0 ) 
			throw new RuntimeException("Must specify a LobbyList ID.") ;
		
		// Okay.  Allocate our ArrayLists and ArrayAdapters.
		mWiFiLobbyDetailsArrayList = new ArrayList<WiFiLobbyDetails>() ;
		mWiFiLobbyDetailsArrayAdapter = new WiFiLobbyDetailsArrayAdapter(
				((Activity)context).getLayoutInflater(),
				R.layout.lobby_list_item,
				0,
				R.id.lobby_list_row) ;

		// NOTE: the ListViews may not have been inflated yet, so only
		// link them with these adapters in 'start'.
		
		mStarted = false ;
		mHandler = new Handler() ;
		mSearchFinishedRunnable = new Runnable() {
			@Override
			public void run() {
				mSearching = false ;
				refreshText() ;
			}
		} ;
		mWifiMonitor = new WifiMonitor(context, this) ;
		
		mMarkAsSearchingRunnable = new Runnable() {
			@Override
			public void run() {
				if ( mStarted ) {
					// update the "searching" status for all
					boolean change = false ;
					if ( mWiFiLobbyDetailsArrayAdapter != null )
						change = mWiFiLobbyDetailsArrayAdapter.updateAllLobbyViewsSearching() ;
					if ( change )
						refreshText() ;
					// repost
					mHandler.postDelayed(this, LOBBY_MARK_AS_SEARCHING_REFRESH_FREQUENCY) ;
				} else {
					mHandler.removeCallbacks(this) ;
				}
			}
		} ;
	}
	
	private String ACTION_NAME_NEW ;
	private String ACTION_NAME_FIND ;
	private String ACTION_NAME_UNHIDE ;
	private String ACTION_NAME_HELP ;
	private String ACTION_NAME_SETTINGS ;
	
	
	// View IDs.  Help us refresh, add challenges, and the like.
	private int mActionStripID ;
	private int mCreateLobbyStripID ;
	private int mLobbyListID ;
	private int mInstructionsTextViewID ;
	
	// Our action strip
	private CustomButtonStrip mActionStrip ;
	
	// Our text view, for informative text!
	private TextView mInstructionsTextView ;
	private String mInstructionsStringLobbiesNoWifi ;
	private String mInstructionsStringLobbiesSearching ;
	private String mInstructionsStringLobbiesNone ;
	private String mInstructionsStringLobbiesFull ;
	private String mInstructionsStringLobbiesOpen ;

	private WiFiLobbyDetailsArrayAdapter mWiFiLobbyDetailsArrayAdapter ;
	
	// Here's a copy of the list provided by the Activity.
	private ArrayList<WiFiLobbyDetails> mWiFiLobbyDetailsArrayList ;
	// Any hidden lobbies apart from this?
	private boolean mHiddenLobbies = false ;
	
	// Our delegate!
	private WeakReference<Delegate> mwrDelegate ;
	// Sound pool and controls
	private QuantroSoundPool mSoundPool ;
	private boolean mSoundControls ;
	
	// Have we started?
	private boolean mStarted ;
	
	// We set a handler going that updates the text view after a certain period.
	// Until that point, we display a "looking for lobbies" message.  Here's
	// how we keep track.
	private Handler mHandler ;
	private Runnable mSearchFinishedRunnable ;
	private WifiMonitor mWifiMonitor ;
	private boolean mSearching ;
	private boolean mOnWifi ;
	private static final int WIFI_MONITOR_REFRESH_FREQUENCY = 1000 ;		// one second
	private static final long TIME_SEARCHING = 5000 ;		// time searching before we
															// allow the message to change.
	
	private Runnable mMarkAsSearchingRunnable ;
	private long mMarkAsSearchingInterval = 15000 ;			// 15 seconds after an update, we transition to "searching" status.
	private static final int LOBBY_MARK_AS_SEARCHING_REFRESH_FREQUENCY = 3000 ;		// three seconds
	
	

	@Override
	synchronized public void start() {
		mStarted = true ;
		
		Context context = getContext() ;
		
		// Link the ArrayAdapter to the ListView.
		ListView lv ;
		lv = (ListView)findViewById(mLobbyListID) ;
		lv.setAdapter(mWiFiLobbyDetailsArrayAdapter) ;
		lv.setOnScrollListener((OnScrollListener)mWiFiLobbyDetailsArrayAdapter) ;
		lv.setItemsCanFocus(true) ;
		if ( lv instanceof PinnedHeaderListView ) {
			View pinnedHeaderView = ((Activity)context).getLayoutInflater().inflate(R.layout.quantro_list_item_header, lv, false) ;
			pinnedHeaderView.setTag( new LobbyListRowTag( pinnedHeaderView ) ) ;
			((PinnedHeaderListView)lv).setPinnedHeaderView( pinnedHeaderView );
			lv.setDividerHeight(0);
		}
		Iterator<WiFiLobbyDetails> iter = mWiFiLobbyDetailsArrayList.iterator() ;
		mWiFiLobbyDetailsArrayAdapter.clear() ;
		for ( ; iter.hasNext() ; )
			mWiFiLobbyDetailsArrayAdapter.add(iter.next()) ;
		mWiFiLobbyDetailsArrayAdapter.notifyDataSetChanged() ;
		
		// Make ourself the Listener for the create lobby strip, set its
		// description to 'null', and refresh it.
		LobbyButtonStrip lbs = (LobbyButtonStrip)findViewById(mCreateLobbyStripID) ;
			if ( lbs != null ) {
			lbs.setDelegate(this) ;
			lbs.setWiFiLobbyDetails(null, false) ;
			lbs.refresh() ;
		}
		
		// Make ourself the Listener for the action strip and refresh it (everything
		// should already be set).
		mActionStrip = (CustomButtonStrip)findViewById(mActionStripID) ;
		if ( mActionStrip != null ) {
			mActionStrip.setDelegate(this) ;
			mActionStrip.setEnabled( mActionStrip.getButton(ACTION_NAME_NEW), true ) ;
			mActionStrip.setEnabled( mActionStrip.getButton(ACTION_NAME_FIND), true ) ;
			mActionStrip.setEnabled( mActionStrip.getButton(ACTION_NAME_UNHIDE), mHiddenLobbies ) ;
			mActionStrip.setEnabled( mActionStrip.getButton(ACTION_NAME_HELP), true ) ;
			mActionStrip.setEnabled( mActionStrip.getButton(ACTION_NAME_SETTINGS), true ) ;
			
			mActionStrip.refresh() ;
			// Set the frame view's drawable background.
		}
		
		
		
		// Start the handler and other metadata going so we change the
		// instructions upon finding lobbies.
		mHandler = new Handler() ;
		mSearching = true ;
		mOnWifi = false ;
		mHandler.post(mWifiMonitor) ;		// our wifi monitor will post 'mSearchFinishedRunnable' when WiFi
											// turns 'on', and continually repost itself.
		mHandler.postDelayed(mSearchFinishedRunnable, TIME_SEARCHING) ;
		mHandler.postDelayed(mMarkAsSearchingRunnable, LOBBY_MARK_AS_SEARCHING_REFRESH_FREQUENCY) ;
		mHandler.postDelayed( new Runnable() {
			@Override
			public void run() {
				refreshText() ;
			}
		}, 100) ;
		
		refreshText() ;
		
		
		this.postInvalidate() ;
	}

	@Override
	synchronized public void stop() {
		mStarted = false ;
		
		mHandler.removeCallbacks(mSearchFinishedRunnable) ;
		mHandler.removeCallbacks(mMarkAsSearchingRunnable) ;
		mHandler.removeCallbacks(mWifiMonitor) ;
	}

	@Override
	synchronized public void setDelegate(Delegate delegate) {
		mwrDelegate = new WeakReference<Delegate>(delegate) ;
	}
	
	/**
	 * Sets a sound pool for button presses.
	 * @param soundPool
	 */
	synchronized public void setSoundPool( QuantroSoundPool soundPool ) {
		mSoundPool = soundPool ;
	}
	
	/**
	 * Sets whether we play sounds for button presses.
	 * @param soundControls
	 */
	synchronized public void setSoundControls( boolean soundControls ) {
		mSoundControls = soundControls ;
	}
	
	synchronized public void setMarkAsSearchingInterval( long interval ) {
		this.mMarkAsSearchingInterval = interval ;
	}

	@Override
	synchronized public void setLobbies(ArrayList<WiFiLobbyDetails> lobbies) {
		mWiFiLobbyDetailsArrayList.clear();
		Iterator<WiFiLobbyDetails> iter = lobbies.iterator() ;
		for ( ; iter.hasNext() ; ) {
			WiFiLobbyDetails ld = iter.next() ;
			int index = insertionSortIndex( mWiFiLobbyDetailsArrayList, ld ) ;
			mWiFiLobbyDetailsArrayList.add(index, ld.clone()) ;
		}
		
		if ( mStarted ) {
			mWiFiLobbyDetailsArrayAdapter.clear();
			iter = mWiFiLobbyDetailsArrayList.iterator() ;
			for ( ; iter.hasNext() ; )
				mWiFiLobbyDetailsArrayAdapter.add(iter.next()) ;
			mWiFiLobbyDetailsArrayAdapter.notifyDataSetChanged() ;
			
			refreshText() ;
		}
	}

	@Override
	synchronized public void addLobby(WiFiLobbyDetails desc) {
		// Check whether this description constitutes an update for one
		// of our Lobby items.  If so, pass it to refreshView.  Otherwise,
		// add in the appropriate location.
		Iterator<WiFiLobbyDetails> iter = mWiFiLobbyDetailsArrayList.iterator() ;
		for ( ; iter.hasNext() ; ) {
			WiFiLobbyDetails ld = iter.next() ;
			if ( desc.isSameLobby(ld) ) {
				refreshView(desc) ;
				return ;
			}
		}
		
		// Whelp, we better add it instead.  Since this is a single-item update
		// we just perform in-place and let the adapter sort out updates.
		int index = insertionSortIndex( mWiFiLobbyDetailsArrayList, desc ) ;
		mWiFiLobbyDetailsArrayList.add(index, desc.clone()) ;
		if ( mStarted ) {
			mWiFiLobbyDetailsArrayAdapter.insert(mWiFiLobbyDetailsArrayList.get(index), index) ;
			mWiFiLobbyDetailsArrayAdapter.notifyDataSetChanged() ;
			refreshText() ;
		}
	}

	@Override
	synchronized public void refreshView(WiFiLobbyDetails desc) {
		if ( desc == null ) {
			refreshText() ;
			return ;
		}
		
		// This description is an update for one currently available.
		// Find it.
		int descLoc = -1 ;
		for ( int i = 0; i < mWiFiLobbyDetailsArrayList.size(); i++ ) {
			if ( desc.isSameLobby(mWiFiLobbyDetailsArrayList.get(i) ) ) {
				descLoc = i ;
				break ;
			}
		}
		
		if ( descLoc == -1 ) {
			addLobby(desc) ;
			return ;
		}
		
		// That's where it WAS.  Find where it SHOULD BE.
		int index = insertionSortIndex( mWiFiLobbyDetailsArrayList, desc ) ;
		// If this is descLoc, or descLoc+1, it hasn't moved.  Perform
		// the update in-place.
		if ( index == descLoc || index == descLoc+1 ) {
			Log.d(TAG, "updating lobby " + descLoc + " in place") ;
			WiFiLobbyDetails localDetails = mWiFiLobbyDetailsArrayList.get(descLoc) ;
			localDetails.mergeFrom( desc ) ;
			if ( mStarted ) {
				// Push a forcible refresh onto the view itself
				((WiFiLobbyDetailsArrayAdapter)mWiFiLobbyDetailsArrayAdapter).refreshLobbyView( localDetails ) ;
				mWiFiLobbyDetailsArrayAdapter.reIndex() ;
				// we will forceRefresh later on, which (among other things)
				// calls notifyDataSetChanged()
			}
		}
		else {
			// Otherwise, we need to actually MOVE the description object to a new
			// location, which is index if index < descLoc, index-1 otherwise (after
			// having removed it).
			
			// Do this is three steps.  Remove, update, insert.
			WiFiLobbyDetails ld = mWiFiLobbyDetailsArrayList.remove(descLoc) ;
			if ( mStarted ) {
				((WiFiLobbyDetailsArrayAdapter)mWiFiLobbyDetailsArrayAdapter).refreshLobbyView( ld ) ;
				mWiFiLobbyDetailsArrayAdapter.remove(ld) ;
			}
			ld.mergeFrom(desc) ;
			mWiFiLobbyDetailsArrayList.add(index < descLoc ? index : index-1, ld) ;
			Log.d(TAG, "updating lobby " + descLoc + " to location " + (index < descLoc ? index : index-1)) ;
			if ( mStarted ) {
				mWiFiLobbyDetailsArrayAdapter.insert(ld, index < descLoc ? index : index-1) ;
				// we will forceRefresh later on, which (among other things)
				// calls notifyDataSetChanged()
			}
		}
		
		if ( mStarted ) {
			refreshText() ;
			forceRefresh( mLobbyListID, mWiFiLobbyDetailsArrayAdapter ) ;
		}
	}
	
	private void forceRefresh( int listID, WiFiLobbyDetailsArrayAdapter adapter ) {
		forceRefresh( (ListView)findViewById(listID), adapter ) ;
	}
	
	private void forceRefresh( ListView v, WiFiLobbyDetailsArrayAdapter adapter ) {
		adapter.notifyDataSetChanged() ;
		v.invalidateViews() ;
		v.postInvalidate() ;
	}

	@Override
	synchronized public void removeLobby(WiFiLobbyDetails desc) {
		// Removing is easy.  Don't use "remove" on the array list, because
		// equals() has some weird functionality.  Maybe I'll get around to
		// fixing this later.
		int descLoc = -1 ;
		for ( int i = 0; i < mWiFiLobbyDetailsArrayList.size(); i++ ) {
			WiFiLobbyDetails ld = mWiFiLobbyDetailsArrayList.get(i) ;
			if ( ld.isSameLobby(desc) ) {
				descLoc = i ;
				break ;
			}
		}
		
		if ( descLoc < 0 )
			return ;
		
		WiFiLobbyDetails ld = mWiFiLobbyDetailsArrayList.remove(descLoc) ;
		if ( mStarted ) {
			mWiFiLobbyDetailsArrayAdapter.remove(ld) ;
			mWiFiLobbyDetailsArrayAdapter.notifyDataSetChanged() ;
			refreshText() ;
		}
	}
	
	@Override
	synchronized public void setHiddenLobbies( boolean has ) {
		if ( has != mHiddenLobbies ) {
			mHiddenLobbies = has ;
			mActionStrip.setEnabled( mActionStrip.getButton(ACTION_NAME_UNHIDE), mHiddenLobbies ) ;
		}
	}
	
	/**
	 * Refreshes the text displayed under challenge lists.
	 */
	private void refreshText() {
		
		Context context = getContext() ;
		
		// Get a reference to the textview if we don't have one already.
		if ( mInstructionsTextView == null ) {
			mInstructionsTextView = (TextView)findViewById(mInstructionsTextViewID) ;
			Resources res = context.getResources() ;
			mInstructionsStringLobbiesNoWifi = res.getString(R.string.menu_wifi_lobby_manager_instructions_no_wifi) ;
			mInstructionsStringLobbiesSearching = res.getString(R.string.menu_wifi_lobby_manager_instructions_searching) ;
			mInstructionsStringLobbiesNone = res.getString(R.string.menu_wifi_lobby_manager_instructions_empty) ;
			mInstructionsStringLobbiesFull = res.getString(R.string.menu_wifi_lobby_manager_instructions_full_only) ;
			mInstructionsStringLobbiesOpen = res.getString(R.string.menu_wifi_lobby_manager_instructions_open) ;
		}
		
		// we can't be sure that we have no lobbies by counting the size of our list;
		// it might be populated with "searches" that don't count as lobbies.
		boolean hasLobby = false ;
		for ( int i = 0; i < mWiFiLobbyDetailsArrayList.size() && !hasLobby; i++ ) {
			WiFiLobbyDetails ld = mWiFiLobbyDetailsArrayList.get(i) ;
			if ( ld != null ) {
				hasLobby = hasLobby || !this.isSearching(ld) ;
			}
		}
		
		if ( mInstructionsTextView != null && mInstructionsTextView.getHeight() >= 80 ) {
			if ( WifiMonitor.getWifiIpAddress(context) == 0 )
				mInstructionsTextView.setText( mInstructionsStringLobbiesNoWifi ) ;
			else if ( mSearching ) {
				Log.d(TAG, "setting instructions to " + mInstructionsStringLobbiesSearching ) ;
				mInstructionsTextView.setText( mInstructionsStringLobbiesSearching ) ;
			}
			else if ( !hasLobby )
				mInstructionsTextView.setText( mInstructionsStringLobbiesNone ) ;
			else {
				// look for open.
				boolean hasOpen = false ;
				Iterator<WiFiLobbyDetails> iter = mWiFiLobbyDetailsArrayList.iterator() ;
				for ( ; iter.hasNext() ; ) {
					WiFiLobbyDetails ld = iter.next() ;
					if ( ld.hasReceivedStatus() && ld.getReceivedStatus().getNumPeople() < ld.getReceivedStatus().getMaxPeople() && !isSearching(ld) ) {
						hasOpen = true ;
						break ;
					}
				}
				
				if ( hasOpen )
					mInstructionsTextView.setText(mInstructionsStringLobbiesOpen) ;
				else
					mInstructionsTextView.setText(mInstructionsStringLobbiesFull) ;
			}
		}
		else if ( mInstructionsTextView != null ) {
			mInstructionsTextView.setText("") ;
		}
	}
	
	
	synchronized private int insertionSortIndex( ArrayList<WiFiLobbyDetails> al, WiFiLobbyDetails ld ) {
		
		// our sorting method is as-follows: we place requested lobbies first, open & discovered
		// lobbies next, and full & discovered lobbies last.
		
		// although all targeted lobbies end up in the same section, we prioritize
		// those which have received data.
		
		// Within each group, we place oldest lobbies first, newest lobbies after.
		// our sorting method is as-follows: we place open lobbies first, closed lobbies after.
		// Within each group, we place oldest lobbies first, newest lobbies last.
		
		int section = WiFiLobbyDetailsIndexer.getSection(ld) ;
		boolean hasReceived = ld.hasReceivedStatus() ;
		for ( int i = 0; i < al.size(); i++ ) {
			WiFiLobbyDetails ld_here = al.get(i) ;
			int section_i = WiFiLobbyDetailsIndexer.getSection(ld_here) ;
			if ( section < section_i )
				return i ;
			else if ( section == section_i ) {
				boolean hasReceived_here = ld_here.hasReceivedStatus() ;
				// if both have received, compare ages.
				if ( hasReceived && hasReceived_here ) {
					if ( ld.getReceivedStatus().getAge() >= ld_here.getReceivedStatus().getAge() ) {
						// older: put it first
						return i ;
					}
				}
				// if only 'ld' has received, put it first (prioritize those
				// which have responses).
				if ( hasReceived && !hasReceived_here ) {
					return i ;
				}
				// if neither have received, compare targeted ats
				if ( !hasReceived && !hasReceived_here ) {
					if ( ((WiFiLobbyDetails.TargetStatus)ld.getStatus()).getTargetedAt()
							<= ((WiFiLobbyDetails.TargetStatus)ld_here.getStatus()).getTargetedAt() ) {
						// older: put it first
						return i ;
					}
				}
			}
		}
		
		return al.size() ;
	}
	
	
	public boolean isSearching( WiFiLobbyDetails details ) {
		return !details.hasReceivedStatus()
				|| details.getReceivedStatus().getTimeSinceReceived() >= this.mMarkAsSearchingInterval ;
	}
	
	private static class WiFiLobbyDetailsIndexer implements SectionIndexer {
		
		protected static final int NUM_SECTIONS = 3 ;
		
		protected String [] mSections = new String[]{ "Searches & Invitations", "Discovered & Open", "Discovered & Full" } ;
		
		
		protected WiFiLobbyDetails [] mWiFiLobbyDetails ;
		
		public WiFiLobbyDetailsIndexer( WiFiLobbyDetails [] descriptions ) {
			setWiFiLobbyDetailsValues( descriptions ) ;
		}
		
		synchronized public void setWiFiLobbyDetails( WiFiLobbyDetails [] descriptions ) {
			setWiFiLobbyDetailsValues( descriptions ) ;
		}
		
		private void setWiFiLobbyDetailsValues( WiFiLobbyDetails [] descriptions ) {
			if ( descriptions == null )
				mWiFiLobbyDetails = null ;
			else {
				// avoid nulls
				int num = 0 ; 
				for ( int i = 0; i < descriptions.length; i++ )
					if ( descriptions[i] != null )
						num++ ;
				if ( mWiFiLobbyDetails == null || mWiFiLobbyDetails.length != num )
					mWiFiLobbyDetails = new WiFiLobbyDetails[num] ;
				num = 0 ;
				for ( int i = 0; i < descriptions.length; i++ )
					if ( descriptions[i] != null )
						mWiFiLobbyDetails[num++] = descriptions[i] ;
			}
		}
		
	    synchronized public Object[] getSections() {
	        return mSections ;
	    }
	    
	    
	    /**
	     * Performs a binary search or cache lookup to find the first row that
	     * matches a given section.
	     * @param sectionIndex the section to search for
	     * @return the row index of the first occurrence, or the nearest next index.
	     * For instance, if section is '1', returns the first index of a history
	     * challenge.  If there are no history challenges, returns the length
	     * of the array.
	     */
	    synchronized public int getPositionForSection(int sectionIndex) {
	    	if ( sectionIndex < 0 ) {
				return -1 ;
	    	}
	    	
	    	if ( sectionIndex >= NUM_SECTIONS )
	    		return mWiFiLobbyDetails.length ;
	    	
	    	// If section index is 0, then no matter what, we return 0.
	    	// Either this is the first invited or searched lobby in
	    	// position 0, or we have none and so return the position
	    	// AFTER the end (which is 0).
	    	if ( sectionIndex == 0 )
	    		return 0 ;
	    	
	    	// we want the first uninvited, untargeted lobby which is
	    	// open (if sectionIndex == 1) or full (if sectionIndex == 2)
	    	for ( int i = 0; i < mWiFiLobbyDetails.length; i++ ) {
	    		WiFiLobbyDetails details = mWiFiLobbyDetails[i] ;
	    		if ( details.getSource() == WiFiLobbyDetails.Source.DISCOVERED ) {
	    			// whether this lobby is full or not, section 1 means the
	    			// first discovered lobby
	    			if ( sectionIndex == 1 )
	    				return i ;
	    			boolean open = details.getReceivedStatus().getNumPeople() < details.getReceivedStatus().getMaxPeople() ;
	    			if ( !open )
	    				return i ;
	    		}
	    	}
	    	
	    	return mWiFiLobbyDetails.length ;
	    }
	    
	    /**
	     * Returns the section index for a given position in the list by
	     * examining the item.
	     */
	    synchronized public int getSectionForPosition(int position) {
	    	if ( position < 0 || position >=  mWiFiLobbyDetails.length )
				return -1 ;
	    	
	    	WiFiLobbyDetails ld = mWiFiLobbyDetails[position] ;
	    	
	    	if ( ld.getSource() != WiFiLobbyDetails.Source.DISCOVERED )
	    		return 0 ;
	    	
	    	// full, or not?
	    	return (ld.getReceivedStatus().getNumPeople() >= ld.getReceivedStatus().getMaxPeople()) ? 2 : 1 ;
	    }
	    
	    synchronized public boolean isFirstInSection( int position ) {
			if ( position < 0 || position >= mWiFiLobbyDetails.length )
				return false ;
			if ( position == 0 )
				return true ;
			// section 0 has been covered (position == 0).  The
			// other sections are "discovered open" and "discovered full"
			WiFiLobbyDetails details = mWiFiLobbyDetails[position] ;
			WiFiLobbyDetails details_pre = mWiFiLobbyDetails[position-1] ;
			if ( details.getSource() != WiFiLobbyDetails.Source.DISCOVERED ) {
				return false ;
			} else if ( details_pre.getSource() != WiFiLobbyDetails.Source.DISCOVERED ) {
				return true ;
			}
			
			boolean full = details.getReceivedStatus().getNumPeople() == details.getReceivedStatus().getMaxPeople() ;
			boolean full_pre = details_pre.getReceivedStatus().getNumPeople() == details_pre.getReceivedStatus().getMaxPeople() ;
			
			// a difference indicates change-of-section.
			return full != full_pre ;
		}
	    
	    public static int getSection( WiFiLobbyDetails details ) {
	    	if ( details.getSource() != WiFiLobbyDetails.Source.DISCOVERED )
	    		return 0 ;
	    	return details.getReceivedStatus().getNumPeople() < details.getReceivedStatus().getMaxPeople()
	    			? 1 : 2 ;
	    }
	}
	
	
	private static class LobbyListRowTag {
		View mHeaderView ;
		View mHeaderViewTopSpacer ;
		TextView mHeaderTextView ;
		
		public LobbyListRowTag( View v ) {
			mHeaderView = v.findViewById(R.id.quantro_list_item_header) ;
			mHeaderViewTopSpacer = v.findViewById(R.id.quantro_list_item_header_top_spacer) ;
			mHeaderTextView = (TextView)v.findViewById(R.id.quantro_list_item_header_text) ;
		}
	}
	
	
	private static class LobbyListItemTag {
		LobbyButtonStrip mLBS ;
		LobbyButtonCollage mLBC ;
		WiFiLobbyDetails mWiFiLobbyDetails ;
		
		public LobbyListItemTag( View v ) {
			mLBS = (LobbyButtonStrip) v.findViewById( R.id.lobby_button_strip ) ;
			mLBC = (LobbyButtonCollage) v.findViewById( R.id.lobby_list_item_button_collage) ;
		}
		
		public void setWiFiLobbyDetails( WiFiLobbyDetails ld, boolean searching ) {
			if ( mLBS != null )
				mLBS.setWiFiLobbyDetails(ld, searching) ;
			if ( mLBC != null )
				mLBC.setWiFiLobbyDetails(ld, searching) ;
			mWiFiLobbyDetails = ld ;
		}
		
		public boolean setSearching( boolean searching ) {
			boolean changed = false ;
			if ( mLBS != null && mLBS.getWiFiLobbySearching() != searching ) {
				mLBS.setWiFiLobbySearching(searching) ;
				changed = true ;
			}
			if ( mLBC != null && mLBC.getWiFiLobbySearching() != searching ) {
				mLBC.setWiFiLobbySearching(searching) ;
				changed = true ;
			}
			return changed ;
		}
		
		public void refresh() {
			if ( mLBS != null )
				mLBS.refresh() ;
			if ( mLBC != null )
				mLBC.refresh() ;
		}
	}
	
	private class WiFiLobbyDetailsArrayAdapter extends SectionableAdapter
			implements OnScrollListener, PinnedHeaderListView.PinnedHeaderAdapter, SectionIndexer {
		
		// An indexer for lobbies
		private WiFiLobbyDetailsIndexer mIndexer;
		private ArrayList<WiFiLobbyDetails> mWiFiLobbyDetails ;
		private Hashtable<WiFiLobbyDetails, LobbyListItemTag> mWiFiLobbyDetailsTag ;
		
		boolean mScrolledToTop ;
		
		public WiFiLobbyDetailsArrayAdapter(LayoutInflater inflater,
				int rowLayoutID, int headerID, int itemHolderID) {
			super(inflater, rowLayoutID, headerID, itemHolderID);
			
			mWiFiLobbyDetails = new ArrayList<WiFiLobbyDetails>() ;
			WiFiLobbyDetails[] lds = new WiFiLobbyDetails[0] ;
			mIndexer = new WiFiLobbyDetailsIndexer(lds);
			mWiFiLobbyDetailsTag = new Hashtable<WiFiLobbyDetails, LobbyListItemTag>() ;
			
			mScrolledToTop = true ;
		}
		
		////////////////////////////////////////////////////////////////////////
		// Override methods for adding / removing items, so we can keep
		// mWiFiLobbyDetails and mIndexer up to date.
		
		/**
		 * Adds the specified object at the end of the array.
		 */
		synchronized public void add(WiFiLobbyDetails obj) {
			mWiFiLobbyDetails.add(obj) ;
			WiFiLobbyDetails[] lds = new WiFiLobbyDetails[mWiFiLobbyDetails.size()] ;
			lds = mWiFiLobbyDetails.toArray(lds) ;
			mIndexer.setWiFiLobbyDetails(lds) ;
		}
		
		synchronized public void clear() {
			mWiFiLobbyDetails.clear() ;
			mIndexer.setWiFiLobbyDetails(new WiFiLobbyDetails[0]) ;
			mWiFiLobbyDetailsTag.clear() ;
		}
		
		
		synchronized public void insert( WiFiLobbyDetails ld, int index ) {
			mWiFiLobbyDetails.add(index, ld) ;
			WiFiLobbyDetails[] lds = new WiFiLobbyDetails[mWiFiLobbyDetails.size()] ;
			lds = mWiFiLobbyDetails.toArray(lds) ;
			mIndexer.setWiFiLobbyDetails(lds) ;
		}
		
		
		synchronized public void remove(WiFiLobbyDetails ld) {
			// find the index
			int index = -1 ;
			for ( int i = 0; i < mWiFiLobbyDetails.size(); i++ )
				if ( mWiFiLobbyDetails.get(i) == ld )
					index = i ;
			
			if ( index == -1 )
				return ;
			
			mWiFiLobbyDetails.remove(index) ;
			WiFiLobbyDetails[] lds = new WiFiLobbyDetails[mWiFiLobbyDetails.size()] ;
			lds = mWiFiLobbyDetails.toArray(lds) ;
			mIndexer.setWiFiLobbyDetails(lds) ;
			mWiFiLobbyDetailsTag.remove(ld) ;
		}
		
		synchronized public void reIndex() {
			WiFiLobbyDetails[] lds = new WiFiLobbyDetails[mWiFiLobbyDetails.size()] ;
			lds = mWiFiLobbyDetails.toArray(lds) ;
			mIndexer.setWiFiLobbyDetails(lds) ;
		}
		
		public void refreshLobbyView( WiFiLobbyDetails desc ) {
			refreshLobbyView( desc, true ) ;
		}
		
		/**
		 * Returns whether any lobby transitioned from searching to not (or
		 * vice versa)
		 * @param desc
		 * @return
		 */
		public boolean updateLobbyViewSearching( WiFiLobbyDetails desc ) {
			return refreshLobbyView( desc, false ) ;
		}
		
		synchronized public boolean refreshLobbyView( WiFiLobbyDetails desc, boolean force ) {
			LobbyListItemTag tag = mWiFiLobbyDetailsTag.get(desc) ;
			if ( tag != null && desc.isSameLobby(tag.mWiFiLobbyDetails) ) {
				if ( tag.setSearching(isSearching(tag.mWiFiLobbyDetails)) ) {
					return true ;
				} else if ( force ) {
					tag.refresh() ;
					return true ;
				}
			}
			return false ;
		}
		
		public void refreshAllLobbyViews() {
			refreshAllLobbyViews(true) ;
		}
		
		/**
		 * Returns whether any lobby transitioned from searching to not (or
		 * vice versa).
		 */
		public boolean updateAllLobbyViewsSearching() {
			return refreshAllLobbyViews(false) ;
		}
		
		
		synchronized public boolean refreshAllLobbyViews( boolean force ) {
			boolean changed = false ;
			Set<Entry<WiFiLobbyDetails, LobbyListItemTag>> entrySet = mWiFiLobbyDetailsTag.entrySet() ;
			
			Iterator<Entry<WiFiLobbyDetails, LobbyListItemTag>> iter = entrySet.iterator() ;
			for ( ; iter.hasNext() ; ) {
				Entry<WiFiLobbyDetails, LobbyListItemTag> entry = iter.next() ;
				WiFiLobbyDetails details = entry.getKey() ;
				LobbyListItemTag tag = entry.getValue() ;
				if ( tag.mWiFiLobbyDetails == details ) {
					// correctly associated
					if ( tag.setSearching(isSearching(tag.mWiFiLobbyDetails)) ) {
						changed = true ;
					} else if ( force ) {
						tag.refresh() ;
						changed = true ;
					}
				}
			}
			return changed ;
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
		    LobbyListRowTag tag = null ;
			if ( topChild != null )
				tag = ((LobbyListRowTag)topChild.getTag()) ;
			
			mScrolledToTop = firstVisibleItem == 0 && visibleItemCount > 0 && topChild.getTop() == 0 ;
			boolean topHasHeader = mIndexer.isFirstInSection(firstVisibleItem) ;
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
			LobbyListRowTag tag = (LobbyListRowTag)header.getTag() ;
        	Drawable bg = tag.mHeaderTextView.getBackground() ;
        	bg.getPadding(tempPaddingRect) ;
        	r.set(0, tag.mHeaderTextView.getTop() + tempPaddingRect.top,
        			header.getWidth(), tag.mHeaderTextView.getBottom() - tempPaddingRect.bottom) ;
        	return true ;
        }
        
        @Override
        public boolean copyPinnedHeaderVisibleBounds(View header, int position, Rect r) {
        	LobbyListRowTag tag = (LobbyListRowTag)header.getTag() ;
        	r.set(0, tag.mHeaderTextView.getTop(),
        			header.getWidth(), tag.mHeaderTextView.getBottom()) ;
        	return true ;
        }
        
        
        @Override
        public boolean copyHeaderOccludingBounds(View view, int position, Rect r) {
        	LobbyListRowTag tag = (LobbyListRowTag)view.getTag() ;
        	if ( tag.mHeaderView.getVisibility() == View.GONE ) {
        		r.set(0, 0, 0, 0) ;
        		return true ;
        	}
        	
        	return copyPinnedHeaderOccludingBounds( tag.mHeaderView, position, r ) ;
        }
        
        @Override
        public boolean copyHeaderVisibleBounds(View view, int position, Rect r) {
        	LobbyListRowTag tag = (LobbyListRowTag)view.getTag() ;
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
				
				LobbyListRowTag tag = (LobbyListRowTag)v.getTag() ;
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
			return this.mWiFiLobbyDetails.get(position) ;
		}


		@Override
		protected int getDataCount() {
			return this.mWiFiLobbyDetails.size() ;
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
			return WiFiLobbyDetailsIndexer.NUM_SECTIONS ;
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
			WiFiLobbyDetails ld = (WiFiLobbyDetails)getItem(position) ;
			
			LobbyListItemTag tag = (LobbyListItemTag)cell.getTag() ;
    		if ( tag == null ) {
				tag = new LobbyListItemTag(cell) ;
				if ( tag.mLBS != null ) {
					tag.mLBS.setDelegate(WiFiLobbyManagerStripView.this) ;
				} 
				if ( tag.mLBC != null ) {
					tag.mLBC.setDelegate(WiFiLobbyManagerStripView.this) ;
				}
				cell.setTag(tag) ;
			}
    		// Set lobby description, set the height to the ideal height, and refresh.
			// This call automatically refreshes.
    		tag.setWiFiLobbyDetails(ld, isSearching(ld)) ;
    		
    		// keep a note for this LBS.
    		if ( ld != null )
    			mWiFiLobbyDetailsTag.put(ld, tag) ;
			
			// Setting height causes a measure.  If this happens before we refresh() we get weird results.
			if ( tag.mLBS != null )
				tag.mLBS.getLayoutParams().height = tag.mLBS.getIdealHeight() ;
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
			
			LobbyListRowTag tag = ((LobbyListRowTag)rowView.getTag()) ;
			if ( tag == null ) {
				tag = new LobbyListRowTag( rowView ) ;
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
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// ACTION BAR LISTENER METHODS /////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	
	

	@Override
	public boolean customButtonStrip_onButtonClick(CustomButtonStrip strip,
			int buttonNum, String name, boolean asOverflow ) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null || name == null )
			 return false ;
		
		boolean didAction = false ;
		
		if ( name.equals( ACTION_NAME_NEW ) ) {
			delegate.wlmvd_createNewWiFiLobby(this) ;
			didAction = true ;
		} else if ( name.equals( ACTION_NAME_FIND ) ) {
			delegate.wlmvd_findWiFiLobby(this) ;
			didAction = true ;
		} else if ( name.equals( ACTION_NAME_UNHIDE ) ) {
			delegate.wlmvd_unhideWifiLobbies(this) ;
			didAction = true ;
		} else if ( name.equals( ACTION_NAME_HELP ) ) {
			delegate.wlmvd_help(this) ;
			didAction = true ;
		} else if ( name.equals( ACTION_NAME_SETTINGS ) ) {
			delegate.wlmvd_settings(this) ;
			didAction = true ;
		}
		
		if ( didAction && mSoundControls && mSoundPool != null && !asOverflow )
			mSoundPool.menuButtonClick() ;
		
		return didAction ;
	}

	@Override
	public boolean customButtonStrip_onButtonLongClick(CustomButtonStrip strip,
			int buttonNum, String name) {
		
		return false;
	}
	
	
	@Override
	public boolean customButtonStrip_supportsLongClick(CustomButtonStrip strip,
			int buttonNum, String name) {
		
		return false;
	}
	
	@Override
	public void customButtonStrip_onPopupOpen(
			CustomButtonStrip strip ) {
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return ;
		
		if ( mSoundPool != null && mSoundControls )
			mSoundPool.menuButtonClick() ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// LOBBY BUTTON STRIP LISTENER METHODS /////////////////////////////////////
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
	public boolean lbs_onButtonClick(
			LobbyButtonStrip strip,
			int buttonNum, int buttonType, int lobbyType, WiFiLobbyDetails lobbyDetails, Lobby lobby ) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null )
			 return false ;
		// no lobby description - no effect
		if ( lobbyDetails == null )
			return false ;
		
		if ( lobbyType != LobbyButtonStrip.LOBBY_TYPE_WIFI )
			return false ;
		
		// our action depends on the button type,  Single-click on CREATE
		// will create, HIDE will hide, MAIN_OPEN will join.  No effect
		// on MAIN_FULL - long-press only for those.
		switch( buttonType ) {
		case LobbyButtonStrip.BUTTON_TYPE_NO_LOBBY:
			delegate.wlmvd_createNewWiFiLobby(this) ;
			if ( mSoundControls && mSoundPool != null )
				mSoundPool.menuButtonClick() ;
			return true ;
		case LobbyButtonStrip.BUTTON_TYPE_SEARCHING_HIDE:
		case LobbyButtonStrip.BUTTON_TYPE_OPEN_HIDE:
		case LobbyButtonStrip.BUTTON_TYPE_FULL_HIDE:
			delegate.wlmvd_hideWiFiLobby(lobbyDetails, this) ;
			if ( mSoundControls && mSoundPool != null )
				mSoundPool.menuButtonClick() ;
			return true ;
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_OPEN_MAIN:
			delegate.wlmvd_joinWiFiLobby(lobbyDetails, this) ;
			if ( mSoundControls && mSoundPool != null )
				mSoundPool.menuButtonClick() ;
			return true ;
		// Full and Searching: examine
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_FULL_MAIN:
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_SEARCHING_MAIN:
			delegate.wlmvd_examineWiFiLobby(lobbyDetails, this) ;
			if ( mSoundControls && mSoundPool != null )
				mSoundPool.menuButtonClick() ;
			return true ;
		}
		
		return false ;
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
	public boolean lbs_onButtonLongClick(
			LobbyButtonStrip strip,
			int buttonNum, int buttonType, int lobbyType, WiFiLobbyDetails lobbyDetails, Lobby lobby ) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null )
			return false ;
		// no lobby description - no effect
		if ( lobbyDetails == null )
			return false ;
		
		if ( lobbyType != LobbyButtonStrip.LOBBY_TYPE_WIFI )
			return false ;
		 
		// our action depends on the button type.  A main lobby button
		// will be examined. A hide lobby button will get a hide menu.
		switch( buttonType ) {
		case LobbyButtonStrip.BUTTON_TYPE_OPEN_HIDE:
		case LobbyButtonStrip.BUTTON_TYPE_FULL_HIDE:
			// TODO: Implement a context menu for this.
			// delegate.wlmvd_hideLobbyMenu(lobbyDescription, this) ;
			// return true ;
			return false ;
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_SEARCHING_MAIN:
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_OPEN_MAIN:
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_FULL_MAIN:
			delegate.wlmvd_examineWiFiLobby(lobbyDetails, this) ;
			if ( mSoundControls && mSoundPool != null )
				mSoundPool.menuButtonHold() ;
			return true ;
		}
		
		return false ;
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
	public boolean lbs_supportsLongClick(
			LobbyButtonStrip strip,
			int buttonNum, int buttonType, int lobbyType, WiFiLobbyDetails lobbyDetails, Lobby lobby ) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null )
			return false ;
		// no lobby description - no effect
		if ( lobbyDetails == null )
			return false ;
		
		if ( lobbyType != LobbyButtonStrip.LOBBY_TYPE_WIFI )
			return false ;
		 
		// our action depends on the button type.  A main lobby button
		// will be examined. A hide lobby button will get a hide menu.
		switch( buttonType ) {
		case LobbyButtonStrip.BUTTON_TYPE_OPEN_HIDE:
		case LobbyButtonStrip.BUTTON_TYPE_FULL_HIDE:
			return false ;
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_SEARCHING_MAIN:
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_OPEN_MAIN:
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_FULL_MAIN:
			return true ;
		}
		
		return false ;
	}
	
	

	@Override
	public boolean lbc_join(LobbyButtonCollage collage, Lobby lobby,
			WiFiLobbyDetails description, int lobbyType) {
		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null )
			 return false ;
		// no lobby description - no effect
		if ( description == null )
			return false ;
		
		if ( lobbyType != LobbyButtonStrip.LOBBY_TYPE_WIFI )
			return false ;
		
		delegate.wlmvd_joinWiFiLobby(description, this) ;
		return true ;
	}

	@Override
	public boolean lbc_examine(LobbyButtonCollage collage, Lobby lobby,
			WiFiLobbyDetails description, int lobbyType) {
		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null )
			 return false ;
		// no lobby description - no effect
		if ( description == null )
			return false ;
		
		if ( lobbyType != LobbyButtonStrip.LOBBY_TYPE_WIFI )
			return false ;
		
		delegate.wlmvd_examineWiFiLobby(description, this) ;
		return true ;
	}

	@Override
	public boolean lbc_hide(LobbyButtonCollage collage, Lobby lobby,
			WiFiLobbyDetails description, int lobbyType) {
		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null )
			 return false ;
		// no lobby description - no effect
		if ( description == null )
			return false ;
		
		if ( lobbyType != LobbyButtonStrip.LOBBY_TYPE_WIFI )
			return false ;
		
		delegate.wlmvd_hideWiFiLobby(description, this) ;
		return true ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// WIFI MONITOR  ///////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Callback for the Wifi monitor.
	 */
	synchronized public void wml_hasWifiIpAddress( boolean hasIP, int ip ) {
		if ( !mStarted )
			return ; 
		
		if ( mOnWifi != hasIP )
			refreshText() ;
		if ( !mOnWifi && hasIP ) {
			// post a "searching" thingy
			mSearching = true ;
			mHandler.postDelayed(mSearchFinishedRunnable, TIME_SEARCHING) ;
		}
		mOnWifi = hasIP ;
		
		// repost the wifi monitor
		mHandler.postDelayed(mWifiMonitor, WIFI_MONITOR_REFRESH_FREQUENCY) ;
	}

}
