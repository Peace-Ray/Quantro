package com.peaceray.quantro.model.systems.special;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.model.state.VersionedState;
import com.peaceray.quantro.model.systems.attack.AttackSystem;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.simulatedarray.SimulatedArray;

/**
 * A special system designed to drop individual blocks.  The policy for "earning"
 * blocks is relatively straightforward; it is based on row clears.
 * 
 * Blocks may be sent as attacks (to be dropped in valleys or on junctions), introduced
 * as controllable pieces (yippee!), or dropped internally at the next opportunity.
 * These configurations are provided when constructed.  Additionally, a certain number
 * of blocks can be "built up" and stored; the maximum is another configuration option.
 * Finally, when used, either one block may be removed from this collection or all
 * used at once.
 * 
 * 'State' tracks the currently available special.
 * 
 * @author Jake
 *
 */
public class BlockDropperSpecialSystem extends SpecialSystem {
	
	
	private static final int VERSION = 0 ;
	
	public enum SpecialType {
		RESERVE, ATTACK
	}
	
	
	////////////////////////////////////////////////////////
	// CONFIGURATION
	
	protected int mPreviewQCombination ;
	protected int mMaxBlocks ;
	protected int mStartBlocks ;
	
	protected SpecialType mSpecialType ;
	
	protected int mSpecialReserveQCombination ;
	
	// How do we earn blocks?
	protected int mPointsToEarnBlock ;
	protected SimulatedArray mPointsPerClear ;
					// 1st clear gets mPointsPerClear[0], etc.
	protected boolean mReclaimBlocks ;
	protected int [] mReclaimableQCombinations ;
	
	
	////////////////////////////////////////////////////////
	// OTHER MEMBERS
	
	protected GameInformation ginfo ;
	protected QInteractions qi ;
	
	protected boolean mConfigured ;
	protected BlockDropperSpecialSystemState mState ;
	
	protected byte [] mTestQO ;
	
	public BlockDropperSpecialSystem( GameInformation ginfo, QInteractions qi ) {
		
		mPreviewQCombination = -1 ;
		mMaxBlocks = 0 ;
		mStartBlocks = 0 ;
		mSpecialType = SpecialType.RESERVE ;
		mSpecialReserveQCombination = -1 ;
		
		mPointsToEarnBlock = 0 ;
		mPointsPerClear = null ;
		
		mReclaimBlocks = false ;
		mReclaimableQCombinations = null ;
		
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		mConfigured = false ;
		mState = new BlockDropperSpecialSystemState(VERSION) ;
		
		mTestQO = new byte[2] ;
	}
	
	@Override
	public GameInformation getGameInformation() {
		return ginfo ;
	}

	@Override
	public QInteractions getQInteractions() {
		return qi ;
	}
	
	
	@Override
	public boolean canHaveReserve() {
		return mSpecialType == SpecialType.RESERVE ;
	}
	
	@Override
	public boolean canHaveAttack() {
		return mSpecialType == SpecialType.ATTACK ;
	}
	
	
	@Override
	public boolean hasSpecial() {
		return mState.mNumBlocks > 0 ;
	}

	@Override
	public int getSpecialPreviewPieceType() {
		if ( mState.mNumBlocks == 0 )
			return -1 ;
		return PieceCatalog.encodeSpecial(
				PieceCatalog.SPECIAL_CAT_GALAXY,
				mState.mNumBlocks, mPreviewQCombination) ;
	}

	@Override
	public boolean specialIsReserveInsert() {
		return hasSpecial() && mSpecialType == SpecialType.RESERVE ;
	}

	@Override
	public boolean specialIsAttack() {
		return hasSpecial() && mSpecialType == SpecialType.ATTACK ;
	}
	
	@Override
	public boolean initialize() {
		if ( mStartBlocks > 0 ) {
			mState.mNumBlocks = mStartBlocks ;
			return true ;
		}
		return false ;
	}
	
	@Override
	public boolean aboutToLock( Piece p, Offset o, byte [][][] blockField ) {
		mState.mClearsThisCycle = 0 ;
		mState.mPieceHasLocked = true ;
		return false ;
	}
	

