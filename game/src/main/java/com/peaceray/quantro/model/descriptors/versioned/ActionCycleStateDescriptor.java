package com.peaceray.quantro.model.descriptors.versioned;

import java.util.ArrayList;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.game.GameState;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.ByteArrayOps;

/**
 * An ActionCycleStateDescriptor is intended as a usually-incomplete,
 * but potentially-complete, description of a Game at the beginning of
 * a "cycle."
 * 
 * A "cycle" begins when a piece enters the game, and ends when there
 * is no longer a piece in play - either the piece locked, or it was
 * moved to reserve and not replaced (leading to another piece being
 * drawn from the queue to fall in at the top).
 * 
 * The primary function of this class, other than storing data, is to
 * provide a means for serializing this information as a byte array
 * that does not blindly include all available fields.
 * 
 * It is important to note that this class does NOT storage functionality
 * of GameState.  Whereas GameState contained all information the game needed
 * between method calls, this class stores only that information which is
 * needed to begin a cycle.  For example, since column unlocks are always
 * processed before the end of a cycle (they never "carry over" from one
 * cycle to the next), there is no need to store any column unlock information
 * here.
 * 
 * Object usage: although one could instantiate a new ActionCycleStateDescriptor
 * object whenever one is needed, it is probably better (to avoid garbage collection,
 * which may slow the game loop) to instead create 1 instance for each unidirectional
 * edge in the information flow, and use copyValsFrom() when appropriate (a call to
 * this method is likely to be faster than instantiating a new object, since instantiation
 * prompts garbage collection).
 * 
 * @author Jake
 *
 */
public final class ActionCycleStateDescriptor {
	
	private static final boolean DEBUG_LOG = false ;
	private static final String TAG = "ActionCycleStateDescriptor" ;
	
	private static final void log( String msg ) {
		if ( DEBUG_LOG ) {
			System.out.println(TAG + " : " + msg) ;
		}
	}
	
	private static final void log( Exception e, String msg ) {
		if ( DEBUG_LOG ) {
			System.err.println(TAG + " : " + msg + " : exception below") ;
			e.printStackTrace() ;
		}
	}
	
	private static final int VERSION = 3 ;
	// 0: Exact copy of unversioned 'version', except uses versioned attack descriptors
	// 1: Includes default rotation for next and reserve pieces.
	// 2: Includes displacementRows and indication of 'number to transfer this cycle.'
	// 3: Removes 'isIncluded_*'.  We introduce a new descriptor for in-place
	//		updates.  The idea is that the first ACSD of a set (after Full Sync.) is
	//		complete, and every cycle after that only sends a truncated "update" object.
	// 		Also adds "Update" class, for short-length in-place updates to ACSDs.  This
	//		class takes the place of previous 'isIncluded_*' fields.
	
	// This information should be constant for the entire lifetime 
	// of the object.
	public int R, C ;
	
	
	// What information do we need to describe a state?
	public byte [][][] blockField ;	
	public byte [][][] displacementRows ;
	public int [] nextPieces ;
	public int [] reservePieces ;
	
	public byte [] nextPiecesDefaultRotation ;
	public byte [] reservePiecesDefaultRotation ;
	
	// Copied completely from ginfo, NOT a reference to it.
	// Remember that the ActionCycleStateDescriptor is a "state-slice".
	public GameInformation ginfo ;
	
	public ArrayList<AttackDescriptor> attackDescriptors ;
	public int numAttackDescriptors ;
	
	// Some setup information
	public boolean dequeueAttackThisCycle ;
	
	// Row transfers?
	public byte displacedRowsToTransferThisCycle ;
	
	
	// Constructor
	public ActionCycleStateDescriptor( int rows, int cols ) {
		initialize(rows, cols); 
	}
	
	public void initialize( int rows, int cols ) {
		R = rows ;
		C = cols ;
		
		assert(rows < Byte.MAX_VALUE) ;
		assert(cols < Byte.MAX_VALUE) ; 
		
		blockField = new byte[2][rows][cols] ;
		displacementRows = new byte[2][rows][cols] ;
		
		if ( nextPieces == null ) {
			nextPieces = new int[GameState.PIECE_LOOKAHEAD] ;
			reservePieces = new int[GameState.PIECE_LOOKAHEAD] ;
			
			nextPiecesDefaultRotation = new byte[GameState.PIECE_LOOKAHEAD] ;
			reservePiecesDefaultRotation = new byte[GameState.PIECE_LOOKAHEAD] ;
		}
		
		if ( ginfo == null )
			ginfo = new GameInformation().finalizeConfiguration() ;
		
		if ( attackDescriptors == null ) {
			attackDescriptors = new ArrayList<AttackDescriptor>() ;
			numAttackDescriptors = 0 ;
		}
		
		dequeueAttackThisCycle = false ;
		
		displacedRowsToTransferThisCycle = 0 ;
	}
	
	public int R () {
		return R ;
	}
	
	public int C () {
		return C ;
	}
	
	
	
