package com.peaceray.quantro.model.communications;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.peaceray.quantro.communications.Message;
import com.peaceray.quantro.model.descriptors.versioned.ActionCycleStateDescriptor;
import com.peaceray.quantro.model.descriptors.versioned.AttackDescriptor;
import com.peaceray.quantro.model.game.Game;
import com.peaceray.quantro.model.game.GameEvents;
import com.peaceray.quantro.utils.ByteArrayOps;

public class GameMessage extends Message {

	@Override
	protected int BUFFER_SIZE() {
		return 4096 ;
	}
	
	public static final byte TYPE_FULL_SYNCHRONIZATION		= Message.MIN_TYPE_IN_SUBCLASS 		+  0 ;		// 0
	public static final byte TYPE_WAITING_FOR_PLAYERS		= Message.MIN_TYPE_IN_SUBCLASS 		+  1 ;
	public static final byte TYPE_PAUSED_BY_PLAYERS			= Message.MIN_TYPE_IN_SUBCLASS 		+  2 ;
	public static final byte TYPE_GO						= Message.MIN_TYPE_IN_SUBCLASS		+  3 ;
	public static final byte TYPE_GAME_OVER					= Message.MIN_TYPE_IN_SUBCLASS		+  4 ;
	public static final byte TYPE_MOVE_QUEUE				= Message.MIN_TYPE_IN_SUBCLASS		+  5 ;		// 5
	public static final byte TYPE_NEXT_CYCLE				= Message.MIN_TYPE_IN_SUBCLASS		+  6 ;
	public static final byte TYPE_NEXT_CYCLE_REQUEST		= Message.MIN_TYPE_IN_SUBCLASS		+  7 ;
	public static final byte TYPE_INCOMING_ATTACK			= Message.MIN_TYPE_IN_SUBCLASS		+  8 ;
	public static final byte TYPE_PAUSE						= Message.MIN_TYPE_IN_SUBCLASS		+  9 ;
	public static final byte TYPE_UNPAUSE					= Message.MIN_TYPE_IN_SUBCLASS		+ 10 ;
	
	public static final byte TYPE_REALTIME_UPDATE			= Message.MIN_TYPE_IN_SUBCLASS		+ 11 ;
	
	public static final byte TYPE_FULL_SYNCHRONIZATION_APPLIED	= Message.MIN_TYPE_IN_SUBCLASS	+ 12 ;
	
	// player status updates (for >2 player games)
	public static final byte TYPE_PLAYER_WON				= Message.MIN_TYPE_IN_SUBCLASS		+ 13 ;
	public static final byte TYPE_PLAYER_LOST				= Message.MIN_TYPE_IN_SUBCLASS		+ 14 ;
	public static final byte TYPE_PLAYER_IS_SPECTATOR 		= Message.MIN_TYPE_IN_SUBCLASS		+ 15 ;
	
	
	Serializable [] fullSynchronization ;
	long messageId ;	// Helps to distinguish messages that have
						// the same type, but for which we need confirmation
						// of arrival of a particular type (for instance: full
						// synchronization messages, on which the game will block
						// until confirmation is received).
	
	protected int [] playerSlotArray ;
	int playerSlotArrayLength ;
	
	byte [] moveQueue ;
	int moveQueueLength ;
	
	long millisTicked ;
	double displacedRows ;
	double displacedSeconds ;
	
