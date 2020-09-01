package com.peaceray.quantro.utils.backgroundbuildable;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public interface BackgroundBuildable {
	
	/**
	 * The background build process is starting.  The thread
	 * which calls this method should be the thread performing the build,
	 * and should call bb_buildFinished() when complete.
	 */
	void bb_buildStartingOnThisThread() ;
	
	/**
	 * If an error occurs while building, call this method to provide
	 * the exception (feel free to throw it again yourself).
	 * 
	 * Because building can take place in a dedicated thread, this 
	 * method ensures that the exception makes its way to a main "use thread."
	 * 
	 * @param e
	 */
	void bb_setBuildException( RuntimeException e ) ;
	
	/**
	 * Sets that the build process is complete.
	 * From this point on methods should allow access;
	 * they should not block.
	 * 
	 * CONDITION: The same thread which called bb_buildStarting()
	 * should call this method.
	 * 
	 */
	void bb_buildFinishedOnThisThread() ;
	
	/**
	 * For BackgroundBuildables that do not require a special
	 * superclass, extending this Implementation allows easy
	 * synchronization.  Call ''bb_blockUntilBuilt' at the start of
	 * every method call (apart from those private or protected 
	 * methods used for building itself).
	 * 
	 * @author Jake
	 *
	 */
	public static class Implementation implements BackgroundBuildable {
		
		
		/**
		 * Blocks until the object is built.  Calling this method
		 * is extremely efficient once the build process is complete;
		 * don't worry about implementing your own efficiency code on
		 * top of it, just make the call.
		 * 
		 * If called from the thread which is building the object, will
		 * return w/o blocking.
		 * 
		 * @return
		 */
		protected final void bb_blockUntilBuiltOrInBuildingThread() {
			if ( !mBuilt ) {
				mBuildingLock.lock() ;
				mBuildingLock.unlock() ;
				// process is immediate if the lock is held by this thread.
			}
			if ( mBuildException != null ) {
				throw mBuildException ;
			}
		}
		
		private boolean mBuildStarted ;
		private boolean mBuilt ;
		private Lock mBuildingLock ;
		private RuntimeException mBuildException ;
		
		protected Implementation() {
			mBuildStarted = false ;
			mBuilt = false ;
			mBuildingLock = new ReentrantLock() ;
			mBuildException = null ;
		}
		
		/**
		 * The background build process is starting.  The thread
		 * which calls this method should be the thread performing the build,
		 * and should call bb_buildFinished() when complete.
		 */
		public final void bb_buildStartingOnThisThread() {
			if ( mBuildStarted )
				throw new IllegalStateException("Only call this method once!") ;
			mBuildingLock.lock() ;
			mBuildStarted = true ;
		}
		
		/**
		 * If an error occurs while building, call this method to provide
		 * the exception (feel free to throw it again yourself).
		 * 
		 * Because building can take place in a dedicated thread, this 
		 * method ensures that the exception makes its way to a main "use thread."
		 * 
		 * @param e
		 */
		public void bb_setBuildException( RuntimeException e ) {
			mBuildException = e ;
		}
		
		/**
		 * Sets that the build process is complete.
		 * From this point on methods should allow access;
		 * they should not block.
		 * 
		 * CONDITION: The same thread which called bb_buildStarting()
		 * should call this method.
		 * 
		 */
		public final void bb_buildFinishedOnThisThread() {
			if ( mBuilt )
				throw new IllegalStateException("Only call this method once!") ;
			mBuilt = true ;
			mBuildingLock.unlock() ;
		}
		
	}
	
}