	@Override
	public boolean aboutToClear( Piece p, Offset o,
			byte [][][] blockFieldBefore, 
			byte [][][] blockFieldAfter,
			byte [][][] blockFieldClearedInverse,
			int [] chromaticClears, boolean [] monochromaticClears ) {
		
		int blocksEarned = 0 ;
		
		// We increase the number of blocks using two methods.  First,
		// we attempt to reclaim blocks.
		if ( mReclaimBlocks ) {
			for ( int i = 0; i < mReclaimableQCombinations.length; i++ ) {
				QCombinations.setAs(mTestQO, mReclaimableQCombinations[i]) ;
				// look for this combination among the cleared inverse.
				for ( int row = 0; row < chromaticClears.length; row++ ) {
					if ( chromaticClears[row] != 0 || monochromaticClears[row] ) {
						for ( int c = 0; c < blockFieldClearedInverse[0][0].length; c++ ) {
							// check both qpanes at this row / c against mTestQO.
							boolean match = true ;
							for ( int qp = 0; qp < 2; qp++ )
								match = match && ( mTestQO[qp] == 0 || mTestQO[qp] == blockFieldClearedInverse[qp][row][c] ) ;
							if ( match )
								blocksEarned++ ;
						}
					}
				}
			}
		}
		
		// Second, we count up points for clears.  We only count
		// those clears that occur after the piece has locked.
		if ( mState.mPieceHasLocked ) {
			for ( int row = 0; row < chromaticClears.length; row++ ) {
				// how many clears here?
				int clears = 0 ;
				if ( monochromaticClears[row] )
					clears = GameModes.numberQPanes(ginfo) == 1 ? 1 : 2 ;
				else if ( chromaticClears[row] == QCombinations.SL
						|| chromaticClears[row] == QCombinations.SS )
					clears = GameModes.numberQPanes(ginfo) == 1 ? 1 : 2 ;
				else if ( chromaticClears[row] != 0 )
					clears = 1 ;
				
				for ( int i = 0; i < clears; i++ ) {
					// add the points!
					mState.mNumPoints += mPointsPerClear.getInt(mState.mClearsThisCycle) ;
					mState.mClearsThisCycle++ ;
				}
			}
			
			// convert points to blocks.
			blocksEarned += mState.mNumPoints / mPointsToEarnBlock ;
			mState.mNumPoints %= mPointsToEarnBlock ;
		}
		
		// Finally, add the blocks to our total.
		blocksEarned = Math.min(blocksEarned, mMaxBlocks - mState.mNumBlocks) ;
		mState.mNumBlocks += blocksEarned ;
		
		return blocksEarned > 0 ;
	}

	@Override
	public boolean endCycle() {
		// ending a cycle has no effect on special availability; we change the special
		// as clears occur, not at the end.
		mState.mPieceHasLocked = false ;
		return false ;
	}

