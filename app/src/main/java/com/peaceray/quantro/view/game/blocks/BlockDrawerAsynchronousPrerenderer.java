package com.peaceray.quantro.view.game.blocks;

import java.lang.ref.WeakReference;

import com.peaceray.quantro.view.game.AnimationSettings;
import com.peaceray.quantro.view.game.DrawSettings;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * A class for asynchronously prerendering a BlockDrawer using
 * provided DrawSettings objects.  A Prerenderer should be attached
 * to a particular BlockDrawer.  Like the asynchronous GameSaver instance,
 * a listener can be attached to the Prerenderer if you want an update at
 * the moment prerendering concludes.
 * 
 * Prererenderers can also be asked specifically whether they are busy.
 * 
 * @author Jake
 *
 */
public class BlockDrawerAsynchronousPrerenderer {

	public interface Listener {
		public void bdapl_prerendered(
				BlockDrawerAsynchronousPrerenderer bdap,
				BlockDrawer bd,
				Object tag ) ;
		
		public void bdapl_prerenderError(
				BlockDrawerAsynchronousPrerenderer bdap,
				BlockDrawer bd,
				Object tag, Exception exception ) ;
		
		public void bdapl_recycled(
				BlockDrawerAsynchronousPrerenderer bdap,
				BlockDrawer bd,
				Object tag ) ;
		
		
	}
	
	private class WorkThread extends Thread {
		
		private static final int WT_WHAT_SET_DRAW_SETTINGS_AND_PRERENDER = 0 ;
		private static final int WT_WHAT_SET_DRAW_AND_ANIMATION_SETTINGS_AND_PRERENDER = 1 ;
		
		private static final int WT_WHAT_RECYCLE = 2;
		private static final int WT_NUM_WHAT = 3 ;
		
		
		Handler mHandler ;
		WeakReference<Context> mwrContext ;
		Listener mListener ;
		Object mTag ;
		
		private WorkThread( Context context, Listener listener, Object tag ) {
			mwrContext = new WeakReference<Context>(context) ;
			mListener = listener ;
			mTag = tag ;
		}
		
		public void run() {
			Looper.prepare();
			mHandler = new Handler() {
				@Override
				public void handleMessage(android.os.Message msg) {
					Context context = mwrContext.get() ;
					if ( context == null )
						return ;
					
					int what = msg.what ;
					Object obj = msg.obj ;
					BlockDrawer blockDrawer = unwrapBlockDrawer( what, obj ) ;
					BlockDrawer blockDrawerMaster = unwrapBlockDrawerMaster( what, obj ) ;
					DrawSettings ds = unwrapDrawSettings( what, obj ) ;
					AnimationSettings as = unwrapAnimationSettings( what, obj ) ;
					
					Exception error = null ;
					
					switch( what ) {
					case WT_WHAT_SET_DRAW_SETTINGS_AND_PRERENDER:
						try {
							if ( blockDrawerMaster == null ) {
								blockDrawer.setDrawSettingsAndPrerender(context, ds) ;
							} else {
								blockDrawer.setDrawSettingsAndPrerenderAsSlave(context, ds, blockDrawerMaster) ;
							}
						} catch ( Exception e ) {
							error = e ;
						} finally {
							mBusy = false ;
							if ( mListener != null ) {
								if ( error == null ) {
									mListener.bdapl_prerendered(BlockDrawerAsynchronousPrerenderer.this,
											blockDrawer, mTag) ;
								} else {
									mListener.bdapl_prerenderError(BlockDrawerAsynchronousPrerenderer.this,
											blockDrawer, mTag, error) ;
								}
							}
						}
						getLooper().quit() ;
						break ;
						
					case WT_WHAT_SET_DRAW_AND_ANIMATION_SETTINGS_AND_PRERENDER:
						blockDrawer.setAnimationSettings(as) ;
						if ( blockDrawerMaster == null ) {
							blockDrawer.setDrawSettingsAndPrerender(context, ds) ;
						} else {
							blockDrawer.setDrawSettingsAndPrerenderAsSlave(context, ds, blockDrawerMaster) ;
						}
						mBusy = false ;
						if ( mListener != null )
							mListener.bdapl_prerendered(BlockDrawerAsynchronousPrerenderer.this,
									blockDrawer, mTag) ;
						getLooper().quit() ;
						break ;
						
					case WT_WHAT_RECYCLE:
						blockDrawer.setDrawSettingsAndPrerender(context, null) ;
						mBusy = false ;
						if ( mListener != null )
							mListener.bdapl_recycled(BlockDrawerAsynchronousPrerenderer.this,
									blockDrawer, mTag) ;
						getLooper().quit() ;
						break ;
					}
				}
			} ;
			
			Looper.loop() ;
		}
	}
	
	
	protected static final Object wrapForMessage( int what,
			BlockDrawer bdRender, BlockDrawer bdMaster, DrawSettings ds, AnimationSettings as ) {
		switch ( what ) {
		case WorkThread.WT_WHAT_SET_DRAW_SETTINGS_AND_PRERENDER:
			return new Object[] { bdRender, bdMaster, ds } ;
		case WorkThread.WT_WHAT_SET_DRAW_AND_ANIMATION_SETTINGS_AND_PRERENDER:
			return new Object[] { bdRender, bdMaster, ds, as } ;
		case WorkThread.WT_WHAT_RECYCLE:
			return bdRender ;
		}
		
		throw new IllegalArgumentException("Don't recognize message 'what' code " + what) ;
	}
	
	
	protected static final BlockDrawer unwrapBlockDrawer( int what, Object obj ) {
		switch ( what ) {
		case WorkThread.WT_WHAT_SET_DRAW_SETTINGS_AND_PRERENDER:
		case WorkThread.WT_WHAT_SET_DRAW_AND_ANIMATION_SETTINGS_AND_PRERENDER:
			return (BlockDrawer)((Object[])obj)[0] ;
		case WorkThread.WT_WHAT_RECYCLE:
			return (BlockDrawer)obj ;
		}
		
		throw new IllegalArgumentException("Don't recognize message 'what' code " + what) ;
	}
	
