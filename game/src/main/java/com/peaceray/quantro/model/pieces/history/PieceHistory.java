package com.peaceray.quantro.model.pieces.history;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;

/**
 * PieceHistory: Yet another "system."  We store a history of the usage of
 * 		pieces over the course of the game.
 * 
 * We base our information on pieceType.  Each type gets its own set of
 * 		data which we track through the course of the game.
 * 
 * 		# popped from each available bag
 * 		# landings / lockings (popped - this = # pushed into reserve)
 * 		# other pieces popped with this as the "next" piece in each bag
 * 		# MAXIMUM RUN of the above over game
 * 		# of times got:
 * 			1 clear, 2 clear, 3 clear, 4 clear initial
 * 		maximum # clears initial, and # times got this number
 * 		maximum # clears resulting including cascades, and # times got this number
 * 		maximum # cascades resulting, and # times got this number
 * 		total # rows cleared by this piece
 * 		total # cascades caused by this piece
 * 		# "successful" uses <-- defined by outside.  for example, SL-activation can be a "success"
 * 		# "failed" uses <-- defined by outside.  for example, SL-lock w/o activation can be a "failure"
 * 			note: #success + #failure != # landed.  Some pieces
 * 				just land without it counting as a success/failure.
 * 		last use: success, failure, neither?
 * 
 * 
 * Like most systems, it implements SerializableState.
 * 
 * @author Jake
 *
 */
public class PieceHistory implements SerializableState {

	private class PieceTypeHistory implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -2354697768149091489L;

		int mPieceType ;		// the type of this piece
		
		// popping, landing, holding
		int [] mNumberPopped ;	// indexed by bag index
		int [] mNumberLanded ;	// indexed by the bag it popped from
		int [] mNumberTimesHeld ;	// indexed by bag index.
		int [] mNumberTimesHeldMaxRun ;	 	// indexed by bag index
		
		// clears
		int [] mNumberTimesClearedNRows ;	// indexed by # clears (1-4)
		int mTotalClearedRows ;		// # rows cleared by this piece, including cascades
		int mTotalClearCascades ;	// # cascades caused by this piece
		int mTotalClearedRowsInitial ;
		int mMaxClearedRowsInitial ;		// maximum # initial clears after landing (w/o cascade)
		int mMaxClearedRowsInitialNumberTimes ;	// # of times we got this "max"
		int mMaxClearedRows;			// max # clears after landing this piece
		int mMaxClearedRowsNumberTimes ;	// # of times we got the "max"
		
		// uses
		int mNumberLandedSuccesses ;	// we are told when a success occurs
		int mNumberLandedFailures ;		// we are told when a failure occurs
		
		// "most recent"
		int mLastPoppedCounter ;		// The "counter" value for the last time this piece popped
		int mLastLandedCounter ;		// The "counter" value for the last time this piece landed
		int mLastSuccessCounter ;		// The last time we "succeeded."
		int mLastFailureCounter ;		// The last time we "failed."
		
		PieceTypeHistory( int type, int numBags ) {
			mPieceType = type ;
			mNumberPopped = new int[numBags] ;
			mNumberLanded = new int[numBags] ;
			mNumberTimesHeld = new int[numBags] ;
			mNumberTimesHeldMaxRun = new int[numBags] ;
			
			mNumberTimesClearedNRows = new int[4] ;
		}
		
