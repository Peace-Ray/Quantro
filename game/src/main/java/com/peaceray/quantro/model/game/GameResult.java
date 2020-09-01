package com.peaceray.quantro.model.game;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Date;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.utils.ArrayOps;

/**
 * GameResult: a Serializable object which describes, in more detail than you
 * require, the state of a Game at the time it ended.
 * 
 * This class is NOT intended to allow the re-instantiation of a paused game.
 * For example, it does not retain upcoming piece information, incoming attacks,
 * etc.  It DOES hold a snapshot of the "state when it ended," but you should
 * not attempt to recreate the game from it.
 * 
 * Another note: Game, GameEvents, and GameInformation all describe an individual
 * Quantro blockfield and the events therein; in other words, a multiplayer game
 * with k players will have k Games, k GameEvents, and k GameInformations.  By
 * contrast, one GameResult object should provide a description of the *entire game*,
 * including the state seen by all players at the time it ended.
 * 
 * Finally, a guiding principle in the design of this class is forward-compatibility
 * for it and its Serialized data.  This implies that there should be a minimum
 * of internal classes (such as, e.g., GameState) that do not have this guarantee
 * of long-term Serialization.  It also implies that information may be added in later
 * revisions but not removed, and that readObject must be able to function even when
 * reading data from earlier revisions.  For the latter, our approach uses redundant
 * information: the first item written is a "version number."  This value is useful
 * if any future revision violates this convention: all data contained in version 1
 * should be written twice, followed by a boolean indicating whether more data follows
 * (i.e., 'true' if version >= 2).
 * 
 * At present, the only classes to provide forward-versioning guarantees are GameInformation
 * and GameResult.
 * 
 * Finally, GameResult does not necessarily represent the "end of the game;" some GameResults
 * represent the ultimate "result" of an entire game, but others may represent the Result of
 * a game played for a certain period of time and then paused or terminated early.
 * 
 * VERSION HISTORY:
 * 
 * VERSION 0: initial.
 * VERSION 1: Adds pieceBlockfield, pieceType.
 * 
 * @author Jake
 *
 */
