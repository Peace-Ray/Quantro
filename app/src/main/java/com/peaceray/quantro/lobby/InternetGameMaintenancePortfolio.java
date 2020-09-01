package com.peaceray.quantro.lobby;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.lobby.exception.CommunicationErrorException;


/**
 * A MaintenancePortfolio provides a record of InternetGames
 * for use in an Internet multiplayer lobby.  The whole idea
 * of this portfolio is to track all the InternetLobbyGames
 * which have been created and passed off to players for hosting
 * and maintenance -- or to maintain the games up until the
 * point that a player takes over (even if the player is on the
 * same device as this portfolio).
 * 
 * The point of this portfolio is:
 * 
 * 1. Provide an easy method of creating InternetLobbyGames without
 * 	tying up a thread working on other important things.  Instances
 * 	of this object have their own work thread.  In fact, ILGs can be
 * 	"pre-created" before they are needed so that, when it comes time
 *  to create a game, there is likely to be one ready.
 *  
 * 2. Provide automatic, effortless "maintenance" of ILGs which have
 * 	been created but not yet distributed.  Happens in the IGMP's work
 * 	thread so it doesn't slow anything else down (apart from eating CPU
 * 	cycles).
 * 
 * 3. Provide a central storage location for all current ILGs and
 * 	associated metadata.  This way we can quickly end those which
 * 	have been abandoned by players (we require players to remain
 * 	connected to the lobby for as long as they are participating in
 * 	a game); temporary disconnects are probably OK, but abandoned
 * 	games should probably be ended -- that does NOT happen automatically,
 * 	but can be done via this Portfolio on its work thread.
 * 
 * 4. Provide a robust and accessible record of ILGs that remains so
 * 	even when server communication fails.
 * 
 * To provide simple implementation and use of these features, this
 * object is assumed to be the ONLY object handling the creation and
 * storage of ILGs.  New ILGs cannot be introduced, and existing
 * ILGs SHOULD NOT BE ENDED except through calls to the creating instance
 * of this class.
 * 
 * Relevant metadata: 
 * 
 * 	1. Whether an ILG is new or has been provided to a player.
 *  2. For hosted games, the personal nonces of the players involved
 *  	and which player is the designated host.
 *  3. A record of any ended games, and whether they were ended by
 *  	this object or by some other entity (e.g. the host or the server).
 * 
 * @author Jake
 *
 */
public class InternetGameMaintenancePortfolio {
	
	private static final String TAG = "InternetGameMaintenancePortfolio" ;
	
	
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
		
		public static final int ACTION_OPEN_GAME = 0 ;
		public static final int ACTION_MAINTAIN_GAME = 1 ;
		public static final int ACTION_REFRESH_GAME = 2 ;
		public static final int ACTION_CLOSE_GAME = 3 ;
		
		
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
		public void igmpd_communicationSuccess(
				InternetGameMaintenancePortfolio igmp, Record record, int action,
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
		 */
		public long igmpd_retryDelayAfterCommunicationFailure(
				InternetGameMaintenancePortfolio igmp, Record record, int action,
				int error, int reason,
				long timeSpentFailing, int numFailures ) ;
		
		
	}

	public class Record {
		
		// either a 'create game' work order, or a 'maintain game' work order.
		// if a 'create game', there is not a current game associated with this record.
		private WorkOrder mWorkOrder ;
		
		private InternetLobbyGame mInternetLobbyGame ;
		private long mTimeCreated ;
			// the time this RECORD created (which probably precedes the game itself).
		
		private boolean mClosedByThis ;
		
		private boolean mHosted ;
		private Nonce [] mPlayerPersonalNonces ;
		private int mHostIndex ;	// indexes the nonce array
		private long mTimeHosted ;
		
		private Record() {
			mTimeCreated = System.currentTimeMillis() ;
			
			mWorkOrder = null ;
			mInternetLobbyGame = null ;
			mClosedByThis = false ;
			mHosted = false ;
			mPlayerPersonalNonces = null ;
			mHostIndex = -1 ;
			mTimeHosted = 0 ;
		}
		
		@Override
		public boolean equals( Object obj ) {
			if ( !(obj instanceof Record) )
				return false ;
			
			return obj == this ;
		}
		
		
		// public accessors
		public InternetLobbyGame getGame() {
			return mInternetLobbyGame ;
		}
		
		public long getRecordAge() {
			return System.currentTimeMillis() - mTimeCreated ;
		}
		
		public boolean getClosedByPortfolio() {
			return mClosedByThis ;
		}
		
		public boolean getHosted() {
			return mHosted ;
		}
		
		public int getNumPlayers() {
			return mPlayerPersonalNonces.length ;
		}
		
		public Nonce getPlayerPersonalNonce( int index ) {
			return mPlayerPersonalNonces[index] ;
		}
		
		public int getHostIndex() {
			return mHostIndex ;
		}
		
		public long getHostedAge() {
			return System.currentTimeMillis() - mTimeHosted ;
		}
		
	}
	
	
	