	protected static final BlockDrawer unwrapBlockDrawerMaster( int what, Object obj ) {
		switch ( what ) {
		case WorkThread.WT_WHAT_SET_DRAW_SETTINGS_AND_PRERENDER:
		case WorkThread.WT_WHAT_SET_DRAW_AND_ANIMATION_SETTINGS_AND_PRERENDER:
			return (BlockDrawer)((Object[])obj)[1] ;
		case WorkThread.WT_WHAT_RECYCLE:
			return null ;
		}
		
		throw new IllegalArgumentException("Don't recognize message 'what' code " + what) ;
	}
	
	protected static final DrawSettings unwrapDrawSettings( int what, Object obj ) {
		switch ( what ) {
		case WorkThread.WT_WHAT_SET_DRAW_SETTINGS_AND_PRERENDER:
		case WorkThread.WT_WHAT_SET_DRAW_AND_ANIMATION_SETTINGS_AND_PRERENDER:
			return (DrawSettings)((Object[])obj)[2] ;
		case WorkThread.WT_WHAT_RECYCLE:
			return null ;
		}
		
		throw new IllegalArgumentException("Don't recognize message 'what' code " + what) ;
	}
	
	protected static final AnimationSettings unwrapAnimationSettings( int what, Object obj ) {
		switch ( what ) {
		case WorkThread.WT_WHAT_SET_DRAW_SETTINGS_AND_PRERENDER:
			return null ;
		case WorkThread.WT_WHAT_SET_DRAW_AND_ANIMATION_SETTINGS_AND_PRERENDER:
			return (AnimationSettings)((Object[])obj)[3] ;
		case WorkThread.WT_WHAT_RECYCLE:
			return null ;
		}
		
		throw new IllegalArgumentException("Don't recognize message 'what' code " + what) ;
	}
	
	
	
	
	protected boolean mBusy ;
	
	public BlockDrawerAsynchronousPrerenderer() {
		mBusy = false ;
	}
	
	public synchronized boolean busy() {
		return mBusy ;
	}
	
	public synchronized boolean prerender( Context context, Listener listener, Object tag, BlockDrawer bd, DrawSettings ds ) {
		return prerender( context, listener, tag, bd, null, ds) ;
	}
	
	
	public synchronized boolean prerender( Context context, Listener listener, Object tag, BlockDrawer bd, DrawSettings ds, AnimationSettings as ) {
		return prerender( context, listener, tag, bd, null, ds, as) ;
	}
	
	
	public synchronized boolean prerender( Context context, Listener listener, Object tag, BlockDrawer bd, BlockDrawer master, DrawSettings ds ) {
		if ( mBusy )
			return false ;
		
		mBusy = true ;
		WorkThread thread = new WorkThread( context, listener, tag ) ;
		thread.start() ;
		while ( thread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		
		int what = WorkThread.WT_WHAT_SET_DRAW_SETTINGS_AND_PRERENDER ;
		thread.mHandler.sendMessage(
				thread.mHandler.obtainMessage(
						what,
						wrapForMessage(what, bd, master, ds, null))) ;
		return true ;
	}
	
	
	public synchronized boolean prerender( Context context, Listener listener, Object tag, BlockDrawer bd, BlockDrawer master, DrawSettings ds, AnimationSettings as ) {
		if ( mBusy )
			return false ;
		
		mBusy = true ;
		WorkThread thread = new WorkThread( context, listener, tag ) ;
		thread.start() ;
		while ( thread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		
		int what = WorkThread.WT_WHAT_SET_DRAW_AND_ANIMATION_SETTINGS_AND_PRERENDER ;
		thread.mHandler.sendMessage(
				thread.mHandler.obtainMessage(
						what,
						wrapForMessage(what, bd, master, ds, as))) ;
		return true ;
	}
	
	
	
	
	
	public synchronized boolean recycle( Context context, Listener listener, Object tag, BlockDrawer bd ) {
		if ( mBusy )
			return false ;
		
		mBusy = true ;
		WorkThread thread = new WorkThread( context, listener, tag ) ;
		thread.start() ;
		while ( thread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		
		int what = WorkThread.WT_WHAT_RECYCLE ;
		thread.mHandler.sendMessage(
				thread.mHandler.obtainMessage(
						what,
						wrapForMessage(what, bd, null, null, null))) ;
		return true ;
	}
	
	
}
