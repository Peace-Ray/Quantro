package com.peaceray.quantro.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.WebConsts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * @author Jake
 * Special thanks to Wei-Meng Lee for their Android Database tutorial.
 * http://www.devx.com/wireless/Article/40842
 */
public class InternetLobbyDatabaseAdapter {
	private static final Object DATABASE_MUTEX = new Object() ;
	
	private static class KEY {
		private static final String ROWID = "_id";
		private static final String NONCE = "nonce" ;
		private static final String TIME_RECEIVED = "time_received" ;

		// some cached info about the lobby itself.
		private static final String STATUS = "status" ;
		private static final String TIME_CREATED = "time_created" ;
		private static final String HOST_NAME = "host_name" ;
		private static final String NAME = "name" ;
		private static final String DESCRIPTION = "description" ;
		private static final String SIZE = "size" ;
		private static final String PUBLIC = "public" ;
		private static final String ITINERANT = "itinerant" ;
		private static final String ORIGIN = "origin" ;
	}
    
    private static final String TAG = "ILDatabaseAdapter";
    
    private static final String DATABASE_NAME = "quantro_internet_lobbies";
    private static final String DATABASE_TABLE = "lobby" ;
    
    private static final int DATABASE_VERSION = 4 ;
    // 1: Initial version
    // 2: Adds 'public' and 'itinerant' as required fields
    // 3: Removes the "not null" requirement for certain settings (time_created, host_name, name, description)
    // 4: Adds Origin and Size.
    
    private static final String DATABASE_CREATE =
        "create table " + DATABASE_TABLE + " ("
		+ KEY.ROWID + " integer primary key autoincrement, "
        + KEY.NONCE + " text not null, "
        + KEY.TIME_RECEIVED + " bigint not null, "
        + KEY.STATUS + " tinyint not null, "
        + KEY.TIME_CREATED + " bigint, "
        + KEY.HOST_NAME + " string, "
        + KEY.NAME + " string, "
        + KEY.DESCRIPTION + " string, "
        + KEY.SIZE + " tinyint not null, "
        + KEY.PUBLIC + " boolean not null, "
        + KEY.ITINERANT + " boolean not null, "
        + KEY.ORIGIN + " tinyint not null "
		+ ")" ;
        
