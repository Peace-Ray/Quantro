package com.peaceray.quantro.database;

import com.peaceray.quantro.model.game.GameSettings;
import com.peaceray.quantro.model.modes.GameModes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class GameSettingsDatabaseAdapter {
	
	private static final int DEFAULT_GAME_MODE = 0 ;
	private static final int DEFAULT_GAME_MODE_PLAYERS = 1 ;
	
	private static final Object DATABASE_MUTEX = new Object() ;
	
	public static final String KEY_ROWID = "_id";
	public static final String KEY_TIME_UPDATED = "time_updated" ;
	public static final String KEY_SINGLE_PLAYER = "single_player" ;
    public static final String KEY_MODE = "mode" ;
    public static final String KEY_PLAYERS = "players" ;
    public static final String KEY_LEVEL = "level" ;
    public static final String KEY_CLEARS_PER_LEVEL = "clears_per_level" ;
    public static final String KEY_GARBAGE = "garbage" ;
    public static final String KEY_GARBAGE_PER_LEVEL = "garbage_per_level" ;
    public static final String KEY_LEVEL_LOCK = "level_lock" ;
    public static final String KEY_DIFFICULTY = "difficulty" ;
    public static final String KEY_DISPLACEMENT_FIXED_RATE = "fixed_rate" ;
    
    private static final String TAG = "GameSettingsDatabaseAdapter";
    
    private static final String DATABASE_NAME = "quantro_game_settings";
    private static final String DATABASE_TABLE = "most_recent_game_settings";
    
    private static final int DATABASE_VERSION = 4;
    // 1: test version.  many changes until the first instantiation of 'cda'.
    // 2: adds 'level lock'
    // 3: adds 'difficulty' and 'fixed rate'
    // 4: adds 'players'
    
    private static final String DATABASE_CREATE =
        "create table " + DATABASE_TABLE
        + " ("+KEY_ROWID+" integer primary key autoincrement, "
        + KEY_TIME_UPDATED+" bigint not null, "
        + KEY_SINGLE_PLAYER+" tinyint not null, "
        + KEY_MODE+" int not null, "
        + KEY_LEVEL+" int, "
        + KEY_CLEARS_PER_LEVEL+" int, "
        + KEY_GARBAGE+" int, "
        + KEY_GARBAGE_PER_LEVEL+" int, "
        + KEY_LEVEL_LOCK+" tinyint, "
        + KEY_DIFFICULTY+" int, "
        + KEY_DISPLACEMENT_FIXED_RATE+" real, "
        + KEY_PLAYERS+" int ) " ;
    
    private static final String COLUMN_DEF_LEVEL_LOCK = KEY_LEVEL_LOCK + " tinyint" ;
    private static final String COLUMN_DEF_DIFFICULTY = KEY_DIFFICULTY + " int" ;
    private static final String COLUMN_DEF_DISPLACEMENT_FIXED_RATE = KEY_DISPLACEMENT_FIXED_RATE + " int" ;
    private static final String COLUMN_DEF_PLAYERS = KEY_PLAYERS + " int" ;
	
    	
    private final Context context; 
    
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public GameSettingsDatabaseAdapter(Context ctx) 
    {
    	synchronized( DATABASE_MUTEX ) {
	        this.context = ctx;
	        DBHelper = new DatabaseHelper(context);
    	}
    }
        
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
        int newVersion) 
        {
        	int version = oldVersion ;
        	if ( version == 1 ) {
        		Log.w(TAG, "Upgrading GameSettings database from version " + version + " to " + newVersion + " via 2 by adding 'level_lock' column") ;
        		db.execSQL("ALTER TABLE " + DATABASE_TABLE + " ADD COLUMN " + COLUMN_DEF_LEVEL_LOCK) ;
        		version = 2 ;		// we are now level 2
        	}
        	
        	if ( version == 2 ) {
        		Log.w(TAG, "Upgrading GameSettings database from version " + version + " to " + newVersion + " via 3 by adding 'difficulty' and 'displacement_fixed_rate' columns") ;
        		db.execSQL("ALTER TABLE " + DATABASE_TABLE + " ADD COLUMN " + COLUMN_DEF_DIFFICULTY) ;
        		db.execSQL("ALTER TABLE " + DATABASE_TABLE + " ADD COLUMN " + COLUMN_DEF_DISPLACEMENT_FIXED_RATE) ;
        		version = 3 ;		// we are now level 3
        	}
        	
        	if ( version == 3 ) {
        		Log.w(TAG, "Upgrading GameSettings database from version " + version + " to " + newVersion + " via 4 by adding 'players' column") ;
        		db.execSQL("ALTER TABLE " + DATABASE_TABLE + " ADD COLUMN " + KEY_PLAYERS) ;
        		version = 4 ;		// we are now level 4
        	}
        	
        	if ( version != newVersion ) {
	            Log.w(TAG, "Upgrading GameSettings database from version " + oldVersion 
	                    + " to "
	                    + newVersion + ", which will destroy all old data");
	            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
	            onCreate(db);
        	}
        }
    }   
    
    //---opens the database---
    public GameSettingsDatabaseAdapter open() throws SQLException 
    {
    	synchronized( DATABASE_MUTEX ) {
	        db = DBHelper.getWritableDatabase();
	        return this;
    	}
    }

    //---closes the database---    
    public void close() 
    {
    	synchronized( DATABASE_MUTEX ) {
    		DBHelper.close();
    	}
    }
    
    
    public static void updateMostRecentInDatabase( Context context, GameSettings gs ) {
    	GameSettingsDatabaseAdapter gsda = new GameSettingsDatabaseAdapter( context ) ;
    	gsda.open() ;
    	try {
    		gsda.updateMostRecent(gs) ;
    	} finally {
    		gsda.close() ;
    	}
    }
    
    public void updateMostRecent( GameSettings gs ) {
    	synchronized( DATABASE_MUTEX ) {
    		// two steps.  First, remove the row from the table (if exists).
    		// Then add the provided entry.
    		db.delete(DATABASE_TABLE, KEY_MODE + " = ?", new String[]{ ""+gs.getMode() }) ;
    		
    		ContentValues initialValues = new ContentValues() ;
    		initialValues.put(KEY_TIME_UPDATED, System.currentTimeMillis() ) ;
    		initialValues.put(KEY_SINGLE_PLAYER, gs.getPlayers() == 1 ? 1 : 0 ) ;
    		initialValues.put(KEY_MODE, gs.getMode()) ;
    		if ( gs.hasLevel() )
    			initialValues.put(KEY_LEVEL, gs.getLevel()) ;
    		if ( gs.hasClearsPerLevel() )
    			initialValues.put(KEY_CLEARS_PER_LEVEL, gs.getClearsPerLevel()) ;
    		if ( gs.hasGarbage() )
    			initialValues.put(KEY_GARBAGE, gs.getGarbage()) ;
    		if ( gs.hasGarbagePerLevel() )
    			initialValues.put(KEY_GARBAGE_PER_LEVEL, gs.getGarbagePerLevel()) ;
    		if ( gs.hasLevelLock() )
    			initialValues.put(KEY_LEVEL_LOCK, gs.getLevelLock() ? 1 : 0) ;
    		if ( gs.hasDifficulty() )
    			initialValues.put(KEY_DIFFICULTY, gs.getDifficulty()) ;
    		if ( gs.hasDisplacementFixedRate() )
    			initialValues.put(KEY_DISPLACEMENT_FIXED_RATE, gs.getDisplacementFixedRate()) ;
    		// number of players is required for GameSettings (but is not
    		// "non-null" in our database, because some DB rows may exist from
    		// before it was added).
    		initialValues.put(KEY_PLAYERS, gs.getPlayers()) ;
    		
    		db.insert(DATABASE_TABLE, null, initialValues) ;
    	}
    	
    }
    
    
    public static GameSettings getMostRecentInDatabase( Context context, int gameMode ) {
    	GameSettingsDatabaseAdapter gsda = new GameSettingsDatabaseAdapter( context ) ;
    	gsda.open() ;
    	try {
    		return gsda.getMostRecent( gameMode ) ;
    	} finally {
    		gsda.close() ;
    	}
    }
    
    public GameSettings getMostRecent( int gameMode ) {
    	synchronized( DATABASE_MUTEX ) {
    		Cursor c = db.query(
    				DATABASE_TABLE,
    				null,
    				KEY_MODE + " = " + gameMode,
    				null,
    				null,
    				null,
    				null) ;
    		
    		try {
	    		if ( c.getCount() == 0 ) {
	    			return new GameSettings(gameMode, GameModes.minPlayers(gameMode)).setImmutable() ;
	    		}
	    		
	    		else {
	    			c.moveToFirst() ;
	    			GameSettings gs = getGameSettings( c ) ;
	    			return gs ;
	    		}
    		} finally {
    			c.close() ;
    		}
    	}
    }
    
    
    public GameSettings getMostRecent() {
    	synchronized( DATABASE_MUTEX ) {
    		// get list in descending order
    		Cursor c = db.query(
    				DATABASE_TABLE,
    				null,
    				null,
    				null,
    				null,
    				null,
    				KEY_TIME_UPDATED + " DESC") ;
    		
    		try {
	    		if ( c.getCount() == 0 ) {
	    			return new GameSettings(DEFAULT_GAME_MODE, DEFAULT_GAME_MODE_PLAYERS).setImmutable() ;
	    		}
	    		
	    		else {
	    			c.moveToFirst() ;
	    			GameSettings gs = getGameSettings( c ) ;
	    			return gs ;
	    		}
    		} finally {
    	    	c.close() ;
    		}
    	}
    }
    
    public static GameSettings getMostRecentSinglePlayerInDatabase( Context context ) {
    	GameSettingsDatabaseAdapter gsda = new GameSettingsDatabaseAdapter( context ) ;
    	gsda.open() ;
    	try {
    		return gsda.getMostRecentSinglePlayer() ;
    	} finally {
    		gsda.close() ;
    	}
    }
    
    public GameSettings getMostRecentSinglePlayer() {
    	synchronized( DATABASE_MUTEX ) {
    		// get list in descending order
    		Cursor c = db.query(
    				DATABASE_TABLE,
    				null,
    				null,
    				null,
    				null,
    				null,
    				KEY_TIME_UPDATED + " DESC") ;
    		
    		try {
				c.moveToFirst() ;
				while ( !c.isAfterLast() ) {
					GameSettings gs = getGameSettings( c ) ;
					if ( gs.getPlayers() == 1 ) {
						return gs ;
					}
					c.moveToNext() ;
				}
	    		
				return new GameSettings(DEFAULT_GAME_MODE, DEFAULT_GAME_MODE_PLAYERS).setImmutable() ;
    		} finally {
    			c.close() ;
    		}
    	}
    }
    
    
    public static boolean deleteFromDatabase( Context context, int gameMode ) {
    	GameSettingsDatabaseAdapter gsda = new GameSettingsDatabaseAdapter( context ) ;
    	gsda.open() ;
    	try {
    		return gsda.delete(gameMode) ;
    	} finally {
    		gsda.close() ;
    	}
    }
    
    
    public boolean delete( int gameMode ) {
    	synchronized( DATABASE_MUTEX ) {
    		int rowsRemoved = db.delete(DATABASE_TABLE,
    				KEY_MODE + " = " + gameMode, null) ;
    		return rowsRemoved > 0 ;
    	}
    }
    
    
    private GameSettings getGameSettings( Cursor c ) {
    	
    	GameSettings gs = new GameSettings() ;
    	
    	// set the mode...
    	gs.setMode( c.getInt( c.getColumnIndex(KEY_MODE) ) ) ;
    	
    	// set level, clears, etc.
    	if ( !c.isNull( c.getColumnIndex( KEY_LEVEL ) ) )
    		gs.setLevel( c.getInt( c.getColumnIndex(KEY_LEVEL) ) ) ;
    	if ( !c.isNull( c.getColumnIndex( KEY_CLEARS_PER_LEVEL ) ) )
    		gs.setClearsPerLevel( c.getInt( c.getColumnIndex(KEY_CLEARS_PER_LEVEL) ) ) ;
    	if ( !c.isNull( c.getColumnIndex( KEY_GARBAGE ) ) )
    		gs.setGarbage( c.getInt( c.getColumnIndex(KEY_GARBAGE) ) ) ;
    	if ( !c.isNull( c.getColumnIndex( KEY_GARBAGE_PER_LEVEL ) ) )
    		gs.setGarbagePerLevel( c.getInt( c.getColumnIndex(KEY_GARBAGE_PER_LEVEL) ) ) ;
    	if ( !c.isNull( c.getColumnIndex( KEY_LEVEL_LOCK ) ) )
    		gs.setLevelLock( c.getInt(c.getColumnIndex( KEY_LEVEL_LOCK) ) == 1 ) ;
    	if ( !c.isNull( c.getColumnIndex( KEY_DIFFICULTY ) ) )
    		gs.setDifficulty( c.getInt(c.getColumnIndex( KEY_DIFFICULTY) ) ) ;
    	if ( !c.isNull( c.getColumnIndex( KEY_DISPLACEMENT_FIXED_RATE ) ) )
    		gs.setDisplacementFixedRate( c.getDouble( c.getColumnIndex( KEY_DISPLACEMENT_FIXED_RATE ) ) ) ;
    	if ( !c.isNull( c.getColumnIndex( KEY_PLAYERS ) ) ) {
    		gs.setPlayers( c.getInt(c.getColumnIndex( KEY_PLAYERS ) ) ) ;
    	} else {
    		gs.setPlayers( GameModes.minPlayers(gs.getMode()) ) ;
    	}
    	
    	return gs.setImmutable() ;
    }
    
}
