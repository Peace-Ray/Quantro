package com.peaceray.quantro.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.game.GameResult;

/**
 * The GameStatsDatabase holds a record of games played.
 * For version 1.0, this information is not used anywhere in
 * the program, but will be useful later to view game stats
 * and history.
 * 
 * Unlike 'Challenges,' which exist as individual Objects
 * that are also held in a database, these records have no
 * "non-database" equivalent (they are NOT GameResult objects,
 * for example; GameResults hold far more information).
 * 
 * For that reason, this class actually contains several
 * static classes, used for different purposes.  The "Record"
 * class holds a small amount of information regarding a single
 * game (and can be constructed from a GameResult).  The
 * "Summary" class holds aggregated information about a particular
 * game mode, such as number of games played, number of games won,
 * total time spent playing, etc.  Finally, the "DatabaseAdapter"
 * class allows Records and Summaries to be read from, and written
 * to, an underlying mySQLite database.
 * 
 * This database does NOT store a complete set of all Records
 * generated over the lifetime of the app.  Instead, it is 
 * interested only in the "best N" for a particular column and
 * game mode: the top N score, e.g., or most recent N games.
 * 
 * @author Jake
 *
 */
public class GameStats {
	
	/**
	 * A GameStats.Record object describes, in limited terms,
	 * a single game played by this app.  It is far more limited
	 * in scope than a GameResult object, which is not intended for
	 * long-term historical mass recording due to its size.  Records
	 * are far more limited, containing only information of interest
	 * for "top N" lists.
	 * 
	 * Records are constructed using GameResult instances.  They can
	 * also be created by the DatabaseAdapter, but there is 
	 * no need for an independent constructor or .Builder worker class.
	 * 
	 * A Record is immutable once constructed.  Typically a single Record
	 * will be created at the time a game ends, and lists (arrays) of
	 * Records can be retrieved from the database.
	 * 
	 * @author Jake
	 *
	 */
	public static class Record {
		
		// GAME OUTCOMES
		public static final int OUTCOME_OTHER = 0 ;		// NEVER CHANGE THIS
		public static final int OUTCOME_WON = 1 ;
		public static final int OUTCOME_LOST = 2 ; 
		public static final int NUM_OUTCOMES = 3 ;
		
		// COMPLEX ACCESSORS
		// These are those for which we expect "sorted lists."  Some of these
		// are specific instance fields; others are complex values resulting
		// from calculations.  They are accessed via integer values, primarily
		// for ease of iteration.
		public static final int VALUE_GAME_LENGTH = 0 ;
		public static final int VALUE_SCORE = 1 ;
		public static final int VALUE_MAX_MULTIPLIER = 2 ;
		public static final int VALUE_TOTAL_CLEARS = 3 ;
		public static final int VALUE_LEVEL = 4 ;
		public static final int VALUE_LEVEL_UPS = 5 ;
		public static final int VALUE_BEST_CASCADE = 6 ;
		public static final int VALUE_TIME_ENDED = 7 ;
		public static final int NUM_VALUES = 8 ;
		
		
		
		// INSTANCE VARS
		
		// The mode of the game this Record represents
		private int mGameMode ;
		
		private Nonce mGameNonce ;
		
		// Time: start and finish (system time) and millis spent
		// playing (ACTUALLY playing; this is not just end - start).
		private long mTimeBegun ;
		private long mTimeEnded ;
		private long mTimeSpentPlaying ;
		private boolean mEnded ;
		
		// Score and clears: how well did you do in this game?
		private long mScore ;
		private float mHighestMultiplier ;
		private int mClearsS0 ;
		private int mClearsS1 ;
		private int mClearsSL ;
		private int mClearsMO ;
		private int mBestCascade ;	// most clears in a cascade.
		private int mStartingLevel ;
		private int mLevel ;
		
		// Game outcome
		private int mOutcome ;	// one of OUTCOME_*
		
		// Multiplayer only: these fields are ignored for single player games.
		private boolean mHasOpponent = false ;
		private String mOpponentName = null ;
		private long mOpponentScore ;
		private float mOpponentHighestMultiplier ;
		private int mOpponentClearsS0 ;
		private int mOpponentClearsS1 ;
		private int mOpponentClearsSL ;
		private int mOpponentClearsMO ;
		private int mOpponentBestCascade ;	// mOpponentost clears in a cascade.
		private int mOpponentStartingLevel ;
		private int mOpponentLevel ;
		
		Record() { }
		
		Record( GameResult gr ) {
			// Set nonce
			mGameNonce = gr.getNonce() ;
			// Set game length and times.
			mTimeBegun = gr.getDateStarted() == null ? 0 : gr.getDateStarted().getTime() ;
			mTimeEnded = gr.getDateEnded() == null ? 0 : gr.getDateEnded().getTime() ;
			mTimeSpentPlaying = gr.getTimeInGame() ;
			mEnded = gr.getTerminated() ;
			
			// get this player's GameInformation object
			int playerSlot = gr.getLocalPlayerSlot() ;
			GameInformation gi = gr.getGameInformation(playerSlot) ;
			
			// Set mode, score and clear info, etc.
			if ( gi != null ) {
				mGameMode = gi.mode ;
				mScore = gi.score ;
				mHighestMultiplier = gi.highestMultiplier ;
				mClearsS0 = gi.s0clears ;
				mClearsS1 = gi.s1clears ;
				mClearsSL = gi.sLclears ;
				mClearsMO = gi.moclears ;
				mBestCascade = gi.longestCascade ;
				mStartingLevel = gi.firstLevel ;
				mLevel = gi.level ;
			}
			
			// Game outcome?
			if ( gr.getWon(playerSlot) )
				mOutcome = OUTCOME_WON ;
			else if ( gr.getLost(playerSlot) )
				mOutcome = OUTCOME_LOST ;
			else
				mOutcome = OUTCOME_OTHER ;
			
			// Opponent info?
			if ( gr.getNumberOfPlayers() == 1 ) {
				mHasOpponent = false ;
			} else {
				mHasOpponent = true ;
				// TODO: Once we have more than 2 players, consider whether
				// this is still the right thing to do.
				int opponentSlot = (playerSlot + 1) % gr.getNumberOfPlayers() ;
				gi = gr.getGameInformation(opponentSlot) ;
				if ( gi != null ) {
					mOpponentScore = gi.score ;
					mOpponentHighestMultiplier = gi.highestMultiplier ;
					mOpponentClearsS0 = gi.s0clears ;
					mOpponentClearsS1 = gi.s1clears ;
					mOpponentClearsSL = gi.sLclears ;
					mOpponentClearsMO = gi.moclears ;
					mOpponentBestCascade = gi.longestCascade ;
					mOpponentStartingLevel = gi.firstLevel ;
					mOpponentLevel = gi.level ;
					
					// Opponent name!
					mOpponentName = gr.getName(opponentSlot) ;
				}
			}
		}
		
		
		// ACCESSORS
		public int getGameMode() { return mGameMode ; }
		