	ActionCycleStateDescriptor.Update actionCycleStateDescriptorUpdate ;
	AttackDescriptor attackDescriptor ;
	
	
	public GameMessage() {
		super() ;
		
		// We allocate these when needed; the first time they are
		// used, and when we receive an object that is incompatible
		// with the previous allocation (e.g. rows and cols don't match).
		fullSynchronization = null ;
		moveQueue = new byte[1024] ;
		playerSlotArray = null ;
		actionCycleStateDescriptorUpdate = null ;
		attackDescriptor = null ;
		
		messageId = 0 ;
	}

	
	@Override
	protected void nullOutsideReferences() {
		super.nullOutsideReferences() ;
		if ( fullSynchronization != null ) {
			for ( int i = 0; i < fullSynchronization.length; i++ )
				fullSynchronization[i] = null ;
		}
		
		// some basics; this improves efficiency of setAs().
		moveQueueLength = 0 ;
		playerSlotArrayLength = 0 ;
	}
	
	
	/**
	 * All messages are written with a prefix (type and length),
	 * currently represented as 5 bytes.  Type is a byte, length
	 * a 4-byte encoding of an int.  For special message types,
	 * we need to know the length so it can be written.  This method
	 * will be called for Messages with type >= MIN_TYE_IN_SUBCLASS;
	 * it should return the exact number of bytes needed to represent
	 * the message in a byte array.
	 * 
	 * @return
	 */
	protected int messageContentLength() {
		ByteArrayOutputStream baos ;
		ObjectOutputStream oos ;
		
		switch( type ) {
		// These messages have 0 length (no content); only 
		// the type needs to be written.
		case TYPE_GO:
		case TYPE_PAUSE:
		case TYPE_UNPAUSE:
			return 0 ;
		
		// An integer (not a nonce).  This int conveys a particular
		// player slot, or the number thereof.
		case TYPE_GAME_OVER:
			return 4 ;
		
		// An integer and a message id.
		case TYPE_FULL_SYNCHRONIZATION_APPLIED:
			return 12 ;
			
		// Write an array of integers, of variable length.  Our length
		// is equal to 4 times playerSlotArrayLength: we write each as an int.
		case TYPE_WAITING_FOR_PLAYERS:
		case TYPE_PAUSED_BY_PLAYERS:
			return 4 * playerSlotArrayLength ;
			
		// Special cases: each of these message types requires
		// unique consideration.
			
		// Synchronization: we have an array of serializables.
		// We include 16 bytes of data, reading the actual content
		// later.  The content is playerSlot, sync message id (a long),
		// the number of elements in the sync array and
		// the length of each element of the synchronization array.
		case TYPE_FULL_SYNCHRONIZATION:
			int len = 16 ;
			try {
				for ( int i = 0; i < fullSynchronization.length; i++ ) {
					baos = new ByteArrayOutputStream() ;
					oos = new ObjectOutputStream(baos) ;
					oos.writeObject(fullSynchronization[i]) ;
					oos.close();
					len += baos.size() + 4 ;		// we also write length
				}
			} catch (  IOException e ) {
				e.printStackTrace() ;
				return 0 ;
			}
			return len ;
		
		// Move queue: we have copied the moves directly into "moveQueue."
		// Note that we also specify the player slot as a 4-byte int.
		case TYPE_MOVE_QUEUE:
			return moveQueueLength + 4 ;
		
		
		// Next Action Cycle: we have a handy conversion method
		// to go from object to bytes.  Count the resulting bytes.
		// We specify the player slot and a message ID, so add 12.
		case TYPE_NEXT_CYCLE:
			return actionCycleStateDescriptorUpdate.writeToByteArray(null, 0, BUFFER_SIZE()) + 12 ;
			
		// Cycle request: a player slot.
		case TYPE_NEXT_CYCLE_REQUEST:
			return 4 ;
			
		// Incoming attack: like action cycle, we convert to a byte
		// array and then count the bytes.  We specify the player slot,
		// so add 4.
		case TYPE_INCOMING_ATTACK:
			return this.attackDescriptor.writeToByteArray(null, 0, BUFFER_SIZE()) + 4 ;
			
		// RealtimeUpdate: this update sends the current available
		// "Real Time Information" from clients to the server.  This information
		// is then factored into game states as needed; for example, it contains
		// current displacement (in rows).
		// Presently it includes player slot, current displacement rows (double) and
		// current time (double).  It also includes milliseconds ticked (long).
		case TYPE_REALTIME_UPDATE:
			return 28 ;
			
		case TYPE_PLAYER_WON:
		case TYPE_PLAYER_LOST:	
		case TYPE_PLAYER_IS_SPECTATOR:
			// size 4: only a player slot.
			return 4 ;
			
		default:
			// WHAAAAAAA???
			return 0 ;
		}
	}
	
	
	/**
	 * Assume that any necessary prefix information has already been
	 * written to the OutputStream.  Write all message content.
	 * 
	 * This method may safely use byteBuffer.
	 * 
	 * @param os
	 * @throws IOException
	 */
	protected void writeMessageContent( Object outputDest ) throws IOException {
		makeByteArrayIfNeeded() ;
		
		int len ;
		switch( type ) {
		// These messages have 0 length (no content); only 
		// the type needs to be written.
		case TYPE_GO:
		case TYPE_PAUSE:
		case TYPE_UNPAUSE:
			break ;
		
		// An integer (not a nonce).  This int conveys a player slot.
		case TYPE_GAME_OVER:
			ByteArrayOps.writeIntAsBytes(playerSlot, eightByteArray, 0) ;
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 4) ;
			break ;
			
		case TYPE_FULL_SYNCHRONIZATION_APPLIED:
			ByteArrayOps.writeIntAsBytes(playerSlot, byteArray, 0) ;
			ByteArrayOps.writeLongAsBytes(messageId, byteArray, 4) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, 12) ;
			break ;
		
