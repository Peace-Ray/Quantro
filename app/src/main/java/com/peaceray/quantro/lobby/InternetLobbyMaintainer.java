package com.peaceray.quantro.lobby;

import java.lang.ref.WeakReference;

import com.peaceray.quantro.lobby.exception.CommunicationErrorException;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


/**
 * A class designed to maintain and host an InternetLobby instance.
 * 
 * In addition to regular updates, this class will respond to outside
 * announcements that certain things should happen, e.g. hosting updates
 * and the like.
 * 
 * Remember: the "host" function provides the server an update regarding
 * connected players.  It is very important therefore that the lobby instance
 * provided is the SAME instance used to host; it will have an up-to-date
 * player count and connection info.
 * 
 * The InternetLobbyMaintainer does not (typically) refresh the lobby given,
 * but it will attempt to do so if a maintenance or hosting attempt is refused.
 * 
 * If the lobby is removed or closed, the maintainer will silently skip
 * host and maintain attempts, reducing communications load and thread cycles
 * 
 * w/o interfering with standard start/stop calls.
 * 
 * @author Jake
 *
 */
public class InternetLobbyMaintainer {
	
	
	/**
	 * The job of the delegate is to handle errors for us.  We don't know
	 * how to recover from (most) errors; the best we normally do is to
	 * reset from a new WorkOrder.
	 * 
	 * However, the Delegate might have some very important actions to perform.
	 * For instance, if the lobby suddenly becomes closed or we have repeated
	 * connection problems, we might want to take some action or at least inform the user.
	 * 
	 * @author Jake
	 *
	 */
	public interface Delegate {
		
		public static final int ACTION_HOST = 0 ;
		public static final int ACTION_MAINTAIN = 1 ;
		public static final int ACTION_REFRESH = 2 ;
		
		
		/**
		 * A successful communication has just occurred with the server while performing 
		 * the specified action.  This is probably of no concern of yours and you should
		 * best ignore it.
		 * 
		 * The only reason this method exists is to notify you of when a series of failures
		 * has ended.  A successful communication can usually be taken to mean a problem
		 * interfering with communication has been resolved.
		 * 
		 * @param ilm
		 * @param action
		 * @param timeSpentFailing What is the length of time between the first in the
		 * 			most recent of failure messages and now?
		 * @param numFailures How many consecutive failures have we seen before this success?
		 */
		public void ilmd_communicationSuccess( InternetLobbyMaintainer ilm, int action,
				long timeSpentFailing, int numFailures ) ;
		
		/**
		 * A failure has occurred when performing the specified action.  Feel
		 * free to ignore certain values, such as a refusal based on 'time':
		 * we will be attempting recovery.
		 * 
		 * The 'long' value returned will be considered as an ADDITIONAL delay
		 * on top of the standard delay we would have otherwise used.  It will
		 * be applied ONLY to retry attempt for this failed action.  If you are stopping
		 * the maintainer, feel free to return a massive value; it will have no
		 * effect on normal operation after it is resumed.
		 * 
		 * However, do NOT return extreme delays if you intend operation to continue.
		 * Usually the retry for this action will be the next time the action
		 * is attempted, so extreme delays can interfere with our future attempts.
		 * 
		 * Feel free to change regular update/maintenance schedules or even stop
		 * the maintainer during this call.
		 * 
		 * @param ilm
		 * @param action
		 * @param error
		 * @param reason
		 * @param timeSpentFailing What is the length of time between the first in the
		 * 			most recent of failure messages and now?  If 0, then this is the first
		 * 			failure of the series (which may end up being length 1).
		 * @param numFailures How many consecutive failures have we seen, including this one?
		 */
		public long ilmd_retryDelayAfterCommunicationFailure( InternetLobbyMaintainer ilm, int action, int error, int reason,
				long timeSpentFailing, int numFailures ) ;
		
		
	}
	

	private static final String TAG = "ILobbyMaintainer" ;
	
	InternetLobbyMaintainerThread mThread ;
	InternetLobby mLobby ;
	WeakReference<Delegate> mwrDelegate ;
	boolean mRefreshOnly ;		// no hosting or maintenance.
	
	private long mHostEvery = 0 ;
	private long mMaintainEvery = 0 ;
	private long mRefreshEvery = 0 ;
	
	private boolean mHostUponStart = false ;
	private boolean mMaintainUponStart = false ;
	private boolean mRefreshUponStart = false ;
	
	
	