		public Nonce getGameNonce() { return mGameNonce ; } 
		
		public long getTimeBegun() { return mTimeBegun ; }
		public long getTimeEnded() { return mTimeEnded ; }
		public long getTimeSpentPlaying() { return mTimeSpentPlaying ; }
		public boolean getEnded() { return mEnded ; }
		
		public long getScore() { return mScore ; }
		public float getHighestMultiplier() { return mHighestMultiplier ; }
		public int getClearsS0() { return mClearsS0 ; }
		public int getClearsS1() { return mClearsS1 ; }
		public int getClearsSL() { return mClearsSL ; }
		public int getClearsMO() { return mClearsMO ; }
		public int getBestCascade() { return mBestCascade ; }
		public int getStartingLevel() { return mStartingLevel ; }
		public int getLevel() { return mLevel ; }
		
		public int getOutcome() { return mOutcome ; } 
		
		public boolean getHasOpponent() { return mHasOpponent ; }
		public String getOpponentName() { return mOpponentName ; } 
		public long getOpponentScore() { return mOpponentScore ; }
		public float getOpponentHighestMultiplier() { return mOpponentHighestMultiplier ; }
		public int getOpponentClearsS0() { return mOpponentClearsS0 ; }
		public int getOpponentClearsS1() { return mOpponentClearsS1 ; }
		public int getOpponentClearsSL() { return mOpponentClearsSL ; }
		public int getOpponentClearsMO() { return mOpponentClearsMO ; }
		public int getOpponentBestCascade() { return mOpponentBestCascade ; }
		public int getOpponentStartingLevel() { return mOpponentStartingLevel ; }
		public int getOpponentLevel() { return mOpponentLevel ; }
		
		
		
		public boolean isInt( int valueCode ) {
			switch( valueCode ) {
			case VALUE_TOTAL_CLEARS:
			case VALUE_LEVEL:
			case VALUE_LEVEL_UPS:
			case VALUE_BEST_CASCADE:
				return true ;
			}
			
			return false ;
		}
		
		public boolean isFloat( int valueCode ) {
			switch( valueCode ) {
			case VALUE_MAX_MULTIPLIER:
				return true ;
			}
			
			return false ;
		}
		
		public boolean isLong( int valueCode ) {
			switch( valueCode ) {
			case VALUE_GAME_LENGTH:
			case VALUE_SCORE:
			case VALUE_TIME_ENDED:
				return true ;
			}
			
			return false ;
		}
		
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder() ;
			sb.append("GameStats.Record") ;
			sb.append(" ").append("[").append(" ") ;
			
			sb.append("mode:" + mGameMode).append(" ") ;
			sb.append("start:(" + new Date(mTimeBegun).toString()).append(") ") ;
			sb.append("end:(" + new Date(mTimeEnded).toString()).append(") ") ;
			sb.append("length:(" + (mTimeSpentPlaying / (1000*60*60)) + ":" + ((mTimeSpentPlaying / (1000*60))%60) + ":" + ((mTimeSpentPlaying / (1000))%60)).append(") ") ;
			sb.append("ended:" + mEnded).append(" ") ;
			
			switch( mOutcome ) {
			case OUTCOME_WON:
				sb.append("outcome:won").append(" ") ;
				break ;
			case OUTCOME_LOST:
				sb.append("outcome:lost").append(" ") ;
				break ;
			case OUTCOME_OTHER:
				sb.append("outcome:other").append(" ") ;
				break ;
			}
			
			sb.append("score:").append(mScore).append(" ") ;
			sb.append("topMult:").append(mHighestMultiplier).append(" ") ;
			sb.append("clearsS0:").append(mClearsS0).append(" ") ;
			sb.append("clearsS1:").append(mClearsS1).append(" ") ;
			sb.append("clearsSL:").append(mClearsSL).append(" ") ;
			sb.append("clearsMO:").append(mClearsMO).append(" ") ;
			sb.append("bestCascade:").append(mBestCascade).append(" ") ;
			sb.append("startingLevel:").append(mStartingLevel).append(" ") ;
			sb.append("level:").append(mLevel).append(" ") ;
			
			if ( mOpponentName != null ) {
				sb.append("OPPONENT:").append(mOpponentName).append(" ") ;
				sb.append("score:").append(mOpponentScore).append(" ") ;
				sb.append("topMult:").append(mOpponentHighestMultiplier).append(" ") ;
				sb.append("clearsS0:").append(mOpponentClearsS0).append(" ") ;
				sb.append("clearsS1:").append(mOpponentClearsS1).append(" ") ;
				sb.append("clearsSL:").append(mOpponentClearsSL).append(" ") ;
				sb.append("clearsMO:").append(mOpponentClearsMO).append(" ") ;
				sb.append("bestCascade:").append(mOpponentBestCascade).append(" ") ;
				sb.append("startingLevel:").append(mOpponentStartingLevel).append(" ") ;
				sb.append("level:").append(mOpponentLevel).append(" ") ;
			}
			sb.append(" ]") ;
			
