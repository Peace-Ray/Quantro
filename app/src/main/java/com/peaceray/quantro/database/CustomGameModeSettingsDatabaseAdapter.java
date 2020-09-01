package com.peaceray.quantro.database;

import java.util.ArrayList;

import com.peaceray.quantro.model.modes.CustomGameModeSettings;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class CustomGameModeSettingsDatabaseAdapter {
	
	private static final String TAG = "CustomGameModeSettingsDatabaseAdapter";

	private static final Object DATABASE_MUTEX = new Object() ;
	
	public static final String KEY_ID = "_id";
	public static final String KEY_TIME_CREATED = "time_created" ;
	public static final String KEY_NAME = "name" ;
	public static final String KEY_SUMMARY = "summary" ;
	public static final String KEY_DESCRIPTION = "description" ;
	public static final String KEY_QPANES = "qpanes" ;
	public static final String KEY_ROWS = "rows" ;
	public static final String KEY_COLS = "cols" ;
	public static final String KEY_HAS_TRIONIMOS = "has_trionimos" ;
	public static final String KEY_HAS_TETROMINOS = "has_tetrominos" ;
	public static final String KEY_HAS_PENTOMINOS = "has_pentominos" ;
	public static final String KEY_HAS_ROTATION = "has_rotation" ;
	public static final String KEY_HAS_REFLECTION = "has_reflection" ;
	public static final String KEY_ALLOW_MULTIPLAYER = "allow_multiplayer" ;
    
    
	private static final String DATABASE_NAME = "quantro_custom_game_mode_settings";
    private static final String DATABASE_TABLE = "custom_game_mode_settings";
    
    private static final int DATABASE_VERSION = 2;
    // 1: test version.  many changes until the first instantiation.
    // 2: adds 'qpanes' as a field, with 1 as the default.
    
    private static final String DATABASE_CREATE =
        "create table " + DATABASE_TABLE
        + " ("+KEY_ID+" integer primary key, "
        + KEY_TIME_CREATED+" bigint not null, "
        + KEY_NAME+" text not null, "
        + KEY_SUMMARY+" text not null, "
        + KEY_DESCRIPTION+" text not null, "
        + KEY_QPANES+" int not null default 1, "
        + KEY_ROWS+" int not null, "
        + KEY_COLS+" int not null, "
        + KEY_HAS_TRIONIMOS+" int not null, "
        + KEY_HAS_TETROMINOS+" int not null, "
        + KEY_HAS_PENTOMINOS+" int not null, "
        + KEY_HAS_ROTATION+" int not null, "
        + KEY_HAS_REFLECTION+" int not null, "
        + KEY_ALLOW_MULTIPLAYER+" int not null "
        + ") " ;
    	
    	
    private final Context context; 
    
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public CustomGameModeSettingsDatabaseAdapter(Context ctx) 
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
        	// update from 1 to 2: add the "qpanes" column.
            Log.w(TAG, "Upgrading GameSettings database from version " + oldVersion 
                    + " to "
                    + newVersion ) ;
            db.execSQL("ALTER TABLE " + DATABASE_TABLE
            		+ " ADD " + KEY_QPANES + " int not null default 1") ;
        }
    }   
    
    //---opens the database---
    public CustomGameModeSettingsDatabaseAdapter open() throws SQLException 
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
    
    
    public static int count( Context context ) {
    	CustomGameModeSettingsDatabaseAdapter da = new CustomGameModeSettingsDatabaseAdapter(context) ;
    	da.open() ;
    	try {
    		return da.countCustomGameModeSettings() ;
    	} finally {
    		da.close() ;
    	}
    }
    
    
    public static boolean has( Context context, CustomGameModeSettings cgms ) {
    	CustomGameModeSettingsDatabaseAdapter da = new CustomGameModeSettingsDatabaseAdapter(context) ;
    	da.open() ;
    	try {
    		return da.hasCustomGameModeSettings(cgms) ;
    	} finally {
    		da.close() ;
    	}
    }
    
    public static ArrayList<CustomGameModeSettings> getAll( Context context ) {
    	CustomGameModeSettingsDatabaseAdapter da = new CustomGameModeSettingsDatabaseAdapter(context) ;
    	da.open() ;
    	try {
    		return da.getAllCustomGameModeSettings() ;
    	} finally {
    		da.close() ;
    	}
    }
    
    public static CustomGameModeSettings get( Context context, int id ) {
    	CustomGameModeSettingsDatabaseAdapter da = new CustomGameModeSettingsDatabaseAdapter(context) ;
    	da.open() ;
    	try {
    		return da.getCustomGameModeSettings(id) ;
    	} finally {
    		da.close() ;
    	}
    }
    
    
    
    
    public static boolean put( Context context, CustomGameModeSettings cgms ) {
    	CustomGameModeSettingsDatabaseAdapter da = new CustomGameModeSettingsDatabaseAdapter(context) ;
    	da.open() ;
    	try {
    		return da.putCustomGameModeSettings(cgms) ;
    	} finally {
    		da.close() ;
    	}
    }
    
    public static boolean delete( Context context, CustomGameModeSettings cgms ) {
    	CustomGameModeSettingsDatabaseAdapter da = new CustomGameModeSettingsDatabaseAdapter(context) ;
    	da.open() ;
    	try {
    		return da.deleteCustomGameModeSettings(cgms) ;
    	} finally {
    		da.close() ;
    	}
    }
    
    public static int deleteAll( Context context ) {
    	CustomGameModeSettingsDatabaseAdapter da = new CustomGameModeSettingsDatabaseAdapter(context) ;
    	da.open() ;
    	try {
    		return da.deleteAllCustomGameModeSettings() ;
    	} finally {
    		da.close() ;
    	}
    }
    
    
    
    
    
    public int countCustomGameModeSettings() {
    	synchronized ( DATABASE_MUTEX ) {
    		Cursor c = db.query(
    				DATABASE_TABLE, 
    				new String[]{ "count(*)" },
    				null,
    				null,
    				null,
    				null,
    				null) ;
    		
    		try {
	    		c.moveToFirst() ;
	    		return c.getInt(0) ;
    		} finally {
    			c.close();
    		}
    	}
    }
    
    
    /**
     * Attempts to delete the provided CGMS (by its id).  Returns whether
     * any rows were deleted.
     * @param cgms
     * @return
     */
    public boolean deleteCustomGameModeSettings( CustomGameModeSettings cgms ) {
    	synchronized( DATABASE_MUTEX ) {
    		return db.delete(DATABASE_TABLE, KEY_ID + " = " + cgms.getID(), null) > 0 ;
    	}
    }
    
    
    /**
     * Deletes all custom game mode settings objects.  Returns the number
     * of rows removed.
     * @return
     */
    public int deleteAllCustomGameModeSettings() {
    	synchronized( DATABASE_MUTEX ) {
    		return db.delete(DATABASE_TABLE, "1", null) ;
    	}
    }
    
    
    /**
     * Retrieve all currently stored custom game mode settings.  They are returned
     * in ASCENDING order of creation time (most recent is last).
     */
    public ArrayList<CustomGameModeSettings> getAllCustomGameModeSettings() {
    	synchronized( DATABASE_MUTEX ) {
    		Cursor c = db.query(
    				DATABASE_TABLE,
    				null,
    				null,
    				null,
    				null,
    				null,
    				KEY_TIME_CREATED + " ASC") ;
    		
    		ArrayList<CustomGameModeSettings> ar = new ArrayList<CustomGameModeSettings>() ;
    		
    		// 'try' so we can close the cursor in 'finally'
    		try {
    			if ( c.getCount() == 0 )
    				return ar ;

    			c.moveToFirst() ;
    			while ( !c.isAfterLast() ) {
    				ar.add( readCustomGameModeSettingsFromCursor( c ) ) ;
    				c.moveToNext() ;
    			}
    			
    			return ar ;
    				
    		} finally {
    			c.close() ;
    		}
    	}
    }
    
    /**
     * Retrieve all currently stored custom game mode settings.  They are returned
     * in ASCENDING order of creation time (most recent is last).
     */
    public CustomGameModeSettings getCustomGameModeSettings(int id) {
    	synchronized( DATABASE_MUTEX ) {
    		Cursor c = db.query(
    				DATABASE_TABLE,
    				null,
    				KEY_ID + " = " + id,
    				null,
    				null,
    				null,
    				KEY_TIME_CREATED + " ASC") ;
    		
    		// 'try' so we can close the cursor in 'finally'
    		try {
    			if ( c.getCount() == 0 )
    				return null ;

    			c.moveToFirst() ;
    			return readCustomGameModeSettingsFromCursor( c ) ;
    		} finally {
    			c.close() ;
    		}
    	}
    }
    
    
    /**
     * Updates the provided CustomGameModeSettings object's database row
     * (using its 'id' to identify a previous version), inserting it if necessary.
     * 
     * Returns whether a new row was created.
     * @param cgms
     * @return Whether a new row was created.
     */
    public boolean putCustomGameModeSettings( CustomGameModeSettings cgms ) {
    	synchronized ( DATABASE_MUTEX ) {
    		if ( !hasCustomGameModeSettings( cgms ) ) {
    			insertCustomGameModeSettings( cgms ) ;
    			return true ;
    		}
    		else {
    			updateCustomGameModeSettings( cgms ) ;
    			return false ;
    		}
    	}
    }
    
    
    private boolean hasCustomGameModeSettings( CustomGameModeSettings cgms ) {
    	synchronized ( DATABASE_MUTEX ) {
    		Cursor c = db.query(
    				DATABASE_TABLE,
    				new String[] {KEY_ID},
    				KEY_ID + " = " + cgms.getID(),
    				null,
    				null,
    				null,
    				null) ;
    		
    		try {
    			return c.getCount() > 0 ;		// is primary key, so have exactly 0 or 1.
    		} finally {
    			c.close() ;
    		}
    	}
    }
    
    private void insertCustomGameModeSettings( CustomGameModeSettings cgms ) {
    	synchronized( DATABASE_MUTEX ) {
    		ContentValues newValues = new ContentValues() ;
    		
    		// id and creation time
    		newValues.put(KEY_ID, cgms.getID()) ;
    		newValues.put(KEY_TIME_CREATED, System.currentTimeMillis()) ;
    		
    		// strings
    		newValues.put(KEY_NAME, cgms.getName()) ;
    		newValues.put(KEY_SUMMARY, cgms.getSummary()) ;
    		newValues.put(KEY_DESCRIPTION, cgms.getDescription()) ;
    		
    		// qpanes
    		newValues.put(KEY_QPANES, cgms.getNumberQPanes()) ;
    		
    		// pieces included
    		newValues.put(KEY_HAS_TRIONIMOS, cgms.getHasTrominoes()) ;
    		newValues.put(KEY_HAS_TETROMINOS, cgms.getHasTetrominoes()) ;
    		newValues.put(KEY_HAS_PENTOMINOS, cgms.getHasPentominoes()) ;
    		
    		// movement allowed
    		newValues.put(KEY_HAS_ROTATION, cgms.getHasRotation()) ;
    		newValues.put(KEY_HAS_REFLECTION, cgms.getHasReflection()) ;
    		
    		// dimensions
    		newValues.put(KEY_ROWS, cgms.getRows()) ;
    		newValues.put(KEY_COLS, cgms.getCols()) ;
    		
    		// multiplayer?
    		newValues.put(KEY_ALLOW_MULTIPLAYER, cgms.getAllowMultiplayer()) ;
    		
    		// INSERT!
    		db.insert(DATABASE_TABLE, null, newValues) ;
    	}
    }
    
    private void updateCustomGameModeSettings( CustomGameModeSettings cgms ) {
    	synchronized( DATABASE_MUTEX ) {
    		ContentValues newValues = new ContentValues() ;
    		
    		// the only content we don't update is id and creation time
    		
    		// strings
    		newValues.put(KEY_NAME, cgms.getName()) ;
    		newValues.put(KEY_SUMMARY, cgms.getSummary()) ;
    		newValues.put(KEY_DESCRIPTION, cgms.getDescription()) ;
    		
    		// qpanes
    		newValues.put(KEY_QPANES, cgms.getNumberQPanes()) ;
    		
    		// pieces included
    		newValues.put(KEY_HAS_TRIONIMOS, cgms.getHasTrominoes()) ;
    		newValues.put(KEY_HAS_TETROMINOS, cgms.getHasTetrominoes()) ;
    		newValues.put(KEY_HAS_PENTOMINOS, cgms.getHasPentominoes()) ;
    		
    		// movement allowed
    		newValues.put(KEY_HAS_ROTATION, cgms.getHasRotation()) ;
    		newValues.put(KEY_HAS_REFLECTION, cgms.getHasReflection()) ;
    		
    		// dimensions
    		newValues.put(KEY_ROWS, cgms.getRows()) ;
    		newValues.put(KEY_COLS, cgms.getCols()) ;
    		
    		// multiplayer?
    		newValues.put(KEY_ALLOW_MULTIPLAYER, cgms.getAllowMultiplayer()) ;
    		
    		// INSERT!
    		db.update(DATABASE_TABLE, newValues, KEY_ID + " = " + cgms.getID(), null) ;
    	}
    }
    
    
    /**
     * Provided with a Cursor pointing to a query of DATABASE_TABLE
     * holding all columns, allocates and returns a CustomGameModeSettings
     * object representing the content of the row.
     */
    private static CustomGameModeSettings readCustomGameModeSettingsFromCursor( Cursor c ) {
    	
    	int id = c.getInt( c.getColumnIndex( KEY_ID ) ) ;
    	CustomGameModeSettings.Builder builder = new CustomGameModeSettings.Builder( id ) ;
    	
    	// set text - name, desc., etc.
    	builder.setName( 		c.getString( c.getColumnIndex( KEY_NAME ) ) ) ;
    	builder.setSummary( 	c.getString( c.getColumnIndex( KEY_SUMMARY ) ) ) ;
    	builder.setDescription( c.getString( c.getColumnIndex( KEY_DESCRIPTION ) ) ) ;
    	
    	// qpanes
    	builder.setNumberQPanes( c.getInt( c.getColumnIndex( KEY_QPANES ) ) ) ;
    	
    	// set pieces contained herein
    	builder.setHasTrominoes( 	c.getInt( c.getColumnIndex( KEY_HAS_TRIONIMOS ) ) == 1 ) ;
    	builder.setHasTetrominoes( 	c.getInt( c.getColumnIndex( KEY_HAS_TETROMINOS ) ) == 1 ) ;
    	builder.setHasPentominoes( 	c.getInt( c.getColumnIndex( KEY_HAS_PENTOMINOS ) ) == 1 ) ;
    	
    	// set movement allowed
    	builder.setHasRotation( 	c.getInt( c.getColumnIndex( KEY_HAS_ROTATION ) ) == 1 ) ;
    	builder.setHasReflection( 	c.getInt( c.getColumnIndex( KEY_HAS_REFLECTION ) ) == 1 ) ;
    	
    	// set sizes
    	builder.setRows( c.getInt( c.getColumnIndex( KEY_ROWS ) ) ) ;
    	builder.setCols( c.getInt( c.getColumnIndex( KEY_COLS ) ) ) ;
    	
    	// set multiplayer
    	builder.setAllowMultiplayer( c.getInt( c.getColumnIndex( KEY_ALLOW_MULTIPLAYER ) ) == 1 ) ;
    	
    	return builder.build() ;
    }
	
}