	public static InternetLobbyMaintainer newRefresher( InternetLobby internetLobby, Delegate d ) {
		return new InternetLobbyMaintainer( internetLobby, d, true ) ;
	}
	
	public static InternetLobbyMaintainer newMaintainer( InternetLobby internetLobby, Delegate d ) {
		return new InternetLobbyMaintainer( internetLobby, d, false ) ;
	}
	
	
	
	private InternetLobbyMaintainer( InternetLobby internetLobby, Delegate d, boolean refreshOnly ) {
		if ( internetLobby == null )
			throw new NullPointerException("Null lobby given") ;
		
		mLobby = internetLobby ;
		mThread = null ;
		mwrDelegate = new WeakReference<Delegate>(d) ;
		mRefreshOnly = refreshOnly ;
	}
	
	
	synchronized public boolean canHost() {
		return !mRefreshOnly ;
	}
	
	synchronized public boolean canMaintain() {
		return !mRefreshOnly ;
	}
	
	synchronized public InternetLobbyMaintainer hostEvery( long millis ) {
		if ( mRefreshOnly )
			throw new IllegalStateException("This InternetLobbyMaintainer created as a Refresher, not a Maintainer") ;
		mHostEvery = millis ;
		if ( mHostEvery > 0 && mThread != null && mThread.mHandler != null )
			mThread.mHandler.sendMessageDelayed(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_HOST),
					mHostEvery) ;
		return this ;
	}
	
	
	synchronized public InternetLobbyMaintainer maintainEvery( long millis ) {
		if ( mRefreshOnly )
			throw new IllegalStateException("This InternetLobbyMaintainer created as a Refresher, not a Maintainer") ;
		mMaintainEvery = millis ;
		if ( mMaintainEvery > 0 && mThread != null && mThread.mHandler != null )
			mThread.mHandler.sendMessageDelayed(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_MAINTAIN),
					mMaintainEvery) ;
		return this ;
	}
	
	synchronized public InternetLobbyMaintainer refreshEvery( long millis ) {
		mRefreshEvery = millis ;
		if ( mRefreshEvery > 0 && mThread != null && mThread.mHandler != null )
			mThread.mHandler.sendMessageDelayed(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_REFRESH),
					mRefreshEvery) ;
		return this ;
	}
	
	
	synchronized public InternetLobbyMaintainer host() {
		if ( mRefreshOnly )
			throw new IllegalStateException("This InternetLobbyMaintainer created as a Refresher, not a Maintainer") ;
		if ( mThread == null || mThread.mHandler == null )
			mHostUponStart = true ;
		else
			mThread.mHandler.sendMessage(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_HOST)) ;
		return this ;
	}
	
	synchronized public InternetLobbyMaintainer maintain() {
		if ( mRefreshOnly )
			throw new IllegalStateException("This InternetLobbyMaintainer created as a Refresher, not a Maintainer") ;
		if ( mThread == null || mThread.mHandler == null )
			mMaintainUponStart = true ;
		else
			mThread.mHandler.sendMessage(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_MAINTAIN)) ;
		return this ;
	}
	
	synchronized public InternetLobbyMaintainer refresh() {
		if ( mThread == null || mThread.mHandler == null )
			mRefreshUponStart = true ;
		else
			mThread.mHandler.sendMessage(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_REFRESH)) ;
		return this ;
	}
	
	
	
	
	synchronized public boolean isStarted() {
		return mThread != null ;
	}
	
	
	synchronized public void start() {
		if ( mThread != null )
			throw new IllegalStateException("Can't start when already started.") ;
		
		mThread = new InternetLobbyMaintainerThread() ;
		mThread.start() ;

		// wait for the handler.
		try {
			while ( mThread.mHandler == null ) {
				Thread.sleep(10) ;
			}
		} catch( InterruptedException ie ) {
			throw new IllegalStateException("Interrupted when setting up thread.") ;
		}
		
		if ( mHostUponStart )
			mThread.mHandler.sendMessage(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_HOST)) ;
		if ( mMaintainUponStart )
			mThread.mHandler.sendMessage(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_MAINTAIN)) ;
		if ( mRefreshUponStart )
			mThread.mHandler.sendMessage(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_REFRESH)) ;
		mHostUponStart = false ;
		mMaintainUponStart = false ;
		mRefreshUponStart = false ;
		
		if ( mHostEvery > 0 )
			mThread.mHandler.sendMessageDelayed(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_HOST),
					mHostEvery) ;
		if ( mMaintainEvery > 0 )
			mThread.mHandler.sendMessageDelayed(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_MAINTAIN),
					mMaintainEvery) ;
		if ( mRefreshEvery > 0 )
			mThread.mHandler.sendMessageDelayed(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_REFRESH),
					mRefreshEvery) ;
	}
	
	synchronized public void stop() {
		if ( mThread == null )
			return ;
		
		mThread.mHandler.sendMessage(
				mThread.mHandler.obtainMessage(MESSAGE_TYPE_STOP)) ;
		
		mThread = null ;
	}
	
	
	private static final int MESSAGE_TYPE_STOP 				= 0 ;
	private static final int MESSAGE_TYPE_HOST				= 1 ;
	private static final int MESSAGE_TYPE_MAINTAIN 			= 2 ;
	private static final int MESSAGE_TYPE_REFRESH			= 3 ;
		// one step up the "maintain" tree.
	
	private class InternetLobbyMaintainerThread extends Thread {
		
		private static final int FAIL_RETRY_DELAY = 10000 ;	// retry in 10 seconds
		private static final int MAINTAIN_STEP_DELAY = 10000 ;		// upon get work order, wait 10 before first work.
		
		
		
		Handler mHandler = null ;
		WorkOrder mMaintenanceWorkOrder ;		// null, incomplete, or complete.
		
		// These values are measured from the latest success.
		int mCommunicationFailures = 0 ;
		long mFirstCommunicationFailureTime = 0 ;
		
		
		private void noteSuccess( int action ) {
			Delegate d = mwrDelegate.get() ;
			if ( d != null )
				d.ilmd_communicationSuccess(
						InternetLobbyMaintainer.this,
						action,
						mCommunicationFailures == 0 ? 0 : System.currentTimeMillis() - mFirstCommunicationFailureTime,
						mCommunicationFailures) ;
			mCommunicationFailures = 0 ;
			mFirstCommunicationFailureTime = 0 ;
		}
		
		private long noteFailureAndGetDelay( int action, CommunicationErrorException cee ) {
			long delay = 0 ;
			Delegate d = mwrDelegate.get() ;
			if ( d != null )
				delay = d.ilmd_retryDelayAfterCommunicationFailure(
						InternetLobbyMaintainer.this,
						action,
						cee.getError(),
						cee.getErrorReason(),
						mCommunicationFailures == 0 ? 0 : System.currentTimeMillis() - mFirstCommunicationFailureTime,
						mCommunicationFailures + 1 ) ;
			if ( mCommunicationFailures == 0 )
				mFirstCommunicationFailureTime = System.currentTimeMillis() ;
			mCommunicationFailures++ ;
			
			return delay ;
		}
		
		@Override
		public void run() {
			Looper.prepare() ;
			
			mHandler = new Handler() {
				
				@Override
				public void handleMessage (Message msg) {
					
					long delay = 0 ;
					
					switch( msg.what ) {
					case MESSAGE_TYPE_STOP:
						getLooper().quit() ;
						break ;
						
					case MESSAGE_TYPE_HOST:
						removeMessages(MESSAGE_TYPE_HOST) ;
						if ( !(mLobby instanceof MutableInternetLobby) )
							break ;
						try {
							// host, then schedule the next hosting message.
							if ( mLobby.getStatus() == WebConsts.STATUS_EMPTY )
								mLobby.refresh() ;
							else if ( mLobby.getStatus() == WebConsts.STATUS_OPEN )
								((MutableInternetLobby)mLobby).host() ;
							// successful communication?
							noteSuccess( Delegate.ACTION_HOST ) ;
							if ( mHostEvery > 0 )
								sendMessageDelayed(
										obtainMessage(MESSAGE_TYPE_HOST),
										mHostEvery ) ;
						} catch( CommunicationErrorException cee ) {
							delay = noteFailureAndGetDelay( Delegate.ACTION_HOST, cee ) ;
							// we'll try again in a bit...
							sendMessageDelayed(
									obtainMessage(MESSAGE_TYPE_HOST),
									FAIL_RETRY_DELAY + delay) ;
						}
						break ;
						
						
					case MESSAGE_TYPE_MAINTAIN:
						if ( !(mLobby instanceof MutableInternetLobby) )
							break ;
						removeMessages(MESSAGE_TYPE_MAINTAIN) ;
						if ( mLobby.getStatus() == WebConsts.STATUS_EMPTY ) {
							try {
								mLobby.refresh() ;
								noteSuccess( Delegate.ACTION_REFRESH ) ;
								sendMessageDelayed(
										obtainMessage(MESSAGE_TYPE_MAINTAIN),
										MAINTAIN_STEP_DELAY ) ;
							} catch ( CommunicationErrorException cee ) {
								delay = noteFailureAndGetDelay( Delegate.ACTION_REFRESH, cee ) ;
								// we'll try again in a bit...
								sendMessageDelayed(
										obtainMessage(MESSAGE_TYPE_MAINTAIN),
										FAIL_RETRY_DELAY + delay) ;
							}
						}
						else if ( mLobby.getStatus() == WebConsts.STATUS_OPEN ) {
							try {
								if ( mMaintenanceWorkOrder == null ) {
									mMaintenanceWorkOrder = ((MutableInternetLobby)mLobby).maintainRequest() ;
									noteSuccess( Delegate.ACTION_MAINTAIN ) ;
									sendMessageDelayed(
											obtainMessage(MESSAGE_TYPE_MAINTAIN),
											MAINTAIN_STEP_DELAY ) ;
								} else if ( !mMaintenanceWorkOrder.isComplete() ) {
									mMaintenanceWorkOrder.performWork(1000) ;	// 1 second of work
									// schedule the next: either immediately (if incomplete)
									// or at the deadline (if complete).
									if ( !mMaintenanceWorkOrder.isComplete() || !mMaintenanceWorkOrder.hasDeadline() )
										sendMessage(
												obtainMessage(MESSAGE_TYPE_MAINTAIN)) ;
									else 
										sendMessageDelayed(
												obtainMessage(MESSAGE_TYPE_MAINTAIN),
												Math.max(0, mMaintenanceWorkOrder.getDeadline() - System.currentTimeMillis()) ) ;
								} else {
									// report.
									((MutableInternetLobby)mLobby).maintainConfirm(mMaintenanceWorkOrder) ;
									noteSuccess( Delegate.ACTION_MAINTAIN ) ;
									mMaintenanceWorkOrder = null ;
										
									// Schedule the next maintenance.
									if ( mMaintainEvery > 0 )
										sendMessageDelayed(
												obtainMessage(MESSAGE_TYPE_MAINTAIN),
												mMaintainEvery ) ;
								}
							} catch ( CommunicationErrorException cee ) {
								boolean reset = false ;		// reset our work order; it's broken
								if ( cee.getError() == WebConsts.ERROR_REFUSED ) {
									// the reason we get might change things...
									switch( cee.getErrorReason() ) {
									case WebConsts.REASON_NONE:
									case WebConsts.REASON_SIGNATURE:
									case WebConsts.REASON_TIME:
									case WebConsts.REASON_WORK:
									case WebConsts.REASON_ADDRESS:
										reset = true ;
										break ;
									}
								}
								
								if ( reset )
									mMaintenanceWorkOrder = null ;

								delay = noteFailureAndGetDelay( Delegate.ACTION_MAINTAIN, cee ) ;
								// we'll try again in a bit...
								sendMessageDelayed(
										obtainMessage(MESSAGE_TYPE_MAINTAIN),
										FAIL_RETRY_DELAY + delay) ;
							}
						} else {
							// Schedule the next maintenance.
							if ( mMaintainEvery > 0 )
								sendMessageDelayed(
										obtainMessage(MESSAGE_TYPE_MAINTAIN),
										mMaintainEvery ) ;
						}
						break ;
						
					case MESSAGE_TYPE_REFRESH:
						removeMessages(MESSAGE_TYPE_REFRESH) ;
						try {
							mLobby.refresh() ;
							noteSuccess( Delegate.ACTION_REFRESH ) ;
							if ( mHostEvery > 0 )
								sendMessageDelayed(
										obtainMessage(MESSAGE_TYPE_REFRESH),
										mRefreshEvery ) ;
						} catch( CommunicationErrorException cee ) {
							delay = noteFailureAndGetDelay( Delegate.ACTION_REFRESH, cee ) ;
							// we'll try again in a bit...
							sendMessageDelayed(
									obtainMessage(MESSAGE_TYPE_REFRESH),
									FAIL_RETRY_DELAY + delay) ;
						}
						break ;
					}
					
				}
				
			} ;
			
			Looper.loop() ;
		}
		
	}
	
}
