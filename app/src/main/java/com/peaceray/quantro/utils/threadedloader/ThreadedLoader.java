package com.peaceray.quantro.utils.threadedloader;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Queue;


/**
 * ThreadedLoader: an abstract class for loading large assets or resources
 * "in the background" rather than in-place for a UI thread.  It is modeled,
 * in basic principle, after the GameSaver thumbnail loader -- in fact,
 * the plan is to refactor that class as a concrete subclass of this.
 * 
 * The basic principle is that individual users will request a load using
 * the 'Params' inner class, with instances of that class containing all 
 * necessary information for the load to take place without future queries
 * to the requester.
 * 
 * After the load (successful or not), the requester receives a callback
 * from this class.  This is the only interaction between the requester
 * and this object after the initial request.
 * 
 * @author Jake
 *
 */
public abstract class ThreadedLoader {

	public static class Params {
		
	}
	
	
	public static abstract class Result {
		
		private boolean mSuccess ;
		
		protected Result setSuccess( boolean success ) {
			mSuccess = success ;
			return this ;
		}
		
		public boolean getSuccess() {
			return mSuccess ;
		}
		
	}
	
	
	/**
	 * A "Client" is an entity requesting a threaded load.
	 * @author Jake
	 *
	 */
	public interface Client {
		
		/**
		 * This client's place in line has been reached.  What
		 * parameters should we use to load a thumbnail?  Populate
		 * the provided object with the appropriate settings.
		 * 
		 * If 'false' is returned, we assume the Client no longer wants
		 * the load to happen and move immediately to the next Client.
		 * 
		 * @param tl
		 * @return
		 */
		public boolean tlc_setParams( ThreadedLoader tl, Params p ) ;
		
		/**
		 * The load is finished.  Check the result for success or failure.
		 * 
		 * The parameters instance returned by tlc_getParams is provided,
		 * in case you need that information.
		 * 
		 * NOTE: Perform your processing as quickly as possible here.  It
		 * is bad practice to monopolize this thread.
		 * 
		 * @param tl
		 * @param p
		 * @param b
		 * @return
		 */
		public void tlc_finished( ThreadedLoader tl, Params p, Result r ) ;
		
	}
	
	
	
	private class ThreadedLoaderThread extends Thread {
		
		private boolean mRunning ;
		
		private ThreadedLoaderThread() {
			super() ;
			mRunning = true ;
		}
		
		@Override
		public void run() {
			
			// loop forever -- until we break.
			while( true ) {
				Client client ;
				synchronized( mQueue ) {
					// verify that we are the current thread for our 
					// container instance.
					if ( mThread != this ) {
						mRunning = false ;
						break ;
					}
					
					// Get a client, if we can.
					WeakReference<Client> wrClient = mQueue.poll() ;
					if ( wrClient == null ) {
						mRunning = false ;
						break ;
					}
					client = wrClient.get() ;
					if ( client == null )
						continue ;
				}
				
				// 'client' has been set.  Perform our operations.
				Params params = newParams() ;
				boolean doLoad = client.tlc_setParams(ThreadedLoader.this, params) ;
				if ( !doLoad )
					continue ;
				
				// perform the load.  This is the main workhorse of the thread.
				Result result = load( params ) ;
				
				// tell the client.
				client.tlc_finished(ThreadedLoader.this, params, result) ;
			}
			
		}
		
	}
	
	ThreadedLoaderThread mThread = null ;
	
	Queue<WeakReference<Client>> mQueue ;
	
	public ThreadedLoader() {
		mThread = null ;
		mQueue = new LinkedList<WeakReference<Client>>() ;
	}
	
	/**
	 * Adds this client to our load queue.  The client will, at some
	 * future point, have .tlc_getParams() called on it by this object.
	 * 
	 * We only store a WeakReference to this client, not a direct reference.
	 * Adding yourself is safe even if you anticipate being deallocated
	 * before the queued load is ready.
	 * 
	 * @param client
	 */
	public synchronized void addClient( Client client ) {
		if ( client == null ) {
			throw new NullPointerException("Can't add a null client!") ;
		}
		
		synchronized( mQueue ) {
			// if null or dead thread, start one.
			if ( mThread == null || !mThread.isAlive() || !mThread.mRunning ) {
				mThread = new ThreadedLoaderThread() ;
				mThread.start() ;
			}
			
			// provide the client on the queue.  Because the thread synchronizes
			// every queue access, we can guarantee that the thread (whether we
			// just started one or not) will not poll an empty queue before this element
			// is added.
			mQueue.add( new WeakReference<Client>(client) ) ;
		}
	}
	
	
	protected abstract Params newParams() ;
	
	/**
	 * Perform the actual load using the provided parameters.
	 * 
	 * Must return a non-null Result, even in event of failure.
	 * 
	 * @param p
	 * @return
	 */
	protected abstract Result load( Params p ) ;
	
}