			return sb.toString() ;
		}
	}
	
	
	
	/**
	 * GameStats.Summary provides some general summary statistics for a
	 * particular game mode.
	 * 
	 * Instances of this class are immutable from outside.  
	 * 
	 * @author Jake
	 *
	 */
	public static class Summary {
		
		private int mGameMode ;
		
		private long mNumStarts ;		// # times this game mode is started
		private long mNumEnds ;			// # times this game mode has been completed
		
		private long mNumWins ;
		private long mNumLosses ;
		
		private long mTimeSpentPlaying ;
		
		
		Summary() { }
		
		public int getGameMode() { return mGameMode ; }
		
		public long getNumStarts() { return mNumStarts ; }
		public long getNumEnds() { return mNumEnds ; }
		
		public long getNumWins() { return mNumWins ; }
		public long getNumLosses() { return mNumLosses ; }
		
		public long getTimeSpentPlaying() { return mTimeSpentPlaying ; }
		
		
		public String toString() {
			StringBuilder sb = new StringBuilder() ;
			sb.append("GameStats.Summary").append(" ").append("[") ;
			sb.append(" ").append("gameMode:" + mGameMode) ;
			sb.append(" ").append("numStarts:" + mNumStarts) ;
			sb.append(" ").append("numEnds:" + mNumEnds) ;
			sb.append(" ").append("numWins:" + mNumWins) ;
			sb.append(" ").append("numLosses:" + mNumLosses) ;
			sb.append(" ").append("timeSpentPlaying:" + mTimeSpentPlaying) ;
			sb.append(" ").append("]").append(" ") ;
			return sb.toString() ;
		}
	}

	
	public static class DatabaseAdapter {
		
		private static final String TAG = "GS.DatabaseAdapter" ;
		
		private static final Object DATABASE_MUTEX = new Object() ;
		
		private static final int NUMBER_OF_RECORDS_TO_KEEP = 10 ;
		
		
		private static final String DATABASE_NAME = "quantro_game_states";
	    private static final String DATABASE_TABLE_RECORDS = "records" ;
	    private static final String DATABASE_TABLE_SUMMARIES = "summaries" ;
	    
	    // Both Records and Summaries use game_mode.
	    public static final String KEY_GAME_MODE = "game_mode" ;
	    
	    // Summaries use these
	    public static final String KEY_NUM_STARTS = "num_starts" ;
	    public static final String KEY_NUM_ENDS = "num_ends" ;
	    public static final String KEY_NUM_WINS = "num_wins" ;
	    public static final String KEY_NUM_LOSSES = "num_losses" ;
	    public static final String KEY_TIME_SPENT_PLAYING = "time_spent_playing" ;
	    
	    // Records use these
	    public static final String KEY_ROW_ID = "row_id" ;
	    //
	    public static final String KEY_NONCE = "nonce" ;
	    //
	    public static final String KEY_TIME_STARTED = "time_started" ;
	    public static final String KEY_TIME_ENDED = "time_ended" ;
	    public static final String KEY_TIME_LENGTH = "time_length" ;
	    public static final String KEY_ENDED = "ended" ;
	    //
	    public static final String KEY_SCORE = "score" ;
	    public static final String KEY_HIGHEST_MULTIPLIER = "highest_multiplier" ;
	    //
	    public static final String KEY_CLEARS_S0 = "clears_s0" ;
	    public static final String KEY_CLEARS_S1 = "clears_s1" ;
	    public static final String KEY_CLEARS_SL = "clears_sl" ;
	    public static final String KEY_CLEARS_MO = "clears_mo" ;
	    public static final String KEY_BEST_CASCADE = "best_cascade" ;
	    public static final String KEY_STARTING_LEVEL = "starting_level" ;
	    public static final String KEY_LEVEL = "level" ;
	    //
	    public static final String KEY_OUTCOME = "outcome" ;
	    //
	    public static final String KEY_HAS_OPPONENT = "has_opponent" ;
	    //
	    public static final String KEY_OPPONENT_SCORE = "opponent_score" ;
	    public static final String KEY_OPPONENT_HIGHEST_MULTIPLIER = "opponent_highest_multiplier" ;
	    //
	    public static final String KEY_OPPONENT_CLEARS_S0 = "opponent_clears_s0" ;
	    public static final String KEY_OPPONENT_CLEARS_S1 = "opponent_clears_s1" ;
	    public static final String KEY_OPPONENT_CLEARS_SL = "opponent_clears_sl" ;
	    public static final String KEY_OPPONENT_CLEARS_MO = "opponent_clears_mo" ;
	    public static final String KEY_OPPONENT_BEST_CASCADE = "opponent_best_cascade" ;
	    public static final String KEY_OPPONENT_STARTING_LEVEL = "opponent_starting_level" ;
	    public static final String KEY_OPPONENT_LEVEL = "opponent_level" ;
	    
	    
	    private static final String DATABASE_TABLE_SUMMARY_CREATE =
	        "create table " + DATABASE_TABLE_SUMMARIES + "("+KEY_GAME_MODE+" integer primary key, "
	        + KEY_NUM_STARTS + " bigint not null, "
	        + KEY_NUM_ENDS + " bigint not null, "
	        + KEY_NUM_WINS + " bigint not null, "
	        + KEY_NUM_LOSSES + " bigint not null, "
	        + KEY_TIME_SPENT_PLAYING + " bigint not null)" ;
	    
	    
	    private static final String DATABASE_TABLE_RECORD_CREATE =
	    	"create table " + DATABASE_TABLE_RECORDS + "("+ KEY_ROW_ID+" integer primary key autoincrement, "
	    	+ KEY_NONCE + " text not null, "
	    	+ KEY_GAME_MODE + " integer not null, "
	    	+ KEY_TIME_STARTED + " bigint not null, "
	    	+ KEY_TIME_ENDED + " bigint not null, "
	    	+ KEY_TIME_LENGTH + " bigint not null, "
	    	+ KEY_ENDED + " boolean not null, "
	    	+ KEY_SCORE + " bigint not null, "
	    	+ KEY_HIGHEST_MULTIPLIER + " float not null, "
	    	+ KEY_CLEARS_S0 + " integer not null, "
	    	+ KEY_CLEARS_S1 + " integer not null, "
	    	+ KEY_CLEARS_SL + " integer not null, "
	    	+ KEY_CLEARS_MO + " integer not null, "
	    	+ KEY_BEST_CASCADE + " integer not null, "
	    	+ KEY_STARTING_LEVEL + " integer not null, "
	    	+ KEY_LEVEL + " integer not null, "
	    	+ KEY_OUTCOME + " integer not null, "
	    	+ KEY_HAS_OPPONENT + " boolean not null, "
	    	+ KEY_OPPONENT_SCORE + " bigint, "
	    	+ KEY_OPPONENT_HIGHEST_MULTIPLIER + " float, "
	    	+ KEY_OPPONENT_CLEARS_S0 + " integer, "
	    	+ KEY_OPPONENT_CLEARS_S1 + " integer, "
	    	+ KEY_OPPONENT_CLEARS_SL + " integer, "
	    	+ KEY_OPPONENT_CLEARS_MO + " integer, "
	    	+ KEY_OPPONENT_BEST_CASCADE + " integer, "
	    	+ KEY_OPPONENT_STARTING_LEVEL + " integer, "
	    	+ KEY_OPPONENT_LEVEL + " integer)" ;
	    
	    	
	    
	    private static final int DATABASE_VERSION = 2;
	    
	    
	    private DatabaseHelper mDBHelper = null ;
	    private SQLiteDatabase mDB = null ;

	    
	    public DatabaseAdapter(Context context) {
	    	synchronized( DATABASE_MUTEX ) {
		        mDBHelper = new DatabaseHelper(context);
	    	}
	    }
	        
	    private static class DatabaseHelper extends SQLiteOpenHelper 
	    {
	        DatabaseHelper(Context context) {
	            super(context, DATABASE_NAME, null, DATABASE_VERSION);
	        }

	        @Override
	        public void onCreate(SQLiteDatabase db) 
	        {
	            db.execSQL(DATABASE_TABLE_SUMMARY_CREATE);
	            db.execSQL(DATABASE_TABLE_RECORD_CREATE);
	        }

	        @Override
	        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
	        int newVersion) 
	        {
	            Log.w(TAG, "Upgrading GameStats database from version " + oldVersion 
	                    + " to "
	                    + newVersion + ", which will destroy all old data");
	            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_SUMMARIES);
	            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_RECORDS);
	            
	            onCreate(db);
	        }
	    }   
	    
	    /**
	     * Opens the underlying database.
	     * 
	     * Call before any retrievals, insertions, or other operations.
	     * 
	     * @return
	     * @throws SQLException
	     */
	    public DatabaseAdapter open() throws SQLException 
	    {
	    	synchronized( DATABASE_MUTEX ) {
	    		if ( mDB != null )
		    		throw new IllegalStateException("Database is already open!") ;
	    		
		        mDB = mDBHelper.getWritableDatabase();
		        return this;
	    	}
	    }

	    /**
	     * Closes the connection to the underlying database.
	     * 
	     * Call when retrievals, etc. are complete.
	     * 
	     */
	    public void close() 
	    {
	    	synchronized( DATABASE_MUTEX ) {
	    		mDBHelper.close();
	    		if ( mDB != null ) {
	    			while ( mDB.isOpen() ) {
	    				Log.d(TAG, "sleeping until closed...") ;
	    				try {
							Thread.sleep(3) ;
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	    			}
	    		}
	    	}
	    }
	    
	    
	    
	    /**
	     * Notes that a game of the specified mode has started.  This
	     * method should be called at the time a game is launched.  It
	     * ensures that the specified game mode is included in the 
	     * Summary table as an entry (even if one that is zeroed-out),
	     * and then increments the number of games started.
	     * 
	     * Note that there is no guarantee that this game will ever
	     * end; i.e., that we will ever have a GameResult from this
	     * game.  For that reason, we expect the number of game started to
	     * not match the number of games ended.
	     * 
	     * @param gameMode
	     */
	    public void addNewGameStarted( int gameMode ) {
	    	synchronized( DATABASE_MUTEX ) {
	    		// Step 1: make sure we have a summary for this game mode.
	    		addSummaryRowIfNeeded( gameMode ) ;
	    		
	    		// Step 2: increment the number of games started
	    		incrementSummaryGamesStarted( gameMode ) ;
	    	}
	    }
	    
	    
	    
	    public static void addGameResultToDatabase( Context context, GameResult gr ) {
	    	DatabaseAdapter da = new DatabaseAdapter(context) ;
	    	da.open() ;
	    	try {
	    		da.addGameResult(gr) ;
	    	} finally {
	    		da.close() ;
	    	}
	    }
	    
	    public static void addNewGameStartedToDatabase( Context context, int gameMode ) {
	    	DatabaseAdapter da = new DatabaseAdapter(context) ;
	    	da.open() ;
	    	try {
	    		da.addNewGameStarted(gameMode) ;
	    	} finally {
	    		da.close() ;
	    	}
	    }
	    
	    public static boolean deleteStatsAndSummaryFromDatabase( Context context, int gameMode ) {
	    	DatabaseAdapter da = new DatabaseAdapter(context) ;
	    	da.open() ;
	    	try {
	    		return da.deleteStatsAndSummary(gameMode) ;
	    	} finally {
	    		da.close() ;
	    	}
	    }
	    
	    
	    
	    /**
	     * Includes the provided GameResult in the underlying database.
	     * 
	     * NOTE: Because the statistics stored are not explicitly GameResult
	     * instances, this method cannot distinguish between a new (unadded)
	     * GameResult and one that has already been added; thus, it is the
	     * caller's responsibility to ensure that this method is called
	     * exactly once for every GameResult concluded.
	     * 
	     * @param gr
	     */
	    public void addGameResult( GameResult gr ) {
	    	synchronized( DATABASE_MUTEX ) {
	    		// Get the game mode
	    		GameInformation ginfo = gr.getGameInformation(0) ;
	    		int gameMode = ginfo.mode ;
	    		
	    		// Step 1: make sure we have a summary for this game mode.
	    		addSummaryRowIfNeeded( gameMode ) ;
	    		
	    		// Step 2: include this game result in the summary row.
	    		addGameResultToSummary( gameMode, gr ) ;
	    		
	    		// Step 3: include this game result as a record
	    		addGameResultToRecords( gameMode, gr ) ;
	    		
	    		// Step 4: trim our records; we only keep a certain
	    		// number of exemplar Records, i.e. the top N in a particular
	    		// column.
	    		int numTrimmed = trimRecords( NUMBER_OF_RECORDS_TO_KEEP ) ;
	    		Log.d(TAG, "trimmed " + numTrimmed + " records") ;
	    	}
	    }
	    
	    
	    
	    /**
	     * Adds a row in the Summary table for the specified game mode.
	     * Returns whether the row was inserted; if 'false,' the row was
	     * already present.
	     * 
	     * @param gameMode
	     * @return
	     */
	    public boolean addSummaryRowIfNeeded( int gameMode ) {
	    	synchronized( DATABASE_MUTEX ) {
	    		if ( !hasSummary( gameMode ) ) {
	    			Summary s = new Summary() ;
	    			s.mGameMode = gameMode ;
	    			s.mNumStarts = 0 ;
	    			s.mNumEnds = 0 ; 
	    			s.mNumWins = 0 ;
	    			s.mNumLosses = 0 ;
	    			s.mTimeSpentPlaying = 0 ;
	    			
	    			insertSummary(s) ;
	    			return true ;
	    		}
	    		
	    		return false ;
	    	}
	    }
	    
	    
	    
	    public boolean deleteStatsAndSummary( int gameMode ) {
	    	synchronized( DATABASE_MUTEX ) {
	    		int summariesDeleted = mDB.delete(
	    				DATABASE_TABLE_SUMMARIES,
	    				KEY_GAME_MODE + " = " + gameMode, null) ;
	    		int recordsDeleted = mDB.delete( 
	    				DATABASE_TABLE_RECORDS,
	    				KEY_GAME_MODE + " = " + gameMode, null ) ;
	    		
	    		return summariesDeleted > 0 || recordsDeleted > 0 ;
	    	}
	    }
	    
	    
	    private void incrementSummaryGamesStarted( int gameMode ) {
	    	synchronized( DATABASE_MUTEX ) {
	    		Summary s = getSummary(gameMode) ;
	    		if ( s != null ) {
	    			s.mNumStarts += 1 ;
	    			updateSummary(s) ;
	    		} else
	    			throw new IllegalStateException("GameStats.DatabaseAdapter: no Summary row for game mode " + gameMode) ;
	    	}
	    }
	    
	    /**
	     * Inserts the summary as a new row in the database.
	     * 
	     * @param s
	     */
	    private void insertSummary( Summary s ) {
	    	synchronized( DATABASE_MUTEX ) {
	    		if ( hasSummary(s.mGameMode) )
	    			throw new IllegalStateException("Summary row for game mode " + s.mGameMode + " already present") ;
	    		
	    		ContentValues initialValues = new ContentValues() ;
    			initialValues.put(KEY_GAME_MODE, s.mGameMode) ;
    			initialValues.put(KEY_NUM_STARTS, s.mNumStarts) ;
    			initialValues.put(KEY_NUM_ENDS, s.mNumEnds) ;
    			initialValues.put(KEY_NUM_WINS, s.mNumWins) ;
    			initialValues.put(KEY_NUM_LOSSES, s.mNumLosses) ;
    			initialValues.put(KEY_TIME_SPENT_PLAYING, s.mTimeSpentPlaying) ;
    	    	
    	    	mDB.insert(DATABASE_TABLE_SUMMARIES, null, initialValues) ;
	    	}
	    }
	    
	    /**
	     * 
	     * @param s
	     */
	    private void updateSummary( Summary s ) {
	    	synchronized( DATABASE_MUTEX ) {
	    		if ( !hasSummary(s.mGameMode) )
	    			throw new IllegalStateException("Summary row for game mode " + s.mGameMode + " already present") ;
	    		
	    		
	    		ContentValues newValues = new ContentValues() ;
    			newValues.put(KEY_NUM_STARTS, s.mNumStarts) ;
    			newValues.put(KEY_NUM_ENDS, s.mNumEnds) ;
    			newValues.put(KEY_NUM_WINS, s.mNumWins) ;
    			newValues.put(KEY_NUM_LOSSES, s.mNumLosses) ;
    			newValues.put(KEY_TIME_SPENT_PLAYING, s.mTimeSpentPlaying) ;
    	    	
    			mDB.update(DATABASE_TABLE_SUMMARIES, newValues, KEY_GAME_MODE + " = " + s.mGameMode, null) ;
	    	}
	    }
	    
	    
	    
	    public boolean hasSummary( int gameMode ) {
	    	synchronized( DATABASE_MUTEX ) {
	    		Cursor cursor = mDB.query(
	    				DATABASE_TABLE_SUMMARIES,
	    				new String[]{ "COUNT(*)"},
	    				KEY_GAME_MODE + " = " + gameMode,
	    				null,
	    				null,
	    				null,
	    				null) ;
	    		
	    		try {
		    		cursor.moveToFirst() ;
		    		int num = cursor.getInt(0) ;
		    		if ( num > 1 ) {
		    			throw new IllegalStateException("GameStats.DatabaseAdapter: more than 1 Summary row for game mode " + gameMode) ;
		    		}
		    			
		    		return num == 1 ;
	    		} finally {
	    			cursor.close() ;
	    		}
	    	}
	    }
	    
	    
	    /**
	     * Returns a new instance of GameStats.Summary for the specified
	     * game mode.
	     * 
	     * If the provided game mode is not included in the database, 'null'
	     * is returned instead.
	     * 
	     * @param gameMode
	     * @return
	     */
	    public Summary getSummary( int gameMode ) {
	    	synchronized( DATABASE_MUTEX ) {
	    		Cursor cursor = mDB.query(
	    			DATABASE_TABLE_SUMMARIES,
	    			new String[] {
	    				KEY_GAME_MODE,			// 0
	    				KEY_NUM_STARTS,			// 1
	    			    KEY_NUM_ENDS,			// 2
	    			    KEY_NUM_WINS,			// 3
	    			    KEY_NUM_LOSSES,			// 4
	    			    KEY_TIME_SPENT_PLAYING},// 5
			    	KEY_GAME_MODE + " = " + gameMode,
			    	null,
			    	null,
			    	null,
			    	null) ;
	    		
	    		try {
		    		if ( cursor.getCount() == 0 ) {
		    			return null ;
		    		}
		    		else if ( cursor.getCount() > 1 ) {
		    			throw new IllegalStateException("GameStats.DatabaseAdapter: more than 1 Summary row for game mode " + gameMode) ;
		    		}
		    			
		    		cursor.moveToFirst() ;
		    		Summary s = new Summary() ;
		    		s.mGameMode = cursor.getInt(0) ;
		    		s.mNumStarts = cursor.getLong(1) ;
		    		s.mNumEnds = cursor.getLong(2) ;
		    		s.mNumWins = cursor.getLong(3) ;
		    		s.mNumLosses = cursor.getLong(4) ;
		    		s.mTimeSpentPlaying = cursor.getLong(5) ;
		    		
		    		return s ;
	    		} finally {
	    			cursor.close() ;
	    		}
	    	}
	    }
	    
	    
	    /**
	     * Returns an array of Records representing the 'top N' Records
	     * according to the provided valueCode, which must be one of
	     * Record.VALUE_*.
	     * 
	     * Records are ordered in descending value of 'valueCode.'
	     * 
	     * The length of the returned array, which is allocated by
	     * this call, is at most 'numberOfRecords' but may be smaller
	     * depending on the number of Records available.
	     * 
	     * @param numberOfRecords
	     * @param gameMode
	     * @param valueCode
	     * @return
	     */
	    public Record [] getBestRecordsForValueCode(
	    		int numberOfRecords, int gameMode, int valueCode ) {
	    	
	    	synchronized( DATABASE_MUTEX ) {
	    		ArrayList<Long> rowIDs = findBestRecordsByRowID(
	    	    		numberOfRecords, gameMode, valueCode, null ) ;
	    		
	    		return getRecords( rowIDs, valueCode ) ;
	    	}
	    	
	    }
	    
	    
	    /**
	     * Allocates and returns new Record objects as specified by the provided
	     * rowIDs, and sorted according to the provided valueCode (descending).
	     * 
	     * @param rowIDs
	     * @return
	     * @throws IOException 
	     */
	    private Record [] getRecords( ArrayList<Long> rowIDs, int sortByValueCode ) {
	    	if ( rowIDs.size() == 0 )
	    		return new Record[0] ;
	    	synchronized( DATABASE_MUTEX ) {
	    		String [] selectionColumns = new String[] {
	    				KEY_GAME_MODE,			// 0
	    				KEY_NONCE,				// 1
	    				KEY_TIME_STARTED,		// 2
	    				KEY_TIME_ENDED,			// 3
	    				KEY_TIME_LENGTH,		// 4
	    				KEY_ENDED,				// 5
	    				KEY_SCORE,				// 6
	    				KEY_HIGHEST_MULTIPLIER,	// 7
	    				KEY_CLEARS_S0,			// 8
	    				KEY_CLEARS_S1,			// 9
	    				KEY_CLEARS_SL,			// 10
	    				KEY_CLEARS_MO,			// 11
	    				KEY_BEST_CASCADE,		// 12
	    				KEY_STARTING_LEVEL,		// 13
	    				KEY_LEVEL,				// 14
	    				KEY_OUTCOME,			// 15
	    				// opponent?
	    				KEY_HAS_OPPONENT,				// 16
	    				KEY_OPPONENT_SCORE,				// 17
	    				KEY_OPPONENT_HIGHEST_MULTIPLIER,// 18
	    				KEY_OPPONENT_CLEARS_S0,			// 19
	    				KEY_OPPONENT_CLEARS_S1,			// 20
	    				KEY_OPPONENT_CLEARS_SL,			// 21
	    				KEY_OPPONENT_CLEARS_MO,			// 22
	    				KEY_OPPONENT_BEST_CASCADE,		// 23
	    				KEY_OPPONENT_STARTING_LEVEL,	// 24
	    				KEY_OPPONENT_LEVEL				// 25
	    		} ;
	    		
	    		StringBuilder sb = new StringBuilder() ;
	    		for ( int i = 0; i < rowIDs.size(); i++ ) {
	    			if ( i > 0 )
	    				sb.append(" OR ") ;
	    			sb.append(KEY_ROW_ID + " = " + rowIDs.get(i).longValue()) ;
	    		}
	    		
	    		String whereClause = sb.toString() ;
	    		
	    		Cursor cursor = mDB.query(
		    			DATABASE_TABLE_RECORDS,
		    			selectionColumns,
				    	whereClause,
				    	null,
				    	null,
				    	null,
				    	getRecordValueClauseByValueCode(sortByValueCode) + " DESC") ;
		    	
	    		try {
		    		if ( cursor.getCount() != rowIDs.size() ) {
		    			throw new IllegalStateException("GameStats.DatabaseAdapter: could not find all of the provided rowIDs.") ;
		    		}
		    			
		    		Record [] records = new Record[cursor.getCount()] ;
		    		int index = 0 ;
		    		
		    		cursor.moveToFirst() ;
		    		while( !cursor.isAfterLast() ) {
			    		Record r = new Record() ;
			    		r.mGameMode = cursor.getInt(0) ;
			    		try {
			    			r.mGameNonce = new Nonce(cursor.getString(1)) ;
			    		} catch( IOException ioe ) {
			    			Log.e(TAG, "Failed to parse nonce " + cursor.getString(1)) ;
			    		}
			    		r.mTimeBegun = cursor.getLong(2) ;
			    		r.mTimeEnded = cursor.getLong(3) ;
			    		r.mTimeSpentPlaying = cursor.getLong(4) ;
			    		r.mEnded = cursor.getInt(5) != 0 ;
			    		r.mScore = cursor.getLong(6) ;
			    		r.mHighestMultiplier = cursor.getFloat(7) ;
			    		r.mClearsS0 = cursor.getInt(8) ;
			    		r.mClearsS1 = cursor.getInt(9) ;
			    		r.mClearsSL = cursor.getInt(10) ;
			    		r.mClearsMO = cursor.getInt(11) ;
			    		r.mBestCascade = cursor.getInt(12) ;
			    		r.mStartingLevel = cursor.getInt(13) ;
			    		r.mLevel = cursor.getInt(14) ;
			    		r.mOutcome = cursor.getInt(15) ;
				    	// opponent?
			    		r.mHasOpponent = cursor.getInt(16) != 0 ;
			    		if ( r.mHasOpponent ) {
			    			r.mOpponentScore = cursor.getLong(17) ;
			    			r.mOpponentHighestMultiplier = cursor.getLong(18) ;
			    			r.mOpponentClearsS0 = cursor.getInt(19) ;
				    		r.mOpponentClearsS1 = cursor.getInt(20) ;
				    		r.mOpponentClearsSL = cursor.getInt(21) ;
				    		r.mOpponentClearsMO = cursor.getInt(22) ;
				    		r.mOpponentBestCascade = cursor.getInt(23) ;
				    		r.mOpponentStartingLevel = cursor.getInt(24) ;
				    		r.mOpponentLevel = cursor.getInt(25) ;
			    		}
			    		
			    		cursor.moveToNext() ;
			    		
			    		records[index++] = r ;
		    		}
		    		
		    		return records ;
	    		} finally {
	    			cursor.close() ;
	    		}
	    	}
	    }
	    
	    
	    private void addGameResultToSummary( int gameMode, GameResult gr ) {
	    	synchronized( DATABASE_MUTEX ) {
	    		Summary s = getSummary(gameMode) ;
	    		if ( s != null ) {
	    			// increment everything
	    			s.mNumEnds++ ;
	    			
	    			int playerSlot = gr.getLocalPlayerSlot() ;
	    			if ( playerSlot >= 0 ) {
		    			if ( gr.getWon(playerSlot) )
		    				s.mNumWins++ ;
		    			else if ( gr.getLost(playerSlot) )
		    				s.mNumLosses++ ;
		    			
		    			s.mTimeSpentPlaying += gr.getTimeInGame() ;
		    			
		    			// return to the database.
		    			updateSummary(s) ;
	    			}
	    		} else
	    			throw new IllegalStateException("GameStats.DatabaseAdapter: does not contain a Summary for game mode " + gameMode) ;
	    	}
	    }
	    
	    
	    private void addGameResultToRecords( int gameMode, GameResult gr ) {
	    	synchronized( DATABASE_MUTEX ) {
	    		// Make a Record object for this game result, then insert it
	    		// into the database.  We don't need to do anything else here;
	    		// for example, there is a separate method for trimming results.
	    		Record r = new Record(gr) ;
	    		insertRecord(r) ;
	    	}
	    }
	    
	    
	    /**
	     * Inserts the summary as a new row in the database.
	     * 
	     * @param s
	     */
	    private void insertRecord( Record r ) {
	    	synchronized( DATABASE_MUTEX ) {
	    		// We have no way of checking for this record in the database.
	    		ContentValues initialValues = new ContentValues() ;
	    		// put game mode and nonce; probably the most important.
	    		initialValues.put(KEY_GAME_MODE, r.getGameMode()) ;
	    		initialValues.put(KEY_NONCE, r.getGameNonce().toString()) ;
	    		// time information
	    		initialValues.put(KEY_TIME_STARTED, r.getTimeBegun()) ;
	    		initialValues.put(KEY_TIME_ENDED, r.getTimeEnded()) ;
	    		initialValues.put(KEY_TIME_LENGTH, r.getTimeSpentPlaying()) ;
	    		initialValues.put(KEY_ENDED, r.getEnded()) ;
	    		// user score and multiplier
	    		initialValues.put(KEY_SCORE, r.getScore()) ;
	    		initialValues.put(KEY_HIGHEST_MULTIPLIER, r.getHighestMultiplier()) ;
	    		// clears and level
	    		initialValues.put(KEY_CLEARS_S0, r.getClearsS0()) ;
	    		initialValues.put(KEY_CLEARS_S1, r.getClearsS1()) ;
	    		initialValues.put(KEY_CLEARS_SL, r.getClearsSL()) ;
	    		initialValues.put(KEY_CLEARS_MO, r.getClearsMO()) ;
	    		initialValues.put(KEY_BEST_CASCADE, r.getBestCascade()) ;
	    		initialValues.put(KEY_STARTING_LEVEL, r.getStartingLevel()) ;
	    		initialValues.put(KEY_LEVEL, r.getLevel()) ;
	    		// outcome!
	    		initialValues.put(KEY_OUTCOME, r.getOutcome()) ;
	    		// opponent?
	    		initialValues.put(KEY_HAS_OPPONENT, r.getHasOpponent()) ;
	    		if ( r.getHasOpponent() ) {
	    			// user score and multiplier
		    		initialValues.put(KEY_OPPONENT_SCORE, r.getOpponentScore()) ;
		    		initialValues.put(KEY_OPPONENT_HIGHEST_MULTIPLIER, r.getOpponentHighestMultiplier()) ;
		    		// clears and level
		    		initialValues.put(KEY_OPPONENT_CLEARS_S0, r.getOpponentClearsS0()) ;
		    		initialValues.put(KEY_OPPONENT_CLEARS_S1, r.getOpponentClearsS1()) ;
		    		initialValues.put(KEY_OPPONENT_CLEARS_SL, r.getOpponentClearsSL()) ;
		    		initialValues.put(KEY_OPPONENT_CLEARS_MO, r.getOpponentClearsMO()) ;
		    		initialValues.put(KEY_OPPONENT_BEST_CASCADE, r.getOpponentBestCascade()) ;
		    		initialValues.put(KEY_OPPONENT_STARTING_LEVEL, r.getOpponentStartingLevel()) ;
		    		initialValues.put(KEY_OPPONENT_LEVEL, r.getOpponentLevel()) ;
	    		}
	    		
	    		// put it in the database.
    	    	mDB.insert(DATABASE_TABLE_RECORDS, null, initialValues) ;
	    	}
	    }
	    
	    
	    /**
	     * Removes any extra Records from the database.  We don't keep exactly
	     * 'numberOfRecords' rows, or even #gameModes * numberOfRecords.
	     * Instead, we trim within each value of interest and within each game
	     * mode so that the best 'numberOfRecords' are left.  For example,
	     * consider 'score,' certainly a value of interest.  We determine 
	     * for a particular game mode which 'N' records represent the 'N'
	     * highest scores.  We "protect" those records from removal, as they
	     * are significant, but the remaining |gameMode| - 'N' do not receive
	     * protection from this comparison.  For a particular record, if there
	     * is NO value of interest which offers protection in this way, it
	     * will be removed.
	     * 
	     * @param numberOfRecords
	     * @return The number of records "trimmed"
	     */
	    public int trimRecords( int numberOfRecords ) {
	    	int numTrimmed = 0 ;
	    	synchronized( DATABASE_MUTEX ) {
	    		// get a list of game modes for which we have records.
	    		// Iterate through each of them.
	    		Cursor cursor = mDB.query(
		    			DATABASE_TABLE_RECORDS,
		    			new String[] {"DISTINCT " + KEY_GAME_MODE},
				    	null,
				    	null,
				    	null,
				    	null,
				    	null) ;
	    		
	    		try {
		    		cursor.moveToFirst() ;
		    		while ( !cursor.isAfterLast() ) {
		    			// It is safe to do this completely independent
		    			// operation because each Record represents a single game
		    			// mode; if a Record with game mode 'gm' is not 'protected'
		    			// within gm and is in fact removed, it is impossible for
		    			// some other game mode to have protected it.
		    			numTrimmed += trimRecords( numberOfRecords, cursor.getInt(0) ) ;
		    			cursor.moveToNext() ;
		    		}
	    		} finally {
	    			cursor.close() ;
	    		}
	    	}
	    	
	    	return numTrimmed ;
	    }
	    
	    
	    /**
	     * Trims the records for the specified game such that, to be kept,
	     * a record must occur in the first 'numberOfRecords' entries in a
	     * sorted list of some value of interest for the records.  For example,
	     * the most recent 'numberOfRecords' Records will be kept, because
	     * VALUE_TIME_ENDED is a value of interest.  Those after 'numberOfRecords'
	     * may be kept, but if so, it will be because they are significant
	     * according to some other value of interest.
	     * 
	     * 
	     * @param numberOfRecords
	     * @param gameMode
	     * @return The number of entries "trimmed."
	     */
	    public int trimRecords( int numberOfRecords, int gameMode ) {
	    	synchronized( DATABASE_MUTEX ) {
	    		// we do this in two steps.
	    		// First, we collect those ROW_IDs
	    		// which are 'protected' by iterating through values of
	    		// interest and querying the row_id for 'numberOfRecords'
	    		// Records ordered by that value.  We take the Union of the
	    		// row IDs for each query.
	    		ArrayList<Long> rowIDs = new ArrayList<Long>() ;
	    		for ( int v = 0; v < Record.NUM_VALUES; v++ )
	    			findBestRecordsByRowID( numberOfRecords, gameMode, v, rowIDs ) ;
	    		
	    		
	    		// Second, we remove every record which 
	    		// 1. has the specified game mode, and
	    		// 2. does NOT appear in the list of row IDs.
	    		// build the where clause...
	    		StringBuilder sb = new StringBuilder() ;
	    		sb.append(KEY_GAME_MODE + " = " + gameMode) ;
	    		for ( int i = 0; i < rowIDs.size(); i++ )
	    			sb.append(" AND ").append(KEY_ROW_ID + " <> " + rowIDs.get(i).longValue()) ;
	    		
	    		return mDB.delete(DATABASE_TABLE_RECORDS, sb.toString(), null) ;
	    	}
	    }
	    
	    
	    /**
	     * Examines the Records in the database for Records which have
	     * the specified gameMode, and occur in the first 'numberOfRecords'
	     * of a list sorted by valueCode.  Adds those rowIDs to the 
	     * provided ArrayList, provided they do not already occur.
	     * 
	     * Upon completion, rowIDs will contain the Union of its previous
	     * contents and the rowIDs of those Records which meet the criteria.
	     * In other words, the size of rowIDs will increase by between 0
	     * and 'numberOfRecords.'  If 'null,' a new ArrayList is allocated.
	     * 
	     * @param numberOfRecords
	     * @param gameMode
	     * @param valueCode
	     * @param rowIDs
	     * @return rowIDs if provided; otherwise, a newly allocated ArrayList.
	     */
	    private ArrayList<Long> findBestRecordsByRowID(
	    		int numberOfRecords, int gameMode,
	    		int valueCode, ArrayList<Long> rowIDs ) {
	    	
	    	if ( rowIDs == null )
	    		rowIDs = new ArrayList<Long>() ;
	    	
	    	synchronized( DATABASE_MUTEX ) {
	    		Cursor cursor = mDB.query(
	    				DATABASE_TABLE_RECORDS,
	    				new String[]{KEY_ROW_ID},
	    				KEY_GAME_MODE + " = " + gameMode,
	    				null,
	    				null,
	    				null,
	    				getRecordValueClauseByValueCode(valueCode) + " DESC",
	    				"0, " + numberOfRecords) ;
	    		
	    		try {
		    		if ( cursor.getColumnCount() > numberOfRecords ) {
		    			throw new IllegalStateException("query returned more than " + numberOfRecords + " rows!") ;
		    		}
		    			
		    		cursor.moveToFirst() ;
		    		while ( !cursor.isAfterLast() ) {
		    			Long rowId = cursor.getLong(0) ;
		    			if ( !rowIDs.contains(rowId) )
		    				rowIDs.add(rowId) ;
		    			cursor.moveToNext() ;
		    		}
	    		} finally {
	    			cursor.close() ;
	    		}
	    	}
	    	
	    	return rowIDs ;
	    }
	    
	    /**
	     * A helper function that converts between the Record.VALUE_* code
	     * and a String which would represent that value in the database
	     * 'Record' table.  If the Value is an explicit column entry, the
	     * name of that column is returned.  If it is a calculated value,
	     * an expression in SQL terms that evaluates to that value is returned.
	     * 
	     * @param valueCode
	     * @return
	     */
	    private String getRecordValueClauseByValueCode( int valueCode ) {
	    	switch( valueCode ) {
	    	case Record.VALUE_GAME_LENGTH:
	    		return KEY_TIME_LENGTH ;
			case Record.VALUE_SCORE:
				return KEY_SCORE ;
			case Record.VALUE_MAX_MULTIPLIER:
				return KEY_HIGHEST_MULTIPLIER ;
			case Record.VALUE_TOTAL_CLEARS:
				StringBuilder sb = new StringBuilder() ;
				sb.append(KEY_CLEARS_S0) ;
				sb.append(" + ").append(KEY_CLEARS_S1) ;
				sb.append(" + 2 * ").append(KEY_CLEARS_SL) ;
				sb.append(" + 2 * ").append(KEY_CLEARS_MO) ;
				return sb.toString() ;
			case Record.VALUE_LEVEL:
				return KEY_LEVEL ;
			case Record.VALUE_LEVEL_UPS:
				return KEY_LEVEL + " - " + KEY_STARTING_LEVEL ;
			case Record.VALUE_BEST_CASCADE:
				return KEY_BEST_CASCADE ;
			case Record.VALUE_TIME_ENDED:
				return KEY_TIME_ENDED ;
	    	}
	    	
	    	throw new IllegalArgumentException("Cannot convert specified value code " + valueCode + " to an SQL expression") ;
	    }
		
	}
	
}
