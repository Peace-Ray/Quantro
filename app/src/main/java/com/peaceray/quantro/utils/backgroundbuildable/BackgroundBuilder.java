package com.peaceray.quantro.utils.backgroundbuildable;


/**
 * A BackgroundBuilder is a class that implements the Builder pattern
 * to build objects which take a long time to build, but after being
 * built, operation is fairly quick.
 * 
 * However, it is assumed that instances of this class are needed
 * by, and built by, components relatively closely linked to
 * draw code.  For that reason, the "build" procedure is likely
 * to be inconvenient if it is performed in-line with the class
 * that will end up using it.
 * 
 * BackgroundBuilder is intended as a solution.  They behave using the standard
 * "Builder Pattern", with the wrinkle that
 * BackgroundBuilder.build() returns an object
 * which is currently in the process of being built by 
 * a background thread.  Synchronization allows "built" components
 * of the Buildable to be accessed only after they are finalized;
 * premature access is typically allowed, but the call will block
 * until the relevant method is finished.
 * 
 * Concrete subclasses are responsible for synchronization.  The
 * simplest method is to implement all public methods as "synchronized;"
 * this will automatically prevent access to not-yet-built components.
 * 
 * More complex operation is available through the use of provided
 * "semaphores," although it is the responsibility of subclass implementations
 * to release and monitor those semaphors at the right times.  This
 * is most appropriate in cases where the build operation can be easily
 * divided into a series of steps, where some publically-facing part
 * of the Buildable is finished and should be accessible after each step.
 * 
 * This class builds only classes which implement BackgroundBuilder
 * or BackgroundBuilderSerialParts (which includes BackgroundBuilder).
 * 
 * Instances of this class will always return the same "built" object;
 * attempts to configure the builder after building should fail.
 * 
 * @author Jake
 *
 */
public abstract class BackgroundBuilder<Buildable> {

	/**
	 * Creates and returns a new instance of the "Buildable"
	 * class.  This method, and the associated "Buildable" constructor,
	 * should be fast and efficient (i.e., it should NOT perform
	 * the necessary configuration, which we prefer to do in the background).
	 * 
	 * If the Builder is not sufficiently configured to create an
	 * object, you may return 'null' or throw an exception, your choice.
	 * 
	 * POSTCONDITION: The object returned should implement either
	 * BackgroundBuildable or BackgroundBuildableSerialParts.  If
	 * it implements both, we treat it as having implemented
	 * BackgroundBuildableSerialParts.
	 * 
	 * @return
	 */
	protected abstract Buildable bb_newEmptyBuildable() ;
	
	
	/**
	 * Perform the necessary steps to build this object.  The instance
	 * provided is the one returned by bb_newEmptyBuildableInstance().
	 * 
	 * Note: implementations should handle bb_build in-line, only returning
	 * once the build process is complete.  Don't worry about background
	 * threading; that's our job.
	 * 
	 * CONDITION: Access to our local data should be read-only; only the
	 * fields of the provided object should be changed by this build process.
	 * 
	 * @param obj
	 */
	protected abstract void bb_build( Buildable obj ) ;
	
	
	/**
	 * Returns whether this Builder has ever been used to
	 * Build an object.  Useful to "lock-in" our current state;
	 * because objects may be built in the background, it is necessary
	 * to ensure that nothing changes about our builder while an object
	 * is being built.
	 * 
	 * @return
	 */
	synchronized protected final boolean bb_hasBuilt() {
		return mHasBuilt ;
	}
	
	
	private boolean mHasBuilt = false ;
	
	
	