public class GameResult implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 959513932084508925L;

	/**
	 * A class for building a GameResult.  Matching the forward-versioning guarantees
	 * of GameResult, one should be able to instantiate a GameResult.Builder from a
	 * partially-completed GameResult.
	 * 
	 * Each method may throw an AssertionException if the method would produce a GameResult
	 * that is inconsistent with some previous state (such as changing the number of 
	 * players).
	 * 
	 * @author Jake
	 *
	 */
	public static class Builder {
		private GameResult gr ;
		long [] startedWaiting ;
		long [] startedPausing ;
		long startedInGame ;
		long startedIdle ;
		
		
		/**
		 * Instantiates a new Builder.
		 */
		public Builder() {
			gr = new GameResult() ;
			startedWaiting = null ;
			startedPausing = null ;
			startedInGame = 0 ;
			startedIdle = 0 ;
		}
		
		/**
		 * Instantiates a Builder which will continue to update the data in the provided
		 * GameResult.
		 * 
		 * NOTE: The instance passed in will not be altered, even after this call.
		 * Instead, the Builder makes a deep-copy for its own records.  You may do
		 * whatever you want to the provided parameter without affecting the Builder,
		 * and vice-versa.
		 * 
		 * @param pendingResult
		 */
		public Builder(GameResult pendingResult) {
			gr = new GameResult( pendingResult ) ;
			if ( gr.num > 0 ) {
				startedWaiting = new long[gr.num] ;
				startedPausing = new long[gr.num] ; 
			}
			else {
				startedWaiting = null ;
				startedPausing = null ;
			}
			startedInGame = 0 ;
			startedIdle = 0 ;
		}
		
		
		public Builder rewind( GameResult previousResult ) {
			GameResult newGR = new GameResult( previousResult ) ;
			// A new rewind...
			newGR.numRewinds = gr.numRewinds + 1 ;
			
			// Keep 'time' information; a rewind doesn't erase time spent playing.
			newGR.dateStarted = gr.dateStarted ;
			newGR.dateEnded = gr.dateEnded ;
			newGR.timeIdle = gr.timeIdle ;
			newGR.timeInGame = gr.timeInGame ;
			if ( newGR.timePausedBy != null ) {
				for ( int i = 0; i < newGR.num; i++ ) {
					newGR.timePausedBy[i] = gr.timePausedBy[i] ;
					newGR.timeWaitedFor[i] = gr.timeWaitedFor[i] ;
				}
			}
			
			// Take this as our new inner GameResult.
			gr = newGR ;
			
			if ( gr.num > 0 ) {
				startedWaiting = new long[gr.num] ;
				startedPausing = new long[gr.num] ; 
			}
			else {
				startedWaiting = null ;
				startedPausing = null ;
			}
			startedInGame = 0 ;
			startedIdle = 0 ;
			
			return this ;
		}
		
		
		/**
		 * A VERY dangerous method to use!  Only use this for accessors, NEVER
		 * for mutators!
		 * 
		 * @return
		 */
		public synchronized GameResult directAccess() {
			return gr ;
		}
		
		/**
		 * Returns a GameResult instance describing the current game result as
		 * described by methods called on this Builder.
		 * 
		 * The object returned is a new instance.  You may do
		 * whatever you want to returned GameResult without affecting the Builder,
		 * and vice-versa.
		 * 
		 * build() may be called multiple times to receive independent GameResult objects.
		 * 
		 * @return
		 */
		public synchronized GameResult build() {
			long timenow = System.currentTimeMillis() ;
			if ( startedIdle > 0 ) {
				gr.timeIdle += timenow - startedIdle ;
				startedIdle = timenow ;
			}
			if ( startedInGame > 0 ) {
				gr.timeInGame += timenow - startedInGame ;
				startedInGame = timenow ;
			}
			
			return new GameResult( gr ) ;
		}
		
		public synchronized Builder terminate() {
			if ( !gr.terminusEst ) {
				gr.terminusEst = true ;
				gr.dateEnded = new Date() ;
				
				// Finalize go/idle time.
				long timenow = System.currentTimeMillis() ;
				if ( startedIdle > 0 )
					gr.timeIdle += timenow - startedIdle ;
				if ( startedInGame > 0 )
					gr.timeInGame += timenow - startedInGame ;
				startedInGame = startedIdle = 0 ;
			}
			return this ;
		}
		
		/**
		 * The game is now over.  Nothing will ever change about it.
		 * @return
		 */
		public synchronized Builder terminate(Date date) {
			assert !gr.terminusEst : "Cannot terminate more than once" ;
			assert date != null || gr.dateEnded != null : "If no date provided, must have previously set dateEnded" ;
			gr.terminusEst = true ;
			if ( date != null )
				gr.dateEnded = date ;
			// Finalize go/idle time.
			long timenow = System.currentTimeMillis() ;
			if ( startedIdle > 0 )
				gr.timeIdle += timenow - startedIdle ;
			if ( startedInGame > 0 )
				gr.timeInGame += timenow - startedInGame ;
			startedInGame = startedIdle = 0 ;
			return this ;
		}
		
		public synchronized Builder setNumberOfPlayers( int num ) {
			allocateIfNeededForNumberOfPlayers(num) ;
			return this ;
		}
		
		public synchronized Builder setGameIsGoing(boolean isgoing) {
			assert !isgoing || !gr.terminusEst: "A terminated game cannot go" ;
			return isgoing ? setGameGo() : setGameIdle() ;
		}
		
		public synchronized Builder setGameGo() {
			assert !gr.terminusEst: "A terminated game cannot go!" ;
			long timenow = System.currentTimeMillis() ;
			if ( startedIdle > 0 ) {
				gr.timeIdle += timenow - startedIdle ;
				startedIdle = 0 ;
			}
			if ( startedInGame == 0 )
				startedInGame = timenow ;
			return this ;
		}
		
		public synchronized Builder setGameIdle() {
			long timenow = System.currentTimeMillis() ;
			if ( startedInGame > 0 ) {
				gr.timeInGame += timenow - startedInGame ;
				startedInGame = 0 ;
			}
			if ( startedIdle == 0 )
				startedIdle = timenow ;
			return this ;
		}
		
		
		
		public synchronized Builder setLocalPlayer(int slot) {
			assert gr.num < 0 || gr.num > slot : "If number of players is set, must be > provided slot" ;
			assert gr.localPlayerSlot < 0 || gr.localPlayerSlot == slot : "Cannot be given multiple local player slots" ;
			gr.localPlayerSlot = slot ;
			return this ;
		}
		
		public synchronized Builder setDateStarted() {
			if ( !gr.terminusEst )
				gr.dateStarted = new Date() ;
			return this ;
		}
		
		/**
		 * Sets the date at which this game began.
		 * @param date
		 */
		public synchronized Builder setDateStarted( Date date ) {
			if ( !gr.terminusEst )
				gr.dateStarted = date ;
			return this ;
		}
		
		public synchronized Builder setDateEnded() {
			if ( !gr.terminusEst )
				gr.dateEnded = new Date() ;
			return this ;
		}
		
		public synchronized Builder setDateEnded( Date date ) {
			if ( !gr.terminusEst )
				gr.dateEnded = date ;
			return this ;
		}
		
		public synchronized Builder setWaitingFor( boolean [] waitingFor ) {
			assert !gr.terminusEst: "A terminated game cannot change waited-for players" ;
			assert gr.num > 0 : "Must have set number of players before calling any array-based methods." ;
			assert gr.num <= waitingFor.length : "Provided array must be at least as long as the number of players" ;
			long timenow = System.currentTimeMillis() ;
			for ( int i = 0; i < gr.num; i++ ) {
				// If waiting for, and weren't before, set startedWaiting
				// to the current time.  If not waiting for, but startedWaiting
				// is > 0, add the difference to total wait time and reset to 0.
				if ( waitingFor[i] && startedWaiting[i] == 0 )
					startedWaiting[i] = timenow ;
				else if ( !waitingFor[i] && startedWaiting[i] > 0 ) {
					gr.timeWaitedFor[i] += (timenow - startedWaiting[i]) ;
					startedWaiting[i] = 0 ;
				}
			}
			return this ;
		}
		
		public synchronized Builder setWaitingForNoOne() {
			assert !gr.terminusEst: "A terminated game cannot change waited-for players" ;
			assert gr.num > 0 : "Can't set waiting for no one when haven't set number of players" ;
			long timenow = System.currentTimeMillis() ;
			for ( int i = 0; i < gr.num; i++ ) {
				if ( startedWaiting[i] > 0 ) {
					gr.timeWaitedFor[i] += (timenow - startedWaiting[i]) ;
					startedWaiting[i] = 0 ;
				}
			}
			return this ;
		}
		
		public synchronized Builder setPausedBy( boolean [] pausedBy ) {
			assert !gr.terminusEst: "A terminated game cannot change paused-by players" ;
			assert gr.num > 0 : "Must have set number of players before calling any array-based methods." ;
			assert gr.num <= pausedBy.length : "Provided array must be at least as long as the number of players" ;
			long timenow = System.currentTimeMillis() ;
			for ( int i = 0; i < gr.num; i++ ) {
				// If paused by, and weren't before, set startedPausing
				// to the current time.  If not waiting for, but startedPausing
				// is > 0, add the difference to total pause time and reset to 0.
				if ( pausedBy[i] && startedPausing[i] == 0 )
					startedPausing[i] = timenow ;
				else if ( !pausedBy[i] && startedPausing[i] > 0 ) {
					gr.timePausedBy[i] += (timenow - startedPausing[i]) ;
					startedPausing[i] = 0 ;
				}
			}
			return this ;
		}
		
		public synchronized Builder setPausedByNoOne() {
			assert !gr.terminusEst: "A terminated game cannot change paused-by players" ;
			assert gr.num > 0 : "Can't set waiting for no one when haven't set number of players" ;
			long timenow = System.currentTimeMillis() ;
			for ( int i = 0; i < gr.num; i++ ) {
				if ( startedPausing[i] > 0 ) {
					gr.timePausedBy[i] += (timenow - startedPausing[i]) ;
					startedPausing[i] = 0 ;
				}
			}
			return this ;
		}
		
		/**
		 * For each 'true', sets the corresponding player to have 'won'.
		 * False values are ignored; they are considered a lack of information.
		 * @param won
		 * @return
		 */
		public synchronized Builder setWon( boolean [] won ) {
			assert gr.num > 0 : "Must have set number of players before calling any array-based methods." ;
			assert gr.num <= won.length : "Provided array must be at least as long as the number of players" ;
			for ( int i = 0; i < won.length; i++ )
				assert !won[i] || !(gr.lost[i] || gr.quit[i]) : "Player " + i + " can't have won if they lost or quit!" ;
			for ( int i = 0; i < won.length; i++ )
				if ( won[i] )
					gr.won[i] = true ;
			return this ;
		}
		
		public synchronized Builder setLost( boolean [] lost ) {
			assert gr.num > 0 : "Must have set number of players before calling any array-based methods." ;
			assert gr.num <= lost.length : "Provided array must be at least as long as the number of players" ;
			for ( int i = 0; i < lost.length; i++ )
				assert !lost[i] || !(gr.lost[i] || gr.quit[i]) : "Player " + i + " can't have lost if they won or quit!" ;
			for ( int i = 0; i < lost.length; i++ )
				if ( lost[i] )
					gr.lost[i] = true ;
			return this ;
		}
		
		public synchronized Builder setQuit( boolean [] quit ) {
			assert gr.num > 0 : "Must have set number of players before calling any array-based methods." ;
			assert gr.num <= quit.length : "Provided array must be at least as long as the number of players" ;
			for ( int i = 0; i < quit.length; i++ )
				assert !quit[i] || !(gr.lost[i] || gr.quit[i]) : "Player " + i + " can't have quit if they lost or won!" ;
			for ( int i = 0; i < quit.length; i++ )
				if ( quit[i] )
					gr.quit[i] = true ;
			return this ;
		}
		
		
		
		public synchronized Builder setWon( int playerSlot ) {
			assert playerSlot > 0 : "Must provide non-negative player slot" ;
			assert gr.num > 0 && gr.num > playerSlot : "Must have set number of players, and given playerSlot from 0 to num-1" ;
			assert !gr.quit[playerSlot] && !gr.lost[playerSlot] : "A player who quit or lost cannot have won!" ;
			gr.won[playerSlot] = true ;
			return this ;
		}
		
		
		public synchronized Builder setLost( int playerSlot ) {
			assert playerSlot > 0 : "Must provide non-negative player slot" ;
			assert gr.num > 0 && gr.num > playerSlot : "Must have set number of players, and given playerSlot from 0 to num-1" ;
			assert !gr.quit[playerSlot] && !gr.won[playerSlot] : "A player who quit or won cannot have lost!" ;
			gr.lost[playerSlot] = true ;
			return this ;
		}
		
		
		public synchronized Builder setQuit( int playerSlot ) {
			assert playerSlot > 0 : "Must provide non-negative player slot" ;
			assert gr.num > 0 && gr.num > playerSlot : "Must have set number of players, and given playerSlot from 0 to num-1" ;
			assert !gr.won[playerSlot] && !gr.lost[playerSlot] : "A player who won or lost cannot have quit!" ;
			gr.quit[playerSlot] = true ;
			return this ;
		}
		
		public synchronized Builder setClient( int playerSlot ) {
			assert playerSlot > 0 : "Must provide non-negative player slot" ;
			assert gr.num > 0 && gr.num > playerSlot : "Must have set number of players, and given playerSlot from 0 to num-1" ;
			assert gr.role[playerSlot] == GameResult.ROLE_CLIENT || gr.role[playerSlot] == GameResult.ROLE_NONE : "A player cannot have more than one role" ;
			gr.role[playerSlot] = GameResult.ROLE_CLIENT ;
			return this ;
		}
		
		public synchronized Builder setHost( int playerSlot ) {
			assert playerSlot > 0 : "Must provide non-negative player slot" ;
			assert gr.num > 0 && gr.num > playerSlot : "Must have set number of players, and given playerSlot from 0 to num-1" ;
			assert gr.role[playerSlot] == GameResult.ROLE_HOST || gr.role[playerSlot] == GameResult.ROLE_NONE : "A player cannot have more than one role" ;
			gr.role[playerSlot] = GameResult.ROLE_HOST ;
			return this ;
		}
		
		public synchronized Builder setSpectator( int playerSlot ) {
			assert playerSlot > 0 : "Must provide non-negative player slot" ;
			assert gr.num > 0 && gr.num > playerSlot : "Must have set number of players, and given playerSlot from 0 to num-1" ;
			assert gr.role[playerSlot] == GameResult.ROLE_SPECTATOR || gr.role[playerSlot] == GameResult.ROLE_NONE : "A player cannot have more than one role" ;
			gr.role[playerSlot] = GameResult.ROLE_SPECTATOR ;
			return this ;
		}
		
		public synchronized Builder setRole( int playerSlot, int role ) {
			assert playerSlot > 0 : "Must provide non-negative player slot" ;
			assert gr.num > 0 && gr.num > playerSlot : "Must have set number of players, and given playerSlot from 0 to num-1" ;
			assert gr.role[playerSlot] == role || gr.role[playerSlot] == GameResult.ROLE_NONE : "A player cannot have more than one role" ;
			assert GameResult.MIN_ROLE <= role && role <= GameResult.MAX_ROLE : "Role provided must be in {" + GameResult.MIN_ROLE + ", ..., " + GameResult.MAX_ROLE + "}" ;
			gr.role[playerSlot] = role ;
			return this ;
		}
		
		public synchronized Builder setRoles( int [] roles ) {
			assert gr.num > 0 : "Must have set number of players before calling any array-based methods." ;
			assert gr.num <= roles.length : "Provided array must be at least as long as the number of players" ;
			for ( int i = 0; i < gr.num; i++ ) {
				assert gr.role[i] == roles[i] || gr.role[i] == GameResult.ROLE_NONE || roles[i] == GameResult.ROLE_NONE : "A player cannot have more than one role" ;
				assert GameResult.MIN_ROLE <= roles[i] && roles[i] <= GameResult.MAX_ROLE : "Role provided must be in {" + GameResult.MIN_ROLE + ", ..., " + GameResult.MAX_ROLE + "}" ;
			}
			for ( int i = 0; i < gr.num; i++ ) {
				if ( roles[i] != GameResult.ROLE_NONE )
					gr.role[i] = roles[i] ;
			}
			return this ; 
		}
		
		/**
		 * Sets names and personal nonces for players.
		 * @param names
		 * @param pnonces
		 * @return
		 */
		public synchronized Builder setPlayers( String [] names, Nonce [] pnonces ) {
			return this.setNames(names).setPersonalNonces(pnonces) ;
		}
		
		/**
		 * Sets the player names to those in the provided array.
		 * @param names
		 * @return
		 */
		public synchronized Builder setNames( String [] names ) {
			assert gr.num > 0 : "Must have set number of players before calling any array-based methods." ;
			assert gr.num <= names.length : "Provided array must be at least as long as the number of players" ;
			for ( int i = 0; i < gr.num; i++ )
				gr.playerName[i] = names[i] ;
			return this ;
		}
		
		public synchronized Builder setPersonalNonces( Nonce [] nonces ) {
			assert gr.num > 0 : "Must have set number of players before calling any array-based methods." ;
			assert gr.num <= nonces.length : "Provided array must be at least as long as the number of players" ;
			for ( int i = 0; i < gr.num; i++ )
				gr.personalNonce[i] = nonces[i] ;
			return this ;
		}
		
		public synchronized Builder setName( int playerSlot, String name ) {
			assert 0 <= playerSlot && playerSlot < gr.num : "Must specify a player slot in {0,...,numPlayers}" ;
			gr.playerName[playerSlot] = name ;
			return this ;
		}
		
		public synchronized Builder setPersonalNonce( int playerSlot, Nonce pnonce ) {
			assert 0 <= playerSlot && playerSlot < gr.num : "Must specify a player slot in {0,...,numPlayers}" ;
			gr.personalNonce[playerSlot] = pnonce ;
			return this ;
		}
		
		public synchronized Builder setNonce( Nonce nonce ) {
			gr.nonce = nonce ;
			return this ;
		}
		
		public synchronized Builder setGameInformation( GameInformation [] ginfo ) {
			assert gr.num > 0 : "Must have set number of players before calling any array-based methods." ;
			assert gr.num <= ginfo.length : "Provided array must be at least as long as the number of players" ;
			for ( int i = 0; i < gr.num; i++ )
				gr.ginfo[i] = new GameInformation().finalizeConfiguration().takeVals(ginfo[i]) ;
			return this ;
		}
		
		public synchronized Builder setBlockField( byte [][][][] blockfield ) {
			assert gr.num > 0 : "Must have set number of players before calling any array-based methods." ;
			assert gr.num <= blockfield.length : "Provided array must be at least as long as the number of players" ;
			assertArrayOfBlockfieldsHaveSameDimensions(blockfield) ;
			allocateIfNeededForBlockfieldDimensions(blockfield[0].length, blockfield[0][0].length, blockfield[0][0][0].length ) ;
			for ( int b = 0; b < blockfield.length; b++ )
				ArrayOps.copyInto(blockfield[b], gr.blockfield[b]) ;
			return this ;
		}
		
		public synchronized Builder setPieceBlockField( int [] ptype, byte [][][][] pieceBlockfield ) {
			assert gr.num > 0 : "Must have set number of players before calling any array-based methods." ;
			assert gr.num <= pieceBlockfield.length : "Provided array must be at least as long as the number of players" ;
			assertArrayOfBlockfieldsHaveSameDimensions(pieceBlockfield) ;
			allocateIfNeededForBlockfieldDimensions(pieceBlockfield[0].length, pieceBlockfield[0][0].length, pieceBlockfield[0][0][0].length ) ;
			for ( int b = 0; b < pieceBlockfield.length; b++ ) {
				ArrayOps.copyInto(pieceBlockfield[b], gr.pieceBlockfield[b]) ;
				gr.pieceType[b] = ptype[b] ;
			}
			return this ;
		}
		
		
		public synchronized Builder setGameSettings( int playerSlot, GameSettings gs ) {
			assert gr.num > playerSlot && playerSlot >= 0 : "Must give 0 <= playerSlot < total number of players" ;
			gr.gamesettings[playerSlot] = new GameSettings(gs) ;
			return this ;
		}
		
		
		public synchronized Builder setGameInformation( int playerSlot, GameInformation ginfo ) {
			assert gr.num > playerSlot && playerSlot >= 0 : "Must give 0 <= playerSlot < total number of players" ;
			gr.ginfo[playerSlot] = new GameInformation().finalizeConfiguration().takeVals(ginfo) ;
			return this ;
		}
		
		public synchronized Builder setBlockField( int playerSlot, byte [][][] blockfield ) {
			assert gr.num > playerSlot && playerSlot >= 0 : "Must give 0 <= playerSlot < total number of players" ;
			assertBlockfieldIsRectangularPrism(blockfield) ;
			allocateIfNeededForBlockfieldDimensions( blockfield.length, blockfield[0].length, blockfield[0][0].length ) ;
			ArrayOps.copyInto(blockfield, gr.blockfield[playerSlot]) ;
			return this ;
		}
		
		public synchronized Builder setPieceBlockField( int playerSlot, int pieceType, byte [][][] pieceBlockfield ) {
			assert gr.num > playerSlot && playerSlot >= 0 : "Must give 0 <= playerSlot < total number of players" ;
			assertBlockfieldIsRectangularPrism(pieceBlockfield) ;
			allocateIfNeededForBlockfieldDimensions( pieceBlockfield.length, pieceBlockfield[0].length, pieceBlockfield[0][0].length ) ;
			ArrayOps.copyInto(pieceBlockfield, gr.pieceBlockfield[playerSlot]) ;
			gr.pieceType[playerSlot] = pieceType ;
			return this ;
		}
		
		
		public synchronized Builder setTotalTimeInGameTicks( int playerSlot, long time ) {
			assert gr.num > playerSlot && playerSlot >= 0 : "Must give 0 <= playerSlot < total number of players" ;
			gr.timeInGameTicks[playerSlot] = time ;
			return this ;
		}
		
		/**
		 * Checks, via assertions, that the provided array
		 * 1. is not 'null' in any dimension, and
		 * 2. represents a stack of identically-sized "rect prisms"; e.g.,
		 * 		bf[i].length == bf[j].length, bf[i][j][k].length == bf[l][m][n].length, etc.
		 * @param bf
		 */
		private synchronized void assertArrayOfBlockfieldsHaveSameDimensions( byte [][][][] bf ) {
			assert bf != null : "Must provide a non-null blockfield" ;
			assert bf.length > 0 && bf[0] != null : "Must provide a non-null blockfield" ;
			assert bf[0].length > 0 && bf[0][0] != null : "Must provide a non-null blockfield" ;
			assert bf[0][0].length > 0 && bf[0][0][0] != null : "Must provide a non-null blockfield" ;
			
			int B = bf.length ;
			int R = bf[0].length ;
			
			try {
				for ( int b = 0; b < B; b++ ) {
					assert bf[b] != null : "Must provide a non-null blockfield" ;
					assert bf[b].length == R : "All blockfields in array must have same number of rows" ;
					assertBlockfieldIsRectangularPrism( bf[b] ) ;
				}
			} catch( NullPointerException e ) {
				assert false : e.getMessage() ;
			} catch (ArrayIndexOutOfBoundsException e ) {
				assert false : e.getMessage() ;
			}
		}
		
		private synchronized void assertBlockfieldIsRectangularPrism( byte [][][] bf ) {
			assert bf != null : "Must provide a non-null blockfield" ;
			assert bf.length > 0 && bf[0] != null : "Must provide a non-null blockfield" ;
			assert bf[0].length > 0 && bf[0][0] != null : "Must provide a non-null blockfield" ;
			assert bf[0][0].length > 0 ;
			
			int R = bf.length ;
			int C = bf[0].length ;
			int D = bf[0][0].length ;
			try {
				for ( int r = 0; r < R; r++ ) {
					assert bf[r] != null : "Must provide a non-null blockfield" ;
					assert bf[r].length == C : "Not the right number of columns" ;
					for ( int c = 0; c < C; c++ ) {
						assert bf[r][c] != null : "Must provide a non-null blockfield" ;
						assert bf[r][c].length == D : "Not the right depth" ;
					}
				}
			} catch( NullPointerException e ) {
				assert false : e.getMessage() ;
			} catch (ArrayIndexOutOfBoundsException e ) {
				assert false : e.getMessage() ;
			}
		}
		
		private synchronized void allocateIfNeededForBlockfieldDimensions( int R, int C, int D ) {
			assert R > 0 && C > 0 && D > 0 : "Must provide positive dimensions" ;
			for ( int b = 0; b < gr.blockfield.length; b++ ) {
				// either null, or exactly these dimensions.
				try {
					assert gr.blockfield[b] == null || (gr.blockfield[b].length == R && gr.blockfield[b][0].length == C && gr.blockfield[b][0][0].length == D) : "Blockfield dimensions are inconsistent" ;
					if ( gr.blockfield[b] == null )
						gr.blockfield[b] = new byte[R][C][D] ;
					assert gr.pieceBlockfield[b] == null || (gr.pieceBlockfield[b].length == R && gr.pieceBlockfield[b][0].length == C && gr.pieceBlockfield[b][0][0].length == D) : "pieceBlockfield dimensions are inconsistent" ;
					if ( gr.pieceBlockfield[b] == null )
						gr.pieceBlockfield[b] = new byte[R][C][D] ;
					
				} catch( NullPointerException e ) {
					assert false : e.getMessage() ;
				} catch (ArrayIndexOutOfBoundsException e ) {
					assert false : e.getMessage() ;
				}
			}
		}
		
		
		/**
		 * Allocates the arrays which represent the players, if needed.
		 * Those arrays that are already the correct length will be left 
		 * alone.  Throws an assertion error if any structure has already
		 * been allocated for a different length.
		 * @param num
		 */
		private synchronized void allocateIfNeededForNumberOfPlayers(int num) {
			assert gr.num < 0 || gr.num == num : "Can't change number of players" ;
			assert gr.gamesettings == null || gr.gamesettings.length == num : "Can't change number of players" ;
			assert gr.ginfo == null || gr.ginfo.length == num : "Can't change number of players" ;
			assert gr.blockfield == null || gr.blockfield.length == num : "Can't change number of players" ;
			assert gr.pieceBlockfield == null || gr.pieceBlockfield.length == num : "Can't change number of players" ;
			assert gr.pieceType == null || gr.pieceType.length == num : "Can't change number of players."  ;
			assert gr.personalNonce == null || gr.personalNonce.length == num : "Can't change number of players" ;
			assert gr.playerName == null || gr.playerName.length == num : "Can't change number of players" ;
			assert gr.won == null || gr.won.length == num : "Can't change number of players" ;
			assert gr.lost == null || gr.lost.length == num : "Can't change number of players" ;
			assert gr.quit == null || gr.quit.length == num : "Can't change number of players" ;
			assert gr.role == null || gr.role.length == num : "Can't change number of players" ;
			assert gr.timeWaitedFor == null || gr.timeWaitedFor.length == num : "Can't change number of players" ;
			assert gr.timePausedBy == null || gr.timePausedBy.length == num : "Can't change number of players" ;
			assert gr.timeInGameTicks == null || gr.timeInGameTicks.length == num : "Can't change number of players"  ;
			
			// Our own structures
			if ( startedWaiting == null ) {
				startedWaiting = new long[num] ;
				startedPausing = new long[num] ;
			}
			
			// Allocate.
			if ( gr.gamesettings == null ) gr.gamesettings = new GameSettings[num] ;
			if ( gr.ginfo == null ) gr.ginfo = new GameInformation[num] ;
			if ( gr.blockfield == null ) gr.blockfield = new byte[num][][][] ;
			if ( gr.pieceBlockfield == null ) gr.pieceBlockfield = new byte[num][][][] ;
			if ( gr.pieceType == null ) gr.pieceType = new int[num] ;
			if ( gr.personalNonce == null ) gr.personalNonce = new Nonce[num] ;
			if ( gr.playerName == null ) gr.playerName = new String[num] ;
			if ( gr.won == null ) gr.won = new boolean[num] ;
			if ( gr.lost == null ) gr.lost = new boolean[num] ;
			if ( gr.quit == null ) gr.quit = new boolean[num] ;
			if ( gr.role == null ) gr.role = new int[num] ;
			if ( gr.timeWaitedFor == null ) gr.timeWaitedFor = new long[num] ;
			if ( gr.timePausedBy == null ) gr.timePausedBy = new long[num] ;
			if ( gr.timeInGameTicks == null ) gr.timeInGameTicks = new long[num] ;
			gr.num = num ;
		}
	}
	
	public static final int VERSION = 3 ;
	// VERSION 1 adds "pieceBlockfield", for holding the falling piece (if any).
	// VERSION 2 adds "gamesettings", for holding the settings with which the game was constructed.
	// VERSION 3 adds "timeInGameTicks", for holding the milliseconds passed as represented
	//			in-game (not from our exterior "app-level" control.
	
	// was the game completely finished at the time this object was constructed?
	// Note: if a player leaves the game early, but the game should continue
	// without them, this would be false.  If a player leaves the game at doing
	// so causes the game to end, this would be true.
	private boolean terminusEst ;
	
	private GameSettings [] gamesettings ;
	private GameInformation [] ginfo ;
	private byte [][][][] blockfield ;
	private byte [][][][] pieceBlockfield ;		// NEW in VERSION 1.
	private int [] pieceType ;					// NEW in VERSION 1. the piece type represented in the blockfield; -1 if none.
	// The state of each game at the time it ended.
	
	// The nonce used to launch the game, if it exists.
	private Nonce nonce ;
	
	// Who played these games?
	private Nonce [] personalNonce ;
	private int num ;

	private String [] playerName ;
	
	// What was the game result - win, lose, disconnect?
	private boolean [] won ;
	private boolean [] lost ;
	private boolean [] quit ;
	
	// Who hosted?
	public static int ROLE_NONE = 0 ;
	public static int ROLE_CLIENT = 1 ;
	public static int ROLE_HOST = 2 ;
	public static int ROLE_SPECTATOR = 3 ;
	private static int MIN_ROLE = 0 ;
	private static int MAX_ROLE = 3 ;
	private int [] role ;
	
	// Time stats.
	private Date dateStarted ;
	private Date dateEnded ;
	private long timeInGame ;		// in milliseconds; the actual amount of time the game spent "in motion"
	private long timeIdle ;			// in milliseconds; the amount of time (this Activity) was idle between start and end.
	
	private long [] timeInGameTicks ;	// in milliseconds; the amount of time (our Game object) spent "in motion."
									// expected to differ slightly from timeInGame, which is "Activity-level"
									// (using a different Thread than the actual ticking game).
	
	// Connection stats.  The host has the most accurate version of this,
	// but other players should do their best to keep track.
	private long [] timeWaitedFor ;
	private long [] timePausedBy ;
	
	// This object exists in a Process which may or may not
	// have participated in the game described.  If >= 0,
	// represents an index into the above arrays.
	private int localPlayerSlot ;
	
	// This object represents the result of a single game, from start to
	// finish.  However, both Endurance and Progression modes have an option
	// to "restart" a game from a checkpoint.  In Endurance, this represents
	// an entirely new game: starting over from scratch.  As such, a new GameResult.Builder
	// should be allocated on this event.
	// In Progression, though, this restores an earlier checkpoint from the game already
	// in progress (the same as "replay level").  This variables stores the number of
	// rewinds performed over the course of the game.
	private int numRewinds ;
	
	private GameResult() {
		terminusEst = false ;
		
		num = -1 ;
		gamesettings = null ;
		ginfo = null ;
		blockfield = null ;
		pieceBlockfield = null ;
		pieceType = null ;
		// The state of each game at the time it ended.
		
		// The nonce used to launch the game, if it exists.
		nonce = null ;
		
		// Who played these games?
		personalNonce = null ;
		playerName = null ;
		
		// What was the game result - win, lose, disconnect?
		won = null ;
		lost = null ;
		quit = null ;
		
		// Who hosted?
		role = null ;
		
		// Time stats.
		dateStarted = new Date() ;
		dateEnded = null ;
		timeInGame = 0 ;		// in milliseconds; the actual amount of time the game spent "in motion"
		timeIdle = 0 ;			// in milliseconds; the amount of time (this Activity) was idle between start and end.
		
		timeInGameTicks = null ;	// in milliseconds; the time spent in game ticks.
		
		// Connection stats.  The host has the most accurate version of this,
		// but other players should do their best to keep track.
		timeWaitedFor = null ;
		timePausedBy = null ;
		
		// This object exists in a Process which may or may not
		// have participated in the game described.  If >= 0,
		// represents an index into the above arrays.
		localPlayerSlot = -1 ;
		
		numRewinds = 0 ;
	}
	
	
	private GameResult( GameResult orig ) {
		terminusEst = orig.terminusEst ;
		
		num = orig.num ;
		if ( orig.gamesettings == null )
			gamesettings = null ;
		else {
			gamesettings = new GameSettings[orig.gamesettings.length] ;
			for ( int i = 0; i < num; i++ )
				if ( orig.gamesettings[i] != null )
					gamesettings[i] = new GameSettings( orig.gamesettings[i] ) ;
		}
		
		if ( orig.ginfo == null )
			ginfo = null ;
		else {
			ginfo = new GameInformation[orig.ginfo.length] ;
			for ( int i = 0; i < num; i++ )
				if ( orig.ginfo[i] != null )
					ginfo[i] = new GameInformation().finalizeConfiguration().takeVals(orig.ginfo[i]) ;
		}
		if ( orig.blockfield == null )
			blockfield = null ;
		else {
			blockfield = new byte[num][][][] ;
			if ( orig.blockfield[0] != null ) {
				// have allocated actual blockfield.s
				for ( int i = 0; i < num; i++ ) {
					blockfield[i] = ArrayOps.allocateToMatchDimensions(blockfield[i], orig.blockfield[i]) ;
					ArrayOps.copyInto( orig.blockfield[i], blockfield[i] ) ;
				}
			}
		}
		if ( orig.pieceBlockfield == null )
			pieceBlockfield = null ;
		else {
			pieceBlockfield = new byte[num][][][] ;
			if ( orig.pieceBlockfield[0] != null ) {
				// have allocated actual blockfield.s
				for ( int i = 0; i < num; i++ ) {
					pieceBlockfield[i] = ArrayOps.allocateToMatchDimensions(pieceBlockfield[i], orig.pieceBlockfield[i]) ;
					ArrayOps.copyInto( orig.pieceBlockfield[i], pieceBlockfield[i] ) ;
				}
			}
		}
		if ( orig.pieceType == null )
			pieceType = null ;
		else {
			pieceType = new int[num] ;
			for ( int i = 0; i < num; i++ )
				pieceType[i] = orig.pieceType[i] ;
		}
		
		// The nonce used to launch the game, if it exists.
		nonce = orig.nonce ;
		
		// Who played these games?
		if ( orig.personalNonce == null )
			personalNonce = null ;
		else {
			personalNonce = new Nonce[num] ;
			for ( int i = 0; i < num; i++ )
				personalNonce[i] = orig.personalNonce[i] ;
		}
		if ( orig.playerName == null )
			playerName = null ;
		else {
			playerName = new String[num] ;
			for ( int i = 0; i < num; i++ )
				playerName[i] = orig.playerName[i] ;
		}
		
		// What was the game result - win, lose, disconnect?
		if ( orig.won == null ) {
			won = null ;
			lost = null ;
			quit = null ;
		}
		else {
			won = new boolean[num] ;
			lost = new boolean[num] ;
			quit = new boolean[num] ;
			for ( int i = 0; i < num; i++ ) {
				won[i] = orig.won[i] ;
				lost[i] = orig.lost[i] ;
				quit[i] = orig.quit[i] ;
			}
		}
		
		
		// Who hosted?
		if ( orig.role == null )
			role = null ;
		else {
			role = new int[num] ;
			for ( int i = 0; i < num; i++ )
				role[i] = orig.role[i] ;
		}
		
		// Time stats.
		dateStarted = orig.dateStarted == null ? null : (Date)orig.dateStarted.clone() ;
		dateEnded = orig.dateEnded == null ? null : (Date)orig.dateEnded.clone() ;
		timeInGame = orig.timeInGame ;
		timeIdle = orig.timeIdle ;
		
		if ( orig.timeInGameTicks == null )
			timeInGameTicks = null ;
		else {
			timeInGameTicks = new long[num] ;
			for ( int i = 0; i < num; i++ )
				timeInGameTicks[i] = orig.timeInGameTicks[i] ;
		}
		
		// Connection stats.  The host has the most accurate version of this,
		// but other players should do their best to keep track.
		if ( orig.timeWaitedFor == null ) {
			timeWaitedFor = timePausedBy = null ;
		}
		else {
			timeWaitedFor = new long[num] ;
			timePausedBy = new long[num] ;
			for ( int i = 0; i < num; i++ ) {
				timeWaitedFor[i] = orig.timeWaitedFor[i] ;
				timePausedBy[i] = orig.timePausedBy[i] ;
			}
		}
		
		// This object exists in a Process which may or may not
		// have participated in the game described.  If >= 0,
		// represents an index into the above arrays.
		localPlayerSlot = orig.localPlayerSlot ;
		
		numRewinds = orig.numRewinds ;
	}
	
	////////////////////////////////////////////////////////////////////////////
	//
	// ACCESSORS
	//
	// A few semi-complex accessors for the above fields.
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	// TODO: Add accessors
	
	public int getNumberOfPlayers() {
		return num ;
	}
	
	public GameSettings getGameSettings( int player ) {
		if ( gamesettings == null || player < 0 || gamesettings.length <= player || gamesettings[player] == null )
			return null ;
		return new GameSettings( gamesettings[player] ) ;
	}
	
	public GameSettings getGameSettingsImmutable( int player ) {
		 if ( gamesettings == null || player < 0 || gamesettings.length <= player )
			 return null ;
		 return gamesettings[player] ;
	}
	
	public GameInformation getGameInformation( int player ) {
		if ( ginfo == null || player < 0 || ginfo.length <= player || ginfo[player] == null )
			return null ;
		return new GameInformation().finalizeConfiguration().takeVals(ginfo[player]) ;
	}
	
	public GameInformation getGameInformationImmutable ( int player ) {
		if ( ginfo == null || ginfo[player] == null )
			return null ;
		return ginfo[player] ;
	}
	
	public int getLocalPlayerSlot() {
		return localPlayerSlot ;
	}
	
	public int getNumRewinds() {
		return numRewinds ;
	}
	
	public byte [][][] getBlockFieldReference( int playerSlot ) {
		if ( blockfield == null || playerSlot < 0 || playerSlot >= blockfield.length )
			return null ;
		return blockfield[playerSlot] ;
	}
	
	public byte [][][] getPieceBlockFieldReference( int playerSlot ) {
		if ( pieceBlockfield == null || playerSlot < 0 || playerSlot >= pieceBlockfield.length )
			return null ;
		return pieceBlockfield[playerSlot] ;
	}
	
	public int getPieceType( int playerSlot ) {
		if ( pieceType == null || playerSlot < 0 || playerSlot >= pieceType.length )
			return -1 ;
		return pieceType[playerSlot] ;
	}
	
	public Nonce getNonce() {
		return nonce ;
	}
	
	public int getRows( int playerSlot ) {
		if ( blockfield == null )
			return 0 ;
		if ( playerSlot < 0 || playerSlot >= blockfield.length )
			return 0 ;
		if ( blockfield[playerSlot] == null || blockfield[playerSlot].length == 0 )
			return 0 ;
		if ( blockfield[playerSlot][0] == null )
			return 0 ;
		return blockfield[playerSlot][0].length ;
	}
	
	public int getCols( int playerSlot ) {
		if ( blockfield == null )
			return 0 ;
		if ( playerSlot < 0 || playerSlot >= blockfield.length )
			return 0 ;
		if ( blockfield[playerSlot] == null || blockfield[playerSlot].length == 0 )
			return 0 ;
		if ( blockfield[playerSlot][0] == null || blockfield[playerSlot][0].length == 0 )
			return 0 ;
		if ( blockfield[playerSlot][0][0] == null )
			return 0 ;
		return blockfield[playerSlot][0][0].length ;
	}
	
	public boolean getWon( int playerSlot ) {
		if ( won != null && playerSlot >= 0 && playerSlot < won.length )
			return won[playerSlot] ;
		return false ;
	}
	
	public boolean getLost( int playerSlot ) {
		if ( lost != null && playerSlot >= 0 && playerSlot < lost.length )
			return lost[playerSlot] ;
		return false ;
	}
	
	public boolean getQuit( int playerSlot ) {
		if ( quit != null && playerSlot >= 0 && playerSlot < quit.length )
			return quit[playerSlot] ;
		return false ;
	}
	
	public String getName( int playerSlot ) {
		if ( playerName != null && playerSlot >= 0 && playerSlot < playerName.length )
			return playerName[playerSlot] ;
		return null ;
	}
	
	public Date getDateStarted() {
		return dateStarted ;
	}
	
	public Date getDateEnded() {
		return dateEnded ;
	}
	
	public long getTimeInGame() {
		return timeInGame ;
	}
	
	public long getTimeIdle() {
		return timeIdle ;
	}
	
	public long getTimeInGameTicks( int playerSlot ) {
		return timeInGameTicks[playerSlot] ;
	}
	
	public boolean getTerminated() {
		return terminusEst ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// BACKWARDS COMPATABILITY
	//
	// This method is called when an object is read from a stream that has a version
	// number below VERSION.  This method is the last operation of the deserialization;
	// in other words, all appropriate fields for 'version' have been set.
	//
	////////////////////////////////////////////////////////////////////////////
	
	private void setDefaultsForVersion( int version ) {
		// VERSION 1 adds pieceType, pieceBlockfield.
		if ( version < 1 ) {
			if ( num > -1 ) {
				pieceType = new int[num] ;
				for ( int i = 0; i < num; i++ ) {
					pieceType[i] = -1 ;
				}
				
				if ( blockfield == null )
					pieceBlockfield = null ;
				else if ( blockfield[0] == null )
					pieceBlockfield = new byte[num][][][] ;
				else
					pieceBlockfield = ArrayOps.allocateToMatchDimensions(pieceBlockfield, blockfield) ;
			}
		}
		
		// VERSION 2 adds gamesettings.
		if ( version < 2 ) {
			if ( num > -1 )
				gamesettings = new GameSettings[num] ;
		}
		
		// VERSION 3 adds timeInGameTicks.  Estimate from timeInGame.
		if ( version < 3 ) {
			if ( num > -1 ) {
				timeInGameTicks = new long[num] ;
				for ( int i = 0; i < num; i++ ) {
					timeInGameTicks[i] = timeInGame ;
				}
			}
		}
	}
	
	
	////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE 
	//
	// These methods provide the implementation of the Serializable
	// interface.
	//
	////////////////////////////////////////////////////////////////
	
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		// write version
		stream.writeInt(VERSION) ;
		
		stream.writeBoolean(terminusEst) ;
		
		stream.writeInt(num) ;
		stream.writeObject(ginfo) ;
		stream.writeObject(blockfield) ;
		// The state of each game at the time it ended.
		
		// The nonce used to launch the game, if it exists.
		stream.writeObject(nonce) ;
		
		// Who played these games?
		stream.writeObject(personalNonce) ;
		stream.writeObject(playerName) ;
		
		// What was the game result - win, lose, disconnect?
		stream.writeObject(won) ;
		stream.writeObject(lost) ;
		stream.writeObject(quit) ;
		
		// Who hosted?
		stream.writeObject(role) ;
		
		// Time stats.
		stream.writeObject(dateStarted) ;
		stream.writeObject(dateEnded) ;
		stream.writeLong(timeInGame) ;
		stream.writeLong(timeIdle) ;
		
		// Connection stats.  The host has the most accurate version of this,
		// but other players should do their best to keep track.
		stream.writeObject(timeWaitedFor) ;
		stream.writeObject(timePausedBy) ;
		
		// This object exists in a Process which may or may not
		// have participated in the game described.  If >= 0,
		// represents an index into the above arrays.
		stream.writeInt(localPlayerSlot) ;
		
		// Rewinds?
		stream.writeInt(numRewinds) ;
		
		// VERSION 0 END
		// write boolean: there is more
		stream.writeBoolean(true) ;
		
		// Write version 1 data: falling piece block field.
		stream.writeObject( pieceBlockfield ) ;
		stream.writeObject( pieceType ) ;
		
		// VERSION 1 END
		stream.writeBoolean(true) ;
		
		stream.writeObject( gamesettings ) ;
		
		// VERSION 2 END
		stream.writeBoolean(true) ;
		
		// Write version 3 data: time in game ticks.
		stream.writeObject( timeInGameTicks ) ;
		
		// VERSION 3 END
		stream.writeBoolean(false) ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int v = stream.readInt() ;
		// should be 3.
		
		terminusEst = stream.readBoolean() ;
		
		num = stream.readInt() ;
		ginfo = (GameInformation[])stream.readObject() ;
		blockfield = (byte [][][][])stream.readObject() ;
		// The state of each game at the time it ended.
		
		// The nonce used to launch the game, if it exists.
		nonce = (Nonce)stream.readObject() ;
		
		// Who played these games?
		personalNonce = (Nonce[])stream.readObject() ;
		playerName = (String[])stream.readObject() ;
		
		// What was the game result - win, lose, disconnect?
		won = (boolean[])stream.readObject() ;
		lost = (boolean[])stream.readObject() ;
		quit = (boolean[])stream.readObject() ;
		
		// Who hosted?
		role = (int[])stream.readObject() ;
		
		// Time stats.
		dateStarted = (Date)stream.readObject() ;
		dateEnded = (Date)stream.readObject() ;
		timeInGame = stream.readLong() ;
		timeIdle = stream.readLong() ;
		
		// Connection stats.  The host has the most accurate version of this,
		// but other players should do their best to keep track.
		timeWaitedFor = (long[])stream.readObject() ;
		timePausedBy = (long[])stream.readObject() ;
		
		// This object exists in a Process which may or may not
		// have participated in the game described.  If >= 0,
		// represents an index into the above arrays.
		localPlayerSlot = stream.readInt();
		
		numRewinds = stream.readInt() ;
		
		// read boolean: finished ?
		boolean more = stream.readBoolean();
		if ( !more ) {
			setDefaultsForVersion(v) ;
			return ;
		}
		
		// VERSION 1: piece blockfield.
		pieceBlockfield = (byte [][][][])stream.readObject() ;
		pieceType = (int [])stream.readObject() ;
		
		// read boolean
		more = stream.readBoolean() ;
		if ( !more ) {
			setDefaultsForVersion(v) ;
			return ;
		}
		
		// VERSION 2: gamesettings.
		gamesettings = (GameSettings[])stream.readObject() ; 
		
		// read boolean
		more = stream.readBoolean() ;
		if ( !more ) {
			setDefaultsForVersion(v) ;
			return ;
		}
		
		// VERSION 3: timeInGameTicks
		timeInGameTicks = (long [])stream.readObject() ;
		
		// read boolean
		stream.readBoolean() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
	}
	
	
	public GameResult qUpconvert() {
		GameResult gr = new GameResult(this) ;
		
		// upconvert pieceType
		if ( gr.pieceType != null ) {
			for ( int i = 0; i < gr.pieceType.length; i++ ) {
				gr.pieceType[i] = PieceCatalog.qUpconvert(gr.pieceType[i]) ;
			}
		}
		
		return gr ;
	}
	
}
