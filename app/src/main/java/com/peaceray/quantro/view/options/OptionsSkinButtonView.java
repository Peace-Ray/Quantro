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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.peaceray.android.graphics.BitmapSoftCache;
import com.peaceray.quantro.R;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.utils.threadedloader.ThreadedBackgroundThumbnailLoader;
import com.peaceray.quantro.utils.threadedloader.ThreadedSkinThumbnailLoader;
import com.peaceray.quantro.view.button.collage.SkinButtonCollage;
import com.peaceray.quantro.view.button.strip.CustomButtonStrip;
import com.peaceray.quantro.view.generic.PinnedHeaderListView;
import com.velosmobile.utils.SectionableAdapter;

public class OptionsSkinButtonView extends OptionsView implements
		OptionsSkinView, com.peaceray.quantro.view.button.strip.CustomButtonStrip.Delegate, com.peaceray.quantro.view.button.collage.SkinButtonCollage.Delegate {
	
	private static final String TAG = "OptionsSkinButtonView" ;
	
	// ACTION BAR BUTTONS
	private String ACTION_NAME_SETTINGS ;
	
	// our delegate!
	private Delegate mDelegate ;
	private View mContentView ;
	private CustomButtonStrip mActionBar ;
	
	// Skin list view
	private PinnedHeaderListView mSkinListView ;
	private SkinButtonListAdapter mSkinButtonListAdapter ;
	private ArrayList<Skin> mSkins ;
	private Hashtable<Skin, OptionAvailability> mSkinAvailability ;
	
	// Current settings
	private Skin mCurrentSkin ;

	// Loading Thumbnails!
	private BitmapSoftCache mThumbnailSoftCache ;
	private ThreadedSkinThumbnailLoader mThreadedSkinLoader ;
	
	
	public OptionsSkinButtonView() {
		mSkins = new ArrayList<Skin>() ;
		mSkinAvailability = new Hashtable<Skin, OptionAvailability>() ;
	}
	

	@Override
	public void setDelegate(Delegate delegate) {
		mDelegate = delegate ;
	}

	@Override
	public void clearSkins() {
		mSkins.clear() ;
		mSkinAvailability.clear() ;
	}

	@Override
	public void addSkin(Skin skin, OptionAvailability availability) {
		if ( mSkins.contains(skin) )
			setSkinAvailability(skin, availability) ;
		else {
			insertInOrder( mSkins, skin ) ;
			mSkinAvailability.put(skin, availability) ;
		}
	}

	@Override
	public void setSkins(Collection<Skin> skins,
			Map<Skin, OptionAvailability> availability) {
		clearSkins() ;
		Iterator<Skin> iter = skins.iterator() ;
		for ( ; iter.hasNext() ; ) {
			Skin s = iter.next() ;
			addSkin( s, availability.get(s) ) ;
		}
	}

	@Override
	public void setSkinAvailability(Skin skin, OptionAvailability availability) {
		if ( !mSkins.contains(skin) )
			throw new IllegalArgumentException("Skin " + skin + " not contained in our records.") ;
		mSkinAvailability.put(skin, availability) ;
	}

	@Override
	public void setSkinAvailability(Map<Skin, OptionAvailability> availability) {
		Set<Entry<Skin, OptionAvailability>> set = availability.entrySet() ;
		Iterator<Entry<Skin, OptionAvailability>> iterator = set.iterator() ;
		for ( ; iterator.hasNext() ; ) {
			Entry<Skin, OptionAvailability> entry = iterator.next() ;
			if ( !mSkins.contains(entry.getKey()) )
				throw new IllegalArgumentException("Skin " + entry.getKey() + " not contained in our records.") ;
			mSkinAvailability.put( entry.getKey(), entry.getValue() ) ;
		}
	}

	@Override
	public void setCurrentSkin(Skin skin) {
		Skin prev = mCurrentSkin ;
		mCurrentSkin = skin ;
		if ( mSkinListView != null && mSkinButtonListAdapter != null ) {
			mSkinButtonListAdapter.refreshView( prev ) ;
			mSkinButtonListAdapter.refreshView( skin ) ;
		}
	}

	@Override
	public void init(Activity activity, View root) {
		mThumbnailSoftCache = new BitmapSoftCache() ;
		mThreadedSkinLoader = new ThreadedSkinThumbnailLoader( activity.getAssets() ) ;
		
		
		Resources res = activity.getResources() ;
		
		// Action names
		ACTION_NAME_SETTINGS = res.getString(R.string.action_strip_name_settings) ;
		
		mContentView = root ;
		
		// get reference to the action bar
		mActionBar = (CustomButtonStrip)mContentView.findViewById(R.id.game_options_skin_action_strip) ;
		mActionBar.setDelegate(this) ;
		mActionBar.setAutoRefresh(false) ;
		mActionBar.setEnabled( mActionBar.getButton(ACTION_NAME_SETTINGS), true ) ;
		mActionBar.refresh() ;
		
		// Set up the skin list.
		// Reference to the a list view?
		this.mSkinListView = (PinnedHeaderListView)mContentView.findViewById(R.id.game_options_skin_list) ;
		if ( mSkinListView != null ) {
			// configure!
			int layoutID = R.layout.button_collage_skin_list_item ;
			this.mSkinButtonListAdapter = new SkinButtonListAdapter(
					activity.getLayoutInflater(),
					layoutID,
					0,
					R.id.skin_list_row) ;
			mSkinListView.setAdapter(mSkinButtonListAdapter) ;
			mSkinListView.setOnScrollListener(mSkinButtonListAdapter) ;
			
			if ( mSkinListView instanceof PinnedHeaderListView ) {
    			View pinnedHeaderView = activity.getLayoutInflater().inflate(R.layout.quantro_list_item_header, mSkinListView, false) ;
    			pinnedHeaderView.setTag( new SkinButtonRowTag(pinnedHeaderView ) ) ;
    			((PinnedHeaderListView)mSkinListView).setPinnedHeaderView( pinnedHeaderView ) ;
    			mSkinListView.setDivider(null) ;
    			mSkinListView.setDividerHeight(0);
    		}
		}
	}

	@Override
	public void refresh() {
		mActionBar.refresh() ;
		
		// refresh the adapter
		if ( mSkinListView != null && mSkinButtonListAdapter != null ) {
			mSkinButtonListAdapter.set( mSkins, mSkinAvailability ) ;
			mSkinButtonListAdapter.notifyDataSetChanged() ;
			mSkinListView.invalidateViews() ;
		}
	}
	
	
	@Override
	protected void relaxCache(boolean isRelaxed) {
		if ( !isRelaxed ) {
			// empty our thumbnail cache.  This cache is
			// populated by individual SkinButtonCollages
			// as-needed.  we should therefore first clear the
			// list, and then clear the cache itself.
			mSkinButtonListAdapter.set( null, null ) ;
			mSkinButtonListAdapter.notifyDataSetChanged() ;
			mThumbnailSoftCache.clear() ;
		}
	}

	@Override
	protected void refreshCache(boolean isRelaxed) {
		if ( isRelaxed ) {
			// our thumbnail cache is empty, and we have cleared
			// the list adapter of Skins.  We DON'T need
			// to manually repopulate the cache, since the list
			// items do that in the background; instead, just set
			// our current backgrounds as the content of that list.
			mSkinButtonListAdapter.set( mSkins, mSkinAvailability ) ;
			mSkinButtonListAdapter.notifyDataSetChanged() ;
			mSkinListView.invalidateViews() ;
		}
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// CUSTOM BUTTON STRIP (OUR ACTION BAR)
	// 
	
	@Override
	public boolean customButtonStrip_onButtonClick(CustomButtonStrip strip,
			int buttonNum, String name, boolean asOverflow ) {
		
		boolean performAction = false ;

		if ( mDelegate != null ) {
			if ( ACTION_NAME_SETTINGS.equals(name) ) {
				performAction = true ;
				mDelegate.osvd_userAdvancedConfiguration(this) ;
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
	// BACKGROUND LIST
	//
	
	
	/**
	 * Returns the appropriate insertion position for the provided Skin object
	 * into the provided list.
	 * 
	 * We sort our tracks in a particular order: ordinal by Game, ordinal
	 * by Template, and then ordinal by Color.
	 * 
	 */
	private int insertionPosition( ArrayList<Skin> list, Skin s ) {
		
		int g = s.getGame().ordinal() ;
		int t = s.getTemplate().ordinal() ;
		int c = s.getColor().ordinal() ;
		
		// find the object which should be immediately after 'm',
		// and return its location.  If none, return size.
		for ( int i = 0; i < list.size(); i++ ) {
			Skin sHere = list.get(i) ;
			
			int gHere = sHere.getGame().ordinal() ;
			int tHere = sHere.getTemplate().ordinal() ;
			int cHere = sHere.getColor().ordinal() ;
			
			if ( gHere > g )
				return i ;
			if ( gHere == g && tHere > t )
				return i ;
			if ( gHere == g && tHere == t && cHere > c )
				return i ;
		}
		
		return list.size() ;
	}
	
	private void insertInOrder( ArrayList<Skin> list, Skin m ) {
		list.add( insertionPosition(list, m), m ) ;
	}
	
	
	private static class SkinButtonCellTag {
		SkinButtonCollage mSBC ;
		
		public SkinButtonCellTag( View v ) {
			mSBC = (SkinButtonCollage) v.findViewById( R.id.button_collage_skin ) ;
		}
	}
	
	
	private static class SkinButtonRowTag {
		View mHeaderView ;
		View mHeaderViewTopSpacer ;
		TextView mHeaderTextView ;
		int mSection = -1 ;
		
		public SkinButtonRowTag( View v ) {
			mHeaderView = v.findViewById(R.id.quantro_list_item_header) ;
			mHeaderViewTopSpacer = v.findViewById(R.id.quantro_list_item_header_top_spacer) ;
			mHeaderTextView = (TextView)v.findViewById(R.id.quantro_list_item_header_text) ;
		}
	}
	
	
	private class SkinButtonListAdapter extends SectionableAdapter
			implements OnScrollListener, PinnedHeaderListView.PinnedHeaderAdapter, SectionIndexer {
		
		
		@SuppressWarnings("unchecked")
		public void set( ArrayList<Skin> m, Hashtable<Skin, OptionAvailability> a ) {
			
			if ( m != null )
				skins = (ArrayList<Skin>)m.clone() ;
			else
				skins.clear() ;
			
			if ( a != null )
				availabilities = (Hashtable<Skin,  OptionAvailability>)a.clone() ;
			else
				availabilities.clear() ;
			
			Skin [] array = new Skin[skins.size()] ;
			array = skins.toArray(array) ;
			mIndexer.setSkins(array) ;
			
			mSkinCellTag.clear() ;
		}
		
		ArrayList<Skin> skins = new ArrayList<Skin>() ;
    	Hashtable<Skin, OptionAvailability> availabilities = new Hashtable<Skin, OptionAvailability>() ;
    	private SkinIndexer mIndexer ;
    	
    	boolean mScrolledToTop ;
    	
    	private Hashtable<Skin, SkinButtonCellTag> mSkinCellTag ;
    	
    	public SkinButtonListAdapter(LayoutInflater inflater,
				int rowLayoutID, int headerID, int itemHolderID) {
			
			super(inflater, rowLayoutID, headerID, itemHolderID);
			
			mIndexer = new SkinIndexer(new Skin[0]) ;
			mScrolledToTop = true ;
			
			mSkinCellTag = new Hashtable<Skin, SkinButtonCellTag>() ;
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
			SkinButtonRowTag tag = null ;
			if ( topChild != null )
				tag = ((SkinButtonRowTag)topChild.getTag()) ;
			
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
			SkinButtonRowTag tag = (SkinButtonRowTag)header.getTag() ;
        	Drawable bg = tag.mHeaderTextView.getBackground() ;
        	bg.getPadding(tempPaddingRect) ;
        	r.set(0, tag.mHeaderTextView.getTop() + tempPaddingRect.top,
        			header.getWidth(), tag.mHeaderTextView.getBottom() - tempPaddingRect.bottom) ;
        	return true ;
        }
        
        @Override
        public boolean copyPinnedHeaderVisibleBounds(View header, int position, Rect r) {
        	SkinButtonRowTag tag = (SkinButtonRowTag)header.getTag() ;
        	r.set(0, tag.mHeaderTextView.getTop(),
        			header.getWidth(), tag.mHeaderTextView.getBottom()) ;
        	return true ;
        }
        
        
        @Override
        public boolean copyHeaderOccludingBounds(View view, int position, Rect r) {
        	SkinButtonRowTag tag = (SkinButtonRowTag)view.getTag() ;
        	if ( tag.mHeaderView.getVisibility() == View.GONE ) {
        		r.set(0, 0, 0, 0) ;
        		return true ;
        	}
        	
        	return copyPinnedHeaderOccludingBounds( tag.mHeaderView, position, r ) ;
        }
        
        @Override
        public boolean copyHeaderVisibleBounds(View view, int position, Rect r) {
        	SkinButtonRowTag tag = (SkinButtonRowTag)view.getTag() ;
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
			
			SkinButtonRowTag tag = (SkinButtonRowTag)v.getTag() ;
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
			return skins.get(position) ;
		}


		@Override
		protected int getDataCount() {
			return skins.size() ;
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
			return SkinIndexer.SECTIONS.length ;
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
			Skin skin = skins.get(position) ;
			
			SkinButtonCellTag tag = (SkinButtonCellTag)cell.getTag() ;
			if ( tag == null ) {
				tag = new SkinButtonCellTag(cell) ;
				if ( tag.mSBC != null ) {
					tag.mSBC.setDelegate(OptionsSkinButtonView.this) ;
					tag.mSBC.setColorScheme(mColorScheme) ;
					tag.mSBC.setSoundPool(mSoundPool) ;
					tag.mSBC.setThumbnailSource( mThumbnailSoftCache, mThreadedSkinLoader ) ;
				}
				cell.setTag(tag) ;
			}
			// Set game mode, set the height to the ideal height, and refresh.
    		tag.mSBC.setContentSkin(skin, availabilities.get(skin), Skin.equals(skin, mCurrentSkin)) ;
			
			mSkinCellTag.put(skin, tag) ;
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
			
			SkinButtonRowTag tag = ((SkinButtonRowTag)rowView.getTag()) ;
			if ( tag == null ) {
				tag = new SkinButtonRowTag( rowView ) ;
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
		

		protected void refreshView( Skin s ) {
			if ( s == null )
				return ;
			SkinButtonCellTag tag = mSkinCellTag.get(s) ;
			if ( tag != null ) {
				SkinButtonCollage sbc = tag.mSBC ;
				if ( sbc != null ) {
					Skin viewSkin = sbc.getContentSkin() ;
					if ( s.equals(viewSkin) && availabilities != null ) {
						sbc.setContentSkin(s, availabilities.get(s), s.equals( mCurrentSkin )) ;
						sbc.refresh() ;
					}
				}
			}
		}
	}
	
	
	private static class SkinIndexer implements SectionIndexer {
		
		// We label each skin Template as its own Section.
		// In other words, they exactly correspond to the Skin ordinals.
		protected static final Skin.Template [] TEMPLATES = Skin.Template.values() ;
		protected static final String [] SECTIONS = new String[TEMPLATES.length] ;
		static {
			for ( int i = 0; i < TEMPLATES.length; i++ )
				SECTIONS[i] = Skin.getName(TEMPLATES[i]) ;
		}
		
		protected Skin [] mSkins ;

		public SkinIndexer( Skin [] skins ) {
			mSkins = skins ;
		}
		
		synchronized public void setSkins( Skin [] skins ) {
			mSkins = skins ;
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
				return mSkins.length ;
			
			// find the first Skin from this section.  Remember
			// that section is identical to Skin Template ordinal.
			int t = TEMPLATES[sectionIndex].ordinal() ;
			for ( int i = 0; i < mSkins.length; i++ ) {
				if ( t <= mSkins[i].getTemplate().ordinal()  )
					return i ;
			}
	    	
	    	// huh?
	    	return mSkins.length ;
	    }
	    
	    /**
	     * Returns the section index for a given position in the list by
	     * examining the item.
	     */
		synchronized public int getSectionForPosition(int position) {
			if ( position < 0 || position >= mSkins.length )
				return -1 ;
			
			// Section index is Template ordinal.   Easy enough.
			return mSkins[position].getTemplate().ordinal() ;
	    }
		
		synchronized public boolean isFirstInSection( int position ) {
			if ( position < 0 || position >= mSkins.length )
				return false ;
			if ( position == 0 )
				return true ;

			// it's not position 0, so we can safely step back one.
			// Section is template ordinal; check templates against each other.
			return mSkins[position].getTemplate() != mSkins[position-1].getTemplate() ;
		}
	}


	@Override
	public boolean sbcd_setSkin(SkinButtonCollage mbc, Skin skin,
			OptionAvailability availability) {
		
		Log.d(TAG, "sbcd_setSkin") ;
		
		boolean performedAction = false ;
		if ( mDelegate != null ) {
			performedAction = true ;
			mDelegate.osvd_userSetCurrentSkin(this, skin, availability) ;
		}
		
		return performedAction ;
	}

	
	//
	////////////////////////////////////////////////////////////////////////////

}
