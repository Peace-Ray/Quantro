package com.peaceray.quantro.model;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Hashtable;

import com.peaceray.quantro.model.game.Game;
import com.peaceray.quantro.model.game.GameEvents;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.game.GameSettings;
import com.peaceray.quantro.model.state.SerializableState;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


/**
 * Provides static methods for saving/loading Quantro game states
 * (game, ginfo, gevents, game system states) to long-term memory
 * on Android.
 * 
 * @author Jake
 *
 */
public class GameSaver {
	
	private class GameSaverThread extends Thread {
		Handler mHandler ;
		WeakReference<Context> mwrContext ;
		Object mTag ;
		
		private static final int GST_WHAT_SAVE_GAME = 0 ;
		private static final int GST_WHAT_SAVE_CHECKPOINT = 1 ;
		private static final int GST_WHAT_SAVE_EPHEMERAL_CHECKPOINT = 2 ;
		private static final int GST_WHAT_SAVE_THUMBNAIL = 3 ;
		private static final int GST_WHAT_LOAD_GAME_STATES = 4 ;
		private static final int GST_WHAT_LOAD_CHECKPOINT_STATES = 5 ;
		private static final int GST_WHAT_LOAD_GAME_RESULT = 6 ;
		private static final int GST_WHAT_LOAD_THUMBNAIL = 7 ;
		private static final int GST_WHAT_LOAD_CHECKPOINT_RESULT = 8 ;
		private static final int GST_WHAT_DELETE_GAME = 9 ;
		private static final int GST_WHAT_STOP = 10 ;
		private static final int GST_NUM_WHAT = 11 ;
		
		private GameSaverThread( Context context, Object tag ) {
			mHandler = null ;
			mwrContext = new WeakReference<Context>(context) ;
			mTag = tag ;
		}
		
		@Override
		public void run() {
			Looper.prepare();
			Log.d(TAG, "Looper Prepared") ;
			mHandler = new Handler() {
				@Override
				public void handleMessage(android.os.Message msg) {
					
					int what = msg.what ;
					int checkpointNum = msg.arg1 ;
					int thumbnailWidth = msg.arg1 ;
					int thumbnailHeight = msg.arg2 ;
					Object [] objs = (Object [])msg.obj ;
					msg.obj = null ;	// null it out for safety

					Listener listener = null ;
					String key = null ;
					Game game = null ;
					GameResult gr = null ;
					GameSettings gs = null ;
					Bitmap thumbnail = null ;
					
					Context context = mwrContext.get() ;
					
					if ( objs != null ) {
						if ( objs[0] != null )
							listener = ((WeakReference<Listener>)objs[0]).get() ;
						key = (String)objs[1] ;
						game = (Game)objs[2] ;
						gr = (GameResult)objs[3] ;
						gs = (GameSettings)objs[4] ;
						thumbnail = (Bitmap)objs[5] ;
						
						
						if ( (listener == null || context == null ) && what == GST_WHAT_STOP ) {
							mHandler.sendMessage(mHandler.obtainMessage(GST_WHAT_STOP)) ;
							return ;
						}
					}
					
					boolean success ;
					
					switch ( what ) {
					case GST_WHAT_SAVE_GAME:
						Log.d(TAG, "GST_WHAT_SAVE_GAME") ;
						success = GameSaver.saveGame(context, key, game, gr, gs, thumbnail) ;
						if ( listener != null )
							listener.gsl_doneSavingGame(GameSaver.this, mTag, success, hasMessages(), key, game, gr) ;
						break ;
					case GST_WHAT_SAVE_CHECKPOINT:
						Log.d(TAG, "GST_WHAT_SAVE_CHECKPOINT") ;
						success = GameSaver.saveCheckpoint(context, key, checkpointNum, game, gr) ;
						if ( listener != null )
							listener.gsl_doneSavingCheckpoint(GameSaver.this, mTag, success, hasMessages(), key, checkpointNum, game, gr) ;
						break ;
					case GST_WHAT_SAVE_EPHEMERAL_CHECKPOINT:
						Log.d(TAG, "GST_WHAT_SAVE_EPHEMERAL_CHECKPOINT") ;
						success = GameSaver.saveEphemeralCheckpoint(context, key, checkpointNum, game, gr) ;
						if ( listener != null )
							listener.gsl_doneSavingEphemeralCheckpoint(GameSaver.this, mTag, success, hasMessages(), key, checkpointNum, game, gr) ;
						break ;
						case GST_WHAT_SAVE_THUMBNAIL:
							Log.d(TAG, "GST_WHAT_SAVE_THUMBNAIL") ;
							success = GameSaver.saveGameThumbnail(context, key, thumbnail) ;
							if ( listener != null )
								listener.gsl_doneSavingThumbnail(GameSaver.this, mTag, success, hasMessages(), key) ;
							break ;
					case GST_WHAT_LOAD_GAME_STATES:
						Log.d(TAG, "GST_WHAT_LOAD_GAME_STATES") ;
						success = GameSaver.loadGameStates(context, key, game) ;
						if ( listener != null )
							listener.gsl_doneLoadingGameStates(GameSaver.this, mTag, success, hasMessages(), key, game) ;
						break ;
					case GST_WHAT_LOAD_CHECKPOINT_STATES:
						Log.d(TAG, "GST_WHAT_LOAD_CHECKPOINT_STATES") ;
						success = GameSaver.loadCheckpointStates(context, key, checkpointNum, game) ;
						if ( listener != null )
							listener.gsl_doneLoadingCheckpointStates(GameSaver.this, mTag, success, hasMessages(), key, checkpointNum, game) ;
						break ;
					case GST_WHAT_LOAD_GAME_RESULT:
						Log.d(TAG, "GST_WHAT_LOAD_GAME_RESULT") ;
						gr = GameSaver.loadGameResult(context, key) ;
						if ( listener != null )
							listener.gsl_doneLoadingGameResult(GameSaver.this, mTag, gr != null, hasMessages(), key, gr) ;
						break ;
					case GST_WHAT_LOAD_THUMBNAIL:
						Log.d(TAG, "GST_WHAT_LOAD_THUMBNAIL") ;
						Bitmap b ;
						if ( thumbnailWidth <= 0 || thumbnailHeight <= 0 )
							b = GameSaver.loadGameThumbnail(context, key) ;
						else
							b = GameSaver.loadGameThumbnail(context, key, thumbnailWidth, thumbnailHeight, true) ;
						if ( listener != null )
							listener.gsl_doneLoadingGameThumbnail(GameSaver.this, mTag, b != null, hasMessages(), key, b) ;
						break ;
					case GST_WHAT_LOAD_CHECKPOINT_RESULT:
						Log.d(TAG, "GST_WHAT_LOAD_CHECKPOINT_RESULT") ;
						gr = GameSaver.loadCheckpointResult(context, key, checkpointNum) ;
						if ( listener != null )
							listener.gsl_doneLoadingCheckpointResult(GameSaver.this, mTag, gr != null, hasMessages(), key, checkpointNum, gr) ;
						break ;
					case GST_WHAT_DELETE_GAME:
						Log.d(TAG, "GST_WHAT_DELETE_GAME") ;
						success = GameSaver.deleteGame(context, key) ;
						if ( listener != null )
							listener.gsl_doneDeletingGame(GameSaver.this, mTag, success, hasMessages(), key) ;
						break ;
					case GST_WHAT_STOP:
						Log.d(TAG, "GST_WHAT_STOP") ;
						if ( listener != null )
							listener.gsl_stopped(GameSaver.this, mTag) ;
						getLooper().quit() ;
						break ;
					}
				}
				
				public boolean hasMessages() {
					for ( int i = 0; i < GST_WHAT_STOP; i++ )
						if ( this.hasMessages(i) )
							return true ;
					return false ;
				}
				
			} ;
			
			Log.d(TAG, "Looper starting") ;
			Looper.loop();
			Log.d(TAG, "Looper quitting") ;
		}
	}
	
