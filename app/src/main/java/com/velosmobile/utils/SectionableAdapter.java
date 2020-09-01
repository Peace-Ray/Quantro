/*
 * Copyright (C) 2012 Velos Mobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * See https://github.com/velos/SectionedGrid/blob/43865648c5106f901f0f89ac436c2352796f8004/src/com/velosmobile/utils/SectionableAdapter.java
 *
 * Changes by Jake Rosin:
 *
 * - Add "CellVisibility" to customize unused call visibility based on position in section;
 * 		apply these settings in getView.
 * - Make cell header (headerID) optional.
 * - Recalculate `sectionsCount` in `notifyDataSetChanged`
 * - Add updateColumnAndSectionCounts to recalculate section and column counts;
 * 		call to update internal counts when retrieving row positions, cell counts, or cell views.
 * - Some format adjustments (e.g. removing newlines before curly braces)
 */

package com.velosmobile.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * An Adapter that populates a grid of 1-n columns. Unlike a standard Android
 * GridView, lists using this Adapter can label sections within the grid, and
 * can include shorter rows that do not fill each column space. Subclasses must
 * define their sections and contents.
 * 
 * @author Velos Mobile
 */
public abstract class SectionableAdapter extends BaseAdapter {
	
	public enum CellVisibility {
		/**
		 * The cell will be Invisible (it will still take up space,
		 * but will not be displayed)
		 */
		INVISIBLE,
		
		/**
		 * The cell will be Gone (it will neither appear on screen,
		 * nor take up space).
		 */
		GONE,
		
		/**
		 * The cell will be Gone if it is in the first row of its section
		 * (otherwise it will be Invisible).
		 */
		GONE_IF_FIRST_ROW_IN_SECTION
	} ;
	

	private LayoutInflater inflater;
	private int rowResID;
	private int headerID;
	private int itemHolderID;
	private int colCount;
	private int sectionsCount;
	
	private CellVisibility mUnusedCellVisibility ;

	/**
	 * Constructor.
	 * 
	 * @param inflater
	 *            inflater to create rows within the grid.
	 * @param rowLayoutID
	 *            layout resource ID for each row within the grid.
	 * @param headerID
	 *            resource ID for the header element contained within the grid
	 *            row. 0 If unused.
	 * @param itemHolderID
	 *            resource ID for the cell wrapper contained within the grid
	 *            row. This View must only contain cells.
	 */
	public SectionableAdapter(LayoutInflater inflater, int rowLayoutID,
			int headerID, int itemHolderID) {
		this(inflater, rowLayoutID, headerID, itemHolderID, CellVisibility.INVISIBLE) ;
	}
	
	
	public SectionableAdapter(LayoutInflater inflater, int rowLayoutID,
			int headerID, int itemHolderID, CellVisibility unusedCellVisibility) {
		
		super();
		this.inflater = inflater;
		this.rowResID = rowLayoutID;
		this.headerID = headerID;
		this.itemHolderID = itemHolderID;
		this.colCount = -1 ;
		this.mUnusedCellVisibility = unusedCellVisibility ;
	}
	
	
	protected void updateColumnAndSectionCounts() {
		// Determine how many columns our row holds.
		View row = inflater.inflate(rowResID, null);
		ViewGroup holder = (ViewGroup) row.findViewById(itemHolderID);
		colCount = holder.getChildCount();
		sectionsCount = getSectionsCount();
	}
	
	
	protected int getItemHolderID() {
		return itemHolderID ;
	}

	/**
	 * Returns the total number of items to display.
	 */
	protected abstract int getDataCount();

	/**
	 * Returns the number of sections to display.
	 */
	protected abstract int getSectionsCount();

	/**
	 * @param index
	 *            the 0-based index of the section to count.
	 * @return the number of items in the requested section.
	 */
	protected abstract int getCountInSection(int index);

	/**
	 * @param position
	 *            the 0-based index of the data element in the grid.
	 * @return which section this item belongs to.
	 */
	protected abstract int getTypeFor(int position);

	/**
	 * @param section
	 *            the 0-based index of the section.
	 * @return the text to display for this section.
	 */
	protected abstract String getHeaderForSection(int section);

	/**
	 * Populate the View and attach any listeners.
	 * 
	 * @param cell
	 *            the inflated cell View to populate.
	 * @param position
	 *            the 0-based index of the data element in the grid.
	 */
	protected abstract void bindView(View cell, int position);

	/**
	 * Perform any row-specific customization your grid requires. For example,
	 * you could add a header to the first row or a footer to the last row.
	 * 
	 * @param row
	 *            the 0-based index of the row to customize.
	 * @param firstPosition
	 *            The 0-based index of the first item position for the row
	 * @param convertView
	 *            the inflated row View.
	 * @param listItemView
	 *            the container view for the entire list item
	 */
	protected void customizeRow(int row, int firstPosition, View rowView) {
		// By default, does nothing. Override to perform custom actions.
	}

