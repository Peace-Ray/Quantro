package com.peaceray.quantro.lobby;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;


/**
 * A log of important events in the lobby, including chat messages,
 * users going active / inactive, game launches, etc.  Unlike 'Lobby',
 * which holds a complete snapshot of the current "lobby state", the
 * LobbyLog holds an (incomplete) historical record of information.
 * 
 * When determining where something should go, a simple heuristic would
 * be to determine whether a particular piece of information is 
 * relevant to a new player who comes into the lobby immediately
 * after the event.  If relevant, put it in the Lobby.  If only relevant
 * to people who experienced it, put it in LobbyLog (some events should
 * be reflected in both, such as a player entering/leaving).
 * 
 * In keeping with the above, a Lobby retains information only regarding
 * the CURRENT players in the lobby.  For example, it has an array of
 * names for those players currently present, with only an implicit (by
 * playerSlot) association between those players and their actions,
 * e.g. votes.  By contrast, LobbyLog explicitly associates player
 * names with their actions, because historical events (player entering,
 * chat comments, etc.) cannot be reliably associated with current 
 * players (maybe a player said something and left, then a new player
 * filled their slot?).
 * 
 * The LobbyLog is comprised primarily of a fixed-length list (array) of
 * LobbyLog.Events.  Although we provide different ways of accessing the
 * data in these events, including direct access to the "private" fields
 * of those Events, you should consider them as read-only and only make
 * changes through the approved public interface for LobbyLog.
 * 
 * LobbyLog can be accessed by copying events, a "slice" of events, filtered
 * events, a filtered "slice", etc. into provided container arrays.  The
 * first event in a LobbyLog will always have id 0, with ids increasing
 * sequentially with subsequent events.  The first event will (almost) always
 * be a CONNECTED message.  The Log does NOT (at present) allow any callbacks
 * to a "Delegate" or "Listener"; whatever object makes additions to the Log
 * should also notify any objects using the log that an update should be 
 * read.
 * 
 * As mentioned, LobbyLog is fixed-length.  Once the log grows to a certain size,
 * the oldest Events will be removed to make room for new ones.  This happens
 * behind the scenes when new events are logged.  It is important to note how
 * this affects memory management and multithreaded access.
 * 
 * MEMORY MANAGEMENT: LobbyLogs are instantiated with a size limit, which is
 * 		LobbyLog.DEFAULT_CAPACITY by default.  Only this many Event objects will
 * 		be allocated to hold the lobby.  They will be allocated one at a time as-needed,
 * 		unless allocate() is called, in which case this will be done at once
 * 		and no subsequent event will cause an allocation.  Any new event beyond the
 * 		length of the Log will replace the oldest event in the Log.
 * 
 * 		LobbyLogs can be resized in-place.  Increasing the size will add space
 * 		for more Log events after the most recent (it will NOT restore events that
 * 		have been forgotten from history), while decreasing the size will cause
 * 		the log to forget the earliest events.  If the lobby size is increased,
 * 		call allocate() if desired to create enough Event objects to fill the
 * 		new size.
 * 
 * 		This direct memory access - explicit settings for size and an explicit call
 * 		to full allocate the Events - is meant to limit garbage collection when
 * 		not desired, and to set a consistent memory footprint for this object.
 * 
 * 
 * MULTITHREADED ACCESS: LobbyLog methods are synchronized, to prevent interference
 * 		between multiple threads.  If only one thread is perfoming reads and writes,
 * 		it is reasonable to use the "*Reference" methods; these provide references to
 * 		the Event objects used by this LobbyLog.  They are therefore quite fast, but
 * 		are dangerous to use, because LobbyLog mutators can then alter the objects returned
 * 		before they are used, and the caller of a "*Reference" method can alter the
 * 		contents of the Log by mutating the object.  Use of these references is thus
 * 		EXTREMELY dangerous, but the memory gains might be worth it.  Up to you.
 * 
 * 		For safer operation, especially in multithreaded settings (even duplex settings
 * 		where one thread adds events and another reads them), use the non-Reference
 * 		methods for access.  These copy Event content to other objects, which are
 * 		guaranteed not to be altered through the LobbyLog by another thread.
 * 
 * 
 * Subclassing: one easy method of subclassing is to override 'logEvent',
 * 		which is responsible for reading the contents of the provided
 * 		event and placing a copy of its data in our records.  This object
 * 		places events in the order they are logged, regardless of (e.g.)
 * 		the time set for each event; a subclass might want to organize by
 * 		time instead.
 * 
 * @author Jake
 *
 */
