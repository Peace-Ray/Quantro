package com.peaceray.quantro.view.generic;

import com.peaceray.quantro.R;
import com.peaceray.quantro.utils.VersionSafe;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * An adapter for altering the contents of a the labeled_seek_bar layout.
 * 
 * @author Jake
 *
 */
public class LabeledSeekBarAdapter implements SeekBar.OnSeekBarChangeListener {
	
	/**
	 * The LabeledSeekBarAdapter handles all the necessary operations to change labels
	 * and current displayed value; however, some other element might be interested in
	 * updates, so we allow this listener.  Methods resemble wrappers to the standard
	 * "SeekBar.OnSeekBarChangeListener" interface, but are called after the adapter
	 * handles internal label changes and the like.
	 * 
	 * @author Jake
	 *
	 */
	public interface OnSeekBarChangeListener {
		/**
		 * Notification that the progress level has changed. Clients can use
		 * the fromUser parameter to distinguish user-initiated changes from
		 * those that occurred programmatically.
		 * 
		 * @param lsba The LabeledSeekBarAdapter object.
		 * @param seekBar The SeekBar whose progress has changed
		 * @param progress 	The current progress level. This will be in the range 0..max where max was set by setMax(int). (The default value for max is 100.)
		 * @param fromUser 	True if the progress change was initiated by the user. 
		 * @param value The value corresponding to this level of progress
		 */
		public void onProgressChanged( LabeledSeekBarAdapter lsba, SeekBar seekBar, int progress, boolean fromUser, int value ) ;
		
		/**
		 * Notification that the progress level has changed. Clients can use
		 * the fromUser parameter to distinguish user-initiated changes from
		 * those that occurred programmatically.
		 * 
		 * @param lsba The LabeledSeekBarAdapter object.
		 * @param seekBar The SeekBar whose progress has changed
		 * @param progress 	The current progress level. This will be in the range 0..max where max was set by setMax(int). (The default value for max is 100.)
		 * @param fromUser 	True if the progress change was initiated by the user. 
		 * @param value The value corresponding to this level of progress
		 */
		public void onProgressChanged( LabeledSeekBarAdapter lsba, SeekBar seekBar, int progress, boolean fromUser, float value ) ;
		
		/**
		 * Notification that the progress level has changed. Clients can use
		 * the fromUser parameter to distinguish user-initiated changes from
		 * those that occurred programmatically.
		 * 
		 * @param lsba The LabeledSeekBarAdapter object.
		 * @param seekBar The SeekBar whose progress has changed
		 * @param progress 	The current progress level. This will be in the range 0..max where max was set by setMax(int). (The default value for max is 100.)
		 * @param fromUser 	True if the progress change was initiated by the user. 
		 * @param value The value corresponding to this level of progress
		 */
		public void onProgressChanged( LabeledSeekBarAdapter lsba, SeekBar seekBar, int progress, boolean fromUser, Object value ) ;
	
		/**
		 * Notification that the user has started a touch gesture. Clients may want to use this to disable advancing the seekbar.
		 *
		 * @param lsba The LabeledSeekBarAdapter object
		 * @param seekBar 	The SeekBar in which the touch gesture began 
		 */
		public void onStartTrackingTouch( LabeledSeekBarAdapter lsba, SeekBar seekBar ) ;
		
		/**
		 * Notification that the user has finished a touch gesture. Clients may want to use this to re-enable advancing the seekbar.
		 * 
		 * @param lsba The LabeledSeekBarAdapter object
		 * @param seekBar 	The SeekBar in which the touch gesture began 
		 */
		public void onStopTrackingTouch( LabeledSeekBarAdapter lsba, SeekBar seekBar ) ;
	}
	
	
	private static final String TAG = "LabeledSeekBarAdapter" ;
	
	
	private static final int ENABLED_ALPHA = 255 ;
	private static final int DISABLED_ALPHA = 128 ;
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// LISTENER
	OnSeekBarChangeListener listener ;
	
	////////////////////////////////////////////////////////////////////////////
	// ENABLED?
	protected boolean mEnabled ;