		PieceTypeHistory( PieceTypeHistory pth ) {
			mPieceType = pth.mPieceType ;		// the type of this piece
			
			// popping, landing, holding
			mNumberPopped = ArrayOps.duplicate(pth.mNumberPopped) ;	// indexed by bag index
			mNumberLanded = ArrayOps.duplicate(pth.mNumberLanded) ;	// indexed by the bag it popped from
			mNumberTimesHeld = ArrayOps.duplicate(pth.mNumberTimesHeld) ;	// indexed by bag index.
			mNumberTimesHeldMaxRun = ArrayOps.duplicate(pth.mNumberTimesHeldMaxRun) ;	 	// indexed by bag index
			
			// clears
			mNumberTimesClearedNRows = ArrayOps.duplicate(pth.mNumberTimesClearedNRows) ;	// indexed by # clears (1-4)
			mTotalClearedRows = pth.mTotalClearedRows ;		// # rows cleared by this piece, including cascades
			mTotalClearCascades = pth.mTotalClearCascades ;	// # cascades caused by this piece
			mTotalClearedRowsInitial = pth.mTotalClearedRowsInitial ;
			mMaxClearedRowsInitial = pth.mMaxClearedRowsInitial ;		// maximum # initial clears after landing (w/o cascade)
			mMaxClearedRowsInitialNumberTimes = pth.mMaxClearedRowsInitialNumberTimes ;	// # of times we got this "max"
			mMaxClearedRows = pth.mMaxClearedRows ;			// max # clears after landing this piece
			mMaxClearedRowsNumberTimes = pth.mMaxClearedRowsNumberTimes ;	// # of times we got the "max"
			
			// uses
			mNumberLandedSuccesses = pth.mNumberLandedSuccesses ;	// we are told when a success occurs
			mNumberLandedFailures = pth.mNumberLandedFailures  ;		// we are told when a failure occurs
			
			// "most recent"
			mLastPoppedCounter = pth.mLastPoppedCounter ;		// The "counter" value for the last time this piece popped
			mLastLandedCounter = pth.mLastLandedCounter  ;		// The "counter" value for the last time this piece landed
			mLastSuccessCounter = pth.mLastSuccessCounter  ;		// The last time we "succeeded."
			mLastFailureCounter = pth.mLastFailureCounter ;		// The last time we "failed."
		}
		
		
		public void clear() {
			mPieceType = -1 ;
			
			for ( int i = 0; i < mNumberPopped.length; i++ ) {
				mNumberPopped[i] = 0 ;
				mNumberLanded[i] = 0 ;
				mNumberTimesHeld[i] = 0 ;
				mNumberTimesHeldMaxRun[i] = 0; 
			}
			
			for ( int i = 0; i < mNumberTimesClearedNRows.length; i++ )
				mNumberTimesClearedNRows[i] = 0 ;
			
			mTotalClearedRows = 0 ;
			mTotalClearCascades = 0 ;
			mMaxClearedRowsInitial = 0 ;
			mMaxClearedRowsInitialNumberTimes = 0 ;
			mMaxClearedRows = 0 ;
			mMaxClearedRowsNumberTimes = 0 ;
			
			mNumberLandedSuccesses = 0 ;
			mNumberLandedFailures = 0 ;
			
			mLastPoppedCounter = 0 ;
			mLastLandedCounter = 0 ;
			mLastSuccessCounter = 0 ;
			mLastFailureCounter = 0 ;
		}
		
		/////////////////////////////////////////////
		// serializable methods
		private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
			stream.writeInt(mPieceType) ;
			
			// write # bags as array length
			stream.writeInt( mNumberPopped.length ) ;
			for ( int i = 0; i < mNumberPopped.length; i++ )
				stream.writeInt( mNumberPopped[i] ) ;
			for ( int i = 0; i < mNumberLanded.length; i++ )
				stream.writeInt( mNumberLanded[i] ) ;
			for ( int i = 0; i < mNumberTimesHeld.length; i++ )
				stream.writeInt( mNumberTimesHeld[i] ) ;
			for ( int i = 0; i < mNumberTimesHeldMaxRun.length; i++ )
				stream.writeInt( mNumberTimesHeldMaxRun[i] ) ;
			
			for ( int i = 0; i < 4; i++ )
				stream.writeInt(mNumberTimesClearedNRows[i]) ;
			stream.writeInt( mTotalClearedRows ) ;
			stream.writeInt( mTotalClearCascades ) ;
			stream.writeInt( mTotalClearedRowsInitial ) ;
			stream.writeInt( mMaxClearedRowsInitial ) ;
			stream.writeInt( mMaxClearedRowsInitialNumberTimes ) ;
			stream.writeInt( mMaxClearedRows ) ;
			stream.writeInt( mMaxClearedRowsNumberTimes ) ;
			
			stream.writeInt( mNumberLandedSuccesses ) ;
			stream.writeInt( mNumberLandedFailures ) ;
			