public class LobbyLog {
	
	public interface Delegate {
		
		/**
		 * This delegate method is called every time a new event is added 
		 * to the log ( if delegate is not null ).  It is called as the last
		 * operation of the add, so the LobbyLog is consistent with the state
		 * described in the parameters.
		 * 
		 * Take care not to 'log' anything in this delegate callback, since
		 * that might cause an infinite recursion.
		 * 
		 * If only one thread is writing to the LobbyLog (using log* methods)
		 * you can be assured that the contents of LobbyLog will not change during
		 * this method.
		 * 
		 * @param lobbyLog
		 * @param id
		 * @param type
		 */
		public void lld_newEvent( LobbyLog lobbyLog, int id, int type ) ;
	}
	
	public static final int DEFAULT_CAPACITY = 100 ;
	
	
	// our events
	protected ArrayList<Event> mEvents ;
	// metadata re: mEvents
	protected int mCapacity ;
	protected int mSize ;
	// a temporary structure for the event currently being logged.
	// should be set only in a log* method, and read only in logEvent().
	protected Event mCurrentEvent ;
	// Our delegate.
	WeakReference<Delegate> mwrDelegate ;
	
	
	/**
	 * Initializes a LobbyLog object with DEFAULT_CAPACITY.
	 * The first event will have id 0; they will increase
	 * sequentially from there.
	 */
	public LobbyLog() {
		this(DEFAULT_CAPACITY) ;
	}
	
	/**
	 * Initializes a LobbyLog object with the specified capacity.
	 * The first event will have id 0; they will increase
	 * sequentially from there.
	 * @param capacity The maximum number of events to retain.
	 */
	public LobbyLog( int capacity ) {
		if ( capacity <= 0 )
			throw new IllegalArgumentException("Must have a positive capacity; " + capacity + " is not positive") ;
		mEvents = new ArrayList<Event>(capacity) ;
		mCapacity = capacity ;
		mSize = 0 ;
		mCurrentEvent = new Event() ;
		mwrDelegate = new WeakReference<Delegate>(null) ;
	}
	
	
	/**
	 * Sets the capacity for this LobbyLog to the specified number.
	 * If greater than the previous capacity, all currently stored
	 * events will be retained.  If smaller, the earliest events may
	 * be lost.
	 * @param capacity 	The new capacity.
	 * @return
	 */
	synchronized public LobbyLog setCapacity( int capacity ) {
		if ( capacity <= 0 )
			throw new IllegalArgumentException("Must have a positive capacity; " + capacity + " is not positive") ;
		ArrayList<Event> e = new ArrayList<Event>(capacity) ;
		for ( int i = Math.max(0, mSize - capacity) ; i < mSize ; i++  ) {
			// consider 2 cases: sufficient space for all events,
			// and insufficient space.  If the new capacity can
			// hold all items, then capacity >= mSize and we begin
			// with i = 0, iterating through all Events.  If
			// the new capacity cannot hold all items, we iterate
			// through the slice [last-cap:last].
			e.add( mEvents.get(i) ) ;
		}
		mEvents = e ;
		mSize = Math.min(capacity, mSize) ;
		mCapacity = capacity ;
		
		return this ;
	}
	
	/**
	 * Immediately allocates any Event objects which may be needed,
	 * up to the current capacity.  New additional allocation will
	 * be performed by this Log as new events are added.
	 * @return
	 */
	synchronized public LobbyLog allocate() {
		while( mEvents.size() < mCapacity )
			mEvents.add( new Event() ) ;
		return this ;
	}
	