	/**
	 * A generic 'copy values' method.  Takes the values of the provided Descriptor.
	 * 
	 * If 'onlySrcInclusions', we copy only those fields which are marked as 'included'
	 * in the source object (acsd).
	 * 
	 * If 'onlyDstInclusions', we copy only those fields which are marked as 'included'
	 * in the destination object (this).
	 * 
	 * If both are true, only the intersection of these will be copied.
	 * 
	 * If 'adjustDstInclusions', we alter the 'included' fields of this object (destination)
	 * to reflect those that have been changed by this takeVals operation AND were included
	 * in the source.
	 * 
	 * Returns whether 'this' object is now in a consistent, copied state: that is,
	 * the fields which are marked as 'included' reflect data from the source.
	 * 
	 * @param acsd
	 */
	public void takeVals( ActionCycleStateDescriptor acsd ) {
		if ( R != acsd.R || C != acsd.C ) {
			R = acsd.R ;
			C = acsd.C ;
			this.blockField = new byte[2][R][C] ;
			this.displacementRows = new byte[2][R][C] ;
		}
		
		// Blockfield
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = 0; r < R; r++ ) {
				for ( int c = 0; c < C; c++ ) {
					this.blockField[q][r][c] = acsd.blockField[q][r][c] ;
				}
			}
		}
		
		// Displacement
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = 0; r < R; r++ ) {
				for ( int c = 0; c < C; c++ ) {
					this.displacementRows[q][r][c] = acsd.displacementRows[q][r][c] ;
				}
			}
		}
		
		// Next pieces
		for ( int i = 0; i < GameState.PIECE_LOOKAHEAD; i++ ) {
			this.nextPieces[i] = acsd.nextPieces[i] ;
			this.nextPiecesDefaultRotation[i] = acsd.nextPiecesDefaultRotation[i] ;
		}

		
		// Reserve pieces
		for ( int i = 0; i < GameState.PIECE_LOOKAHEAD; i++ ) {
			this.reservePieces[i] = acsd.reservePieces[i] ;
			this.reservePiecesDefaultRotation[i] = acsd.reservePiecesDefaultRotation[i] ;
		}
		
		// ginfo
		this.ginfo.takeVals( acsd.ginfo ) ;

		
		// Attack Descriptors
		this.numAttackDescriptors = acsd.numAttackDescriptors ;
		for ( int i = 0; i < acsd.numAttackDescriptors; i++ ) {
			if ( i >= attackDescriptors.size() )
				attackDescriptors.add( new AttackDescriptor( R, C ) ) ;
			
			attackDescriptors.get(i).copyValsFrom( acsd.attackDescriptors.get(i) ) ;
		}
		
		this.dequeueAttackThisCycle = acsd.dequeueAttackThisCycle ;
		this.displacedRowsToTransferThisCycle = acsd.displacedRowsToTransferThisCycle ;
	}
	
	
	/**
	 * A tightly-packaged update object, designed to patch one ACSD into a
	 * different one with a very small number of bytes.
	 * 
	 * The purpose is to reduce the byte-length of our regular cycle state updates
	 * in multiplayer: currently, each Cycle State takes up more than one UDP 
	 * packet, which we think is undesireable.
	 * 
	 * Solution: send one Cycle State to start things off, then rely on Updates
	 * instead (until the next Synchronization or the user requests an explicit
	 * Cycle State).
	 * 
	 * Because the goal of an Update is to minimize the byte-length of an MP message,
	 * we don't bother doing any checking or verification when applied.  Obviously
	 * you can imagine a scenario when an Update is generated to convert ACSD X to Y,
	 * but is then (erroneously) applied to Z.  From the perspective of this object,
	 * we assume this scenario is being prevented by careful bookkeeping by outside
	 * sources.
	 * 
	 * As with ACSD instances, we prefer reusable instances of Update, rather than
	 * creating a new instance every time.
	 * 
	 * @author Jake
	 *
	 */
	public static class Update {
		
		/**
		 * QueueUpdateTypes are transmitted as ordinals; add new types to the BOTTOM
		 * of this list.
		 * 
		 * @author Jake
		 *
		 */
		private enum QueueUpdateType {
			/**
			 * This Update does not contain any Queue information.  Keep exactly the
			 * same queue as the previous ACSD.
			 */
			NONE,
			
			/**
			 * We provide an explicit queue, which should replace the previous
			 * one.
			 */
			EXPLICIT,
			
			/**
			 * We provide an int, representing the number of items to be polled
			 * (removed) from the front of the queue, and an explicit queue of
			 * additional items which should be added to the end.  Almost certainly
			 * shorter (byte-wise) than an explicit queue would be; in the cases 
			 * where it is longer, it will be minimally so (4-bytes: length of the
			 * int indicating "no removals").
			 */
			DEQUEUE_AND_ENQUEUE,
		}
		
		
		private enum FieldUpdateType {
			/**
			 * Rows are explicitly marked; those rows marked 'true'
			 * need to be updated.  Keep the rest the same.
			 */
			UPDATE_ROWS,
			
			/**
			 * Rows are explicitly marked; those rows marked 'true'
			 * are not empty, and their content is available.  All
			 * other rows are empty.
			 */
			NON_EMPTY_ROWS,
			
			/**
			 * The entire field is empty.  Set it as such.
			 */
			EMPTY,
		}
		
		// This information should be constant for the entire lifetime 
		// of the object.
		public int mR, mC ;
		
		// Blockfields: we have the space to store the entire blockfield
		// (and displacement) but we explicitly note those rows which
		// should be altered by the update.  Only those rows are transmitted
		// and used; the rest are ignored.
		public FieldUpdateType mBlockFieldUpdateType ;
		public boolean [] mBlockFieldRowUpdated ;
		public byte [][][] mBlockField ;	
		public FieldUpdateType mDisplacementRowsUpdateType ;
		public boolean [] mDisplacementRowUpdated ;
		public byte [][][] mDisplacementRows ;
		
		// Upcoming pieces: just note whether or not there was
		// a change.
		public boolean mNextPiecesUpdated ;
		public int [] mNextPieces ;
		public byte [] mNextPiecesDefaultRotation ;
		public boolean mReservePiecesUpdated ;
		public int [] mReservePieces ;
		public byte [] mReservePiecesDefaultRotation ;
		
		// For now, we assume that every cycle state changes Ginfo.
		public GameInformation mGinfo ;
		
		// Attack Descriptors, if altered, are altered in one of
		// two wasy.  First, we might explicitly note the list of
		// attack descriptors -- in this case we explicitly send all
		// and copy to the ACSD when applied.  Second, we might instead
		// send poll/add instructions: the number of AttackDescriptors to
		// remove from the list, followed by a set of ADs to add at the end.
		// This construction requires an equality operator for ADs which
		// has not yet been implemented: we therefore have currently
		// implemented the "apply" behavior for this case, but no code exists
		// which would generate such an update.
		public QueueUpdateType mAttackDescriptorsQueueUpdateType ;
		public int mNumAttackDescriptorsToDequeue ;
		public ArrayList<AttackDescriptor> mAttackDescriptors ;
		public int mNumAttackDescriptors ;
		
		// This information is always kept for the update; it is
		// explicitly copied over, every time.
		public boolean mDequeueAttackThisCycle ;
		public byte mDisplacedRowsToTransferThisCycle ;
		
		

		public Update( int rows, int cols ) {
			initialize(rows, cols) ;
		}
		
		
		/**
		 * Creates a new Update which, when applied to 'from', alters it to match 'to.'
		 * 
		 * Neither object is affected by this method.
		 * 
		 * PRECONDITION: 'to' is non-null.  If 'from' is non-null, its row / column counts
		 * 		must match those of 'to.'
		 * 
		 * POSTCONDITION: The newly created object is an update which, when applied to 'from',
		 * 		alters its fields to match those of 'to.'
		 * 
		 * @param from The 'before update' ACSD.  Can be null, in which case the Update
		 * 			will completely represent the fields in 'to.'
		 * @param to The 'after update' ACSD.  Cannot be null.
		 */
		public Update( ActionCycleStateDescriptor from, ActionCycleStateDescriptor to ) {
			this(to.R, to.C) ;
			set(from, to) ;
		}
		
		public Update( byte [] ar, int index ) {
			int version = ByteArrayOps.readIntAsBytes(ar, index) ;
			
			if ( version != VERSION )
				throw new IllegalArgumentException("Don't know how to process version " + version) ;
			
			int R = ar[index+4] ;
			int C = ar[index+5] ;
			
			initialize(R, C) ;
			
			// Read the provided info
			this.readFromByteArray(ar, index) ;
		}
		
		private void initialize(int rows, int cols) {
			mR = rows ;
			mC = cols ;
			
			mBlockFieldUpdateType = FieldUpdateType.UPDATE_ROWS ;
			mBlockFieldRowUpdated = new boolean[mR] ;
			mBlockField = new byte[2][mR][mC] ;
			mDisplacementRowsUpdateType = FieldUpdateType.UPDATE_ROWS ;
			mDisplacementRowUpdated = new boolean[mR] ;
			mDisplacementRows = new byte[2][mR][mC] ;
			
			// Upcoming pieces: just note whether or not there was
			// a change.
			mNextPiecesUpdated = false ;
			mNextPieces = new int[GameState.PIECE_LOOKAHEAD] ;
			mNextPiecesDefaultRotation = new byte[GameState.PIECE_LOOKAHEAD] ;
			mReservePiecesUpdated = false ;
			mReservePieces = new int[GameState.PIECE_LOOKAHEAD] ;
			mReservePiecesDefaultRotation = new byte[GameState.PIECE_LOOKAHEAD] ;
			
			// For now, we assume that every cycle state changes Ginfo.
			mGinfo = new GameInformation().finalizeConfiguration() ;
			
			// Attack Descriptors, if altered, are altered in one of
			// two wasy.  First, we might explicitly note the list of
			// attack descriptors -- in this case we explicitly send all
			// and copy to the ACSD when applied.  Second, we might instead
			// send poll/add instructions: the number of AttackDescriptors to
			// remove from the list, followed by a set of ADs to add at the end.
			// This construction requires an equality operator for ADs which
			// has not yet been implemented: we therefore have currently
			// implemented the "apply" behavior for this case, but no code exists
			// which would generate such an update.
			mAttackDescriptorsQueueUpdateType = QueueUpdateType.NONE ;
			mNumAttackDescriptorsToDequeue = 0 ;
			mAttackDescriptors = new ArrayList<AttackDescriptor>() ;
			mNumAttackDescriptors = 0 ;
			
			// This information is always kept for the update; it is
			// explicitly copied over, every time.
			mDequeueAttackThisCycle = false ;
			mDisplacedRowsToTransferThisCycle = 0 ;
		}
		
		
		public int R() {
			return mR ;
		}
		
		public int C() {
			return mC ;
		}
		
		/**
		 * Will this Update completely overwrite the previous configuration?
		 * 
		 * A "Full Update," when applied, will result in a CycleState that is 
		 * independent of the previous state.
		 * 
		 * @return
		 */
		public boolean isFullUpdate() {
			boolean full = true ;
			for ( int i = 0; i < 2; i++ ) {
				FieldUpdateType type ;
				boolean [] rows ;
				if ( i == 0 ) {
					type = mBlockFieldUpdateType ;
					rows = mBlockFieldRowUpdated ;
				} else {
					type = mDisplacementRowsUpdateType ;
					rows = mDisplacementRowUpdated ;
				}
				switch( type ) {
				case UPDATE_ROWS:
					for ( int r = 0; r < mR; r++ ) {
						full = full && rows[r] ;
					}
					break ;
				case NON_EMPTY_ROWS:
				case EMPTY:
					// yup, this is complete.
					break ;
				default:
					throw new IllegalStateException("type is not set") ;
				}
			}
			
			full = full && mNextPiecesUpdated ;
			full = full && mReservePiecesUpdated ;
			
			// ginfo always updates...
			
			// attack descriptors: must be EXPLICIT
			full = full && mAttackDescriptorsQueueUpdateType == QueueUpdateType.EXPLICIT ;
			
			// dequeue and displace rows are always included.
			
			// full update?
			return full ;
		}
		
		public void takeVals( ActionCycleStateDescriptor.Update update ) {
			if ( update.mR != mR || update.mC != mC )
				initialize(update.mR, update.mC) ;
			
			// Unnecessary data is copied here.  If you want, expand the copyInto()
			// calls for the blockfields, and copy only the affected rows.
			this.mBlockFieldUpdateType = update.mBlockFieldUpdateType ;
			ArrayOps.copyInto(update.mBlockFieldRowUpdated, this.mBlockFieldRowUpdated) ;
			ArrayOps.copyInto(update.mBlockField, this.mBlockField) ;
			this.mDisplacementRowsUpdateType = update.mDisplacementRowsUpdateType ;
			ArrayOps.copyInto(update.mDisplacementRowUpdated, this.mDisplacementRowUpdated) ;
			ArrayOps.copyInto(update.mDisplacementRows, this.mDisplacementRows) ;
			
			// Upcoming pieces.
			mNextPiecesUpdated = update.mNextPiecesUpdated ;
			if ( mNextPiecesUpdated ) {
				ArrayOps.copyInto(update.mNextPieces, this.mNextPieces) ;
				ArrayOps.copyInto(update.mNextPiecesDefaultRotation, this.mNextPiecesDefaultRotation) ;
			}
			mReservePiecesUpdated = update.mReservePiecesUpdated ;
			if ( mReservePiecesUpdated ) {
				ArrayOps.copyInto(update.mReservePieces, this.mReservePieces) ;
				ArrayOps.copyInto(update.mReservePiecesDefaultRotation, this.mReservePiecesDefaultRotation) ;
			}
			
			// Copy Ginfo values.
			mGinfo.takeVals(update.mGinfo) ;
			
			//  Copy everything, why not.
			mAttackDescriptorsQueueUpdateType = update.mAttackDescriptorsQueueUpdateType ;
			mNumAttackDescriptorsToDequeue = update.mNumAttackDescriptorsToDequeue ;
			mNumAttackDescriptors = update.mNumAttackDescriptors ;
			for ( int i = 0; i < mNumAttackDescriptors; i++ ) {
				if ( mAttackDescriptors.size() <= i ) {
					mAttackDescriptors.add(new AttackDescriptor(mR, mC)) ;
				}
				mAttackDescriptors.get(i).copyValsFrom(update.mAttackDescriptors.get(i)) ;
			}
			
			// This information is always kept for the update; it is
			// explicitly copied over, every time.
			mDequeueAttackThisCycle = update.mDequeueAttackThisCycle ;
			mDisplacedRowsToTransferThisCycle = update.mDisplacedRowsToTransferThisCycle ;
		}
		
		
		/**
		 * Sets this instance to an Update which, when applied to 'from',
		 * alters it to match 'to.'
		 * 
		 * Neither object is affected by this method.
		 * 
		 * PRECONDITION: 'to' is non-null.  If 'from' is non-null, its row / column counts
		 * 		must match those of 'to.'
		 * 
		 * POSTCONDITION: The newly created object is an update which, when applied to 'from',
		 * 		alters its fields to match those of 'to.'
		 * 
		 * @param from The 'before update' ACSD.  Can be null, in which case the Update
		 * 			will completely represent the fields in 'to.'  If 'null', we guarantee
		 * 			that after this call isFullUpdate() will return 'true.'
		 * @param to The 'after update' ACSD.  Cannot be null.
		 */
		public void set( ActionCycleStateDescriptor from, ActionCycleStateDescriptor to ) {
			
			// initialize structures if we need to...
			if ( mR != to.R || mC != to.C )
				initialize(to.R, to.C) ;
			
			if ( from == null ) {
				// this update should explicitly contain all the info
				// in 'to.'
				// blockfield and displacement:
				// either EMPTY or NON_EMPTY_ROWS
				mBlockFieldUpdateType = getUpdateType(null, to.blockField) ;
				if ( mBlockFieldUpdateType == FieldUpdateType.NON_EMPTY_ROWS ) {
					for ( int r = 0; r < mR; r++ ) {
						mBlockFieldRowUpdated[r] = setBlockFieldRowNonEmptyRows( r, to.blockField, mBlockField ) ;
					}
				}
				mDisplacementRowsUpdateType = getUpdateType(null, to.displacementRows) ;
				if ( mDisplacementRowsUpdateType == FieldUpdateType.NON_EMPTY_ROWS ) {
					for ( int r = 0; r < mR; r++ ) {
						mDisplacementRowUpdated[r] = setBlockFieldRowNonEmptyRows( r, to.displacementRows, mDisplacementRows ) ;
					}
				}
				
				// next / reserve piece
				mNextPiecesUpdated = true ;
				mReservePiecesUpdated = true ;
				for ( int i = 0; i < mNextPieces.length; i++ ) {
					mNextPieces[i] = to.nextPieces[i] ;
					mNextPiecesDefaultRotation[i] = to.nextPiecesDefaultRotation[i] ;
					mReservePieces[i] = to.reservePieces[i] ;
					mReservePiecesDefaultRotation[i] = to.reservePiecesDefaultRotation[i] ;
				}
				
				// Take the whole GINFO.
				mGinfo.takeVals(to.ginfo) ;
				
				// Attack Descriptors are represented as an explicit queue update.
				mAttackDescriptorsQueueUpdateType = QueueUpdateType.EXPLICIT ;
				mNumAttackDescriptorsToDequeue = 0 ;	// ignored
				for ( int i = 0; i < to.numAttackDescriptors; i++ ) {
					if ( mAttackDescriptors.size() <= i ) {
						mAttackDescriptors.add( new AttackDescriptor(mR, mC) ) ;
					}
					mAttackDescriptors.get(i).copyValsFrom(to.attackDescriptors.get(i)) ;
				}
				mNumAttackDescriptors = to.numAttackDescriptors ;
				
				// This information is always kept for the update; it is
				// explicitly copied over, every time.
				mDequeueAttackThisCycle = to.dequeueAttackThisCycle ;
				mDisplacedRowsToTransferThisCycle = to.displacedRowsToTransferThisCycle ;
			} else {
				assert( from.R == to.R ) ;
				assert( from.C == to.C ) ;
				
				// this update includes only the data that changes between
				// 'from' and 'to.'
				// blockfield and displacement rows:
				mBlockFieldUpdateType = getUpdateType(from.blockField, to.blockField) ;
				switch( mBlockFieldUpdateType ) {
				case EMPTY:
					// nothing to copy
					break ;
				case UPDATE_ROWS:
					for ( int r = 0; r < mR; r++ ) {
						mBlockFieldRowUpdated[r] = setBlockFieldRowUpdateRows(
								r, from.blockField, to.blockField, mBlockField ) ;
					}
					break ;
				case NON_EMPTY_ROWS:
					for ( int r = 0; r < mR; r++ ) {
						mBlockFieldRowUpdated[r] = setBlockFieldRowNonEmptyRows( r, to.blockField, mBlockField ) ;
					}
					break ;
				}
				mDisplacementRowsUpdateType = getUpdateType(from.displacementRows, to.displacementRows) ;
				switch( mDisplacementRowsUpdateType ) {
				case EMPTY:
					// nothing to copy
					break ;
				case UPDATE_ROWS:
					for ( int r = 0; r < mR; r++ ) {
						mDisplacementRowUpdated[r] = setBlockFieldRowUpdateRows(
								r, from.displacementRows, to.displacementRows, mDisplacementRows ) ;
					}
					break ;
				case NON_EMPTY_ROWS:
					for ( int r = 0; r < mR; r++ ) {
						mDisplacementRowUpdated[r] = setBlockFieldRowNonEmptyRows( r, to.displacementRows, mDisplacementRows ) ;
					}
					break ;
				}
				
				// next / reserve piece.  Include if a change occurs.
				mNextPiecesUpdated = false ;
				mReservePiecesUpdated = false ;
				for ( int i = 0; i < mNextPieces.length; i++ ) {
					// next pieces: check for a change.  Copy whether or not
					// one is detected: we might detect a change in a later
					// iteration, and if so, we'd want to have the correct values here
					// already.
					if ( from.nextPieces[i] != to.nextPieces[i] ||
							from.nextPiecesDefaultRotation[i] != to.nextPiecesDefaultRotation[i] )
						mNextPiecesUpdated = true ;
					mNextPieces[i] = to.nextPieces[i] ;
					mNextPiecesDefaultRotation[i] = to.nextPiecesDefaultRotation[i] ;
					// reserve pieces: same deal.
					if ( from.reservePieces[i] != to.reservePieces[i] ||
							from.reservePiecesDefaultRotation[i] != to.reservePiecesDefaultRotation[i] )
						mReservePiecesUpdated = true ;
					mReservePieces[i] = to.reservePieces[i] ;
					mReservePiecesDefaultRotation[i] = to.reservePiecesDefaultRotation[i] ;
				}
				
				// Take the whole GINFO.
				mGinfo.takeVals(to.ginfo) ;
				
				// Attack Descriptors are represented as an explicit queue update.
				// TODO: when equality operators are supported for AttackDescriptors,
				// then check the equality of the lists.  If identical, use NONE.
				// If a minor change (representable by a certain number of dequeues
				// and enqueues), use DEQUEUE_AND_ENQUEUE.  Only fall back to EXPLICIT
				// if we can't easily use the other two approaches.
				mAttackDescriptorsQueueUpdateType = QueueUpdateType.EXPLICIT ;
				mNumAttackDescriptorsToDequeue = 0 ;	// ignored
				for ( int i = 0; i < to.numAttackDescriptors; i++ ) {
					if ( mAttackDescriptors.size() <= i ) {
						mAttackDescriptors.add( new AttackDescriptor(mR, mC) ) ;
					}
					mAttackDescriptors.get(i).copyValsFrom(to.attackDescriptors.get(i)) ;
				}
				mNumAttackDescriptors = to.numAttackDescriptors ;
				
				// This information is always kept for the update; it is
				// explicitly copied over, every time.
				mDequeueAttackThisCycle = to.dequeueAttackThisCycle ;
				mDisplacedRowsToTransferThisCycle = to.displacedRowsToTransferThisCycle ;
			}
		}
		
		
		private static FieldUpdateType getUpdateType( byte [][][] fieldFrom, byte [][][] fieldTo ) {
			if ( ArrayOps.isEmpty(fieldTo) ) {
				return FieldUpdateType.EMPTY ;
			} else {
				if ( fieldFrom == null ) {
					return FieldUpdateType.NON_EMPTY_ROWS ;
				}
				
				// we want the minimum number of explicit rows.
				// count rows that are non-equal, and rows that are non-empty,
				// and take the minimum.
				int Q = fieldTo.length ;
				int R = fieldTo[0].length ;
				int nonEqual = 0 ;
				int nonEmpty = 0 ;
				for ( int r = 0; r < R; r++ ) {
					boolean isEqual = true ;
					boolean isEmpty = true ;
					for ( int q = 0; q < Q; q++ ) {
						isEqual = isEqual && ArrayOps.areEqual(fieldFrom[q][r], fieldTo[q][r]) ;
						isEmpty = isEmpty && ArrayOps.isEmpty(fieldTo[q][r]) ;
					}
					if ( !isEqual )
						nonEqual++ ;
					if ( !isEmpty )
						nonEmpty++ ;
				}
				
				if ( nonEqual < nonEmpty )
					return FieldUpdateType.UPDATE_ROWS ;
				return FieldUpdateType.NON_EMPTY_ROWS ;
			}
		}
		
		
		/**
		 * A helper method: given the 'from', 'to' and 'update' blockfields, checks the
		 * specified row (in both qPanes).  If it changes from->to, sets the corresponding
		 * row in 'update' to the values in 'to' and returns true.
		 * 
		 * If both 'from' and 'to' are identical, makes no changes to 'update' and returns
		 * false.
		 * 
		 * Suggested usage:
		 * 
		 * 		for ( int r = 0; r < mR; r++ ) {
		 * 			mBlockFieldUpdate[r] = setBlockFieldRowUpdate( r, from.bf, to.bf, this.bf )
		 * 		}
		 * @param r
		 * @param from
		 * @param to
		 * @param update
		 * @return
		 */
		private static boolean setBlockFieldRowUpdateRows( int r, byte [][][] from, byte [][][] to, byte [][][] update ) {
			boolean include = false ;
			for ( int q = 0; q < to.length; q++ ) {
				include = include || !ArrayOps.areEqual(from[q][r], to[q][r]) ;
			}
			if ( include ) {
				for ( int q = 0; q < to.length; q++ ) {
					ArrayOps.copyInto(to[q][r], update[q][r]) ;
				}
			}
			return include ;
		}
		
		private static boolean setBlockFieldRowNonEmptyRows( int r, byte [][][] to, byte [][][] update ) {
			boolean include = false ;
			for ( int q = 0; q < to.length; q++ ) {
				include = include || !ArrayOps.isEmpty(to[q][r]) ;
			}
			if ( include ) {
				for ( int q = 0; q < to.length; q++ ) {
					ArrayOps.copyInto(to[q][r], update[q][r]) ;
				}
			}
			return include ;
		}
		
		
		
		/**
		 * Applies this update to the provided ACSD.
		 * 
		 * PRECONDITION: This update has been 'set()' to transition 'from' --> 'to'.
		 * 		'acsd' is exactly equivalent to 'from', the argument used to set this
		 * 		update.
		 * 
		 * POSTCONDITION: 'acsd' is exactly equivalent to 'to.'
		 * 
		 * @param acsd
		 */
		public void apply( ActionCycleStateDescriptor acsd ) {
			if ( mR != acsd.R || mC != acsd.C ) {
				if ( isFullUpdate() ) {
					acsd.initialize(mR, mC) ;
				} else {
					throw new IllegalArgumentException("A mis-sized acsd provided for a non-full update") ;
				}
			}
			
			// blockfield and displacement: apply only those rows which have been
			// updated.
			switch( mBlockFieldUpdateType ) {
			case UPDATE_ROWS:
				for ( int r = 0; r < mR; r++ ) {
					if ( mBlockFieldRowUpdated[r] ) {
						applyBlockFieldRowUpdate(r, mBlockField, acsd.blockField) ;
					}
				}
				break ;
			case NON_EMPTY_ROWS:
				for ( int r = 0; r < mR; r++ ) {
					if ( mBlockFieldRowUpdated[r] ) {
						applyBlockFieldRowUpdate(r, mBlockField, acsd.blockField) ;
					} else {
						applyBlockFieldRowClear(r, acsd.blockField) ;
					}
				}
				break ;
			case EMPTY:
				ArrayOps.setEmpty( acsd.blockField ) ;
				break ;
			}
			switch( mDisplacementRowsUpdateType ) {
			case UPDATE_ROWS:
				for ( int r = 0; r < mR; r++ ) {
					if ( mDisplacementRowUpdated[r] ) {
						applyBlockFieldRowUpdate(r, mDisplacementRows, acsd.displacementRows) ;
					}
				}
				break ;
			case NON_EMPTY_ROWS:
				for ( int r = 0; r < mR; r++ ) {
					if ( mDisplacementRowUpdated[r] ) {
						applyBlockFieldRowUpdate(r, mDisplacementRows, acsd.displacementRows) ;
					} else {
						applyBlockFieldRowClear(r, acsd.displacementRows) ;
					}
				}
				break ;
			case EMPTY:
				ArrayOps.setEmpty( acsd.displacementRows ) ;
				break ;
			}
			
			// next / reserve piece.
			if ( mNextPiecesUpdated ) {
				for ( int i = 0; i < mNextPieces.length; i++ ) {
					acsd.nextPieces[i] = mNextPieces[i] ;
					acsd.nextPiecesDefaultRotation[i] = mNextPiecesDefaultRotation[i] ;
				}
			}
			if ( mReservePiecesUpdated ) {
				for ( int i = 0; i < mReservePieces.length; i++ ) {
					acsd.reservePieces[i] = mReservePieces[i] ;
					acsd.reservePiecesDefaultRotation[i] = mReservePiecesDefaultRotation[i] ;
				}
			}
			
			// GINFO
			acsd.ginfo.takeVals(mGinfo) ;
			
			// Attack descriptor.  We update this queue one of three ways:
			// explicit update, dequeue-and-enqueue, or none (no change).
			switch( mAttackDescriptorsQueueUpdateType ) {
			case NONE:
				// no change
				break ;
				
			case EXPLICIT:
				// we have an explicit attack descriptor list.  Set ACSD to match.
				for ( int i = 0; i < mNumAttackDescriptors; i++ ) {
					if ( acsd.attackDescriptors.size() <= i ) {
						acsd.attackDescriptors.add( new AttackDescriptor(mR, mC) ) ;
					}
					acsd.attackDescriptors.get(i).copyValsFrom(mAttackDescriptors.get(i)) ;
				}
				acsd.numAttackDescriptors = mNumAttackDescriptors ;
				break ;
				
			case DEQUEUE_AND_ENQUEUE:
				// we need to dequeue (pull from the front) the specified number
				// of attack decriptors, then add the descriptors present to the 
				// back of the list.
				// dequeue...
				for ( int i = 0; i < mNumAttackDescriptorsToDequeue; i++ ) {
					// instead of throwing away instances, move them to the very
					// back and decrement size.
					acsd.attackDescriptors.add( acsd.attackDescriptors.remove(0) ) ;
					acsd.numAttackDescriptors-- ;
				}
				// and enqueue...
				for ( int i = 0; i < mNumAttackDescriptors; i++ ) {
					if ( acsd.attackDescriptors.size() == acsd.numAttackDescriptors ) {
						acsd.attackDescriptors.add( new AttackDescriptor(mR, mC) ) ;
					}
					acsd.attackDescriptors.get(acsd.numAttackDescriptors).copyValsFrom(mAttackDescriptors.get(i)) ;
					acsd.numAttackDescriptors++ ;
				}
				break ;
			}
			
			// This information is always kept for the update; it is
			// explicitly copied over, every time.
			acsd.dequeueAttackThisCycle = mDequeueAttackThisCycle ;
			acsd.displacedRowsToTransferThisCycle = mDisplacedRowsToTransferThisCycle ;
			
			// the update has been applied.
		}
		
		
		/**
		 * Applies the specified row (both qPanes) from 'update' to 'blockfield.'
		 * @param r
		 * @param update
		 * @param blockfield
		 */
		private static void applyBlockFieldRowUpdate( int r, byte [][][] update, byte [][][] blockfield ) {
			for ( int q = 0; q < update.length; q++ ) {
				ArrayOps.copyInto(update[q][r], blockfield[q][r]) ;
			}
		}
		
		private static void applyBlockFieldRowClear( int r, byte [][][] blockfield ) {
			for ( int q = 0; q < blockfield.length; q++ ) {
				ArrayOps.setEmpty(blockfield[q][r]) ;
			}
		}
		
		
		// Package / unpackage as a byte array.
		
		/**
		 * Writes the current contents of this Update to
		 * the provided byte array, beginning at 'ind'.
		 * 
		 * Returns the number of bytes written.  It is the caller's responsibliity
		 * that the full size of the Update can fit in the
		 * provided byte array (especially within the "valid writen region",
		 * if there is such a thing).  Will throw an exception if it writes past the
		 * end of the array, or up to (including) topBounds.
		 * 
		 * In the event that an exception is thrown, this method has written from 'ind'
		 * to the maximum bound, whether topBounds or b.length.
		 * 
		 * Take care that topBounds is not < ind, or this method may throw an
		 * exception even if the entire length of the byte representation has been
		 * written.
		 * 
		 * @param b An array of bytes
		 * @param ind the first index to write to.
		 * @param topBounds Will treat the array as if this is its length, including throwing
		 * 			IndexOutOfBoundsExceptions.
		 * 
		 * @return The number of bytes written.
		 * 
		 */
		public int writeToByteArray( byte [] b, int ind, int topBounds ) throws IndexOutOfBoundsException {
			int indOrig = ind ;
			
			// For each field, we first write 0 or 1, indicating whether it is
			// included.  The exception is R and C, which will ALWAYS be written.
			// We assume that R, C will be less than Byte.MAX_VALUE.
			
			// We write version, R and C.
			//log("writeToByteArray -- writing version, R, C with byte array " + b) ;
			if ( b == null )
				ind += 6 ;
			else {
				ByteArrayOps.writeIntAsBytes(VERSION, b, ind) ;
				ind += 4 ;
				b[ind++] = (byte) mR ;
				b[ind++] = (byte) mC ;
			}
			
			// BlockField.  These methods are null-array safe, so we
			// don't need special case handling for b == null.
			// rows included?
			//log("writeToByteArray -- writing blockfield with byte array " + b) ;
			if ( b == null ) {
				ind++ ;		// block field update type
			} else {
				b[ind++] = (byte)mBlockFieldUpdateType.ordinal() ;
			}
			if ( mBlockFieldUpdateType != FieldUpdateType.EMPTY ) {
				ind += ByteArrayOps.toBytes( mBlockFieldRowUpdated, b, ind ) ;
				// rows themselves?
				ind += ByteArrayOps.toBytes( mBlockField, mBlockFieldRowUpdated, b, ind ) ;
			}
			
			// Displacement rows.  These methods are null-array safe, so we
			// don't need special case handling for b == null.
			// rows included?
			//log("writeToByteArray -- writing displacement with byte array " + b) ;
			if ( b == null ) {
				ind++ ;		// block field update type
			} else {
				b[ind++] = (byte)mDisplacementRowsUpdateType.ordinal() ;
			}
			if ( mDisplacementRowsUpdateType != FieldUpdateType.EMPTY ) {
				ind += ByteArrayOps.toBytes( mDisplacementRowUpdated, b, ind ) ;
				// rows themselves?
				ind += ByteArrayOps.toBytes( mDisplacementRows, mDisplacementRowUpdated, b, ind ) ;
			}
			
			// Next pieces and reserve pieces.  Pieces are small enough that we can
			// use the full size of an int (writing to 4 consecutive bytes).
			// We use big-endian representation.
			//log("writeToByteArray -- writing next/reserve with byte array " + b) ;
			if ( b == null ) {
				ind += mNextPiecesUpdated 		? 1 + 5 * GameState.PIECE_LOOKAHEAD : 1 ;
				ind += mReservePiecesUpdated 	? 1 + 5 * GameState.PIECE_LOOKAHEAD : 1 ;
			} else {
				b[ind++] = (byte)(mNextPiecesUpdated ? 1 : 0) ;
				if ( mNextPiecesUpdated ) {
					for ( int i = 0; i < GameState.PIECE_LOOKAHEAD; i++ ) {
						ByteArrayOps.writeIntAsBytes( this.mNextPieces[i], b, ind ) ;
						ind += 4 ;
						b[ind++] = this.mNextPiecesDefaultRotation[i] ;
					}
				}
				b[ind++] = (byte)(mReservePiecesUpdated ? 1 : 0) ;
				if ( mReservePiecesUpdated ) {
					for ( int i = 0; i < GameState.PIECE_LOOKAHEAD; i++ ) {
						ByteArrayOps.writeIntAsBytes( this.mReservePieces[i], b, ind ) ;
						ind += 4 ;
						b[ind++] = this.mReservePiecesDefaultRotation[i] ;
					}
				}
			}
			
			// Write game information!  (or rather, let it write itself...)
			//log("writeToByteArray -- writing ginfo with starting offset " + (ind - indOrig) + " and byte array " + b) ;
			if ( b == null ) {
				ind += mGinfo.writeToByteArray(null, ind, topBounds) ;
			} else {
				ind += mGinfo.writeToByteArray(b, ind, topBounds) ;
			}
			
			// Write attack descriptors.
			//log("writeToByteArray -- writing attack descriptors with byte array " + b) ;
			if ( b == null ) {
				// include update type explicitly, as a byte..
				ind ++ ;
				switch( mAttackDescriptorsQueueUpdateType ) {
				case NONE:
					break ;
				case EXPLICIT:
					// num attack descriptors
					ind += 4 ;
					// attack descriptors themselves
					for ( int i = 0; i < mNumAttackDescriptors; i++ ) {
						ind += mAttackDescriptors.get(i).writeToByteArray( null, ind, topBounds ) ;
					}
					break ;
				case DEQUEUE_AND_ENQUEUE:
					// num attack descriptors to dequeue and enqueue
					ind += 8 ;
					// attack descriptors to enqueue
					for ( int i = 0; i < mNumAttackDescriptors; i++ ) {
						ind += mAttackDescriptors.get(i).writeToByteArray( null, ind, topBounds ) ;
					}
					break ;
				default:
					throw new IllegalStateException("Can't encode QueueUpdateType " + mAttackDescriptorsQueueUpdateType) ; 
				}
			} else {
				// include update type explicitly..
				b[ind++] = (byte)(mAttackDescriptorsQueueUpdateType.ordinal()) ;
				switch( mAttackDescriptorsQueueUpdateType ) {
				case NONE:
					break ;
				case EXPLICIT:
					// num attack descriptors
					ByteArrayOps.writeIntAsBytes(mNumAttackDescriptors, b, ind) ;
					ind += 4 ;
					// attack descriptors themselves
					for ( int i = 0; i < mNumAttackDescriptors; i++ ) {
						ind += mAttackDescriptors.get(i).writeToByteArray( b, ind, topBounds ) ;
					}
					break ;
				case DEQUEUE_AND_ENQUEUE:
					// num attack descriptors to dequeue and enqueue
					ByteArrayOps.writeIntAsBytes(this.mNumAttackDescriptorsToDequeue, b, ind) ;
					ByteArrayOps.writeIntAsBytes(this.mNumAttackDescriptors, b, ind + 4) ;
					ind += 8 ;
					// attack descriptors to enqueue
					for ( int i = 0; i < mNumAttackDescriptors; i++ ) {
						ind += mAttackDescriptors.get(i).writeToByteArray( b, ind, topBounds ) ;
					}
					break ;
				default:
					throw new IllegalStateException("Can't encode QueueUpdateType " + mAttackDescriptorsQueueUpdateType) ; 
				}
			}
			
			// Dequeue attack and number to transfer are always included.
			//log("writeToByteArray -- writing always-included values with byte array " + b) ;
			if ( b == null ) {
				ind += 2 ;
			} else {
				b[ind++] = (byte)((mDequeueAttackThisCycle) ? 1 : 0) ;
				b[ind++] = mDisplacedRowsToTransferThisCycle ;
			}
			
			// Bounds check and return.
			if ( ind > topBounds )
				throw new IndexOutOfBoundsException("ActionCycleStateDescriptor.writeToByteArray wrote past the provided top bounds: wrote to from " + indOrig + " to " + ind + ", with top bounds " + topBounds) ;
			
			if ( isFullUpdate() )
				log("writeToByteArray - wrote FULL UPDATE to byte array with total length " + (ind - indOrig)) ;
			else
				log("writeToByteArray - wrote update to byte array with total length " + (ind - indOrig)) ;
			
			return ind - indOrig ;
		}
		
		/**
		 * Reads the current contents of this ActionCycleStateDescriptor to
		 * the provided byte array, beginning at 'ind'.
		 * 
		 * Returns the number of bytes read.  For @return val, ind+val is the
		 * 
		 * 
		 * In the event that an exception is thrown, this method has written from 'ind'
		 * to the maximum bound, whether topBounds or b.length.
		 * 
		 * Take care that topBounds is not < ind, or this method may throw an
		 * exception even if the entire length of the byte representation has been
		 * written.
		 * 
		 * @param b An array of bytes
		 * @param ind the first index to write to.
		 * @param topBounds Will treat the array as if this is its length, including throwing
		 * 			IndexOutOfBoundsExceptions.
		 * 
		 * @return The number of bytes written.
		 * 
		 */
		public int readFromByteArray( byte [] b, int ind ) throws IndexOutOfBoundsException {
			
			int origInd = ind ;
			
			//log("readFromByteArray -- reading version, R and C") ;
			
			int version = ByteArrayOps.readIntAsBytes(b, ind) ;
			ind += 4 ;
			
			if ( version < 3 || version > VERSION )
				throw new IllegalStateException("Don't know how to read version " + version) ;
			
			// Verify that R,C fit.
			byte R_in = b[ind++] ;
			byte C_in = b[ind++] ;
			if ( R_in != mR || C_in != mC )
				initialize(R_in, C_in) ;
			
			// BlockField.  These methods are null-array safe, so we
			// don't need special case handling for b == null.
			// rows included?
			//log("readFromByteArray -- reading blockfield") ;
			mBlockFieldUpdateType = FieldUpdateType.values()[b[ind++]] ;
			if ( mBlockFieldUpdateType != FieldUpdateType.EMPTY ) {
				ind += ByteArrayOps.fromBytes( mBlockFieldRowUpdated, b, ind ) ;
				// rows themselves?
				ind += ByteArrayOps.fromBytes( mBlockField, mBlockFieldRowUpdated, b, ind ) ;
			}
			
			// Displacement rows.  These methods are null-array safe, so we
			// don't need special case handling for b == null.
			// rows included?
			//log("readFromByteArray -- reading displacement") ;
			mDisplacementRowsUpdateType = FieldUpdateType.values()[b[ind++]] ;
			if ( mDisplacementRowsUpdateType != FieldUpdateType.EMPTY ) {
				ind += ByteArrayOps.fromBytes( mDisplacementRowUpdated, b, ind ) ;
				// rows themselves?
				ind += ByteArrayOps.fromBytes( mDisplacementRows, mDisplacementRowUpdated, b, ind ) ;
			}
			
			// Next pieces and reserve pieces.  Pieces are small enough that we can
			// use the full size of an int (writing to 4 consecutive bytes).
			// We use big-endian representation.
			//log("readFromByteArray -- reading next pieces") ;
			mNextPiecesUpdated = b[ind++] != 0 ;
			if ( mNextPiecesUpdated ) {
				for ( int i = 0; i < GameState.PIECE_LOOKAHEAD; i++ ) {
					mNextPieces[i] = ByteArrayOps.readIntAsBytes( b, ind ) ;
					ind += 4 ;
					mNextPiecesDefaultRotation[i] = b[ind++] ;
				}
			}
			//log("readFromByteArray -- reading reserve pieces") ;
			mReservePiecesUpdated = b[ind++] != 0 ;
			if ( mReservePiecesUpdated ) {
				for ( int i = 0; i < GameState.PIECE_LOOKAHEAD; i++ ) {
					mReservePieces[i] = ByteArrayOps.readIntAsBytes( b, ind ) ;
					ind += 4 ;
					mReservePiecesDefaultRotation[i] = b[ind++] ;
				}
			}
			
			// Read game information!  (or rather, let it read itself...)
			//log("readFromByteArray -- reading ginfo with starting offset " + (ind - origInd) + " and byte array " + b) ;
			ind += mGinfo.readFromByteArray(b, ind) ;
			
			// Read attack descriptors.
			// Update type included as an ordinal...
			//log("readFromByteArray -- reading attack descriptors") ;
			mAttackDescriptorsQueueUpdateType = QueueUpdateType.values()[b[ind++]] ;
			switch( mAttackDescriptorsQueueUpdateType ) {
			case NONE:
				break ;
			case EXPLICIT:
				// num attack descriptors
				mNumAttackDescriptors = ByteArrayOps.readIntAsBytes(b, ind) ;
				ind += 4 ;
				// attack descriptors themselves
				for ( int i = 0; i < mNumAttackDescriptors; i++ ) {
					if ( mAttackDescriptors.size() <= i ) {
						mAttackDescriptors.add(new AttackDescriptor(mR, mC)) ;
					}
					ind += mAttackDescriptors.get(i).readFromByteArray(b, ind) ;
				}
				break ;
			case DEQUEUE_AND_ENQUEUE:
				// num attack descriptors to dequeue / enqueue
				mNumAttackDescriptorsToDequeue = ByteArrayOps.readIntAsBytes(b, ind) ;
				mNumAttackDescriptors = ByteArrayOps.readIntAsBytes(b, ind + 4) ;
				ind += 8 ;
				// attack descriptors themselves
				for ( int i = 0; i < mNumAttackDescriptors; i++ ) {
					if ( mAttackDescriptors.size() <= i ) {
						mAttackDescriptors.add(new AttackDescriptor(mR, mC)) ;
					}
					ind += mAttackDescriptors.get(i).readFromByteArray(b, ind) ;
				}
				break ;
			default:
				throw new IllegalStateException("Can't encode QueueUpdateType " + mAttackDescriptorsQueueUpdateType) ; 
			}
			
			//log("readFromByteArray -- reading always-included info") ;
			mDequeueAttackThisCycle = b[ind++] != 0 ;
			mDisplacedRowsToTransferThisCycle = b[ind++] ;
			
			if ( isFullUpdate() )
				log("readFromByteArray - read FULL UPDATE to byte array with total length " + (ind - origInd)) ;
			else
				log("readFromByteArray - read update to byte array with total length " + (ind - origInd)) ;
			
			// Return length
			return ind - origInd ;
		}
		
	}
	
}