	private ArrayList<Record> mRecordsCreating ;
	private ArrayList<Record> mRecordsUnhosted ;
	private ArrayList<Record> mRecordsHosted ;
	private ArrayList<Record> mRecordsClosed ;			// or removed
	
	private Hashtable<Nonce, Record> mAllRecords ;		// indexed by ILG nonce.
	
	private Object mRecordsMutex ;
	
	private MutableInternetLobby mLobby ;
	private InternetGameMaintenancePortfolioThread mThread ;
	private WeakReference<Delegate> mwrDelegate ;
	
	// scheduling and queueing
	private int mPrecreatedGames = 0 ;
	private long mMaintainEvery = 0 ;
	private long mRefreshEvery = 0 ;
	
	
	public InternetGameMaintenancePortfolio( MutableInternetLobby internetLobby, Delegate d ) {
		if ( internetLobby == null )
			throw new NullPointerException("Null lobby given") ;
		
		mLobby = internetLobby ;
		mThread = null ;
		mwrDelegate = new WeakReference<Delegate>(d) ;
		
		mRecordsCreating = new ArrayList<Record>() ;
		mRecordsUnhosted = new ArrayList<Record>() ;
		mRecordsHosted = new ArrayList<Record>() ;
		mRecordsClosed = new ArrayList<Record>() ;
		
		mAllRecords = new Hashtable<Nonce, Record>() ;
		
		mRecordsMutex = new Object() ;
	}
	
	
	/**
	 * This class works by pre-opening games in advance of when they are needed.
	 * 
	 * We always attempt to have at least 1 game pre-opened (when a game is
	 * given to players for hosting, there may be a delay while a new game
	 * is created to replace it in the cache).
	 * 
	 * However, if you want a larger window of pre-opened games (for instance,
	 * a lobby of size 8 might want 4 pre-opened games available so all 8 players
	 * can be immediately paired off), call this method to set that value.
	 * 
	 * Pre-opening will still take some time, so don't call this method if
	 * newHostedGame() returns 'null' -- you won't get any benefit.  However,
	 * calling this method early (e.g. when the lobby is opened) will allow later
	 * sequential calls to newHostedGame() to proceed smoothly.
	 * 
	 * The downside: more rows required by the server database.  More rows =
	 * more overhead = we may get punished with more work or 
	 * 
	 * @param numGames
	 */
	synchronized public void openUpTo( int numGames ) {
		mPrecreatedGames = Math.max(1, numGames) ;
		
		if ( mThread != null && mThread.mHandler != null  ) {
			// Just in case, check whether we should start up a new record creation process.
			synchronized( mRecordsMutex ) {
				if ( !mThread.mHandler.hasMessages(MESSAGE_TYPE_CREATE)
						&& mRecordsUnhosted.size() + mRecordsCreating.size() < mPrecreatedGames ) {
					mThread.mHandler.sendMessage( mThread.mHandler.obtainMessage( MESSAGE_TYPE_CREATE, null )) ;
					// Log.d(TAG, "openUpTo: adding MESSAGE_TYPE_CREATE with null record") ;
				}
			}
		}
	}	
	
	synchronized public void maintainEvery( long millis ) {
		mMaintainEvery = millis ;
		if ( mMaintainEvery > 0 && mThread != null && mThread.mHandler != null ) {
			// maintain every MAINTAINABLE record.
			synchronized( mRecordsMutex ) {
				Iterator<Record> iter = mRecordsUnhosted.iterator() ;
				for ( ; iter.hasNext() ; )
					mThread.mHandler.sendMessageDelayed(
							mThread.mHandler.obtainMessage(MESSAGE_TYPE_MAINTAIN, iter.next()),
							mMaintainEvery) ;
			}
		}
	}
	
	synchronized public void refreshEvery( long millis ) {
		mRefreshEvery = millis ;
		if ( mRefreshEvery > 0 && mThread != null && mThread.mHandler != null ) {
			// refresh every REFRESHABLE record.
			synchronized( mRecordsMutex ) {
				Iterator<Record> iter = mRecordsUnhosted.iterator() ;
				for ( ; iter.hasNext() ; )
					mThread.mHandler.sendMessageDelayed(
							mThread.mHandler.obtainMessage(MESSAGE_TYPE_REFRESH, iter.next()),
							mRefreshEvery) ;
				iter = mRecordsHosted.iterator() ;
				for ( ; iter.hasNext() ; )
					mThread.mHandler.sendMessageDelayed(
							mThread.mHandler.obtainMessage(MESSAGE_TYPE_REFRESH, iter.next()),
							mRefreshEvery) ;
				
			}
		}
	}
	