		// Write an array of integers, of variable length.
		case TYPE_WAITING_FOR_PLAYERS:
		case TYPE_PAUSED_BY_PLAYERS:
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				ByteArrayOps.writeIntAsBytes(this.playerSlotArray[i], byteArray, i*4) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, playerSlotArrayLength*4) ;
			break ;
			
		// Special cases: each of these message types requires
		// unique consideration.
			
		// Synchronization: we have an array of serializables.  Write
		// them all.  We actually use an ObjectOutputStream for this!
		case TYPE_FULL_SYNCHRONIZATION:
			// Writing directly with an ObjectOutputStream seems
			// to produce massive failures.  Try using an OOS to write to
			// an intermediate byte array.  Write the length of said
			// array, then the array itself...
			// We write first the player slot, then the messageID.
			ByteArrayOps.writeIntAsBytes(playerSlot, byteArray, 0) ;
			ByteArrayOps.writeLongAsBytes(messageId, byteArray, 4) ;
			ByteArrayOps.writeIntAsBytes(fullSynchronization.length, byteArray, 12) ;
			
			// write this header...
			this.writeBytesInByteArray(outputDest, byteArray, 0, 16) ;
			
			// for each object, we write its length as a  byte array,
			// then the byte array itself.
			for ( int i = 0; i < fullSynchronization.length; i++ ) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream() ;
				ObjectOutputStream oos = new ObjectOutputStream(baos) ;
				oos.writeObject( fullSynchronization[i] ) ;
				oos.close() ;
				
				byte [] syncBytes = baos.toByteArray() ;
				ByteArrayOps.writeIntAsBytes(syncBytes.length, eightByteArray, 0) ;
				
				// write length and content
				this.writeBytesInByteArray(outputDest, eightByteArray, 0, 4) ;
				this.writeBytesInByteArray(outputDest, syncBytes, 0, syncBytes.length) ;
			}
			break ;
		
		// Move queue: we have copied the moves directly into "moveQueue."
		// We can write them from there.
		case TYPE_MOVE_QUEUE:
			// Write the destination player slot, then the objects themselves.
			ByteArrayOps.writeIntAsBytes(this.playerSlot, eightByteArray, 0) ;
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 4) ;
			this.writeBytesInByteArray(outputDest, moveQueue, 0, moveQueueLength) ;
			break ;
		
		
		// Next Action Cycle: we have a handy conversion method
		// to go from object to bytes.  Write the resulting bytes.
		// (remember to write the player slot first!)
		case TYPE_NEXT_CYCLE:
			// Write the destination player slot, then the objects themselves.
			ByteArrayOps.writeIntAsBytes(this.playerSlot, byteArray, 0) ;
			ByteArrayOps.writeLongAsBytes(this.messageId, byteArray, 4) ;
			len = actionCycleStateDescriptorUpdate.writeToByteArray(byteArray, 12, byteArray.length) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, len+12) ;
			break ;
			
		case TYPE_NEXT_CYCLE_REQUEST:
			ByteArrayOps.writeIntAsBytes(this.playerSlot, byteArray, 0) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, 4) ;
			break ;
			
		// Incoming attack: like action cycle, we convert to a byte
		// array and then write it.
		case TYPE_INCOMING_ATTACK:
			// Write the destination player slot, then the attack.
			ByteArrayOps.writeIntAsBytes(this.playerSlot, eightByteArray, 0) ;
			len = this.attackDescriptor.writeToByteArray(byteArray, 0, byteArray.length) ;
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 4) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, len) ;
			break ;
			
		// RealtimeUpdate: includes information which is adjusted in real-time.
		// Currently the number of displaced rows and displaced seconds.
		case TYPE_REALTIME_UPDATE:
			ByteArrayOps.writeIntAsBytes(playerSlot, byteArray, 0) ;
			ByteArrayOps.writeDoubleAsBytes(this.displacedRows, byteArray, 4) ;
			ByteArrayOps.writeDoubleAsBytes(this.displacedSeconds, byteArray, 12) ;
			ByteArrayOps.writeLongAsBytes(this.millisTicked, byteArray, 20) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, 28) ;
			break ;
			
		case TYPE_PLAYER_WON:
		case TYPE_PLAYER_LOST:	
		case TYPE_PLAYER_IS_SPECTATOR:
			ByteArrayOps.writeIntAsBytes(this.playerSlot, eightByteArray, 0) ;
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 4) ;
			break ;
		}
	}
	
	
	/**
	 * Read message content from 'is'.  This method will be called
	 * by 'read' after the message type and length have been read;
	 * therefore, we can assume 'typeRead' and 'lengthRead' are
	 * both true.  Additionally, 'length' is the value read as
	 * the message length, and contentBytesRead is zero (for the first
	 * call on this message), or the value set during the last call.
	 * 
	 * This method will be called if the message type being read is
	 * equal to, or above, MIN_TYPE_IN_SUBCLASS.
	 * 
	 * Note that 'is' may not provide all of the message content
	 * in a single read; therefore, this method should return 'true'
	 * if the method content has been completely read and processed.
	 * 
	 * PRECONDITION: 'is' is a stream from which 'type' and 'length'
	 * have been read, plus an additional 'contentBytesRead'.
	 * You may use 'byteBuffer' to store persistent content; it will not be altered
	 * by Message until after this method returns 'true'.
	 * 
	 * POSTCONDITION: A total of 'contentBytesRead' have now been read
	 * from the InputStream.  If 'false' is returned, the stream is still
	 * good and there are additional bytes to read.  If 'true' is returned,
	 * then the entire message content (and NO MORE) has been read
	 * from the stream, and this Message object configured appropriately
	 * (as the method will only be called for subclass message types,
	 * we can't actually process the content ourselves).  You may use
	 * Message fields such as playerSlot or 'string', if appropriate.
	 * Throws an exception if the stream failed for any reason, or seemed
	 * to hold an incomplete message (i.e. a read returned -1).
	 * 
	 * @param is The input stream from which to read.
	 * @return Whether ALL message content has been read.
	 */
	protected boolean readMessageContent( Object inputSource ) throws IOException, ClassNotFoundException {
		makeByteArrayIfNeeded() ;
		
		// NOTE: Synchronization messages get special handling.
		// Because the objects being read / written are so large,
		// we cannot guarantee they fit in the byteArray.  Instead,
		// we manually read them by-parts.
		if ( type == TYPE_FULL_SYNCHRONIZATION ) {
			boolean ok = true ;
			// get player slot and messageID...
			ok = ok && this.readAllBytesIntoByteArray(inputSource, byteArray, 0, 16) ;
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			messageId = ByteArrayOps.readLongAsBytes(byteArray, 4) ;
			int numSyncObjects = ByteArrayOps.readIntAsBytes(byteArray, 12) ;
			// sanity check!
			ok = ok && numSyncObjects > 0 && numSyncObjects < 50 ;
			fullSynchronization = new Serializable[ numSyncObjects ] ;
			
			// get synchronization...
			byte [] barray = null ;
			for ( int i = 0; i < numSyncObjects && ok; i++ ) {
				// read length, allocate, read content.
				ok = ok && this.readAllBytesIntoByteArray(inputSource, eightByteArray, 0, 4) ;
				int objLen = ByteArrayOps.readIntAsBytes(eightByteArray, 0) ;
				ok = ok && objLen > 0 ;
				if ( ok ) {
					// don't re-allocate if we don't need to
					barray = (barray == null || barray.length < objLen) ? new byte[objLen] : barray ;
					ok = ok && this.readAllBytesIntoByteArray(inputSource, barray, 0, objLen) ;
					if ( ok ) {
						ByteArrayInputStream bais = new ByteArrayInputStream( barray, 0, objLen ) ;
						ObjectInputStream ois = new ObjectInputStream(bais) ;
						fullSynchronization[i] = (Serializable)ois.readObject() ;
						ois.close() ;
					}
				}
			}
			
			// that's it.
			if ( !ok )
				throw new IllegalArgumentException("Provided input source includes malformed FULL_SYNCHRONIZATION message, or is non-blocking.") ;
			return true ;
		}
		
		// If we get here, this is NOT a synchronization message.
		// All other message types are handled by reading bytes into
		// byteBuffer, then processing them.  We use the helper function
		// for this.
		if ( !readBytesIntoByteArray( inputSource ) ) {
			// Incomplete.  Return false.
			return false ;
		}
		
		
		// We have the message contents.  Now set the relevant
		// field for the message type.
		switch( type ) {
		// These messages have 0 length (no content); since we
		// have read the message type, we are done.
		case TYPE_GO:
		case TYPE_PAUSE:
		case TYPE_UNPAUSE:
			break ;
		
		
		// An integer (not a nonce).  This int conveys a particular
		// player slot, or the number thereof.
		case TYPE_GAME_OVER:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			break ;
			
		// Player slot and nonce.
		case TYPE_FULL_SYNCHRONIZATION_APPLIED:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			messageId = ByteArrayOps.readLongAsBytes(byteArray, 4) ;
			break ;
			
		
		// Write an array of integers, of variable length.
		case TYPE_WAITING_FOR_PLAYERS:
		case TYPE_PAUSED_BY_PLAYERS:
			if ( playerSlotArray == null || length / 4 > playerSlotArray.length )
				playerSlotArray = new int[length] ;
			for ( int i = 0; i < length/4; i++ )
				playerSlotArray[i] = ByteArrayOps.readIntAsBytes(byteArray, i*4) ;
			playerSlotArrayLength = length/4 ;
			break ;
			
		// Special cases: each of these message types requires
		// unique consideration.
		
		// Move queue: we have copied the moves directly into "moveQueue."
		// Copy them to our MoveQueue array.
		case TYPE_MOVE_QUEUE:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			length -= 4 ;
			moveQueueLength = length ;
			if ( length > moveQueue.length )
				moveQueue = new byte[length] ;
			//System.out.println("In Message: reading move queue of length " + length) ;
			for ( int i = 0; i < length; i++ ) {
				moveQueue[i] = byteArray[i+4] ;
				//System.out.print("" + moveQueue[i] + "\t") ;
			}
			//System.out.println() ;
			break ;
		
		
		// Next Action Cycle: we have a handy conversion method
		// to go from object to bytes.  Write the resulting bytes.
		case TYPE_NEXT_CYCLE:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			messageId = ByteArrayOps.readLongAsBytes(byteArray, 4) ;
			if ( actionCycleStateDescriptorUpdate == null )
				actionCycleStateDescriptorUpdate = new ActionCycleStateDescriptor.Update( byteArray, 12 ) ;
			else
				actionCycleStateDescriptorUpdate.readFromByteArray(byteArray, 12) ;
			break ;
			
		case TYPE_NEXT_CYCLE_REQUEST:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			break ;
			
		// Incoming attack: like action cycle, we convert to a byte
		// array and then write it.
		case TYPE_INCOMING_ATTACK:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			if ( attackDescriptor == null ) 
				attackDescriptor = new AttackDescriptor( byteArray, 4 ) ;
			else
				attackDescriptor.readFromByteArray( byteArray, 4 ) ;
			break ;
			
			
		// RealtimeUpdate: includes information which is adjusted in real-time.
		// Currently only the number of displaced rows.
		case TYPE_REALTIME_UPDATE:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			displacedRows = ByteArrayOps.readDoubleAsBytes(byteArray, 4) ;
			displacedSeconds = ByteArrayOps.readDoubleAsBytes(byteArray, 12) ;
			millisTicked = ByteArrayOps.readLongAsBytes(byteArray, 20) ;
			break ;
			
		case TYPE_PLAYER_WON:
		case TYPE_PLAYER_LOST:	
		case TYPE_PLAYER_IS_SPECTATOR:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			break ;
			
		// Next Action Cycle update: 
			
		}
		
		return true ;
	}
	
	

	
	public void getParticipantPlayers( boolean [] pSlot ) {
		for ( int i = 0; i < pSlot.length; i++ )
			pSlot[i] = false ;
		for ( int i = 0; i < playerSlotArrayLength; i++ )
			pSlot[ playerSlotArray[i] ] = true ;
	}
	
	public void getFullSynchronization( Game game ) {
		game.setStateAsSerializable( fullSynchronization[0] ) ;
		game.ginfo.setStateAsSerializable( fullSynchronization[1] ) ;
		
		Serializable [] systems = new Serializable[fullSynchronization.length - 2] ;
		for( int i = 0; i < systems.length; i++ )
			systems[i] = fullSynchronization[i+2] ;
		game.setSystemsFromSerializables(systems) ;	// should include pseudorandom
		// NOTHING HAPPENED last tick.  Allowing events from previous
		// full synchronizations to bleed in after a full sync causes some
		// rendering problems: copying GameBlocksSlices sometimes looks at
		// previous events for guidance about the kind of copy to make.
		game.s.geventsLastTick.clearHappened() ;
		game.gevents.clearHappened() ;
		
		game.s.geventsLastTick.setHappened(GameEvents.EVENT_FULL_SYNCHRONIZATION_APPLIED) ;
		game.gevents.setHappened(GameEvents.EVENT_FULL_SYNCHRONIZATION_APPLIED) ;
		
		game.refresh();
	}
	
	
	public int getMoveQueue( byte [] queue ) {
		for ( int i = 0; i < moveQueueLength; i++ )
			queue[i] = moveQueue[i] ;
		return moveQueueLength ;
	}
	
	
	/**
	 * Convention: a NextCycle is an update for a previous ACSD iff:
	 * 
	 * 1. It is a 'full update,' such that the result is independent of 
	 * 		the input, and 'messageID' is less than our message ID, -OR-
	 * 2. This update has the next message num (i.e. messageID + 1).
	 * @param messageID
	 * @return
	 */
	public boolean getNextCycleIsUpdateFor( long messageID ) {
		return ( actionCycleStateDescriptorUpdate.isFullUpdate() && messageID < this.messageId )
				|| messageID + 1 == this.messageId ;
	}
	
	public void getNextCycleUpdateAndApply( ActionCycleStateDescriptor acsd ) {
		actionCycleStateDescriptorUpdate.apply(acsd) ;
	}
	
	public void getIncomingAttack( AttackDescriptor ad ) {
		ad.copyValsFrom(attackDescriptor) ;
 	}
	
	public long getMessageId() {
		return messageId ;
	}
	
	public double getDisplacedRows() {
		return displacedRows ;
	}
	
	public double getDisplacedSeconds() {
		return displacedSeconds ;
	}
	
	public long getMillisecondsTicked() {
		return millisTicked ;
	}

	/**
	 * Sets this Message as a full synchronization for the game,
	 * which is likely to be much more information than necessary.
	 * 
	 * POSTCONDITION: Until this Message has been sent over an
	 * OutputStream, it is fragile in that changes to the provided
	 * Game (or its GameEvents, etc.) will change the contents of
	 * the Message.  In other words, we do not copy the states, we
	 * simply store references to them.  Send this message before
	 * making any changes to the Game.
	 * 
	 * @param targetPlayerSlot
	 * @param game
	 */
	public GameMessage setAsFullSynchronization( int targetPlayerSlot, long messageId, Game game ) {
		nullOutsideReferences() ;
		
		type = TYPE_FULL_SYNCHRONIZATION ;
		
		playerSlot = targetPlayerSlot ;
		this.messageId = messageId ;
		// Get serialized state from each.
		
		if ( fullSynchronization == null || fullSynchronization.length != 2 + Game.numSystems() )
			fullSynchronization = new Serializable[2 + Game.numSystems()] ;
		
		fullSynchronization[0] = game.getCloneStateAsSerializable() ;
		fullSynchronization[1] = game.ginfo.getCloneStateAsSerializable() ;
		Serializable [] ar = game.getClonedSerializablesFromSystems() ;
		for ( int i = 0; i < ar.length; i++ )
			fullSynchronization[i+2] = ar[i] ;
		
		return this ;
	}
	
	
	public GameMessage setAsFullSynchronizationApplied( int playerSlot, long messageId ) {
		nullOutsideReferences() ;
		
		type = TYPE_FULL_SYNCHRONIZATION_APPLIED ;
		this.playerSlot = playerSlot ;
		this.messageId = messageId ;
		
		return this ;
	}
	
	
	/**
	 * Sets this Message as "waiting for players"
	 * @param waitingForPlayerSlot
	 */
	public GameMessage setAsWaitingForPlayers( boolean [] waitingForPlayerSlot ) {
		nullOutsideReferences() ;
		
		type = TYPE_WAITING_FOR_PLAYERS ;
		
		playerSlotArrayLength = 0 ;
		
		if ( playerSlotArray == null || playerSlotArray.length != waitingForPlayerSlot.length )
			playerSlotArray = new int[waitingForPlayerSlot.length] ;
		
		for ( int i = 0; i < waitingForPlayerSlot.length; i++ ) {
			if ( waitingForPlayerSlot[i] ) {
				playerSlotArray[playerSlotArrayLength] = i ;
				playerSlotArrayLength++ ;
			}
		}
		
		return this ;
	}
	
	/**
	 * Sets this message as "paused by players".
	 * @param pausedByPlayerSlot
	 */
	public GameMessage setAsPausedByPlayers( boolean [] pausedByPlayerSlot ) {
		nullOutsideReferences() ;
		
		type = TYPE_PAUSED_BY_PLAYERS ;
		
		playerSlotArrayLength = 0 ;
		
		if ( playerSlotArray == null || playerSlotArray.length != pausedByPlayerSlot.length )
			playerSlotArray = new int[pausedByPlayerSlot.length] ;
		
		for ( int i = 0; i < pausedByPlayerSlot.length; i++ ) {
			if ( pausedByPlayerSlot[i] ) {
				playerSlotArray[playerSlotArrayLength] = i ;
				playerSlotArrayLength++ ;
			}
		}
		
		return this ;
	}
	
	/**
	 * Sets this message as "go".
	 */
	public GameMessage setAsGo( ) {
		nullOutsideReferences() ;
		
		type = TYPE_GO ;
		
		return this ;
	}
	
	/**
	 * Sets this Message as GameOver, given the winning player.
	 * @param winningPlayerSlot
	 */
	public GameMessage setAsGameOver( int winningPlayerSlot ) {
		nullOutsideReferences() ;
		
		type = TYPE_GAME_OVER ;
		playerSlot = winningPlayerSlot ;
		
		return this ;
	}
	
	
	/**
	 * Sets this Message as a MoveQueue.
	 * @param targetPlayerSlot
	 * @param moveQueue
	 * @param ind
	 * @param length
	 */
	public GameMessage setAsMoveQueue( int targetPlayerSlot, byte [] moveQueue, int ind, int length ) {
		nullOutsideReferences() ;
		
		type = TYPE_MOVE_QUEUE ;
		playerSlot = targetPlayerSlot ;
		
		if ( this.moveQueue == null || this.moveQueue.length < moveQueue.length )
			this.moveQueue = new byte[ moveQueue.length ] ; 
		
		for ( int i = 0; i < length; i++ )
			this.moveQueue[i] = moveQueue[i + ind] ;
		moveQueueLength = length ;
		
		return this ;
	}
	
	
	/**
	 * Sets this Message as a Cycle descriptor.  Internally we represent this
	 * as a 'full update.'
	 * 
	 * @param targetPlayerSlot
	 * @param acsd
	 */
	public GameMessage setAsNextCycle( int targetPlayerSlot, long id, ActionCycleStateDescriptor acsd ) {
		nullOutsideReferences() ;
		
		type = TYPE_NEXT_CYCLE ;
		playerSlot = targetPlayerSlot ;
		messageId = id ;
		
		if ( actionCycleStateDescriptorUpdate == null
				|| acsd.R() != actionCycleStateDescriptorUpdate.R()
				|| acsd.C() != actionCycleStateDescriptorUpdate.C() ) {
			actionCycleStateDescriptorUpdate = new ActionCycleStateDescriptor.Update(acsd.R(), acsd.C()) ;
		}
		actionCycleStateDescriptorUpdate.set(null, acsd) ;
		
		return this ;
	}
	
	
	public GameMessage setAsNextCycle( int targetPlayerSlot, long id,
			ActionCycleStateDescriptor prevACSD,
			ActionCycleStateDescriptor acsd ) {
		nullOutsideReferences() ;
		
		type = TYPE_NEXT_CYCLE ;
		playerSlot = targetPlayerSlot ;
		messageId = id ;
		
		if ( actionCycleStateDescriptorUpdate == null
				|| acsd.R() != actionCycleStateDescriptorUpdate.R()
				|| acsd.C() != actionCycleStateDescriptorUpdate.C() ) {
			actionCycleStateDescriptorUpdate = new ActionCycleStateDescriptor.Update(acsd.R(), acsd.C()) ;
		}
		actionCycleStateDescriptorUpdate.set(prevACSD, acsd) ;
		
		return this ;
	}
	
	
	public GameMessage setAsNextCycleRequest( int targetPlayerSlot ) {
		nullOutsideReferences() ;
		
		type = TYPE_NEXT_CYCLE_REQUEST ;
		playerSlot = targetPlayerSlot ;
		
		return this ;
	}
	
	
	/**
	 * Sets this message as an incoming attack.
	 * @param targetPlayerSlot
	 * @param ad
	 */
	public GameMessage setAsIncomingAttack( int targetPlayerSlot, AttackDescriptor ad ) {
		nullOutsideReferences() ;
		
		type = TYPE_INCOMING_ATTACK ;
		playerSlot = targetPlayerSlot ;
		
		if ( attackDescriptor == null
				|| attackDescriptor.R() != ad.R() || attackDescriptor.C() != ad.C() ) {
			attackDescriptor = new AttackDescriptor( ad.R(), ad.C() ) ;
		}
		attackDescriptor.copyValsFrom(ad) ;
		
		return this ; 
	}
	
	
	/**
	 * Sets this message as a pause request/statement.
	 */
	public GameMessage setAsPause() {
		nullOutsideReferences() ;
		
		type = TYPE_PAUSE ;
		
		return this ;
	}
	
	/**
	 * Sets this message as an unpause request/statement.
	 */
	public GameMessage setAsUnpause() {
		nullOutsideReferences() ;
		
		type = TYPE_UNPAUSE ;
		
		return this ;
	}
	
	
	/**
	 * Sets this message as a Real Time Update.
	 * @param displacedRows
	 * @return
	 */
	public GameMessage setAsRealTimeUpdate( int player, long millisTicked, double displacedSeconds, double displacedRows ) {
		nullOutsideReferences() ;
		
		type = TYPE_REALTIME_UPDATE ;
		
		this.playerSlot = player ;
		this.displacedSeconds = displacedSeconds ;
		this.displacedRows = displacedRows ;
		this.millisTicked = millisTicked ;
		
		return this ;
	}
	
	
	public GameMessage setAsPlayerWon( int playerSlot ) {
		nullOutsideReferences() ;
		
		type = TYPE_PLAYER_WON ;
		
		this.playerSlot = playerSlot ;
		return this; 
	}
	
	public GameMessage setAsPlayerLost( int playerSlot ) {
		nullOutsideReferences() ;
		
		type = TYPE_PLAYER_LOST ;
		
		this.playerSlot = playerSlot ;
		return this; 
	}
	
	public GameMessage setAsPlayerIsSpectator( int playerSlot ) {
		nullOutsideReferences() ;
		
		type = TYPE_PLAYER_IS_SPECTATOR ;
		
		this.playerSlot = playerSlot ;
		return this; 
	}
	
	
	
	
	@Override
	public Message setAs( Message m ) {
		nullOutsideReferences() ;
		
		if ( !(m instanceof GameMessage) )
			throw new IllegalArgumentException("Can't set a GameMessage as a non-GameMessage!") ;
		
		// Call to super, then set our own instance vars.
		super.setAs(m) ;
		
		GameMessage myM = (GameMessage)m ;
		
		if ( myM.fullSynchronization != null ) {
			if ( this.fullSynchronization == null || this.fullSynchronization.length < myM.fullSynchronization.length ) {
				this.fullSynchronization = new Serializable[myM.fullSynchronization.length] ;
			}
			for ( int i = 0; i < this.fullSynchronization.length; i++ )
				this.fullSynchronization[i] = myM.fullSynchronization[i] ;
		}
		
		this.messageId = myM.messageId ;
		this.millisTicked = myM.millisTicked ;
		this.displacedSeconds = myM.displacedSeconds ;
		this.displacedRows = myM.displacedRows ;
		
		this.playerSlotArrayLength = myM.playerSlotArrayLength ;
		if ( myM.playerSlotArray != null ) {
			if ( this.playerSlotArray == null || this.playerSlotArray.length < myM.playerSlotArray.length )
				this.playerSlotArray = new int[myM.playerSlotArray.length] ;
			for ( int i = 0; i < myM.playerSlotArrayLength; i++ )
				this.playerSlotArray[i] = myM.playerSlotArray[i] ;
		}
		
		this.moveQueueLength = myM.moveQueueLength ;
		if ( myM.moveQueue != null ) {
			if ( this.moveQueue == null || this.moveQueue.length < myM.moveQueue.length )
				this.moveQueue = new byte[myM.moveQueue.length] ;
			for ( int i = 0; i < myM.moveQueueLength; i++ )
				this.moveQueue[i] = myM.moveQueue[i] ;
		}
		
		if ( myM.getType() == TYPE_NEXT_CYCLE ) {
			if ( actionCycleStateDescriptorUpdate == null
					|| myM.actionCycleStateDescriptorUpdate.R() != actionCycleStateDescriptorUpdate.R()
					|| myM.actionCycleStateDescriptorUpdate .C() != actionCycleStateDescriptorUpdate.C() ) {
				actionCycleStateDescriptorUpdate = new ActionCycleStateDescriptor.Update(myM.actionCycleStateDescriptorUpdate.R(), myM.actionCycleStateDescriptorUpdate .C()) ;
			}
			this.actionCycleStateDescriptorUpdate.takeVals( myM.actionCycleStateDescriptorUpdate ) ;
		}
		
		if ( myM.getType() == TYPE_INCOMING_ATTACK ) {
			if ( attackDescriptor == null
					|| attackDescriptor.R() != myM.attackDescriptor.R() || attackDescriptor.C() != myM.attackDescriptor.C() ) {
				attackDescriptor = new AttackDescriptor( myM.attackDescriptor.R(), myM.attackDescriptor.C() ) ;
			}
		}
		
		return this ;
	}
}
