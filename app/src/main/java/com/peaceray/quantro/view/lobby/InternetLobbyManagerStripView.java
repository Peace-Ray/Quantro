package com.peaceray.quantro.view.lobby;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import com.peaceray.quantro.R;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.lobby.WiFiLobbyDetails;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.VersionSafe;
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
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

public class InternetLobbyManagerStripView extends RelativeLayout implements
	InternetLobbyManagerView, LobbyButtonStrip.Delegate, CustomButtonStrip.Delegate, OnClickListener, com.peaceray.quantro.view.button.collage.LobbyButtonCollage.Delegate {

	
	private static final String TAG = "ILManagerStripView" ;
	
	
	
	// Inflate from XML
	public InternetLobbyManagerStripView( Context context, AttributeSet attrs ) {
		super(context, attrs) ;
		
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public InternetLobbyManagerStripView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	private void constructor_setDefaultValues(Context context) {
		mActionStripID = 0 ;
		mLobbyListID = 0 ;
		mInstructionsTextViewID = 0 ;
		
		// Our delegate!
		mwrDelegate = new WeakReference<Delegate>(null) ;
		// Have we started?
		mStarted = false ;
		
		mServerResponse = SERVER_RESPONSE_UNSET ;
		
		ACTION_NAME_NEW = context.getResources().getString(R.string.action_strip_name_new) ;
		ACTION_NAME_REFRESH = context.getResources().getString(R.string.action_strip_name_refresh) ;
		ACTION_NAME_UNHIDE = context.getResources().getString(R.string.action_strip_name_unhide) ;
		ACTION_NAME_HELP = context.getResources().getString(R.string.action_strip_name_help) ;
		ACTION_NAME_SETTINGS = context.getResources().getString(R.string.action_strip_name_settings) ;
	}
	
	
	private void constructor_readAttributeSet(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.InternetLobbyManagerStripView);
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			case R.styleable.InternetLobbyManagerStripView_ilmsv_action_strip:
				mActionStripID = a.getResourceId(attr, mActionStripID) ;
				break ;
			case R.styleable.InternetLobbyManagerStripView_ilmsv_lobby_list:
				mLobbyListID = a.getResourceId(attr, mLobbyListID) ;
				break ;
			case R.styleable.InternetLobbyManagerStripView_ilmsv_instructions_text_view:
				mInstructionsTextViewID = a.getResourceId(attr, mInstructionsTextViewID) ;
				break ;
			}
		}
		
		// recycle
		a.recycle() ;
	}
	
	private void constructor_allocateAndInitialize(Context context) {
		// First: verify that we have the necessary ID fields.
		if ( mActionStripID == 0 )
			throw new RuntimeException("Must specify an a Strip ID for an Action Strip ID.") ;
		else if ( mLobbyListID == 0 ) 
			throw new RuntimeException("Must specify a LobbyList ID.") ;
		
		// Okay.  Allocate our ArrayLists and ArrayAdapters.
		mLobbyArrayList = new ArrayList<InternetLobby>() ;
		mLobbyArrayAdapter = new LobbyArrayAdapter(
				((Activity)context).getLayoutInflater(),
				R.layout.lobby_list_item,
				0,
				R.id.lobby_list_row) ;

		// NOTE: the ListViews may not have been inflated yet, so only
		// link them with these adapters in 'start'.
		
		mwrDelegate = new WeakReference<Delegate>(null) ;
		mStarted = false ;
		mHandler = new Handler() ;
	}
	
	
	private String ACTION_NAME_NEW ;
	private String ACTION_NAME_REFRESH ;
	private String ACTION_NAME_UNHIDE ;
	private String ACTION_NAME_HELP ;
	private String ACTION_NAME_SETTINGS ;
	
	// View IDs.  Help us refresh, add challenges, and the like.
	private int mActionStripID ;
	private int mLobbyListID ;
	private int mInstructionsTextViewID ;
	
	// ActionStrip!
	private CustomButtonStrip mActionStrip ;
	
	// Our text view, for informative text!
	private TextView mInstructionsTextView ;
	private String mInstructionsStringLobbiesNoResponse ;
	private String mInstructionsStringLobbiesSearching ;
	private String mInstructionsStringLobbiesNone ;
	private String mInstructionsStringLobbiesFull ;
	private String mInstructionsStringLobbiesOpen ;

	private LobbyArrayAdapter mLobbyArrayAdapter ;
	
	// Here's a copy of the list provided by the Activity.
	private ArrayList<InternetLobby> mLobbyArrayList ;
	// Are there any hidden lobbies apart from these?
	private boolean mLobbiesHidden = false ;
	
	private PinnedHeaderListView mLobbyListView ;
	
	// Our delegate!
	private WeakReference<Delegate> mwrDelegate ;
	// Sound pool and controls
	private QuantroSoundPool mSoundPool ;
	private boolean mSoundControls ;
	
	// Have we started?
	private boolean mStarted ;
	
	private int mServerResponse ;
	
	// We set a handler going that updates the text view after a certain period.
	// Until that point, we display a "looking for lobbies" message.  Here's
	// how we keep track.
	private Handler mHandler ;
	
	
	
	@Override
	synchronized public void start() {
		mStarted = true ;
		
		Context context = getContext() ;
		
		// Link the ArrayAdapter to the ListView.
		ListView lv ;
		lv = (ListView)findViewById(mLobbyListID) ;
		lv.setAdapter(mLobbyArrayAdapter) ;
		lv.setOnScrollListener((OnScrollListener)mLobbyArrayAdapter) ;
		lv.setItemsCanFocus(true) ;
		if ( lv instanceof PinnedHeaderListView ) {
			mLobbyListView = (PinnedHeaderListView) lv ;
			View pinnedHeaderView = ((Activity)context).getLayoutInflater().inflate(R.layout.quantro_list_item_header, lv, false) ;
			pinnedHeaderView.setTag( new LobbyListRowTag( pinnedHeaderView ) ) ;
			((PinnedHeaderListView)lv).setPinnedHeaderView( pinnedHeaderView );
			lv.setDividerHeight(0);
		}
		Iterator<InternetLobby> iter = mLobbyArrayList.iterator() ;
		mLobbyArrayAdapter.clear() ;
		for ( ; iter.hasNext() ; )
			mLobbyArrayAdapter.add(iter.next()) ;
		mLobbyArrayAdapter.notifyDataSetChanged() ;
		
		// Make ourself the Listener for the action strip and refresh it (everything
		// should already be set).
		mActionStrip = (CustomButtonStrip)findViewById(mActionStripID) ;
		if ( mActionStrip != null ) {
			mActionStrip.setDelegate(this) ;
			mActionStrip.setEnabled( mActionStrip.getButton(ACTION_NAME_NEW), true ) ;
			mActionStrip.setEnabled( mActionStrip.getButton(ACTION_NAME_REFRESH), true ) ;
			mActionStrip.setEnabled( mActionStrip.getButton(ACTION_NAME_UNHIDE), mLobbiesHidden ) ;
			mActionStrip.setEnabled( mActionStrip.getButton(ACTION_NAME_HELP), true ) ;
			mActionStrip.setEnabled( mActionStrip.getButton(ACTION_NAME_SETTINGS), true ) ;
			
			mActionStrip.refresh() ;
		}
		
		
		
		// Start the handler and other metadata going so we change the
		// instructions upon finding lobbies.
		mHandler = new Handler() ;
		// mHandler.post( a Runnable ) ;
		// TODO: post any runnables, possibly delayed.
		
		refreshText() ;

		
		this.postInvalidate() ;
	}

	@Override
	synchronized public void stop() {
		mStarted = false ;
		
		// TODO: remove any callbacks in place.
		// mHandler.removeCallbacks( a Runnable reference ) ;
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
	
	
	@Override
	synchronized public void setServerResponse( int response ) {
		mServerResponse = response ;
		refreshText() ;
	}
	
	
	@Override
	synchronized public void setLobbies(ArrayList<InternetLobby> lobbies) {
		mLobbyArrayList.clear();
		Iterator<InternetLobby> iter = lobbies.iterator() ;
		for ( ; iter.hasNext() ; ) {
			InternetLobby l = iter.next() ;
			int index = insertionSortIndex( mLobbyArrayList, l ) ;
			mLobbyArrayList.add(index, l.newInstance()) ;
		}
		
		if ( mStarted ) {
			mLobbyArrayAdapter.clear();
			iter = mLobbyArrayList.iterator() ;
			for ( ; iter.hasNext() ; )
				mLobbyArrayAdapter.add(iter.next()) ;
			mLobbyArrayAdapter.notifyDataSetChanged() ;
			
			if ( mLobbyListView != null )
				mLobbyListView.invalidateViews() ;
			
			// Refresh immediately, and in 1/2 second.  This helps resolve
			// a bug where the list suddenly grows, pushing the text view
			// to a very small size, but the content has already been 
			// set before the measurement.  We assume the measurement will
			// be finished in 1/2 second.
			refreshText() ;
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					refreshText() ;
				}
			}, 500) ;
		}
	}
	
	@Override
	synchronized public void addLobby(InternetLobby lobby) {
		// Check whether this description constitutes an update for one
		// of our Lobby items.  If so, pass it to refreshView.  Otherwise,
		// add in the appropriate location.
		Iterator<InternetLobby> iter = mLobbyArrayList.iterator() ;
		for ( ; iter.hasNext() ; ) {
			InternetLobby l = iter.next() ;
			if ( lobby.getLobbyNonce().equals(l.getLobbyNonce()) ) {
				refreshView(lobby) ;
				return ;
			}
		}
		
		// Whelp, we better add it instead.  Since this is a single-item update
		// we just perform in-place and let the adapter sort out updates.
		int index = insertionSortIndex( mLobbyArrayList, lobby ) ;
		mLobbyArrayList.add(index, lobby.newInstance()) ;
		if ( mStarted ) {
			mLobbyArrayAdapter.insert(mLobbyArrayList.get(index), index) ;
			if ( mLobbyListView != null )
				mLobbyListView.invalidateViews() ;
			
			// Refresh text immediately, and in 1/2 second.  This helps resolve
			// a bug where the list suddenly grows, pushing the text view
			// to a very small size, but the content has already been 
			// set before the measurement.  We assume the measurement will
			// be finished in 1/2 second.
			refreshText() ;
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					refreshText() ;
				}
			}, 500) ;
		}
	}
	
	
	@Override
	synchronized public void refreshView(InternetLobby lobby) {
		if ( lobby == null ) {
			refreshText() ;
			return ;
		}
		
		// Find it.
		int loc = -1 ;
		for ( int i = 0; i < mLobbyArrayList.size(); i++ ) {
			if ( lobby.getLobbyNonce().equals( (mLobbyArrayList.get(i).getLobbyNonce() ) ) ) {
				loc = i ;
				break ;
			}
		}
		
		if ( loc == -1 ) {
			addLobby(lobby) ;
			return ;
		}
		
		// That's where it WAS.  Find where it SHOULD BE.
		int index = insertionSortIndex( mLobbyArrayList, lobby ) ;
		// If this is descLoc, or descLoc+1, it hasn't moved.  Perform
		// the update in-place.
		if ( index == loc || index == loc+1 ) {
			mLobbyArrayList.set(loc, lobby.newInstance()) ;
			if ( mStarted ) {
				// Push a forcible refresh onto the view itself
				((LobbyArrayAdapter)mLobbyArrayAdapter).refreshLobbyView( lobby ) ;
				// Notify the ArrayAdapter that the underlying data has changed.
				mLobbyArrayAdapter.notifyDataSetChanged() ;
			}
		}
		else {
			// Otherwise, we need to actually MOVE the description object to a new
			// location, which is index if index < descLoc, index-1 otherwise (after
			// having removed it).
			
			// Do this is three steps.  Remove, update, insert.
			InternetLobby l = mLobbyArrayList.remove(loc) ;
			if ( mStarted ) {
				((LobbyArrayAdapter)mLobbyArrayAdapter).refreshLobbyView( lobby ) ;
				mLobbyArrayAdapter.remove(l) ;
			}
			mLobbyArrayList.add(index < loc ? index : index-1, lobby) ;
			if ( mStarted ) {
				mLobbyArrayAdapter.insert(lobby, index < loc ? index : index-1) ;
				((LobbyArrayAdapter)mLobbyArrayAdapter).refreshLobbyView( lobby ) ;
			}
		}
		
		if ( mStarted ) {
			forceRefresh( mLobbyListID, mLobbyArrayAdapter ) ;
			if ( mLobbyListView != null )
				mLobbyListView.invalidateViews() ;
			
			// Refresh text immediately, and in 1/2 second.  This helps resolve
			// a bug where the list suddenly grows, pushing the text view
			// to a very small size, but the content has already been 
			// set before the measurement.  We assume the measurement will
			// be finished in 1/2 second.
			refreshText() ;
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					refreshText() ;
				}
			}, 500) ;
		}
	}
	
	private void forceRefresh( int listID, LobbyArrayAdapter adapter ) {
		forceRefresh( (ListView)findViewById(listID), adapter ) ;
	}
	
	private void forceRefresh( ListView v, LobbyArrayAdapter adapter ) {
		adapter.notifyDataSetChanged() ;
		v.invalidateViews() ;
		v.postInvalidate() ;
	}
	
	
	@Override
	synchronized public void removeLobby(InternetLobby lobby) {
		// Removing is easy.  Don't use "remove" on the array list, because
		// equals() has some weird functionality.  Maybe I'll get around to
		// fixing this later.
		int loc = -1 ;
		for ( int i = 0; i < mLobbyArrayList.size(); i++ ) {
			InternetLobby l = mLobbyArrayList.get(i) ;
			if ( lobby.getLobbyNonce().equals( l.getLobbyNonce() ) ) {
				loc = i ;
				break ;
			}
		}
		
		if ( loc < 0 )
			return ;
		
		mLobbyArrayList.remove(loc) ;
		if ( mStarted ) {
			mLobbyArrayAdapter.clear();
			Iterator<InternetLobby> iter = mLobbyArrayList.iterator() ;
			for ( ; iter.hasNext() ; )
				mLobbyArrayAdapter.add(iter.next()) ;
			mLobbyArrayAdapter.notifyDataSetChanged() ;
			
			if ( mLobbyListView != null )
				mLobbyListView.invalidateViews() ;
			
			// Refresh text immediately, and in 1/2 second.  This helps resolve
			// a bug where the list suddenly grows, pushing the text view
			// to a very small size, but the content has already been 
			// set before the measurement.  We assume the measurement will
			// be finished in 1/2 second.
			refreshText() ;
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					refreshText() ;
				}
			}, 500) ;
		}
	}
	
	
	/**
	 * Are there currently hidden lobbies? 
	 * @param hidden
	 */
	public void setHiddenLobbies( boolean hidden ) {
		if ( mLobbiesHidden != hidden ) {
			mLobbiesHidden = hidden ;
			if ( mActionStrip != null ) {
				mActionStrip.setEnabled( mActionStrip.getButton(ACTION_NAME_UNHIDE), mLobbiesHidden ) ;
				mActionStrip.refresh() ;
			}
		}
	}
	
	
	/**
	 * Refreshes the text displayed under challenge lists.
	 */
	private void refreshText() {
		
		// Get a reference to the textview if we don't have one already.
		if ( mInstructionsTextView == null ) {
			mInstructionsTextView = (TextView)findViewById(mInstructionsTextViewID) ;
			Resources res = getContext().getResources() ;
			mInstructionsStringLobbiesNoResponse = res.getString(R.string.menu_internet_lobby_manager_instructions_no_response) ;
			mInstructionsStringLobbiesSearching = res.getString(R.string.menu_internet_lobby_manager_instructions_pending) ;
			mInstructionsStringLobbiesNone = res.getString(R.string.menu_internet_lobby_manager_instructions_empty) ;
			mInstructionsStringLobbiesFull = res.getString(R.string.menu_internet_lobby_manager_instructions_full_only) ;
			mInstructionsStringLobbiesOpen = res.getString(R.string.menu_internet_lobby_manager_instructions_open) ;
		
			mInstructionsTextView.setOnClickListener(this) ;
		}
		
		if ( mInstructionsTextView != null && mInstructionsTextView.getHeight() >= 120 ) {
			if ( mServerResponse == SERVER_RESPONSE_NONE )
				mInstructionsTextView.setText( mInstructionsStringLobbiesNoResponse ) ;
			else if ( mServerResponse == SERVER_RESPONSE_PENDING
					|| (mServerResponse == SERVER_RESPONSE_UNSET && mLobbyArrayList.size() == 0 )) {
				Log.d(TAG, "setting instructions to " + mInstructionsStringLobbiesSearching ) ;
				mInstructionsTextView.setText( mInstructionsStringLobbiesSearching ) ;
			}
			else if ( mLobbyArrayList.size() == 0 )
				mInstructionsTextView.setText( mInstructionsStringLobbiesNone ) ;
			else {
				// look for open.
				boolean hasOpen = false ;
				Iterator<InternetLobby> iter = mLobbyArrayList.iterator() ;
				for ( ; iter.hasNext() ; ) {
					InternetLobby ld = iter.next() ;
					if ( ld.getNumPeople() < ld.getMaxPeople() ) {
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
	
	
	synchronized private int insertionSortIndex( ArrayList<InternetLobby> al, InternetLobby l ) {
		
		// our sorting method is as-follows: we place non public-listed lobbies first,
		// public and non-full lobbies in the middle, and finally full, public lobbies.
		// We maintain the existing order within each group (we trust that the server knows
		// what it's doing).
		
		// Existing order can be maintained by: check the section for the particular lobby.  If it
		// exists, return its location.  Otherwise, return last+1 to place it after the rest
		// (new lobbies get put at the back).
		
		// To be general use, we manually inspect the list of lobbies for this region boundaries,
		// but assume the list itself has been organized by the rules above.
		
		boolean public_listed = l.getOrigin() == InternetLobby.ORIGIN_PUBLIC_LIST ;
		boolean open = l.getNumPeople() < l.getMaxPeople() ;
		
		int firstOpen = al.size(), firstFull = al.size() ;
		for ( int i = al.size()-1; i >= 0; i-- ) {
			InternetLobby lHere = al.get(i) ;
			if ( lHere.getOrigin() == InternetLobby.ORIGIN_PUBLIC_LIST ) {
				if ( lHere.getNumPeople() < lHere.getMaxPeople() )
					firstOpen = i ;
				firstFull = i ;
			}
		}
		
		// now place.
		if ( !public_listed ) {
			for ( int i = 0; i < firstOpen; i++ )
				if ( al.get(i).getLobbyNonce().equals(l.getLobbyNonce()) )
					return i ;
			return firstOpen ;
		}
		
		if ( open ) {
			for ( int i = firstOpen; i < firstFull; i++ )
				if ( al.get(i).getLobbyNonce().equals(l.getLobbyNonce()) )
					return i ;
			return firstFull ;
		}
		
		for ( int i = firstFull; i < al.size(); i++ )
			if ( al.get(i).getLobbyNonce().equals(l.getLobbyNonce()) )
				return i ;
		return al.size() ;
	}
	
	
	private class LobbyIndexer implements SectionIndexer {
		
		protected static final int NUM_SECTIONS = 3 ;
		
		protected String [] mSections = new String[]{ "Invitations", "Public Lobbies", "Full Lobbies" } ;
		protected InternetLobby [] mLobbies ;
		
		public LobbyIndexer( InternetLobby [] lobbies ) {
			setLobbyValues( lobbies ) ;
		}
		
		synchronized public void setLobbies( InternetLobby [] lobbies ) {
			setLobbyValues( lobbies ) ;
		}
		
		private void setLobbyValues( InternetLobby [] lobbies ) {
			if ( lobbies == null )
				mLobbies = null ;
			else {
				// avoid nulls
				int num = 0 ; 
				for ( int i = 0; i < lobbies.length; i++ )
					if ( lobbies[i] != null )
						num++ ;
				if ( mLobbies == null || mLobbies.length != num )
					mLobbies = new InternetLobby[num] ;
				num = 0 ;
				for ( int i = 0; i < lobbies.length; i++ )
					if ( lobbies[i] != null )
						mLobbies[num++] = lobbies[i] ;
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
	    	
	    	if ( sectionIndex < 0 )
				return -1 ;
	    	
	    	if ( sectionIndex == 0 )
	    		return 0 ;
	    	
	    	for ( int i = 0; i < mLobbies.length; i++ ) {
	    		InternetLobby l = mLobbies[i] ;
	    		if ( sectionIndex == 1 && l.getOrigin() == InternetLobby.ORIGIN_PUBLIC_LIST )
	    			return i ;
	    		else if ( sectionIndex == 2 && l.getNumPeople() == l.getMaxPeople() )
	    			return i ;
	    	}
	    	
	    	return mLobbies.length ;
	    }
	    
	    /**
	     * Returns the section index for a given position in the list by
	     * examining the item.
	     */
	    synchronized public int getSectionForPosition(int position) {
	    	if ( position < 0 || position >=  mLobbies.length )
				return -1 ;
	    	
	    	InternetLobby l = mLobbies[position] ;

	    	if ( l.getOrigin() != InternetLobby.ORIGIN_PUBLIC_LIST )
	    		return 0 ;
	    	// basically, is this full, or not?
	    	return (l.getNumPeople() == l.getMaxPeople()) ? 2 : 1 ;
	    }
	    
	    synchronized public boolean isFirstInSection( int position ) {
			if ( position < 0 || position >= mLobbies.length )
				return false ;
			if ( position == 0 )
				return true ;
			// section 0 has been covered (position == 0).  Sections
			// 1 and 2 are non-invited and 'non-invited-full', respectively.
			boolean invited = mLobbies[position].getOrigin() != InternetLobby.ORIGIN_PUBLIC_LIST ;
			boolean full = mLobbies[position].getNumPeople() == mLobbies[position].getMaxPeople() ;
			boolean invited_pre = mLobbies[position-1].getOrigin() != InternetLobby.ORIGIN_PUBLIC_LIST ;
			boolean full_pre = mLobbies[position-1].getNumPeople() == mLobbies[position-1].getMaxPeople() ;
			
			// a difference indicates change-of-section.
			return invited != invited_pre || full != full_pre ;
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
		Lobby mLobby ;
		
		public LobbyListItemTag( View v ) {
			mLBS = (LobbyButtonStrip) v.findViewById( R.id.lobby_button_strip ) ;
			mLBC = (LobbyButtonCollage) v.findViewById( R.id.lobby_list_item_button_collage) ;
		}
		
		public void setLobby( Lobby l ) {
			if ( mLBS != null )
				mLBS.setLobby(l) ;
			if ( mLBC != null )
				mLBC.setLobby(l) ;
			mLobby = l ;
		}
		
		public void refresh() {
			if ( mLBS != null )
				mLBS.refresh() ;
			if ( mLBC != null )
				mLBC.refresh() ;
		}
	}
	
	private class LobbyArrayAdapter extends SectionableAdapter
			implements OnScrollListener, PinnedHeaderListView.PinnedHeaderAdapter, SectionIndexer {
		
		// An indexer for lobbies
		private LobbyIndexer mIndexer;
		private ArrayList<InternetLobby> mLobbies ;
		private Hashtable<Nonce, LobbyListItemTag> mLobbyTag ;
		
		boolean mScrolledToTop ;
		
		public LobbyArrayAdapter(LayoutInflater inflater,
				int rowLayoutID, int headerID, int itemHolderID) {
			super(inflater, rowLayoutID, headerID, itemHolderID);
			
			mLobbies = new ArrayList<InternetLobby>() ;
			mIndexer = new LobbyIndexer(new InternetLobby[0]);
			mLobbyTag = new Hashtable<Nonce, LobbyListItemTag>() ;
			
			mScrolledToTop = true ;
		}
		
		////////////////////////////////////////////////////////////////////////
		// Override methods for adding / removing items, so we can keep
		// mLobbyDescriptions and mIndexer up to date.
		
		/**
		 * Adds the specified object at the end of the array.
		 */
		synchronized public void add(InternetLobby obj) {
			mLobbies.add(obj) ;
			InternetLobby[] ils = new InternetLobby[mLobbies.size()] ;
			ils = mLobbies.toArray(ils) ;
			mIndexer.setLobbies(ils) ;
		}
		
		synchronized public void clear() {
			mLobbies.clear() ;
			mIndexer.setLobbies(new InternetLobby[0]) ;
			mLobbyTag.clear() ;
		}
		
		
		synchronized public void insert( InternetLobby ld, int index ) {
			mLobbies.add(index, ld) ;
			InternetLobby[] ils = new InternetLobby[mLobbies.size()] ;
			ils = mLobbies.toArray(ils) ;
			mIndexer.setLobbies(ils) ;
		}
		
		
		synchronized public void remove(InternetLobby c) {
			// find the index
			int index = -1 ;
			for ( int i = 0; i < mLobbies.size(); i++ )
				if ( mLobbies.get(i) == c )
					index = i ;
			
			if ( index == -1 )
				return ;
			
			mLobbies.remove(index) ;
			InternetLobby[] ils = new InternetLobby[mLobbies.size()] ;
			ils = mLobbies.toArray(ils) ;
			mIndexer.setLobbies(ils) ;
			mLobbyTag.remove(c) ;
		}
		
		
		synchronized public void refreshLobbyView( InternetLobby l ) {
			LobbyListItemTag tag = mLobbyTag.get(l.getLobbyNonce()) ;
			if ( tag != null && tag.mLobby != null && l.getLobbyNonce().equals( tag.mLobby.getLobbyNonce() ) )
				tag.refresh() ;
		}
		
		
		synchronized public void refreshAllLobbyViews() {
			Set<Entry<Nonce, LobbyListItemTag>> entrySet = mLobbyTag.entrySet() ;
			
			Iterator<Entry<Nonce, LobbyListItemTag>> iter = entrySet.iterator() ;
			for ( ; iter.hasNext() ; )
				iter.next().getValue().refresh() ;
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
				tag.mHeaderTextView.setVisibility(View.VISIBLE) ;
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
			return this.mLobbies.get(position) ;
		}


		@Override
		protected int getDataCount() {
			return this.mLobbies.size() ;
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
			return LobbyIndexer.NUM_SECTIONS ;
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
			InternetLobby il = (InternetLobby)getItem(position) ;
			
			LobbyListItemTag tag = (LobbyListItemTag)cell.getTag() ;
    		if ( tag == null ) {
				tag = new LobbyListItemTag(cell) ;
				if ( tag.mLBS != null ) {
					tag.mLBS.setDelegate(InternetLobbyManagerStripView.this) ;
				} 
				if ( tag.mLBC != null ) {
					tag.mLBC.setDelegate(InternetLobbyManagerStripView.this) ;
				}
				cell.setTag(tag) ;
			}
    		// Set lobby description, set the height to the ideal height, and refresh.
			// This call automatically refreshes.
    		if ( tag.mLobby != il ) {
    			tag.setLobby(il) ;
    		}
    		
    		// keep a note for this LBS.
    		if ( il != null )
    			mLobbyTag.put(il.getLobbyNonce(), tag) ;
			
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
			int buttonNum, String name, boolean asOverflow) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null || name == null )
			 return false ;
		
		boolean didAction = false ;
		
		if ( name.equals( ACTION_NAME_NEW ) ) {
			delegate.ilmvd_createNewInternetLobby(this) ;
			didAction = true ;
		} else if ( name.equals( ACTION_NAME_REFRESH ) ) {
			delegate.ilmvd_refreshInternetLobbyList(this) ;
			didAction = true ;
		} else if ( name.equals( ACTION_NAME_UNHIDE ) ) {
			delegate.ilmvd_unhideInternetLobbies(this) ;
			didAction = true ;
		} else if ( name.equals( ACTION_NAME_HELP ) ) {
			delegate.ilmvd_help(this) ;
			didAction = true ;
		} else if ( name.equals( ACTION_NAME_SETTINGS ) ) {
			delegate.ilmvd_settings(this) ;
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
			int buttonNum, int buttonType, int lobbyType, WiFiLobbyDetails wifiLobbyDetails, Lobby lobby ) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null )
			 return false ;
		
		// no lobby - no effect
		if ( lobby == null || !(lobby instanceof InternetLobby) )
			return false ;
		
		// our action depends on the button type,  Single-click on CREATE
		// will create, HIDE will hide, MAIN_OPEN will join.  No effect
		// on MAIN_FULL - long-press only for those.
		switch( buttonType ) {
		case LobbyButtonStrip.BUTTON_TYPE_NO_LOBBY:
			delegate.ilmvd_createNewInternetLobby(this) ;
			if ( mSoundControls && mSoundPool != null )
				mSoundPool.menuButtonClick() ;
			return true ;
			
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_OPEN_MAIN:
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_FULL_MAIN:
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_CLOSED_MAIN:
			delegate.ilmvd_joinInternetLobby((InternetLobby)lobby, this) ;
			if ( mSoundControls && mSoundPool != null )
				mSoundPool.menuButtonClick() ;
			return true ;
			
		case LobbyButtonStrip.BUTTON_TYPE_OPEN_HIDE:
		case LobbyButtonStrip.BUTTON_TYPE_FULL_HIDE:
		case LobbyButtonStrip.BUTTON_TYPE_CLOSED_HIDE:
			if ( lobby != null ) {
				delegate.ilmvd_hideInternetLobby((InternetLobby)lobby, this) ;
				if ( mSoundControls && mSoundPool != null )
					mSoundPool.menuButtonClick() ;
				return true ;
			}
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
			int buttonNum, int buttonType, int lobbyType, WiFiLobbyDetails wifiLobbyDetails, Lobby lobby ) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null )
			return false ;
		// no lobby - no effect
		if ( lobby == null || !(lobby instanceof InternetLobby) )
			return false ;
		 
		// our action depends on the button type.  A main lobby button
		// will be examined. A hide lobby button will get a hide menu.
		switch( buttonType ) {
		// Fill in examinable cases.
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_OPEN_MAIN:
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_FULL_MAIN:
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_CLOSED_MAIN:
			delegate.ilmvd_examineInternetLobby((InternetLobby)lobby, this) ;
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
			int buttonNum, int buttonType, int lobbyType, WiFiLobbyDetails wifiLobbyDetails, Lobby lobby ) {

		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null )
			return false ;
		// no lobby - no effect
		if ( lobby == null || !(lobby instanceof InternetLobby) )
			return false ;
		 
		// our action depends on the button type.  A main lobby button
		// will be examined. A hide lobby button will get a hide menu.
		switch( buttonType ) {
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_OPEN_MAIN:
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_FULL_MAIN:
		case LobbyButtonStrip.BUTTON_TYPE_LOBBY_CLOSED_MAIN:
			return true ;
		}
		
		return false ;
	}
	

	@Override
	public boolean lbc_join(LobbyButtonCollage collage, Lobby lobby,
			WiFiLobbyDetails wifiLobbyDetails, int lobbyType) {
		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null )
			 return false ;
		
		// no lobby - no effect
		if ( lobby == null || !(lobby instanceof InternetLobby) )
			return false ;
		
		delegate.ilmvd_joinInternetLobby((InternetLobby)lobby, this) ;
		return true ;
	}

	@Override
	public boolean lbc_examine(LobbyButtonCollage collage, Lobby lobby,
			WiFiLobbyDetails wifiLobbyDetails, int lobbyType) {
		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null )
			 return false ;
		
		// no lobby - no effect
		if ( lobby == null || !(lobby instanceof InternetLobby) )
			return false ;
		
		delegate.ilmvd_examineInternetLobby((InternetLobby)lobby, this) ;
		return true ;
	}

	@Override
	public boolean lbc_hide(LobbyButtonCollage collage, Lobby lobby,
			WiFiLobbyDetails wifiLobbyDetails, int lobbyType) {
		Delegate delegate = mwrDelegate.get() ;
		
		// no delegate = no effect
		if ( delegate == null )
			 return false ;
		
		// no lobby - no effect
		if ( lobby == null || !(lobby instanceof InternetLobby) )
			return false ;
		
		delegate.ilmvd_hideInternetLobby((InternetLobby)lobby, this) ;
		return true ;
	}
	
	
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// ONCLICK LISTENER TO REFRESH /////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	

	@Override
	public void onClick(View v) {
		Delegate delegate = mwrDelegate.get() ;
		if ( v == mInstructionsTextView && this.mServerResponse == SERVER_RESPONSE_NONE ) {
			// refresh
			if ( delegate != null )
				delegate.ilmvd_refreshInternetLobbyList(this) ;
		}
	}
}