	synchronized public boolean maintain( InternetLobbyGame game ) {
		if ( mThread == null || mThread.mHandler == null )
			throw new IllegalStateException("Not started") ;
		
		if ( game == null )
			throw new NullPointerException("Provided game is null") ;
		
		// find the record...
		Record record = null ;
		// only unhosted records can be maintained.
		synchronized( mRecordsMutex ) {
			Iterator<Record> iter = mRecordsUnhosted.iterator() ;
			for ( ; iter.hasNext() ; ) {
				Record r = iter.next() ;
				if ( r.mInternetLobbyGame.getNonce().equals( game.getNonce() ) )
					record = r ;
			}
		}
		if ( record != null )
			mThread.mHandler.sendMessage(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_MAINTAIN, record)) ;
		
		return record != null ;
	}
	
	
	synchronized public boolean refresh( InternetLobbyGame game ) {
		if ( mThread == null || mThread.mHandler == null )
			throw new IllegalStateException("Not started") ;
		
		if ( game == null )
			throw new NullPointerException("Provided game is null") ;
		
		// find the record...
		Record record = null ;
		// only unhosted or hosted records can be maintained.
		synchronized( mRecordsMutex ) {
			Iterator<Record> iter = mRecordsUnhosted.iterator() ;
			for ( ; iter.hasNext() ; ) {
				Record r = iter.next() ;
				if ( r.mInternetLobbyGame.getNonce().equals( game.getNonce() ) )
					record = r ;
			}
			iter = mRecordsHosted.iterator() ;
			for ( ; iter.hasNext() ; ) {
				Record r = iter.next() ;
				if ( r.mInternetLobbyGame.getNonce().equals( game.getNonce() ) )
					record = r ;
			}
		}
		if ( record != null )
			mThread.mHandler.sendMessage(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_REFRESH, record)) ;
		
		return record != null ;
	}
	
	
	synchronized public boolean close( InternetLobbyGame game ) {
		if ( mThread == null || mThread.mHandler == null )
			throw new IllegalStateException("Not started") ;
		
		if ( game == null )
			throw new NullPointerException("Provided game is null") ;
		
		// find the record...
		Record record = null ;
		// only unhosted or hosted records can be maintained.
		synchronized( mRecordsMutex ) {
			Iterator<Record> iter = mRecordsUnhosted.iterator() ;
			for ( ; iter.hasNext() ; ) {
				Record r = iter.next() ;
				if ( r.mInternetLobbyGame.getNonce().equals( game.getNonce() ) )
					record = r ;
			}
			iter = mRecordsHosted.iterator() ;
			for ( ; iter.hasNext() ; ) {
				Record r = iter.next() ;
				if ( r.mInternetLobbyGame.getNonce().equals( game.getNonce() ) )
					record = r ;
			}
			iter = mRecordsClosed.iterator() ;
			for ( ; iter.hasNext() ; ) {
				Record r = iter.next() ;
				if ( r.mInternetLobbyGame.getNonce().equals( game.getNonce() ) )
					record = r ;
			}
		}
		if ( record != null )
			mThread.mHandler.sendMessage(
					mThread.mHandler.obtainMessage(MESSAGE_TYPE_CLOSE, record)) ;
		
		return record != null ;
	}
	
	
	/**
	 * An optional call letting the InternetGameMaintenancePortfolio
	 * know that a call to "hostNewGame" is expected presently.
	 * 
	 * @return
	 */
	synchronized public void prepareForHostNewGame() {
		// synchronized this whole thing.
		synchronized( mRecordsMutex ) {
			
			while ( true ) {
				if ( mRecordsUnhosted.size() > 0 ) {
					int status =  mRecordsUnhosted.get(0).mInternetLobbyGame.getStatus() ;
					if ( status == WebConsts.STATUS_CLOSED || status == WebConsts.STATUS_REMOVED ) {
						mRecordsClosed.add( mRecordsUnhosted.remove(0) ) ;
					} else {
						mThread.mHandler.sendMessage(
								mThread.mHandler.obtainMessage(
										MESSAGE_TYPE_REFRESH,
										mRecordsUnhosted.get(0) ) ) ;
						return ;
					}
				} else if ( !mThread.mHandler.hasMessages(MESSAGE_TYPE_CREATE) ) {
					mThread.mHandler.sendMessage(
							mThread.mHandler.obtainMessage(MESSAGE_TYPE_CREATE, null)) ;
					return ;
				}
			}
			
		}
	}
	
	
	/**
	 * Notes that we wish to assign a new InternetLobbyGame to the specified players,
	 * with the indexed player acting as the game's host.
	 * 
	 * This method will perform either of the following actions atomically:
	 * 
	 * 1. Make all internal notes necessary to record these players as the participants
	 * 		in a new game, which is currently open and active (maintained) on the server.
	 * 
	 * 		Return a reference to an InternetLobbyGame object representing that new, OPEN
	 * 		game.
	 * 
	 * 		This operation should be very fast (much faster than manually creating a new game).
	 * 
	 * 2. Perform absolutely no changes and return 'null.'  This indicates that, for whatever
	 * 		reason, there is no game available for hosting.  Could be caused by server refusal,
	 * 		an inability to communicate with the server, or maybe we're in the process of
	 * 		creating a new game and a new one will be available in a bit.
	 * 
	 * Will throw an exception if the provided parameters are invalid in any way.
	 * 
	 * @param personalNonces
	 * @param hostIndex
	 * @return
	 */
	synchronized public InternetLobbyGame hostNewGame( Nonce [] personalNonces, int hostIndex ) {
		
		if ( personalNonces == null || hostIndex < 0 || hostIndex >= personalNonces.length )
			throw new IllegalArgumentException("Must provide personal nonces of length at least 1 containing the provided hostIndex.") ;
		
		for ( int i = 0; i < personalNonces.length; i++ )
			if ( personalNonces[i] == null )
				throw new IllegalArgumentException("Personal nonces must be non-null") ;
		

		
		// synchronized this whole thing.
		synchronized( mRecordsMutex ) {
			
			// Log.d(TAG, "hostNewGame: have " + mRecordsUnhosted.size() + " games available.") ;
			
			while ( mRecordsUnhosted.size() > 0 ) {
				// remove from unhosted
				Record record = mRecordsUnhosted.remove(0) ;
				
				// Start a new "create" attempt.
				if ( mThread != null && mThread.mHandler != null  ) {
					// Just in case, check whether we should start up a new record creation process.
					if ( !mThread.mHandler.hasMessages(MESSAGE_TYPE_CREATE)
							&& mRecordsUnhosted.size() + mRecordsCreating.size() < mPrecreatedGames ) {
						mThread.mHandler.sendMessage( mThread.mHandler.obtainMessage( MESSAGE_TYPE_CREATE, null )) ;
					}
				}
				
				int status = record.mInternetLobbyGame.getStatus() ;
				
				if ( status == WebConsts.STATUS_CLOSED || status == WebConsts.STATUS_REMOVED ) {
					mRecordsClosed.add(record) ;
					continue ;		// can't use this one!
				}
				
				// set data
				record.mHosted = true ;
				record.mPlayerPersonalNonces = new Nonce[personalNonces.length] ;
				for ( int i = 0; i < personalNonces.length; i++ )
					record.mPlayerPersonalNonces[i] = personalNonces[i] ;
				record.mHostIndex = hostIndex ;
				record.mTimeHosted = System.currentTimeMillis() ;
				
				// move to hosted
				mRecordsHosted.add(record) ;
				
				return record.mInternetLobbyGame ;
			}
			
		}
		
		return null ;
	}
	
	
	/**
	 * Returns all games which are currently hosted by players.  These
	 * are games that 1. have been assigned via 'hostNewGame' and which
	 * 2. have not been closed (either by this class or by the hosting players).
	 * 
	 * Our information regarding closed games may lag behind slightly.  Feel
	 * free to refresh these instances and/or use our provided method to refresh.
	 * 
	 * Additionally, these games could be closed at any time and this object might
	 * update their instances.  Don't assume they remain open forever just
	 * because they were returned by this method.
	 * 
	 * @return
	 */
	synchronized public ArrayList<InternetLobbyGame> getHostedGames() {
		ArrayList<InternetLobbyGame> list = new ArrayList<InternetLobbyGame>() ;
		
		synchronized( mRecordsMutex ) {
			Iterator<Record> iter = mRecordsHosted.iterator() ;
			for ( ; iter.hasNext() ; ) {
				list.add(iter.next().mInternetLobbyGame) ;
			}
		}
		
		return list ;
	}
	
	
	synchronized public Record getRecord( InternetLobbyGame game ) {
		// finds the record associated with the provided game.  Useful if
		// you want more detailed information about the game.
		
		Nonce n = game.getNonce() ;
		
		synchronized( mRecordsMutex ) {
			if ( mAllRecords.containsKey(n) )
				return mAllRecords.get(n) ;
		}
		return null ;
	}
	
	
	synchronized public boolean isStarted() {
		return mThread != null ;
	}
	
	
	synchronized public void start() {
		
		if ( mThread != null )
			throw new IllegalStateException("Can't start when already started.") ;
		
		mThread = new InternetGameMaintenancePortfolioThread() ;
		mThread.start() ;

		// wait for the handler.
		try {
			while ( mThread.mHandler == null ) {
				Thread.sleep(10) ;
			}
		} catch( InterruptedException ie ) {
			throw new IllegalStateException("Interrupted when setting up thread.") ;
		}
		
		synchronized( mRecordsMutex ) {
			if ( mMaintainEvery > 0 ) {
				Iterator<Record> iter = mRecordsUnhosted.iterator() ;
				for ( ; iter.hasNext() ; )
					mThread.mHandler.sendMessageDelayed(
							mThread.mHandler.obtainMessage(MESSAGE_TYPE_MAINTAIN, iter.next()),
							mMaintainEvery) ;
			}
			if ( mRefreshEvery > 0 ) {
				Iterator<Record> iter = mRecordsUnhosted.iterator() ;
				for ( ; iter.hasNext() ; )
					mThread.mHandler.sendMessageDelayed(
							mThread.mHandler.obtainMessage(MESSAGE_TYPE_REFRESH, iter.next()),
							mRefreshEvery) ;
				iter = mRecordsHosted.iterator() ;
				for ( ; iter.hasNext() ; )
					mThread.mHandler.sendMessageDelayed(
							mThread.mHandler.obtainMessage(MESSAGE_TYPE_REFRESH, iter.next()),
							mRefreshEvery) ;
			}
			
			if ( mRecordsCreating.size() > 0 ) {
				mThread.mHandler.sendMessage( mThread.mHandler.obtainMessage( MESSAGE_TYPE_CREATE, mRecordsCreating.get(0) )) ;
				// Log.d(TAG, "start: adding MESSAGE_TYPE_CREATE with null record") ;
			}
			if ( !mThread.mHandler.hasMessages(MESSAGE_TYPE_CREATE)
					&& mRecordsUnhosted.size() + mRecordsCreating.size() < mPrecreatedGames ) {
				mThread.mHandler.sendMessage( mThread.mHandler.obtainMessage( MESSAGE_TYPE_CREATE, null )) ;
				// Log.d(TAG, "openUpTo: adding MESSAGE_TYPE_CREATE with null record") ;
			}
		}
	}
	
	synchronized public void stop() {
		if ( mThread == null )
			return ;
			
		mThread.mHandler.sendMessage(
				mThread.mHandler.obtainMessage(MESSAGE_TYPE_STOP)) ;
		
		mThread = null ;
	}
	

	private static final int MESSAGE_TYPE_STOP 		= 0 ;
	private static final int MESSAGE_TYPE_CREATE 	= 1 ;
	private static final int MESSAGE_TYPE_MAINTAIN 	= 2 ;
	private static final int MESSAGE_TYPE_CLOSE		= 3 ;
	private static final int MESSAGE_TYPE_REFRESH 	= 4 ;
	private static final int NUM_MESSAGE_TYPES		= 5 ;
	
	// fairly generic types.  Each (aside from STOP) is associated
	// with a record instance.  We assume the record is currently in
	// the appropriate list (Creating for 'CREATE; Unhosted for
	// MAINTAIN; Unhosted or Hosted for END.
	//
	// If the record is not found in the appropriate list, the action
	// will not be performed.
	
	private class InternetGameMaintenancePortfolioThread extends Thread {
		
		private static final int FAIL_RETRY_DELAY = 10000 ;	// retry in 10 seconds
		private static final int CREATE_STEP_DELAY = 5000 ;		// upon get work order, wait 5 before first work.
		private static final int MAINTAIN_STEP_DELAY = 10000 ;
		
		
		Handler mHandler = null ;
		
		// These values are measured from the latest success.
		int mCommunicationFailures = 0 ;
		long mFirstCommunicationFailureTime = 0 ;
		
		
		private void noteSuccess( Record record, int action ) {
			Delegate d = mwrDelegate.get() ;
			if ( d != null )
				d.igmpd_communicationSuccess(
						InternetGameMaintenancePortfolio.this,
						record,
						action,
						mCommunicationFailures == 0 ? 0 : System.currentTimeMillis() - mFirstCommunicationFailureTime,
						mCommunicationFailures) ;
			mCommunicationFailures = 0 ;
			mFirstCommunicationFailureTime = 0 ;
		}
		
		private long noteFailureAndGetDelay( Record record, int action, CommunicationErrorException cee ) {
			//cee.printStackTrace() ;
			long delay = 0 ;
			Delegate d = mwrDelegate.get() ;
			if ( d != null )
				delay = d.igmpd_retryDelayAfterCommunicationFailure(
						InternetGameMaintenancePortfolio.this,
						record,
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
					
					Record record = (Record) msg.obj ;
					
					switch( msg.what ) {
					case MESSAGE_TYPE_STOP:
						getLooper().quit() ;
						break ;
						
					case MESSAGE_TYPE_CREATE:
						// the lobby must not be closed.
						if ( mLobby.getStatus() == WebConsts.STATUS_CLOSED || mLobby.getStatus() == WebConsts.STATUS_REMOVED )
							break ;
						
						// create a game.  This is the only message type
						// in which the record is allowed to be null.
						if ( record == null ) {
							// create a new record, add it to the appropriate list,
							// and queue up a new message with the record attached.
							record = new Record() ;
							synchronized( mRecordsMutex ) {
								mRecordsCreating.add(record) ;
							}
							sendMessage( obtainMessage(MESSAGE_TYPE_CREATE, record) ) ;
						} else {
							// remove any duplicate messages.
							removeMessages(MESSAGE_TYPE_CREATE, record) ;
							
							// first check: this record MUST be in recordsCreating.
							if ( mRecordsCreating.contains(record) ) {
								// next check: this record must NOT have a created
								// ILG instance.
								if ( record.mInternetLobbyGame == null ) {
									// okay, now move forward with creation.
									if ( record.mWorkOrder == null ) {
										// If there is no work order, request one and re-post.
										try {
											record.mWorkOrder = mLobby.openGameRequest() ;
											noteSuccess( record, Delegate.ACTION_OPEN_GAME ) ;
											// repost to continue
											sendMessageDelayed(
													obtainMessage( MESSAGE_TYPE_CREATE, record ),
													CREATE_STEP_DELAY) ;
										} catch ( CommunicationErrorException cee ) {
											delay = noteFailureAndGetDelay( record, Delegate.ACTION_OPEN_GAME, cee ) ;
											// whoops
											sendMessageDelayed(
													obtainMessage( MESSAGE_TYPE_CREATE, record ),
													FAIL_RETRY_DELAY + delay) ;
										}
									} else if ( !record.mWorkOrder.isComplete() ) {
										// If there is an incomplete work order, do some work and then
										// re-post.  This should allow other messages time to be processed,
										// while we spend the rest of our time working on this.
										// However, if there are no messages waiting, then there is no
										// downside to repeatedly performing work (spinning the thread
										// here rather than re-posting).
										record.mWorkOrder.performWork(500) ;		// very important to do at least one!
										while ( !record.mWorkOrder.isComplete() && !hasMessages() )
											record.mWorkOrder.performWork(500) ;
										// repost
										sendMessageDelayed(
												obtainMessage( MESSAGE_TYPE_CREATE, record),
												record.mWorkOrder.isComplete() ? CREATE_STEP_DELAY : 0 ) ;
										// if ( new Random().nextInt(50) == 1 )
											// Log.d(TAG, "handleMessage: resending MESSAGE_TYPE_CREATE continuing work order (random 1/50th log)") ;
									} else {
										// complete work order.  Confirm it.
										try {
											record.mInternetLobbyGame = mLobby.openGameConfirm(record.mWorkOrder) ;
											noteSuccess( record, Delegate.ACTION_OPEN_GAME ) ;
											record.mWorkOrder = null ;
											// move to the unhosted list...
											boolean needsMore = false ;
											synchronized( mRecordsMutex ) {
												mRecordsCreating.remove(record) ;
												if ( !mRecordsUnhosted.contains(record) )
													mRecordsUnhosted.add(record) ;
												mAllRecords.put(record.mInternetLobbyGame.getNonce(), record) ;
												needsMore = mRecordsUnhosted.size() + mRecordsCreating.size() < mPrecreatedGames 
														&& !hasMessages(MESSAGE_TYPE_CREATE) ;
											}
											// repost to maintain
											if ( mMaintainEvery > 0 && !this.hasMessages(MESSAGE_TYPE_MAINTAIN, record) )
												sendMessageDelayed(
														obtainMessage( MESSAGE_TYPE_MAINTAIN, record ),
														mMaintainEvery) ;
											if ( mRefreshEvery > 0 && !this.hasMessages(MESSAGE_TYPE_REFRESH, record) )
												sendMessageDelayed(
														obtainMessage( MESSAGE_TYPE_REFRESH, record ),
														mRefreshEvery) ;
											// post a fresh new creation IF we need more and there is
											// not already a creation message present.
											if ( needsMore ) {
												sendMessage( obtainMessage( MESSAGE_TYPE_CREATE, null ) ) ;
											}
										} catch ( CommunicationErrorException cee ) {
											// we might want to retire the work order.
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
												record.mWorkOrder = null ;
											
											delay = noteFailureAndGetDelay( record, Delegate.ACTION_OPEN_GAME, cee ) ;
											// whoops
											sendMessageDelayed(
													obtainMessage( MESSAGE_TYPE_CREATE, record ),
													FAIL_RETRY_DELAY + delay) ;
										}
									}
								}
							}
						}
						break ;
						
					case MESSAGE_TYPE_MAINTAIN:
						// the lobby must not be closed.
						if ( mLobby.getStatus() == WebConsts.STATUS_CLOSED || mLobby.getStatus() == WebConsts.STATUS_REMOVED )
							break ;
						
						
						// remove any duplicate messages.
						removeMessages(MESSAGE_TYPE_MAINTAIN, record) ;
						
						// first check: this record MUST be in recordsUnhosted.
						if ( mRecordsUnhosted.contains(record) ) {
							// depending on work order status, we do one of several things.
							if ( record.mWorkOrder == null ) {
								// If there is no work order, request one and re-post.
								try {
									record.mWorkOrder = record.mInternetLobbyGame.maintainRequest() ;
									noteSuccess( record, Delegate.ACTION_MAINTAIN_GAME ) ;
									// repost to continue
									sendMessageDelayed(
											obtainMessage( MESSAGE_TYPE_MAINTAIN, record ),
											MAINTAIN_STEP_DELAY) ;
								} catch ( CommunicationErrorException cee ) {
									// if it was refused due to being 'closed', move it to the closed list...
									// AND check whether we need to create a new one!
									if ( cee.getError() == WebConsts.ERROR_REFUSED && cee.getErrorReason() == WebConsts.REASON_CLOSED ) {
										synchronized ( mRecordsMutex ) {
											mRecordsUnhosted.remove(record) ;
											if ( !mRecordsClosed.contains(record) )
												mRecordsClosed.add(record) ;
											
											// schedule a refresh.
											sendMessageDelayed(
													obtainMessage( MESSAGE_TYPE_REFRESH, record ),
													FAIL_RETRY_DELAY) ;
											
											// start a new creation?
											if ( !hasMessages(MESSAGE_TYPE_CREATE) && mRecordsUnhosted.size() + mRecordsCreating.size() < mPrecreatedGames ) {
												sendMessage( obtainMessage( MESSAGE_TYPE_CREATE, null ) ) ;
											}
										}
									}
									delay = noteFailureAndGetDelay( record, Delegate.ACTION_MAINTAIN_GAME, cee ) ;
									// whoops.  If closed or removed, this is redundant, but that's fine.  When
									// this message rolls around we will immediately skip if the record is not
									// found in the right place.
									sendMessageDelayed(
											obtainMessage( MESSAGE_TYPE_MAINTAIN, record ),
											FAIL_RETRY_DELAY + delay) ;
								}
							} else if ( !record.mWorkOrder.isComplete() ) {
								// If there is an incomplete work order, do some work and then
								// re-post.  This should allow other messages time to be processed,
								// while we spend the rest of our time working on this.
								// However, if there are no messages waiting, then there is no
								// downside to repeatedly performing work (spinning the thread
								// here rather than re-posting).
								record.mWorkOrder.performWork(500) ;
								while ( !record.mWorkOrder.isComplete() && !hasMessages() )
									record.mWorkOrder.performWork(500) ;
								// repost
								long redemption_wait = record.mWorkOrder.hasDeadline()
										? Math.max(0, record.mWorkOrder.getDeadline() - System.currentTimeMillis() )
										: MAINTAIN_STEP_DELAY ;
								sendMessageDelayed(
										obtainMessage( MESSAGE_TYPE_MAINTAIN, record ),
										record.mWorkOrder.isComplete() ? redemption_wait : 0 ) ;
							} else {
								// complete work order.  Confirm it.
								try {
									record.mInternetLobbyGame.maintainConfirm(record.mWorkOrder) ;
									noteSuccess( record, Delegate.ACTION_MAINTAIN_GAME ) ;
									record.mWorkOrder = null ;
									// repost to maintain
									if ( mMaintainEvery > 0 )
										sendMessageDelayed(
												obtainMessage( MESSAGE_TYPE_MAINTAIN, record ),
												mMaintainEvery) ;
								} catch ( CommunicationErrorException cee ) {
									// has it been ended or removed?
									if ( cee.getError() == WebConsts.ERROR_REFUSED && cee.getErrorReason() == WebConsts.REASON_CLOSED ) {
										synchronized ( mRecordsMutex ) {
											mRecordsUnhosted.remove(record) ;
											if ( !mRecordsClosed.contains(record) )
												mRecordsClosed.add(record) ;
											
											// schedule a refresh.
											sendMessageDelayed(
													obtainMessage( MESSAGE_TYPE_REFRESH, record ),
													FAIL_RETRY_DELAY) ;
											
											// start a new creation?
											if ( !hasMessages(MESSAGE_TYPE_CREATE) && mRecordsUnhosted.size() + mRecordsCreating.size() < mPrecreatedGames ) {
												sendMessage( obtainMessage( MESSAGE_TYPE_CREATE, null ) ) ;
											}
										}
									}
									
									// we might want to retire the work order.
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
										record.mWorkOrder = null ;
									
									delay = noteFailureAndGetDelay( record, Delegate.ACTION_MAINTAIN_GAME, cee ) ;
									// whoops.  If closed or removed, this is redundant, but that's fine.  When
									// this message rolls around we will immediately skip if the record is not
									// found in the right place.
									sendMessageDelayed(
											obtainMessage( MESSAGE_TYPE_MAINTAIN, record ),
											FAIL_RETRY_DELAY + delay) ;
								}
							}
						}
						
						// Just in case, check whether we should start up a new record creation process.
						synchronized( mRecordsMutex ) {
							if ( !hasMessages(MESSAGE_TYPE_CREATE)
									&& mRecordsUnhosted.size() + mRecordsCreating.size() < mPrecreatedGames ) {
								sendMessage( obtainMessage( MESSAGE_TYPE_CREATE, null )) ;
							}
						}
						
						break ;
						
						
						
					case MESSAGE_TYPE_CLOSE:
						// Close the specified game.  Closing does not require any special
						// process; it's pretty much a single call.
						
						// remove any duplicate messages.
						removeMessages(MESSAGE_TYPE_CLOSE, record) ;
						
						// we can close even if the lobby is closed.  We allow
						// the closing of both unhosted and hosted games.
						if ( mRecordsUnhosted.contains(record) || mRecordsHosted.contains(record) ) {
							// perform a close action if we need to
							if ( record.mInternetLobbyGame.getStatus() != WebConsts.STATUS_CLOSED
									&& record.mInternetLobbyGame.getStatus() != WebConsts.STATUS_REMOVED ) {
								
								try {
									record.mInternetLobbyGame.close() ;
									noteSuccess( record, Delegate.ACTION_CLOSE_GAME ) ;
								} catch (CommunicationErrorException cee) {
									delay = noteFailureAndGetDelay( record, Delegate.ACTION_CLOSE_GAME, cee ) ;
									// whoops
									sendMessageDelayed(
											obtainMessage( MESSAGE_TYPE_CLOSE, record ),
											FAIL_RETRY_DELAY + delay) ;
								}
							}
							
							// if currently closed (either because we closed it or 
							// it was closed already...), close it now.
							// move to our closed list.
							if ( ( record.mInternetLobbyGame.getStatus() == WebConsts.STATUS_CLOSED
									|| record.mInternetLobbyGame.getStatus() == WebConsts.STATUS_REMOVED )
									&& ( mRecordsUnhosted.contains(record) || mRecordsHosted.contains(record) ) ) {
								synchronized( mRecordsMutex ) {
									mRecordsUnhosted.remove(record) ;
									mRecordsHosted.remove(record) ;
									if ( !mRecordsClosed.contains(record) )
										mRecordsClosed.add(record) ;
								}
							}
						}
						
						// Just in case, check whether we should start up a new record creation process.
						synchronized( mRecordsMutex ) {
							if ( !hasMessages(MESSAGE_TYPE_CREATE)
									&& mRecordsUnhosted.size() + mRecordsCreating.size() < mPrecreatedGames ) {
								sendMessage( obtainMessage( MESSAGE_TYPE_CREATE, null )) ;
							}
						}
						
						break ;
						
						
					case MESSAGE_TYPE_REFRESH:
						// remove any duplicate messages.
						removeMessages(MESSAGE_TYPE_REFRESH, record) ;
						
						
						// there is no need to refresh if it is already closed
						// or not yet open.
						if ( record.mInternetLobbyGame.getStatus() != WebConsts.STATUS_CLOSED
								&& record.mInternetLobbyGame.getStatus() != WebConsts.STATUS_REMOVED ) {
							
							try {
								record.mInternetLobbyGame.refresh() ;
								noteSuccess( record, Delegate.ACTION_REFRESH_GAME ) ;
							} catch (CommunicationErrorException cee) {
								delay = noteFailureAndGetDelay( record, Delegate.ACTION_REFRESH_GAME, cee ) ;
								// whoops
								sendMessageDelayed(
										obtainMessage( MESSAGE_TYPE_REFRESH, record ),
										FAIL_RETRY_DELAY + delay) ;
							}
							
						}
						
						// if closed and not in the closed list, move it there.
						if ( ( record.mInternetLobbyGame.getStatus() == WebConsts.STATUS_CLOSED
								|| record.mInternetLobbyGame.getStatus() == WebConsts.STATUS_REMOVED )
								&& ( mRecordsUnhosted.contains(record) || mRecordsHosted.contains(record) ) ) {
							synchronized( mRecordsMutex ) {
								mRecordsUnhosted.remove(record) ;
								mRecordsHosted.remove(record) ;
								if ( !mRecordsClosed.contains(record) )
									mRecordsClosed.add(record) ;
							}
						}
						
						// reschedule
						if ( record.mInternetLobbyGame.getStatus() != WebConsts.STATUS_CLOSED
								&& record.mInternetLobbyGame.getStatus() != WebConsts.STATUS_REMOVED
								&& mRefreshEvery > 0
								&& !this.hasMessages(MESSAGE_TYPE_REFRESH, record ) ) {
							sendMessageDelayed(
									obtainMessage( MESSAGE_TYPE_REFRESH, record ),
									mRefreshEvery) ;
						}
						
						// Just in case, check whether we should start up a new record creation process.
						synchronized( mRecordsMutex ) {
							if ( !hasMessages(MESSAGE_TYPE_CREATE)
									&& mRecordsUnhosted.size() + mRecordsCreating.size() < mPrecreatedGames ) {
								sendMessage( obtainMessage( MESSAGE_TYPE_CREATE, null )) ;
							}
						}
						
						break ;
						
						
					}
					
				}
				
				
				
				/**
				 * Returns 'true' if there are currently messages enqueued.
				 * This method is NOT thread-safe, and race conditions may result
				 * in an incorrect response.
				 * 
				 * Therefore, recommended usage is to use the result of this call
				 * to influence the length of a delay, but NOT whether or not an
				 * action is actually taken.  For instance, if you are performing
				 * a stage-by-stage action, it would make sense to enqueue the next
				 * stage with a positive delay if hasMessages(), but with no delay
				 * if not.
				 * 
				 * @return
				 */
				private boolean hasMessages() {
					for ( int i = 0; i < NUM_MESSAGE_TYPES; i++ )
						if ( hasMessages(i) )
							return true ;
					return false ;
				}
				
			} ;
			
			Looper.loop() ;
		}
		
		
		
	}
	
	
}
