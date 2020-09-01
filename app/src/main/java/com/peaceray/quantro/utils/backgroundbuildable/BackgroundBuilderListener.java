package com.peaceray.quantro.utils.backgroundbuildable;


/**
 * BackgroundBuilders are meant to proceed invisibly.  However, in
 * some cases, you might want to receive notification of how
 * far along we've gotten.
 * 
 * @author Jake
 *
 */
public interface BackgroundBuilderListener<Buildable> {

	/**
	 * We have started building the provided object.
	 * 
	 * @param bb
	 * @param bc
	 */
	public void bbl_buildStarting( BackgroundBuilder<Buildable> bb, Buildable bc ) ;
	
	/**
	 * We have finished the build process for the provided object.  However,
	 * we have not yet released our semaphore locks, so thread-controlled
	 * methods will still block.  In short: bb_setBuilt() has not yet been called.
	 * 
	 * NOTE: Instances of
	 * BackgroundBuildable.Implementation and
	 * BackgroundBuildableSerialParts.Implementation use 'Recursive Locks' to handle
	 * access.  This means any lock-protected methods can safely be used within this
	 * callback.  This can be useful if final configuration is needed.
	 * 
	 * @param bb
	 * @param bc
	 */
	public void bbl_buildFinishing( BackgroundBuilder<Buildable> bb, Buildable bc ) ;
	
	/**
	 * We have finished the build process for the provided object, and bb_setBuilt()
	 * has been called.  Useful if you were waiting until now to collect a reference.
	 * 
	 * If you have final configuration to do after building, it is highly recommended
	 * that you do so in bb_buildFinishing, not this method.
	 * 
	 * @param bb
	 * @param bc
	 */
	public void bbl_buildFinished( BackgroundBuilder<Buildable> bb, Buildable bc ) ;
	
}