	////////////////////////////////////////////////////////////////////////////
	// VIEW REFERENCES
	protected View mView ;		// this whole structure.
	
	// Top label - value area
	protected View mValueArea ;
	protected TextView mValueAreaFullLabel ;
	protected View mValueAreaParts ;
	protected TextView mValueAreaPartsLabel ;
	protected TextView mValueAreaPartsValue ;
	
	// Seek bar!
	protected SeekBar mSeekBar ;
	
	// Min/max value labels.
	protected View mMinMaxArea ;
	protected TextView mMinLabel ;
	protected TextView mMaxLabel ;
	protected View mMinMaxAreaSubstitute ;
	
	protected enum Style {
		/**
		 * Explicit values and labels -- the standard format.  Before
		 * revising this to match SeekBarPreference, this was how all
		 * labeled seek bars behaved.
		 */
		EXPLICIT,
		
		
		/**
		 * A simple range between min / max at a given interval.  Labels
		 * are given as the value plus explicit units (prefix and/or postfix).
		 */
		RANGE,
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Values!
	protected Style mStyle ;
	// Used for EXPLICIT style
	protected boolean mUsingValueFullLabel ;
	protected String [] mValueTextFullLabels ;
	protected String mValueTextPartsLabel ;
	protected String [] mValueTextPartsValues ;
	// Actual values.  Can be objects, ints, floats.
	protected int [] mValuesInt ;
	protected float [] mValuesFloat ;
	protected Object [] mValuesObject ;
	
	// Used for RANGE style
	protected int mMaxValue ;
	protected int mMinValue ;
	protected int mInterval = 1 ;
	protected int mExponent = 0 ;
	protected String mUnitsLeft  = "";
	protected String mUnitsRight = "";
	
	
	// Number of seek bar items!
	protected int mSeekBarMax ;
	
	// Min/max value labels.
	protected String mMinTextLabel ;
	protected String mMaxTextLabel ;
	
	////////////////////////////////////////////////////////////////////////////
	// Every time we make a manually change to progress, we set this value,
	// so we don't bother telling the listener.
	protected boolean mProgrammaticChange ;
	
	////////////////////////////////////////////////////////////////////////////
	// Runnables allowing changes to the SeekBar and associated labels to
	// occur on the UIThread.  We also keep the Activity.
	Activity mActivity ;
	Runnable mRunnableRefreshSeekBar ;
	Runnable mRunnableRefreshValue ;
	Runnable mRunnableRefreshMinMax ;
	// Set visibility for elements
	Runnable mRunnableSetViewVisible ;
	Runnable mRunnableSetViewGone ;
	Runnable mRunnableSetValueVisible ;
	Runnable mRunnableSetValueGone ;
	Runnable mRunnableSetMinMaxVisible ;
	Runnable mRunnableSetMinMaxGone ;
	// Set enabled
	Runnable mRunnableSetEnabled ;
	Runnable mRunnableSetDisabled ;
	
	
	