	/**
	 * Returns a new instance which is fully built.
	 * 
	 * Note: this method is no safer than "build", and the processor
	 * cycles saved by using it are negligible.
	 * 
	 * The advantage here is that, with buildNow(), the cycles wasted
	 * on the build process will occur immediately, rather than
	 * (potentially) when an access call is later made.
	 * 
	 * @return
	 */
	synchronized public final Buildable buildNow() {
		return buildNow( null ) ;
	}
	
	
	/**
	 * Returns a new instance which is fully built.
	 * 
	 * Note: this method is no safer than "build", and the processor
	 * cycles saved by using it are negligible.
	 * 
	 * The advantage here is that, with buildNow(), the cycles wasted
	 * on the build process will occur immediately, rather than
	 * (potentially) when an access call is later made.
	 * 
	 * @return
	 */
	synchronized public final Buildable buildNow(BackgroundBuilderListener<Buildable> listener) {
		mHasBuilt = true ;
		Buildable b = bb_newEmptyBuildable() ;
		if ( b == null )
			return null ;
		
		if ( !(b instanceof BackgroundBuildable)
				&& !(b instanceof BackgroundBuildableSerialParts) )
			throw new IllegalStateException("bb_newEmptyBuildableInstance returned object is not an instance of BackgroundBuildable or BackgroundBuildableSerialParts!") ;
		
		RuntimeException buildException = null ;
		try {
			// this call acquires 1 or more locks, which are unlocked
			// in doBuildPost.  Make sure we call the unlock component!
			doBuildPre( b ) ;
			try {
				doBuild( b, listener ) ;
			} catch ( RuntimeException re ) {
				buildException = re ;
				doBuildSetException( b, re ) ;
			} finally {
				// Always unlock, even if an exception is thrown by build.
				doBuildPost( b, listener ) ;
			}
		} catch ( RuntimeException re ) {
			buildException = re ;
		}
		
		if ( buildException != null )
			throw buildException ;
		
		return b ;
	}
	
	
	/**
	 * Returns a new instance for which this class will serve
	 * as a background builder.
	 * 
	 * The returned object may not be fully "built", but it will obey
	 * all the access restrictions, so it can be used as if built.
	 * 
	 * Thread note: if this method returns a non-null value, a new thread
	 * has been started to perform the build.
	 * 
	 * @return
	 */
	synchronized public final Buildable build() {
		return build(null) ;
	}
	
	
	/**
	 * Returns a new instance for which this class will serve
	 * as a background builder.
	 * 
	 * The returned object may not be fully "built", but it will obey
	 * all the access restrictions, so it can be used as if built.
	 * 
	 * @return
	 */
	synchronized public final Buildable build(BackgroundBuilderListener<Buildable> listener) {
		mHasBuilt = true ;
		Buildable b = bb_newEmptyBuildable() ;
		if ( b == null )
			return null ;
		
		if ( !(b instanceof BackgroundBuildable)
				&& !(b instanceof BackgroundBuildableSerialParts) )
			throw new IllegalStateException("bb_newEmptyBuildableInstance returned object is not an instance of BackgroundBuildable or BackgroundBuildableSerialParts!") ;
		
		
		BuildThread buildThread = new BuildThread( b, listener ) ;
		buildThread.start() ;
		while ( !buildThread.buildStarted() ) {
			try {
				Thread.sleep(3) ;
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted while waiting for BuildThread to start.  Can't recover from this.") ;
			}
		}
		
		// the build has started; return the object under construction.
		return b ;
	}
	
	
	
	/**
	 * Helper method: performs the build.  Meant to be called inline or
	 * by a BuildThread.
	 * 
	 * @param obj
	 * @param listener
	 */
	private final void doBuildPre( Buildable b ) {
		// Let the object know that the build is starting.
		if ( b instanceof BackgroundBuildableSerialParts ) {
			BackgroundBuildableSerialParts bbsp = (BackgroundBuildableSerialParts)b ;
			bbsp.bb_buildStartingOnThisThread() ;
		} else if ( b instanceof BackgroundBuildable ) {
			BackgroundBuildable bb = (BackgroundBuildable)b ;
			bb.bb_buildStartingOnThisThread() ;
		}
	}
	
	private final void doBuild( Buildable b, BackgroundBuilderListener<Buildable> listener ) {
		if ( listener != null )
			listener.bbl_buildStarting(this, b) ;
		
		bb_build(b) ;
		
		if ( listener != null )
			listener.bbl_buildFinishing(this, b) ;
	}
	
	private final void doBuildSetException( Buildable b, RuntimeException re ) {
		if ( b instanceof BackgroundBuildableSerialParts ) {
			BackgroundBuildableSerialParts bbsp = (BackgroundBuildableSerialParts)b ;
			bbsp.bb_setBuildException(re) ;
		} else if ( b instanceof BackgroundBuildable ) {
			BackgroundBuildable bb = (BackgroundBuildable)b ;
			bb.bb_setBuildException(re) ;
		}
	}
	
	private final void doBuildPost( Buildable b, BackgroundBuilderListener<Buildable> listener ) {
		// Let the object know that the build is finished.
		if ( b instanceof BackgroundBuildableSerialParts ) {
			BackgroundBuildableSerialParts bbsp = (BackgroundBuildableSerialParts)b ;
			bbsp.bb_buildFinishedOnThisThread() ;
		} else if ( b instanceof BackgroundBuildable ) {
			BackgroundBuildable bb = (BackgroundBuildable)b ;
			bb.bb_buildFinishedOnThisThread() ;
		}
		
		// Tell the listener.
		if ( listener != null )
			listener.bbl_buildFinished(this, b) ;
	}
	
	
	
	private class BuildThread extends Thread {
		
		boolean mBuildStarted ;
		Buildable mBuildable ;
		BackgroundBuilderListener<Buildable> mListener ;
		
		private BuildThread( Buildable b, BackgroundBuilderListener<Buildable> listener ) {
			mBuildStarted = false ;
			mBuildable = b ;
			mListener = listener ;
		}
		
		private boolean buildStarted() {
			return mBuildStarted ;
		}
		
		@Override
		public void run() {
			RuntimeException buildException = null ;
			try {
				// this call acquires 1 or more locks, which are unlocked
				// in doBuildPost.  Make sure we call the unlock component!
				doBuildPre( mBuildable ) ;
				mBuildStarted = true ;
				try {
					doBuild( mBuildable, mListener ) ;
				} catch ( RuntimeException re ) {
					buildException = re ;
					doBuildSetException( mBuildable, re ) ;
				} finally {
					// Always unlock, even if an exception is thrown by build.
					doBuildPost( mBuildable, mListener ) ;
				}
			} catch ( RuntimeException re ) {
				buildException = re ;
			}
			
			if ( buildException != null )
				throw buildException ;
			
		}
		
	}
	
}