	@Override
	public int getCount() {
		if ( colCount < 0 ) {
			updateColumnAndSectionCounts() ;
		}
		int totalCount = 0;
		for (int i = 0; i < sectionsCount; ++i) {
			int count = getCountInSection(i);
			if (count > 0)
				totalCount += (getCountInSection(i) - 1) / colCount + 1;
		}
		if (totalCount == 0)
			totalCount = 1;
		return totalCount;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	/**
	 * Given the 0-indexed real-position (counting actual items), returns the
	 * row-number (0-indexed) in which that position is represented.
	 * 
	 * @param realPosition
	 * @return
	 */
	protected int getRowPosition(int realPosition) {
		if ( colCount < 0 ) {
			updateColumnAndSectionCounts() ;
		}
		
		int in = realPosition;
		int row = 0;
		for (int i = 0; i < sectionsCount; ++i) {
			int sectionCount = getCountInSection(i);
			if (realPosition < sectionCount) {
				// appears in this section...
				while (realPosition >= colCount) {
					row++;
					realPosition -= colCount;
				}

				return row;
			}

			// skip this section.
			realPosition -= sectionCount;
			row += Math.ceil(((double) sectionCount) / colCount);
		}

		return -1;
	}

	/**
	 * Given the 0-indexed position relative to any attached list-view (i.e.,
	 * the row position) returns the real position (i.e., the first displayed
	 * item in the row).
	 * 
	 * @param position
	 */
	protected int getRealPosition(int rowPosition) {
		if ( colCount < 0 ) {
			updateColumnAndSectionCounts() ;
		}
		
		if (rowPosition < 0)
			return -1;

		int realPosition = 0;
		int rows = 0;

		for (int i = 0; i < sectionsCount; ++i) {
			int sectionCount = getCountInSection(i);
			if (sectionCount > 0
					&& rowPosition <= rows + (sectionCount - 1) / colCount) {
				realPosition += (rowPosition - rows) * colCount;
				break;
			} else {
				if (sectionCount > 0) {
					rows += (int) ((sectionCount - 1) / colCount + 1);
				}
				realPosition += sectionCount;
			}
		}

		return realPosition;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if ( colCount < 0 ) {
			updateColumnAndSectionCounts() ;
		}
		
		int realPosition = 0;
		int viewsToDraw = 0;
		int rows = 0;
		int totalCount = 0;
		for (int i = 0; i < sectionsCount; ++i) {
			int sectionCount = getCountInSection(i);
			totalCount += sectionCount;
			if (sectionCount > 0
					&& position <= rows + (sectionCount - 1) / colCount) {
				realPosition += (position - rows) * colCount;
				viewsToDraw = (int) (totalCount - realPosition);
				break;
			} else {
				if (sectionCount > 0) {
					rows += (int) ((sectionCount - 1) / colCount + 1);
				}
				realPosition += sectionCount;
			}
		}
		// Phew... okay! Now we just need to build it!
		if (convertView == null) {
			convertView = inflater.inflate(rowResID, parent, false);
		}
		int lastType = -1;
		if (realPosition > 0)
			lastType = getTypeFor(realPosition - 1);
		if (getDataCount() > 0 && headerID != 0) {
			TextView header = (TextView) convertView.findViewById(headerID);
			int newType = getTypeFor(realPosition);
			if (newType != lastType) {
				header.setVisibility(View.VISIBLE);
				header.setText(getHeaderForSection(newType));

			} else {
				header.setVisibility(View.GONE);
			}
		}
		customizeRow(position, realPosition, convertView);

		for (int i = 0; i < colCount; ++i) {
			View child = ((ViewGroup) convertView.findViewById(itemHolderID))
					.getChildAt(i);
			if (i < viewsToDraw && child != null) {
				bindView(child, realPosition + i);
				child.setVisibility(View.VISIBLE);
			} else if (child != null) {
				int visibility = View.INVISIBLE ;
				switch( mUnusedCellVisibility ) {
				case GONE:
					visibility = View.GONE ;
					break ;
				case INVISIBLE:
					visibility = View.INVISIBLE ;
					break ;
				case GONE_IF_FIRST_ROW_IN_SECTION:
					if ( realPosition == 0 || getTypeFor(realPosition) != getTypeFor(realPosition-1) ) {
						// first row in this section
						visibility = View.GONE ;
					} else {
						visibility = View.INVISIBLE ;
					}
					break ;
				}
				child.setVisibility(visibility);
			}
		}
		return convertView;
	}
	
	@Override
	public void notifyDataSetChanged() {
		// a good time to refresh our sections count...
		sectionsCount = getSectionsCount();
		super.notifyDataSetChanged() ;
	}

}
