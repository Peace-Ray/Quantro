package com.peaceray.quantro.lobby;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.peaceray.quantro.communications.nonce.Nonce;

/**
 * A lobby client is basically an information store.  It holds the
 * current status of the lobby - or at least, the status as set by
 * a controller.  It also provides some useful views into the data,
 * such as "names voting for," etc.
 * 
 * Apart from allowing convenient access to information in different
 * formats, this class allows a single user (the player on this device)
 * privileged status in terms of voting.  If a vote is set or unset on
 * this object, it will be reported as such regardless of incoming
 * messages; this allows any View to see immediate results from
 * voting, rather than having to wait for updates that the vote
 * was counted.
 * 
 * @author Jake
 *
 */
public class Lobby implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1683892841044388120L;

	/**
	 * Changes to the Lobby will result in calls to the delegate.
	 * LobbyClients may be changed for all kinds of reasons - a LobbyView
	 * might perform an update, e.g. when the user touches a button, or from
	 * a LobbyClientConnection, e.g. when new information comes in.
	 * 
	 * @author Jake
	 *
	 */
	public interface Delegate {
		
		/**
		 * Called when a player joins the lobby.
		 * 
		 * @param slot The slot for the new player.
		 * @param name The name of the player who just joined, provided as a convenience.
		 * @param lobby
		 */
		public void ld_memberJoinedLobby( int slot, String name, Lobby lobby ) ;
		
		
		/**
		 * Called when the specified player leaves the lobby.
		 * 
		 * @param slot The slot the player just left.
		 * @param name The name of the player who left the lobby, provided as a convenience.
		 * @param lobby
		 */
		public void ld_memberLeftLobby( int slot, String name, Lobby lobby ) ;
		
		
		/**
		 * Called when the specified player changes their name.
		 * This method is NOT called when a player joins.
		 * 
		 * @param slot The slot of the player whose name changed.
		 * @param oldName The old player name.
		 * @param newName The new name for the player.
		 * @param lobby
		 */
		public void ld_memberChangedName( int slot, String oldName, String newName, Lobby lobby ) ;
		
		
		/**
		 * The member changed status.  This is a forward-method, since we don't have
		 * status yet, but (for e.g.) a player pressing "Home" should put them in
		 * a particular status, such as "Idle" or something.
		 * 
		 * @param slot
		 * @param oldStatus
		 * @param newStatus
		 * @param lobby
		 */
		public void ld_memberChangedStatus( int slot, int oldStatus, int newStatus, Lobby lobby ) ;
		
		
		/**
		 * A new message!
		 * @param fromSlot The slot of the player who sent the message.
		 * @param msg The text of the message itself.
		 * @param lobby The Lobby object.
		 */
		public void ld_newMessage( int fromSlot, String msg, Lobby lobby ) ;
		
		
		/**
		 * The available game modes have changed.
		 * 
		 * @param lobby
		 */
		public void ld_updatedGameModes( Lobby lobby ) ;
		
		
		/**
		 * The votes for the specified game mode have changed.
		 * 
		 * @param mode
		 * @param lobby
		 */
		public void ld_updatedGameModeVotes( int mode, Lobby lobby ) ;

		/**
		 * We have a new countdown.
		 * 
		 * @param countdownNumber
		 * @param gameMode
		 * @param lobby
		 */
		public void ld_newCountdown( int countdownNumber, int gameMode, Lobby lobby ) ;
		
		/**
		 * The specified countdown has updated.
		 * @param countdownNumber
		 * @param lobby
		 */
		public void ld_updatedCountdown( int countdownNumber, int gameMode, Lobby lobby ) ;
		
		
		/**
		 * We are REMOVING the specified countdown, but it has not yet been removed.
		 * The countdown may thus be accessed DURING this delegate call, but not
		 * afterwards.
		 * 
		 * @param countdownNumber
		 * @param gameMode
		 * @param lobby
		 * @param failure: we are removing this countdown specifically 
		 * 		because a launch attempt failed.
		 */
		public void ld_removingCountdown( int countdownNumber, int gameMode, Lobby lobby, boolean failure ) ;
	}
	
	
	// Player status!
	public static final int PLAYER_STATUS_NOT_CONNECTED = 0 ;
	public static final int PLAYER_STATUS_ACTIVE = 1 ;
	public static final int PLAYER_STATUS_INACTIVE = 2 ;
	public static final int PLAYER_STATUS_IN_GAME = 3 ;
	
	public static final int NUM_PLAYER_STATUSES = 4 ;
	
	public static final int PLAYER_STATUS_UNUSED = -1 ;
	
	// Countdown status!
	public static final int COUNTDOWN_STATUS_NONE = 0 ;			// no countdown in this slot
	public static final int COUNTDOWN_STATUS_ACTIVE = 1 ;		// currently counting
	public static final int COUNTDOWN_STATUS_HALTED = 2 ;		// we should still consider this a countdown,
																// but it's currently paused because one or
																// more players involved is in the
																// "inactive" state.  ASSUMPTIONS:
																// this countdown will be restarted from
																// scratch, or canceled.  It will NOT
																// resume from its current ETA.
	
	public static final int COUNTDOWN_STATUS_UNUSED = -1 ;
	
	// Some info about messages...
	public static final int MAX_NUMBER_OF_MESSAGES = 40 ;
	public static final int MAX_MESSAGE_LENGTH = 150 ;
	
	// Lobby location.
	SocketAddress directSocketAddress ;
	String directAddress ;			// kept separate so we don't accidentally convert in a UI thread.
	// Lobby nonce
	Nonce sessionNonce ;
	Nonce nonce ;
	
	// This is the 'slot' for the local player.  These fields are used
	// when the Lobby instance belongs to a client.  They serve 
	// a useful purpose, in that they allow the LobbyView to display
	// only what the Lobby reports, but for those reports to reflect the
	// most recent actions by the player (not needing the LobbyCoordinator to respond
	// and make the change official).  Methods which include these values
	// in their results, and methods which return the canonical, "according to
	// LobbyCoordinator" values, are clearly distinguished in their 
	// names and Javadocs.
	int playerSlot ;
	Nonce playerPersonalNonce ;
	String playerName ;
	
	// Votes?
	boolean [] playerVotes ;
	
	// Lobby size
	private int maxPlayers ;
	boolean [] playerInLobby ;
	String lobbyName ;
	// Time it started (we access this through age, a long of milliseconds)
	long timeStarted ;
	
	// Launches?
	int numLaunches ;
	
	// Game modes
	int numGameModes ;
	int [] gameModes ;
	
	// Connected players
	String [] playerNames ;
	Nonce [] playerPersonalNonces ;
	
	// Player votes.
	boolean [][] voteForGameModeIndexByPlayer ;
	
	// Player status?
	int [] playerStatus ;
	
	// Games launching?  There are never more than numPlayers active
	// countdowns.  Countdowns are set so that, even if every countdown
	// reaches completion and prompts a launch, no player will be in
	// more than one game.  Every countdown has a unique number, assigned
	// by the Coordinator.
	int numCountdowns ;
	int [] countdownNumber ;
	long [] countdownStartTime ;
	long [] countdownDelay ;
	int [] countdownGameModeIndex ;
	boolean [][] countdownIncludesPlayer ;
	int [] countdownStatus ;	// a new edition 12/6.
	Object [] countdownTag ;	// a new edition 12/6.
	
	
	// DELEGATE
	WeakReference<Lobby.Delegate> mwrDelegate = null ;
	
	
	public Lobby() {
		directSocketAddress = null ;
		directAddress = null ;
		nonce = Nonce.ZERO ;
		
		// Default values for everything!
		playerSlot = -1 ;
		playerPersonalNonce = Nonce.ZERO ;
		playerName = null ;
		playerVotes = new boolean[0] ;
		
		maxPlayers = 0 ;
		playerInLobby = new boolean[0] ;
		lobbyName = null ;
		
		timeStarted = System.currentTimeMillis() ;
		
		numLaunches = 0 ;
		
		// Game modes
		numGameModes = 0 ;
		gameModes = new int[0] ;
		
		// Connected players
		playerNames = new String[0] ;
		playerPersonalNonces = new Nonce[0] ;
		
		// Player votes.
		voteForGameModeIndexByPlayer = new boolean[0][0] ;
		playerStatus = new int[0] ;
		
		// Games launching?
		numCountdowns = 0 ;
		countdownNumber = new int[0] ;
		countdownStartTime = new long[0] ;
		countdownDelay = new long[0] ;
		countdownGameModeIndex = new int[0] ;
		countdownIncludesPlayer = new boolean[0][0] ;
		countdownStatus = new int[0] ;
		countdownTag = new Object[0] ;
		
		mwrDelegate = new WeakReference<Delegate>(null) ;
	}
	
	
	public Lobby( Lobby l ) {
		this() ;
		takeVals(l) ;
	}
	
	public void takeVals( Lobby lobby ) {
		if ( lobby == null )
			return ;
		
		synchronized( lobby ) {
			// Lobby location.
			directSocketAddress = lobby.directSocketAddress ;
			directAddress = lobby.directAddress ;
			// Lobby nonce
			nonce = lobby.nonce ;
			
			// This player
			playerSlot = lobby.playerSlot ;
			playerPersonalNonce = lobby.playerPersonalNonce ;
			playerName = lobby.playerName ;
			// Votes?
			playerVotes = setVals( playerVotes, lobby.playerVotes ) ;
			
			// Lobby size
			maxPlayers = lobby.maxPlayers ;
			playerInLobby = setVals( playerInLobby, lobby.playerInLobby ) ;
			lobbyName = lobby.lobbyName ;
			// Time it started (we access this through age, a long of milliseconds)
			timeStarted = lobby.timeStarted ;
			
			this.numLaunches = lobby.numLaunches ;
			
			// Game modes
			numGameModes = lobby.numGameModes ;
			gameModes = setVals( gameModes, lobby.gameModes ) ;
			
			// Connected players
			playerNames = setVals( playerNames, lobby.playerNames ) ;
			playerPersonalNonces = setVals( playerPersonalNonces, lobby.playerPersonalNonces ) ;
			
			// Player votes.
			voteForGameModeIndexByPlayer =
				setVals(voteForGameModeIndexByPlayer,
						lobby.voteForGameModeIndexByPlayer ) ;
			
			// Player active?
			playerStatus = setVals( playerStatus, lobby.playerStatus ) ;
			
			// Games launching
			numCountdowns = lobby.numCountdowns ;
			countdownNumber = setVals( countdownNumber, lobby.countdownNumber ) ;
			countdownStartTime = setVals( countdownStartTime, lobby.countdownStartTime ) ;
			countdownDelay = setVals( countdownDelay, lobby.countdownDelay ) ;
			countdownGameModeIndex = setVals( countdownGameModeIndex, lobby.countdownGameModeIndex ) ;
			countdownIncludesPlayer = setVals( countdownIncludesPlayer, lobby.countdownIncludesPlayer ) ;
			countdownStatus = setVals( countdownStatus, lobby.countdownStatus ) ;
			countdownTag = setVals( countdownTag, lobby.countdownTag ) ;
		}
	}
	
	
	private boolean [] setVals( boolean [] dst, boolean [] src ) {
		if ( src == null )
			return null ;
		else if ( dst == null || dst.length != src.length )
			dst = new boolean[src.length] ;
		for ( int i = 0; i < src.length; i++ )
			dst[i] = src[i] ;
		return dst ;
	}
	
	private int [] setVals( int [] dst, int [] src ) {
		if ( src == null )
			return null ;
		else if ( dst == null || dst.length != src.length )
			dst = new int[src.length] ;
		for ( int i = 0; i < src.length; i++ )
			dst[i] = src[i] ;
		return dst ;
	}
	
	private long [] setVals( long [] dst, long [] src ) {
		if ( src == null )
			return null ;
		else if ( dst == null || dst.length != src.length )
			dst = new long[src.length] ;
		for ( int i = 0; i < src.length; i++ )
			dst[i] = src[i] ;
		return dst ;
	}
	
	private String [] setVals( String [] dst, String [] src ) {
		if ( src == null )
			return null ;
		else if ( dst == null || dst.length != src.length )
			dst = new String[src.length] ;
		for ( int i = 0; i < src.length; i++ )
			dst[i] = src[i] ;
		return dst ;
	}
	
	private Nonce [] setVals( Nonce [] dst, Nonce [] src ) {
		if ( src == null )
			return null ;
		else if ( dst == null || dst.length != src.length )
			dst = new Nonce[src.length] ;
		for ( int i = 0; i < src.length; i++ )
			dst[i] = src[i] ;
		return dst ;
	}
	
	private Object [] setVals( Object [] dst, Object [] src ) {
		if ( src == null )
			return null ;
		else if ( dst == null || dst.length != src.length )
			dst = new Object[src.length] ;
		for ( int i = 0; i < src.length; i++ )
			dst[i] = src[i] ;
		return dst ;
	}
	
	private boolean [][] setVals( boolean [][] dst, boolean [][] src ) {
		if ( src == null )
			return null ;
		else if ( dst == null || dst.length != src.length )
			dst = new boolean[src.length][] ;
		for ( int i = 0; i < src.length; i++ )
			dst[i] = setVals( dst[i], src[i] ) ;
		return dst ;
	}
	
	
	///////////////////////////////////////////////////////////////////
	//
	// CONNECTION INFORMATION
	//
	///////////////////////////////////////////////////////////////////
	
	public synchronized void setDirectAddress( SocketAddress address ) {
		directSocketAddress = address ;
		directAddress = ((InetSocketAddress)address).getAddress().getHostAddress() ;
	}
	
	public synchronized SocketAddress getDirectAddress() {
		return directSocketAddress ;
	}
	
	public synchronized String getDirectIPAddress() {
		return directAddress ;
	}
	
	///////////////////////////////////////////////////////////////////
	//
	// SESSION INFORMATION
	//
	///////////////////////////////////////////////////////////////////
	
	public synchronized void setSessionNonce( Nonce nonce ) {
		this.sessionNonce = nonce ;
	}
	
	public synchronized void setLobbyNonce( Nonce nonce ) {
		this.nonce = nonce ;
	}
	
	public synchronized Nonce getSessionNonce() {
		return this.sessionNonce ;
	}
	
	public synchronized Nonce getLobbyNonce() {
		return this.nonce ;
	}

	
	///////////////////////////////////////////////////////////////////
	//
	// DELEGATE SETUP!
	//
	///////////////////////////////////////////////////////////////////
	
	public synchronized void setDelegate( Lobby.Delegate delegate ) {
		this.mwrDelegate = new WeakReference<Delegate>(delegate) ;
	}
	
	
	///////////////////////////////////////////////////////////////////
	//
	// LAUNCH?
	//
	///////////////////////////////////////////////////////////////////
	
	public synchronized void countLaunch() {
		this.numLaunches++ ;
	}
	
	public synchronized int numLaunches() {
		return numLaunches ;
	}
	
	
	///////////////////////////////////////////////////////////////////
	//
	// FANCY ACCESSORS
	//
	///////////////////////////////////////////////////////////////////
	
	/**
	 * Returns the player slot assigned to the local player, i.e., the
	 * client using this Lobby instance as his personal datastore.
	 */
	public synchronized int getLocalPlayerSlot() {
		return this.playerSlot ;
	}
	
	/**
	 * Returns the canonical owner name of this lobby.
	 * @return
	 */
	public synchronized String getLobbyName() {
		return this.lobbyName ;
	}
	
	/**
	 * Returns the canonical maximum number of people in this lobby.  Will
	 * return 0 if setMaxPeople has not been called.
	 * @return
	 */
	public synchronized int getMaxPeople() {
		return this.maxPlayers ;
	}
	
	
	/**
	 * Returns an array of length #gamemodes giving the game mode numbers
	 * supported by this lobby.
	 * @return
	 */
	public synchronized int [] getGameModes() {
		int [] g = new int[gameModes.length] ;
		for ( int i = 0; i < g.length; i++ )
			g[i] = gameModes[i] ;
		return g ;
	}
	
	
	public synchronized boolean hasGameMode( int gameMode ) {
		for ( int i = 0; i < gameModes.length; i++ ) {
			if ( gameModes[i] == gameMode ) {
				return true ;
			}
		}
		return false ;
	}
	
	
	public synchronized int getNumPeople() {
		int num = 0 ;
		for ( int i = 0; i < playerInLobby.length; i++ )
			if ( playerInLobby[i] )
				num++ ;
		return num ;
	}

	
	public synchronized boolean [] getPlayersInLobby() {
		boolean [] players = new boolean[playerInLobby.length] ;
		for ( int i = 0; i < players.length; i++ )
			players[i] = playerInLobby[i] ;
		return players ;
	}
	
	
	/**
	 * Returns the canonical list of player names.  Only the connected
	 * players are included in the list returned.
	 * @return
	 */
	public synchronized String [] getPlayerNamesInLobby() {
		int numIncluded = count( this.playerInLobby ) ;
		
		String [] n = new String[ numIncluded ] ;
		numIncluded = 0 ;
		for ( int i = 0; i < playerInLobby.length; i++ ) {
			if ( playerInLobby[i] ) {
				n[numIncluded] = playerNames[i] ;
				numIncluded++ ;
			}
		}
		
		return n ;
	}
	
	/**
	 * Returns the canonical status list of connected players.  Obviously
	 * only connected players are included; valid statuses are PLAYER_STATUS_ACTIVE
	 * and PLAYER_STATUS_INACTIVE.
	 * @return
	 */
	public synchronized int [] getPlayerStatusesInLobby() {
		int numIncluded = count( this.playerInLobby ) ;
		
		int [] s = new int[numIncluded] ;
		numIncluded = 0 ;
		for ( int i = 0; i < playerStatus.length; i++ ) {
			if ( playerInLobby[i] ) {
				s[numIncluded] = playerStatus[i] ;
				numIncluded++ ;
			}
		}
		
		return s ;
	}
	
	/**
	 * Returns a complete canonical list of player statuses.  All players,
	 * connected or not, are included.  Thus, the status of the player in
	 * slot 'slot' is given by returnedValue[slot].
	 * 
	 * @return
	 */
	public synchronized int [] getPlayerStatuses() {
		int [] s = new int[this.maxPlayers]; 
		for ( int i = 0; i < this.maxPlayers; i++ ) {
			if ( !playerInLobby[i] )
				s[i] = PLAYER_STATUS_NOT_CONNECTED ;
			else 
				s[i] = playerStatus[i] ;
		}
		return s ;
	}
	
	
	public synchronized int getPlayerStatus( int slot ) {
		if ( !playerInLobby[slot] )
			return PLAYER_STATUS_NOT_CONNECTED ;
		return playerStatus[slot] ;
	}
	
	
	public synchronized String getPlayerName( int slot ) {
		if ( playerNames == null || playerNames.length <= slot || slot < 0 )
			return null ;
		return playerNames[slot] ;
	}
	
	/**
	 * Returns the canonical list of player names; includes unconnected players.
	 * @return
	 */
	public synchronized String [] getPlayerNames() {
		return playerNames ;
	}
	
	/**
	 * Returns a list of player names, in order of ascending slot number, where
	 * only those slots indicated with a 'true' value in the parameter are included.
	 * Thus, returnedValue.length == sum( players ) where true = 1, false = 0.
	 * @param players
	 * @return
	 */
	public synchronized String [] getPlayerNames( boolean [] players ) {
		int numIncluded = count( players ) ;
		
		String [] namesReturned = new String[numIncluded] ;
		numIncluded = 0 ;
		for ( int i = 0; i < players.length; i++ ) {
			if ( players[i] ) {
				namesReturned[numIncluded] = playerNames[i] ;
				numIncluded++ ;
			}
		}
		
		return namesReturned ;
	}
	
	/**
	 * Returns an array of player names, where returnedValue[i] is the name
	 * of the player in slot slots[i], and returnedValue.length == slots.length.
	 * @param slots
	 * @return
	 */
	public synchronized String [] getPlayerNames( int [] slots ) {
		String [] n = new String[slots.length] ;
		for ( int i = 0; i < slots.length; i++ )
			n[slots[i]] = playerNames[slots[i]] ;
		return n ;
	}
	
	
	public synchronized Nonce [] getPlayerPersonalNonces() {
		Nonce [] n = new Nonce[maxPlayers] ;
		for ( int i = 0; i < n.length; i++ )
			n[i] = playerPersonalNonces[i] ;
		return n ;
	}
	
	
	/**
	 * Returns an array of personalPlayerNonces, in order of ascending slot number, where
	 * only those slots indicated with a 'true' value in the parameter are included.
	 * Thus, returnedValue.length == sum( players ) where true = 1, false = 0.
	 * @param players
	 * @return
	 */
	public synchronized Nonce [] getPlayerPersonalNonces( boolean [] players ) {
		int num = count( players ) ;
		
		Nonce [] noncesR = new Nonce[num] ;
		num = 0 ;
		for ( int i = 0; i < players.length; i++ ) {
			if ( players[i] ) {
				noncesR[num] = playerPersonalNonces[i] ;
				num++ ;
			}
		}
		
		return noncesR ;
	}
	
	
	/**
	 * Returns an array of player personal nonces, where returnedValue[i] is the nonce
	 * of the player in slot slots[i], and returnedValue.length == slots.length.
	 * @param slots
	 * @return
	 */
	public synchronized Nonce [] getPlayerPersonalNonces( int [] slots ) {
		Nonce [] n = new Nonce[slots.length] ;
		for ( int i = 0; i < slots.length; i++ )
			n[slots[i]] = playerPersonalNonces[slots[i]] ;
		return n ;
	}
	
	
	/**
	 * Returns an array of player slots in ascending order, where
	 * returnedValue.length is the number of players who have voted for 'gameMode',
	 * and the entries of returnedValue are the slots of those voters.
	 * @param gameMode
	 * @return
	 */
	public synchronized int [] getVoterSlots( int gameMode ) {
		int gameModeIndex = findGameModeIndex( gameMode ) ;
		if ( gameModeIndex == -1 )
			return new int[0] ;
		int num = count( this.voteForGameModeIndexByPlayer[gameModeIndex] ) ;
		
		int [] vSlots = new int[num] ;
		num = 0 ;
		for ( int i = 0; i < this.maxPlayers; i++ ) {
			if ( this.voteForGameModeIndexByPlayer[gameModeIndex][i] ) {
				vSlots[num] = i ;
				num++ ;
			}
		}
		return vSlots ;
	}
	
	
	/**
	 * Returns an array of player names in ascending order of slot, where
	 * returnedValue.length is the number of players who have voted for 'gameMode',
	 * and the entries of returnedValue are the names of those voters.
	 * @param gameMode
	 * @return
	 */
	public synchronized String [] getVoterNames( int gameMode ) {
		int gameModeIndex = findGameModeIndex( gameMode ) ;
		if ( gameModeIndex == -1 )
			return new String[0] ;
		
		int num = count( this.voteForGameModeIndexByPlayer[gameModeIndex] ) ;
		
		String [] vnames = new String[num] ;
		num = 0 ;
		for ( int i = 0; i < this.maxPlayers; i++ ) {
			if ( this.voteForGameModeIndexByPlayer[gameModeIndex][i] ) {
				vnames[num] = playerNames[i] ;
				num++ ;
			}
		}
		return vnames ;
	}
	
	
	/**
	 * Returns a boolean array of length = maxPlayers, where returnedValue[i] == true
	 * indicates that the player in slot 'i' has voted for the specified gameMode.
	 * @param gameMode
	 * @return
	 */
	public synchronized boolean [] getVotes( int gameMode ) {
		int gameModeIndex = findGameModeIndex( gameMode ) ;
		boolean [] v = new boolean[maxPlayers] ;
		if ( gameModeIndex == -1 )
			return v ;
		
		for ( int i = 0; i < this.maxPlayers; i++ ) {
			v[i] = this.voteForGameModeIndexByPlayer[gameModeIndex][i] ;
		}
		return v ;
	}
	
	
	public synchronized boolean getPlayerVote( int playerSlot, int gameMode ) {
		int gameModeIndex = findGameModeIndex( gameMode ) ;
		if ( gameModeIndex == -1 )
			return false ;
		
		return this.voteForGameModeIndexByPlayer[gameModeIndex][playerSlot] ;
	}
	
	
	/** 
	 * Returns a boolean array of length = #gameModes, where a 'true' indicates
	 * that the local player has voted for the game mode with that index.
	 */
	public synchronized boolean [] getLocalVotes() {
		boolean [] votes = new boolean[playerVotes.length] ;
		for ( int i = 0; i < votes.length; i++ )
			votes[i] = playerVotes[i] ;
		return votes ;
	}
	
	
	/**
	 * Returns a list of integers, where each integer indicates a game mode voted
	 * for by the local player.
	 * @return
	 */
	public synchronized int [] getLocalVoteGameModes() {
		int num = count( playerVotes ) ;
		int [] modes = new int[num] ;
		num = 0 ;
		for ( int i = 0; i < playerVotes.length; i++ ) {
			if ( playerVotes[i] ) {
				modes[num] = this.gameModes[i] ;
				num++ ;
			}
		}
		return modes ;
	}
	
	
	/**
	 * Returns a boolean array of length = number of players, where returnedValue[i] == true
	 * indicates that the player in slot i has voted for the specified game mode.
	 * Unlike getVotes, this method will include the 'local player votes', even if
	 * they are not yet considered canonical by the Lobby.
	 * @param gameMode
	 * @return
	 */
	public synchronized boolean [] getVotesIncludingLocal( int gameMode ) {
		int gameModeIndex = findGameModeIndex( gameMode ) ;
		
		if ( gameModeIndex >= 0 ) {
			boolean [] votes = new boolean[ this.maxPlayers ] ;
			
			if ( playerVotes != null && voteForGameModeIndexByPlayer != null ) {
				for ( int p = 0; p < this.maxPlayers; p++ ) {
					if ( p == this.playerSlot )
						votes[p] = this.playerVotes[gameModeIndex] ;
					else
						votes[p] = this.voteForGameModeIndexByPlayer[gameModeIndex][p] ;
				}
			}
			
			return votes ;
		}
		else
			return new boolean[0] ;
 	}
	
	
	/**
	 * Returns whether the local player has voted for the specified gameMode.
	 * This method checks local records, not canonical ones.
	 * @param gameMode
	 * @return
	 */
	public synchronized boolean getLocalVote( int gameMode ) {
		int gameModeIndex = findGameModeIndex( gameMode ) ;
		if ( gameModeIndex >= 0 )
			return this.playerVotes[gameModeIndex] ;
		else 
			return false ;
	}
	
	
	/**
	 * Returns whether the specified player is currently in a countdown.
	 * @param playerSlot
	 * @return
	 */
	public synchronized boolean getPlayerIsInCountdown( int playerSlot ) {
		if ( playerSlot >= maxPlayers )
			return false ;
		for ( int i = 0; i < numCountdowns; i++ ) {
			if ( this.countdownIncludesPlayer[i][playerSlot] )
				return true ;
		}
		return false ;
	}
	
	/**
	 * Returns whether the specified player is currently in a countdown
	 * for the specified game mode.
	 * @param playerSlot
	 * @return
	 */
	public synchronized boolean getPlayerIsInGameModeCountdown( int playerSlot, int gameMode ) {
		if ( playerSlot >= maxPlayers )
			return false ;
		int gameModeIndex = this.findGameModeIndex(gameMode) ;
		if ( gameModeIndex == -1 )
			return false ;
		for ( int i = 0; i < numCountdowns; i++ ) {
			if ( this.countdownIncludesPlayer[i][playerSlot] && this.countdownGameModeIndex[i] == gameModeIndex )
				return true ;
		}
		return false ;
	}
	
	
	/**
	 * Returns the number of the countdown the player is participating in.
	 * If multiple countdowns, returns the first found.  If none are found,
	 * returns -1.
	 * @param playerSlot
	 * @return
	 */
	public synchronized int getPlayerCountdownNumber( int playerSlot ) {
		for ( int i = 0; i < numCountdowns; i++ ) {
			if ( this.countdownIncludesPlayer[i][playerSlot] )
				return this.countdownNumber[i] ;
		}
		return -1 ;
	}
	
	public synchronized int [] getCountdownsForGameMode( int gameMode ) {
		int gameModeIndex = this.findGameModeIndex(gameMode) ;
		if ( gameModeIndex == -1 )
			return new int[0] ;
		
		int num = 0 ;
		for ( int c = 0; c < numCountdowns; c++ )
			if ( this.countdownGameModeIndex[c] == gameModeIndex )
				num++ ;
		if ( num == 0 )
			return new int[0] ;
		
		int [] countdowns = new int[num] ;
		int index = 0 ;
		for ( int c = 0; c < numCountdowns; c++ ) {
			if ( this.countdownGameModeIndex[c] == gameModeIndex ) {
				countdowns[index] = this.countdownNumber[c] ;
				index++ ;
			}
		}
		
		return countdowns ;
	}
	
	
	public synchronized boolean getHasCountdown( int countdownNumber ) {
		for ( int i = 0; i < numCountdowns; i++ )
			if ( this.countdownNumber[i] == countdownNumber )
				return true ;
		return false ;
	}
	
	
	public synchronized long getCountdownDelay( int countdownNumber ) {
		for ( int i = 0; i < numCountdowns; i++ )
			if ( this.countdownNumber[i] == countdownNumber )
				return Math.max( 0, this.countdownDelay[i] - (System.currentTimeMillis() - this.countdownStartTime[i]) ) ;
		return -1 ;
		//throw new IllegalArgumentException( "No countdown found for number " + countdownNumber ) ;
	}
	
	public synchronized int getCountdownStatus( int countdownNumber ) {
		for ( int i = 0; i < numCountdowns; i++ )
			if ( this.countdownNumber[i] == countdownNumber )
				return this.countdownStatus[i] ;
		return COUNTDOWN_STATUS_NONE ;
	}
	
	public synchronized Object getCountdownTag( int countdownNumber ) {
		for ( int i = 0; i < numCountdowns; i++ )
			if ( this.countdownNumber[i] == countdownNumber )
				return this.countdownTag[i] ;
		return null ;
	}
	
	
	/**
	 * Fills the provided arrays with information regarding the current countdowns.
	 * Each parameter should either be pre-allocated to size maxPlayers, or 'null',
	 * in which case it will be ignored.  Calling this method with all 'null' parameters
	 * will query only the number of countdowns.
	 * 
	 * After this call, with return value C, for c < C, 
	 * countdown c has number countdownNumber[c], game mode gameMode[c], delay[c] milliseconds
	 * until launch, and includes the players with value 'true' in participant[c].
	 * 
	 * @param countdownNumber If non-null, will be filled with the numbers of the current countdowns.
	 * @param gameMode If non-null, will be filled with the game modes of teh current countdowns.
	 * @param delay If non-null, will be filled with the time remaining in the countdowns (in milliseconds)
	 * @param participant If non-null, will be filled with the players participating in the countdown
	 * 			(almost certainly a subset of those who voted for the game mode).
	 * @return The number of countdowns current going.
	 */
	public synchronized int getCountdowns( int [] countdownNumber, int [] gameMode, long [] delay, boolean [][] participant, int [] status, Object [] tags ) {
		long curTime = System.currentTimeMillis() ;
		for ( int c = 0; c < numCountdowns; c++ ) {
			if ( countdownNumber != null )
				countdownNumber[c] = this.countdownNumber[c] ;
			if ( gameMode != null )
				gameMode[c] = gameModes[ this.countdownGameModeIndex[c] ] ;
			if  ( delay != null )
				delay[c] = Math.max( 0, this.countdownDelay[c] - (curTime - this.countdownStartTime[c]) ) ;
			if ( participant != null )
				for ( int p = 0; p < maxPlayers; p++ )
					participant[c][p] = this.countdownIncludesPlayer[c][p] ;
			if ( status != null )
				status[c] = this.countdownStatus[c] ;
			if ( tags != null )
				tags[c] = this.countdownTag[c] ;
		}
		return numCountdowns ;
	}

	
	// Setters for information.
	
	/**
	 * Sets the player slot for the local player.
	 */
	public synchronized  void setLocalPlayerSlot( int slot ) {
		playerSlot = slot ;
	}
	
	/**
	 * Sets the personal Nonce for the local player.
	 * @param nonce
	 */
	public synchronized  void setLocalPlayerPersonalNonce( Nonce nonce ) {
		playerPersonalNonce = nonce ;
	}
	
	/**
	 * Sets the name for the local player.
	 * @param name
	 */
	public synchronized void setLocalPlayerName( String name ) {
		playerName = name ;
	}
	
	
	/**
	 * Sets the canonical lobby owner's name.
	 * @param name
	 */
	public synchronized void setLobbyName( String name ) {
		lobbyName = name ;
	}
	
	/**
	 * Sets the canonical name for the player in the specified slot.
	 */
	public synchronized void setName( int slot, String name ) {
		if ( slot < 0 || playerNames == null || playerNames.length <= slot )
			return ;
		
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate != null )
			delegate.ld_memberChangedName(slot, playerNames[slot], name, this) ;
		
		playerNames[slot] = name ;
	}
	
	/**
	 * Sets the canonical personal nonce for the player in the specified slot.
	 * @param slot
	 * @param nonce
	 */
	public synchronized void setPersonalNonce( int slot, Nonce nonce ) {
		playerPersonalNonces[slot] = nonce ;
	}
	
	
	/**
	 * Sets the local player's non-canonical vote for the specified game mode to 'v'.
	 * @param gameMode
	 * @param v
	 */
	public synchronized void setLocalPlayerVoteForGameMode( int gameMode, boolean v ) {
		int gameModeIndex = findGameModeIndex( gameMode ) ;
		if ( gameModeIndex == -1 )
			return ;
		
		boolean voteChanged = false ;
		if ( playerVotes[gameModeIndex] != v )
			voteChanged = true ;
		
		playerVotes[gameModeIndex] = v ;
		
		Delegate delegate = mwrDelegate.get() ;
		if ( voteChanged && delegate != null )
			delegate.ld_updatedGameModeVotes(gameMode, this) ;
	}
	
	
	public synchronized void resetMembership() {
		if ( playerVotes != null )
			for ( int i = 0; i < playerVotes.length; i++ )
				playerVotes[i] = false ;
		
		if ( playerInLobby != null ) {
			for ( int i = 0; i < playerInLobby.length; i++ ) {
				playerInLobby[i] = false ;
				playerStatus[i] = PLAYER_STATUS_NOT_CONNECTED ;
			}
		}
		
		if ( playerNames != null ) {
			for ( int i = 0; i < playerNames.length; i++ ) {
				playerNames[i] = null ;
				playerPersonalNonces[i] = null ;
			}
		}
		
		if ( voteForGameModeIndexByPlayer != null ) {
			for ( int i = 0; i < voteForGameModeIndexByPlayer.length; i++ ) {
				for ( int j = 0; j < voteForGameModeIndexByPlayer[i].length; j++ ) {
					voteForGameModeIndexByPlayer[i][j] = false ;
				}
			}
		}
		
		numCountdowns = 0 ;
		if ( countdownNumber != null ) {
			for ( int i = 0; i < countdownNumber.length; i++ ) {
				countdownNumber[i] = 0 ;
				countdownStartTime[i] = 0 ;
				countdownDelay[i] = 0 ;
				countdownGameModeIndex[i] = 0 ;
				countdownStatus[i] = COUNTDOWN_STATUS_NONE ;
				countdownTag[i] = null ;
				for ( int j = 0; j < countdownIncludesPlayer[i].length; j++ )
					countdownIncludesPlayer[i][j] = false ;
			}
		}
	}
	
	/**
	 * Sets all the local player's non-canonical votes to false.
	 */
	public synchronized void resetLocalPlayerVotes() {
		for ( int i = 0; i < playerVotes.length; i++ )
			playerVotes[i] = false ;
	}
	
	public synchronized void resetPlayerVotes( int playerSlot ) {
		Delegate delegate = mwrDelegate.get() ;
		for ( int i = 0; i < this.voteForGameModeIndexByPlayer.length; i++ ) {
			boolean changed = this.voteForGameModeIndexByPlayer[i][playerSlot] ;
			this.voteForGameModeIndexByPlayer[i][playerSlot] = false ;
			if ( delegate != null && changed )
				delegate.ld_updatedGameModeVotes(i, this) ;
		}
		
	}
	
	/**
	 * Sets the maximum number of players in the lobby.  This is a necessary call
	 * very early on, as it allocates important structures.  Calling this method
	 * in a Lobby object which has been active (i.e., calling it for a second
	 * time) will erase almost all previously stored information (IF the provided
	 * number of players differs from the previous number).
	 * @param max
	 */
	public synchronized void setMaxPlayers( int max ) {
		this.maxPlayers = max ;
		
		// Reallocate?
		if ( playerInLobby == null || playerInLobby.length != this.maxPlayers )
			playerInLobby = new boolean[max] ;
		if ( playerStatus == null || playerStatus.length != this.maxPlayers )
			playerStatus = new int[max] ; 
		if ( playerNames == null || playerNames.length != this.maxPlayers )
			playerNames = new String[max] ;
		if ( playerPersonalNonces == null || playerPersonalNonces.length != this.maxPlayers ) {
			playerPersonalNonces = new Nonce[max] ;
			for ( int i = 0; i < max; i++ )
				playerPersonalNonces[i] = Nonce.ZERO ;
		}
			
		if ( this.numGameModes > 0 &&
				( this.voteForGameModeIndexByPlayer == null
						|| this.voteForGameModeIndexByPlayer[0].length != this.maxPlayers ) ) {
			this.voteForGameModeIndexByPlayer = new boolean[this.numGameModes][this.maxPlayers] ;
		}
		
		// Countdowns need to be reallocated as well.  It is impossible to
		// have more countdowns than there are players.
		if ( this.countdownStartTime == null || this.countdownStartTime.length != this.maxPlayers ) {
			this.countdownStartTime = new long[this.maxPlayers] ;
			this.countdownDelay = new long[this.maxPlayers] ;
			this.countdownGameModeIndex = new int[this.maxPlayers] ;
			this.countdownIncludesPlayer = new boolean[this.maxPlayers][this.maxPlayers] ; 
			this.countdownNumber = new int[this.maxPlayers] ;
			this.countdownStatus = new int[this.maxPlayers] ;
			this.countdownTag = new Object[this.maxPlayers] ;
		}
	}
	
	public synchronized void setPlayerInLobby( int player, boolean inLobby ) {
		boolean prevInLobby = playerInLobby[player] ;
		playerInLobby[player] = inLobby ;
		if ( !inLobby )
			playerStatus[player] = PLAYER_STATUS_NOT_CONNECTED ;
		
		Delegate delegate = mwrDelegate.get() ;
		if ( prevInLobby && !inLobby && delegate != null )
			delegate.ld_memberLeftLobby(player, playerNames[player], this) ;
		else if ( !prevInLobby && inLobby && delegate != null )
			delegate.ld_memberJoinedLobby(player, playerNames[player], this) ;
	}
	
	/**
	 * Set the players in this lobby.  This method activates the delegate call
	 * to "ld_memberJoinedLobby" and "ld_memberLeftLobby", as appropriate.
	 * @param players
	 */
	public synchronized void setPlayersInLobby( boolean [] players ) {
		for ( int i = 0; i < players.length; i++ ) {
			boolean prevInLobby = playerInLobby[i] ;
			playerInLobby[i] = players[i] ;
			if ( !playerInLobby[i] )
				playerStatus[i] = PLAYER_STATUS_NOT_CONNECTED ;
			
			Delegate delegate = mwrDelegate.get() ;
			if ( players[i] && !prevInLobby && delegate != null )
				delegate.ld_memberJoinedLobby(i, playerNames[i], this) ;
			else if ( !players[i] && prevInLobby && delegate != null )
				delegate.ld_memberLeftLobby(i, playerNames[i], this) ;
			playerInLobby[i] = players[i] ;
		}
	}
	
	
	/**
	 * Sets the specified player as active, inactive, 
	 * 
	 * @param playerSlot
	 * @param active
	 */
	public synchronized void setPlayerStatus( int playerSlot, int status ) {
		if ( status == PLAYER_STATUS_NOT_CONNECTED )
			throw new IllegalArgumentException("NOT_CONNECTED cannot be explicitly set as a status.") ;
		if ( status < 0 || status >= NUM_PLAYER_STATUSES )
			throw new IllegalArgumentException("Value " + status + " is not a valid status") ;
		int prevStatus = playerStatus[playerSlot] ;
		playerStatus[playerSlot] = status ;
		
		Delegate delegate = mwrDelegate.get() ;
		if ( prevStatus != status && delegate != null )
			delegate.ld_memberChangedStatus(playerSlot, prevStatus, status, this) ;
	}
	
	
	public synchronized void setPlayerActive( int playerSlot ) {
		int prevStatus = playerStatus[playerSlot] ;
		playerStatus[playerSlot] = PLAYER_STATUS_ACTIVE ;
		
		Delegate delegate = mwrDelegate.get() ;
		if ( prevStatus != PLAYER_STATUS_ACTIVE && delegate != null )
			delegate.ld_memberChangedStatus(playerSlot, prevStatus, PLAYER_STATUS_ACTIVE, this) ;
	}
	
	public synchronized void setPlayerInactive( int playerSlot ) {
		int prevStatus = playerStatus[playerSlot] ;
		playerStatus[playerSlot] = PLAYER_STATUS_INACTIVE ;
		
		Delegate delegate = mwrDelegate.get() ;
		if ( prevStatus != PLAYER_STATUS_INACTIVE && delegate != null )
			delegate.ld_memberChangedStatus(playerSlot, prevStatus, PLAYER_STATUS_INACTIVE, this) ;
	}
	
	public synchronized void setPlayerInGame( int playerSlot ) {
		int prevStatus = playerStatus[playerSlot] ;
		playerStatus[playerSlot] = PLAYER_STATUS_IN_GAME ;
		
		Delegate delegate = mwrDelegate.get() ;
		if ( prevStatus != PLAYER_STATUS_IN_GAME && delegate != null )
			delegate.ld_memberChangedStatus(playerSlot, prevStatus, PLAYER_STATUS_IN_GAME, this) ;
	}
	
	
	
	
	/**
	 * Sets the "activeness" of all players according to the true/false value in the provided array.
	 * @param players
	 */
	public synchronized void setPlayersStatuses( int [] status ) {
		Delegate delegate = mwrDelegate.get() ;
		for ( int i = 0; i < playerStatus.length; i++ ) {
			int prevStatus = playerStatus[i] ;
			playerStatus[i] = status[i] ;
			if ( status[i] != prevStatus && delegate != null )
				delegate.ld_memberChangedStatus(i, prevStatus, status[i], this) ;
		}
	}
	
	
	/**
	 * Sets the NUMBER of game modes available in this lobby.  This call will allocate
	 * new structures, and thus lose data, if made to a Lobby object that has been in
	 * use for a while.
	 * @param num
	 */
	public synchronized void setNumberOfGameModes( int num ) {
		this.numGameModes = num ;
		
		// Reallocate?
		if ( gameModes == null || gameModes.length != this.numGameModes ) 
			gameModes = new int[this.numGameModes];
		if ( playerVotes == null || playerVotes.length != this.numGameModes )
			playerVotes = new boolean[this.numGameModes] ;
		
		if ( this.maxPlayers > 0 &&
				( this.voteForGameModeIndexByPlayer == null
						|| this.voteForGameModeIndexByPlayer.length != this.numGameModes ) )
			this.voteForGameModeIndexByPlayer = new boolean[this.numGameModes][this.maxPlayers] ;
	}
	
	
	/**
	 * Sets the game modes to those in the provided array.  Includes a call to
	 * setNumberOfGameModes, IF the provided array is of a different length than
	 * the previously set number of game modes.
	 * @param modeList
	 */
	public synchronized void setGameModes( int [] modeList ) {
		if ( this.gameModes == null || this.gameModes.length != modeList.length )
			setNumberOfGameModes( modeList.length ) ;
		
		for ( int i = 0; i < modeList.length; i++ ) {
			this.gameModes[i] = modeList[i] ;
		}
		
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null )
			delegate.ld_updatedGameModes(this) ;
	}
	
	
	public synchronized void setPlayerVoteForGameMode( int playerSlot, int gameMode, boolean v ) {
		int gameModeIndex = findGameModeIndex( gameMode ) ;
		if ( gameModeIndex == -1 )
			return ;
		this.voteForGameModeIndexByPlayer[gameModeIndex][playerSlot] = v ;
		
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null )
			delegate.ld_updatedGameModeVotes(gameMode, this) ;
	}
	
	
	/**
	 * Sets the canonical votes for the specified game mode.
	 * @param gameMode
	 * @param votes
	 */
	public synchronized void setVotesForGameMode( int gameMode, boolean [] votes ) {
		int gameModeIndex = findGameModeIndex( gameMode ) ;
		if ( gameModeIndex == -1 )
			return ;
		for ( int j = 0; j < this.maxPlayers; j++ )
			this.voteForGameModeIndexByPlayer[gameModeIndex][j] = votes[j] ;
		
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null )
			delegate.ld_updatedGameModeVotes(gameMode, this) ;
	}
	
	
	
	/**
	 * Sets the delay for the provided countdown.  Returns 'true' if a new countdown was
	 * added, 'false' if an existing countdown has been modified.
	 * 
	 * Will throw an IllegalArgumentException if the provided countdown partially describes
	 * an existing one, e.g. having the same countdownNumber but a different game mode.
	 * 
	 * Acceptable updates: an update will be accepted (i.e. an existing countdown modified,
	 * 		with 'false' being returned, if:
	 * 
	 * 		The countdown number and game modes match.
	 * 
	 * Previously we refused a change which altered the participating players, but that produces errors
	 * for game modes with 3+ players (since the same countdown can be modified as players are added / removed.
	 * 
	 * @param countdownNumber
	 * @param gameMode
	 * @param players
	 * @param delay
	 * 
	 * @return 'true' if this is a NEW countdown, 'false' if an update
	 * to an existing one.
	 */
	public synchronized boolean setCountdownDelay( int countdownNumber, int gameMode, boolean [] players, long delay, int status, Object tag ) {
		
		int gameModeIndex = findGameModeIndex( gameMode ) ;
		if ( gameModeIndex == -1 )
			throw new IllegalArgumentException("Can't add new countdown: invalid game mode") ;
		
		if ( status != COUNTDOWN_STATUS_ACTIVE && status != COUNTDOWN_STATUS_HALTED )
			throw new IllegalArgumentException("Specified countdown status " + status + " is not valid for setting a delay.") ;
		if ( gameModeIndex == -1 )
			throw new IllegalArgumentException("Specified game mode " + gameMode + " does not exist in this lobby.") ;
		
		boolean newCountdown = false ;
		int c ;
		for ( c = 0; c < numCountdowns; c++ ) {
			if ( this.countdownNumber[c] == countdownNumber )
				break ;
		}
		
		if ( c == numCountdowns ) {
			// maybe a new one
			if ( numCountdowns >= maxPlayers )
				throw new IllegalArgumentException("Can't add new countdown: already full") ;
			
			numCountdowns++ ;
			newCountdown = true ;
		}
		
		// If not a new countdown, verify that the game mode and players match.
		if ( !newCountdown ) {
			if ( this.countdownGameModeIndex[c] != gameModeIndex )
				throw new IllegalArgumentException("Countdown #" + countdownNumber + " has game mode index " + this.countdownGameModeIndex[c] + ", not " + gameModeIndex ) ;
		}
		
		// Whelp, set err'thing.
		this.countdownNumber[c] = countdownNumber ;
		this.countdownDelay[c] = delay ;
		this.countdownStartTime[c] = System.currentTimeMillis() ;
		this.countdownGameModeIndex[c] = gameModeIndex ;
		for ( int p = 0; p < this.maxPlayers; p++ )
			this.countdownIncludesPlayer[c][p] = players[p] ;
		this.countdownStatus[c] = status ;
		this.countdownTag[c] = tag ;
		
		// Tell the delegate.
		Delegate delegate = mwrDelegate.get() ;
		if ( newCountdown && delegate != null )
			delegate.ld_newCountdown(countdownNumber, gameMode, this) ;
		else if ( !newCountdown && delegate != null )
			delegate.ld_updatedCountdown(countdownNumber, gameMode, this) ;
		return newCountdown ;
	}

	
	/**
	 * Sets the specified countdown as "halted."  Returns 'true' if such
	 * a countdown was found, 'false' otherwise.  Will make a delegate call
	 * to ld_updatedCountdown if a coutndown was found and a delegate is set.
	 * @param countdownNumber
	 * @param gameMode
	 * @param players
	 * @return
	 */
	public synchronized boolean setCountdownHalted( int countdownNumber, int gameMode, boolean [] players ) {
		int this_c ;
		for ( this_c = 0; this_c < numCountdowns; this_c++ ) {
			if ( this.countdownNumber[this_c] == countdownNumber )
				break ;
		}
		
		if ( this_c == numCountdowns )
			return false ;
		
		this.countdownStatus[this_c] = COUNTDOWN_STATUS_HALTED ;
		
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null )
			delegate.ld_updatedCountdown(countdownNumber, gameMode, this) ;
		
		return true ;
	}
	
	
	public synchronized boolean setCountdownAborted( int countdownNumber, boolean failure ) {
		// We look among our existing countdowns for one that matches this game
		// mode and player list.  If found, abort the count.  Returns 'false'
		// if the countdown could not be found.
		
		int this_c ;
		for ( this_c = 0; this_c < numCountdowns; this_c++ ) {
			if ( this.countdownNumber[this_c] == countdownNumber )
				break ;
		}
		
		if ( this_c == numCountdowns )
			return false ;
		
		
		// We're GOING to remove the countdown...
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null )
			delegate.ld_removingCountdown(countdownNumber, this.countdownGameModeIndex[this_c], this, failure) ;
		
		// Whelp, move err'thing down a ways.
		for ( int c = this_c + 1; c < numCountdowns; c++ ) {
			this.countdownNumber[c-1] = this.countdownNumber[c] ;
			this.countdownDelay[c-1] = this.countdownDelay[c] ;
			this.countdownStartTime[c-1] = this.countdownStartTime[c] ;
			this.countdownGameModeIndex[c-1] = this.countdownGameModeIndex[c] ;
			for ( int p = 0; p < this.maxPlayers; p++ )
				this.countdownIncludesPlayer[c-1][p] = this.countdownIncludesPlayer[c][p] ;
			this.countdownStatus[c-1] = this.countdownStatus[c] ;
			this.countdownTag[c-1] = this.countdownTag[c] ;
		}
		numCountdowns-- ;
		
		return true ;
	}
	
	// Consistency checks!
	public synchronized boolean playerNameIsConsistent() {
		if ( playerNames == null )
			return false ;
		if ( playerSlot < 0 || playerSlot >= playerNames.length )
			return false ;
		if ( !playerName.equals( playerNames[playerSlot] ) )
			return false ;
		
		return true ;
	}
	
	// Consistency checks!
	public synchronized boolean playerPersonalNonceIsConsistent() {
		if ( playerPersonalNonces == null )
			return false ;
		if ( playerSlot < 0 || playerSlot >= playerPersonalNonces.length )
			return false ;
		if ( playerPersonalNonce != playerPersonalNonces[playerSlot] )
			return false ;
		
		return true ;
	}
	
	public synchronized boolean playerVotesAreConsistent() {
		if ( playerVotes == null || voteForGameModeIndexByPlayer == null )
			return false ;
		if ( playerSlot < 0 || playerSlot >= voteForGameModeIndexByPlayer[0].length)
			return false ;
		for ( int i = 0; i < voteForGameModeIndexByPlayer.length; i++ )
			if ( playerVotes[i] != voteForGameModeIndexByPlayer[i][playerSlot] )
				return false ;
		
		return true ;
	}
	
	
	public synchronized long getAge() {
		if ( timeStarted < 0 )
			return -1 ;
		return System.currentTimeMillis() - timeStarted ;
	}
	
	
	public synchronized void setAge( long age ) {
		if ( age == -1 )
			timeStarted = -1 ;
		else
			timeStarted = System.currentTimeMillis() - age ;
	}
	
	
	//////////////////////////////////////////////////
	//
	// HELPERS!
	//
	//////////////////////////////////////////////////
	
	
	private int count( boolean [] ar ) {
		int num = 0 ;
		for ( int i = 0; i < ar.length; i++ )
			if ( ar[i] )
				num++ ;
		return num ;
	}
	
	private int findGameModeIndex( int gameMode ) {
		for ( int i = 0; i < numGameModes; i++ )
			if ( gameModes[i] == gameMode )
				return i ;
		return -1 ;
	}
	
	
	
	
	//////////////////////////////////////////////////////////////////////
	// METHODS FOR SERIALIZING
	//////////////////////////////////////////////////////////////////////
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeObject(directSocketAddress) ;
		stream.writeObject(directAddress) ;
		
		stream.writeObject(sessionNonce) ;
		stream.writeObject(nonce) ;
		
		stream.writeInt( playerSlot ) ;
		stream.writeObject( playerPersonalNonce ) ;
		stream.writeObject( playerName ) ;
		
		stream.writeObject( playerVotes ) ;
		
		stream.writeInt( maxPlayers ) ;
		stream.writeObject( playerInLobby ) ;
		stream.writeObject( lobbyName ) ;
		stream.writeLong( timeStarted ) ;
		
		stream.writeInt( numLaunches ) ;
		
		stream.writeInt( numGameModes ) ;
		stream.writeObject( gameModes ) ;
		
		stream.writeObject( playerNames ) ;
		stream.writeObject( playerPersonalNonces ) ;
		
		stream.writeObject( voteForGameModeIndexByPlayer ) ;
		
		stream.writeObject( playerStatus ) ;
		
		// Countdowns are not included!  Don't attempt to serialize a lobby
		// with active countdowns.  What would that even mean?
		
		// Delegate not included in the output, for reasons which are hopefully
		// obvious.
	}
	
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		directSocketAddress = (SocketAddress)stream.readObject() ;
		directAddress = (String)stream.readObject() ;
		
		sessionNonce = (Nonce)stream.readObject() ;
		nonce = (Nonce)stream.readObject() ;
		
		playerSlot = stream.readInt() ;
		playerPersonalNonce = (Nonce)stream.readObject() ;
		playerName = (String)stream.readObject();
		
		playerVotes = (boolean [])stream.readObject() ;
		
		maxPlayers = stream.readInt();
		playerInLobby = (boolean []) stream.readObject() ;
		lobbyName = (String)stream.readObject() ;
		timeStarted = stream.readLong() ;
		
		numLaunches = stream.readInt() ;
		
		numGameModes = stream.readInt() ;
		gameModes = (int [])stream.readObject() ;
		
		playerNames = (String [])stream.readObject() ;
		playerPersonalNonces = (Nonce [])stream.readObject() ;
		
		voteForGameModeIndexByPlayer = (boolean [][])stream.readObject() ;
		playerStatus = (int [])stream.readObject() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required Challenge structure.") ;
	}
	
	
	public synchronized Lobby newInstance() {
		Lobby l = new Lobby() ;
		cloneIntoInstance( l ) ;
		return l ;
	}
	
	protected void cloneIntoInstance( Lobby l ) {
		l.directSocketAddress = directSocketAddress ;
		l.directAddress = directAddress ;
		
		l.sessionNonce = sessionNonce ;
		l.nonce = nonce ;
		
		l.playerSlot = playerSlot ;
		l.playerPersonalNonce = playerPersonalNonce ;
		l.playerName = playerName ;
		
		l.playerVotes = playerVotes == null ? null : playerVotes.clone() ;
		
		l.maxPlayers = maxPlayers ;
		l.playerInLobby = playerInLobby == null ? null : playerInLobby.clone() ;
		l.lobbyName = lobbyName ;
		
		l.timeStarted = timeStarted ;
		
		l.numLaunches = numLaunches ;
		
		l.numGameModes = numGameModes ;
		l.gameModes = gameModes == null ? null : gameModes.clone() ;
		
		l.playerNames = playerNames == null ? null : playerNames.clone();
		l.playerPersonalNonces = playerPersonalNonces == null ? null : playerPersonalNonces.clone() ;
		
		if ( voteForGameModeIndexByPlayer != null ) {
			l.voteForGameModeIndexByPlayer = new boolean[voteForGameModeIndexByPlayer.length][] ;
			for ( int i = 0 ; i < voteForGameModeIndexByPlayer.length; i++ )
				l.voteForGameModeIndexByPlayer[i] = voteForGameModeIndexByPlayer[i] == null ? null : voteForGameModeIndexByPlayer[i].clone() ;
		}
		
		l.playerStatus = playerStatus == null ? null : playerStatus.clone() ;
		
		l.numCountdowns = numCountdowns ;
		l.countdownNumber = countdownNumber == null ? null : countdownNumber.clone() ;
		l.countdownStartTime = countdownStartTime == null ? null : countdownStartTime.clone() ;
		l.countdownDelay = countdownDelay == null ? null : countdownDelay.clone() ;
		l.countdownGameModeIndex = countdownGameModeIndex == null ? null : countdownGameModeIndex.clone() ;
		if ( countdownIncludesPlayer != null ) {
			l.countdownIncludesPlayer = new boolean[countdownIncludesPlayer.length][] ;
			for ( int i = 0; i < countdownIncludesPlayer.length; i++ )
				l.countdownIncludesPlayer[i] = countdownIncludesPlayer[i] == null ? null : countdownIncludesPlayer[i].clone() ;
		}
		l.countdownStatus = countdownStatus == null ? null : countdownStatus.clone() ;
		l.countdownTag = countdownTag == null ? null : countdownTag.clone() ;
	}
	
}