	@Override
	public int useSpecialAsReserveInsert() {
		if ( mSpecialType != SpecialType.RESERVE )
			throw new IllegalStateException("Not configured for reserve insert") ;
		
		if ( mState.mNumBlocks <= 0 )
			throw new IllegalStateException("No reserve insert available") ;
		
		mState.mNumBlocks-- ;
		
		int type = -1 ;
		int qc = mSpecialReserveQCombination ;
		if ( qc == QCombinations.F0 || qc == QCombinations.F1 ) {
			type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_FLASH, qc) ;
		} else if ( qc == QCombinations.PUSH_DOWN ) {
			type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_PUSH_DOWN, qc) ;
		} else {
			type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_BLOCK, qc) ;
		}
		
		return type ;
	}

	
	@Override
	public void useSpecialAsAttack(AttackSystem as) {
		if ( mSpecialType != SpecialType.ATTACK )
			throw new IllegalStateException("Not configured for attacks") ;
		
		if ( mState.mNumBlocks <= 0 )
			throw new IllegalStateException("No attack available") ;
		
		// Currently, attacks send all blocks at once to junctions.  Add configuration
		// if this is not desired behavior.
		as.directApi_addAttack_dropBlocks(0, 0, 0, 0, mState.mNumBlocks) ;
		
		mState.mNumBlocks = 0 ;
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// OUR STATE
	
	private class BlockDropperSpecialSystemState extends VersionedState {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 47704461490858313L;
		
		int mNumPoints ;
		int mNumBlocks ;
		
		int mClearsThisCycle ;
		
		boolean mPieceHasLocked ;
		
		public BlockDropperSpecialSystemState(int version) {
			super(version);
			mNumPoints = 0 ;
			mNumBlocks = 0 ;
			
			mClearsThisCycle = 0 ;
			
			mPieceHasLocked = false ;
		}
		
		public BlockDropperSpecialSystemState(BlockDropperSpecialSystemState state) {
			super(state);
			mNumPoints = state.mNumPoints ;
			mNumBlocks = state.mNumBlocks ;
			
			mClearsThisCycle = state.mClearsThisCycle ;
			
			mPieceHasLocked = state.mPieceHasLocked ;
		}

		/////////////////////////////////////////////
		// serializable methods
		private void writeObject(java.io.ObjectOutputStream stream) throws IOException { 
			stream.writeInt(version) ;
			
			stream.writeInt(mNumPoints) ;
			stream.writeInt(mNumBlocks) ;
			
			stream.writeInt(mClearsThisCycle) ;
			
			stream.writeBoolean(mPieceHasLocked) ;
		}
		
		private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
			version = stream.readInt() ;
			
			mNumPoints = stream.readInt() ;
			mNumBlocks = stream.readInt() ;
			
			mClearsThisCycle = stream.readInt() ;
			
			mPieceHasLocked = stream.readBoolean() ;
		}
		
		@SuppressWarnings("unused")
		private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
			throw new ClassNotFoundException("Stream does not match required system state structure.") ;
		}
		
	}

	
	////////////////////////////////////////////////////////////////////////////
	// CONFIGURATION
	
	public void configureSpecialType( SpecialType type ) {
		if ( mConfigured )
			throw new IllegalStateException("Cannot set special Type after finalizing configuration!") ;
		
		mSpecialType = type ;
	}
	
	
	public void configurePreviewQCombination( int qc ) {
		if ( mConfigured )
			throw new IllegalStateException("Cannot set special preview qc after finalizing configuration!") ;
		
		mPreviewQCombination = qc ;
	}
	
	
	public void configureMaxBlocks( int max ) {
		if ( mConfigured )
			throw new IllegalStateException("Cannot set max blocks after finalizing configuration!") ;
		
		mMaxBlocks = max ;
	}
	
	
	public void configureStartBlocks( int blocks ) {
		if ( mConfigured )
			throw new IllegalStateException("Cannot set start blocks after finalizing configuration!") ;
		
		mStartBlocks = blocks ;
	}
	
	public void configureReserveQCombination( int qc ) {
		if ( mConfigured )
			throw new IllegalStateException("Cannot set reserve qcombination after finalizing configuration!") ;
		
		mSpecialReserveQCombination = qc ;
	}
	
	
	public void configureEarnBlocksFromClears( int pointsNeeded, SimulatedArray pointsPerClear ) {
		if ( mConfigured )
			throw new IllegalStateException("Cannot set block earning policy after finalizing configuration!") ;
		
		mPointsToEarnBlock = pointsNeeded ;
		mPointsPerClear = pointsPerClear ;
	}
	
	
	public void configureReclaimBlocks( int [] qCombinations ) {
		if ( mConfigured )
			throw new IllegalStateException("Cannot set block reclaimation policy after finalizing configuration!") ;
		
		if ( qCombinations == null || qCombinations.length == 0 ) {
			mReclaimBlocks = false ;
			mReclaimableQCombinations = null ;
		}
		else {
			mReclaimBlocks = true ;
			mReclaimableQCombinations = ArrayOps.duplicate(qCombinations) ;
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// SERIALIZABLE STATE
	
	
	@Override
	public SerializableState finalizeConfiguration()
			throws IllegalStateException {
		if ( mConfigured )
			throw new IllegalStateException("finalizeConfiguration() should only be called once!") ;
		if ( mSpecialType == SpecialType.ATTACK ) {
			// check values set
			if ( this.mPreviewQCombination == -1 )
				throw new IllegalStateException("Preview QCombination is not set!") ;
		} else if ( mSpecialType == SpecialType.RESERVE ) {
			if ( this.mPreviewQCombination == -1 )
				throw new IllegalStateException("Preview QCombination is not set!") ;
			if ( this.mSpecialReserveQCombination == -1 )
				throw new IllegalStateException("Reserve QCombination is not set!") ;
		}
		mConfigured = true ;
		return this ;
	}

	@Override
	public Serializable getStateAsSerializable() throws IllegalStateException {
		if ( !mConfigured )
			throw new IllegalStateException("Must finalizeConfiguration() before retrieving state!") ;
		return mState ;
	}

	@Override
	public Serializable getCloneStateAsSerializable()
			throws IllegalStateException {
		if ( !mConfigured )
			throw new IllegalStateException("Must finalizeConfiguration() before retrieving state!") ;
		return new BlockDropperSpecialSystemState( mState ) ;
	}

	@Override
	public SerializableState setStateAsSerializable(Serializable in)
			throws IllegalStateException {
		if ( !mConfigured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		mState = (BlockDropperSpecialSystemState)in ;
		return this ;
	}

	@Override
	public void writeStateAsSerializedObject(ObjectOutputStream outStream)
			throws IllegalStateException, IOException {
		if ( !mConfigured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		outStream.writeObject(mState) ;
	}

	@Override
	public BlockDropperSpecialSystem readStateAsSerializedObject(
			ObjectInputStream inStream) throws IllegalStateException,
			IOException, ClassNotFoundException {
		if ( !mConfigured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		mState = (BlockDropperSpecialSystemState)inStream.readObject() ;
		return this ;
	}

}
