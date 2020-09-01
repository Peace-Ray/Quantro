package com.peaceray.quantro.view.options;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.peaceray.quantro.R;
import com.peaceray.quantro.content.Music;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.view.button.collage.MusicButtonCollage;
import com.peaceray.quantro.view.button.strip.CustomButtonStrip;
import com.peaceray.quantro.view.generic.LabeledSeekBarAdapter;
import com.peaceray.quantro.view.generic.PinnedHeaderListView;
import com.velosmobile.utils.SectionableAdapter;


/**
 * An OptionsSoundMusicView that uses sliders for volume and buttons (in
 * a ListView) for music tracks.
 * 
 * @author Jake
 *
 */
public class OptionsSoundMusicButtonView extends OptionsView implements
		OptionsSoundMusicView,
		CustomButtonStrip.Delegate,
		LabeledSeekBarAdapter.OnSeekBarChangeListener, com.peaceray.quantro.view.button.collage.MusicButtonCollage.Delegate {
	
	private static final String TAG = "OptionsSoundMusicButtonView" ;
	
	
	Delegate mDelegate ;
	
	
	// ACTION BAR BUTTONS
	private String ACTION_NAME_MUTE_ON ;
	private String ACTION_NAME_MUTE_OFF ;
	private String ACTION_NAME_SETTINGS ;
	
	// View references
	private View mContentView ;
	private CustomButtonStrip mActionBar ;
	private LabeledSeekBarAdapter mLabeledSeekBarVolumeMusic ;
	private LabeledSeekBarAdapter mLabeledSeekBarVolumeSound ;
	private TextView mTextViewMutedAlert ;
	
	// Music list view
	private PinnedHeaderListView mMusicListView ;
	private MusicButtonListAdapter mMusicButtonListAdapter ;
	private ArrayList<Music> mMusic ;
	private Hashtable<Music, OptionAvailability> mAvailability ;
	
	// Strings for placing in these
	private String mMutedByRingerAlertString ;
	private String mMutedByUserAlertString ;
	
	// Current audio settings.
	private boolean mSoundOn ;
	private boolean mSoundMutedByRinger ;
	private int mSoundVolPercent ;
	private int mMusicVolPercent ;
	private Music mCurrentMusic ;
	
	
	public OptionsSoundMusicButtonView() {
		mMusic = new ArrayList<Music>() ;
		mAvailability = new Hashtable<Music, OptionAvailability>() ;
	}

	@Override
	public void setDelegate(Delegate delegate) {
		mDelegate = delegate ;
	}

	@Override
	public void clearMusic() {
		mMusic.clear() ;
		mAvailability.clear() ;
	}

	@Override
	public void addMusic(Music music, OptionAvailability availability) {
		if ( mMusic.contains(music) )
			setMusicAvailability(music, availability) ;
		else {
			insertInOrder( mMusic, music ) ;
			mAvailability.put(music, availability) ;
		}
	}

	@Override
	public void setMusics(Collection<Music> musics,
			Map<Music, OptionAvailability> availability) {
		
		clearMusic() ;
		Iterator<Music> iter = musics.iterator() ;
		for ( ; iter.hasNext() ; ) {
			Music m = iter.next() ;
			addMusic( m, availability.get(m) ) ;
		}
	}

	@Override
	public void setMusicAvailability(Music music,
			OptionAvailability availability) {
		
		if ( !mMusic.contains(music) )
			throw new IllegalArgumentException("Music " + music + " not contained in our records.") ;
		mAvailability.put(music, availability) ;
	}

	@Override
	public void setMusicAvailability(Map<Music, OptionAvailability> availability) {
		
		Set<Entry<Music, OptionAvailability>> set = availability.entrySet() ;
		Iterator<Entry<Music, OptionAvailability>> iterator = set.iterator() ;
		for ( ; iterator.hasNext() ; ) {
			Entry<Music, OptionAvailability> entry = iterator.next() ;
			if ( !mMusic.contains(entry.getKey()) )
				throw new IllegalArgumentException("Music " + entry.getKey() + " not contained in our records.") ;
			mAvailability.put( entry.getKey(), entry.getValue() ) ;
		}
	}

	@Override
	public void setCurrentMusic(Music music) {
		Music prev = mCurrentMusic ;
		mCurrentMusic = music ;
		if ( mMusicListView != null && mMusicButtonListAdapter != null ) {
			mMusicButtonListAdapter.refreshView( prev ) ;
			mMusicButtonListAdapter.refreshView( music ) ;
		}
	}

	@Override
	public void setSoundOn(boolean on, boolean isMutedByRinger) {
		mSoundOn = on ;
		mSoundMutedByRinger = isMutedByRinger ;
	}

	@Override
	public void setSoundVolumePercent(int vol) {
		mSoundVolPercent = vol ;
	}

	@Override
	public void setMusicVolumePercent(int vol) {
		mMusicVolPercent = vol ;
	}

	@Override
	public void init(Activity activity, View root) {
		Resources res = activity.getResources() ;
		
		// button names
		ACTION_NAME_SETTINGS = res.getString(R.string.action_strip_name_settings) ;
		ACTION_NAME_MUTE_ON = res.getString(R.string.action_strip_name_mute_on) ;
		ACTION_NAME_MUTE_OFF = res.getString(R.string.action_strip_name_mute_off) ;
		
		mContentView = root ;
		
		// get reference to the action bar
		mActionBar = (CustomButtonStrip)mContentView.findViewById(R.id.game_options_music_sound_action_strip) ;
		mActionBar.setDelegate(this) ;
		mActionBar.setAutoRefresh(false) ;
		mActionBar.setEnabled( mActionBar.getButton(ACTION_NAME_SETTINGS), true ) ;
		mActionBar.setEnabled( mActionBar.getButton(ACTION_NAME_MUTE_ON), true ) ;
		mActionBar.setEnabled( mActionBar.getButton(ACTION_NAME_MUTE_OFF), true ) ;
		mActionBar.refresh() ;
		
		// Note: our music / sound volume sliders, and the mute alert
		// that sits on top of them, is no longer present in our
		// root view.  Instead, we inflate it as a header for our list.
		// That means we need the list first...
		
		// Reference to the a list view?
		this.mMusicListView = (PinnedHeaderListView)mContentView.findViewById(R.id.game_options_music_sound_track_list) ;
		if ( mMusicListView != null ) {
			// configure the header view...
			View volumeContainer = (View)activity.getLayoutInflater().inflate(R.layout.game_options_sound_music_volume_panel, null) ;
			mMusicListView.addHeaderView(volumeContainer) ;
			
			// Music / Sound volume sliders
			View v = volumeContainer.findViewById(R.id.game_options_music_sound_labeled_seek_bar_music_volume) ;
			mLabeledSeekBarVolumeMusic = v == null ? null : new LabeledSeekBarAdapter( activity, v ) ;
			v = volumeContainer.findViewById(R.id.game_options_music_sound_labeled_seek_bar_sound_volume) ;
			mLabeledSeekBarVolumeSound = v == null ? null : new LabeledSeekBarAdapter( activity, v ) ;
			// configure these sliders...
			
			String unitsLeft = res.getString(R.string.game_options_music_sound_volume_slider_units_left) ;
			String unitsRight = res.getString(R.string.game_options_music_sound_volume_slider_units_right) ;
			
			if ( mLabeledSeekBarVolumeMusic != null ) {
				String label = res.getString(R.string.game_options_music_sound_volume_slider_music_label) ;
				mLabeledSeekBarVolumeMusic.setRangeValues(
						0, 200, 1, label, unitsLeft, unitsRight) ;
				mLabeledSeekBarVolumeMusic.setListener(this) ;
				mLabeledSeekBarVolumeMusic.setShow(true) ;
				mLabeledSeekBarVolumeMusic.setShowMinMax(false) ;
			}
			if ( mLabeledSeekBarVolumeSound != null ) {
				String label = res.getString(R.string.game_options_music_sound_volume_slider_sound_label) ;
				mLabeledSeekBarVolumeSound.setRangeValues(
						0, 200, 1, label, unitsLeft, unitsRight) ;
				mLabeledSeekBarVolumeSound.setListener(this) ;
				mLabeledSeekBarVolumeSound.setShow(true) ;
				mLabeledSeekBarVolumeSound.setShowMinMax(false) ;
			}
			
			// Muted alert text
			this.mTextViewMutedAlert = (TextView)volumeContainer.findViewById(R.id.game_options_music_sound_description_muted_only) ;
			// and the text we'll put there...
			mMutedByRingerAlertString = res.getString(R.string.game_options_music_sound_muted_by_ringer_alert) ;
			mMutedByUserAlertString = res.getString(R.string.game_options_music_sound_muted_by_user_alert) ;
			
			
			
			// configure the list view...
			int layoutID = R.layout.button_collage_music_list_item ;
			this.mMusicButtonListAdapter = new MusicButtonListAdapter(
					activity.getLayoutInflater(),
					layoutID,
					0,
					R.id.music_list_row) ;
			mMusicListView.setAdapter(mMusicButtonListAdapter) ;
			mMusicListView.setOnScrollListener(mMusicButtonListAdapter) ;
			
			if ( mMusicListView instanceof PinnedHeaderListView ) {
    			View pinnedHeaderView = activity.getLayoutInflater().inflate(R.layout.quantro_list_item_header, mMusicListView, false) ;
    			pinnedHeaderView.setTag( new MusicButtonRowTag(pinnedHeaderView ) ) ;
    			((PinnedHeaderListView)mMusicListView).setPinnedHeaderView( pinnedHeaderView ) ;
    			mMusicListView.setDivider(null) ;
    			mMusicListView.setDividerHeight(0);
    		}
		}
	}

	@Override
	public void refresh() {
		// mute / unmute buttons visible based on whether sound is currently on.
		boolean soundOn = mSoundOn && !mSoundMutedByRinger ;
		mActionBar.setVisible( mActionBar.getButton(ACTION_NAME_MUTE_ON), soundOn ) ;
		mActionBar.setVisible( mActionBar.getButton(ACTION_NAME_MUTE_OFF), !soundOn ) ;
		
		mActionBar.refresh() ;
		
		// Set the current sound / music volumes.
		if ( mLabeledSeekBarVolumeMusic != null ) {
			mLabeledSeekBarVolumeMusic.setIntValue(mMusicVolPercent) ;
			// set enabled according to currently playing sound
			mLabeledSeekBarVolumeMusic.setEnabled(soundOn) ;
		}
		if ( mLabeledSeekBarVolumeSound != null ) {
			mLabeledSeekBarVolumeSound.setIntValue(mSoundVolPercent) ;
			// set enabled according to currently playing sound
			mLabeledSeekBarVolumeSound.setEnabled(soundOn) ;
		}
		
		// Set muted alert
		if ( mTextViewMutedAlert != null ) {
			mTextViewMutedAlert.setVisibility( soundOn ? View.INVISIBLE : View.VISIBLE ) ;
			if ( mSoundOn && mSoundMutedByRinger )
				mTextViewMutedAlert.setText(mMutedByRingerAlertString) ;
			else
				mTextViewMutedAlert.setText(mMutedByUserAlertString) ;
		}

		// refresh the adapter
		if ( mMusicListView != null && mMusicButtonListAdapter != null ) {
			mMusicButtonListAdapter.set( mMusic, mAvailability ) ;
			mMusicButtonListAdapter.notifyDataSetChanged() ;
			mMusicListView.invalidateViews() ;
		}
	}
	
	

	@Override
	protected void relaxCache(boolean isRelaxed) {
		// no effect; we don't cache images
	}

	@Override
	protected void refreshCache(boolean isRelaxed) {
		// no effect; we don't cache images
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// CUSTOM BUTTON STRIP DELEGATE METHODS (for ActionBar)
	//

	@Override
	public boolean customButtonStrip_onButtonClick(CustomButtonStrip strip,
			int buttonNum, String name, boolean asOverflow ) {
		
		boolean performAction = false ;

		if ( mDelegate != null ) {
			if ( ACTION_NAME_MUTE_ON.equals(name) ) {
				performAction = true ;
				mDelegate.osmv_userSetSoundOn(this, false) ;
			} 
			
			if ( ACTION_NAME_MUTE_OFF.equals(name) ) {
				performAction = true ;
				mDelegate.osmv_userSetSoundOn(this, true) ;
			}
			
			if ( ACTION_NAME_SETTINGS.equals(name) ) {
				performAction = true ;
				mDelegate.osmv_userAdvancedConfiguration(this) ;
			}
		}
		
		// play sound.  We play this after the effect, because we may
		// have changed our mute/ unmute status.
		if ( performAction && mSoundControls && mSoundPool != null && !asOverflow )
			mSoundPool.menuButtonClick() ;
		
		return performAction ;
	}

	@Override
	public boolean customButtonStrip_onButtonLongClick(CustomButtonStrip strip,
			int buttonNum, String name) {
		// No long-press.
		return false;
	}

	@Override
	public boolean customButtonStrip_supportsLongClick(CustomButtonStrip strip,
			int buttonNum, String name) {
		// No long-press.
		return false;
	}
	
	@Override
	public void customButtonStrip_onPopupOpen(
			CustomButtonStrip strip ) {
		
		if ( mDelegate == null )
			return ;
		
		if ( mSoundPool != null && mSoundControls )
			mSoundPool.menuButtonClick() ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	////////////////////////////////////////////////////////////////////////////
	//
	// LABELED SEEK BAR METHODS (for volume)
	//

	@Override
	public void onProgressChanged(LabeledSeekBarAdapter lsba, SeekBar seekBar,
			int progress, boolean fromUser, int value) {
		// ignore.  We wait until the user lets go.
	}

	@Override
	public void onProgressChanged(LabeledSeekBarAdapter lsba, SeekBar seekBar,
			int progress, boolean fromUser, float value) {
		// ignore.  We wait until the user lets go.
	}

	@Override
	public void onProgressChanged(LabeledSeekBarAdapter lsba, SeekBar seekBar,
			int progress, boolean fromUser, Object value) {
		// ignore.  We wait until the user lets go.
	}

	@Override
	public void onStartTrackingTouch(LabeledSeekBarAdapter lsba, SeekBar seekBar) {
		// ignore.  We don't care.
	}

	@Override
	public void onStopTrackingTouch(LabeledSeekBarAdapter lsba, SeekBar seekBar) {
		boolean update = false ;
		// tell the delegate!
		int volPercent = lsba.intValue() ;
		if ( lsba == mLabeledSeekBarVolumeMusic ) {
			Log.d(TAG, "userSetMusicVolumePercent to " + volPercent) ;
			update = mDelegate.osmv_userSetMusicVolumePercent(this, volPercent) ;
		}
		else if ( lsba == mLabeledSeekBarVolumeSound ) {
			Log.d(TAG, "userSetSoundVolumePercent to " + volPercent) ;
			update = mDelegate.osmv_userSetSoundVolumePercent(this, volPercent) ;
		}
		
		if ( update ) {
			if ( lsba == mLabeledSeekBarVolumeMusic )
				mMusicVolPercent = volPercent ;
			if ( lsba == mLabeledSeekBarVolumeSound )
				mSoundVolPercent = volPercent ;
		}
	}
	
	//
	////////////////////////////////////////////////////////////////////////////

	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// MANAGING MUSIC COLLECTIONS AND DISPLAYED LISTS
	//
	
	/**
	 * Returns the appropriate insertion position for the provided Music object
	 * into the provided list.
	 * 
	 * We sort our tracks in a particular order: ordinal by Setting, then within
	 * setting, ordinal by Track.
	 * 
	 */
	private int insertionPosition( ArrayList<Music> list, Music m ) {
		
		int s = m.getSetting().ordinal() ;
		int t = m.getTrack().ordinal() ;
		
		// find the object which should be immediately after 'm',
		// and return its location.  If none, return size.
		for ( int i = 0; i < list.size(); i++ ) {
			Music mHere = list.get(i) ;
			int sHere = mHere.getSetting().ordinal() ;
			int tHere = mHere.getTrack().ordinal() ;
			
			if ( sHere > s )
				return i ;
			
			if ( sHere == s && tHere > t )
				return i ;
		}
		
		return list.size() ;
	}
	
	private void insertInOrder( ArrayList<Music> list, Music m ) {
		list.add( insertionPosition(list, m), m ) ;
	}
	
	
	private static class MusicButtonCellTag {
		MusicButtonCollage mMBC ;
		
		public MusicButtonCellTag( View v ) {
			mMBC = (MusicButtonCollage) v.findViewById( R.id.button_collage_music ) ;
		}
	}
	
	
	private static class MusicButtonRowTag {
		View mHeaderView ;
		View mHeaderViewTopSpacer ;
		TextView mHeaderTextView ;
		
		public MusicButtonRowTag( View v ) {
			mHeaderView = v.findViewById(R.id.quantro_list_item_header) ;
			mHeaderViewTopSpacer = v.findViewById(R.id.quantro_list_item_header_top_spacer) ;
			mHeaderTextView = (TextView)v.findViewById(R.id.quantro_list_item_header_text) ;
		}
	}
	
	
	private class MusicButtonListAdapter extends SectionableAdapter
			implements OnScrollListener, PinnedHeaderListView.PinnedHeaderAdapter, SectionIndexer {
		
		
		@SuppressWarnings("unchecked")
		public void set( ArrayList<Music> m, Hashtable<Music, OptionAvailability> a ) {
			musics = (ArrayList<Music>)m.clone() ;
			availabilities = (Hashtable<Music,  OptionAvailability>)a.clone() ;
			
			Music [] array = new Music[musics.size()] ;
			array = musics.toArray(array) ;
			mIndexer.setMusics(array) ;
			
			mMusicCellTag.clear() ;
		}
		
		ArrayList<Music> musics = new ArrayList<Music>() ;
    	Hashtable<Music, OptionAvailability> availabilities = new Hashtable<Music, OptionAvailability>() ;
    	private MusicIndexer mIndexer ;
    	
    	boolean mScrolledPastFirstListItem ;
    	
    	private Hashtable<Music, MusicButtonCellTag> mMusicCellTag ;
    	
    	public MusicButtonListAdapter(LayoutInflater inflater,
				int rowLayoutID, int headerID, int itemHolderID) {
			
			super(inflater, rowLayoutID, headerID, itemHolderID);
			
			mIndexer = new MusicIndexer(new Music[0]) ;
			mScrolledPastFirstListItem = false ;
			
			mMusicCellTag = new Hashtable<Music, MusicButtonCellTag>() ;
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
			int numHeaderViews = 0 ;
			if ( view instanceof ListView )
				numHeaderViews = ((ListView)view).getHeaderViewsCount() ;
			
			View topChild = view.getChildAt(0) ;
			MusicButtonRowTag tag = null ;
			if ( topChild != null )
				tag = ((MusicButtonRowTag)topChild.getTag()) ;
			
			mScrolledPastFirstListItem = firstVisibleItem >= numHeaderViews && visibleItemCount > 0 && topChild.getTop() < 0 ;
			boolean topHasHeader = mIndexer.isFirstInSection( getRealPosition(firstVisibleItem - numHeaderViews) ) ;
			
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
				boolean headerNotYetInPosition = ( tag != null ) && topHasHeader && firstVisibleItem != numHeaderViews
						&& ( topChild.getTop() + tag.mHeaderTextView.getTop() > 0 ) ;
				
                ((PinnedHeaderListView) view).configureHeaderView(
                		headerNotYetInPosition
	                		? firstVisibleItem -1 -numHeaderViews
	                		: firstVisibleItem -numHeaderViews,
                		!headerNotYetInPosition );
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
            
            if ( !mScrolledPastFirstListItem )
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
			MusicButtonRowTag tag = (MusicButtonRowTag)header.getTag() ;
        	Drawable bg = tag.mHeaderTextView.getBackground() ;
        	bg.getPadding(tempPaddingRect) ;
        	r.set(0, tag.mHeaderTextView.getTop() + tempPaddingRect.top,
        			header.getWidth(), tag.mHeaderTextView.getBottom() - tempPaddingRect.bottom) ;
        	return true ;
        }
        
        @Override
        public boolean copyPinnedHeaderVisibleBounds(View header, int position, Rect r) {
        	MusicButtonRowTag tag = (MusicButtonRowTag)header.getTag() ;
        	r.set(0, tag.mHeaderTextView.getTop(),
        			header.getWidth(), tag.mHeaderTextView.getBottom()) ;
        	return true ;
        }
        
        
        @Override
        public boolean copyHeaderOccludingBounds(View view, int position, Rect r) {
        	MusicButtonRowTag tag = (MusicButtonRowTag)view.getTag() ;
        	if ( tag.mHeaderView.getVisibility() == View.GONE ) {
        		r.set(0, 0, 0, 0) ;
        		return true ;
        	}
        	
        	return copyPinnedHeaderOccludingBounds( tag.mHeaderView, position, r ) ;
        }
        
        @Override
        public boolean copyHeaderVisibleBounds(View view, int position, Rect r) {
        	MusicButtonRowTag tag = (MusicButtonRowTag)view.getTag() ;
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
			
			MusicButtonRowTag tag = (MusicButtonRowTag)v.getTag() ;
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
			return musics.get(position) ;
		}


		@Override
		protected int getDataCount() {
			return musics.size() ;
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
			return MusicIndexer.NUM_SECTIONS ;
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
			Music music = musics.get(position) ;
			
			MusicButtonCellTag tag = (MusicButtonCellTag)cell.getTag() ;
    		if ( tag == null ) {
				tag = new MusicButtonCellTag(cell) ;
				if ( tag.mMBC != null ) {
					tag.mMBC.setDelegate(OptionsSoundMusicButtonView.this) ;
					tag.mMBC.setColorScheme(mColorScheme) ;
					tag.mMBC.setSoundPool(mSoundPool) ;
				}
				cell.setTag(tag) ;
			}
			// Set game mode, set the height to the ideal height, and refresh.
    		tag.mMBC.setMusic(music, availabilities.get(music), music.equals(mCurrentMusic)) ;
			
			mMusicCellTag.put(music, tag) ;
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
			
			MusicButtonRowTag tag = ((MusicButtonRowTag)rowView.getTag()) ;
			if ( tag == null ) {
				tag = new MusicButtonRowTag( rowView ) ;
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
		
		
		protected void refreshView( Music m ) {
			if ( m == null )
				return ;
			MusicButtonCellTag tag = mMusicCellTag.get(m) ;
			if ( tag != null ) {
				MusicButtonCollage mbc = tag.mMBC ;
				Music viewMusic = mbc.getMusic() ;
				if ( m.equals(viewMusic) ) {
					mbc.setMusic(m, availabilities.get(m), m.equals( mCurrentMusic )) ;
					mbc.refresh() ;
				}
			}
		}
    	
	}
	
	
	private class MusicIndexer implements SectionIndexer {
		
		// For now, we index all music together in a single
		// section labeled "music."  With more music we should
		// probably change the sectioning?
		protected static final int SECTION_MUSIC = 0 ;
		public static final int NUM_SECTIONS = 1 ;
		
		protected String [] mSections = new String[]{ "Music" } ;
		protected Music [] mMusics ;
		
		public MusicIndexer( Music [] musics ) {
			mMusics = musics ;
		}
		
		synchronized public void setMusics( Music [] musics ) {
			mMusics = musics ;
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
	    	
			// MUSIC: 
			// Only one section, so it's always section 0.
	    	if ( sectionIndex == SECTION_MUSIC )
	    		return 0 ;
	    	
	    	// huh?
	    	return mMusics.length ;
	    }
	    
	    /**
	     * Returns the section index for a given position in the list by
	     * examining the item.
	     */
		synchronized public int getSectionForPosition(int position) {
			if ( position < 0 || position >= mMusics.length )
				return -1 ;
			
			// always section 0.
	    	return SECTION_MUSIC ;
	    }
		
		synchronized public boolean isFirstInSection( int position ) {
			if ( position < 0 || position >= mMusics.length )
				return false ;
			if ( position == 0 )
				return true ;

			// Only one section, so if position is not 0, it's not the first.
			return false ;
		}
	}

	
	////////////////////////////////////////////////////////////////////////////
	// BUTTON DELEGATE CALLBACK

	@Override
	public boolean mbcd_playMusic(MusicButtonCollage mbc, Music music,
			OptionAvailability availability, boolean setAsDefault) {
		
		boolean performedAction = false ;
		if ( mDelegate != null ) {
			mDelegate.osmv_userSetCurrentMusic(this, music, availability) ;
			if ( setAsDefault )
				mDelegate.osmv_userSetDefaultMusic(this, music, availability) ;
			performedAction = true ;
		}
		
		return performedAction ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
}