	synchronized public LobbyLog setDelegate( Delegate delegate ) {
		this.mwrDelegate = new WeakReference<Delegate>(delegate) ;
		return this ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// 
	// MUTATORS
	// 
	// Methods for logging events.  These methods return the 'id' of the 
	// event logged.
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	synchronized public int logConnected( int playerSlot, String name, int status, Object tag ) {
		mCurrentEvent.setAsConnected(nextId(), System.currentTimeMillis(), playerSlot, name, status) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ;
	}
	
	
	synchronized public int logConnected( long time, int playerSlot, String name, int status, Object tag ) {
		mCurrentEvent.setAsConnected(nextId(), time, playerSlot, name, status) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ;
	}
	
	
	synchronized public int logDisconnected( int playerSlot, String name, int status, Object tag ) {
		mCurrentEvent.setAsDisconnected(nextId(), System.currentTimeMillis(), playerSlot, name, status) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ; 
	}
	
	
	synchronized public int logDisconnected( long time, int playerSlot, String name, int status, Object tag ) {
		mCurrentEvent.setAsDisconnected(nextId(), time, playerSlot, name, status) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ; 
	}


	synchronized public int logPlayerJoined( int playerSlot, String name, int status, Object tag ) {
		mCurrentEvent.setAsPlayerJoined(nextId(), System.currentTimeMillis(), playerSlot, name, status) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ; 
	}
	
	
	synchronized public int logPlayerJoined( long time, int playerSlot, String name, int status, Object tag ) {
		mCurrentEvent.setAsPlayerJoined(nextId(), time, playerSlot, name, status) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ; 
	}
	
	
	synchronized public int logPlayerLeft( int playerSlot, String name, int status, Object tag ) {
		mCurrentEvent.setAsPlayerLeft(nextId(), System.currentTimeMillis(), playerSlot, name, status) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ; 
	}
	
	
	synchronized public int logPlayerLeft( long time, int playerSlot, String name, int status, Object tag ) {
		mCurrentEvent.setAsPlayerLeft(nextId(), time, playerSlot, name, status) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ; 
	}
	
	
	synchronized public int logPlayerKicked( int playerSlot, String name, String msg, int status, Object tag ) {
		mCurrentEvent.setAsPlayerKicked(nextId(), System.currentTimeMillis(), playerSlot, name, msg, status) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ; 
	}
	
	
	synchronized public int logPlayerKicked( long time, int playerSlot, String name, String msg, int status, Object tag ) {
		mCurrentEvent.setAsPlayerKicked(nextId(), time, playerSlot, name, msg, status) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ; 
	}
	
	
	synchronized public int logPlayerStatusChange( int playerSlot, String name, int newStatus, Object tag ) {
		mCurrentEvent.setAsPlayerStatusChange(nextId(), System.currentTimeMillis(), playerSlot, name, newStatus ) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ;
	}
	
	synchronized public int logPlayerStatusChange( long time, int playerSlot, String name, int newStatus, Object tag ) {
		mCurrentEvent.setAsPlayerStatusChange(nextId(), time, playerSlot, name, newStatus ) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ;
	}
	
	synchronized public int logPlayerStatusChange( int playerSlot, String name, int newStatus, int oldStatus, Object tag ) {
		mCurrentEvent.setAsPlayerStatusChange(nextId(), System.currentTimeMillis(), playerSlot, name, newStatus, oldStatus ) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ; 
	}
	
	synchronized public int logPlayerStatusChange( long time, int playerSlot, String name, int newStatus, int oldStatus, Object tag ) {
		mCurrentEvent.setAsPlayerStatusChange(nextId(), time, playerSlot, name, newStatus, oldStatus ) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ; 
	}
	
	synchronized public int logChat( int playerSlot, String name, String msg, int status, Object tag ) {
		mCurrentEvent.setAsChat(nextId(), System.currentTimeMillis(), playerSlot, name, msg) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ;
	}
	
	synchronized public int logChat( long time, int playerSlot, String name, String msg, int status, Object tag ) {
		mCurrentEvent.setAsChat(nextId(), time, playerSlot, name, msg) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ;
	}
	
	synchronized public int logLaunch( int hostSlot, String name, int gameMode, Object tag ) {
		mCurrentEvent.setAsLaunch(nextId(), System.currentTimeMillis(), hostSlot, name, gameMode) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ;
	}
	
	synchronized public int logLaunch( long time, int hostSlot, String name, int gameMode, Object tag ) {
		mCurrentEvent.setAsLaunch(nextId(), time, hostSlot, name, gameMode) ;
		mCurrentEvent.tag = tag ;
		this.logEvent( mCurrentEvent ) ;
		this.informDelegateOfNewEvent( mCurrentEvent ) ;
		return mCurrentEvent.id ;
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// Mutator helpers 
	
	
	protected int indexForId( int id ) {
		if ( mSize == 0 )
			return -1 ;
		int firstId = mEvents.get(0).id ;
		int index = id - firstId ;
		if ( index < 0 || index > mSize )
			return -1 ;
		
		return index ;
	}
	
	
	/**
	 * This method uses mCurrentEvent as a reference for content, placing
	 * an Event with the exact same data in the log.  This implementation
	 * of LobbyLog places events in order of ID number, which itself is
	 * serial with each call to log*; in other words, we simply place this
	 * event at the end.
	 * 
	 * @param e An event to place in our log.  NOTE: We should NOT retain a 
	 * reference to 'e' after this call; instead, its content should be
	 * copied.
	 */
	synchronized protected void logEvent( Event e ) {
		// If mEvents is not yet full to capacity, place this event at the end.
		if ( mEvents.size() < mCapacity )
			mEvents.add( e.clone() ) ;
		// If mEvents is full BUT mSize does not agree, we pre-allocated objects
		// and should copy the contents of e into the appropriate one.
		else if ( mEvents.size() > mSize )
			mEvents.get(mSize).set(e) ;
		// Otherwise, move the FIRST event to the end, setting its content as e.
		else
			mEvents.add( mEvents.remove(0).set(e) ) ;
			// note that event.set(e) returns a reference to 'event' itself.
		
		// Increase mSize (if appropriate)
		if ( mSize < mEvents.size() )
			mSize++ ;
	}
	
	
	synchronized protected void informDelegateOfNewEvent( Event e ) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null )
			delegate.lld_newEvent(this, e.id, e.type) ;
	}
	
	
	/**
	 * Returns the id number for the next event, defined as
	 * f(n) = f(n-1) + 1
	 * f(0) = 0.
	 * @return
	 */
	synchronized protected int nextId() {
		if ( mSize == 0 )
			return 0 ;
		return mEvents.get(mSize-1).id + 1 ;
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// 
	// ACCESSORS
	// 
	// Methods for accessing event logs.  Bare in mind the warning in the class
	// declaration comments regarding reference access vs. copy access, and the
	// dangers of multithreaded operation.
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	////////////////////////////////////////////////////////////////////////////
	// META ACCESSORS.  Data about this log.  Accurate at the time the method
	// is called, but multithreaded operation may invalidate the result immediately
	// after.  One safe assumption is that the size of the log will never decrease
	// (unless 'setCapacity' is called); another is that the result of calling
	// firstId() or lastId() will never decrease (without exception).
	
	/**
	 * The number of events in this log.
	 */
	synchronized public int size() {
		return mSize ;
	}
	
	/**
	 * The maximum capacity of this log.
	 * @return
	 */
	synchronized public int capacity() {
		return mCapacity ;
	}
	
	/**
	 * The 'id' number of the first event in this log.  -1 if the log is empty.
	 * @return
	 */
	synchronized public int firstId() {
		return mSize == 0 ? -1 : mEvents.get(0).id ;
	}
	
	
	/**
	 * The 'id' number of the LAST event in this log.  -1 if the log is
	 * empty.  Note the difference between the value returned by lastId(),
	 * the 'id' of the last Event, and 'endId' as a parameter in any
	 * Slice method, which is the id PAST the last item included in the
	 * slice.  To slice from 'id' to the last item, use
	 * 
	 * int num = lobbyLog.getEventSlice( events, id, lobbyLog.lastId() + 1 ) ;
	 * 
	 * @return
	 */
	synchronized public int lastId() {
		return mSize == 0 ? -1 : mEvents.get(mSize-1).id ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// TAG-ACCESS.  Returns a reference to the tag provided to the given object.
	synchronized public Object getTag( int id ) {
		int index = indexForId( id ) ;
		if ( index < 0 )
			return null ;
		
		return mEvents.get(index).tag ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// DATA-COPY METHODS.  These methods do not provide references to the 
	// Event objects stored by this object.  If event tags are used, the
	// tags of the Events returned will reference the same objects (as will
	// any Strings, but Strings are immutable).
	
	
	/**
	 * Returns a newly allocated Event object, representing the specified 'id'.
	 * Returns 'null' if the provided id does not reference an event in our log.
	 */
	synchronized public Event getEvent(int id) {
		int index = indexForId(id) ;
		return index < 0 ? null : mEvents.get(index).clone() ;
	}
	
	/**
	 * Sets the provided Event object to represent the 'id'th event.  Returns
	 * success.  If 'false', the specified id does not occur in our log and
	 * 'e' has not been changed.
	 * 
	 * @param e
	 * @param id
	 * @return
	 */
	synchronized public boolean getEvent(Event e, int id) {
		int index = indexForId(id) ;
		if ( index < 0 )
			return false ;
		e.set(mEvents.get(index)) ;
		return true ;
	}
	
	
	/**
	 * Copies all events currently in the log into the provided ArrayList.
	 * 
	 * events.get(i).set(e) is used, from i=0 up to the size of events,
	 * after which new Event objects will be allocated.
	 * 
	 * Returns the number of events captured in the slice; in other 
	 * words, the size of this log.
	 * 
	 * @param events
	 * @return
	 */
	synchronized public int getEvents( ArrayList<Event> events ) {
		return getEventSlice(events, 0, Integer.MAX_VALUE) ;
	}
	
	
	/**
	 * Sets the provided ArrayList to the indicated slice, beginning at startId
	 * and ending at the event just before endId (similar to a python list slice).
	 * 
	 * events.get(i).set(e) is used, from i=0 up to the size of events,
	 * after which new events will be allocated.
	 * 
	 * Returns the number of events captured in the slice, which ideally is
	 * endId - startId, but may be less (even zero) if the specified slice is
	 * not wholly contained in the log.
	 * 
	 * @param events
	 * @param startId
	 * @param endId
	 * @return
	 */
	synchronized public int getEventSlice(ArrayList<Event> events, int startId, int endId) {
		if ( mSize == 0 )
			return 0 ;
		int startIndex = Math.max( indexForId(startId), 0 ) ;
			// start from the specified id, or the first item if later.
		int endIndex = indexForId(endId) ;
		if ( endIndex == -1 )
			endIndex = mSize ;
		else
			endIndex = Math.min( endIndex, mSize ) ;
			// end at the specified id, or the last item if earlier.
		
		int num = 0 ;
		for ( int i = startIndex; i < endIndex; i++ ) {
			if ( events.size() > num )
				events.get(num).set( mEvents.get(i) ) ;
			else
				events.add( mEvents.get(i).clone() ) ;
			num++ ;
		}
		
		return num ;
	}
	
	
	/**
	 * Slices up to the provided id.  Equivalent to getEventSlice( events, 0, endId ).
	 * @param events
	 * @param endId
	 * @return
	 */
	synchronized public int getEventSliceTo( ArrayList<Event> events, int endId ) {
		return getEventSlice( events, 0, endId ) ;
	}
	
	/**
	 * Slices from the provided id to the last event.  Equivalent to 
	 * getEventSlice( events, startId, Integer.MAX_VALUE ) ;
	 * @param events
	 * @param startId
	 * @return
	 */
	synchronized public int getEventSliceFrom( ArrayList<Event> events, int startId ) {
		return getEventSlice( events, startId, Integer.MAX_VALUE ) ;
	}
	
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// REFERENCE METHODS.  These methods provide references to the actual Event
	// data stored in the LobbyLog.  As such, they are extremely dangerous to
	// use, but may be slightly faster.  Only use them is situations where
	// LobbyLog access is limited to a single thread, and the references retrieved
	// are to be used only up to the next method mutator call on LobbyLog.
	
	// TODO: Since the above scenario doesn't seem to be the way we'll use LobbyLog,
	// I have left implementation as an exercise.
	
	

	public static class Event {
		
		// event type?
		public static final int CONNECTED 		= 0 ;
		public static final int DISCONNECTED	= 1 ;
		public static final int PLAYER_JOINED 	= 2 ;
		public static final int PLAYER_LEFT 	= 3 ;
		public static final int PLAYER_KICKED 	= 4 ;
		public static final int PLAYER_STATUS_CHANGE = 5 ;
		public static final int CHAT			= 6 ;			// TEXT is the message.
		public static final int LAUNCH 			= 7 ;			
		
		
		/**
		 * Returns a human-readable, CamelCase name for this event type.
		 * @return
		 */
		public String eventTypeName() {
			return eventTypeName( type ) ;
		}
		
		public static String eventTypeName( int type ) {
			switch( type ) {
			case CONNECTED:
				return "Connected" ;
			case DISCONNECTED:
				return "Disconnected" ;
			case PLAYER_JOINED:
				return "PlayerJoined" ;
			case PLAYER_LEFT:
				return "PlayerLeft" ;
			case PLAYER_KICKED:
				return "PlayerKicked" ;
			case PLAYER_STATUS_CHANGE:
				return "PlayerStatusChange" ;
			case CHAT:
				return "Chat" ;
			case LAUNCH:
				return "Launch" ;
			default:
				return "Unknown" ;
			}
		}
		
		
		// publically accessible fields.  DON'T CHANGE THESE!
		// USE THEM FOR READ-ONLY ONLY!
		public int id ;
		public long time ;
		public int type ;
		public int slot ;
		public String name ;
		public String text ;
		public int status ;		// the player's status, as Lobby.PLAYER_STATUS_*, at the time of this event.  For PLAYER_STATUS_CHANGE, this is the new status.
		public int arg1 ;
		public int arg2 ;
		public Object tag ;
		
		

		
		public Event() {
			set( 0, System.currentTimeMillis(), 0, 0, null, null, Lobby.PLAYER_STATUS_UNUSED, 0, 0, null ) ;
		}
		
		public Event( int id, long curTimeMillis, int type, int playerSlot, String name, String text, int status, int arg1, int arg2, Object obj ) {
			set( id, curTimeMillis, type, playerSlot, name, text, status, arg1, arg2, obj ) ;
		}
		
		@Override
		public Event clone() {
			return new Event( id, time, type, slot, name, text, status, arg1, arg2, tag ) ;
		}
		
		public Event set(Event e) {
			return set( e.id, e.time, e.type, e.slot, e.name, e.text, e.status, e.arg1, e.arg2, e.tag ) ;
		}
		
		public Event set( int id, long curTimeMillis, int type, int playerSlot, String name, String text, int status, int arg1, int arg2, Object tag ) {
			this.id = id ;
			this.time = curTimeMillis ;
			this.type = type ;
			this.slot = playerSlot ;
			this.name = name == null ? "" : name ;
			this.text = text == null ? "" : text ;
			this.status = status ;
			this.arg1 = arg1 ;
			this.arg2 = arg2 ;
			this.tag = tag ;
			
			return this ;
		}
		
		public Event setAsConnected( int id, long curTimeMillis, int playerSlot, String name, int status ) {
			return this.set(id, curTimeMillis, CONNECTED, playerSlot, name, null, status, 0, 0, null) ;
		}
		
		public Event setAsDisconnected( int id, long curTimeMillis, int playerSlot, String name, int status ) {
			return this.set(id, curTimeMillis, CONNECTED, playerSlot, name, null, status, 0, 0, null) ;
		}
		
		public Event setAsPlayerJoined( int id, long curTimeMillis, int playerSlot, String name ) {
			return this.setAsPlayerJoined(id, curTimeMillis, playerSlot, name, Lobby.PLAYER_STATUS_UNUSED ) ;
		}
		
		public Event setAsPlayerJoined( int id, long curTimeMillis, int playerSlot, String name, int joinedAsStatus ) {
			return set( id, curTimeMillis, PLAYER_JOINED, playerSlot, name, null, joinedAsStatus, 0, 0, null ) ;
		}
		
		public Event setAsPlayerLeft( int id, long curTimeMillis, int playerSlot, String name ) {
			return setAsPlayerLeft( id, curTimeMillis, playerSlot, name, Lobby.PLAYER_STATUS_UNUSED ) ;
		}
		
		public Event setAsPlayerLeft( int id, long curTimeMillis, int playerSlot, String name, int leftWithStatus ) {
			// almost the same as playerJoined
			this.setAsPlayerJoined( id, curTimeMillis, playerSlot, name, leftWithStatus ) ;
			this.type = PLAYER_LEFT ;
			return this ;
		}
		
		public Event setAsPlayerKicked( int id, long curTimeMillis, int playerSlot, String name, String msg ) {
			return this.setAsPlayerKicked(id, curTimeMillis, playerSlot, name, msg, Lobby.PLAYER_STATUS_UNUSED) ;
		}
		
		public Event setAsPlayerKicked( int id, long curTimeMillis, int playerSlot, String name, String msg, int kickedWithStatus ) {
			return set(id, curTimeMillis, PLAYER_KICKED, playerSlot, name, msg, kickedWithStatus, 0, 0, null ) ;
		}
		
		public Event setAsPlayerStatusChange( int id, long curTimeMillis, int playerSlot, String name, int newStatus ) {
			return set(id, curTimeMillis, PLAYER_STATUS_CHANGE, playerSlot, name, null, newStatus, Lobby.PLAYER_STATUS_UNUSED, 0, null ) ;
		}
		
		public Event setAsPlayerStatusChange( int id, long curTimeMillis, int playerSlot, String name, int prevStatus, int newStatus ) {
			return set(id, curTimeMillis, PLAYER_STATUS_CHANGE, playerSlot, name, null, newStatus, prevStatus, 0, null ) ;
		}
		
		public Event setAsChat( int id, long curTimeMillis, int playerSlot, String name, String msg ) {
			return set(id, curTimeMillis, CHAT, playerSlot, name, msg, Lobby.PLAYER_STATUS_ACTIVE, 0, 0, null ) ;
		}
		
		public Event setAsChat( int id, long curTimeMillis, int playerSlot, String name, String msg, int status ) {
			return set(id, curTimeMillis, CHAT, playerSlot, name, msg, status, 0, 0, null ) ;
		}
		
		public Event setAsLaunch( int id, long curTimeMillis, int hostSlot, String name, int gameMode ) {
			return set(id, curTimeMillis, LAUNCH, hostSlot, name, null, Lobby.PLAYER_STATUS_UNUSED, gameMode, 0, null ) ;
		}
		
		public long age() {
			return age( System.currentTimeMillis() ) ;
		}
		
		public long age( long curTime ) {
			return curTime - this.time ;
		}
		
		
		/**
		 * Represents this event as a string.  WARNING: this method allocates
		 * new data.  Only call this if you're not concerned with triggering the 
		 * Garbage Collector.
		 */
		public String toString() {
			StringBuilder res = new StringBuilder() ;
			res.append("Event " + id + "\t") ;
			res.append("at " + new Date(time) + "\t") ;
			switch( type ) {
			case CONNECTED:
				res.append("CONNECTED\t") ;
				break ;
			case DISCONNECTED:
				res.append("DISCONNECTED\t") ;
				break ;
			case PLAYER_JOINED:
				res.append("PLAYER_JOINED\t") ;
				break ;
			case PLAYER_LEFT:
				res.append("PLAYER_LEFT\t") ;
				break ;
			case PLAYER_KICKED:
				res.append("PLAYER_KICKED\t") ;
				break ;
			case PLAYER_STATUS_CHANGE:
				res.append("PLAYER_STATUS_CHANGE\t") ;
				break ;
			case CHAT:
				res.append("CHAT\t") ;
				break ;
			case LAUNCH:
				res.append("LAUNCH\t") ;
				break ;
			}
			
			res.append(slot + ", " + name + ", " + text + ", " + status + ", " + arg1 + ", " + arg2 + ", " + tag) ;
			return res.toString() ;
		}
		
	}
	
}