			stream.writeInt( mLastPoppedCounter ) ;
			stream.writeInt( mLastLandedCounter ) ;
			stream.writeInt( mLastSuccessCounter ) ;
			stream.writeInt( mLastFailureCounter ) ;
		}
		
		private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
			mPieceType = stream.readInt() ;
			
			// write # bags as array length
			int numBags = stream.readInt();
			mNumberPopped = new int[numBags] ;
			mNumberLanded = new int[numBags] ;
			mNumberTimesHeld = new int[numBags] ;
			mNumberTimesHeldMaxRun = new int[numBags] ;
			for ( int i = 0; i < mNumberPopped.length; i++ )
				mNumberPopped[i] = stream.readInt();
			for ( int i = 0; i < mNumberLanded.length; i++ )
				mNumberLanded[i] = stream.readInt();
			for ( int i = 0; i < mNumberTimesHeld.length; i++ )
				mNumberTimesHeld[i] = stream.readInt();
			for ( int i = 0; i < mNumberTimesHeldMaxRun.length; i++ )
				mNumberTimesHeldMaxRun[i] = stream.readInt();
			
			mNumberTimesClearedNRows = new int[4] ;
			for ( int i = 0; i < 4; i++ )
				mNumberTimesClearedNRows[i] = stream.readInt() ;
			mTotalClearedRows = stream.readInt() ;
			mTotalClearCascades = stream.readInt() ;
			mTotalClearedRowsInitial = stream.readInt() ;
			mMaxClearedRowsInitial = stream.readInt() ;
			mMaxClearedRowsInitialNumberTimes = stream.readInt() ;
			mMaxClearedRows = stream.readInt() ;
			mMaxClearedRowsNumberTimes = stream.readInt() ;
			
			mNumberLandedSuccesses = stream.readInt() ;
			mNumberLandedFailures = stream.readInt() ;
			
			mLastPoppedCounter = stream.readInt() ;
			mLastLandedCounter = stream.readInt() ;
			mLastSuccessCounter = stream.readInt() ;
			mLastFailureCounter = stream.readInt() ;
		}
		
		@SuppressWarnings("unused")
		private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
			throw new ClassNotFoundException("Stream does not match required system state structure.") ;
		}
		
		
		PieceTypeHistory qUpconvert() {
			PieceTypeHistory pth = new PieceTypeHistory(this) ;
			pth.mPieceType = PieceCatalog.qUpconvert(pth.mPieceType) ;
			return pth ;
		}
		
	}
	
	
	/**
	 * The "state" of a PieceHistory.
	 * 
	 * Includes both an array of PieceTypeHistories, and some
	 * useful "current state" metadata that isn't necessarily associated
	 * with a piece.
	 * 
	 * @author Jake
	 *
	 */
	private class PieceHistoryState implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -5967704394601028310L;

		ArrayList<PieceTypeHistory> mPieceTypeHistory ;
		
		int [] mHeldInBag ;
		int [] mCounterWhenHeldInBag ;
		
		int mPieceCounter ;		// incremented with every pop.
		PieceTypeHistory mCurrentHistory ;		// a "history" showing our most recent piece info.
												// e.g., any "total" value is "since this piece entered."
		
		PieceHistoryState( ArrayList<int []> pieceTypes ) {
			// initialize the type history using insertion-sort.
			mPieceTypeHistory = new ArrayList<PieceTypeHistory>() ;
			for ( int i = 0; i < pieceTypes.size(); i++ ) {
				int [] types = pieceTypes.get(i) ;
				for ( int j = 0; j < types.length; j++ ) {
					// if we have this type, do nothing.  If we
					// DON'T, make one and insert in the right place.
					if ( pieceTypeHistoryIndex( types[j] ) == -1 ) {
						PieceTypeHistory pth = new PieceTypeHistory( types[j], pieceTypes.size() ) ;
						int newIndex = 0 ;
						while ( newIndex < mPieceTypeHistory.size() && types[j] > mPieceTypeHistory.get(newIndex).mPieceType )
							newIndex++ ;
						
						mPieceTypeHistory.add(newIndex, pth) ;
					}
				}
			}
			
			mHeldInBag = new int[pieceTypes.size()] ;
			mCounterWhenHeldInBag = new int[pieceTypes.size()] ;
			for ( int i = 0; i < pieceTypes.size(); i++ ) {
				mHeldInBag[i] = -1 ;
				mCounterWhenHeldInBag[i] = -1 ;
			}
			
			// initialize the rest
			mPieceCounter = -1 ;
			mCurrentHistory = new PieceTypeHistory( -1, pieceTypes.size() ) ;
		}
		
		
		PieceHistoryState( PieceHistoryState phs ) {
			 mPieceTypeHistory = new ArrayList<PieceTypeHistory>() ;
			 Iterator<PieceTypeHistory> iter = phs.mPieceTypeHistory.iterator() ;
			 for ( ; iter.hasNext() ; )
				 mPieceTypeHistory.add( new PieceTypeHistory( iter.next() ) ) ;
			
			mHeldInBag = ArrayOps.duplicate(phs.mHeldInBag) ;
			mCounterWhenHeldInBag = ArrayOps.duplicate(phs.mCounterWhenHeldInBag);
			
			mPieceCounter = phs.mPieceCounter ;		// incremented with every pop.
			mCurrentHistory = new PieceTypeHistory(phs.mCurrentHistory);
		}
		
		int pieceTypeHistoryIndex( int pieceType ) {
			int boundMin = 0 ;
			int boundMax = mPieceTypeHistory.size() - 1 ;

			while ( boundMin < boundMax ) {
				int indexMid = ( boundMax + boundMin ) / 2 ;
				int typeMid = mPieceTypeHistory.get(indexMid).mPieceType ;
				if ( typeMid < pieceType )
					boundMin = indexMid + 1 ;
				else if ( typeMid > pieceType )
					boundMax = indexMid - 1 ;
				else
					return indexMid ;
			}
			
			if ( boundMin == boundMax
					&& pieceType == mPieceTypeHistory.get(boundMin).mPieceType )
				return boundMin ;
			
			return -1 ;
		}
		
		/////////////////////////////////////////////
		// serializable methods
		private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
			stream.writeInt( mPieceTypeHistory.size() ) ;
			for ( int i = 0; i < mPieceTypeHistory.size(); i++ )
				stream.writeObject( mPieceTypeHistory.get(i) ) ;
			
			// Write # bags as length
			stream.writeInt( mHeldInBag.length ) ;
			for ( int i = 0; i < mHeldInBag.length; i++ )
				stream.writeInt( mHeldInBag[i] ) ;
			for ( int i = 0; i < mCounterWhenHeldInBag.length; i++ )
				stream.writeInt( mCounterWhenHeldInBag[i] ) ;
			
			stream.writeInt( mPieceCounter ) ;
			stream.writeObject( mCurrentHistory ) ;
		}
		
		private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
			int numTypes = stream.readInt() ;
			mPieceTypeHistory = new ArrayList<PieceTypeHistory>() ;
			for ( int i = 0; i < numTypes; i++ )
				mPieceTypeHistory.add( (PieceTypeHistory)stream.readObject() ) ;
			
			// Write # bags as length
			int numBags = stream.readInt() ;
			mHeldInBag = new int[numBags] ;
			mCounterWhenHeldInBag = new int[numBags] ;
			for ( int i = 0; i < mHeldInBag.length; i++ )
				mHeldInBag[i] = stream.readInt() ;
			for ( int i = 0; i < mCounterWhenHeldInBag.length; i++ )
				mCounterWhenHeldInBag[i] = stream.readInt() ;
			
			mPieceCounter = stream.readInt() ;
			mCurrentHistory = (PieceTypeHistory)stream.readObject() ;
		}
		
		@SuppressWarnings("unused")
		private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
			throw new ClassNotFoundException("Stream does not match required system state structure.") ;
		}
		
		
		public PieceHistoryState qUpconvert() {
			PieceHistoryState phs = new PieceHistoryState(this) ;
			for ( int i = 0; i < mPieceTypeHistory.size(); i++ )
				phs.mPieceTypeHistory.set(i, mPieceTypeHistory.get(i).qUpconvert()) ;
			for ( int i = 0; i < mHeldInBag.length; i++ ) {
				phs.mHeldInBag[i] = PieceCatalog.qUpconvert(mHeldInBag[i]) ;
				phs.mCounterWhenHeldInBag[i] = PieceCatalog.qUpconvert(mCounterWhenHeldInBag[i]) ;
			}
			
			phs.mCurrentHistory = mCurrentHistory == null ? null : mCurrentHistory.qUpconvert() ;
			
			return phs ;
		}
	}
	
	public static final int PIECE_BAG = 0 ;
	public static final int RESERVE_BAG = 1 ;
	public static final int OTHER_BAG_OFFSET = 2 ;
	
	private PieceHistoryState s ;
	private PieceTypeHistory mPTHQueryCache ;
	private boolean mConsolidateClears ;
	
	private ArrayList<int []> bagContents ;
	private boolean configured ;
	
	private GameInformation ginfo ;
	
	public PieceHistory( GameInformation ginfo ) {
		s = null ;
		mPTHQueryCache = null ;
		mConsolidateClears = false ;
		
		bagContents = new ArrayList<int []>() ;
		configured = false ;
		
		setGameInformation(ginfo) ;
	}
	
	public PieceHistory setGameInformation( GameInformation ginfo ) {
		this.ginfo = ginfo ;
		return this ;
	}
	
	public GameInformation getGameInformation() {
		return this.ginfo ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// RECORDING EVENTS
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * The specified piece has just been popped from the specified bag.
	 * 
	 * Should be called exactly once per piece preparation.  It is also
	 * appropriate to call when a reserve piece is swapped.
	 * 
	 * @param pieceType
	 * @param sourceBag
	 */
	public void setPiecePopped( int pieceType, int sourceBag ) {
		if ( !configured )
			throw new IllegalStateException("Finalize first.") ;
		
		// 1st: if we have something in "current history," note it
		// in the actual history.
		setDoneWithCurrentPiece() ;
		
		// 2nd: increment counter
		s.mPieceCounter++ ;
		
		// 3rd: establish a "current history."
		s.mCurrentHistory.mPieceType = pieceType ;
		s.mCurrentHistory.mNumberPopped[sourceBag] = 1 ;
		s.mCurrentHistory.mLastPoppedCounter = s.mPieceCounter ;
		
		// 4th: how long did we hold this piece?
		if ( s.mHeldInBag[sourceBag] == pieceType )
			s.mCurrentHistory.mNumberTimesHeld[sourceBag] = s.mPieceCounter - s.mCounterWhenHeldInBag[sourceBag] ;
			
		s.mHeldInBag[sourceBag] = -1 ;
	}
	
	
	/**
	 * Notes that a piece has been pushed into the specified bag.
	 * 
	 * @param destBag
	 */
	public void setPiecePushedIntoBag( int destBag ) {
		if ( !configured )
			throw new IllegalStateException("Finalize first.") ;
		
		if ( s.mHeldInBag[destBag] > -1 ) {
			int index = s.pieceTypeHistoryIndex(s.mHeldInBag[destBag]) ;
			if ( index > -1 ) {
				PieceTypeHistory pth = s.mPieceTypeHistory.get(index) ;
				int timeHeld = s.mPieceCounter - s.mCounterWhenHeldInBag[destBag] ;
				pth.mNumberTimesHeld[destBag] += timeHeld ;
				if ( timeHeld > pth.mNumberTimesHeldMaxRun[destBag] ) {
					pth.mNumberTimesHeldMaxRun[destBag] = timeHeld ;
				}
			}
			
			// Reset the held counter.  Do this so we don't accidentally
			// count this twice if someone calls "piecePushedIntoBag"
			// followed by pieceHeldInBag.
			s.mCounterWhenHeldInBag[destBag] = s.mPieceCounter ;
		}
	}
	
	/**
	 * Notes that the specified piece is currently held in the bag.
	 * Should be called after piecePopped and after piecePushedIntoBag.
	 * 
	 * @param pieceType
	 * @param sourceBag
	 */
	public void setPieceHeldInBag( int pieceType, int sourceBag ) {
		if ( !configured )
			throw new IllegalStateException("Finalize first.") ;
		
		if ( s.mHeldInBag[sourceBag] != pieceType ) {
			s.mHeldInBag[sourceBag] = pieceType ;
			s.mCounterWhenHeldInBag[sourceBag] = s.mPieceCounter ;
		}
	}
	
	
	public void setPieceLanded() {
		if ( !configured )
			throw new IllegalStateException("Finalize first.") ;
		
		for ( int i = 0; i < s.mCurrentHistory.mNumberLanded.length; i++ ) {
			if ( s.mCurrentHistory.mNumberPopped[i] > 0 ) {
				if ( s.mCurrentHistory.mNumberLanded[i] > 0 )
					throw new IllegalStateException("Are you trying to land piece twice?  Did you remember to call piecePopped?") ;
				s.mCurrentHistory.mNumberLanded[i] = 1 ;
			}
		}
	}
	
	
	public void setClear( int [] clears, boolean [] monoclears ) {
		if ( !configured )
			throw new IllegalStateException("Finalize first.") ;
		
		int numberClears = 0 ;
		
		for ( int i = 0; i < clears.length; i++ ) {
			if ( mConsolidateClears ) {
				if ( clears[i] > 0 || monoclears[i] )
					numberClears++ ;
			} else {
				if ( monoclears[i] )
					numberClears += 2 ;
				else if ( clears[i] == QOrientations.S0 )
					numberClears++ ;
				else if ( clears[i] == QOrientations.S1 )
					numberClears++ ;
				else if ( clears[i] == QCombinations.SS || clears[i] == QCombinations.SL )
					numberClears += 2 ;
			}
		}
		
		int cascadeNumber = s.mCurrentHistory.mTotalClearCascades ;
		
		if ( cascadeNumber == 0 && numberClears <= 4 ) {
			s.mCurrentHistory.mNumberTimesClearedNRows[numberClears-1] = 1 ;
			s.mCurrentHistory.mTotalClearedRowsInitial = numberClears ;
		}
		
		s.mCurrentHistory.mTotalClearedRows += numberClears ;
		s.mCurrentHistory.mTotalClearCascades += 1 ;
	}
	
	
	public void setSuccess() {
		if ( !configured )
			throw new IllegalStateException("Finalize first.") ;
		
		s.mCurrentHistory.mNumberLandedSuccesses = 1 ;
	}
	
	public void setFailure() {
		if ( !configured )
			throw new IllegalStateException("Finalize first.") ;
		
		s.mCurrentHistory.mNumberLandedFailures = 1 ;
	}
	
	
	public void setDoneWithCurrentPiece() {
		if ( !configured )
			throw new IllegalStateException("Finalize first.") ;
		
		
		
		// First: migrate the data stored in current history to our actual history.
		int index = s.pieceTypeHistoryIndex( s.mCurrentHistory.mPieceType ) ;
		if ( index < 0 ) {
			s.mCurrentHistory.clear() ;
			return ;
		}
		
		PieceTypeHistory pth = s.mPieceTypeHistory.get(index) ;
		// times popped etc.
		for ( int i = 0; i < pth.mNumberPopped.length; i++ ) {
			pth.mNumberPopped[i] += s.mCurrentHistory.mNumberPopped[i] ;
			pth.mNumberLanded[i] += s.mCurrentHistory.mNumberLanded[i] ;
			pth.mNumberTimesHeld[i] += s.mCurrentHistory.mNumberTimesHeld[i] ;
			
			if ( pth.mNumberTimesHeldMaxRun[i] < s.mCurrentHistory.mNumberTimesHeld[i] )
				pth.mNumberTimesHeldMaxRun[i] = s.mCurrentHistory.mNumberTimesHeld[i] ;
		}
		// clears
		for ( int i = 0; i < pth.mNumberTimesClearedNRows.length; i++ )
			pth.mNumberTimesClearedNRows[i] += s.mCurrentHistory.mNumberTimesClearedNRows[i] ;
		pth.mTotalClearedRows += s.mCurrentHistory.mTotalClearedRows ;
		pth.mTotalClearCascades += s.mCurrentHistory.mTotalClearCascades ;
		pth.mTotalClearedRowsInitial += s.mCurrentHistory.mTotalClearedRowsInitial ;
		if ( pth.mMaxClearedRows == s.mCurrentHistory.mTotalClearedRows )
			pth.mMaxClearedRowsNumberTimes++ ;
		else if ( pth.mMaxClearedRows < s.mCurrentHistory.mTotalClearedRows ) {
			pth.mMaxClearedRows = Math.max( pth.mMaxClearedRows, s.mCurrentHistory.mTotalClearedRows ) ;
			pth.mMaxClearedRowsNumberTimes = 1 ;
		}
		if ( pth.mMaxClearedRowsInitial == s.mCurrentHistory.mTotalClearedRowsInitial )
			pth.mMaxClearedRowsInitialNumberTimes++ ;
		else if ( pth.mMaxClearedRowsInitial < s.mCurrentHistory.mTotalClearedRowsInitial ) {
			pth.mMaxClearedRowsInitial = Math.max( pth.mMaxClearedRowsInitial, s.mCurrentHistory.mTotalClearedRowsInitial ) ;
			pth.mMaxClearedRowsInitialNumberTimes = 1 ;
		}
		
		// success/fail
		pth.mNumberLandedSuccesses += s.mCurrentHistory.mNumberLandedSuccesses ;
		pth.mNumberLandedFailures += s.mCurrentHistory.mNumberLandedFailures ;
		
		// counters
		pth.mLastPoppedCounter = s.mPieceCounter ;
		for ( int i = 0; i < s.mCurrentHistory.mNumberLanded.length; i++ )
			if ( s.mCurrentHistory.mNumberLanded[i] > 0 )
				pth.mLastLandedCounter = s.mPieceCounter ;
		if ( s.mCurrentHistory.mNumberLandedSuccesses > 0 )
			pth.mLastSuccessCounter = s.mPieceCounter ;
		if ( s.mCurrentHistory.mNumberLandedFailures > 0 )
			pth.mLastFailureCounter = s.mPieceCounter ;
		
		// One last thing: check pth.mTotalClearedRows against the GameInformation's
		// current best cascade.
		ginfo.longestCascade = Math.max( ginfo.longestCascade, s.mCurrentHistory.mTotalClearedRows ) ;
		
		// Last: empty our current history so that 1. we don't accidentally do
		// this again, and 2. we are ready for the next piece.
		s.mCurrentHistory.clear() ;
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// ACCESSING HISTORY
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	public int getNumberPopped( int pieceType ) {
		if ( !configured )
			throw new IllegalStateException("Finalize first") ;
		
		if ( mPTHQueryCache == null || mPTHQueryCache.mPieceType != pieceType ) {
			int index = s.pieceTypeHistoryIndex(pieceType) ;
			if ( index < 0 )
				return 0 ;
			mPTHQueryCache = s.mPieceTypeHistory.get(index) ;
		}
		
		int num = 0 ;
		for ( int i = 0; i < mPTHQueryCache.mNumberPopped.length; i++ )
			num += mPTHQueryCache.mNumberPopped[i] ;
		
		return num ;
	}
	
	
	public int getNumberLanded( int pieceType ) {
		if ( !configured )
			throw new IllegalStateException("Finalize first") ;
		
		if ( mPTHQueryCache == null || mPTHQueryCache.mPieceType != pieceType ) {
			int index = s.pieceTypeHistoryIndex(pieceType) ;
			if ( index < 0 )
				return 0 ;
			mPTHQueryCache = s.mPieceTypeHistory.get(index) ;
		}
		
		int num = 0 ;
		for ( int i = 0; i < mPTHQueryCache.mNumberLanded.length; i++ )
			num += mPTHQueryCache.mNumberLanded[i] ;
		
		return num ;
	}
	
	
	public int getNumberPoppedWithQCombination( int qc ) {
		if ( !configured )
			throw new IllegalStateException("Finalize first") ;
		
		PieceTypeHistory pth ;
		int num = 0 ;
		for ( int i = 0; i < s.mPieceTypeHistory.size(); i++ ) {
			pth = s.mPieceTypeHistory.get(i) ;
			if ( PieceCatalog.getQCombination(pth.mPieceType) == qc ) {
				for ( int j = 0; j < pth.mNumberPopped.length; j++ )
					num += pth.mNumberPopped[j] ;
			}
		}
		
		return num ;
	}
	
	public int getNumberLandedWithQCombination( int qc ) {
		if ( !configured )
			throw new IllegalStateException("Finalize first") ;
		
		PieceTypeHistory pth ;
		int num = 0 ;
		for ( int i = 0; i < s.mPieceTypeHistory.size(); i++ ) {
			pth = s.mPieceTypeHistory.get(i) ;
			if ( PieceCatalog.getQCombination(pth.mPieceType) == qc ) {
				for ( int j = 0; j < pth.mNumberLanded.length; j++ )
					num += pth.mNumberLanded[j] ;
			}
		}
		
		return num ;
	}
	
	
	
	public boolean getDidLandLast( int pieceType ) {
		if ( !configured )
		throw new IllegalStateException("Finalize first") ;
	
		if ( mPTHQueryCache == null || mPTHQueryCache.mPieceType != pieceType ) {
			int index = s.pieceTypeHistoryIndex(pieceType) ;
			if ( index < 0 )
				return false ;
			mPTHQueryCache = s.mPieceTypeHistory.get(index) ;
		}
		
		return mPTHQueryCache.mLastPoppedCounter == mPTHQueryCache.mLastLandedCounter ;
	}
	
	
	public boolean getDidFailLast( int pieceType ) {
		if ( !configured )
		throw new IllegalStateException("Finalize first") ;
	
		if ( mPTHQueryCache == null || mPTHQueryCache.mPieceType != pieceType ) {
			int index = s.pieceTypeHistoryIndex(pieceType) ;
			if ( index < 0 )
				return false ;
			mPTHQueryCache = s.mPieceTypeHistory.get(index) ;
		}
		
		return mPTHQueryCache.mLastPoppedCounter == mPTHQueryCache.mLastFailureCounter ;
	}
	
	
	public boolean getDidSucceedLast( int pieceType ) {
		if ( !configured )
		throw new IllegalStateException("Finalize first") ;
	
		if ( mPTHQueryCache == null || mPTHQueryCache.mPieceType != pieceType ) {
			int index = s.pieceTypeHistoryIndex(pieceType) ;
			if ( index < 0 )
				return false ;
			mPTHQueryCache = s.mPieceTypeHistory.get(index) ;
		}
		
		return mPTHQueryCache.mLastPoppedCounter == mPTHQueryCache.mLastSuccessCounter ;
	}
	
	
	public boolean getDidLandLastWithQCombination( int qc ) {
		if ( !configured )
			throw new IllegalStateException("Finalize first") ;
	
		int mostRecentCounter = -1 ;
		boolean didLandMostRecent = false ;
		
		PieceTypeHistory pth ;
		for ( int i = 0; i < s.mPieceTypeHistory.size(); i++ ) {
			pth = s.mPieceTypeHistory.get(i) ;
			if ( PieceCatalog.getQCombination(pth.mPieceType) == qc ) {
				if ( pth.mLastPoppedCounter > mostRecentCounter ) {
					mostRecentCounter = pth.mLastPoppedCounter ;
					didLandMostRecent = pth.mLastPoppedCounter == pth.mLastLandedCounter ;
				}
			}
		}
		
		return didLandMostRecent ;
	}
	
	
	public boolean getDidFailLastWithQCombination( int qc ) {
		if ( !configured )
			throw new IllegalStateException("Finalize first") ;
	
		int mostRecentCounter = -1 ;
		boolean didFailMostRecent = false ;
		
		PieceTypeHistory pth ;
		for ( int i = 0; i < s.mPieceTypeHistory.size(); i++ ) {
			pth = s.mPieceTypeHistory.get(i) ;
			if ( PieceCatalog.getQCombination(pth.mPieceType) == qc ) {
				if ( pth.mLastPoppedCounter > mostRecentCounter ) {
					mostRecentCounter = pth.mLastPoppedCounter ;
					didFailMostRecent = pth.mLastPoppedCounter == pth.mLastFailureCounter ;
				}
			}
		}
		
		return didFailMostRecent ;
	}
	
	
	public boolean getDidSucceedLastWithQCombination( int qc ) {
		if ( !configured )
			throw new IllegalStateException("Finalize first") ;
	
		int mostRecentCounter = -1 ;
		boolean didSucceedMostRecent = false ;
		
		PieceTypeHistory pth ;
		for ( int i = 0; i < s.mPieceTypeHistory.size(); i++ ) {
			pth = s.mPieceTypeHistory.get(i) ;
			if ( PieceCatalog.getQCombination(pth.mPieceType) == qc ) {
				// System.err.println("** last popped " + pth.mLastPoppedCounter + " last succeed " + pth.mLastSuccessCounter) ;
				if ( pth.mLastPoppedCounter > mostRecentCounter ) {
					mostRecentCounter = pth.mLastPoppedCounter ;
					didSucceedMostRecent = pth.mLastPoppedCounter == pth.mLastSuccessCounter ;
				}
			}
		}
		
		return didSucceedMostRecent ;
	}
	
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// CONFIGURING
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	public void addBagContents ( int [] types ) {
		if ( configured )
			throw new IllegalStateException("Can't add bag contents once finalized") ;
		bagContents.add(types) ;
	}


	public void setConsolidateClears( boolean cc ) {
		if ( configured )
			throw new IllegalStateException("Can't set consolidate once finalized") ;
		mConsolidateClears = cc ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE STATE
	//
	////////////////////////////////////////////////////////////////////////////

	@Override
	public SerializableState finalizeConfiguration() throws IllegalStateException {
		if ( configured )
			throw new IllegalStateException("Can't finalize once finalized") ;
		
		s = new PieceHistoryState( bagContents ) ;
		bagContents = null ;
		configured = true ;
		
		return this ;
	}



	@Override
	public Serializable getStateAsSerializable() throws IllegalStateException {
		
		if ( !configured )
			throw new IllegalStateException("Can't get an unfinalized state") ;
		
		return s ;
	}

	
	
	/**
	 * Returns, as a Serializable object, the current "state" of
	 * this object, which is assumed to be in "stateful use" phase.
	 * 
	 * Calling 'setStateAsParcelable' on this object at any future
	 * point, or on another instance of the object which had an identical
	 * configuration phase, should produce an object with identical
	 * behavior and state to the one whose Serializable state was
	 * extracted - no matter what state the object was in before
	 * setState... was called.
	 * 
	 * @return Current state as a Serializable
	 * 
	 * @throws IllegalStateException If called before 'finalizeConfiguration'
	 */
	public Serializable getCloneStateAsSerializable() throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before retrieving state!") ;
		return new PieceHistoryState( s ) ;
	}


	@Override
	public SerializableState setStateAsSerializable(Serializable in)
			throws IllegalStateException {
		
		if ( !configured )
			throw new IllegalStateException("Can't set state until finalized") ;
		
		s = (PieceHistoryState)in ;
		mPTHQueryCache = null ;
		return this ;
	}



	@Override
	public void writeStateAsSerializedObject(ObjectOutputStream outStream)
			throws IllegalStateException, IOException {
		
		if ( !configured )
			throw new IllegalStateException("Can't write state until finalized") ;
		outStream.writeObject(s) ;
	}



	@Override
	public SerializableState readStateAsSerializedObject(
			ObjectInputStream inStream) throws IllegalStateException,
			IOException, ClassNotFoundException {
		
		if ( !configured )
			throw new IllegalStateException("Can't read state until finalized") ;
		s = (PieceHistoryState)inStream.readObject() ; 
		mPTHQueryCache = null ;
		return this ;
	}
	
	
}
