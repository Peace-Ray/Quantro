package com.peaceray.quantro.view.game;

import com.peaceray.quantro.model.game.GameBlocksSlice;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.game.blocks.BlockDrawer;
import com.peaceray.quantro.view.game.blocks.BlockDrawerAsynchronousPrerenderer;
import com.peaceray.quantro.view.game.blocks.BlockDrawerSliceTime;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigCanvas;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

/**
 * A View which displays a game's block field.  This View is designed to be
 * positioned within any static layout.  Unlike GameView, this View does not
 * perform game updates.  It draws a single, static field view, using the
 * standard onMeasure, onLayout, onDraw process.  In other words, this View
 * is effectively a very simple wrapper for BlockDrawer.
 * 
 * Future direction: we may want a View that performs simple animation loops.
 * 		There are two basic ways to do this.  First, we could extend BlockFieldView
 * 		to play a very specific animation.  Second, we could extend GameView (or configure
 * 		the game with a TriggerSystem) that will repeat the same actions continuously.
 * 
 * @author Jake
 *
 */
public class BlockFieldView extends View
		implements BlockDrawerAsynchronousPrerenderer.Listener,
					QuantroContentWrappingButton.SelfUpdatingButtonContent{
	
	private static final String TAG = "BlockFieldView" ;

	
	public BlockFieldView(Context context) {
		super(context);
		
		// Set basic defaults
		constructor_setDefaultValues(context) ;
	}
	
	public BlockFieldView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		constructor_setDefaultValues(context) ;
	}
	
	public BlockFieldView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		constructor_setDefaultValues(context) ;
	}
	
	private void constructor_setDefaultValues(Context context) {
		l = t = 0 ;
		r = b = 100 ;
		
		mSlice = null ;
		mBlockDrawer = null ;
		
		mBlockDrawer = new BlockDrawer( context, null, new AnimationSettings() ) ;
		mSliceTime = new BlockDrawerSliceTime() ;
		mPrerenderer = new BlockDrawerAsynchronousPrerenderer() ;
		
		VersionSafe.disableHardwareAcceleration(this) ;
	}
	
	// Data from onLayout.
	int l,t,r,b ;
	
	GameBlocksSlice mWrapperSlice ;
	
	// We can also draw slices!
	long mTimeSliceSet ;
	GameBlocksSlice mSlice ;
	BlockDrawerSliceTime mSliceTime ;
	boolean mSliceNew ;
	long mTimeLastDraw ;
	boolean mStillAnimatingSlice ;
	
	// blocks to fit.  If < 0, fit the whole thing.
	int mWidthInBlocks ;
	int mHeightInBlocks ;
	
	QuantroContentWrappingButton mQCWB ;
	
	DrawSettings mDrawSettings = null ;
	
	BlockDrawer mBlockDrawer = null ;
	BlockDrawerAsynchronousPrerenderer mPrerenderer = null ;
	boolean mPrerendered = false ;
	boolean mPrerenderingQueued = false ;
	boolean mRecyclingQueued = false ;
	Object mPrerendererMutex = new Object() ;
	
	
	synchronized private boolean hasContentToDraw() {
		return mSlice != null || mWrapperSlice != null ;
	}
	
	
	synchronized public void resetContentSizeInBlocks() {
		mWidthInBlocks = -1 ;
		mHeightInBlocks = -1 ;
		
		postInvalidate() ;
		adjustDrawSettings() ;
	}
	
	synchronized public void setContentSizeInBlocks( int width, int height ) {
		mWidthInBlocks = width ;
		mHeightInBlocks = height ;
		
		postInvalidate() ;
		adjustDrawSettings() ;
	}
	
	synchronized public void setContent( DrawSettings settings, byte [][][] blockField ) {
		if ( settings != null )
			mDrawSettings = new DrawSettings(settings) ;
		else
			mDrawSettings = null ;
		
		mWrapperSlice = wrapInSlice( mWrapperSlice, blockField ) ;
		mSlice = null ;
		
		postInvalidate() ;
		adjustDrawSettings() ;
	}
	
	
	synchronized public void setContent( DrawSettings settings, byte [][][] blockField, int pieceType, byte [][][] pieceBlockField, int numGhostComponents, byte [][][][] ghostBlockField ) {
		if ( settings != null )
			mDrawSettings = new DrawSettings(settings) ;
		else
			mDrawSettings = null ;
		
		mWrapperSlice = wrapInSlice( mWrapperSlice, blockField, pieceType, pieceBlockField, numGhostComponents, ghostBlockField ) ;
		
		mSlice = null ;
		
		postInvalidate() ;
		adjustDrawSettings() ;
	}
	
	
	synchronized public void setContent( DrawSettings settings, GameBlocksSlice gbs ) {
		if ( settings != null )
			mDrawSettings = new DrawSettings(settings) ;
		else
			mDrawSettings = null ;
		
		setContent(gbs) ;
		adjustDrawSettings() ;
	}
	
	
	private GameBlocksSlice wrapInSlice( GameBlocksSlice slice, byte [][][] blockField ) {
		int buf = mDrawSettings != null ? mDrawSettings.blockFieldOuterBuffer : 1 ;
		int R = blockField[0].length - 2 * buf ;
		int C = blockField[0][0].length - 2 * buf ;
		if ( slice == null
				|| slice.rows() != R
				|| slice.cols() != C
				|| slice.edge() != buf ) {
			
			slice = new GameBlocksSlice(
					R, C, 4, buf ) ;
		}
		
		slice.setBlocksState(GameBlocksSlice.BLOCKS_STABLE) ;
		slice.setPieceType(0) ;
		ArrayOps.copyInto( blockField, slice.getBlockfieldStable() ) ;
		
		return slice ;
	}
	
	
	private GameBlocksSlice wrapInSlice( GameBlocksSlice slice, byte [][][] blockField, int pieceType,
			byte [][][] pieceBlockField, int numGhostComponents, byte [][][][] ghostBlockField ) {
		int buf = mDrawSettings != null ? mDrawSettings.blockFieldOuterBuffer : 1 ;
		int R = blockField[0].length - 2 * buf ;
		int C = blockField[0][0].length - 2 * buf ;
		if ( slice == null
				|| slice.rows() != R
				|| slice.cols() != C
				|| slice.edge() != buf
				|| slice.num() < numGhostComponents ) {
			
			slice = new GameBlocksSlice(
					R, C, numGhostComponents + 4, buf ) ;
		}
		
		slice.setBlocksState(GameBlocksSlice.BLOCKS_PIECE_FALLING) ;
		slice.setPieceType(pieceType) ;
		ArrayOps.copyInto( blockField, slice.getPieceFallingBlockfield() ) ;
		ArrayOps.copyInto( pieceBlockField, slice.getPieceFallingPiece() ) ;
		
		// attempt to make "stable:" piece + blockfield.  A little more difficult
		// w/o a LockSystem but we'll manage.
		byte [][][] stable = slice.getBlockfieldStable() ;
		ArrayOps.copyInto( blockField, stable ) ;
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( int r = 0; r < stable[0].length; r++ ) {
				for ( int c = 0; c < stable[0][0].length; c++ ) {
					if ( pieceBlockField[qp][r][c] != 0 )
						stable[qp][r][c] = pieceBlockField[qp][r][c] ;
				}
			}
		}
		
		byte [][][][] components = slice.getPieceFallingGhosts() ;
		for ( int i = 0; i < numGhostComponents; i++ ) {
			ArrayOps.copyInto( ghostBlockField[i], components[0] ) ;
		}
		slice.setNumPieceGhosts(numGhostComponents) ;
		
		return slice ;
	}
	
	
	
	
	
	
	synchronized public void setDrawSettings( DrawSettings settings ) {
		if ( settings != null )
			mDrawSettings = new DrawSettings(settings) ;
		else
			mDrawSettings = null ;
		
		adjustDrawSettings() ;
	}
	
	synchronized public void setContent( GameBlocksSlice gbs ) {
		// for now, take vals if possible.
		if ( mSlice != null && mSlice.fits(gbs) )
			mSlice.takeVals(gbs) ;
		else if ( gbs != null )
			mSlice = new GameBlocksSlice(gbs) ;
		else
			mSlice = null ;
		
		mStillAnimatingSlice = mSlice != null ;
		mSliceNew = true ;
		mTimeSliceSet = System.currentTimeMillis() ;
		
		postInvalidate() ;
	}
	
	synchronized public void setContentReference( DrawSettings settings, GameBlocksSlice gbs ) {
		if ( settings != null )
			mDrawSettings = new DrawSettings(settings) ;
		else
			mDrawSettings = null ;
		
		setContentReference(gbs) ;
		adjustDrawSettings() ;
	}
	
	synchronized public void setContentReference( GameBlocksSlice gbs ) {
		mSlice = gbs ;
		if ( mBlockDrawer != null )
			mBlockDrawer.nextGameBlocksSliceMayBeInconsistent() ;
		
		mSliceNew = true ;
		mTimeSliceSet = System.currentTimeMillis() ;
		
		postInvalidate() ;
	}

	/**
	 * Recycles the blockDrawer at the next available opportunity.  Settings
	 * will be automatically re-generated upon a call to onDraw (asynchronously
	 * - this will not cause a delay) or the next time a DrawSettings object is
	 * provided, whichever is sooner.  This is primarily useful after setting
	 * the view to GONE, in case you want to keep the view around without taking
	 * too much memory.
	 */
	synchronized public void recycleUntilDraw() {
		synchronized( mPrerendererMutex ) {
			Context context = getContext() ;
			// If we are not prerendering, do so now.  If we are, set that we need to prerender
			mPrerenderingQueued = false ;
			if ( !mPrerenderer.busy() ) {
				mPrerenderer.recycle(context, this, null, mBlockDrawer) ;
			} else {
				mRecyclingQueued = true ;
			}
		}
	}
	
	
	@Override
	synchronized protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Apply the measure spec in determining the size of our content.  Take the cols
		// and displayed rows to get the width/height ratio we need.  From that, take 
		// our most limiting dimension and and scale the other.
		Context context = getContext() ;
		
		int modeWidth = MeasureSpec.getMode(widthMeasureSpec) ;
		int maxWidth ;
		if ( modeWidth == MeasureSpec.UNSPECIFIED ) {
			Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			maxWidth = display.getWidth() ;
		}
		else
			maxWidth = MeasureSpec.getSize(widthMeasureSpec) ;
		
		int modeHeight = MeasureSpec.getMode(heightMeasureSpec) ;
		int maxHeight ;
		if ( modeHeight == MeasureSpec.UNSPECIFIED ) {
			Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			maxHeight = display.getHeight() ;
		}
		else
			maxHeight = MeasureSpec.getSize(heightMeasureSpec) ;
		
		int width = maxWidth ;
		int height = maxHeight ;
		
		// Without draw settings, just take what they give us.
		// With them, though, if we are using squareBlocks, 
		// use the number of columns and displayed rows to get
		// a width/height ratio, and scale width and height according
		// to it so the whole thing fits within the space available.
		if ( mDrawSettings != null && mDrawSettings.squareBlocks ) {
			float widthToHeight ;
			if ( mWidthInBlocks > 0 && mHeightInBlocks > 0 )
				widthToHeight = (float)mWidthInBlocks / (float)mHeightInBlocks ;
			else
				widthToHeight = (float)mDrawSettings.COLS / (float)mDrawSettings.rowsToFit ;
			int horizPadding = getPaddingLeft() + getPaddingRight() ;
			int vertPadding = getPaddingTop() + getPaddingBottom() ;
			
			width = (int)Math.floor( Math.min(width, (height-vertPadding) * widthToHeight + horizPadding) ) ;
			height = (int)Math.floor( Math.min(height, (width-horizPadding) / widthToHeight + vertPadding) ) ;
		}
		
		if ( modeWidth == MeasureSpec.EXACTLY )
			width = maxWidth ;
		if ( modeHeight == MeasureSpec.EXACTLY )
			height = maxHeight ;
		
		setMeasuredDimension(width, height) ;
	}
	
	
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b) ;
		
		if ( changed ) {
			this.l = l ;
			this.t = t ; 
			this.r = r ;
			this.b = b ;
			
			adjustDrawSettings() ;
		}
	}
	
	
	
	@Override
	synchronized public void onDraw( Canvas canvas ) {
		super.onDraw(canvas) ;
		
		// Draw based on what we have.  Only draw if we are not currently
		// prerendering; otherwise the UI thread blocks until prerendering
		// is finished.
		
		synchronized( mPrerendererMutex ) {
			Context context = getContext() ;
			
			if ( !mPrerenderer.busy() && mBlockDrawer != null && mDrawSettings != null ) {
				if ( mPrerendered && hasContentToDraw() ) {
					if ( mWrapperSlice != null || mSlice != null ) {
						GameBlocksSlice slice = mWrapperSlice != null ? mWrapperSlice : mSlice ;
						long curTime = System.currentTimeMillis() ;
						if ( mSliceNew )
							mBlockDrawer.advanceNewSliceInForeground(slice, mSliceTime, curTime - mTimeLastDraw, false) ;
						else
							mBlockDrawer.advanceSliceInForeground(slice, mSliceTime, curTime - mTimeLastDraw, false) ;
						mStillAnimatingSlice = !mBlockDrawer.drawGameBlocksSlice(canvas, null, slice, mSliceTime) ;
						mTimeLastDraw = curTime ;
						mSliceNew = false ;
						if ( mStillAnimatingSlice )
							sliceDrawn() ;
						else
							sliceDrawnAnimationFinished() ;
					}
				} else if ( hasContentToDraw() && mDrawSettings.configCanvas.region.width() > 0 && mDrawSettings.configCanvas.region.height() > 0 ) {
					// prerender!
					mPrerenderer.prerender(context, this, null, mBlockDrawer, mDrawSettings) ;
				}
			}
		}
		
	}
	
	
	/**
	 * Redraws the most recent frame to the provided Canvas, using ConfigCanvas
	 * as its settings.  Useful for screen-captures.
	 * 
	 * @param canvas
	 * @param configCanvas
	 * @return
	 */
	synchronized public boolean redrawFrameTo( Canvas canvas, BlockDrawerConfigCanvas configCanvas ) {
		synchronized( mPrerendererMutex ) {
			if ( !mPrerenderer.busy() && mBlockDrawer != null && mDrawSettings != null ) {
				if ( mPrerendered && hasContentToDraw() ) {
					if ( mWrapperSlice != null || mSlice != null ) {
						mBlockDrawer.redrawLastGameBlocksSlice(canvas, configCanvas) ;
						return true ;
					}
				}
			}
		}
		
		return false ;
	}
	
	protected void sliceDrawn() {
		// nothing; here for overridden
	}
	
	protected void sliceDrawnAnimationFinished() {
		// nothing; here to be overridden
	}
	
	
	synchronized private void adjustDrawSettings() {
		if ( mDrawSettings == null )
			return ;
		
		int bfL = getPaddingLeft() ;
		int bfT = getPaddingTop() ;
		int bfR = (r-l) - getPaddingRight() ;
		int bfB = (b-t) - getPaddingBottom() ;
		
		// do our work in a COPY of mDrawSettings; otherwise we could alter
		// a DrawSettings object currently being prerendered, which would be Bad.
		DrawSettings ds = new DrawSettings(mDrawSettings) ;
		
		// we set our drawSettings based on the available drawing space.
		// However, if using square blocks, shrink the appropriate dimension
		// so we fit in the center of the available area.
		if ( !ds.squareBlocks ) 
			ds.setCanvasTarget(ds.getBlit(), ds.getScale(), bfL, bfT, bfR-bfL, bfB-bfT) ;
		else {
			// TODO: support gravity?
			float widthToHeight ;
			if ( mWidthInBlocks > 0 && mHeightInBlocks > 0 )
				widthToHeight = (float)mWidthInBlocks / (float)mHeightInBlocks ;
			else
				widthToHeight = (float)mDrawSettings.COLS / (float)mDrawSettings.rowsToFit ;
			
			int width = Math.round( Math.min( bfR-bfL, (bfB-bfT) * widthToHeight ) ) ;
			int height = Math.round( Math.min( bfB-bfT, (bfR-bfL) / widthToHeight ) ) ;
			
			int wInset = ((bfR-bfL) - width) / 2 ;
			int hInset = ((bfB-bfT) - height) / 2 ;
			ds.setCanvasTarget(ds.getBlit(), ds.getScale(),
					bfL + wInset,
					bfT + hInset,
					bfL + wInset + width,
					bfT + hInset + height) ;
			
		}
		
		if ( mWidthInBlocks > 0 && mHeightInBlocks > 0 )
			ds.setBlockSizesToFit(mHeightInBlocks, mWidthInBlocks) ;
		else
			ds.setBlockSizesToFit() ;
		
		// Log.d(TAG, "have sized to " + ds.drawRegion + " with margin " + ds.horizontalMargin + ", " + ds.verticalMargin) ;
		
		synchronized( mPrerendererMutex ) {
			Context context = getContext() ;
			// If we are not prerendering, do so now.  If we are, set that we need to prerender
			// so we do it upon the previous completion.
			mDrawSettings = ds ;
			if ( hasContentToDraw() && ds.configCanvas.region.width() > 0 && ds.configCanvas.region.height() > 0 ) {
				if ( !mPrerenderer.busy() ) {
					AnimationSettings as = mDrawSettings.drawAnimations == DrawSettings.DRAW_ANIMATIONS_ALL
							? new AnimationSettings()
							: null ;
					mPrerenderer.prerender(context, this, null, mBlockDrawer, mDrawSettings, as) ;
				} else {
					mPrerenderingQueued = true ;
				}
			} else {
				mPrerendered = false ;
			}
		}
	}
	
	
	public void bdapl_prerendered(
			BlockDrawerAsynchronousPrerenderer bdap,
			BlockDrawer bd, Object tag ) {
		
		synchronized( mPrerendererMutex ) {
			Context context = getContext() ;
			
			mPrerendered = true ;
			
			// If we need to prerender, another DrawSettings object has
			// been prepared since this call started.  Prerender it.
			// Otherwise, we are done; set mPrerendering and postInvalidate().
			if ( mPrerenderingQueued && mDrawSettings.configCanvas.region.width() > 0 && mDrawSettings.configCanvas.region.height() > 0 ) {
				AnimationSettings as = mDrawSettings.drawAnimations == DrawSettings.DRAW_ANIMATIONS_ALL
						? new AnimationSettings()
						: null ;
				mPrerenderer.prerender(context, this, null, mBlockDrawer, mDrawSettings, as) ;
				mPrerenderingQueued = false ;
			} else if ( mRecyclingQueued ) {
				mPrerenderer.recycle(context, this, null, mBlockDrawer) ;
				mRecyclingQueued = false ;
			} else {
				postInvalidate() ;
				if ( mQCWB != null )
					mQCWB.invalidateContent(this) ;
			}
 		}
	}
	
	public void bdapl_prerenderError(
			BlockDrawerAsynchronousPrerenderer bdap,
			BlockDrawer bd,
			Object tag, Exception exception ) {
		// we ignore this.  Who wants to special-case this crap?
		// It's here for GameView.
		
	}
	
	public void bdapl_recycled(
			BlockDrawerAsynchronousPrerenderer bdap,
			BlockDrawer bd, Object tag ) {
		
		synchronized( mPrerendererMutex ) {
			
			Context context = getContext() ;
			
			mPrerendered = false ;
			
			// If we need to prerender, another DrawSettings object has
			// been prepared since this call started.  Prerender it.
			// Otherwise, we are done; set mPrerendering and postInvalidate().
			if ( mPrerenderingQueued && mDrawSettings.configCanvas.region.width() > 0 && mDrawSettings.configCanvas.region.height() > 0 ) {
				AnimationSettings as = mDrawSettings.drawAnimations == DrawSettings.DRAW_ANIMATIONS_ALL
						? new AnimationSettings()
						: null ;
				mPrerenderer.prerender(context, this, null, mBlockDrawer, mDrawSettings, as) ;
				mPrerenderingQueued = false ;
			} else if ( mRecyclingQueued ) {
				mPrerenderer.recycle(context, this, null, mBlockDrawer) ;
				mRecyclingQueued = false ;
			}
 		}
	}
	

	@Override
	public void setContainingButton(QuantroContentWrappingButton qcwb) {
		// we wrap this to avoid a null-pointer exception when prerendering is done.
		synchronized( mPrerendererMutex ) {
			mQCWB = qcwb ;
		}
	}
	
}