    private final Context context;
    private final DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public InternetLobbyDatabaseAdapter(Context ctx) {
    	synchronized( DATABASE_MUTEX ) {
	        this.context = ctx;
	        DBHelper = new DatabaseHelper(context);
    	}
    }
        
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	Log.w(TAG, "onUpgrade " + DATABASE_NAME + " " + oldVersion + " to " + newVersion);
            clearDatabase( db ) ;
        }
        
        public void clearDatabase(SQLiteDatabase db) {
        	Log.w(TAG, "clearDatabase " + DATABASE_NAME + " -- will delete all data");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }    

    public InternetLobbyDatabaseAdapter open() throws SQLException {
    	synchronized( DATABASE_MUTEX ) {
	        db = DBHelper.getWritableDatabase();
	        return this;
    	}
    }

    public void close() {
    	synchronized( DATABASE_MUTEX ) {
    		DBHelper.close();
    		db = null;
    	}
    }
    

    public long insertLobby( InternetLobby lobby ) {
    	synchronized( DATABASE_MUTEX ) {
	    	if ( hasLobby(lobby) )
	    		throw new IllegalArgumentException("Cannot insert a lobby already in the database; use 'insertLobby'") ;
	    	
	    	ContentValues initialValues = new ContentValues() ;
	    	initialValues.put(KEY.NONCE, lobby.getLobbyNonce().toString()) ;
	    	initialValues.put(KEY.TIME_RECEIVED, System.currentTimeMillis()) ;
	    	initialValues.put(KEY.STATUS, lobby.getStatus()) ;
	    	initialValues.put(KEY.TIME_CREATED, lobby.getAge() == -1 ? -1 : System.currentTimeMillis() - lobby.getAge()) ;
	    	initialValues.put(KEY.HOST_NAME, lobby.getHostName()) ;
	    	initialValues.put(KEY.NAME, lobby.getLobbyName()) ;
	    	initialValues.put(KEY.DESCRIPTION, lobby.getDescription()) ;
	    	initialValues.put(KEY.SIZE, lobby.getMaxPeople()) ;
	    	initialValues.put(KEY.PUBLIC, lobby.isPublic()) ;
	    	initialValues.put(KEY.ITINERANT, lobby.isItinerant()) ;
	    	initialValues.put(KEY.ORIGIN, lobby.getOrigin()) ;
	    	
	    	return db.insert(DATABASE_TABLE, null, initialValues) ;
    	}
    }
    
    
    public void deleteDatabase(  ) {
    	synchronized ( DATABASE_MUTEX ) {
    		DBHelper.clearDatabase(db) ;
    	}
    }
    
    public boolean deleteLobby( InternetLobby lobby ) {
    	synchronized( DATABASE_MUTEX ) {
	    	return db.delete(DATABASE_TABLE, KEY.NONCE + 
	        		"=" + "'" + lobby.getLobbyNonce() + "'", null) > 0;
    	}
    }
    
    
    public int deleteClosedAndNonItinerantLobbiesCreatedBefore( Date date ) {
    	synchronized( DATABASE_MUTEX ) {
	    	return db.delete( DATABASE_TABLE, KEY.TIME_CREATED +
	    			"<=" + date.getTime() + " AND " + KEY.ITINERANT + " = 0 "
	    				+ " AND ( " + KEY.STATUS + " = " + WebConsts.STATUS_CLOSED + " OR " + KEY.STATUS + " = " + WebConsts.STATUS_REMOVED + ")", null ) ;
    	}
    }
    
    public int deleteClosedAndNonItinerantLobbiesCreatedAfter( Date date ) {
    	synchronized( DATABASE_MUTEX ) {
	    	return db.delete( DATABASE_TABLE, KEY.TIME_CREATED +
	    			">=" + date.getTime() + " AND " + KEY.ITINERANT + " = 0 "
	    				+ " AND ( " + KEY.STATUS + " = " + WebConsts.STATUS_CLOSED + " OR " + KEY.STATUS + " = " + WebConsts.STATUS_REMOVED + ")", null ) ;
    	}
    }
    
    public int deleteClosedAndNonItinerantLobbiesReceivedBefore( Date date ) {
    	synchronized( DATABASE_MUTEX ) {
	    	return db.delete( DATABASE_TABLE, KEY.TIME_RECEIVED +
	    			"<=" + date.getTime() + " AND " + KEY.ITINERANT + " = 0 "
	    				+ " AND ( " + KEY.STATUS + " = " + WebConsts.STATUS_CLOSED + " OR " + KEY.STATUS + " = " + WebConsts.STATUS_REMOVED + ")", null ) ;
    	}
    }
    
    public int deleteClosedAndNonItinerantLobbiesReceivedAfter( Date date ) {
    	synchronized( DATABASE_MUTEX ) {
	    	return db.delete( DATABASE_TABLE, KEY.TIME_RECEIVED +
	    			">=" + date.getTime() + " AND " + KEY.ITINERANT + " = 0 "
	    				+ " AND ( " + KEY.STATUS + " = " + WebConsts.STATUS_CLOSED + " OR " + KEY.STATUS + " = " + WebConsts.STATUS_REMOVED + ")", null ) ;
    	}
    }
    
    
    public ArrayList<InternetLobby> getAllLobbiesWithStatus( int status ) throws IOException {
    	synchronized( DATABASE_MUTEX ) {
    		return getAllLobbiesWithStatus( new int []{ status } ) ;
    	}
    }
    
    public ArrayList<InternetLobby> getAllLobbiesWithStatus( int [] statuses ) throws IOException {
    	synchronized( DATABASE_MUTEX ) {
	    	String WHERE_CLAUSE = null ;
	    	// The SQL Equals operator is =
	    	if ( statuses != null && statuses.length >= 1 ) {
	    		WHERE_CLAUSE = KEY.STATUS + " = " + statuses[0] ;
	    		for ( int i = 1; i < statuses.length; i++ )
	    			WHERE_CLAUSE = WHERE_CLAUSE + " OR " + KEY.STATUS + " = " + statuses[i] ;
	    	}
	    	Cursor cursor = db.query(DATABASE_TABLE, 
		    		selectLobbyStringArray(),
			    	WHERE_CLAUSE,
			    	null,
			    	null,
			    	null,
			    	KEY.TIME_RECEIVED) ;
	    	
	    	try {
		    	ArrayList<InternetLobby> lobbies = new ArrayList<InternetLobby>() ;
		    	cursor.moveToFirst() ;
		    	while( !cursor.isAfterLast() ) {
		    		lobbies.add( readLobbyFromCursor( cursor ) ) ;
		    		cursor.moveToNext() ;
		    	}
		    	
		    	return lobbies ;
	    	} finally {
	    		cursor.close() ;
	    	}
    	}
    }
    
    
    public ArrayList<InternetLobby> getAllLobbiesWithoutStatus( int status ) throws IOException {
    	synchronized( DATABASE_MUTEX ) {
    		return getAllLobbiesWithoutStatus( new int []{ status } ) ;
    	}
    }
    
    public ArrayList<InternetLobby> getAllLobbiesWithoutStatus( int [] statuses ) throws IOException {
    	synchronized( DATABASE_MUTEX ) {
	    	String WHERE_CLAUSE = null ;
	    	// The SQL Equals operator is =
	    	if ( statuses != null && statuses.length >= 1 ) {
	    		WHERE_CLAUSE = KEY.STATUS + " <> " + statuses[0] ;
	    		for ( int i = 1; i < statuses.length; i++ )
	    			WHERE_CLAUSE = WHERE_CLAUSE + " AND " + KEY.STATUS + " <> " + statuses[i] ;
	    	}
	    	Cursor cursor = db.query(DATABASE_TABLE, 
		    		selectLobbyStringArray(),
			    	WHERE_CLAUSE,
			    	null,
			    	null,
			    	null,
			    	KEY.TIME_RECEIVED) ;
	    	
	    	try {
		    	ArrayList<InternetLobby> lobbies = new ArrayList<InternetLobby>() ;
		    	cursor.moveToFirst() ;
		    	while( !cursor.isAfterLast() ) {
		    		lobbies.add( readLobbyFromCursor( cursor ) ) ;
		    		cursor.moveToNext() ;
		    	}
		    	
		    	return lobbies ;
	    	} finally {
	    		cursor.close() ;
	    	}
    	}
    }
    
    
    public static ArrayList<InternetLobby> getAllActive( Context context ) throws IOException {
		InternetLobbyDatabaseAdapter dba = new InternetLobbyDatabaseAdapter(context) ;
		dba.open() ;
		ArrayList<InternetLobby> al = dba.getAllActiveLobbies() ;
		dba.close() ;
		return al ;
    }
    
    
    /**
     * Retrieves all "active" lobbies.  An active lobby is one we have not verified
     * to be or removed; e.g., its status could be NONE (not yet retrieved) or OPEN.
     * Additionally, ITINERANT lobbies, which have the special feature that they can
     * be re-opened once closed, will be retrieved if they have the possibility
     * of re-opening even if we've stored them with status CLOSED or REMOVED.
     * 
     * @return
     * @throws IOException
     */
    public ArrayList<InternetLobby> getAllActiveLobbies() throws IOException {
    	ArrayList<InternetLobby> lobbies = getAllLobbies() ;
    	ArrayList<InternetLobby> active = new ArrayList<InternetLobby>() ;
    	Iterator<InternetLobby> iter = lobbies.iterator() ;
    	for ( ; iter.hasNext() ; ) {
    		InternetLobby lobby = iter.next() ;
    		int status = lobby.getStatus() ;
    		if ( status == WebConsts.STATUS_EMPTY || status == WebConsts.STATUS_OPEN || lobby.isItinerant() )
    			active.add(lobby) ;
    	}
    	
    	return active ;
    }
    
    
    
    public ArrayList<InternetLobby> getAllLobbies() throws IOException {
    	return getAllLobbiesWithoutStatus( new int[0] ) ;
    }
    
    public InternetLobby getLobby( Nonce nonce ) throws IOException {
    	synchronized( DATABASE_MUTEX ) {
	    	Cursor cursor = db.query(DATABASE_TABLE, selectLobbyStringArray(),
		    	KEY.NONCE + "=" + "'" + nonce.toString() + "'",
		    	null,
		    	null,
		    	null,
		    	null) ;
	    	
	    	try {
		    	if ( cursor.getCount() == 0 )
		    		return null ;
		    	if ( cursor.getCount() > 1 ) {
		    		throw new IllegalStateException("Lobby database has multiple lobbies with the same nonce!") ;
		    	}
		    	
		    	cursor.moveToFirst() ;
		    	// Load this lobby.
		    	return readLobbyFromCursor( cursor ) ;
	    	} finally {
	    		cursor.close() ;
	    	}
    	}
    }
    
    
    public boolean updateLobby(InternetLobby lobby) {
    	synchronized( DATABASE_MUTEX ) {
	        ContentValues args = new ContentValues();
	        if ( lobby.getStatus() != WebConsts.STATUS_EMPTY )
	        	args.put(KEY.STATUS, lobby.getStatus()) ;
	        if ( lobby.getAge() > -1 )
	        	args.put(KEY.TIME_CREATED, System.currentTimeMillis() - lobby.getAge()) ;
	        if ( lobby.getHostName() != null )
	        	args.put(KEY.HOST_NAME, lobby.getHostName()) ;
	        if ( lobby.getLobbyName() != null )
	        	args.put(KEY.NAME, lobby.getLobbyName()) ;
	        if ( lobby.getDescription() != null )
	        	args.put(KEY.DESCRIPTION, lobby.getDescription()) ;
	        args.put(KEY.SIZE, lobby.getMaxPeople()) ;
	    	args.put(KEY.PUBLIC, lobby.isPublic()) ;
	    	args.put(KEY.ITINERANT, lobby.isItinerant()) ;
	    	args.put(KEY.ORIGIN, lobby.getOrigin()) ;
	    	// note: although we expect Name, TimeCreated and Description to be unchanged even after
	    	// an update, the initial insertion may not have KNOWN the correct
	    	// time.  However, the nonce and time received are guaranteed to never change
	    	// after the initial insertion.
	    	
	        return db.update(DATABASE_TABLE, args, 
	                         KEY.NONCE + "=" + "'" + lobby.getLobbyNonce().toString() + "'", null) > 0;
    	}
    }
    
    public boolean updateLobbyStatus( Nonce nonce, int status ) {
    	synchronized( DATABASE_MUTEX ) {
	    	ContentValues args = new ContentValues();
	    	args.put(KEY.STATUS, status) ;
	    	
	    	return db.update(DATABASE_TABLE, args, 
	                KEY.NONCE + "=" + "'" + nonce.toString() + "'", null) > 0;
    	}
    }
    
    public boolean hasLobby( InternetLobby lobby ) {
    	return hasLobby( lobby.getLobbyNonce() ) ;
    }
    
    public boolean hasLobby( Nonce nonce ) {
    	synchronized( DATABASE_MUTEX ) {
	    	Cursor cursor =
	            db.query(true, DATABASE_TABLE, new String[] {
	        			KEY.ROWID
	    	    	},
	            KEY.NONCE + "=" + "'" + nonce.toString() + "'", 
	            null,
	            null, 
	            null, 
	            null, 
	            null);
	    	
	    	try {
	    		return cursor.getCount() > 0 ;
	    	} finally {
	    		cursor.close() ;
	    	}
    	}
    }
    
    
    //---count lobbies---
    public int numLobbies() {
    	synchronized( DATABASE_MUTEX ) {
    		return numLobbiesWithoutStatus( new int[0] ) ;
    	}
    }
    
    public int numLobbiesWithStatus( int status ) {
    	synchronized( DATABASE_MUTEX ) {
    		return numLobbiesWithStatus( new int[]{status} ) ;
    	}
    }
    
    public int numLobbiesWithStatus( int [] omittedStatuses ) {
    	synchronized( DATABASE_MUTEX ) {
    		String WHERE_CLAUSE = null ;
	    	// The SQL DNE operator is <>
	    	if ( omittedStatuses != null && omittedStatuses.length >= 1 ) {
	    		WHERE_CLAUSE = KEY.STATUS + " = " + omittedStatuses[0] ;
	    		for ( int i = 1; i < omittedStatuses.length; i++ )
	    			WHERE_CLAUSE = WHERE_CLAUSE + " OR " + KEY.STATUS + " = " + omittedStatuses[i] ;
	    	}
	    	Cursor cursor = db.query(DATABASE_TABLE, new String[] {
	    			KEY.NONCE,
		    	},
		    	WHERE_CLAUSE,
		    	null,
		    	null,
		    	null,
		    	null) ;
	    	
	    	try {
	    		return cursor.getCount();
	    	} finally {
	    		cursor.close() ;
	    	}
    	}
    }
    
    public int numLobbiesWithoutStatus( int status ) {
    	synchronized( DATABASE_MUTEX ) {
    		return numLobbiesWithoutStatus( new int[]{status} ) ;
    	}
    }
    
    public int numLobbiesWithoutStatus( int [] omittedStatuses ) {
    	synchronized( DATABASE_MUTEX ) {
    		String WHERE_CLAUSE = null ;
	    	// The SQL DNE operator is <>
	    	if ( omittedStatuses != null && omittedStatuses.length >= 1 ) {
	    		WHERE_CLAUSE = KEY.STATUS + " <> " + omittedStatuses[0] ;
	    		for ( int i = 1; i < omittedStatuses.length; i++ )
	    			WHERE_CLAUSE = WHERE_CLAUSE + " AND " + KEY.STATUS + " <> " + omittedStatuses[i] ;
	    	}
	    	Cursor cursor = db.query(DATABASE_TABLE, new String[] {
	    			KEY.NONCE,
		    	},
		    	WHERE_CLAUSE,
		    	null,
		    	null,
		    	null,
		    	null) ;
	    	
	    	try {
	    		return cursor.getCount();
	    	} finally {
	    		cursor.close() ;
	    	}
    	}
    }
    
    
    private String [] selectLobbyStringArray() {
    	return new String[] {
    			KEY.ROWID,				// 0
    			KEY.NONCE,				// 1
    			KEY.TIME_RECEIVED,		// 2
    			KEY.STATUS,				// 3
    			KEY.TIME_CREATED,		// 4
    			KEY.HOST_NAME,			// 5
    			KEY.NAME,				// 6
    			KEY.DESCRIPTION,		// 7
    			KEY.SIZE,				// 8
    			KEY.PUBLIC,				// 9
    			KEY.ITINERANT,			// 10
    			KEY.ORIGIN,				// 11
	    	} ;
    }
    
    
    /**
     * Given a cursor positioned at a lobby row, returns an unrefreshed InternetLobby
     * instance.
     * 
     * Caller is responsible for positioning the cursor before the call, and advancing
     * it after.
     * 
     * Throws an exception upon failure.
     * 
     * @param cursor
     * @return
     * @throws IOException 
     */
    private InternetLobby readLobbyFromCursor( Cursor cursor ) throws IOException {
    	// Load this lobby.
		Nonce nonce = new Nonce( cursor.getString(1) ) ;
		long received = cursor.getLong(2) ;
		int status = cursor.getInt(3) ;
		long created = cursor.getLong(4) ;
		String host = cursor.getString(5) ;
		String name = cursor.getString(6) ;
		String description = cursor.getString(7) ;
		int size = cursor.getInt(8) ;
		boolean isPublic = cursor.getInt(9) == 1 ;
		boolean isItinerant = cursor.getInt(10) == 1 ;
		int origin = cursor.getInt(11) ;
		
		
		// Construct an unrefreshed InternetLobby object.
		InternetLobby il = InternetLobby.newUnrefreshedInstance(
				nonce, status, created, host, name, description, isPublic, isItinerant, origin) ;
		il.setMaxPlayers(size) ;
		return il ;
    }
}