	GameSaverThread mThread ;
	WeakReference<Listener> mwrListener ;
	
	public GameSaver(  ) {
		
	}
	
	public GameSaver start( Context context, Listener listener, Object tag ) {
		if ( mThread != null )
			throw new IllegalStateException("Inner mThread has already been started!") ;
		
		mwrListener = new WeakReference<Listener>(listener) ;
		mThread = new GameSaverThread(context, tag) ;
		mThread.start();
		while ( mThread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		return this ;
	}
	
	public GameSaver saveGame( String key, Game game, GameResult gr, GameSettings gs, Bitmap thumbnail ) {
		Object [] objs = makeMessageObjs( key, game, gr, gs, thumbnail ) ;
		while ( mThread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		
		mThread.mHandler.sendMessage(
				mThread.mHandler.obtainMessage(GameSaverThread.GST_WHAT_SAVE_GAME, 0, 0, objs)
				) ;
		return this ;
	}
	
	public GameSaver saveCheckpoint( String key, int num, Game game, GameResult gr ) {
		Object [] objs = makeMessageObjs( key, game, gr, null, null ) ;
		while ( mThread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		mThread.mHandler.sendMessage(
				mThread.mHandler.obtainMessage(GameSaverThread.GST_WHAT_SAVE_CHECKPOINT, num, 0, objs)
		) ;
		return this ;
	}
	
	public GameSaver saveEphemeralCheckpoint( String key, int num, Game game, GameResult gr ) {
		Object [] objs = makeMessageObjs( key, game, gr, null, null ) ;
		while ( mThread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		mThread.mHandler.sendMessage(
				mThread.mHandler.obtainMessage(GameSaverThread.GST_WHAT_SAVE_EPHEMERAL_CHECKPOINT, num, 0, objs)
		) ;
		return this ;
	}
	
	public GameSaver saveGameThumbnail( String key, Bitmap thumbnail ) {
		Object [] objs = makeMessageObjs( key, null, null, null, thumbnail ) ;
		while ( mThread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		
		mThread.mHandler.sendMessage(
				mThread.mHandler.obtainMessage(GameSaverThread.GST_WHAT_SAVE_THUMBNAIL, 0, 0, objs)
				) ;
		return this ;
	}
	
	public GameSaver loadGameStates( String key, Game game ) {
		Object [] objs = makeMessageObjs( key, game, null, null, null ) ;
		while ( mThread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		mThread.mHandler.sendMessage(
				mThread.mHandler.obtainMessage(GameSaverThread.GST_WHAT_LOAD_GAME_STATES, 0, 0, objs)
		) ;
		return this ;
	}
	
	public GameSaver loadCheckpointStates( String key, int num, Game game ) {
		Object [] objs = makeMessageObjs( key, game, null, null, null ) ;
		while ( mThread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		mThread.mHandler.sendMessage(
				mThread.mHandler.obtainMessage(GameSaverThread.GST_WHAT_LOAD_CHECKPOINT_STATES, num, 0, objs)
		) ;
		return this ;
	}
	
	public GameSaver loadGameResult( String key ) {
		Object [] objs = makeMessageObjs( key, null, null, null, null ) ;
		while ( mThread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		mThread.mHandler.sendMessage(
				mThread.mHandler.obtainMessage(GameSaverThread.GST_WHAT_LOAD_GAME_RESULT, 0, 0, objs)
		) ;
		return this ;
	}
	
	public GameSaver loadThumbnail( String key ) {
		Object [] objs = makeMessageObjs( key, null, null, null, null ) ;
		while ( mThread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		mThread.mHandler.sendMessage(
				mThread.mHandler.obtainMessage(GameSaverThread.GST_WHAT_LOAD_THUMBNAIL, 0, 0, objs)
		) ;
		return this ;
	}
	
	public GameSaver loadCheckpointResult( String key, int num ) {
		Object [] objs = makeMessageObjs( key, null, null, null, null ) ;
		while ( mThread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		mThread.mHandler.sendMessage(
				mThread.mHandler.obtainMessage(GameSaverThread.GST_WHAT_LOAD_CHECKPOINT_RESULT, num, 0, objs)
		) ;
		return this ;
	}
	
	public GameSaver deleteGame( String key ) {
		Object objs = makeMessageObjs( key, null, null, null, null ) ;
		while ( mThread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		mThread.mHandler.sendMessage(
				mThread.mHandler.obtainMessage(GameSaverThread.GST_WHAT_DELETE_GAME, 0, 0, objs)
		) ;
		return this ;
	}
	
	public GameSaver stop() {
		if ( mThread == null )
			throw new IllegalStateException("Wasn't started!") ;
		
		mwrListener = null ;
		Object [] objs = makeMessageObjs( null, null, null, null, null ) ;
		while ( mThread.mHandler == null ) {
			try { Thread.sleep(3) ; } catch( InterruptedException e ) { }
		}
		mThread.mHandler.sendMessage(
				mThread.mHandler.obtainMessage(GameSaverThread.GST_WHAT_STOP, 0, 0, objs)
		) ;
		mThread = null ;
		return this ;
	}

	private Object [] makeMessageObjs( String key, Game game, GameResult gr, GameSettings gs, Bitmap thumbnail ) {
		return new Object[] { mwrListener, key, game, gr, gs, thumbnail } ;
	}
	
	public interface Listener {
		
		/**
		 * Indicates to the Listener that we have finished saving the game.
		 * @param success
		 * @param key
		 * @param game
		 * @param gr
		 */
		public void gsl_doneSavingGame( GameSaver gs, Object tag, boolean success, boolean hasMore, String key, Game game, GameResult gr ) ;
		
		/**
		 * Indicates to the Listener that we have finished saving the checkpoint.
		 * @param success
		 * @param hasMore This game saver has more work to do on another request.
		 * @param key
		 * @param checkpointNum
		 * @param game
		 * @param gr
		 */
		public void gsl_doneSavingCheckpoint( GameSaver gs, Object tag,  boolean success, boolean hasMore, String key, int checkpointNum, Game game, GameResult gr ) ;
		
		/**
		 * Indicates to the Listener that we have finished saving the checkpoint.
		 * @param success
		 * @param key
		 * @param checkpointNum
		 * @param game
		 * @param gr
		 */
		public void gsl_doneSavingEphemeralCheckpoint( GameSaver gs, Object tag,  boolean success, boolean hasMore, String key, int checkpointNum, Game game, GameResult gr ) ;
		
		/**
		 * Indicates to the listener that we have finished saving the thumbnail.
		 * @param gs
		 * @param tag
		 * @param success
		 * @param hasMore
		 * @param key
		 */
		public void gsl_doneSavingThumbnail( GameSaver gs, Object tag, boolean success, boolean hasMore, String key ) ;
		
		/**
		 * Indicates to the Listener that we have finished loading game states.
		 * @param gs
		 * @param success
		 * @param hasMore
		 * @param key
		 * @param game
		 */
		public void gsl_doneLoadingGameStates( GameSaver gs, Object tag,  boolean success, boolean hasMore, String key, Game game ) ;
		
		/**
		 * Indicates to the Listener that we have finished loading the checkpoint states.
		 * @param gs
		 * @param success
		 * @param hasMore
		 * @param key
		 * @param checkpointNum
		 * @param game
		 */
		public void gsl_doneLoadingCheckpointStates( GameSaver gs, Object tag,  boolean success, boolean hasMore, String key, int checkpointNum, Game game ) ;
		
		/**
		 * Indicates to the Listener that we have finished loading the game result.
		 * @param gs
		 * @param success
		 * @param hasMore
		 * @param key
		 * @param gr
		 */
		public void gsl_doneLoadingGameResult( GameSaver gs, Object tag,  boolean success, boolean hasMore, String key, GameResult gr ) ;
		
		/**
		 * Indicates to the Listener that we have finished loading the game thumbnail.
		 * @param gs
		 * @param tag
		 * @param success
		 * @param hasMore
		 * @param key
		 * @param b
		 */
		public void gsl_doneLoadingGameThumbnail( GameSaver gs, Object tag, boolean success, boolean hasMore, String key, Bitmap b ) ;
		
		/**
		 * Indicates to the Listener that we have finished loading the checkpoint result.
		 * @param gs
		 * @param success
		 * @param hasMore
		 * @param key
		 * @param checkpointNum
		 * @param gr
		 */
		public void gsl_doneLoadingCheckpointResult( GameSaver gs, Object tag,  boolean success, boolean hasMore, String key, int checkpointNum, GameResult gr ) ;
	
		/**
		 * Indicates to the Listener that we have finished deleting the specified game.
		 * @param gs
		 * @param tag
		 * @param success
		 * @param hasMore
		 * @param key
		 */
		public void gsl_doneDeletingGame(GameSaver gs, Object tag, boolean success, boolean hasMore, String key) ;
		
		/**
		 * Indicates to the Listener that the thread is stopping.
		 */
		public void gsl_stopped( GameSaver gs, Object tag ) ;
	}
	
	
	
	/**
	 * The standard GameSaver instantiable class provides a "set and forget"
	 * method for loading and / or saving; you don't need to remember the
	 * commands entered, because GameSaver will remind you of them in the listener
	 * methods.
	 * 
	 * This is not necessarily the best approach for some applications; for example,
	 * displaying thumbnails in a list view, where individual views might change
	 * their game modes before, during or after the load; GameSaver provides no
	 * convenient way to cancel existing requests.
	 * 
	 * The ThumbnailLoader provides a better approach: individual clients add
	 * themselves to the queue, and then are queried for their specific loading
	 * parameters when their place in the queue is reached.
	 * 
	 * @author Jake
	 *
	 */
	public static class ThumbnailLoader {
		
		public static class Params {
			public enum ScaleType {
				/**
				 * Load the image at its full resolution, ignoring
				 * 'width' and 'height'
				 */
				FULL,
				
				/**
				 * Load the image downscaled to fit within the specified width and height.
				 */
				FIT,
				
				/**
				 * Load the image downscaled so that at least one dimension
				 * fits within the specified width or height.
				 */
				FIT_X_OR_Y
			}
			
			public ScaleType scaleType ;
			public int width, height ;
			public String loadKey ;
			public int gameMode ;
		}
		
		public interface Client {
			
			/**
			 * This client's place in line has been reached.  What
			 * parameters should we use to load a thumbnail?  If 'null'
			 * is returned, we do nothing and move to the next client.
			 * 
			 * @param tl
			 * @return
			 */
			public Params tlc_getParams( ThumbnailLoader tl ) ;
			
			/**
			 * The load has been completed.  The provided bitmap -- or null, if none
			 * -- is the result of loading with the provided parameters (the same object
			 * provided by the call to tlc_getParams()).
			 * 
			 * @param tl
			 * @param p
			 * @param b
			 * @return
			 */
			public void tlc_hasLoaded( ThumbnailLoader tl, Params p, Bitmap b ) ;
			
		}
		

		private class ThumbnailLoaderThread extends Thread {
			Handler mHandler ;
			
			private static final int TLT_WHAT_LOAD_THUMBNAIL = 0 ;
			private static final int TLT_WHAT_STOP = 1 ;
			private static final int TLT_NUM_WHAT = 2 ;
			
			private ThumbnailLoaderThread() {
				mHandler = null ;
			}
			
			@Override
			public void run() {
				Looper.prepare();
				Log.d(TAG, "Looper Prepared") ;
				mHandler = new Handler() {
					@Override
					public void handleMessage(android.os.Message msg) {
						
						int what = msg.what ;
						Client client = ((WeakReference<Client>)msg.obj).get() ;
						msg.obj = null ;	// null it out for safety
						
						Context context = mwrContext.get() ;
						
						switch ( what ) {
						case TLT_WHAT_LOAD_THUMBNAIL:
							Log.d(TAG, "TLT_WHAT_LOAD_THUMBNAIL") ;
							if ( context == null )
								return ;
							if ( client == null )
								return ;
							Params params = client.tlc_getParams(ThumbnailLoader.this) ;
							if ( params == null )
								return ;
							// we have load params now!
							Bitmap b ;
							if ( params.scaleType == Params.ScaleType.FULL || params.width <= 0 || params.height <= 0 ) {
								b = GameSaver.loadGameThumbnail(context, params.loadKey) ;
							} else if ( params.scaleType == Params.ScaleType.FIT ) {
								b = GameSaver.loadGameThumbnail(context, params.loadKey, params.width, params.height, true) ;
							}  else {
								b = GameSaver.loadGameThumbnail(context, params.loadKey, params.width, params.height, false) ;
							}
							client.tlc_hasLoaded(ThumbnailLoader.this, params, b) ;
							break ;
							
						case TLT_WHAT_STOP:
							Log.d(TAG, "TLT_WHAT_STOP") ;
							getLooper().quit() ;
							break ;
						}
					}
					
				} ;
				
				Log.d(TAG, "Looper starting") ;
				Looper.loop();
			}
			
			public void terminate() {
				for ( int i = 0; i < TLT_NUM_WHAT; i++ ) {
					if ( i != TLT_WHAT_STOP ) {
						mHandler.removeMessages(i) ;
					}
				}
				mHandler.sendMessage(mHandler.obtainMessage(TLT_WHAT_STOP)) ;
			}
			
		}
		
		WeakReference<Context> mwrContext ;
		ThumbnailLoaderThread mThread = null ;
		
		public ThumbnailLoader( Context context ) {
			mwrContext = new WeakReference<Context>(context) ;
			mThread = null ;
		}
		
		/**
		 * Adds this client to our load queue.  The client will, at some
		 * future point, have .tlc_getParams() called on it by this object.
		 * @param client
		 */
		public synchronized void addClient( Client client ) {
			if ( client == null ) {
				throw new NullPointerException("Can't add a null client!") ;
			}
			
			if ( mThread == null ) {
				mThread = new ThumbnailLoaderThread() ;
				mThread.start() ;
				while( mThread.mHandler == null ) {
					try {
						Thread.sleep(50) ;
					} catch (InterruptedException e) {
						e.printStackTrace();
						return ;
					}
				}
			}
			
			mThread.mHandler.sendMessage(mThread.mHandler.obtainMessage(ThumbnailLoaderThread.TLT_WHAT_LOAD_THUMBNAIL, new WeakReference<Client>(client))) ;
		}
		
		public synchronized void stop() {
			if ( mThread != null ) {
				mThread.mHandler.sendMessage(mThread.mHandler.obtainMessage(ThumbnailLoaderThread.TLT_WHAT_STOP)) ;
				mThread = null ;
			}
		}
		
		/**
		 * An immediate stop that prevents any queued clients have loading data.
		 */
		public synchronized void stopImmediately() {
			if ( mThread != null ) {
				mThread.terminate() ;
				mThread = null ;
			}
		}
		
	}
	
	
	private static final int VERSION_PREVERSIONED = -1 ;
	private static final int VERSION = 0 ;
	// 0: first versioned number.  Represents the moment when qUpconversion is necessary.
	
	private static final Object GAME_SAVER_MUTEX = new Object() ;

	private static final String TAG = "GameSaver" ;
	
	private static final String SAVED_GAMES_DIRECTORY = "SavedGames" ;
	private static final String FILENAME_GAME = "game_state.bin" ;
	private static final String FILENAME_GAME_EVENTS = "gevents_state.bin" ;
	private static final String FILENAME_GAME_INFORMATION = "ginfo_state.bin" ;
	private static final String FILENAME_GAME_SYSTEMS = "systems_state.bin" ;
	private static final String FILENAME_VALID = "valid_save.txt" ;
	private static final String FILENAME_GAME_RESULT = "game_result.bin" ;
	private static final String FILENAME_GAME_RESULT_VALID = "valid_result.txt" ;
	private static final String FILENAME_GAME_SETTINGS = "game_settings.bin" ;
	private static final String FILENAME_GAME_SETTINGS_VALID = "valid_settings.bin" ;
	private static final String FILENAME_GAME_THUMBNAIL = "thumbnail.png" ;
	private static final String FILENAME_GAME_THUMBNAIL_TEMP = "thumbnail_temp.png" ;
	
	
	
	private static final String CHECKPOINT_DIRECTORY_PREFIX = "Checkpoint_" ;
	private static final String EPHEMERAL_CHECKPOINT_DIRECTORY_PREFIX = "Ephemeral_Checkpoint_" ;
	
	
	private static Hashtable<Integer, SoftReference<String>> FREE_PLAY_GAME_MODE_SAVE_KEY = new Hashtable<Integer, SoftReference<String>>() ;
	public static String freePlayGameModeToSaveKey( Integer gameMode ) {
		String key = null ;
		SoftReference<String> sw = FREE_PLAY_GAME_MODE_SAVE_KEY.get(gameMode) ;
		if ( sw != null ) {
			key = sw.get() ;
		}
		
		if ( key == null ) {
			key = "sp_save_" + gameMode.toString() ;
			FREE_PLAY_GAME_MODE_SAVE_KEY.put(gameMode, new SoftReference<String>(key)) ;
		}
		
		return key ;
	}
	
	
	/**
	 * Saves the given game (game, game.ginfo, game.gevents, and game systems) in
	 * a Serialized format to a directory derived from the provided key
	 * 
	 * ( SavedGames/<keys>/... )
	 * 
	 * These files may be later retrieved using loadGameStates,
	 * by providing the same key.
	 * 
	 * SIDE EFFECT: any Ephemeral Checkpoints become "real" checkpoints.
	 * 
	 * @param context Current context
	 * @param key Key determining file location
	 * @param game Game object to save
	 * @return Success (true) or failure (false).  Upon failure, a
	 * 			note will be entered in Log.d
	 */
	public static boolean saveGame(
			Context context, String key, Game game, GameResult gr, GameSettings gs, Bitmap thumbnail ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			
			// Writes current states to files:
			// SavedGames/<key>/game_state.bin
			//                 /gevents_state.bin
			//				   /ginfo_state.bin
			//                 /systems_states.bin
			//				   /valid_save.txt
			//				   /game_results.bin
			//                 /valid_result.bin
			//				   /game_settings.bin
			//				   /valid_settings.bin
			
			try {
				// Get the file to the directory.
				File keyDir = directoryFileFromKey( context, key ) ;
				
				materializeEphemeralCheckpoints( context, keyDir ) ;
				return saveGame( context, keyDir, game, gr, gs, thumbnail ) ;
				
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return false ;
			}
		
		}
	}
	
	
	/**
	 * Saves the provided game as a checkpoint.
	 * 
	 * Side effect: erases any corresponding ephemeral checkpoint.
	 * 
	 * @param context
	 * @param key
	 * @param checkpointNum
	 * @param game
	 * @param gr
	 * @return
	 */
	public static boolean saveCheckpoint(
			Context context, String key, int checkpointNum, Game game, GameResult gr ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			// Writes current states to files:
			// SavedGames/<key>/Checkpoint_<checkpointNum>/game_state.bin
			//                							  /gevents_state.bin
			//				   							  /ginfo_state.bin
			//                							  /systems_states.bin
			//				   							  /valid_save.txt
			//				   							  /game_results.bin
			//               							  /valid_result.bin
			
			try {
				// Get the file to the directory.
				File keyDir = directoryFileFromKey( context, key ) ;
				File checkpointDir = fileFromDirectoryFile( keyDir, CHECKPOINT_DIRECTORY_PREFIX + checkpointNum ) ;
				File ephemeralDir = fileFromDirectoryFile( keyDir, EPHEMERAL_CHECKPOINT_DIRECTORY_PREFIX + checkpointNum ) ;
				if ( ephemeralDir.exists() )
					delete(ephemeralDir) ;
				
				return saveGame( context, checkpointDir, game, gr, null, null ) ;
				
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return false ;
			}
		}
	}
	
	
	/**
	 * Saves the provided game as an ephemeral checkpoint.
	 * 
	 * Ephemeral checkpoints have odd behavior.  They will be loaded, and their existence
	 * checkable, as with normal checkpoints, until one of two things happens.
	 * 
	 * If this game (i.e., this key) is LOADED (the normal save, not a checkpoint), this
	 * ephemeral checkpoint will be destroyed.  This gives a way to preemptively save
	 * a checkpoint that we are not sure we will want - loading the game again destroys
	 * the checkpoint.
	 * 
	 * However, if this game (i.e., this key) is SAVED, all ephemeral checkpoints become
	 * normal checkpoints (overwriting any existent normal checkpoints).  From this point,
	 * load/save is safe and the checkpoints will be retained.
	 * 
	 * @param context
	 * @param key
	 * @param checkpointNum
	 * @param game
	 * @param gr
	 * @return
	 */
	public static boolean saveEphemeralCheckpoint(
			Context context, String key, int checkpointNum, Game game, GameResult gr ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			// Writes current states to files:
			// SavedGames/<key>/Checkpoint_<checkpointNum>/game_state.bin
			//                							  /gevents_state.bin
			//				   							  /ginfo_state.bin
			//                							  /systems_states.bin
			//				   							  /valid_save.txt
			//				   							  /game_results.bin
			//               							  /valid_result.bin
			
			try {
				// Get the file to the directory.
				File keyDir = directoryFileFromKey( context, key ) ;
				File ephemeralDir = fileFromDirectoryFile( keyDir, EPHEMERAL_CHECKPOINT_DIRECTORY_PREFIX + checkpointNum ) ;
				
				return saveGame( context, ephemeralDir, game, gr, null, null ) ;
				
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return false ;
			}
		}
	}
	
	
	
	/**
	 * Saves the given game (game, game.ginfo, game.gevents, and game systems) in
	 * a Serialized format to a directory derived from the provided key
	 * 
	 * ( SavedGames/<keys>/... )
	 * 
	 * These files may be later retrieved using loadGameStates,
	 * by providing the same key.
	 * 
	 * @param context Current context
	 * @param key Key determining file location
	 * @param game Game object to save
	 * @return Success (true) or failure (false).  Upon failure, a
	 * 			note will be entered in Log.d
	 */
	private static boolean saveGame(
			Context context, File directory, Game game, GameResult gr, GameSettings gs, Bitmap thumbnail) {
		
		// Writes current states to files:
		// SavedGames/<key>/game_state.bin
		//                 /gevents_state.bin
		//				   /ginfo_state.bin
		//                 /systems_states.bin
		//				   /valid_save.txt
		//		   /game_results.bin
		//                 /valid_result.bin
		//				   /game_settings.bin
		//				   /valid_settings.bin
		
		
		try {
			GameInformation ginfo = game.ginfo ;
			GameEvents gevents = game.gevents ;
			
			// Get the file to the directory.
			File validFile = fileFromDirectoryFile( directory, FILENAME_VALID ) ;
			// If does not exist, make it.  If it does, check and remove the "valid" file.
			if ( directory.exists() && validFile.exists() ) {
				validFile.delete() ;
			}
			else {
				directory.mkdirs() ;
			}
			
			File file ;
			
			file = fileFromDirectoryFile( directory, FILENAME_GAME ) ;
			writeSerializableStateToFile( context, file, game ) ;
			file = fileFromDirectoryFile( directory, FILENAME_GAME_INFORMATION ) ;
			writeSerializableStateToFile( context, file, ginfo ) ;
			file = fileFromDirectoryFile( directory, FILENAME_GAME_EVENTS ) ;
			writeSerializableStateToFile( context, file, gevents ) ;
			file = fileFromDirectoryFile( directory, FILENAME_GAME_SYSTEMS ) ;
			writeSerializableArrayToFile( context, file, game.getSerializablesFromSystems() ) ;
			
			if ( gr != null ) {
				File grValidFile = fileFromDirectoryFile( directory, FILENAME_GAME_RESULT_VALID ) ;
				// If does not exist, make it.  If it does, check and remove the "valid" file.
				if ( grValidFile.exists() ) {
					grValidFile.delete() ;
				}
				
				file = fileFromDirectoryFile( directory, FILENAME_GAME_RESULT ) ;
				writeSerializableObjectToFile( context, file, gr ) ;
				
				// Create valid file.
				writeValidFile( context, grValidFile ) ;
			}
			
			if ( gs != null ) {
				File gsValidFile = fileFromDirectoryFile( directory, FILENAME_GAME_SETTINGS_VALID ) ;
				// If does not exist, make it.  If it does, check and remove the "valid" file.
				if ( gsValidFile.exists() ) {
					gsValidFile.delete() ;
				}
				
				file = fileFromDirectoryFile( directory, FILENAME_GAME_SETTINGS ) ;
				writeSerializableObjectToFile( context, file, gs ) ;
				
				// Create valid file.
				writeValidFile( context, gsValidFile ) ;
			}
			
			// Thumbnail file.  Require an up-to-date version; delete the old if there is one.
			File thumbnailFile = fileFromDirectoryFile( directory, FILENAME_GAME_THUMBNAIL ) ;
			if ( thumbnail != null ) {
				// temp file...
				File thumbnailTempFile = fileFromDirectoryFile( directory, FILENAME_GAME_THUMBNAIL_TEMP ) ;
				// just in case the thumbnail is being updated...
				synchronized( thumbnail ) {
					if ( GameSaver.writeBitmapToFile( context, thumbnailTempFile, thumbnail ) ) {
						thumbnailTempFile.renameTo(thumbnailFile) ;
					}
				}
			}
			
			// Create valid file.
			writeValidFile( context, validFile ) ;
			
			Log.d(TAG, "game seems to have saved successfully") ;
			return true ;
			
		} catch (Exception e) {
			e.printStackTrace() ;
			Log.d(TAG, e.toString() ) ;
			return false ;
		}
	}
	
	
	
	/**
	 * Saves the given game (game, game.ginfo, game.gevents, and game systems) in
	 * a Serialized format to a directory derived from the provided key
	 * 
	 * ( SavedGames/<keys>/... )
	 * 
	 * These files may be later retrieved using loadGameStates,
	 * by providing the same key.
	 * 
	 * SIDE EFFECT: any Ephemeral Checkpoints become "real" checkpoints.
	 * 
	 * @param context Current context
	 * @param key Key determining file location
	 * @param game Game object to save
	 * @return Success (true) or failure (false).  Upon failure, a
	 * 			note will be entered in Log.d
	 */
	public static boolean saveGameThumbnail(
			Context context, String key, Bitmap thumbnail ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			
			// Writes current states to files:
			// SavedGames/<key>/game_state.bin
			//                 /gevents_state.bin
			//				   /ginfo_state.bin
			//                 /systems_states.bin
			//				   /valid_save.txt
			//				   /game_results.bin
			//                 /valid_result.bin
			//				   /game_settings.bin
			//				   /valid_settings.bin
			
			try {
				// Get the file to the directory.
				File keyDir = directoryFileFromKey( context, key ) ;
				
				return saveGameThumbnail( context, keyDir, thumbnail ) ;
				
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return false ;
			}
		
		}
	}
	
	
	private static boolean saveGameThumbnail(
			Context context, File directory, Bitmap thumbnail) {
		
		try {
			File validFile = fileFromDirectoryFile( directory, FILENAME_VALID ) ;
			if ( !directory.exists() || !validFile.exists() ) {
				return false ;
			}
			
			// Thumbnail file.  Require an up-to-date version; delete the old if there is one.
			File thumbnailFile = fileFromDirectoryFile( directory, FILENAME_GAME_THUMBNAIL ) ;
			if ( thumbnail != null ) {
				// temp file...
				File thumbnailTempFile = fileFromDirectoryFile( directory, FILENAME_GAME_THUMBNAIL_TEMP ) ;
				// just in case the thumbnail is being updated...
				synchronized( thumbnail ) {
					if ( GameSaver.writeBitmapToFile( context, thumbnailTempFile, thumbnail ) ) {
						thumbnailTempFile.renameTo(thumbnailFile) ;
					}
				}
			}
			
			return true ;
			
		} catch (Exception e) {
			e.printStackTrace() ;
			Log.d(TAG, e.toString() ) ;
			return false ;
		}
		
	}
	
	
	/**
	 * Side effect: destroys any ephemeral checkpoints.
	 * @param context
	 * @param key
	 * @param game
	 * @return
	 */
	public static boolean loadGameStates( 
			Context context, String key, Game game ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			// Reads current states from files:
			// SavedGames/<key>/game_state.bin
			//                 /gevents_state.bin
			//				   /ginfo_state.bin
			//                 /systems_states.bin
			
			try {
				File keyDir = directoryFileFromKey(context, key) ;
				deleteEphemeralCheckpoints( context, keyDir ) ;
				
				return loadGameStates( context, keyDir, game ) ;
				
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return false ;
			}
		}
	}
	
	public static boolean loadCheckpointStates( 
			Context context, String key, int checkpointNum, Game game ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			// Reads current states from files:
			// SavedGames/<key>/game_state.bin
			//                 /gevents_state.bin
			//				   /ginfo_state.bin
			//                 /systems_states.bin
			
			try {
				File keyDir = directoryFileFromKey(context, key) ;
				File checkpointDir = fileFromDirectoryFile(keyDir, CHECKPOINT_DIRECTORY_PREFIX + checkpointNum ) ;
				File ephemeralDir = fileFromDirectoryFile(keyDir, EPHEMERAL_CHECKPOINT_DIRECTORY_PREFIX + checkpointNum ) ;
				
				if ( ephemeralDir.exists() )
					return loadGameStates( context, ephemeralDir, game ) ;
				else
					return loadGameStates( context, checkpointDir, game ) ;
				
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return false ;
			}
		}
	}
	
	private static boolean loadGameStates(
			Context context, File directory, Game game ) {
		
		try {
			File validFile = fileFromDirectoryFile( directory, FILENAME_VALID ) ;
			
			if ( !validFile.exists() ) {
				Log.d(TAG, "Game Save ValidFile does not exist") ;
				return false ;
			}
			
			int version = readValidFile( context, validFile ) ;
			
			GameInformation ginfo = game.ginfo ;
			GameEvents gevents = game.gevents ;
			
			File file ;
			
			file = fileFromDirectoryFile( directory, FILENAME_GAME ) ;
			readSerializableStateFromFile( context, file, game, version ) ;
			file = fileFromDirectoryFile( directory, FILENAME_GAME_INFORMATION ) ;
			readSerializableStateFromFile( context, file, ginfo, version ) ;
			file = fileFromDirectoryFile( directory, FILENAME_GAME_EVENTS ) ;
			readSerializableStateFromFile( context, file, gevents, version ) ;
			
			file = fileFromDirectoryFile( directory, FILENAME_GAME_SYSTEMS ) ;
			Serializable [] ar = createSerializableArrayFromFile( context, file, version ) ;
			game.setSystemsFromSerializables(ar) ;
			
			// FAILING TO REFRESH WILL CAUSE A CRASH, DUDE
			game.refresh() ;
			
			return true ;
			
		} catch (Exception e) {
			e.printStackTrace() ;
			Log.d(TAG, e.toString() ) ;
			return false ;
		}
	}
	
	public static GameResult loadGameResult( Context context, String key ) {
		// Reads game result from file:
		// SavedGames/<key>/game_result.bin
		
		synchronized ( GAME_SAVER_MUTEX ) {
			try {
				File keyDir = directoryFileFromKey( context, key ) ;
				return loadGameResult( context, keyDir ) ;
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return null ;
			}
		}
	}
	
	public static GameSettings loadGameSettings( Context context, String key ) {
		// Reads gameSettings from file:
		// SavedGames/<key>/game_settings.bin
		
		synchronized ( GAME_SAVER_MUTEX ) {
			try {
				File keyDir = directoryFileFromKey( context, key ) ;
				return loadGameSettings( context, keyDir ) ;
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return null ;
			}
		}
	}

	public static Bitmap loadGameThumbnail( Context context, String key ) {
		// Reads thumbnail from file:
		// SavedGames/<key>/thumbnail.jpg
		
		synchronized ( GAME_SAVER_MUTEX ) {
			try {
				File keyDir = directoryFileFromKey( context, key ) ;
				return loadGameThumbnail( context, keyDir ) ;
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return null ;
			}
		}
	}
	
	public static Bitmap loadGameThumbnail( Context context, String key, int maxWidth, int maxHeight, boolean fitFully ) {
		// Reads thumbnail from file:
		// SavedGames/<key>/thumbnail.jpg
		
		synchronized ( GAME_SAVER_MUTEX ) {
			try {
				File keyDir = directoryFileFromKey( context, key ) ;
				return loadGameThumbnail( context, keyDir, maxWidth, maxHeight, true ) ;
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return null ;
			}
		}
	}
	
	
	
	public static GameResult loadCheckpointResult( Context context, String key, int checkpointNum ) {
		// Reads game result from file:
		// SavedGames/<key>/Checkpoint_<checkpointNum>/game_result.bin
		
		synchronized ( GAME_SAVER_MUTEX ) {
			try {
				File keyDir = directoryFileFromKey( context, key ) ;
				File directory = fileFromDirectoryFile( keyDir, CHECKPOINT_DIRECTORY_PREFIX + checkpointNum ) ;
				File ephemeralDir = fileFromDirectoryFile( keyDir, EPHEMERAL_CHECKPOINT_DIRECTORY_PREFIX + checkpointNum ) ;
				
				if ( ephemeralDir.exists() )
					return loadGameResult( context, ephemeralDir ) ;
				else
					return loadGameResult( context, directory ) ;
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return null ;
			}
		}
	}
	
	
	
	
	public static GameResult loadGameResult( Context context, File directory ) {
		synchronized ( GAME_SAVER_MUTEX ) {
			try {
				File validFile = fileFromDirectoryFile( directory, FILENAME_GAME_RESULT_VALID ) ;
				
				if ( !validFile.exists() ) {
					Log.d(TAG, "Game Save ValidFile does not exist") ;
					return null ;
				}
				
				int version = readValidFile( context, validFile ) ;
				
				File file = fileFromDirectoryFile( directory, FILENAME_GAME_RESULT ) ;
				GameResult gr = (GameResult)readSerializableObjectFromFile( context, file, version ) ;
	
				return gr ;
				
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return null ;
			}
		}
	}
	
	
	public static GameSettings loadGameSettings( Context context, File directory ) {
		synchronized ( GAME_SAVER_MUTEX ) {
			try {
				File validFile = fileFromDirectoryFile( directory, FILENAME_GAME_SETTINGS_VALID ) ;
				
				if ( !validFile.exists() ) {
					Log.d(TAG, "Game Save ValidFile does not exist") ;
					return null ;
				}
				
				int version = readValidFile( context, validFile ) ;
				
				File file = fileFromDirectoryFile( directory, FILENAME_GAME_SETTINGS ) ;
				Object obj = readSerializableObjectFromFile( context, file, version ) ;
				GameSettings gs = null ;
				if ( obj instanceof com.peaceray.quantro.model.GameSettings )
					gs = ((com.peaceray.quantro.model.GameSettings)obj).convertFromLegacy() ;
				else
					gs = (GameSettings)obj ;
	
				return gs ;
				
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return null ;
			}
		}
	}
	
	
	public static Bitmap loadGameThumbnail( Context context, File directory ) {
		synchronized ( GAME_SAVER_MUTEX ) {
			try {
				File file = fileFromDirectoryFile( directory, FILENAME_GAME_THUMBNAIL ) ;
				Bitmap b = GameSaver.readBitmapFromFile(context, file) ;
				
				return b ;
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return null ;
			}
		}
	}
	
	public static Bitmap loadGameThumbnail( Context context, File directory, int maxWidth, int maxHeight, boolean fitFully ) {
		synchronized ( GAME_SAVER_MUTEX ) {
			try {
				File file = fileFromDirectoryFile( directory, FILENAME_GAME_THUMBNAIL ) ;
				Bitmap b = GameSaver.readBitmapFromFile(context, file, maxWidth, maxHeight, fitFully) ;
				
				return b ;
			} catch (Exception e) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString() ) ;
				return null ;
			}
		}
	}
	
	
	
	private static void materializeEphemeralCheckpoints( Context context, File directory ) throws IOException {
		// For any ephemeral checkpoints in this directory, convert them to normal
		// checkpoints (overwrite the checkpoint previously present, if any).
		if ( directory.isDirectory() ) {
			for ( File c : directory.listFiles() ) {
				// If this is ephemeral, delete the corresponding normal
				// checkpoint, and copy (rename).
				if ( c.getName().startsWith(EPHEMERAL_CHECKPOINT_DIRECTORY_PREFIX) ) {
					int num = Integer.parseInt(c.getName().substring(EPHEMERAL_CHECKPOINT_DIRECTORY_PREFIX.length())) ;
					File chk = fileFromDirectoryFile( directory, CHECKPOINT_DIRECTORY_PREFIX + num ) ;
					if ( chk.exists() )
						delete(chk) ;
					
					// now move
					c.renameTo( chk ) ;
				}
			}
		}
	}
	
	private static void deleteEphemeralCheckpoints( Context context, File directory ) throws IOException {
		// For any ephemeral checkpoints in this directory, delete them.
		if ( directory.isDirectory() ) {
			for ( File c : directory.listFiles() ) {
				// If this is ephemeral, delete the corresponding normal
				// checkpoint, and copy (rename).
				if ( c.getName().startsWith(EPHEMERAL_CHECKPOINT_DIRECTORY_PREFIX) ) {
					delete(c) ;
				}
			}
		}
	}
	
	
	public static boolean hasGameStates(
			Context context, String key ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			if ( key == null )
				return false ;
			
			// Check for the existence of a "valid" file.
			File file = fileFromKeyAndFilename( context, key, FILENAME_VALID ) ;
			return file.exists() ;
		}
	}
	
	public static boolean hasGameResult(
			Context context, String key ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			// Check for the existence of a "valid" file.
			File file = fileFromKeyAndFilename( context, key, FILENAME_GAME_RESULT_VALID ) ;
			return file.exists() ;
		}
	}
	
	public static boolean hasGameSettings( Context context, String key ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			// Check for the existence of a "valid" file.
			File file = fileFromKeyAndFilename( context, key, FILENAME_GAME_SETTINGS_VALID ) ;
			return file.exists() ;
		}
	}
	
	
	public static boolean hasCheckpointStates(
			Context context, String key, int checkpointNum ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			if ( key == null )
				return false ;
			
			// Check for the existence of a "valid" file.
			File file ;
			file = directoryFileFromKey( context, key ) ;
			File chk = fileFromDirectoryFile( file, CHECKPOINT_DIRECTORY_PREFIX + checkpointNum ) ;
			chk = fileFromDirectoryFile( chk, FILENAME_VALID ) ;
			File eph = fileFromDirectoryFile( file, EPHEMERAL_CHECKPOINT_DIRECTORY_PREFIX + checkpointNum ) ;
			eph = fileFromDirectoryFile( eph, FILENAME_VALID ) ;
			return chk.exists() || eph.exists() ;
		}
	}
	
	public static boolean hasCheckpointResult(
			Context context, String key, int checkpointNum ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			// Check for the existence of a "valid" file.
			File file ;
			file = directoryFileFromKey( context, key ) ;
			File chk = fileFromDirectoryFile( file, CHECKPOINT_DIRECTORY_PREFIX + checkpointNum ) ;
			chk = fileFromDirectoryFile( chk, FILENAME_GAME_RESULT_VALID ) ;
			File eph = fileFromDirectoryFile( file, EPHEMERAL_CHECKPOINT_DIRECTORY_PREFIX + checkpointNum ) ;
			eph = fileFromDirectoryFile( eph, FILENAME_GAME_RESULT_VALID ) ;
			return chk.exists() || eph.exists() ;
		}
	}
	
	
	
	
	/**
	 * Deletes the specified game states.  Returns 'true' if the
	 * state was successfully deleted ( or did not exist ); 'false'
	 * if there was a problem with deletion.
	 * @param context
	 * @param key
	 * @return
	 */
	public static boolean deleteGame(
			Context context, String key ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			// Get the file to the directory.
			File keyDir = directoryFileFromKey( context, key ) ;
			
			if ( !keyDir.exists() ) 
				return true ;
			
			try {
				delete(keyDir) ;
				return true ;
			} catch( IOException e ) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString()) ;
				return false ;
			}
		}
	}
	
	private static void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				delete(c);
		}
		if (!f.delete())
			throw new FileNotFoundException("Failed to delete file: " + f);
	}

	
	/**
	 * Deletes ONLY THE valid file for the main game.  This retains checkpoints
	 * and ephemeral checkpoints.
	 * 
	 * @param context
	 * @param key
	 * @return
	 */
	public static boolean deleteGameButKeepCheckpoints(
			Context context, String key ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			// Get the file to the directory.
			File keyDir = directoryFileFromKey( context, key ) ;
			
			if ( !keyDir.exists() ) 
				return true ;
			
			File validFile = fileFromDirectoryFile( keyDir, FILENAME_VALID ) ;
			
			if ( !validFile.exists() )
				return true ;
			
			try {
				delete(validFile) ;
				return true ;
			} catch( IOException e ) {
				e.printStackTrace() ;
				Log.d(TAG, e.toString()) ;
				return false ;
			}
		}
	}
	
	
	/**
	 * Examines the saved game directory, looking for files that look invalid.
	 * If any are found, performs a deletion.  Returns the number of game
	 * states that have been removed in this way.
	 * @param context
	 * @return
	 */
	public static int cleanGames( Context context ) {
		
		synchronized ( GAME_SAVER_MUTEX ) {
			
			//String [] keys = new File
			// stub
			return 0;
		}
	}
	
	
	
	private static File directoryFileFromKey( Context context, String key ) {
		File gamesDir = context.getDir(SAVED_GAMES_DIRECTORY, 0) ;
		File keyDir = new File( gamesDir, key ) ;
		return keyDir ;
	}
	
	
	private static File fileFromKeyAndFilename( Context context, String key, String filename ) {
		File keyDir = directoryFileFromKey( context, key ) ;
		File result = new File( keyDir, filename ) ;
		return result ;
	}
	
	private static File fileFromDirectoryFile( File dirFile, String filename ) {
		File result = new File( dirFile, filename ) ;
		return result ;
	}
	
	
	private static void writeSerializableObjectToFile (
			Context context, File file, Serializable obj ) throws Exception {
		
		FileOutputStream fos = new FileOutputStream(file) ;
		ObjectOutputStream oos = new ObjectOutputStream(fos) ;
		
		// Write the object to the stream... 
		oos.writeObject( obj ) ;
		
		// Shut.  Down.  Everything.
		oos.close() ;	// Also closes fos
	}
	
	
	private static void writeSerializableStateToFile(
			Context context, File file, SerializableState objectWithState ) throws Exception {
		
		FileOutputStream fos = new FileOutputStream(file) ;
		ObjectOutputStream oos = new ObjectOutputStream(fos) ;
		
		// Write the state to the stream... 
		oos.writeObject( objectWithState.getStateAsSerializable() ) ;
		
		// Shut.  Down.  Everything.
		oos.close() ;	// Also closes fos


	}
	
	private static boolean writeSerializableArrayToFile(
			Context context, File file, Serializable [] objects ) {
		
		try {
			FileOutputStream fos = new FileOutputStream(file) ;
			ObjectOutputStream oos = new ObjectOutputStream(fos) ;
			
			// How big is the array?
			oos.writeInt( objects.length ) ;
			
			// Write the objects...
			for ( int i = 0; i < objects.length; i++ )
				oos.writeObject( objects[i] ) ;
			
			// Shut.  Down.  Everything.
			oos.close() ;	// Also closes fos
			return true ;
		} catch (FileNotFoundException e) {
			// Log this error, return false.
			e.printStackTrace() ;
			Log.d(TAG, e.toString() ) ;
			return false ;
		} catch (IOException e) {
			e.printStackTrace() ;
			Log.d(TAG, e.toString() ) ;
			return false ;
		} catch (RuntimeException e) {
			e.printStackTrace() ;
			Log.d(TAG, e.toString() ) ;
			return false ;
		}
	}
	
	
	private static void readSerializableStateFromFile(
			Context context, File file, SerializableState objectNeedingState, int savedVersion ) throws Exception {
		
		FileInputStream fis = new FileInputStream(file) ;
		ObjectInputStream ois = new ObjectInputStream(fis) ;
		
		// Write the state to the stream...
		Object state = ois.readObject() ;
		
		// Shut.  Down.  Everything.
		ois.close() ;	// Also closes fis
		
		// upconvert the state if predates versioning.
		if ( savedVersion == VERSION_PREVERSIONED ) {
			try {
				Method m = state.getClass().getMethod("qUpconvert") ;
				state = m.invoke(state) ;
			} catch( NoSuchMethodException e ) {
				//e.printStackTrace() ;
				Log.d(TAG, "no method qUpconvert for " + state) ;
				// no problem; can't be upconverted.
			}
		}
		objectNeedingState.setStateAsSerializable( (Serializable)state ) ;
	}
	
	private static Object readSerializableObjectFromFile (
			Context context, File file, int savedVersion ) throws Exception {
		
		FileInputStream fis = new FileInputStream(file) ;
		ObjectInputStream ois = new ObjectInputStream(fis) ;
		
		// Read the object from the stream... 
		Object obj = ois.readObject() ;
		
		// Shut.  Down.  Everything.
		ois.close() ;	// Also closes fos
		
		// upconvert the state if predates versioning.
		if ( savedVersion == VERSION_PREVERSIONED ) {
			try {
				Method m = obj.getClass().getMethod("qUpconvert") ;
				obj = m.invoke(obj) ;
			} catch( NoSuchMethodException e ) {
				//e.printStackTrace() ;
				Log.d(TAG, "no method qUpconvert for " + obj) ;
				// no problem; can't be upconverted.
			}
		}
		
		return obj ;
	}

	private static Serializable [] createSerializableArrayFromFile(
			Context context, File file, int savedVersion ) throws Exception {
		
		FileInputStream fis = new FileInputStream(file) ;
		ObjectInputStream ois = new ObjectInputStream(fis) ;
		
		// Write the state to the stream...
		int num = ois.readInt() ;
		
		Serializable [] objects = new Serializable[num] ;
		
		for ( int i = 0; i < num; i++ )
			objects[i] = (Serializable)ois.readObject() ;
		
		// Shut.  Down.  Everything.
		ois.close() ;	// Also closes fis
		
		for ( int i = 0; i < num; i++ ) {
			if ( savedVersion == VERSION_PREVERSIONED ) {
				try {
					Method m = objects[i].getClass().getMethod("qUpconvert") ;
					objects[i] = (Serializable) m.invoke( objects[i] ) ;
				} catch( NoSuchMethodException e ) {
					//e.printStackTrace() ;
					Log.d(TAG, "no method qUpconvert for " + objects[i]) ;
					// no problem; can't be upconverted.
				}
			}
		}
		
		return objects ;
	}

	
	private static void writeValidFile (
			Context context, File file ) throws Exception {
		
		FileOutputStream fos = new FileOutputStream(file) ;
		ObjectOutputStream oos = new ObjectOutputStream(fos) ;
		
		// Write version number to the stream... 
		oos.writeInt( VERSION ) ;
		
		// Shut.  Down.  Everything.
		oos.close() ;	// Also closes fos
	}

	private static int readValidFile (
			Context context, File file ) throws Exception {
		
		FileInputStream fis = new FileInputStream(file) ;
		ObjectInputStream ois = null ;
		
		int version ;
		try {
			ois = new ObjectInputStream(fis) ;
			version = ois.readInt() ;
		} catch( EOFException eof ) {
			version = VERSION_PREVERSIONED ;
		} finally {
			if ( ois != null )
				ois.close() ;
		}
		
		return version ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// LOAD / SAVE BITMAPS

	private static boolean writeBitmapToFile( Context context, File file, Bitmap bitmap ) {
		
		// get the right format.
		Bitmap.CompressFormat compressFormat ;
		String fname = file.getName() ;
		String [] components = fname.split("\\.") ;
		if ( components.length > 1 && components[components.length-1].toLowerCase().equals("png") ) {
			Log.d(TAG, "writing as PNG") ;
			compressFormat = Bitmap.CompressFormat.PNG ;
		} else {
			Log.d(TAG, "writing as JPG") ;
			compressFormat = Bitmap.CompressFormat.JPEG ;
		}
		
		// write
		try {
			FileOutputStream out = new FileOutputStream(file) ;
			bitmap.compress(compressFormat, 90, out) ;
			out.flush() ;
			out.close() ;
			return true ;
			
		} catch( Exception e ) {
			e.printStackTrace() ;
			Log.d(TAG, e.toString()) ;
			return false ;
		}
	}
	
	
	private static Bitmap readBitmapFromFile( Context context, File file ) {
		try {
			FileInputStream fin = new FileInputStream(file) ;
			Bitmap b = BitmapFactory.decodeStream(fin) ;
			fin.close() ;
			
			return b ;
		} catch( Exception e ) {
			e.printStackTrace() ;
			Log.d(TAG, e.toString()) ;
			return null ;
		}
	}
	
	/**
	 * Reads the specified bitmap from file.  Max width/height must both be > 0.
	 * If 'fitFully', the image will be downsampled to fit fully within these bounds;
	 * if '!fitFully', the image will be downsampled so at least one dimension fits
	 * the bounds.
	 * @param context
	 * @param file
	 * @param maxWidth
	 * @param maxHeight
	 * @param fitFully
	 * @return
	 */
	private static Bitmap readBitmapFromFile( Context context, File file, int maxWidth, int maxHeight, boolean fitFully ) {
		try {
			// read twice.  First to get bounds, second to downsample to within the
			// specified bounds.
			BitmapFactory.Options options = new BitmapFactory.Options() ;
			options.inJustDecodeBounds = true ;
			
			FileInputStream fin = new FileInputStream(file) ;
			BitmapFactory.decodeStream(fin, null, options) ;
			fin.close() ;
			
			if ( options.outWidth < 0 || options.outHeight < 0 )
				return null ;
			
			int downsamp ;
			if ( fitFully ) {
				downsamp = (int)Math.max(
						Math.ceil( (double)options.outWidth / maxWidth ),
						Math.ceil( (double)options.outHeight / maxHeight ) ) ;
			} else {
				downsamp = (int)Math.min(
						Math.ceil( (double)options.outWidth / maxWidth ),
						Math.ceil( (double)options.outHeight / maxHeight ) ) ;
			}
			
			options = new BitmapFactory.Options() ;
			options.inSampleSize = downsamp ;
			fin = new FileInputStream(file) ;
			Bitmap b = BitmapFactory.decodeStream(fin, null, options) ;
			fin.close() ;
			
			return b ;
		} catch( Exception e ) {
			// e.printStackTrace() ;
			Log.d(TAG, e.toString()) ;
			return null ;
		}
	}
	
}
