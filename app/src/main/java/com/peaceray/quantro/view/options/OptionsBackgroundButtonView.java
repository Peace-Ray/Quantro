package com.peaceray.quantro.view.options;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.peaceray.android.graphics.BitmapSoftCache;
import com.peaceray.quantro.R;
import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.content.Background.Template;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.utils.threadedloader.ThreadedBackgroundThumbnailLoader;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.button.collage.BackgroundButtonCollage;
import com.peaceray.quantro.view.button.strip.CustomButtonStrip;
import com.peaceray.quantro.view.generic.PinnedHeaderListView;
import com.peaceray.quantro.view.lobby.WifiLobbyManagerView.Delegate;
import com.velosmobile.utils.SectionableAdapter;

public class OptionsBackgroundButtonView extends OptionsView implements
		OptionsBackgroundView, com.peaceray.quantro.view.button.strip.CustomButtonStrip.Delegate, com.peaceray.quantro.view.button.collage.BackgroundButtonCollage.Delegate {
	
	// ACTION BAR BUTTONS
	private String ACTION_NAME_SETTINGS ;
	private String ACTION_NAME_SHUFFLE ;
	
	
	// our delegate!
	private Delegate mDelegate ;
	private View mContentView ;
	private CustomButtonStrip mActionBar ;
	
	// Background list view
	private PinnedHeaderListView mBackgroundListView ;
	private BackgroundButtonListAdapter mBackgroundButtonListAdapter ;
	private ArrayList<Background> mBackgrounds ;
	private Hashtable<Background, OptionAvailability> mBGAvailability ;
	
	// Current settings
	private boolean mShuffleSupported ;
	private boolean mShuffling ;
	private Background mCurrentBackground ;
	private Hashtable<Background, Boolean> mBGShuffled ;
	
	// Loading Thumbnails!
	private BitmapSoftCache mThumbnailSoftCache ;
	private ThreadedBackgroundThumbnailLoader mThreadedBackgroundLoader ;

	public OptionsBackgroundButtonView() {
		mBackgrounds = new ArrayList<Background>() ;
		mBGAvailability = new Hashtable<Background, OptionAvailability>() ;
		mBGShuffled = new Hashtable<Background, Boolean>() ;
	}
	
	@Override
	public void setDelegate(Delegate delegate) {
		mDelegate = delegate ;
	}

	@Override
	public void clearBackgrounds() {
		mBackgrounds.clear() ;
		mBGAvailability.clear() ;
		mBGShuffled.clear() ;
	}

	@Override
	public void addBackground(Background background,
			OptionAvailability availability) {
		if ( mBackgrounds.contains(background) )
			setBackgroundAvailability(background, availability) ;
		else {
			insertInOrder( mBackgrounds, background ) ;
			mBGAvailability.put(background, availability) ;
		}
	}

	@Override
	public void setBackgrounds(Collection<Background> backgrounds,
			Map<Background, OptionAvailability> availability) {
		clearBackgrounds() ;
		Iterator<Background> iter = backgrounds.iterator() ;
		for ( ; iter.hasNext() ; ) {
			Background bg = iter.next() ;
			addBackground( bg, availability.get(bg) ) ;
		}
	}

	@Override
	public void setBackgroundAvailability(Background background,
			OptionAvailability availability) {
		if ( !mBackgrounds.contains(background) )
			throw new IllegalArgumentException("Background " + background + " not contained in our records.") ;
		mBGAvailability.put(background, availability) ;
	}

	@Override
	public void setBackgroundAvailability(
			Map<Background, OptionAvailability> availability) {
		Set<Entry<Background, OptionAvailability>> set = availability.entrySet() ;
		Iterator<Entry<Background, OptionAvailability>> iterator = set.iterator() ;
		for ( ; iterator.hasNext() ; ) {
			Entry<Background, OptionAvailability> entry = iterator.next() ;
			if ( !mBackgrounds.contains(entry.getKey()) )
				throw new IllegalArgumentException("Background " + entry.getKey() + " not contained in our records.") ;
			mBGAvailability.put( entry.getKey(), entry.getValue() ) ;
		}
	}
	
	@Override
	public void setBackgroundShuffleSupported( boolean supported ) {
		mShuffleSupported = supported ;
	}

	@Override
	public void setBackgroundShuffling(boolean shuffling) {
		mShuffling = shuffling ;
	}

	@Override
	public void setCurrentBackground(Background background) {
		Background prev = mCurrentBackground ;
		mCurrentBackground = background ;
		if ( mBackgroundListView != null && mBackgroundButtonListAdapter != null ) {
			mBackgroundButtonListAdapter.refreshView( prev ) ;
			mBackgroundButtonListAdapter.refreshView( background ) ;
		}
	}

	@Override
	public void setBackgroundShuffled(Background background, boolean inRotation) {
		mBGShuffled.put(background, Boolean.valueOf(inRotation)) ;
		if ( mBackgroundButtonListAdapter != null ) {
			mBackgroundButtonListAdapter.refreshView(background) ;
		}
	}

	@Override
	public void setBackgroundsShuffled(Collection<Background> backgrounds) {
		// the provided backgrounds are EXACTLY those which are shuffled.
		// All others have shuffle status set to FALSE.  Fill with FALSEs
		// and then set those given to TRUE instead.
		mBGShuffled.clear() ; 
		for ( Background bg: mBackgrounds ) {
			mBGShuffled.put(bg, Boolean.FALSE) ;
		}
		for ( Background bg: backgrounds ) {
			mBGShuffled.put(bg, Boolean.TRUE) ;
		}
		for ( Background bg: backgrounds ) {
			mBackgroundButtonListAdapter.refreshView(bg) ;
		}
	}

	@Override
	public void init(Activity activity, View root) {
		mThumbnailSoftCache = new BitmapSoftCache() ;
		mThreadedBackgroundLoader = new ThreadedBackgroundThumbnailLoader( activity.getAssets() ) ;
		
		Resources res = activity.getResources() ;
		
		// Action names
		ACTION_NAME_SETTINGS = res.getString(R.string.action_strip_name_settings) ;
		ACTION_NAME_SHUFFLE = res.getString(R.string.action_strip_name_shuffle) ;
		
		mContentView = root ;
		
		// get reference to the action bar
		mActionBar = (CustomButtonStrip)mContentView.findViewById(R.id.game_options_background_action_strip) ;
		mActionBar.setDelegate(this) ;
		mActionBar.setAutoRefresh(false) ;
		mActionBar.setEnabled( mActionBar.getButton(ACTION_NAME_SETTINGS), true ) ;
		mActionBar.setEnabled( mActionBar.getButton(ACTION_NAME_SHUFFLE), mShuffleSupported ) ;
		mActionBar.setVisible( mActionBar.getButton(ACTION_NAME_SHUFFLE), mShuffleSupported ) ;
		mActionBar.refresh() ;
		
		// Set up the background list.
		// Reference to the a list view?
		this.mBackgroundListView = (PinnedHeaderListView)mContentView.findViewById(R.id.game_options_background_list) ;
		if ( mBackgroundListView != null ) {
			// configure!
			int layoutID = R.layout.button_collage_background_list_item ;
			this.mBackgroundButtonListAdapter = new BackgroundButtonListAdapter(
					activity.getLayoutInflater(),
					layoutID,
					0,
					R.id.background_list_row) ;
			mBackgroundListView.setAdapter(mBackgroundButtonListAdapter) ;
			mBackgroundListView.setOnScrollListener(mBackgroundButtonListAdapter) ;
			
			if ( mBackgroundListView instanceof PinnedHeaderListView ) {
    			View pinnedHeaderView = activity.getLayoutInflater().inflate(R.layout.quantro_list_item_header, mBackgroundListView, false) ;
    			pinnedHeaderView.setTag( new BackgroundButtonRowTag(pinnedHeaderView ) ) ;
    			((PinnedHeaderListView)mBackgroundListView).setPinnedHeaderView( pinnedHeaderView ) ;
    			mBackgroundListView.setDivider(null) ;
    			mBackgroundListView.setDividerHeight(0);
    		}
		}
	}

	@Override
	public void refresh() {
		// Set the shuffle glow according to whether shuffled or not.
		mActionBar.getButtonAccess( mActionBar.getButton(ACTION_NAME_SHUFFLE) ).setContentState(
				mShuffling
				? QuantroContentWrappingButton.ContentState.EXCLUSIVE
				: QuantroContentWrappingButton.ContentState.OPTIONAL) ;
		mActionBar.refresh() ;
		
		// refresh the adapter
		if ( mBackgroundListView != null && mBackgroundButtonListAdapter != null ) {
			mBackgroundButtonListAdapter.set( mBackgrounds, mBGAvailability ) ;
			mBackgroundButtonListAdapter.notifyDataSetChanged() ;
			mBackgroundListView.invalidateViews() ;
		}
	}
	
	
	@Override
	protected void relaxCache(boolean isRelaxed) {
		if ( !isRelaxed ) {
			// empty our thumbnail cache.  This cache is
			// populated by individual BackgroundButtonCollages
			// as-needed.  we should therefore first clear the
			// list, and then clear the cache itself.
			mBackgroundButtonListAdapter.set( null, null ) ;
			mBackgroundButtonListAdapter.notifyDataSetChanged() ;
			mThumbnailSoftCache.clear() ;
		}
	}

	@Override
	protected void refreshCache(boolean isRelaxed) {
		if ( isRelaxed ) {
			// our thumbnail cache is empty, and we have cleared
			// the list adapter of Backgrounds.  We DON'T need
			// to manually repopulate the cache, since the list
			// items do that in the background; instead, just set
			// our current backgrounds as the content of that list.
			mBackgroundButtonListAdapter.set( mBackgrounds, mBGAvailability ) ;
			mBackgroundButtonListAdapter.notifyDataSetChanged() ;
			mBackgroundListView.invalidateViews() ;
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// ACTION BAR CALLBACKS
	


	@Override
	public boolean customButtonStrip_onButtonClick(CustomButtonStrip strip,
			int buttonNum, String name, boolean asOverflow ) {
		
		boolean performAction = false ;

		if ( mDelegate != null ) {
			if ( ACTION_NAME_SHUFFLE.equals(name) ) {
				performAction = true ;
				// toggle state
				mDelegate.obvd_userSetBackgroundShuffling(this, !mShuffling) ;
			} 
			
			if ( ACTION_NAME_SETTINGS.equals(name) ) {
				performAction = true ;
				mDelegate.obvd_userAdvancedConfiguration(this) ;
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
		// No long-press
		return false;
	}

	@Override
	public boolean customButtonStrip_supportsLongClick(CustomButtonStrip strip,
			int buttonNum, String name) {
		// No long-press
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
	// BACKGROUND LIST
	//
	
	
	/**
	 * Returns the appropriate insertion position for the provided Background object
	 * into the provided list.
	 * 
	 * We sort our tracks in a particular order: ordinal by Template, then
	 * ordinal by Shade.
	 * 
	 */
	private int insertionPosition( ArrayList<Background> list, Background bg ) {
		
		int t = bg.getTemplate().ordinal() ;
		int s = bg.getShade().ordinal() ;
		
		// find the object which should be immediately after 'm',
		// and return its location.  If none, return size.
		for ( int i = 0; i < list.size(); i++ ) {
			Background bgHere = list.get(i) ;
			int tHere = bgHere.getTemplate().ordinal() ;
			int sHere = bgHere.getShade().ordinal() ;
			
			if ( tHere > t )
				return i ;
			
			if ( tHere == t && sHere > s )
				return i ;
		}
		
		return list.size() ;
	}
	
	private void insertInOrder( ArrayList<Background> list, Background m ) {
		list.add( insertionPosition(list, m), m ) ;
	}
	
	
	private static class BackgroundButtonCellTag {
		BackgroundButtonCollage mBBC ;
		
		public BackgroundButtonCellTag( View v ) {
			mBBC = (BackgroundButtonCollage) v.findViewById( R.id.button_collage_background ) ;
		}
	}
	
	
	private static class BackgroundButtonRowTag {
		View mHeaderView ;
		View mHeaderViewTopSpacer ;
		TextView mHeaderTextView ;
		int mSection = -1 ;
		
		public BackgroundButtonRowTag( View v ) {
			mHeaderView = v.findViewById(R.id.quantro_list_item_header) ;
			mHeaderViewTopSpacer = v.findViewById(R.id.quantro_list_item_header_top_spacer) ;
			mHeaderTextView = (TextView)v.findViewById(R.id.quantro_list_item_header_text) ;
		}
	}
	
	
	private class BackgroundButtonListAdapter extends SectionableAdapter
			implements OnScrollListener, PinnedHeaderListView.PinnedHeaderAdapter, SectionIndexer {
		
		
		@SuppressWarnings("unchecked")
		public void set( ArrayList<Background> m, Hashtable<Background, OptionAvailability> a ) {
			if ( m != null )
				backgrounds = (ArrayList<Background>)m.clone() ;
			else
				backgrounds.clear() ;
			
			if ( a != null )
				availabilities = (Hashtable<Background,  OptionAvailability>)a.clone() ;
			else
				availabilities.clear() ;
			
			Background [] array = new Background[backgrounds.size()] ;
			array = backgrounds.toArray(array) ;
			mIndexer.setBackgrounds(array) ;
			
			mBackgroundCellTag.clear() ;
		}
		
		ArrayList<Background> backgrounds = new ArrayList<Background>() ;
    	Hashtable<Background, OptionAvailability> availabilities = new Hashtable<Background, OptionAvailability>() ;
    	private BackgroundIndexer mIndexer ;
    	
    	boolean mScrolledToTop ;
    	
    	private Hashtable<Background, BackgroundButtonCellTag> mBackgroundCellTag ;
    	
    	public BackgroundButtonListAdapter(LayoutInflater inflater,
				int rowLayoutID, int headerID, int itemHolderID) {
			
			super(inflater, rowLayoutID, headerID, itemHolderID);
			
			mIndexer = new BackgroundIndexer(new Background[0]) ;
			mScrolledToTop = true ;
			
			mBackgroundCellTag = new Hashtable<Background, BackgroundButtonCellTag>() ;
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
			BackgroundButtonRowTag tag = null ;
			if ( topChild != null )
				tag = ((BackgroundButtonRowTag)topChild.getTag()) ;
			
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
			BackgroundButtonRowTag tag = (BackgroundButtonRowTag)header.getTag() ;
        	Drawable bg = tag.mHeaderTextView.getBackground() ;
        	bg.getPadding(tempPaddingRect) ;
        	r.set(0, tag.mHeaderTextView.getTop() + tempPaddingRect.top,
        			header.getWidth(), tag.mHeaderTextView.getBottom() - tempPaddingRect.bottom) ;
        	return true ;
        }
        
        @Override
        public boolean copyPinnedHeaderVisibleBounds(View header, int position, Rect r) {
        	BackgroundButtonRowTag tag = (BackgroundButtonRowTag)header.getTag() ;
        	r.set(0, tag.mHeaderTextView.getTop(),
        			header.getWidth(), tag.mHeaderTextView.getBottom()) ;
        	return true ;
        }
        
        
        @Override
        public boolean copyHeaderOccludingBounds(View view, int position, Rect r) {
        	BackgroundButtonRowTag tag = (BackgroundButtonRowTag)view.getTag() ;
        	if ( tag.mHeaderView.getVisibility() == View.GONE ) {
        		r.set(0, 0, 0, 0) ;
        		return true ;
        	}
        	
        	return copyPinnedHeaderOccludingBounds( tag.mHeaderView, position, r ) ;
        }
        
        @Override
        public boolean copyHeaderVisibleBounds(View view, int position, Rect r) {
        	BackgroundButtonRowTag tag = (BackgroundButtonRowTag)view.getTag() ;
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
			
			BackgroundButtonRowTag tag = (BackgroundButtonRowTag)v.getTag() ;
			tag.mHeaderTextView.setText(title) ;
			tag.mHeaderViewTopSpacer.setVisibility(View.GONE) ;
			tag.mSection = section ;
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
			return backgrounds.get(position) ;
		}


		@Override
		protected int getDataCount() {
			return backgrounds.size() ;
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
			return BackgroundIndexer.SECTIONS.length ;
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
			Background background = backgrounds.get(position) ;
			
			BackgroundButtonCellTag tag = (BackgroundButtonCellTag)cell.getTag() ;
			if ( tag == null ) {
				tag = new BackgroundButtonCellTag(cell) ;
				if ( tag.mBBC != null ) {
					tag.mBBC.setDelegate(OptionsBackgroundButtonView.this) ;
					tag.mBBC.setColorScheme(mColorScheme) ;
					tag.mBBC.setSoundPool(mSoundPool) ;
					tag.mBBC.setThumbnailSource( mThumbnailSoftCache, mThreadedBackgroundLoader ) ;
				}
				cell.setTag(tag) ;
			}
			// Set game mode, set the height to the ideal height, and refresh.
    		tag.mBBC.setContentBackground(background, availabilities.get(background), Background.equals(background, mCurrentBackground), mBGShuffled.get(background)) ;
    		
    		tag.mBBC.setMode( mShuffling ? BackgroundButtonCollage.Mode.SHUFFLE : BackgroundButtonCollage.Mode.STANDARD ) ;
			
			mBackgroundCellTag.put(background, tag) ;
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
			
			BackgroundButtonRowTag tag = ((BackgroundButtonRowTag)rowView.getTag()) ;
			if ( tag == null ) {
				tag = new BackgroundButtonRowTag( rowView ) ;
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
            tag.mSection = section ;
		}
		

		protected void refreshView( Background bg ) {
			if ( bg == null )
				return ;
			BackgroundButtonCellTag tag = mBackgroundCellTag.get(bg) ;
			if ( tag != null ) {
				BackgroundButtonCollage bbc = tag.mBBC ;
				// why do this two-step conversion, when we can
				// inline mBGShuffled.get(bg) as a method parameter?
				// Because the Object -> primitive conversion will cause
				// a NullPointerException if 'bg' is not an element in
				// mBGShuffled, as it would be if this is called during
				// an update rather than before or after it.
				Boolean inShuffleObj = mBGShuffled.get(bg) ;
				boolean inShuffle = inShuffleObj == null ? false : inShuffleObj.booleanValue() ;
				if ( bbc != null ) {
					Background viewBackground = bbc.getContentBackground() ;
					if ( bg.equals(viewBackground) && availabilities != null && mBGShuffled != null ) {
						bbc.setContentBackground(bg, availabilities.get(bg), bg.equals( mCurrentBackground ), inShuffle) ;
						bbc.refresh() ;
					}
				}
			}
		}
    	
	}
	
	
	private static class BackgroundIndexer implements SectionIndexer {
		
		// We label each background Template as its own Section.
		// In other words, they exactly correspond to the Background ordinals.
		protected static final Template [] TEMPLATES = Background.Template.values() ;
		protected static final String [] SECTIONS = new String[TEMPLATES.length] ;
		static {
			for ( int i = 0; i < TEMPLATES.length; i++ )
				SECTIONS[i] = Background.getName(TEMPLATES[i]) ;
		}
		
		protected Background [] mBackgrounds ;

		public BackgroundIndexer( Background [] backgrounds ) {
			mBackgrounds = backgrounds ;
		}
		
		synchronized public void setBackgrounds( Background [] backgrounds ) {
			mBackgrounds = backgrounds ;
		}
		
		synchronized public Object[] getSections() {
	        return SECTIONS ;
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
	    	
			if ( sectionIndex >= TEMPLATES.length )
				return mBackgrounds.length ;
			
			// find the first Background from this section.  Remember
			// that section is identical to Background Template ordinal.
			int t = TEMPLATES[sectionIndex].ordinal() ;
			for ( int i = 0; i < mBackgrounds.length; i++ ) {
				if ( t <= mBackgrounds[i].getTemplate().ordinal()  )
					return i ;
			}
	    	
	    	// huh?
	    	return mBackgrounds.length ;
	    }
	    
	    /**
	     * Returns the section index for a given position in the list by
	     * examining the item.
	     */
		synchronized public int getSectionForPosition(int position) {
			if ( position < 0 || position >= mBackgrounds.length )
				return -1 ;
			
			// Section index is Template ordinal.   Easy enough.
			return mBackgrounds[position].getTemplate().ordinal() ;
	    }
		
		synchronized public boolean isFirstInSection( int position ) {
			if ( position < 0 || position >= mBackgrounds.length )
				return false ;
			if ( position == 0 )
				return true ;

			// it's not position 0, so we can safely step back one.
			// Section is template ordinal; check templates against each other.
			return mBackgrounds[position].getTemplate() != mBackgrounds[position-1].getTemplate() ;
		}
	}


	@Override
	public boolean bbcd_setBackground(BackgroundButtonCollage mbc,
			Background background, OptionAvailability availability) {
		
		boolean performedAction = false ;
		if ( mDelegate != null ) {
			performedAction = true ;
			mDelegate.obvd_userSetCurrentBackground(this, background, availability) ;
		}
		
		return performedAction ;
		
	}
	
	@Override
	public boolean bbcd_setBackgroundAndIncludeInShuffle(BackgroundButtonCollage mbc,
			Background background, OptionAvailability availability) {
		
		boolean performedAction = false ;
		if ( mDelegate != null ) {
			performedAction = true ;
			mDelegate.obvd_userSetCurrentBackgroundAndIncludeInShuffle(this, background, availability) ;
		}
		
		return performedAction ;
		
	}

	@Override
	public boolean bbcd_setBackgroundInShuffle(BackgroundButtonCollage mbc,
			Background background, OptionAvailability availability,
			boolean inShuffle) {
		
		boolean performedAction = false ;
		if ( mDelegate != null ) {
			performedAction = true ;
			mDelegate.obvd_userSetBackgroundShuffled(this, background, inShuffle, availability) ;
		}
		
		return performedAction ;
		
	}
	
	//
	////////////////////////////////////////////////////////////////////////////

}