	/**
	 * 
	 * @param labeledSeekBarLayout
	 */
	public LabeledSeekBarAdapter( Activity activity, View labeledSeekBarLayout ) {
		////////////////////////////////////////////////////////////////////////
		// REFERENCES TO VIEWS
		mView = labeledSeekBarLayout ;
		
		// Look for elements.
		mValueArea = mView.findViewById(R.id.labeled_seek_bar_value_area) ;
		mValueAreaFullLabel = (TextView)mView.findViewById(R.id.labeled_seek_bar_value_full) ;
		mValueAreaParts = mView.findViewById(R.id.labeled_seek_bar_value_parts) ;
		mValueAreaPartsLabel = (TextView)mView.findViewById(R.id.labeled_seek_bar_value_parts_label) ;
		mValueAreaPartsValue = (TextView)mView.findViewById(R.id.labeled_seek_bar_value_parts_value) ;
		
		// Seek bar!
		mSeekBar = (SeekBar)mView.findViewById(R.id.labeled_seek_bar_seek_bar) ;
		
		// Min/max value labels.
		mMinMaxArea = mView.findViewById(R.id.labeled_seek_bar_min_max_labels) ;
		mMinLabel = (TextView)mView.findViewById(R.id.labeled_seek_bar_min_label) ;
		mMaxLabel = (TextView)mView.findViewById(R.id.labeled_seek_bar_max_label) ;
		mMinMaxAreaSubstitute = mView.findViewById(R.id.labeled_seek_bar_min_max_labels_substitute) ;
		
		////////////////////////////////////////////////////////////////////////
		// VERIFY THAT WE HAVE ALL THE VIEWS
		if ( !hasAllReferences() )
			Log.d(TAG, "WARNING: labeled_seek_bar layout does not contain all recommended components") ;
		if ( mSeekBar == null )
			throw new IllegalArgumentException("Provided layout must include a SeekBar!") ;
		
		////////////////////////////////////////////////////////////////////////
		// SET OURSELF AS LISTENER
		mSeekBar.setOnSeekBarChangeListener(this) ;
		
		////////////////////////////////////////////////////////////////////////
		// BY DEFAULT USE 'EXPLICIT' STYLE
		mStyle = Style.EXPLICIT ;
		
		////////////////////////////////////////////////////////////////////////
		// INITIALIZE ALL TEXT
		mUsingValueFullLabel = true ;
		mValueTextFullLabels = null ;
		mValueTextPartsLabel = null ;
		mValueTextPartsValues = null ;
		mValuesInt = null ;
		mValuesFloat = null ;
		mValuesObject = null ;
		
		// Number of seek bar items!
		mSeekBarMax = 1 ;
		
		// Min/max value labels.
		mMinTextLabel = null ;
		mMaxTextLabel = null ;
		
		mProgrammaticChange = false ;
		
		////////////////////////////////////////////////////////////////////////
		// INITIALIZE 'RANGE' VALUES
		mMaxValue = 0;
		mMinValue = 0 ;
		mInterval = 1 ;
		mUnitsLeft  = "" ;
		mUnitsRight = "" ;
		
		////////////////////////////////////////////////////////////////////////
		// Make Runnables
		mActivity = activity ;
		mRunnableRefreshSeekBar = new Runnable() {
			@Override
			public void run() {
				int prevLevel = mSeekBar.getProgress() ;
				mProgrammaticChange = true ;
				mSeekBar.setMax(mSeekBarMax) ;
				if ( prevLevel > mSeekBarMax )
					mSeekBar.setProgress(mSeekBarMax) ;
				else if ( prevLevel < 0 )
					mSeekBar.setProgress(0) ;
				else
					mSeekBar.setProgress(prevLevel) ;
				mProgrammaticChange = false ;
			}
		};
		mRunnableRefreshValue = new Runnable() {
			@Override
			public void run() {
				refreshValue() ;
			}
		};
		mRunnableRefreshMinMax = new Runnable() {
			@Override
			public void run() {
				if ( mMinLabel != null )
					mMinLabel.setText( mMinTextLabel ) ;
				if ( mMaxLabel != null )
					mMaxLabel.setText( mMaxTextLabel ) ;
			}
		};
		mRunnableSetViewVisible = new Runnable() {
			@Override
			public void run() {
				if ( mView != null )
					mView.setVisibility(View.VISIBLE) ;
			}
		};
		mRunnableSetViewGone = new Runnable() {
			@Override
			public void run() {
				if ( mView != null )
					mView.setVisibility(View.GONE) ;
			}
		};
		mRunnableSetValueVisible = new Runnable() {
			@Override
			public void run() {
				if ( mValueArea != null )
					mValueArea.setVisibility(View.VISIBLE) ;
			}
		};
		mRunnableSetValueGone = new Runnable() {
			@Override
			public void run() {
				if ( mValueArea != null )
					mValueArea.setVisibility(View.GONE) ;
			}
		};
		mRunnableSetMinMaxVisible = new Runnable() {
			@Override
			public void run() {
				if ( mMinMaxArea != null )
					mMinMaxArea.setVisibility(View.VISIBLE) ;
				if ( mMinMaxAreaSubstitute != null )
					mMinMaxAreaSubstitute.setVisibility(View.GONE) ;
			}
		};
		mRunnableSetMinMaxGone = new Runnable() {
			@Override
			public void run() {
				if ( mMinMaxArea != null )
					mMinMaxArea.setVisibility(View.GONE) ;
				if ( mMinMaxAreaSubstitute != null )
					mMinMaxAreaSubstitute.setVisibility(View.VISIBLE) ;
			}
		};
		mRunnableSetEnabled = new Runnable() {
			@Override
			public void run() {
				float alpha = ENABLED_ALPHA / 255.0f ;
				
				if ( mValueAreaFullLabel != null )
					VersionSafe.setAlpha(mValueAreaFullLabel, alpha) ;
				if ( mValueAreaPartsLabel != null )
					VersionSafe.setAlpha(mValueAreaPartsLabel, alpha) ;
				if ( mValueAreaPartsValue != null )
					VersionSafe.setAlpha(mValueAreaPartsValue, alpha) ;
				
				mSeekBar.setEnabled(true) ;
				
				if ( mMinLabel != null )
					VersionSafe.setAlpha(mMinLabel, alpha) ;
				if ( mMaxLabel != null )
					VersionSafe.setAlpha(mMaxLabel, alpha) ;
				
				mEnabled = true ;
			}
		};
		mRunnableSetDisabled = new Runnable() {
			@Override
			public void run() {
				float alpha = DISABLED_ALPHA / 255.0f ;
				
				if ( mValueAreaFullLabel != null )
					VersionSafe.setAlpha(mValueAreaFullLabel, alpha) ;
				if ( mValueAreaPartsLabel != null )
					VersionSafe.setAlpha(mValueAreaPartsLabel, alpha) ;
				if ( mValueAreaPartsValue != null )
					VersionSafe.setAlpha(mValueAreaPartsValue, alpha) ;
				
				mSeekBar.setEnabled(false) ;
				
				if ( mMinLabel != null )
					VersionSafe.setAlpha(mMinLabel, alpha) ;
				if ( mMaxLabel != null )
					VersionSafe.setAlpha(mMaxLabel, alpha) ;
				
				mEnabled = false ;
			}
		};
		
	}
	
	
	/**
	 * Returns whether all view references are non-null.  These are set
	 * in the constructor, so this method provides a means to verify that the
	 * constructor-provided layout contained all the necessary structures.
	 * @return
	 */
	protected boolean hasAllReferences() {
		boolean missing = false ;
		// Value elements
		missing = missing || null == mValueArea ;
		missing = missing || null == mValueAreaFullLabel ;
		missing = missing || null == mValueAreaParts ;
		missing = missing || null == mValueAreaPartsLabel ;
		missing = missing || null == mValueAreaPartsValue ;
		// Seek bar
		missing = missing || null == mSeekBar ;
		// Min/max value labels
		missing = missing || null == mMinMaxArea ;
		missing = missing || null == mMinLabel ;
		missing = missing || null == mMaxLabel ;
		
		return !missing ;
	}
	
	
	synchronized public void setListener( OnSeekBarChangeListener listener ) {
		this.listener = listener ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// ACCESSORS / VALUE MUTATORS
	//
	// Get currently set stuff.
	//
	////////////////////////////////////////////////////////////////////////////	
	
	
	synchronized public void setIntValue( int val ) {
		switch( mStyle ) {
		case EXPLICIT:
			// find the appropriate value (nearest <=)...
			int index = 0 ;
			for ( int i = 0; i < mValuesInt.length; i++ ) {
				if ( mValuesInt[i] <= val )
					index = i ;
			}
			mActivity.runOnUiThread( new SetProgressRunnable(index) ) ;
			break ;
			
		case RANGE:
			// find the appropriate value (nearest <=)...
			int mult ;
			mult = ( val - this.mMinValue ) / mInterval ;
			mult = Math.max(0, mult) ;
			mult = Math.min(mSeekBarMax, mult) ;
			mActivity.runOnUiThread( new SetProgressRunnable(mult) ) ;
			break ;
		}
	}
	
	synchronized public int intValue() {
		switch( mStyle ) {
		case EXPLICIT:
			return mValuesInt[mSeekBar.getProgress()] ;
		case RANGE:
			return mSeekBar.getProgress() * mInterval + mMinValue ;
		}
		
		throw new IllegalStateException("Can't handle style " + mStyle) ;
	}
	
	synchronized public float floatValue() {
		return mValuesFloat[mSeekBar.getProgress()] ;
	}
		
	synchronized public Object objectValue() {
		return mValuesObject[mSeekBar.getProgress()] ;
	}
	
	synchronized public void setProgress( int p ) {
		mActivity.runOnUiThread( new SetProgressRunnable(p) ) ;
	}
	
	synchronized public int getProgress() {
		return mSeekBar.getProgress() ;
	}
	
	private class SetProgressRunnable implements Runnable {
		int p ;
		
		public SetProgressRunnable( int p ) {
			this.p = p ;
		}
		
		@Override
		public void run() {
			mProgrammaticChange = true ;
			mSeekBar.setMax(mSeekBarMax) ;
			if ( p > mSeekBarMax )
				mSeekBar.setProgress(mSeekBarMax) ;
			else if ( p < 0 )
				mSeekBar.setProgress(0) ;
			else
				mSeekBar.setProgress(p) ;
			refreshValue() ;
			mProgrammaticChange = false ;
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// SET ELEMENT VISIBILITY
	//
	// Set whether individual elements are shown or not.
	//
	////////////////////////////////////////////////////////////////////////////
	
	synchronized public void setShow( boolean show ) {
		if ( show )
			mActivity.runOnUiThread( mRunnableSetViewVisible ) ;
		else
			mActivity.runOnUiThread( mRunnableSetViewGone ) ;
	}
	
	synchronized public void setShowValue( boolean show ) {
		if ( show )
			mActivity.runOnUiThread( mRunnableSetValueVisible ) ;
		else
			mActivity.runOnUiThread( mRunnableSetValueGone ) ;
	}
	
	synchronized public void setShowMinMax( boolean show ) {
		if ( show )
			mActivity.runOnUiThread( mRunnableSetMinMaxVisible ) ;
		else
			mActivity.runOnUiThread( mRunnableSetMinMaxGone ) ;
	}
	
	synchronized public void setEnabled( boolean enabled ) {
		if ( enabled )
			mActivity.runOnUiThread( mRunnableSetEnabled ) ;
		else
			mActivity.runOnUiThread( mRunnableSetDisabled ) ;
	}
	
	synchronized public boolean getEnabled() {
		return mEnabled ;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// 
	// SET VALUES
	//
	// Set the values for this labeled seek bar.
	//
	////////////////////////////////////////////////////////////////////////////
	
	synchronized public void setExplicitValues( int [] vals, String [] labels, String minLabel, String maxLabel ) {
		setExplicitValues( vals, null, null, labels, null, null, minLabel, maxLabel ) ;
	}
	
	synchronized public void setExplicitValues( float [] vals, String [] labels, String minLabel, String maxLabel ) {
		setExplicitValues( null, vals, null, labels, null, null, minLabel, maxLabel ) ;
	}
	
	synchronized public void setExplicitValues( Object [] vals, String [] labels, String minLabel, String maxLabel ) {
		setExplicitValues( null, null, vals, labels, null, null, minLabel, maxLabel ) ;
	}
	
	synchronized public void setExplicitValues( int [] vals, String labelName, String [] labelValues, String minLabel, String maxLabel ) {
		setExplicitValues( vals, null, null, null, labelName, labelValues, minLabel, maxLabel ) ;
	}
	
	synchronized public void setExplicitValues( float [] vals, String labelName, String [] labelValues, String minLabel, String maxLabel ) {
		setExplicitValues( null, vals, null, null, labelName, labelValues, minLabel, maxLabel ) ;
	}
	
	synchronized public void setExplicitValues( Object [] vals, String labelName, String [] labelValues, String minLabel, String maxLabel ) {
		setExplicitValues( null, null, vals, null, labelName, labelValues, minLabel, maxLabel ) ;
	}
	
	protected void setExplicitValues(
			int [] iVals, float [] fVals, Object [] oVals,
			String [] fullLabels, String partLabel, String [] partLabelVals,
			String minLabel, String maxLabel ) {
		
		mStyle = Style.EXPLICIT ;
		
		// Set values.
		mValuesInt = iVals ;
		mValuesFloat = fVals ;
		mValuesObject = oVals ;
		
		// Max!
		mSeekBarMax = 0 ;
		if ( mValuesInt != null )
			mSeekBarMax = mValuesInt.length - 1 ;
		else if ( mValuesFloat != null )
			mSeekBarMax = mValuesFloat.length - 1 ;
		else if ( mValuesObject != null )
			mSeekBarMax = mValuesObject.length - 1 ;
		
		// Set labels.
		mValueTextFullLabels = fullLabels ;
		mValueTextPartsLabel = partLabel ;
		mValueTextPartsValues = partLabelVals ;
		mUsingValueFullLabel = mValueTextFullLabels != null ;
		
		// Min/max
		mMinTextLabel = minLabel ;
		mMaxTextLabel = maxLabel ;
		// Set the min/max views
		mActivity.runOnUiThread(mRunnableRefreshMinMax) ;
		
		////////////////////////////////////////////////////////////////////////
		// Next, we will refresh the length of the progress bar and change its
		// value to be within the bounds of the new set of values.
		mActivity.runOnUiThread(mRunnableRefreshSeekBar) ;
		mActivity.runOnUiThread(mRunnableRefreshValue) ;
	}
	
	synchronized public void setRangeValues( int min, int max, int interval, String labelName, String unitsLeft, String unitsRight ) {
		setRangeValues( min, max, interval, 0, labelName, unitsLeft, unitsRight ) ;
	}
	
	
	synchronized public void setRangeValues( int min, int max, int interval, int exponent, String labelName, String unitsLeft, String unitsRight ) {
		mStyle = Style.RANGE ;
		
		// Set values
		mMinValue = min ;
		mMaxValue = max ;
		mInterval = interval ;
		mExponent = exponent ;
		
		this.mUnitsLeft = unitsLeft == null ? "" : unitsLeft ;
		this.mUnitsRight = unitsRight == null ? "" : unitsRight ;
		
		// Max!
		mSeekBarMax = (mMaxValue - mMinValue)/mInterval ;
		
		// Set labels.
		mValueTextPartsLabel = labelName ;
		mUsingValueFullLabel = false ;
		
		// Min/max
		mMinTextLabel = asString( mMinValue, mExponent ) ;
		mMaxTextLabel = asString( mSeekBarMax * mInterval + mMinValue, mExponent ) ;
			
		// Set the min/max views
		mActivity.runOnUiThread( mRunnableRefreshMinMax ) ;
		
		////////////////////////////////////////////////////////////////////////
		// Next, we will refresh the length of the progress bar and change its
		// value to be within the bounds of the new set of values.
		mActivity.runOnUiThread(mRunnableRefreshSeekBar) ;
		mActivity.runOnUiThread(mRunnableRefreshValue) ;
	}
	
		
	private String asString( int num, int exponent ) {
		if ( exponent == 0 ) {
			return String.valueOf(num) ;
		} else if ( exponent > 0 ) {
			long mult = 1 ;
			for ( int i = 0; i < exponent; i++ ) {
				mult *= 10 ;
			}
			return String.valueOf(num * mult) ;
		} else {
			
			boolean neg = false ;
			if ( num < 0 ) {
				neg = true ;
				num = -num ;
			}

			long div = 1 ;
			for ( int i = 0; i < -exponent; i++ ) {
				div *= 10 ;
			}
			int wholeComponent = (int)(num / div) ;
			int realComponent = (int)(num % div) ;
			
			// how many zeros?
			int zeros = 0 ;
			while ( div / 10 > realComponent ) {
				zeros++ ;
				div /= 10 ;
			}
			
			StringBuilder sb = new StringBuilder() ;
			if ( neg )
				sb.append("-") ;
			sb.append(wholeComponent).append(".") ;
			for ( int i = 0; i < zeros; i++ )
				sb.append("0") ;
			if ( realComponent > 0 )
				sb.append(realComponent) ;
			
			return sb.toString() ;
		}
	}
		

	synchronized protected void refreshValue() {
		int progress = mSeekBar.getProgress() ;
		
		if ( mStyle == Style.EXPLICIT ) {
			if ( mUsingValueFullLabel ) {
				String label = null ;
				if ( mValueTextFullLabels != null && mValueTextFullLabels.length > progress )
					label = mValueTextFullLabels[progress] ;
				if ( mValueAreaFullLabel != null ) {
					mValueAreaFullLabel.setText(label) ;
					mValueAreaFullLabel.setVisibility(View.VISIBLE) ;
				}
				if ( mValueAreaParts != null )
					mValueAreaParts.setVisibility(View.GONE) ;
			}
			else {
				String label = null ;
				if ( mValueTextPartsValues != null && mValueTextPartsValues.length > progress )
					label = mValueTextPartsValues[progress] ;
				if ( mValueAreaPartsLabel != null )
					mValueAreaPartsLabel.setText(mValueTextPartsLabel) ;
				if ( mValueAreaFullLabel != null )
					mValueAreaFullLabel.setVisibility(View.GONE) ;
				if ( mValueAreaPartsValue != null )
					mValueAreaPartsValue.setText(label) ;
				if ( mValueAreaParts != null )
					mValueAreaParts.setVisibility(View.VISIBLE) ;
			}
		}
		
		else if ( mStyle == Style.RANGE ) {
			int val = intValue() ;
			String label = mUnitsLeft + asString(val, mExponent) + mUnitsRight ;
			if ( mValueAreaFullLabel != null ) {
				mValueAreaFullLabel.setVisibility(View.GONE) ;
			}
			if ( mValueAreaPartsValue != null )
				mValueAreaPartsValue.setText(label) ;
			if ( mValueAreaParts != null )
				mValueAreaParts.setVisibility(View.VISIBLE) ;
			
			if ( mValueAreaPartsLabel != null )
				mValueAreaPartsLabel.setText(mValueTextPartsLabel) ;
		}
	}
	

	@Override
	synchronized public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		
		if ( seekBar != mSeekBar ) {
			Log.e(TAG, "onProgressChanged with seekBar " + seekBar + ", where mSeekBar is " + mSeekBar) ;
			return ;
		}
		
		if ( mProgrammaticChange )
			return ;
		
		// Assume this is called from the UI thread.
		refreshValue() ;
		
		// Tell listener
		if ( listener != null ) {
			if ( mStyle == Style.EXPLICIT ) {
				if ( mValuesInt != null )
					listener.onProgressChanged(this, mSeekBar, progress, fromUser, mValuesInt[progress]) ;
				else if ( mValuesFloat != null )
					listener.onProgressChanged(this, mSeekBar, progress, fromUser, mValuesFloat[progress]) ;
				else if ( mValuesObject != null )
					listener.onProgressChanged(this, mSeekBar, progress, fromUser, mValuesObject[progress]) ;
				else
					listener.onProgressChanged(this, mSeekBar, progress, fromUser, null) ;
			} else if ( mStyle == Style.RANGE ) {
				listener.onProgressChanged(this, mSeekBar, progress, fromUser, intValue()) ;
			}
		}
	}


	@Override
	synchronized public void onStartTrackingTouch(SeekBar seekBar) {
		if ( seekBar != mSeekBar ) {
			Log.e(TAG, "onStartTrackingTouch with seekBar " + seekBar + ", where mSeekBar is " + mSeekBar) ;
			return ;
		}
		
		if ( listener != null && !mProgrammaticChange )
			listener.onStartTrackingTouch(this, mSeekBar) ;
	}


	@Override
	synchronized public void onStopTrackingTouch(SeekBar seekBar) {
		if ( seekBar != mSeekBar ) {
			Log.e(TAG, "onStopTrackingTouch with seekBar " + seekBar + ", where mSeekBar is " + mSeekBar) ;
			return ;
		}
		
		if ( listener != null && !mProgrammaticChange )
			listener.onStopTrackingTouch(this, mSeekBar) ;
	}
	
}
