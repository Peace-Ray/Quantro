package com.peaceray.quantro.model.game;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import com.peaceray.quantro.adapter.action.ActionAdapter;
import com.peaceray.quantro.exceptions.GameSystemException;
import com.peaceray.quantro.exceptions.InvalidPieceException;
import com.peaceray.quantro.exceptions.QOrientationConflictException;
import com.peaceray.quantro.model.descriptors.versioned.ActionCycleStateDescriptor;
import com.peaceray.quantro.model.descriptors.versioned.AttackDescriptor;
import com.peaceray.quantro.model.modes.GameModes ;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.model.pieces.bags.PieceBag;
import com.peaceray.quantro.model.pieces.history.PieceHistory;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.model.systems.attack.AttackSystem;
import com.peaceray.quantro.model.systems.clear.ClearSystem;
import com.peaceray.quantro.model.systems.collision.CollisionSystem;
import com.peaceray.quantro.model.systems.displacement.DisplacementSystem;
import com.peaceray.quantro.model.systems.kick.KickSystem;
import com.peaceray.quantro.model.systems.level.LevelSystem;
import com.peaceray.quantro.model.systems.lock.LockSystem;
import com.peaceray.quantro.model.systems.metamorphosis.MetamorphosisSystem;
import com.peaceray.quantro.model.systems.rotation.RotationSystem;
import com.peaceray.quantro.model.systems.score.ScoreSystem;
import com.peaceray.quantro.model.systems.special.SpecialSystem;
import com.peaceray.quantro.model.systems.timing.TimingSystem;
import com.peaceray.quantro.model.systems.trigger.TriggerSystem;
import com.peaceray.quantro.model.systems.trigger.Triggerable;
import com.peaceray.quantro.model.systems.valley.ValleySystem;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.LooselyBoundedArray.LooselyBoundedArray;

public class Game implements SerializableState, Triggerable {
	
	//private static final String TAG = "Game" ;
	
	// Some important information
	private static int enm = -1 ;
	public static final int STATE_INITIALIZING = enm++ ;		// -1
	public static final int STATE_SYNCHRONIZING = enm++ ;		// 0
	public static final int STATE_PREPARING = enm++ ;
	public static final int STATE_ENTERING = enm++ ;
	public static final int STATE_FALLING = enm++ ;
	public static final int STATE_PROGRESSION = enm++ ;			// 4
	public static final int STATE_ENDING_CYCLE = enm++ ;		// 5
	
	public static final int PERIOD_PRESTART = enm++ ;			// 6
	public static final int PERIOD_ONGOING = enm++ ;
	public static final int PERIOD_GAME_LOST = enm++ ;			// 7
	
	
	public static final int PROGRESSION_COMPONENTS_UNLOCK = enm++ ;	// 8
	public static final int PROGRESSION_COMPONENTS_FALL = enm++ ;
	public static final int PROGRESSION_COMPONENTS_LOCK = enm++ ;	// 9
	public static final int PROGRESSION_CLEAR = enm++ ;				// 10
	public static final int PROGRESSION_CHUNKS_UNLOCK = enm++ ;		// 11
	public static final int PROGRESSION_CHUNKS_FALL = enm++ ;		// 12
	public static final int PROGRESSION_CHUNKS_LOCK = enm++ ;		// 13
	
	public static final int PROGRESSION_TRIGGERED_METAMORPHOSIS = enm++ ; 	// 14
	
	//public static final int PROGRESSION_DROPPED_COMPONENTS_LAST = enm++ ;
	//public static final int PROGRESSION_LOCKED_LAST = enm++ ;
	//public static final int PROGRESSION_CLEARED_LAST = enm++ ;
	//public static final int PROGRESSION_UNLOCKED_LAST = enm++ ;
	
	
	// Here are some triggers for the "triggerable" interface.
	public static final int TRIGGER_SUCCESS_UNLOCK_BEST_COLUMN = enm++ ;
	public static final int TRIGGER_SUCCESS_DROP_BLOCKS_IN_VALLEYS = enm++ ;
	public static final int TRIGGER_SUCCESS_UNLOCK_BEST_COLUMN_OR_DROP_BLOCKS_IN_VALLEYS = enm++ ;
	public static final int TRIGGER_SUCCESS_IF_UNLOCK_COLUMN_ABOVE_F_PIECE_BLOCKS = enm++ ;
	public static final int TRIGGER_CANCEL_PIECE_LOCK = enm++ ;
	public static final int TRIGGER_SUCCESS_LOCK_AND_METAMORPHOSIS_ACTIVATE = enm++ ;
	public static final int TRIGGER_FAILURE_LOCK_AND_METAMORPHOSIS_DEACTIVATE = enm++ ;
	public static final int TRIGGER_METAMORPHOSIS_ACTIVATE_BEFORE_END_CYCLE = enm++ ;
	public static final int TRIGGER_SUCCESS_NO_OTHER_EFFECT = enm++ ;
	public static final int TRIGGER_FAILURE_NO_OTHER_EFFECT = enm++ ;
			// LOCK_AND_METAMORPHOSIS_* force a multi-step update: we lock the piece in
			// place (separate components, fall, lock) and then perform the appropriate
			// metamorphosis.  At this point we either proceed (if no sig. events occurred
			// in that list of crap) to a normal progression, or stop and let things be
			// drawn.
	
	
	public static final int LEAN_NONE = 0 ;
	public static final int LEAN_LEFT = 1 ;
	public static final int LEAN_RIGHT = 2 ;
	public static final int LEAN_DOWN = 3 ;
	public static final int LEAN_DOWN_LEFT = 4 ;
	public static final int LEAN_DOWN_RIGHT = 5 ;
	
	
	// Usage:
	// No Parameters (parameters will be ignored)
	// 		TRIGGER_UNLOCK_BEST_COLUMN
	// 		TRIGGER_UNLOCK_COLUMN_ABOVE_F_PIECE_BLOCKS
	// 		TRIGGER_CANCEL_PIECE_LOCK
	//		TRIGGER_METAMORPHOSIS_ACTIVATE
	//
	// TRIGGER_DROP_BLOCKS_IN_VALLEYS
	//		Should have 1 parameter: maximum number of blocks to drop.
	// TRIGGER_UNLOCK_BEST_COLUMN_OR_DROP_BLOCKS_IN_VALLEYS
	//		Should have 1 parameter: maximum number of blocks to drop.  If there is
	//		no "best column," we drop up to that many blocks.
	
	
	
	// Behavior-influencing game components: systems,
	// gameInformation, pieceBags, etc.
	// Info
	public GameInformation ginfo ;
	public QInteractions qi ;
	public GameEvents gevents ;
	
	// Adapter which dictates behavior and actions
	ActionAdapter adapter ;
	
	// Systems
	AttackSystem as ;
	ClearSystem cls ;
	CollisionSystem cs ;
	DisplacementSystem ds ;
	KickSystem ks ;
	LevelSystem lvs ;
	LockSystem ls ;
	MetamorphosisSystem ms ;
	RotationSystem rs ;
	ScoreSystem ss ;
	SpecialSystem sps ;
	TimingSystem ts ;
	ValleySystem vs ;
	// Trigger system
	TriggerSystem trs ;
	// Piece Bags
	PieceBag pieceBag ;
	PieceBag reserveBag ;
	// Piece History
	PieceHistory pieceHistory ;
	
	// Activate neutron purge.
	public GameState s ;
	protected boolean configured ;
	
	// Temporary storage that isn't used between method calls, only within
	// methods.  These replace variables that should be local vars, but
	// are treated as instance vars to prevent frequent allocation/deallocation.
	private Offset tempOffset ;
	private Piece tempPiece ;
	
	private Offset tempCenter ;
	private Offset tempCenterReserve ;
	private Offset tempOffsetPositionForEntry ;
	
	private byte [][][] tempField ;
	private boolean [][][] tempFieldBoolean ;
	private boolean [] tempBooleans ;
	private int [] tempInts ;
	
	// Here's some static, method-specific temporary storage, to a method
	// that calls others from stepping on its own toes.
	private byte [][][] bctuField ;		// bestColumnToUnlock field
	private byte [][][] bctuRevertToField ;	// bestColumnToUnlock
	private ArrayList<Piece> bctuPieces ;
	private ArrayList<Offset> bctuOffsets ;
	private ArrayList<Offset> bctuOffsetsAfterFall ;
	private Offset delta ;		
		// Passed from public calls to private methods,
		// but not retained between public method calls.
	private Offset lean ;
	private byte [][][] emptyField ;
		// Won't ever change.
	
	// Useful for semi-random testing
	private Random testingRandom = new Random() ;
	
	// Constructing a game object:
	// Do one of the following paths:
	// g = new Game(R, C)
	// g.setGameInformation(gi) ;
	// g.setGameEvents( gameEvents ) ;
	// g.setSystems(null) ;
	// g.makeReady() ;
	// g.finalizeConfiguration() ;
	
	// g = bundle.getParcelable("game")
	// g.setGameInformation(gi) ;
	// g.setGameEvents( gameEvents ) ;
	// g.setSystems( Parcelable [] systems ) ;
	// g.makeReady() ;
	// g.finalizeConfiguration() ;
	
	public Game( int R, int C ) {
		s = new GameState(R, C) ;
		configured = false ;
		
		// Ensure blockFields are set to NO
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = 0; r < R; r++ ) {
				for ( int c = 0; c < C; c++ ) {
					s.blockFieldInverseClear[q][r][c] = s.blockFieldBefore[q][r][c] = s.blockField[q][r][c] = QOrientations.NO ;
				}
			}
		}
		
		preallocateStorage() ;
	}
	
	protected Game() { }
	
	public Game setGameInformation( GameInformation ginfo ) {
		this.ginfo = ginfo ;
		this.qi = GameModes.newQInteractions(ginfo) ;
		return this ;
	}
	
	public Game setGameEvents( GameEvents gevents ) {	
		this.gevents = gevents ;
		return this ;
	}
	
	public Game setActionAdapter( ActionAdapter adapter ) {
		this.adapter = adapter ;
		return this ;
	}

	public static int numSystems() {
		return 17 ;
		// pieceBag, reserveBag, pieceHistory,
		//	as,	cls,cs,	ds,	ks,
		//	lvs,ls,	ms,	rs,	ss,
		//	sps,ts,vs,	trs
	}
	
	private static boolean hasSpecialSystemState( Serializable [] ar ) {
		return ar.length >= 16 ;		// Special was the 16th System added.
	}
	
	private static boolean hasDisplacementSystemState( Serializable [] ar ) {
		return ar.length >= 17 ;		// Displacement was 17th System added.
	}
	
	public Game setSystemsFromSerializables( Serializable [] ar ) {
		if ( ar == null ) {
			// Piece bags
			pieceBag = GameModes.newPieceBag(ginfo) ;
			reserveBag = GameModes.newReserveBag(ginfo) ;
			pieceHistory = GameModes.newPieceHistory(ginfo, pieceBag, reserveBag) ;
			
			// Systems!
			as = GameModes.newAttackSystem(ginfo, qi) ;
			cls = GameModes.newClearSystem(ginfo, qi) ;
			cs = GameModes.newCollisionSystem(ginfo, qi) ;
			ds = GameModes.newDisplacementSystem(ginfo, qi) ;
			ks = GameModes.newKickSystem(ginfo, qi) ;
			lvs = GameModes.newLevelSystem(ginfo, qi) ;
			ls = GameModes.newLockSystem(ginfo, qi) ;
			ms = GameModes.newMetamorphosisSystem(ginfo, qi) ;
			rs = GameModes.newRotationSystem(ginfo, qi, pieceBag, reserveBag) ;
			ss = GameModes.newScoreSystem(ginfo, qi) ;
			sps = GameModes.newSpecialSystem(ginfo, qi) ;
			ts = GameModes.newTimingSystem(ginfo, qi) ;
			vs = GameModes.newValleySystem(ginfo, qi) ;
			
			// Trigger system...
			trs = GameModes.newTriggerSystem(this, ginfo, qi, cls, cs, ls, ss) ;
		}
		else {
			int enm = 0 ;
			
			// Piece bags
			pieceBag = (PieceBag)GameModes.newPieceBag(ginfo).setStateAsSerializable(ar[enm++]) ;
			reserveBag = (PieceBag)GameModes.newReserveBag(ginfo).setStateAsSerializable(ar[enm++]) ;
			pieceHistory = (PieceHistory)GameModes.newPieceHistory(ginfo, pieceBag, reserveBag).setStateAsSerializable(ar[enm++]) ;
			
			// Systems!
			as = (AttackSystem)GameModes.newAttackSystem(ginfo, qi).setStateAsSerializable(ar[enm++]) ;
			cls = (ClearSystem)GameModes.newClearSystem(ginfo, qi).setStateAsSerializable(ar[enm++]) ;
			cs = (CollisionSystem)GameModes.newCollisionSystem(ginfo, qi).setStateAsSerializable(ar[enm++]) ;
			ds = (DisplacementSystem)GameModes.newDisplacementSystem(ginfo, qi) ;
			if ( hasDisplacementSystemState(ar) )
				ds.setStateAsSerializable(ar[enm++]) ;
			
			ks = (KickSystem)GameModes.newKickSystem(ginfo, qi).setStateAsSerializable(ar[enm++]) ;
			lvs = (LevelSystem)GameModes.newLevelSystem(ginfo, qi).setStateAsSerializable(ar[enm++]) ;
			ls = (LockSystem)GameModes.newLockSystem(ginfo, qi).setStateAsSerializable(ar[enm++]) ;
			ms = (MetamorphosisSystem)GameModes.newMetamorphosisSystem(ginfo, qi).setStateAsSerializable(ar[enm++]) ;
			rs = (RotationSystem)GameModes.newRotationSystem(ginfo, qi, pieceBag, reserveBag).setStateAsSerializable(ar[enm++]) ;
			ss = (ScoreSystem)GameModes.newScoreSystem(ginfo, qi).setStateAsSerializable(ar[enm++]) ;
			sps = (SpecialSystem)GameModes.newSpecialSystem(ginfo, qi) ;
			if ( hasSpecialSystemState( ar ) )
				sps.setStateAsSerializable(ar[enm++]) ;
				
			ts = (TimingSystem)GameModes.newTimingSystem(ginfo, qi).setStateAsSerializable(ar[enm++]) ;
			vs = (ValleySystem)GameModes.newValleySystem(ginfo, qi).setStateAsSerializable(ar[enm++]) ;
			
			// Trigger system... make new, then set state.
			trs = (TriggerSystem)GameModes.newTriggerSystem(this, ginfo, qi, cls, cs, ls, ss).setStateAsSerializable(ar[enm++]) ;
		}
		
		return this ;
	}
	
	
	public synchronized void setPseudorandom( int pseudorandom ) {
		as.setPseudorandom(pseudorandom) ;
		ds.setPseudorandom(pseudorandom) ;
		vs.setPseudorandom(pseudorandom) ;
	}
	
	/**
	 * Puts all the current 
	 * @return
	 */
	public synchronized Serializable [] getSerializablesFromSystems() {
		Serializable [] ar = new Serializable[Game.numSystems()] ;
		
		int enm = 0 ;
		
		// Piece bags
		ar[enm++] = pieceBag.getStateAsSerializable() ;
		ar[enm++] = reserveBag.getStateAsSerializable() ;
		ar[enm++] = pieceHistory.getStateAsSerializable() ;
		
		// Systems!
		ar[enm++] = as.getStateAsSerializable() ;
		ar[enm++] = cls.getStateAsSerializable() ;
		ar[enm++] = cs.getStateAsSerializable() ;
		ar[enm++] = ds.getStateAsSerializable() ;
		ar[enm++] = ks.getStateAsSerializable() ;
		ar[enm++] = lvs.getStateAsSerializable() ;
		ar[enm++] = ls.getStateAsSerializable() ;
		ar[enm++] = ms.getStateAsSerializable() ;
		ar[enm++] = rs.getStateAsSerializable() ;
		ar[enm++] = ss.getStateAsSerializable() ;
		ar[enm++] = sps.getStateAsSerializable() ;
		ar[enm++] = ts.getStateAsSerializable() ;
		ar[enm++] = vs.getStateAsSerializable() ;
		
		ar[enm++] = trs.getStateAsSerializable() ;
		
		return ar ;
	}
	
	/**
	 * Puts all the current 
	 * @return
	 */
	public synchronized Serializable [] getClonedSerializablesFromSystems() {
		Serializable [] ar = new Serializable[Game.numSystems()] ;
		
		int enm = 0 ;
		
		// Piece bags
		ar[enm++] = pieceBag.getCloneStateAsSerializable() ;
		ar[enm++] = reserveBag.getCloneStateAsSerializable() ;
		ar[enm++] = pieceHistory.getCloneStateAsSerializable() ;
		
		// Systems!
		ar[enm++] = as.getCloneStateAsSerializable() ;
		ar[enm++] = cls.getCloneStateAsSerializable() ;
		ar[enm++] = cs.getCloneStateAsSerializable() ;
		ar[enm++] = ds.getCloneStateAsSerializable() ;
		ar[enm++] = ks.getCloneStateAsSerializable() ;
		ar[enm++] = lvs.getCloneStateAsSerializable() ;
		ar[enm++] = ls.getCloneStateAsSerializable() ;
		ar[enm++] = ms.getCloneStateAsSerializable() ;
		ar[enm++] = rs.getCloneStateAsSerializable() ;
		ar[enm++] = ss.getCloneStateAsSerializable() ;
		ar[enm++] = sps.getCloneStateAsSerializable() ;
		ar[enm++] = ts.getCloneStateAsSerializable() ;
		ar[enm++] = vs.getCloneStateAsSerializable() ;
		
		ar[enm++] = trs.getCloneStateAsSerializable() ;
		
		return ar ;
	}
	
	
	public synchronized int getPieceType() {
		return s.piece.type ;
	}
	
	public PieceHistory getPieceHistory() {
		return pieceHistory ;
	}
	
	public synchronized boolean getDisplaces() {
		return ds.displaces() ;
	}
	
	public synchronized double getDisplacedRows() {
		return ds.getDisplacedRows() ;
	}
	
	public synchronized double getDisplacedAndTransferredRows() {
		return ds.getDisplacedAndTransferredRows() ;
	}
	
	public synchronized double getDisplacementSeconds() {
		return ds.getDisplacementSeconds() ;
	}
	
	public synchronized void setRealtimeMillisAndDisplacementSecondsAndDisplacedAndTransferredRows( long millis, double seconds, double disp ) {
		long mill = ginfo.milliseconds ;
		double sec = ds.getDisplacementSeconds() ;
		double prev = ds.getDisplacedAndTransferredRows() ;
		
		if ( mill != millis || sec != seconds || prev != disp ) {
			ginfo.milliseconds = millis ;
			ds.setDisplacementSecondsAndDisplacedAndTransferredRows(seconds, disp) ;
			adapter.game_didChangeRealtime(this, ginfo.milliseconds, seconds, disp) ;
		}
	}
	
	public synchronized double getTotalSecondsTicked() {
		return ginfo.milliseconds / 1000.0 ;
	}
	
	public synchronized long getTotalMillisecondsTicked() {
		return ginfo.milliseconds ;
	}
	
	public synchronized void setTotalSecondsTicked( double seconds ) {
		ginfo.milliseconds = Math.round( seconds * 1000 ) ;
 	} 
	
	public synchronized void setTotalMillisecondsTicked( long millis ) {
		ginfo.milliseconds = millis ;
	}
		
	/**
	 * Returns the maximum width, in blocks, of a piece at rotation 0 that
	 * was drawn from the PieceBag.
	 * @return
	 */
	public synchronized boolean hasTrominoes() {
		PieceBag pb = pieceBag ;
		return pb == null ? false : pb.hasTrominoes() ;
	}
	
	/**
	 * Returns the maximum width, in blocks, of a piece at rotation 0 that
	 * was drawn from the PieceBag.
	 * @return
	 */
	public boolean hasTetrominoes() {
		PieceBag pb = pieceBag ;
		return pb == null ? false : pb.hasTetrominoes() ;
	}
	
	/**
	 * Returns the maximum width, in blocks, of a piece at rotation 0 that
	 * was drawn from the PieceBag.
	 * @return
	 */
	public boolean hasPentominoes() {
		PieceBag pb = pieceBag ;
		return pb == null ? false : pb.hasPentominoes() ;
	}
	
	
	public boolean hasSpecialReserve() {
		return GameModes.reserveBehavior(ginfo) == GameModes.RESERVE_BEHAVIOR_SPECIAL ;
	}
	
	public boolean hasSpecialReserveAttack() {
		return sps.canHaveAttack() ;
	}
	
	/**
	 * Performs all remaining setup to make the game ready for operation.
	 * Anything not already set is initialized and/or allocated.  The game
	 * is ready to begin.
	 * @return
	 */
	public Game makeReady() throws InvalidPieceException {
		if ( configured )
			throw new IllegalStateException("Cannot change configuration after finalizeConfiguration()!") ;
		
		// We might have copied important information from a previously
		// running game.  Note that "preallocateAll" has already been called,
		// as has "setSystemsFromParcelables"
		
		
		// Reserve piece.  Should be initialized if we are using
		// insert; otherwise, it is empty.
		int rb = GameModes.reserveBehavior( ginfo ) ;
		if ( rb == GameModes.RESERVE_BEHAVIOR_SWAP || rb == GameModes.RESERVE_BEHAVIOR_SWAP_REENTER ) {
			s.reservePieces[0].type = s.reservePieces[0].defaultType = -1 ;
		}
		else if ( rb == GameModes.RESERVE_BEHAVIOR_INSERT || rb == GameModes.RESERVE_BEHAVIOR_INSERT_REENTER ) {
			for ( int i = 0; i < GameState.PIECE_LOOKAHEAD; i++ ) {
				reserveBag.pop( s.reservePieces[i] );
				rs.turn0( s.reservePieces[i] ) ;
			}
		}
		else if ( rb == GameModes.RESERVE_BEHAVIOR_SPECIAL ) {
			s.reservePieces[0].type = -1 ;
		}
		
		// Blocks for the piece?
		if ( s.piece.type > -1 )
			rs.turn0(s.piece) ;
		
		// Queue up the "next" piece, if necessary.
		for ( int i = 0; i < GameState.PIECE_LOOKAHEAD; i++ ) {
			pieceBag.pop( s.nextPieces[i] );
			rs.turn0( s.nextPieces[i] ) ;
		}
		
		
		// ArrayLists for components and chunks.  Allocate space for
		// them, especially the blocks arrays for chunks.
		for ( int i = 0; i < 4; i++ ) {
			Piece p = new Piece() ;
			p.blocks = new byte[2][4][4] ;
			p.setBounds() ;
			s.components.add( p ) ;
			s.componentOriginalOffsets.add( new Offset() ) ;
			s.componentFellOffsets.add( new Offset() ) ;
		}
		for ( int i = 0; i < 32; i++ ) {
			Piece p = new Piece() ;
			p.blocks = new byte[2][s.R][s.C] ;
			p.setBounds() ;
			s.chunks.add( p ) ;
			s.chunkOriginalOffsets.add( new Offset() ) ;
			s.chunkFellOffsets.add( new Offset() ) ;
		}
		
		gevents.setHappened(GameEvents.EVENT_NEXT_PIECE_CHANGED) ;
		gevents.setHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED) ;
		s.geventsLastTick.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
		s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
		
		return this ;
	}
	
	
	/**
	 * Refreshes any game state objects that may have incomplete information;
	 * for example, pieces get their blocks reset by the rotation system.
	 */
	public synchronized void refresh() {
		if ( s.piece.type > -1 )
			rs.turn0( s.piece ) ;
		for ( int i = 0; i < GameState.PIECE_LOOKAHEAD; i++ ) {
			if ( s.nextPieces[i].type > -1 )
				rs.turn0( s.nextPieces[i] ) ;
			if ( s.reservePieces[i].type > -1 )
				rs.turn0( s.reservePieces[i] ) ;
		}
	}
	
	/**
	 * preallocateAll Performs all necessary preallocation to
	 * get space for things.  Basically, we don't want to allocate
	 * structures as we go - better to premake them all upon construction.
	 */
	private void preallocateStorage() {
		if ( configured )
			throw new IllegalStateException("Cannot change configuration after finalizeConfiguration()!") ;
		
		// The blockfields used for the game
		emptyField = new byte[2][s.R][s.C] ;
		for ( int q = 0; q < 2; q++ ) 
			for ( int r = 0; r < s.R; r++ ) 
				for ( int c = 0; c < s.C; c++ ) 
					emptyField[q][r][c] = QOrientations.NO ;
		
		// delta - used for generic movement functions.
		delta = new Offset() ;
		// lead - used for a prefered kick direction.
		lean = new Offset() ;
		
		// tempOffset - used for in-method operations.
		tempOffset = new Offset() ;
		
		// tempPiece - used for in-method operations
		tempPiece = new Piece() ;
		
		// tempCenter - used to align piece reserve center of gravity.
		tempCenter = new Offset() ;
		tempCenterReserve = new Offset() ;
		tempOffsetPositionForEntry = new Offset() ;
		
		// tempField - used to simulate events that would mess
		// up the blockField.
		tempField = new byte[2][s.R][s.C] ;
		tempFieldBoolean = new boolean[2][s.R][s.C] ; 
		
		// tempBooleans - a helpful place to store some boolean values.
		tempBooleans = new boolean[2*s.R*s.C] ;
		
		// tempInts - another helpful array!
		tempInts = new int[2*s.R*s.C] ;
		
		// method-specific temporary structures
		bctuField = new byte[2][s.R][s.C] ;
		bctuRevertToField = new byte[2][s.R][s.C] ;
		bctuPieces = new ArrayList<Piece>() ;
		bctuOffsets = new ArrayList<Offset>() ;
		bctuOffsetsAfterFall = new ArrayList<Offset>() ;
	}
	
	/*
	 ***************************************************************************
	 * GAME INTERFACE FOR USER
	 *
	 * Pressing buttons, gesture controls, etc. will cause these
	 * functions to be called.  In other words, this is the "public interface"
	 * for the game.  A layer sitting between the player and the game
	 * will interpret touches and button presses as triggering these
	 * effects.
	 * 
	 * Some of the below (the private methods) are helpers for the public
	 * interface, which are not referenced outside of these public functions.
	 * The main idea is that these functions allow a generic template of actions
	 * to be applied to specific actions, such as "moveLeftOnce" / "moveRightOnce".
	 * 
	 ***************************************************************************
	 */
	
	/**
	 * If possible, move the piece one tick to the left.  If not, do nothing.
	 * Returns whether the piece moved.
	 */
	public synchronized boolean moveLeftOnce() {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		delta.y = 0 ;
		delta.x = -1 ;
		
		if ( moveHorizontallyOnce( delta ) ) {
			// We succeed in moving the piece left.
			adapter.game_didMove( ActionAdapter.MOVE_DIRECTION_LEFT ) ;
			return true ;
		}
		return false ;
	}
	
	
	/**
	 * Returns whether the game is in a state where leftward movement is OK.
	 * NOTE: This is distinct from a move to the left failing because because
	 * of an obstacle; in that case the STATE allows leftward movement, but
	 * the actual blockfield prevents it.
	 * 
	 * One possible use of this method is to determine, in advance, if a leftward
	 * action should be dequeued.  If this method returns false, we know that
	 * a call to moveLeftOnce() (assuming it occurs before some other method call)
	 * will have absolutely no effect.
	 */
	public synchronized boolean stateOK_moveLeftOnce() {
		return stateOK_moveHorizontallyOnce() ;
	}
	
	/**
	 * If possible, move the piece one tick to the right.  If not, do nothing.
	 * Returns whether the piece moved.
	 */
	public synchronized boolean moveRightOnce() {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		delta.y = 0 ;
		delta.x = 1 ;
		
		if ( moveHorizontallyOnce( delta ) ) {
			// We succeed in moving the piece right.
			adapter.game_didMove( ActionAdapter.MOVE_DIRECTION_RIGHT ) ;
			return true ;
		}
		return false ;
	}
	
	/**
	 * Returns whether the game is in a state where rightward movement is OK.
	 * NOTE: This is distinct from a move to the right failing because because
	 * of an obstacle; in that case the STATE allows rightward movement, but
	 * the actual blockfield prevents it.
	 * 
	 * One possible use of this method is to determine, in advance, if a rightward
	 * action should be dequeued.  If this method returns false, we know that
	 * a call to moveRightOnce() (assuming it occurs before some other method call)
	 * will have absolutely no effect.
	 */
	public synchronized boolean stateOK_moveRightOnce() {
		return stateOK_moveHorizontallyOnce() ;
	}
	
	/**
	 * If possible, move the piece using the assigned delta.  We assume that
	 * 'delta' is a move to the left or right; y value is ignored.
	 * @param delta The difference for the move.
	 */
	private synchronized boolean moveHorizontallyOnce( Offset delta ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		if ( s.state == STATE_FALLING || s.state == STATE_ENTERING ) {
			// Okay to move
			if ( ts.canMove( delta ) ) {
				// Timing system says it's fine...
				tempOffset.x = s.offset.x + delta.x ;
				tempOffset.y = s.offset.y ;
				// Movement should occur if:
				//		1. The piece is falling, and there is no collision
				//		2. The piece is entering, and the move doesn't take it
				//			outside the blockField bounds.
				if ( ( s.state == STATE_FALLING && !cs.collides(s.blockField, s.piece, tempOffset) )
						|| ( s.state == STATE_ENTERING && cs.within(s.blockField, s.piece, tempOffset) ) ) {
					// If doesn't collide, do the move!
					s.offset.takeVals(tempOffset) ;
					ts.didMove( delta ) ;
					if ( delta.x > 0 ) {
						gevents.setHappened( GameEvents.EVENT_PIECE_MOVED_RIGHT ) ;
						s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_MOVED_RIGHT ) ;
					}
					else if ( delta.x < 0 ) {
						gevents.setHappened( GameEvents.EVENT_PIECE_MOVED_LEFT ) ;
						s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_MOVED_LEFT ) ;
					}
					
					
					// Tell the trigger system
					trs.didMove(delta) ;
					return true ;
				} else if ( s.state == STATE_FALLING
						&& cs.collides(s.blockField, s.piece, tempOffset)
						&& !cs.collides(s.blockField, s.piece, s.offset) ) {
					if ( delta.x > 0 ) {
						gevents.setHappened( GameEvents.EVENT_PIECE_FAILED_MOVE_RIGHT ) ;
						s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_FAILED_MOVE_RIGHT ) ;
					} else if ( delta.x < 0 ) {
						gevents.setHappened( GameEvents.EVENT_PIECE_FAILED_MOVE_LEFT ) ;
						s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_FAILED_MOVE_LEFT ) ;
					}
				}
			}
		}
		
		return false ;
	}
	
	
	/**
	 * Returns whether the game is in a state where horizontal movement is OK.
	 * NOTE: This is distinct from a move failing because because
	 * of an obstacle; in that case the STATE allows horizontal movement, but
	 * the actual blockfield prevents it.
	 * 
	 * One possible use of this method is to determine, in advance, if a horizontal
	 * action should be dequeued.  If this method returns false, we know that
	 * a call to moveLeftOnce() or moveRightOnce() (assuming it occurs before
	 * some other method call) will have absolutely no effect.
	 */
	public synchronized boolean stateOK_moveHorizontallyOnce() {
		return s.state == STATE_FALLING ;
	}
	
	/**
	 * If possible, drop the currently falling piece to the bottom.
	 */
	public synchronized void drop() {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// If the piece is entering, then enter it!
		if ( s.state == STATE_ENTERING )
			enterPiece() ;
		
		// Now, if the piece is falling, drop it as far as you can.
		if ( s.state == STATE_FALLING ) {
			// How far?
			int dist = cs.spaceBelow(s.blockField, s.piece, s.offset, true) ;	// DO have a wall
			// This is our new offset.
			tempOffset.x = s.offset.x ;
			tempOffset.y = s.offset.y - dist ;
			// Drop that beat like an ugly baby
			if ( !adapter.game_shouldUseTimingSystem(this) || ts.canDrop(s.piece, tempOffset) ) {
				
				// Tell the trigger system first.  This might affect the score we get!
				trs.didDrop(s.piece, tempOffset) ;
				
				// Drop it!
				// TODO: Add necessary metadata to support fancy falling?
				ss.scoreDrop(s.piece, s.offset, tempOffset) ;
				s.offset.y = tempOffset.y ;
				ts.didDrop(s.piece, s.offset) ;
				gevents.setHappened( GameEvents.EVENT_PIECE_DROPPED ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_DROPPED ) ;
				
				// Inform the adapter.
				adapter.game_didMove(ActionAdapter.MOVE_DIRECTION_DOWN) ;
			}
		}
	}
	
	/**
	 * Returns whether the game is in a state where a drop is OK.
	 * NOTE: This is distinct from a drop failing because an obstacle is
	 * below and the piece will travel 0 blocks; that concerns the blockField,
	 * NOT the game state.
	 */
	public synchronized boolean stateOK_drop() {
		return s.state == STATE_FALLING ;
	}
	
	/**
	 * If appropriate, turns the piece 90 degrees clockwise.
	 * @throws InvalidPieceException
	 */
	public synchronized void turnCW( int kickLean ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		/*
		 * Kicking rules are different based on whether the
		 * piece is currently entering (i.e., does not collide
		 * with field contents, only with field walls) or falling
		 * (i.e., collides with both field walls and field contents).
		 * In order to implement this method as a generic operation,
		 * set a pointer to the appropriate field (blockfield or empty),
		 * perform the turn, then call a turn resolution helper that works
		 * for any turn type.
		 */
		byte [][][] field = null ;
		if ( s.state == STATE_ENTERING )
			field = emptyField ;
		else if ( s.state == STATE_FALLING )
			field = s.blockField ;
		
		// Perform the turn 
		if ( field != null && ts.canTurn(s.piece, RotationSystem.TURN_CW) ) {
			// Turn the piece...
			rs.turnCW(s.piece) ;
			
			// Handle the consequences.
			if ( resolveTurn( field, s.piece, s.offset, kickLean ) ) {
				// If the turn resolved successfully (true), tell the adapter.
				adapter.game_didTurnCW( leanToActionAdapterMoveDirection( kickLean ) ) ;
				// ...and adjust game events
				gevents.setHappened( GameEvents.EVENT_PIECE_TURNED_CW ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_TURNED_CW ) ;
			}
		}
	}
	
	/**
	 * Returns whether the game is in a state where a CW turn is OK
	 * NOTE: This is distinct from a turn failing because of obstacles.
	 * Even in blockfields that prevent any rotation, the game STATE
	 * still allows a turn; it is merely the game board that prevents it.
	 */
	public synchronized boolean stateOK_turnCW() {
		return stateOK_turn() ;
	}
	
	/**
	 * If appropriate, turns the piece 90 degrees counterclockwise.
	 * @throws InvalidPieceException
	 */
	public synchronized void turnCCW( int kickLean ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		/*
		 * Kicking rules are different based on whether the
		 * piece is currently entering (i.e., does not collide
		 * with field contents, only with field walls) or falling
		 * (i.e., collides with both field walls and field contents).
		 * In order to implement this method as a generic operation,
		 * set a pointer to the appropriate field (blockfield or empty),
		 * perform the turn, then call a turn resolution helper that works
		 * for any turn type.
		 */
		byte [][][] field = null ;
		if ( s.state == STATE_ENTERING )
			field = emptyField ;
		else if ( s.state == STATE_FALLING )
			field = s.blockField ;
		
		// Perform the turn 
		if ( field != null && ts.canTurn(s.piece, RotationSystem.TURN_CCW) ) {
			// Turn the piece...
			rs.turnCCW(s.piece) ;
			
			// Handle the consequences.
			if ( resolveTurn( field, s.piece, s.offset, kickLean ) ) {
				// If the turn resolved successfully (true), tell the adapter.
				adapter.game_didTurnCCW( leanToActionAdapterMoveDirection( kickLean ) ) ;
				// ...and adjust game events
				gevents.setHappened( GameEvents.EVENT_PIECE_TURNED_CCW ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_TURNED_CCW ) ;
			}
		}
		
		//System.err.println("turnCCW piece is " + s.piece.type + " " + s.piece.rotation + " game is " + this) ;
		
	}
	
	/**
	 * Returns whether the game is in a state where a CCW turn is OK
	 * NOTE: This is distinct from a turn failing because of obstacles.
	 * Even in blockfields that prevent any rotation, the game STATE
	 * still allows a turn; it is merely the game board that prevents it.
	 */
	public synchronized boolean stateOK_turnCCW() {
		return stateOK_turn() ;
	}
	
	/**
	 * If appropriate, turns the piece 180 degrees clockwise.
	 * @throws InvalidPieceException
	 */
	public synchronized void turnCW180( int kickLean ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		/*
		 * Kicking rules are different based on whether the
		 * piece is currently entering (i.e., does not collide
		 * with field contents, only with field walls) or falling
		 * (i.e., collides with both field walls and field contents).
		 * In order to implement this method as a generic operation,
		 * set a pointer to the appropriate field (blockfield or empty),
		 * perform the turn, then call a turn resolution helper that works
		 * for any turn type.
		 */
		byte [][][] field = null ;
		if ( s.state == STATE_ENTERING )
			field = emptyField ;
		else if ( s.state == STATE_FALLING )
			field = s.blockField ;
		
		// Perform the turn 
		if ( field != null && ts.canTurn(s.piece, RotationSystem.TURN_CW180) ) {
			// Turn the piece...
			rs.turnCW180(s.piece) ;
			
			// Handle the consequences.
			if ( resolveTurn( field, s.piece, s.offset, kickLean ) ) {
				// If the turn resolved successfully (true), tell the adapter.
				adapter.game_didTurnCW180( leanToActionAdapterMoveDirection( kickLean ) ) ;
				// ...and adjust game events
				gevents.setHappened( GameEvents.EVENT_PIECE_TURNED_CW_180 ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_TURNED_CW_180 ) ;
			}
		}
	}
	
	/**
	 * Returns whether the game is in a state where a 180 CW turn is OK
	 * NOTE: This is distinct from a turn failing because of obstacles.
	 * Even in blockfields that prevent any rotation, the game STATE
	 * still allows a turn; it is merely the game board that prevents it.
	 */
	public synchronized boolean stateOK_turnCW180() {
		return stateOK_turn() ;
	}
	
	/**
	 * If appropriate, turns the piece 180 degrees counterclockwise.
	 * @throws InvalidPieceException
	 */
	public synchronized void turnCCW180( int kickLean ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		/*
		 * Kicking rules are different based on whether the
		 * piece is currently entering (i.e., does not collide
		 * with field contents, only with field walls) or falling
		 * (i.e., collides with both field walls and field contents).
		 * In order to implement this method as a generic operation,
		 * set a pointer to the appropriate field (blockfield or empty),
		 * perform the turn, then call a turn resolution helper that works
		 * for any turn type.
		 */
		byte [][][] field = null ;
		if ( s.state == STATE_ENTERING )
			field = emptyField ;
		else if ( s.state == STATE_FALLING )
			field = s.blockField ;
		
		// Perform the turn 
		if ( field != null && ts.canTurn(s.piece, RotationSystem.TURN_CCW180) ) {
			// Turn the piece...
			rs.turnCCW180(s.piece) ;
			
			// Handle the consequences.
			if ( resolveTurn( field, s.piece, s.offset, kickLean ) ) {
				// If the turn resolved successfully (true), tell the adapter.
				adapter.game_didTurnCCW180( leanToActionAdapterMoveDirection( kickLean ) ) ;
				// ...and adjust game events
				gevents.setHappened( GameEvents.EVENT_PIECE_TURNED_CCW_180 ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_TURNED_CCW_180 ) ;
			}
		}
	}
	
	/**
	 * Returns whether the game is in a state where a 180 CCW turn is OK
	 * NOTE: This is distinct from a turn failing because of obstacles.
	 * Even in blockfields that prevent any rotation, the game STATE
	 * still allows a turn; it is merely the game board that prevents it.
	 */
	public synchronized boolean stateOK_turnCCW180() {
		return stateOK_turn() ;
	}
	
	/**
	 * Returns whether the game is in a state where a turn is OK
	 * NOTE: This is distinct from a turn failing because of obstacles.
	 * Even in blockfields that prevent any rotation, the game STATE
	 * still allows a turn; it is merely the game board that prevents it.
	 */
	public synchronized boolean stateOK_turn() {
		return s.state == STATE_FALLING && GameModes.hasRotation(ginfo) ;
	}
	
	/**
	 * Attempts to resolve the turn just performed on the provided piece.
	 * Will kick the piece (if necessary) and undo the turn if no valid
	 * location for it can be found.  Notifies the timing system of
	 * any changes made.
	 * 
	 * PRECONDITION: 'piece' was at 'offset', which was a valid position
	 * 		in 'field'.  A single rotation function (turnCW, turnCCW, turnCW180,
	 * 		turnCCW180) was then called on 'piece'
	 * 
	 * POSTCONDITION: 'piece' at 'offset' is valid for 'field'.  The timing system
	 * 		has been notified of any changes.  Specific outcomes are
	 * 		(in order of priority)
	 * 		1. Exactly as called
	 * 		2. Piece has been kicked
	 * 		3. Turn has been "undone" <-- note precondition that everything
	 * 										was valid before the turn!
	 * 		
	 * 
	 * @param field The field to check for collisions
	 * @param piece The piece (just rotated)
	 * @param offset 
	 * @return Returns whether the turn produced some change to the piece.
	 * 
	 * @throws InvalidPieceException
	 */
	public synchronized boolean resolveTurn( byte [][][] field, Piece piece, Offset offset, int kickLean ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// set the "lean" for our kick.
		setLean( lean, kickLean ) ;
		
		// The piece has been recently turned.  If there is
		// a collision, try kicking - if no kick is appropriate, undo
		// the turn (shouldn't be too tough - the piece stores the
		// previous rotation, after all).
		// ADJUSTMENT: to allow "shake" kicks, we always kick, even if we don't
		// need to.  The 'didKick' value is set according to whether offset changed.
		boolean didKick = false ;
		tempOffset.takeVals(offset) ;
		if ( ts.canKick(piece) ) {
			if ( ks.kick(cs, field, piece, offset, lean) ) {
				didKick = offset.x != tempOffset.x || offset.y != tempOffset.y ;
			} else {
				rs.undoTurn(piece) ;
				return false ;
			}
		} else if ( cs.collides( field, piece, offset ) ) {
			rs.undoTurn(piece) ;
			return false ;
		}
		
		// If we get here, either there was no collision (and the turn was OK)
		// or we turned and kicked, and everything's fine now.
		ts.didTurn(piece) ;
		trs.didTurn(piece) ;
		if ( didKick ) {
			ts.didKick(piece, offset) ;
			trs.didKick(piece, offset) ;
			gevents.setHappened( GameEvents.EVENT_PIECE_KICKED ) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_KICKED ) ;
		}
		
		// Done.
		return true ;
	}
	
	
	/**
	 * Returns whether the game is in a state where a 180 CW turn is OK
	 * NOTE: This is distinct from a turn failing because of obstacles.
	 * Even in blockfields that prevent any rotation, the game STATE
	 * still allows a turn; it is merely the game board that prevents it.
	 */
	public synchronized boolean stateOK_flip() {
		return s.state == STATE_FALLING && GameModes.hasReflection(ginfo) ;
	}
	
	/**
	 * If appropriate, turns the piece 180 degrees counterclockwise.
	 * @throws InvalidPieceException
	 */
	public synchronized void flip( int kickLean ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		/*
		 * Kicking rules are different based on whether the
		 * piece is currently entering (i.e., does not collide
		 * with field contents, only with field walls) or falling
		 * (i.e., collides with both field walls and field contents).
		 * In order to implement this method as a generic operation,
		 * set a pointer to the appropriate field (blockfield or empty),
		 * perform the turn, then call a turn resolution helper that works
		 * for any turn type.
		 */
		byte [][][] field = null ;
		if ( s.state == STATE_ENTERING )
			field = emptyField ;
		else if ( s.state == STATE_FALLING )
			field = s.blockField ;
		
		// Perform the turn 
		if ( field != null && ts.canFlip(s.piece) ) {
			// Flipping is, by its very nature, completely reversible.
			rs.flipVertical(s.piece, s.offset) ;
			
			// HANDLE THE CONSEQUENCES!
			// set the "lean" for our kick.
			setLean( lean, kickLean ) ;
			
			boolean didKick = false ;
			tempOffset.takeVals(s.offset) ;
			if ( ts.canKick(s.piece) ) {
				if ( ks.kick(cs, field, s.piece, s.offset, lean) ) {
					didKick = s.offset.x != tempOffset.x || s.offset.y != tempOffset.y ;
				} else {
					// undo the flip!
					rs.flipVertical(s.piece, s.offset) ;
					return ;
				}
			} else if ( cs.collides( field, s.piece, s.offset ) ) {
				// undo the flip!
				rs.flipVertical(s.piece, s.offset) ;
				return ;
			}
			
			// If we get here, either there was no collision (and the turn was OK)
			// or we turned and kicked, and everything's fine now.
			ts.didFlip(s.piece, s.offset) ;
			trs.didFlip(s.piece, s.offset) ;
			gevents.setHappened( GameEvents.EVENT_PIECE_FLIPPED ) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_FLIPPED ) ;
			if ( didKick ) {
				ts.didKick(s.piece, s.offset) ;
				trs.didKick(s.piece, s.offset) ;
				gevents.setHappened( GameEvents.EVENT_PIECE_KICKED ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_KICKED ) ;
			}
		}
	}
	
	
	
	protected void setLean( Offset lean, int leanCode ) {
		switch ( leanCode ) {
		case LEAN_LEFT:
			lean.x = -1 ;
			lean.y = 0 ;
			break ;
		case LEAN_RIGHT:
			lean.x = 1 ;
			lean.y = 0 ;
			break ;
		case LEAN_DOWN:
			lean.x = 0 ;
			lean.y = -1 ;
			break ;
		case LEAN_DOWN_LEFT:
			lean.x = -1 ;
			lean.y = -1 ;
			break ;
		case LEAN_DOWN_RIGHT:
			lean.x = 1 ;
			lean.y = -1 ;
			break ;
		default:
			lean.x = 0 ;
			lean.y = 0 ;
			break ;
		}
	}
	
	protected int leanToActionAdapterMoveDirection( int leanCode ) {
		switch ( leanCode ) {
		case LEAN_LEFT:
			return ActionAdapter.MOVE_DIRECTION_LEFT ;
		case LEAN_RIGHT:
			return ActionAdapter.MOVE_DIRECTION_RIGHT ;
		case LEAN_DOWN:
			return ActionAdapter.MOVE_DIRECTION_DOWN ;
		case LEAN_DOWN_LEFT:
			return ActionAdapter.MOVE_DIRECTION_DOWN_LEFT;
		case LEAN_DOWN_RIGHT:
			return ActionAdapter.MOVE_DIRECTION_DOWN_RIGHT ;
		default:
			return ActionAdapter.MOVE_DIRECTION_NONE ;
		}
	}
	
	
	/**
	 * Use the reserve piece; put it into operation.
	 * @throws InvalidPieceException
	 */
	public synchronized void useReserve( int kickLean ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// Use a reserve piece.  Specific behavior differs
		// based on whether we are swapping or inserting (or
		// attacking, for multiplayer).
		
		if ( !canUseReserve() )
			return ;
		
		int rb = GameModes.reserveBehavior( ginfo ) ;
		
		// In some states, we wait.
		if ( s.state != STATE_FALLING ) {
			s.reserveQueuedForNextCycle = true ;
			int event = -1 ;
			switch( rb ) {
			case GameModes.RESERVE_BEHAVIOR_SWAP:
			case GameModes.RESERVE_BEHAVIOR_SWAP_REENTER:
				event = s.reservePieces[0] == null || s.reservePieces[0].type == -1
						? GameEvents.EVENT_RESERVE_PUSH_QUEUED
						: GameEvents.EVENT_RESERVE_SWAP_QUEUED ;
				break ;
			case GameModes.RESERVE_BEHAVIOR_INSERT:
			case GameModes.RESERVE_BEHAVIOR_INSERT_REENTER:
				event = GameEvents.EVENT_RESERVE_INSERT_QUEUED ;
				break ;
			case GameModes.RESERVE_BEHAVIOR_SPECIAL:
				if ( sps.specialIsReserveInsert() )
					event = GameEvents.EVENT_RESERVE_INSERT_QUEUED ;
				else if ( sps.specialIsAttack() )
					event = GameEvents.EVENT_RESERVE_ATTACK_QUEUED ;
				break ;
			}
			
			if ( event != -1 ) {
				gevents.setHappened(event) ;
				s.geventsLastTick.setHappened(event) ;
			}
			return ;
		}

		
		setLean( lean, kickLean ) ;
		int moveDirection = leanToActionAdapterMoveDirection( kickLean ) ;
		
		// TODO: Add support for GameEvents and triggering conditions.
		//			Also add support for controls enforcing a "one use
		//			per piece entered" limit.
		
		// If behavior is "SWAP", then replace the current piece with
		// the "reservePiece".  This might mess up in PROGRESSION,
		// so, uh... don't do it then?
		if ( rb == GameModes.RESERVE_BEHAVIOR_SWAP ) {
			if ( s.reservePieces[0] == null || s.reservePieces[0].type == -1 ) {
				// Special case - move current piece to reserve.
				if ( s.state == STATE_PROGRESSION || s.state == STATE_PREPARING ) {
					s.reservePieces[0].takeDefaultVals(s.nextPieces[0]) ;
					for ( int i = 0; i < GameState.PIECE_LOOKAHEAD-1; i++ )
						s.nextPieces[i].takeDefaultVals( s.nextPieces[i+1] ) ;
					pieceBag.pop( s.nextPieces[GameState.PIECE_LOOKAHEAD-1] );
					rs.turn0(s.nextPieces[GameState.PIECE_LOOKAHEAD-1]) ;
					
					// Trigger first, then score.  Remember that the trigger might affect our score!
					trs.didPutInReserve(s.reservePieces[0]) ;
					ss.scorePutInReserve(s.reservePieces[0]) ;
					
					// Piece history
					pieceHistory.setPiecePushedIntoBag(PieceHistory.RESERVE_BAG) ;
					pieceHistory.setPieceHeldInBag(s.nextPieces[0].type, PieceHistory.PIECE_BAG) ;
					pieceHistory.setPieceHeldInBag(s.reservePieces[0].type, PieceHistory.RESERVE_BAG) ;
					
					gevents.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
					gevents.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
					gevents.setHappened( GameEvents.EVENT_RESERVE_PUSHED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
					gevents.setHappened( GameEvents.EVENT_RESERVE_PUSHED ) ;
					
					// We INSERTED a piece.  This does NOT end a cycle, since a cycle
					// wasn't running.
					adapter.game_didUseReserve(moveDirection) ;
					
					//usedReserve = true ;
					// Special case: the piece that was to fall has been put in reserve,
					// and another piece followed it.  Allow that other piece to be
					// swapped if necessary (THAT time, we will set "usedReserve")
				}
				else {
					// Case: We are using a swap-reserve, it is currently empty,
					// and a piece was falling.
					s.reservePieces[0].takeDefaultVals(s.piece) ;
					s.piece.type = -1 ;
					s.piece.defaultType = -1 ;
					s.offset.y = 100 ;
					
					// Trigger first, then score.  Remember that the trigger might affect our score!
					trs.didPutInReserve(s.reservePieces[0]) ;
					ss.scorePutInReserve(s.reservePieces[0]) ;
					
					pieceHistory.setDoneWithCurrentPiece() ;
					pieceHistory.setPiecePushedIntoBag(PieceHistory.RESERVE_BAG) ;
					pieceHistory.setPieceHeldInBag(s.reservePieces[0].type, PieceHistory.RESERVE_BAG) ;
					
					gevents.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
					gevents.setHappened( GameEvents.EVENT_RESERVE_PUSHED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PUSHED ) ;
					
					
					// We move our piece to reserve.  That ends the cycle!
					adapter.game_didUseReserve(moveDirection) ;
					adapter.game_didEndActionCycle() ;
					
					s.state = STATE_ENDING_CYCLE ;
					
					//System.out.println("moved to reserve; synchronizing") ;
					//usedReserve = true ;
					// Special case: the piece that was to fall has been put in reserve,
					// and another piece followed it.  Allow that other piece to be
					// swapped if necessary (THAT time, we will set "usedReserve")
				}
				
				// Stub: fill in with GameEvents and triggering conditions.
			}
			else {
				// Try swapping the "current" and "reserve" pieces.
				// Note that we need to make sure there's room for
				// the reserve.  Do the swap and try a kick.  If
				// not, don't swap.
				// Note that, with the option for fixed-rotation, we no
				// longer allow piece rotation to carry over to the swapped
				// piece.  HOWEVER, we need to still track rotation, so we
				// can restore later.  Do this by storing the current piece
				// in tempPiece.
				if ( s.state == STATE_PROGRESSION || s.state == STATE_PREPARING ) {
					Piece.swap(s.nextPieces[0], s.reservePieces[0]) ;
				}
				else {
					tempPiece.takeVals(s.piece) ;
					s.piece.centerGravity(tempCenter) ;
					
					// We keep the rotation in the currently active piece.
					Piece.swapDefaults(s.piece, s.reservePieces[0]) ;
					rs.turn0(s.piece) ;
					
					s.piece.centerGravity(tempCenterReserve) ;
					
					// we want these gravity centers to line up exactly.  If
					// tempCenterReserve is less than tempCenter (say in X)
					// then it will appear shifted to the left; to compensate
					// we ADD the difference to x.
					// During this process we store the previous offset in tempOffset,
					// in case we need to revert (faster than recalculating the difference).
					tempOffset.takeVals(s.offset) ;
					s.offset.x += ( tempCenter.x - tempCenterReserve.x ) ;
					s.offset.y += ( tempCenter.y - tempCenterReserve.y ) ;
				}
				
				// Check for collision, assuming the piece was falling.
				if ( s.state == STATE_ENTERING || s.state == STATE_FALLING ) {
					if ( cs.collides(s.blockField, s.piece, s.offset) ) {
						// Try a kick.
						if ( !ks.kick(cs, s.blockField, s.piece, s.offset, lean) ) {
							// Revert
							s.reservePieces[0].takeDefaultVals(s.piece) ;
							s.piece.takeVals(tempPiece) ;
							rs.turn0(s.piece) ;
							s.offset.takeVals(tempOffset) ;
						}
						else {
							// Stub: fill in with triggering conditions and
							// GameEvents code.
							gevents.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
							gevents.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
							s.geventsLastTick.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
							s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
							
							gevents.setHappened( GameEvents.EVENT_RESERVE_SWAPPED ) ;
							s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_SWAPPED ) ;
							
							// Case: we swapped the reserve piece with the falling one.
							// Trigger first, then score.  Remember that the trigger might affect our score!
							trs.didPutInReserve(s.reservePieces[0]) ;
							trs.didSwap(s.piece, s.offset) ;
							ss.scorePutInReserve(s.reservePieces[0]) ;
							ss.scoreUseReserve(s.piece, s.offset) ;
							
							// Tell the piece history.  We swapped in-place the falling piece and the reserve.
							pieceHistory.setDoneWithCurrentPiece() ;
							pieceHistory.setPiecePopped(s.piece.type, PieceHistory.RESERVE_BAG) ;
							pieceHistory.setPiecePushedIntoBag(PieceHistory.RESERVE_BAG) ;
							pieceHistory.setPieceHeldInBag(s.reservePieces[0].type, PieceHistory.RESERVE_BAG) ;
							
							s.usedReserve = true ;
							adapter.game_didUseReserve(moveDirection) ;
						}
					}
					else {
						// No collision; swap OK.  Note everything.
						gevents.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
						gevents.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
						s.geventsLastTick.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
						s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
						
						gevents.setHappened( GameEvents.EVENT_RESERVE_SWAPPED ) ;
						s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_SWAPPED ) ;
						
						// Trigger first, then score.  Remember that the trigger might affect our score!
						trs.didPutInReserve(s.reservePieces[0]) ;
						trs.didSwap(s.piece, s.offset) ;
						ss.scorePutInReserve(s.reservePieces[0]) ;
						ss.scoreUseReserve(s.piece, s.offset) ;
						
						// Tell the piece history.  We swapped in-place the falling piece and the reserve.
						pieceHistory.setDoneWithCurrentPiece() ;
						pieceHistory.setPiecePopped(s.piece.type, PieceHistory.RESERVE_BAG) ;
						pieceHistory.setPiecePushedIntoBag(PieceHistory.RESERVE_BAG) ;
						pieceHistory.setPieceHeldInBag(s.reservePieces[0].type, PieceHistory.RESERVE_BAG) ;
						
						
						s.usedReserve = true ;
						adapter.game_didUseReserve(moveDirection) ;
					}
				}
				else {
					// Stub: fill in triggering conditions and gevents code.
					gevents.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
					gevents.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
					
					gevents.setHappened( GameEvents.EVENT_RESERVE_SWAPPED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_SWAPPED ) ;
					
					// Trigger first, then score.
					// Trigger first, then score.  Remember that the trigger might effect our score!
					trs.didPutInReserve(s.reservePieces[0]) ;
					trs.didInsert(s.nextPieces[0]) ;
					ss.scorePutInReserve(s.reservePieces[0]) ;
					ss.scoreUseReserve(s.nextPieces[0]) ;
					
					// We swapped the reserve and the "next" piece.
					pieceHistory.setPiecePushedIntoBag(PieceHistory.PIECE_BAG) ;
					pieceHistory.setPiecePushedIntoBag(PieceHistory.RESERVE_BAG) ;
					pieceHistory.setPieceHeldInBag(s.nextPieces[0].type, PieceHistory.PIECE_BAG) ;
					pieceHistory.setPieceHeldInBag(s.reservePieces[0].type, PieceHistory.RESERVE_BAG) ;
					
					s.usedReserve = true ;
					adapter.game_didUseReserve(moveDirection) ;
				}
			}
		}
		
		else if ( rb == GameModes.RESERVE_BEHAVIOR_INSERT ) {
			// Insertion is different.  Push the current piece
			// to nextPiece, and nextPiece back on the pieceBag
			// stack.  Replace 'piece' with the reserve, and pop
			// a new reserve for the stack.
			if ( s.state == STATE_PROGRESSION || s.state == STATE_PREPARING ) {
				// Replace nextPiece, not current.
				pieceBag.push( s.nextPieces[GameState.PIECE_LOOKAHEAD-1] ) ;
				for ( int i = GameState.PIECE_LOOKAHEAD-1; i > 0; i-- )
					s.nextPieces[i].takeVals( s.nextPieces[i-1] ) ;
				s.nextPieces[0].takeVals(s.reservePieces[0]) ;
				for ( int i = 0; i < GameState.PIECE_LOOKAHEAD-1; i++ )
					s.reservePieces[i].takeVals( s.reservePieces[i+1] ) ;
				reserveBag.pop( s.reservePieces[GameState.PIECE_LOOKAHEAD-1] );
				rs.turn0(s.reservePieces[GameState.PIECE_LOOKAHEAD-1]) ;
				
				// Stub for triggering conditions, GameEvents
				gevents.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
				gevents.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
				
				gevents.setHappened( GameEvents.EVENT_RESERVE_INSERTED ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_INSERTED ) ;
				
				// We inserted the piece into the next queue.
				trs.didInsert(s.nextPieces[0]) ;
				ss.scoreUseReserve(s.nextPieces[0]) ;
				
				// Tell the piece history.  We pushed into next and put a new one
				// on reserve.
				pieceHistory.setPiecePushedIntoBag(PieceHistory.PIECE_BAG) ;
				pieceHistory.setPieceHeldInBag(s.nextPieces[0].type, PieceHistory.PIECE_BAG) ;
				pieceHistory.setPieceHeldInBag(s.reservePieces[0].type, PieceHistory.RESERVE_BAG) ;
				
				
				s.usedReserve = true ;
				adapter.game_didUseReserve(moveDirection) ;
			}
			else {
				s.piece.centerGravity(tempCenter) ;
				
				// We attempt a kick / collision check before swapping the
				// piece in.  This lets us do 0 or 1 swaps rather than
				// swap - check - swap back.
				// We no longer retain rotation after a swap, to deal with flipped
				// pieces or those with no allowed rotations.
				rs.turn0( s.reservePieces[0] ) ;
				s.reservePieces[0].centerGravity(tempCenterReserve) ;
				
				// we want these gravity centers to line up exactly.  If
				// tempCenterReserve is less than tempCenter (say in X)
				// then it will appear shifted to the left; to compensate
				// we ADD the difference to x.
				// During this process we store the previous offset in tempOffset,
				// in case we need to revert (faster than recalculating the difference).
				tempOffset.takeVals(s.offset) ;
				s.offset.x += ( tempCenter.x - tempCenterReserve.x ) ;
				s.offset.y += ( tempCenter.y - tempCenterReserve.y ) ;
				
				if ( ks.kick(cs, s.blockField, s.reservePieces[0], s.offset, lean)) {
					// Success!  Use the reserve, it fits at "offset".
					pieceBag.push( s.nextPieces[GameState.PIECE_LOOKAHEAD-1] ) ;
					for ( int i = GameState.PIECE_LOOKAHEAD-1; i > 0; i-- )
						s.nextPieces[i].takeVals( s.nextPieces[i-1] ) ;
					s.nextPieces[0].takeDefaultVals(s.piece) ;
					s.piece.takeDefaultVals(s.reservePieces[0]) ;
					rs.turn0(s.piece) ;
					for ( int i = 0; i < GameState.PIECE_LOOKAHEAD-1; i++ )
						s.reservePieces[i].takeVals( s.reservePieces[i+1] ) ;
					reserveBag.pop( s.reservePieces[GameState.PIECE_LOOKAHEAD-1] );
					rs.turn0(s.reservePieces[GameState.PIECE_LOOKAHEAD-1]) ;
					
					// Stub for triggering conditions, GameEvents
					gevents.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
					gevents.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
					
					gevents.setHappened( GameEvents.EVENT_RESERVE_INSERTED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_INSERTED ) ;
					
					// We swapped the piece with our reserve, pushing the original
					// into the 'next' queue.
					trs.didSwap(s.piece, s.offset) ;
					ss.scoreUseReserve(s.piece, s.offset) ;
					
					// We pushed the currently falling piece on top of next, and "popped" the
					// reserve into play.
					pieceHistory.setDoneWithCurrentPiece() ;
					pieceHistory.setPiecePushedIntoBag(PieceHistory.PIECE_BAG) ;
					pieceHistory.setPiecePopped(s.piece.type, PieceHistory.RESERVE_BAG) ;
					pieceHistory.setPieceHeldInBag(s.nextPieces[0].type, PieceHistory.PIECE_BAG) ;
					pieceHistory.setPieceHeldInBag(s.reservePieces[0].type, PieceHistory.RESERVE_BAG) ;
					
					
					
					s.usedReserve = true ;
					adapter.game_didUseReserve(moveDirection) ;
				}
				else {
					// no room!  Revert!
					s.offset.takeVals(tempOffset) ;
				}
			}
		}
		
		else if ( rb == GameModes.RESERVE_BEHAVIOR_SWAP_REENTER ) {
			// swaps the top reserve piece and the next one.  Unlike standard
			// 'swap' behavior, where we maintain the current position, the
			// new piece will 're-enter' from the top following standard
			// prepare -> enter behavior.  Among other things, this means the
			// new piece starts at the top of the screen, fall-timing resets,
			// etc.
			if ( s.reservePieces[0] == null || s.reservePieces[0].type == -1 ) {
				// Special case - move current piece to reserve.
				if ( s.state != STATE_FALLING ) {
					throw new IllegalStateException("SWAP_REENTER is only supported for STATE_FALLING!") ;
				}
				else {
					// Case: We are using a swap-reserve, it is currently empty,
					// and a piece was falling.  Move that piece to reserve,
					// and prepare the next piece.
					s.reservePieces[0].takeDefaultVals(s.piece) ;
					rs.turn0(s.reservePieces[0]) ;
					
					
					pieceHistory.setDoneWithCurrentPiece() ;
					pieceHistory.setPiecePushedIntoBag(PieceHistory.RESERVE_BAG) ;
					pieceHistory.setPieceHeldInBag(s.reservePieces[0].type, PieceHistory.RESERVE_BAG) ;
					
					gevents.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
					
					gevents.setHappened( GameEvents.EVENT_RESERVE_PUSHED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PUSHED ) ;
					
					// We move our piece to reserve.
					s.usedReserve = true ;
					adapter.game_didUseReserve(moveDirection) ;
					
					// Trigger first, then score.  Remember that the trigger might affect our score!
					trs.didPutInReserve(s.reservePieces[0]) ;
					ss.scorePutInReserve(s.reservePieces[0]) ;
					
					// preparePiece
					preparePiece() ;
					// this call sets the current state to ENTERING.
				}
				
				// Stub: fill in with GameEvents and triggering conditions.
			}
			else {
				if ( s.state != STATE_FALLING ) {
					throw new IllegalStateException("SWAP_REENTER is only supported for STATE_FALLING!") ;
				}
				else {
					// Push 'next' pieces up the stack; the reserve piece will go there (to fall in next).
					pieceBag.push( s.nextPieces[GameState.PIECE_LOOKAHEAD-1] ) ;
					for ( int i = GameState.PIECE_LOOKAHEAD -1; i > 0; i-- )
						s.nextPieces[i].takeVals(s.nextPieces[i-1]) ;
					
					// put the reserve next
					s.nextPieces[0].takeVals(s.reservePieces[0]) ;
					// put the current piece as the reserve.
					s.reservePieces[0].takeDefaultVals(s.piece) ;
					rs.turn0(s.reservePieces[0]) ;
					
					// make all our necessary notes
					gevents.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
					gevents.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
					
					gevents.setHappened( GameEvents.EVENT_RESERVE_SWAPPED ) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_SWAPPED ) ;
					
					// Tell the piece history.  We swapped in-place the falling piece and the reserve.
					pieceHistory.setDoneWithCurrentPiece() ;
					pieceHistory.setPiecePushedIntoBag(PieceHistory.RESERVE_BAG) ;
					pieceHistory.setPiecePopped(s.nextPieces[0].type, PieceHistory.RESERVE_BAG) ;
					pieceHistory.setPiecePushedIntoBag(PieceHistory.PIECE_BAG) ;
					pieceHistory.setPieceHeldInBag(s.nextPieces[0].type, PieceHistory.PIECE_BAG) ;
					pieceHistory.setPieceHeldInBag(s.reservePieces[0].type, PieceHistory.RESERVE_BAG) ;
					
					s.usedReserve = true ;
					adapter.game_didUseReserve(moveDirection) ;
					
					// Trigger first, then score.  Remember that the trigger might affect our score!
					trs.didPutInReserve(s.reservePieces[0]) ;
					trs.didInsert(s.nextPieces[0]) ;
					ss.scorePutInReserve(s.reservePieces[0]) ;
					ss.scoreUseReserve(s.nextPieces[0]) ;
					
					// preparePiece
					preparePiece() ;
					// this call sets the current state to ENTERING.
				}
			}
		}
		
		else if ( rb == GameModes.RESERVE_BEHAVIOR_INSERT_REENTER || ( rb == GameModes.RESERVE_BEHAVIOR_SPECIAL && sps.specialIsReserveInsert() ) ) {
			// inserts the top reserve piece as a replacement for the current one.
			// The current piece moves to the 'next' piece, and the top reserve
			// begins falling from the top.
			//
			// Unlike standard 'swap' behavior, where we maintain the current
			// position, the new piece will 're-enter' from the top following standard
			// prepare -> enter behavior.  Among other things, this means the
			// new piece starts at the top of the screen, fall-timing resets,
			// etc.
			
			if ( s.state != STATE_FALLING ) {
				throw new IllegalStateException("INSERT_REENTER is only supported for STATE_FALLING!") ;
			}
			else {
				
				boolean isSpecial = rb == GameModes.RESERVE_BEHAVIOR_SPECIAL ;
				
				// this behavior is fairly simple compared to INSERT.  INSERT would
				// fail if the piece didn't fit or couldn't be 'kicked' in place,
				// whereas INSERT_REENTER doesn't care about that (yes, that means you
				// can lose the game by pressing the 'reserve' button!).  We do the
				// following:  1, push the current piece on the next stack.
				// 2, push the 'swap' piece into 'next.'  3. pop the next reserve,
				// 4, 'preparePiece.',  5, set state to ENTERING.
				
				// First: push the current piece on the next stack.
				pieceBag.push( s.nextPieces[GameState.PIECE_LOOKAHEAD-1] ) ;
				for ( int i = GameState.PIECE_LOOKAHEAD -1; i > 0; i-- )
					s.nextPieces[i].takeVals(s.nextPieces[i-1]) ;
				s.nextPieces[0].takeDefaultVals(s.piece) ;
				rs.turn0(s.nextPieces[0]) ;
				
				// Next: push the reserve piece on the next stack.  The reserve
				pieceBag.push( s.nextPieces[GameState.PIECE_LOOKAHEAD-1] ) ;
				// or a true reserve.
				for ( int i = GameState.PIECE_LOOKAHEAD -1; i > 0; i-- )
					s.nextPieces[i].takeVals(s.nextPieces[i-1]) ;
				
				if ( isSpecial ) {
					s.nextPieces[0].type = sps.useSpecialAsReserveInsert() ;
					rs.turnMinimumHorizontalProfile(s.nextPieces[0]) ;
				}
				else {
					s.nextPieces[0].takeVals(s.reservePieces[0]) ;
				        rs.turn0(s.nextPieces[0]) ;
					
					// pop the next reserve!
					for ( int i = 0; i < GameState.PIECE_LOOKAHEAD-1; i++ )
						s.reservePieces[i].takeVals( s.reservePieces[i+1] ) ;
				reserveBag.pop( s.reservePieces[GameState.PIECE_LOOKAHEAD-1] );
				rs.turn0(s.reservePieces[GameState.PIECE_LOOKAHEAD-1]) ;
				}
				
				// Stub for triggering conditions, GameEvents
				gevents.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
				gevents.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
				
				gevents.setHappened( GameEvents.EVENT_RESERVE_INSERTED ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_INSERTED ) ;
				
				// We pushed the currently falling piece on top of next, and "popped" the
				// reserve into play.
				pieceHistory.setDoneWithCurrentPiece() ;
				pieceHistory.setPiecePushedIntoBag(PieceHistory.PIECE_BAG) ;
				pieceHistory.setPiecePopped(s.piece.type, PieceHistory.RESERVE_BAG) ;
				pieceHistory.setPiecePushedIntoBag(PieceHistory.PIECE_BAG) ;
				pieceHistory.setPieceHeldInBag(s.nextPieces[0].type, PieceHistory.PIECE_BAG) ;
				pieceHistory.setPieceHeldInBag(s.reservePieces[0].type, PieceHistory.RESERVE_BAG) ;
				
				s.usedReserve = true ;
				adapter.game_didUseReserve(moveDirection) ;
				
				trs.didPutPieceBack(s.nextPieces[1]) ;
				trs.didInsert(s.nextPieces[0]) ;
				ss.scorePutBack(s.nextPieces[1]) ;
				ss.scoreUseReserve(s.nextPieces[0]) ;
				
				// preparePiece
				preparePiece() ;
				// this call sets the current state to ENTERING.
			}
		}
		
		else if ( rb == GameModes.RESERVE_BEHAVIOR_SPECIAL && sps.specialIsAttack() ) {
			sps.useSpecialAsAttack(as) ;
			
			// whelp, that just happened.
			gevents.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_PIECE_CHANGED ) ;
			
			gevents.setHappened( GameEvents.EVENT_RESERVE_ATTACK ) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_RESERVE_ATTACK ) ;
			
			adapter.game_didIssueSpecialAttack(this) ;
		}
		
		if ( s.nextPieces[0].type > -1 )
			rs.turn0(s.nextPieces[0]) ; 
		if ( s.reservePieces[0].type > -1 )
			rs.turn0(s.reservePieces[0]) ;
	}
	
	/**
	 * Returns whether the game is in a state where using a reserve piece is OK.
	 */
	public synchronized boolean stateOK_useReserve() {
		return canUseReserve() ;
	}
	
	
	/**
	 * A "FakeTick" is an update call that should not make any changes to the
	 * underlying game.  Its main purpose is to prevent repeated button presses
	 * during a veiled or paused section from building up in an array.
	 */
	public synchronized void fakeTick() {
		adapter.game_fakeDequeueActions(this) ;
	}
	

	/**
	 * A tick advances the state of the game by a certain amount.
	 * We rely on GameEvents and the TimingSystem to tell us how far
	 * things should advance.  Upon exiting this function, either
	 * something significant has happened, or the timing system
	 * said to wait a while before doing something (probably entering
	 * or falling a piece).
	 * @param seconds
	 * @returns Whether the state of the game has been altered in any
	 * 			way by this call.  Note that advancing time is considered
	 * 			a change in game state, so this method will always return 'true'
	 * 			unless either adapter.game_shouldUseTimingSystem or
	 * 			adapter.game_shouldTick return false.
	 * @throws Exception 
	 */
	public synchronized boolean tick( double seconds ) throws Exception {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		if ( !stillPlaying() )
			adapter.game_fakeDequeueActions(this) ;
		
		boolean changed = false ;
		// Many calls below return a boolean indicating whether they had any
		// effect.  We make those calls in the form
		// changed = call() || changed ;
		
		boolean realtimeChange = false ;
		
		// Time has passed
		if ( stillPlaying() && adapter.game_shouldTick(this) ) {
			ginfo.passTime( Math.round(seconds * 1000) ) ;
			realtimeChange = true ;
		}
		
		// Maybe we need something different here?  We might want to disable this
		// for certain states, similarly to how we disable ticks under certain
		// circumstances.
		if ( stillPlaying() && ds.tick( seconds ) ) {
			realtimeChange = true ;
		}
		
		if ( realtimeChange ) {
			adapter.game_didChangeRealtime(
					this,
					ginfo.milliseconds,
					ds.getDisplacementSeconds(),
					ds.getDisplacedAndTransferredRows()) ;
		}
		
		// If we are going to iterate at all, reset our list of game events.
		if ( !gevents.significantEventHappened() && stillPlaying() ) {
			s.geventsLastTick.clearHappened() ;
		}
		
		// timing system?
		if ( adapter.game_shouldUseTimingSystem(this) && adapter.game_shouldTick(this)
				&& !gevents.significantEventHappened() && stillPlaying() ) {
			ts.tick( seconds ) ;
			changed = true ;
		}
		
		// While nothing significant has happened and we're still playing...
		while( !gevents.significantEventHappened() && stillPlaying() ) {
			
			// Dequeue some actions!
			changed = adapter.game_dequeueActions(this) || changed ;
			
			// STATE: Initializing.  First time.
			if ( s.state == STATE_INITIALIZING ) {
				// System.err.println("Game.tick() INITIALIZING with game_shouldUseTimingSystem " + adapter.game_shouldUseTimingSystem(this)) ;
				// TODO: some testing states.  Try them if you want.
				// makeTestBlackjack() ;
				// makeTestJackpot() ;
				// makeTestSuperCritical() ;
				/* if ( GameModes.numberQPanes(ginfo.mode) == 1 )
					makeScreencapRainbowBullets() ;
				else 
					makeScreencapQuantroBullets() ; */
				/*
				if ( GameModes.numberQPanes(ginfo.mode) == 1 ) {
					//makeTestStrategicGarbageRetro1() ;
					makeTestStrategicGarbageRetro2() ;
				} else {
					//makeTestStrategicGarbageQuantro1() ;
					//makeTestStrategicGarbageQuantro2() ;
					//makeTestStrategicGarbageQuantro3() ;
					makeTestStrategicGarbageQuantro4() ;
				}
				*/
				
				/*
				if ( GameModes.numberQPanes(ginfo.mode) == 1 )
					makeTestRetroAllGlows() ;
				else
					makeTestQuantroAllGlows() ;
					*/
				
				//makeTestFallingChunks() ;
				//makeTestPentoST() ;
				//makeTestPentoUL() ;
				//makeTestSpecialTrigger() ;
				//makeTestTurnLine() ;
				//makeTestTurnShake1() ;
				//makeTestTurnShake2() ;
				//makeTestTurnShake3() ;
				//makeTestTurnShake4() ;
				//makeTestLockClip() ;
				//makeTestSTFailure() ;
				//makeTestSTFailureAlt() ;
				//makeTestReserveInsertionKickFailure() ;
				//makeTestFallPieceComponents() ;
				//makeTestLockGlow() ;
				//makeTestComplexLockGlow() ;
				//makeScreencapLogo() ;
				//makeScreencapQuickSlide() ;	
				//makeScreencap180Turn() ;
				//makeScreencapKickTurn() ;
				//makeScreencapLeanTurn() ;
				//makeScreencapQuantroTypes() ;
				//makeScreencapQuantroTypesSpread() ;
				//makeScreencapRetroTypes() ;
				//makeScreencapMonochromeClear() ;
				//makeTestTightenedFallingChunk() ;
				//makeTestTightenedFallingChunk2() ;
				//makeTestQuantroGarbageClears() ;
				//makeTestRetroGarbageClears() ;
				//continue ;
				//makeTestEfficientSafelyFallPieces() ;
				//makeTestLeaningKick1() ;
				//makeTestLeaningKick2() ;
				//makeTestLeaningKick3() ;
				//makeTestLeaningKick4() ;
				//makeTestFallingChunks1() ;
				//makeTestFallingChunks2() ;
				//makeTestFallingChunks3() ;
				//makeTestFallingChunks4() ;
				//makeTestUnlockBug1() ;
				//makeTestUnlockBug2() ;
				//makeTestUnlockBug3() ;
				//this.makeTestST1() ;
				//this.makeTestST2() ;
				//this.makeTestST3() ;
				//this.makeTestST4() ;
				//this.makeTestST5() ;
				//this.makeTestST6() ;
				//this.makeTestST7();
				//this.makeTestST8() ;
				//this.makeTestSTLock() ;
				//makeTestSL0() ;
				//makeTestSL1() ;
				//makeTestSL2() ;
				//makeTestSL3() ;
				
				// if ( ginfo.mode == 2 )
					//makeTestQuantroGarbage0( 2 ) ;
					
				
				//if ( ginfo.mode == 0 || ginfo.mode == 4 )
					//makeTestHurdle0() ;
					//makeTestHurdle1() ;
					//makeTestImmobile0() ;
					//makeTestRetroGarbage0( 3 ) ;
					//makeTestRetroGarbage0(2) ;
					
				//this.makeTestUL1() ;
				//this.makeTestUL2() ;
				
				
				//this.makeTestFL0() ;
				//this.makeTestFL1() ;
				//this.makeTestClear1() ;
				//System.out.println("GAME synchronizing") ;
				
				if ( adapter != null )
					adapter.game_aboutToInitialize(this) ;
				if ( sps.initialize() ) {
					gevents.setHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED) ;
					s.geventsLastTick.setHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED) ;
					gevents.setHappened(GameEvents.EVENT_SPECIAL_UPGRADE) ;
					s.geventsLastTick.setHappened(GameEvents.EVENT_SPECIAL_UPGRADE) ;
				}
				
				// fill the displacement rows
				if ( ds.displaces() ) {
					ds.prefill(s.blockFieldDisplacementRows) ;
					gevents.setHappened(GameEvents.EVENT_DISPLACEMENT_PREFILLED) ;
					s.geventsLastTick.setHappened(GameEvents.EVENT_DISPLACEMENT_PREFILLED) ;
				}
				
				s.state = STATE_SYNCHRONIZING ;
				changed = true ;
				continue ;
				
			}
			
			// Attempt to synchronize.  If it fails, return; we will try again
			// next tick.  If it succeeds, we may want to proceed immediately.
			if ( s.state == STATE_SYNCHRONIZING ) {
				// System.err.println("Game.tick() SYNCHRONIZING with game_shouldUseTimingSystem " + adapter.game_shouldUseTimingSystem(this)) ;
				if ( synchronize() ) {
					/*
					// WHAT WHAT WHAAAT!!???
					if ( GameModes.numberQPanes(ginfo) == 1 ) {
						makeTestMassiveCascadeRetro() ;
					} else if ( GameModes.numberQPanes(ginfo) == 2 ) {
						makeTestMassiveCascadeQuantro() ;
					}
					*/
					
					/*
					if ( GameModes.numberQPanes(ginfo) == 1 && testingRandom.nextInt(3) == 0) {
						makeTestNormalFallingRetro() ;
					} else if ( GameModes.numberQPanes(ginfo) == 2 && testingRandom.nextInt(3) == 0 ) {
						makeTestNormalFallingQuantro() ;
					}
					*/
					
					
					
					changed = true ;
					//System.out.println("GAME synchronized") ;
					continue ;
				}
				else {
					//System.out.println("GAME not synchronized") ;
					return changed ;
				}
			}
			
			// STATE: Preparing.  Before the piece is prepared, we allow
			// garbage rows and attack blocks to be added to the screen.
			if ( s.state == STATE_PREPARING && as.hasDropBlocks() ) {
				s.numBlocksForValleys += as.unleashDropBlocksInValleys() ;
				s.numBlocksForJunctions += as.unleashDropBlocksOnJunctions() ;
				s.numBlocksForPeaks += as.unleashDropBlocksOnPeaks() ;
				s.numBlocksForCorners += as.unleashDropBlocksOnCorners() ;
				s.numBlocksForTroll += as.unleashDropBlocksTroll() ;
				
				gevents.setHappened(GameEvents.EVENT_ATTACK_DROP_BLOCKS) ;
				s.geventsLastTick.setHappened(GameEvents.EVENT_ATTACK_DROP_BLOCKS) ;
				
				// We start a progression to drop all the pieces from above.
				s.stateAfterProgression = STATE_PREPARING ;
				s.state = STATE_PROGRESSION ;
				s.progressionState = PROGRESSION_CHUNKS_UNLOCK ;
				
				if ( as.hasOutgoingAttacks() )
					adapter.game_hasOutgoingAttack(this) ;
				
				changed = true ;
				continue ;
			}
			
			// adding rows: attack system garbage / push, displacement rows transferred into play.
			if ( s.state == STATE_PREPARING && ( as.hasGarbageRows() || as.hasPushRows() ) ) {
				// We push rows and unleash garbage as one step.  This is important so
				// we don't animate a double row push.
				ArrayOps.copyInto(s.blockField, s.blockFieldBefore) ;
				s.numberOfRowsPushedUp = 0 ;
				s.numberOfRowsPushedDown = -1 ;
				s.numberOfRowsPushedUpThatAreGarbage = 0 ;
				s.numberOfDisplacedRowsTransferred = 0 ;
				
				boolean pushed = false ;
				
				if ( as.hasPushRows() ) {
					// System.err.println("Has push rows... " + as.numPushDown() + " down, " + as.numPushUp() + " up") ;
					// this is 'net change.'  It will always be nonnegative, and
					// furthermore, at least one will be zero.  This is equivalent
					// to dropping by numberOfRowsPushedDown and then raising by
					// numberOfRowsPushedUp -- NOT raising TO that watermark, but
					// raising BY that amount after dropping.
					
					// If numPushDown is 0, we want to maintain our -1 setting.
					if ( as.numPushDown() > 0 )
						s.numberOfRowsPushedDown = as.numPushDownNet() ;
					s.numberOfRowsPushedUp = as.numPushUpNet() ;
					
					as.unleashPushRows(s.blockField) ;
					pushed = true ;
				}
				
				if ( as.hasGarbageRows() ) {
					// Garbage rows push what's currently present up even further.
					// System.err.println("Unleashing garbage...") ;
					s.numberOfRowsPushedUpThatAreGarbage = as.unleashGarbageRows(s.blockField) ;
					s.numberOfRowsPushedUp += s.numberOfRowsPushedUpThatAreGarbage ;
					pushed = true ;
				}
				
				if ( pushed ) {
					gevents.setHappened(GameEvents.EVENT_ATTACK_PUSH_ROWS) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_ATTACK_PUSH_ROWS ) ;
					
					if ( as.hasOutgoingAttacks() )
						adapter.game_hasOutgoingAttack(this) ;
				}
				

				// We start a progression to drop all the pieces from above.
				s.stateAfterProgression = STATE_PREPARING ;
				s.state = STATE_PROGRESSION ;
				s.progressionState = PROGRESSION_CHUNKS_UNLOCK ;
				
				changed = true ;
				continue ;
			}
			
			if ( s.state == STATE_PREPARING && s.numberOfDisplacedRowsToTransferThisCycle > 0 ) {
				ArrayOps.copyInto(s.blockField, s.blockFieldBefore) ;
				s.numberOfRowsPushedUp = 0 ;
				s.numberOfRowsPushedDown = -1 ;
				s.numberOfRowsPushedUpThatAreGarbage = 0 ;
				s.numberOfDisplacedRowsTransferred = 0 ;
				
				ds.transferDisplacedRows(
						s.blockField,
						s.blockFieldDisplacementRows,
						s.numberOfDisplacedRowsToTransferThisCycle) ;
				s.numberOfDisplacedRowsTransferred = s.numberOfDisplacedRowsToTransferThisCycle ;
				s.numberOfDisplacedRowsToTransferThisCycle = 0 ;
				
				// whelp...
				gevents.setHappened( GameEvents.EVENT_DISPLACEMENT_TRANSFERRED ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_DISPLACEMENT_TRANSFERRED ) ;
				
				// We start a progression to drop all the pieces from above.
				s.stateAfterProgression = STATE_PREPARING ;
				s.state = STATE_PROGRESSION ;
				s.progressionState = PROGRESSION_CHUNKS_UNLOCK ;
				
				changed = true ;
				continue ;
			}
			
			if ( s.state == STATE_PREPARING && as.hasDisplaceRows() ) {
				as.unleashDisplacementRows(ds) ;
				
				gevents.setHappened( GameEvents.EVENT_ATTACK_DISPLACEMENT_ACCEL ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_ATTACK_DISPLACEMENT_ACCEL ) ;
				changed = true ;
				continue ;
			}
			
			// Attack System level-up
			if ( s.state == STATE_PREPARING && as.hasLevelChange() ) {
				// System.err.println("Game.tick() PREPARING LEVEL CHANGE with game_shouldUseTimingSystem " + adapter.game_shouldUseTimingSystem(this)) ;
				// TODO: consider whether these adapter and gevents updates are appropriate.
				int numLevelUps = as.numLevelChange() ;
				adapter.game_aboutToLevelUp() ;
				as.aboutToLevelUp( numLevelUps ) ;
				int num = as.unleashLevelChange() ;
				if ( num > 0 ) {
					gevents.setHappened(GameEvents.EVENT_GAME_LEVEL_UP) ;
					s.geventsLastTick.setHappened( GameEvents.EVENT_GAME_LEVEL_UP ) ;
					adapter.game_didLevelUp() ;
					changed = true ;
				}
				
				if ( as.hasOutgoingAttacks() )
					adapter.game_hasOutgoingAttack(this) ;
				
				// TODO: notify trigger system, score system, etc. that we leveled-up.
				continue ;
			}
			
			// STATE: Preparing.  Make next piece the current piece.
			if ( s.state == STATE_PREPARING ) {
				//System.err.println("Game.tick() PREPARING with game_shouldUseTimingSystem " + adapter.game_shouldUseTimingSystem(this)) ;
				//System.out.println("GAME preparing") ;
				// If we are in this state, we always prepare.
				// Because we dequeue attacks in the preparePiece(), 
				// it is important that there never be a delay between synchronizing
				// and preparing.
				preparePiece() ;
				changed = true ;
				continue ;
			}
			
			// STATE: Entering.  Check if it's time to enter the piece yet.
			if ( s.state == STATE_ENTERING ) {
				// System.err.println("Game.tick() ENTERING with game_shouldUseTimingSystem " + adapter.game_shouldUseTimingSystem(this)) ;
				//System.out.println("GAME entering") ;
				if ( (!adapter.game_shouldUseTimingSystem(this) || ts.canEnter(s.piece, s.offset) ) ) {
					enterPiece() ;
					
					// try using reserve!
					if ( s.reserveQueuedForNextCycle ) {
						s.reserveQueuedForNextCycle = false ;
						if ( canUseReserve() ) {
							gevents.setHappened(GameEvents.EVENT_RESERVE_DEQUEUED) ;
							s.geventsLastTick.setHappened(GameEvents.EVENT_RESERVE_DEQUEUED) ;
							this.useReserve(LEAN_NONE) ;
							if ( s.state == STATE_ENTERING )
								enterPiece() ;
						}
					}
					
					changed = true ;
					continue ;
				}
				else {		// We're waiting to enter a piece, but it isn't time yet.
					return changed ;
				}
			}
			
			// STATE: Falling.  Check if it's time to lock.
			if ( s.state == STATE_FALLING ) {
				// System.err.println("Game.tick() FALLING with game_shouldUseTimingSystem " + adapter.game_shouldUseTimingSystem(this)) ;
				//System.out.println("GAME falling") ;
				// Maybe it's time to fall.  However, if we don't fall,
				// we may still do something else significant (like lock!)
				// Important to note: we NEVER fall, and NEVER lock, unless the timing system says so,
				// or we get a specific action declaring it.
				// This is distinct from most other actions, which we ALWAYS do
				// if the timing system isn't listened to.
				if ( (adapter.game_shouldUseTimingSystem(this) && ts.canFall()) && !ls.shouldLock(s.blockField, s.piece, s.offset) ) {
					fall() ;
					changed = true ;
					continue ;
				}
				else if ( adapter.game_shouldUseTimingSystem(this) && ts.canLock(s.blockField, s.piece, s.offset)
						&& ls.shouldLock(s.blockField, s.piece, s.offset) ) {
					lockPiece() ;
					changed = true ;
					continue ;
				}
				else {	// It's falling... but we can't fall or lock.
					return changed ;
				}
			}
			
			// STATE: Progression.
			if ( s.state == STATE_PROGRESSION ) {
				// System.err.println("Game.tick() PROGRESSION with game_shouldUseTimingSystem " + adapter.game_shouldUseTimingSystem(this)) ;
				//System.out.println("GAME progression") ;
				// Depending on how far along we are, perform the appropriate action.
				if ( s.progressionState == PROGRESSION_COMPONENTS_UNLOCK ) {
					// Do this thing.
					unlockComponents() ;
					changed = true ;
					continue ;
				}
				else if ( s.progressionState == PROGRESSION_COMPONENTS_FALL ) {
					fallComponents() ;
					changed = true ;
					continue ;
				}
				else if ( s.progressionState == PROGRESSION_COMPONENTS_LOCK ) {
					lockComponents() ;
					changed = true ;
					continue ;
				}
				else if ( s.progressionState == PROGRESSION_TRIGGERED_METAMORPHOSIS ) {
					triggeredMetamorphosis() ;
					changed = true ;
					continue ;
				}
				else if ( s.progressionState == PROGRESSION_CLEAR ) {
					// Clear is the only progression state tied to a timer...
					if ( (!adapter.game_shouldUseTimingSystem(this) || ts.canClear() ) ) {
						clear() ;
						changed = true ;
						continue ;
					}
					else {	// Wait for some time to pass...
						return changed ;
					}
				}
				else if ( s.progressionState == PROGRESSION_CHUNKS_UNLOCK ) {
					unlockChunks() ;
					changed = true ;
					continue ;
				}
				else if ( s.progressionState == PROGRESSION_CHUNKS_FALL ) {
					fallChunks() ;
					changed = true ;
					continue ;
				}
				else if ( s.progressionState == PROGRESSION_CHUNKS_LOCK ) {
					lockChunks() ;
					changed = true ;
					continue ;
				}
				else {
					// In a progression, but progression state not set??!
					throw new Exception("tick: state indicates progression, but progressionState is not valid") ;
				}
			}
			
			// GAIN A LEVEL!  HOOOO BOY!  I CAN'T WAIT!
			if ( s.state == STATE_ENDING_CYCLE && lvs.shouldGainLevel() ) {
				// System.err.println("Game.tick() ENDING LEVEL UP with game_shouldUseTimingSystem " + adapter.game_shouldUseTimingSystem(this)) ;
				// System.err.println(" level is  " + ginfo.level + " should gain level is " + lvs.shouldGainLevel()) ;
				adapter.game_aboutToLevelUp() ;
				as.aboutToLevelUp( 1 ) ;
				ginfo.levelUp() ;
				lvs.didGainLevel() ;
				gevents.setHappened(GameEvents.EVENT_GAME_LEVEL_UP) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_GAME_LEVEL_UP ) ;
				adapter.game_didLevelUp() ;
				changed = true ;
				
				if ( as.hasOutgoingAttacks() )
					adapter.game_hasOutgoingAttack(this) ;
				
				// TODO: notify trigger system, score system, etc. that we leveled-up.
				continue ;
			}
			
			if ( s.state == STATE_ENDING_CYCLE && ( s.activateThenEndCycle || s.deactivateThenEndCycle ) ) {
				if ( s.activateThenEndCycle ) {
					ArrayOps.copyInto(s.blockField, s.blockFieldBefore) ;
					if ( ms.metamorphosize( MetamorphosisSystem.METAMORPHOSIS_ACTIVATE, s.blockField) ) {
						gevents.setHappened( GameEvents.EVENT_METAMORPHOSIS ) ;
						s.geventsLastTick.setHappened( GameEvents.EVENT_METAMORPHOSIS ) ;
						gevents.setHappened( GameEvents.EVENT_METAMORPHOSIS_DID_ACTIVATE ) ;
						s.geventsLastTick.setHappened( GameEvents.EVENT_METAMORPHOSIS_DID_ACTIVATE ) ;
						
						trs.didMetamorphis(s.blockFieldBefore, s.blockField) ;
						
						as.metamorphosis(s.blockField, s.blockFieldBefore) ;
						
						s.activateThenEndCycle = false ;
						
						if ( as.hasOutgoingAttacks() )
							adapter.game_hasOutgoingAttack(this) ;
						
						continue ;
					}
				}
				
				if ( s.deactivateThenEndCycle ) {
					ArrayOps.copyInto(s.blockField, s.blockFieldBefore) ;
					if ( ms.metamorphosize( MetamorphosisSystem.METAMORPHOSIS_DEACTIVATE, s.blockField) ) {
						gevents.setHappened( GameEvents.EVENT_METAMORPHOSIS ) ;
						s.geventsLastTick.setHappened( GameEvents.EVENT_METAMORPHOSIS ) ;
						gevents.setHappened( GameEvents.EVENT_METAMORPHOSIS_DID_DEACTIVATE ) ;
						s.geventsLastTick.setHappened( GameEvents.EVENT_METAMORPHOSIS_DID_DEACTIVATE ) ;
						
						trs.didMetamorphis(s.blockFieldBefore, s.blockField) ;
						
						as.metamorphosis(s.blockField, s.blockFieldBefore) ;
						
						s.deactivateThenEndCycle = false ;
						
						if ( as.hasOutgoingAttacks() )
							adapter.game_hasOutgoingAttack(this) ;
						
						continue ;
					}
				}
			}
			
			if ( s.state == STATE_ENDING_CYCLE ) {
				// System.err.println("Game.tick() ENDING with game_shouldUseTimingSystem " + adapter.game_shouldUseTimingSystem(this)) ;
				//System.out.println("GAME ending cycle") ;
				endCycle() ;
				changed = true ;
				s.state = STATE_SYNCHRONIZING ;
				continue ;
			}
		}
		
		return changed ;
		
	}
	
	/*
	 ***************************************************************************
	 * STATE QUERY FUNCTIONS
	 *
	 * Although the current game "state" is available, it may not be clear
	 * whether the state allows for certain actions.  For instance, the
	 * reserve piece may be used in some states but not others.  These functions
	 * provide a means to query Game capabilities at the given moment.
	 * 
	 ***************************************************************************
	 */
	
	/**
	 * Returns whether the reserve can currently be used.  This is a strict check:
	 * if it returns 'true', the reserve can currently be used at this very moment.
	 * 
	 * At the very least, that requires that a piece is currently falling.
	 */
	public synchronized boolean canUseReserve() {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// We refuse to honor attempts to use the reserve if they
		// occur when the piece is not falling.
		
		if ( s.usedReserve || s.reserveQueuedForNextCycle || s.state == STATE_INITIALIZING ) {
			//System.out.println("Game cant use reserve") ;
			return false ;
		}
		
		if ( GameModes.reserveBehavior(ginfo) == GameModes.RESERVE_BEHAVIOR_SPECIAL )
			return sps.hasSpecial() ;
		
		// For now, we can use the reserve at any time.
		//System.out.println("Game can!! use reserve") ;
		return true ;
	}
	
	/**
	 * A more lenient alternative to 'canUseReserve'.  It asks the question:
	 * when the next piece is falling, could we use the reserve?
	 * 
	 * One possible usage: a visual indication of whether the reserve is available.
	 * We don't want something that flashes one and off every time a piece
	 * lands.  Using this method instead will temporarily disable the vis.
	 * indication each time the reserve is used, then bring it back until the
	 * next time, ignoring future state changes.
	 * 
	 * @return
	 */
	public synchronized boolean couldUseReserveWhenPieceFalling() {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// 
		if ( s.usedReserve && ( s.state == STATE_ENTERING || s.state == STATE_FALLING ) )
			return false ;
		if ( s.reserveQueuedForNextCycle )
			return false ;
		if ( s.state == STATE_INITIALIZING )
			return false ;
		if ( GameModes.reserveBehavior(ginfo) == GameModes.RESERVE_BEHAVIOR_SPECIAL )
			return sps.hasSpecial() ;
		
		return true ;
	}
	
	
	
	public synchronized boolean stillPlaying() {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		return !( hasWon() || hasLost() ) ;
	}
	
	public synchronized boolean hasWon() {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// STUB
		return false ;
	}
	
	public synchronized boolean hasLost() {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// STUB
		return s.game_period == PERIOD_GAME_LOST ;
	}
	
	public synchronized int getCurrentPieceType() {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		return s.piece.type ;
	}
	
	// to reduce in-game allocation
	private Offset copySimplifiedBlockField_offset = new Offset() ;
	/**
	 * Using the current game state, copies, as best it can, the
	 * current game block field into the single buffer provided.
	 * 
	 * This necessarily implies that certain information will be lost;
	 * i.e., there is no distinction between falling chunks and stable
	 * ones, a falling piece controlled by the player and a chunk which
	 * is not, pieces that are locked and those not, etc.  However,
	 * this method provides a single, simplified representation that
	 * is appropriate for rendering as a thumbnail, or some other view which
	 * does not require (or support...) animation and other fanciness.
	 * 
	 * @param bf_blockField
	 * @param rowOffset
	 * @param colOffset
	 */
	private synchronized void copySimplifiedBlockField(
			LooselyBoundedArray lba, int rowOffset, int colOffset ) {
		
		// Our method for constructing this representation depends
		// on the current game state.  Here is our canonical simplified
		// representation:
		
		// Preparing / entering: show current block field.
		// Falling: show current block field with piece locked in place.
		// Falling chunks or components: show everything at PRE-FALL positions.
		// Clearing: show PRE-CLEAR blockfield.
		
		// Why, for falling chunks and clearing, do we show the "before" state?
		// Because this allows even a thumbnail to "animate."  By showing
		// falling chunks at pre-fall positions, we show them hovering over
		// empty space.  After they fall, we will either Clear or Prepare;
		// in either case, the next thing displayed is the board where
		// those pieces have landed (note that "clearing" will show them in
		// place, while the next state will show "after clear", so every
		// intermediate blockField setup has a chance for display.
		
		// Bounding: we copy directly into this array; our assumption is that this
		// overwrites every value.  Bound the entire thing.
		lba.boundAll() ;
		byte [][][] bf_blockField = (byte [][][]) lba.array() ;
		
		// In almost all cases, we copy the blockfield.
		if ( s.state != Game.STATE_PROGRESSION || s.progressionState != Game.PROGRESSION_CLEAR ) {
			ArrayOps.copyInto(s.blockField, bf_blockField, rowOffset, colOffset) ;
		}
		else {
			// When clearing, though, show the pre-clear.
			ArrayOps.copyInto(s.blockFieldBefore, bf_blockField, rowOffset, colOffset) ;
		}
		
		// In some cases, pieces or chunks are currently falling.  Draw them
		// at their top position (for pieces, draw where they now are.  For
		// chunks/components, draw at pre-fall height).
		if ( s.state == Game.STATE_FALLING || s.state == Game.STATE_ENTERING ) {
			try {
				copySimplifiedBlockField_offset.takeVals(s.offset) ;
				copySimplifiedBlockField_offset.x += colOffset ;
				copySimplifiedBlockField_offset.y += rowOffset ;
				ls.lock(bf_blockField, s.piece, copySimplifiedBlockField_offset) ;
 			}
			catch( Exception e ) {
				// do nothing.  We just don't want this method, which should
				// only transfer information, to fail!
			}
		}
		
		if ( s.state == Game.STATE_PROGRESSION
				&& ( s.progressionState == Game.PROGRESSION_CHUNKS_FALL 
						|| s.progressionState == Game.PROGRESSION_CHUNKS_LOCK
						|| s.progressionState == Game.PROGRESSION_CHUNKS_UNLOCK ) ) {
			// Lock the chunks at "top" position.
			for ( int i = 0; i < s.numChunks; i++ ) {
				try {
					copySimplifiedBlockField_offset.takeVals(s.chunkOriginalOffsets.get(i)) ;
					copySimplifiedBlockField_offset.x += colOffset ;
					copySimplifiedBlockField_offset.y += rowOffset ;
					ls.lock(bf_blockField, s.chunks.get(i), copySimplifiedBlockField_offset) ;
				} catch( Exception e ) {
					// do nothing
				}
			}
		}
		
		if ( s.state == Game.STATE_PROGRESSION
				&& ( s.progressionState == Game.PROGRESSION_COMPONENTS_FALL 
						|| s.progressionState == Game.PROGRESSION_COMPONENTS_LOCK
						|| s.progressionState == Game.PROGRESSION_COMPONENTS_UNLOCK ) ) {
			// Lock the chunks at "top" position.
			try {
				for ( int i = 0; i < s.numComponents; i++ ) {
					copySimplifiedBlockField_offset.takeVals( s.componentOriginalOffsets.get(i) ) ;
					copySimplifiedBlockField_offset.x += colOffset ;
					copySimplifiedBlockField_offset.y += rowOffset ;
					ls.lock(bf_blockField, s.components.get(i), copySimplifiedBlockField_offset) ;
				}
			}
			catch( Exception e ) {
				// do nothing.
			}
		}
		
		// That's all.
		
	}
	
	
	/**
	 * Using the current game state, copies, as best it can, the
	 * current game block field into the two buffers provided.
	 * 
	 * This necessarily implies that certain information will be lost;
	 * i.e., there is no distinction between falling chunks and stable
	 * ones, pieces that are locked and those not, etc.  However,
	 * this method provides a simplified representation that
	 * is appropriate for rendering as a thumbnail, or some other view which
	 * does not require (or support...) animation and other fanciness.
	 * 
	 * The main distinction between the simplier prototype of
	 * copySimplifiedBlockField, and this one, is that falling piece blocks
	 * are copied into bf_piece rather than bf_blockField rather than
	 * 
	 * 
	 * @param bf_blockField
	 * @param rowOffset
	 * @param colOffset
	 * @return Whether there is a piece currently in bf_piece.
	 */
	public synchronized boolean copySimplifiedBlockField(
			byte [][][] bf_blockField, byte [][][] bf_piece, int rowOffset, int colOffset ) {
		
		// Our method for constructing this representation depends
		// on the current game state.  Here is our canonical simplified
		// representation:
		
		// Preparing / entering: show current block field.
		// Falling: show current block field with piece locked in place.
		// Falling chunks or components: show everything at PRE-FALL positions.
		// Clearing: show PRE-CLEAR blockfield.
		
		// Why, for falling chunks and clearing, do we show the "before" state?
		// Because this allows even a thumbnail to "animate."  By showing
		// falling chunks at pre-fall positions, we show them hovering over
		// empty space.  After they fall, we will either Clear or Prepare;
		// in either case, the next thing displayed is the board where
		// those pieces have landed (note that "clearing" will show them in
		// place, while the next state will show "after clear", so every
		// intermediate blockField setup has a chance for display.
		
		// In almost all cases, we copy the blockfield.
		if ( s.state != Game.STATE_PROGRESSION || s.progressionState != Game.PROGRESSION_CLEAR ) {
			ArrayOps.copyInto(s.blockField, bf_blockField, rowOffset, colOffset) ;
		}
		else {
			// When clearing, though, show the pre-clear.
			ArrayOps.copyInto(s.blockFieldBefore, bf_blockField, rowOffset, colOffset) ;
		}
		
		ArrayOps.setEmpty(bf_piece) ;
		boolean hasPiece = false ;
		
		Offset o = new Offset() ;
		
		// In some cases, pieces or chunks are currently falling.  Draw them
		// at their top position (for pieces, draw where they now are.  For
		// chunks/components, draw at pre-fall height).
		if ( s.state == Game.STATE_FALLING || s.state == Game.STATE_ENTERING
				|| (s.state == Game.STATE_PROGRESSION && s.progressionState == Game.PROGRESSION_COMPONENTS_UNLOCK ) ) {
			try {
				o.takeVals(s.offset) ;
				o.x += colOffset ;
				o.y += rowOffset ;
				ls.lock(bf_piece, s.piece, o) ;
				hasPiece = true ;
 			}
			catch( Exception e ) {
				// do nothing.  We just don't want this method, which should
				// only transfer information, to fail!
			}
		}
		
		if ( s.state == Game.STATE_PROGRESSION
				&& ( s.progressionState == Game.PROGRESSION_CHUNKS_FALL 
						|| s.progressionState == Game.PROGRESSION_CHUNKS_LOCK ) ) {
			// Do nothing here for "chunk unlock": note that if we are in PROGRESSION_STATE_CHUNK_UNLOCK,
			// it is assumed that those chunks are NOT yet unlocked (and, in fact, the number
			// of chunks will be set to zero upon unlocking, and those any chunks currently
			// there are from a previous PROGRESSION).
			// Lock the chunks at "top" position.
			try {
				for ( int i = 0; i < s.numChunks; i++ ) {
					o.takeVals( s.chunkOriginalOffsets.get(i) ) ;
					o.x += colOffset ;
					o.y += rowOffset ;
					ls.lock(bf_blockField, s.chunks.get(i), o) ;
				}
			}
			catch( Exception e ) {
				// do nothing.
			}
		}
		
		if ( s.state == Game.STATE_PROGRESSION
				&& ( s.progressionState == Game.PROGRESSION_COMPONENTS_FALL 
						|| s.progressionState == Game.PROGRESSION_COMPONENTS_LOCK ) ) {
			// Do nothing for "components unlock."  We have already handled this case
			// by locking the piece into bf_piece.
			// Lock the chunks at "top" position.
			try {
				for ( int i = 0; i < s.numComponents; i++ ) {
					o.takeVals( s.componentOriginalOffsets.get(i) ) ;
					o.x += colOffset ;
					o.y += rowOffset ;
					ls.lock(bf_blockField, s.components.get(i), o) ;
				}
			}
			catch( Exception e ) {
				// do nothing.
			}
		}
		
		// That's all.
		return hasPiece ;
	}
	
	
	public synchronized void copyDisplacementBlocks( GameBlocksSlice slice ) {
		LooselyBoundedArray lba = slice.getDisplacementBlockfieldLBA() ;
		
		lba.boundAll() ;
		ArrayOps.copyInto(
				s.blockFieldDisplacementRows,
				((byte [][][] )lba.array()),
				slice.getEdge(), slice.getEdge()) ;
		slice.setDisplacement(ds.getDisplacedRows()) ;
	}
	
	
	public synchronized void updateDisplacement( GameBlocksSlice slice ) {
		slice.setDisplacement(ds.getDisplacedRows()) ;
	}
	
	public synchronized void copyGameBlocksSlice( GameBlocksSlice slice ) {
		try {
			// depending on recent events, attempts to copy the slice.
			
			// A piece falling?
			if ( s.state == Game.STATE_FALLING || s.geventsLastTick.getHappened(GameEvents.EVENT_PIECE_PREPARED) ) {
				
				// copy piece and ghost components!
				slice.setBlocksState(GameBlocksSlice.BLOCKS_PIECE_FALLING) ;
				
				// we use 1 for the first ghost component.
				int num = copyFallingPieceBlockFields(
						slice.getPieceFallingBlockfieldLBA(),
						slice.getPieceFallingPieceLBA(),
						slice.getPieceFallingGhostsLBA(), 1,
						slice.getEdge(), slice.getEdge()) ;
				slice.setFirstPieceGhost(1) ;
				slice.setNumPieceGhosts(num) ;
				slice.setPieceType(s.piece.type) ;
				
				copySimplifiedBlockField(
						slice.getBlockfieldStableLBA(),
						slice.getEdge(), slice.getEdge()) ;
				
				return ;
			}
			
			if ( s.geventsLastTick.getHappened(GameEvents.EVENT_COMPONENTS_FELL)
					&& gevents.getSignificance(GameEvents.EVENT_COMPONENTS_FELL) ) {
				
				slice.setBlocksState(GameBlocksSlice.BLOCKS_CHUNKS_FALLING) ;
				
				// our default behavior uses 1 for the first piece component;
				// '0' represents an empty component.  's cool.
				int num = copyFallingComponents(
						slice.getChunksFallingPreBlockfieldLBA(),
						slice.getBlockfieldsLBA(),
						slice.getChunksLBA(),
						slice.getChunksAggregatedLBA(),
						slice.getFallDistances(),
						slice.getNewToBlockField(),
						slice.getEdge(), slice.getEdge() ) ;
				slice.setFirstChunk(0) ;
				slice.setNumChunks(num) ;
				slice.setChunksArePieceComponents(true) ;
				slice.setIncludesAggregatedChunks(true) ;
				if ( s.piece != null && s.piece.type >= 0 )
					slice.setPieceType(s.piece.type) ;
				
				slice.setNumUnlockedColumns(0) ;
				
				return ;
			}
			
			if ( s.geventsLastTick.getHappened(GameEvents.EVENT_CHUNKS_FELL) 
					&& gevents.getSignificance(GameEvents.EVENT_CHUNKS_FELL)) {
				
				slice.setBlocksState(GameBlocksSlice.BLOCKS_CHUNKS_FALLING) ;
				
				// our default behavior uses 1 for the first piece component;
				// '0' represents an empty component.  's cool.
				int num = copyFallingChunks(
						slice.getChunksFallingPreBlockfieldLBA(),
						slice.getBlockfieldsLBA(),
						slice.getChunksLBA(),
						slice.getChunksAggregatedLBA(),
						slice.getFallDistances(),
						slice.getNewToBlockField(),
						slice.getEdge(), slice.getEdge() ) ;
				slice.setFirstChunk(0) ;
				slice.setNumChunks(num) ;
				slice.setChunksArePieceComponents(false) ;
				slice.setIncludesAggregatedChunks(true) ;
				if ( s.piece != null && s.piece.type >= 0 )
					slice.setPieceType(s.piece.type) ;
				
				num = copyUnlockedColumnOffsets(slice.getUnlockedColumns()) ;
				slice.setNumUnlockedColumns(num) ;
				
				return ;
			}
			
			if ( s.geventsLastTick.getHappened(GameEvents.EVENT_CLEAR)
					&& gevents.getSignificance(GameEvents.EVENT_CLEAR) ) {
				
				slice.setBlocksState(GameBlocksSlice.BLOCKS_ROWS_CLEARING) ;
				
				copyClearedBlockfieldsAndRows(
						slice.getClearPreBlockfieldLBA(),
						slice.getClearPostBlockfieldLBA(),
						slice.getClearClearedBlocksLBA(),
						slice.getClears(),
						slice.getMonochromeClears(),
						slice.getEdge(), slice.getEdge() ) ;
				slice.setClearCascadeNumber(s.clearCascadeNumber) ;
				if ( s.piece != null && s.piece.type >= 0 )
					slice.setPieceType(s.piece.type) ;
				
				return ;
			}
			
			if ( ( s.geventsLastTick.getHappened(GameEvents.EVENT_ATTACK_PUSH_ROWS)
						&& gevents.getSignificance(GameEvents.EVENT_ATTACK_PUSH_ROWS) ) ||
					( s.geventsLastTick.getHappened(GameEvents.EVENT_DISPLACEMENT_TRANSFERRED)
							&& gevents.getSignificance(GameEvents.EVENT_DISPLACEMENT_TRANSFERRED) ) ) {
				
				slice.setBlocksState(GameBlocksSlice.BLOCKS_PUSHING_ROWS) ;
				
				copyPushingRowsBlockfields(
						slice.getPushingRowsPreBlockfieldLBA(),
						slice.getPushingRowsPostBlockfieldLBA(),
						slice.getEdge(), slice.getEdge() ) ;
				
				if ( s.numberOfRowsPushedDown < 0 )
					slice.setNumRowsPushedUp( s.numberOfRowsPushedUp ) ;
				else
					slice.setNumRowsPushed( s.numberOfRowsPushedDown, s.numberOfRowsPushedUp ) ;
				
				slice.setNumPushedRowsThatAreGarbage(s.numberOfRowsPushedUpThatAreGarbage) ;
				
				slice.setNumRowsTransferredIn( s.numberOfDisplacedRowsTransferred ) ;
				
				if ( s.piece != null && s.piece.type >= 0 )
					slice.setPieceType(s.piece.type) ;
				
				return ;
			}
			
			if ( s.geventsLastTick.getHappened(GameEvents.EVENT_METAMORPHOSIS)
					&& gevents.getSignificance(GameEvents.EVENT_METAMORPHOSIS) ) {
				
				slice.setBlocksState(GameBlocksSlice.BLOCKS_METAMORPHOSIZING) ;
				
				copyMetamorphosisBlockfields(
						slice.getMetamorphosisPreBlockfieldLBA(),
						slice.getMetamorphosisPostBlockfieldLBA(),
						slice.getEdge(), slice.getEdge() ) ;
				if ( s.piece != null && s.piece.type >= 0 )
					slice.setPieceType(s.piece.type) ;
				slice.setMetamorphosisPreIncludesNewBlocks(false) ;	
				
				return ;
			}
			
			if ( s.geventsLastTick.getHappened(GameEvents.EVENT_PIECE_LOCK_THEN_METAMORPHOSIS )
					&& gevents.getSignificance(GameEvents.EVENT_PIECE_LOCK_THEN_METAMORPHOSIS)) {
				
				slice.setBlocksState(GameBlocksSlice.BLOCKS_METAMORPHOSIZING) ;
				
				copyMetamorphosisBlockfields(
						slice.getMetamorphosisPreBlockfieldLBA(),
						slice.getMetamorphosisPostBlockfieldLBA(),
						slice.getEdge(), slice.getEdge() ) ;
				if ( s.piece != null && s.piece.type >= 0 )
					slice.setPieceType(s.piece.type) ;
				slice.setMetamorphosisPreIncludesNewBlocks(true) ;
					
				return ;
			}
		} catch ( Exception e ) {
			System.err.println("Exception caught when copying GameBlocksSlice.  Reverting to Stable slice.") ;
			e.printStackTrace() ;
		}
		
		// default: stable
		slice.setBlocksState(GameBlocksSlice.BLOCKS_STABLE) ;
		
		copySimplifiedBlockField(
				slice.getBlockfieldStableLBA(),
				slice.getEdge(), slice.getEdge() ) ;
	}
	
	
	
	
	private Offset copyFallingPieceBlockField_offset = new Offset() ;
	/**
	 * Copies into the provided buffers the "current" state, assuming a
	 * piece is falling.  This method does NOT check the current game state.
	 * Using this method allows a view to obtain a representation of the
	 * current game state that will not change halfway through the
	 * process of drawing it, without locking game progression during
	 * a draw.
	 * 
	 * @param bf_blockField	The current, stable block field
	 * @param bf_piece	The currently falling piece, in place.
	 * @param bf_ghost	The currently falling piece at the location it will reach upon
	 * 					being dropped and locked.
	 * @throws QOrientationConflictException 
	 * @throws GameSystemException 
	 */
	private synchronized int copyFallingPieceBlockFields(
			
			LooselyBoundedArray lba_blockField,
			LooselyBoundedArray lba_piece,
			LooselyBoundedArray [] lba_ghostComponents,
			int firstGhost, int rowOffset, int colOffset ) throws QOrientationConflictException, GameSystemException {
		
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		
		// Exact copy for blockField.  We use copy into, so we bound the entire thing
		// but don't bother clearing it here.
		lba_blockField.boundAll() ;
		ArrayOps.copyInto(s.blockField, (byte[][][])lba_blockField.array(), rowOffset, colOffset) ;
		
		// Lock the piece into lba_piece, an otherwise empty blockfield.  We clear
		// the current bounds, set those bounds to empty, the lock the piece and
		// set the bounds to the piece dimensions.
		lba_piece.clear().boundNone() ;
		if ( s.piece.blocks != null ) {
			copyFallingPieceBlockField_offset.takeVals(s.offset) ;
			copyFallingPieceBlockField_offset.x += colOffset ;
			copyFallingPieceBlockField_offset.y += rowOffset ;
			s.piece.boundLBA(lba_piece, copyFallingPieceBlockField_offset) ;
			ls.lock((byte[][][])lba_piece.array(), s.piece, copyFallingPieceBlockField_offset) ;
		
			// Unlock ghost components, drop them, etc.
			// These methods are safe to use; they alter only temporary structures
			// that would, even if in-use, hold exactly the same information.
			// However, they may delay the draw or the next game state update.
			unlockComponentsNoStateUpdate() ;
			
			// Updates the values "numComponents", "components",
			// "componentOriginalOffsets," and "componentFellOffsets".
			// It is important to note that "componentFellOffsets" is
			// currently equal to "componentOriginalOffsets".
			// Perform the fall.
			try {
				this.safelyFallPieces(s.blockField, s.components, s.componentFellOffsets, s.numComponents) ;
				
				// Now lock those components to the ghost field.
				Piece c ;
				for ( int i = 0; i < s.numComponents; i++ ) {
					copyFallingPieceBlockField_offset.takeVals( s.componentFellOffsets.get(i) ) ;
					copyFallingPieceBlockField_offset.x += colOffset ;
					copyFallingPieceBlockField_offset.y += rowOffset ;
					// as with the piece blockfield, we clear out the current
					// bounds and set the new bounds to the new content.
					c = s.components.get(i) ;
					lba_ghostComponents[i+firstGhost].clear().boundNone() ;
					c.boundLBA(lba_ghostComponents[i+firstGhost], copyFallingPieceBlockField_offset) ;
					ls.lock((byte[][][])lba_ghostComponents[i+firstGhost].array(), c, copyFallingPieceBlockField_offset) ;
				}
			} catch( Exception e ) {
				// Some failure occurred here.  For example, if the game
				// is over because of a collision, then safelyFallPieces
				// will throw an exception.  Whoops!  No problem, we don't
				// expect these "copy" functions to change the game state
				// at all; this loss condition will be found elsewhere.
			}
			
		}
		
		// Done
		//System.err.println("Game.copyFallingPieceBlockFields: returning " + s.numComponents) ;
		//new Exception("nothing").printStackTrace() ;
		return s.numComponents ;
	}
	
	
	
	public synchronized int copyFallingComponents(
			LooselyBoundedArray [] lba_blockfields,
			LooselyBoundedArray [] lba_fallingComponents,
			int [] fallDistance, boolean [] newToBlockField, int rowOffset, int colOffset ) throws QOrientationConflictException, GameSystemException {
		
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		
		return copyFallingPieceChunks( s.numComponents, s.components, s.componentOriginalOffsets, s.componentFellOffsets, null, s.lockPiece,
				lba_blockfields, lba_fallingComponents, null, fallDistance, newToBlockField, rowOffset, colOffset ) ;
	}
	
	private Offset copyFallingComponents_offset = new Offset() ;
	private synchronized int copyFallingComponents(
			LooselyBoundedArray lba_pre_blockfield,
			LooselyBoundedArray [] lba_blockfields,
			LooselyBoundedArray [] lba_fallingComponents,
			LooselyBoundedArray [] lba_fallingComponentsAggregated,
			int [] fallDistance, boolean [] newToBlockField, int rowOffset, int colOffset ) {
		
		try {
			// Set pre_blockfield to current chunk positions.  We directly copy
			// the current blockfield followed by all components; use full bounds
			// for this.
			lba_pre_blockfield.boundAll() ;
			ArrayOps.copyInto( s.blockField, (byte [][][])lba_pre_blockfield.array(), rowOffset, colOffset ) ;
			for ( int i = 0; i < s.numComponents; i++ ) {
				copyFallingComponents_offset.takeVals( s.componentOriginalOffsets.get(i) ) ;
				copyFallingComponents_offset.x += colOffset ;
				copyFallingComponents_offset.y += rowOffset ;
				// clear the current bounds and then set it to exactly the component boundaries.
				ls.lock((byte [][][])lba_pre_blockfield.array(), s.components.get(i), copyFallingComponents_offset) ;
			}
			
			return copyFallingPieceChunks( s.numComponents, s.components, s.componentOriginalOffsets, s.componentFellOffsets, null, s.lockPiece,
					lba_blockfields, lba_fallingComponents, lba_fallingComponentsAggregated, fallDistance, newToBlockField, rowOffset, colOffset ) ;
		} catch ( Exception e ) {
			e.printStackTrace() ;
			// A hideous error occurred!  But we don't really care, because these
			// "copy" functions should never be taken as error-checking or 
			// ways of updating game state.
		}
		return 1 ;
		// STOPGAP: we return 1 as a stopgap to get around the strange lock-failure bugs.
		// This is a HACK.
		// TODO: Find and identify this bug, which may be purely the result of echoed game actions.
	}
	
	
	private Offset copyFallingChunks_offset = new Offset() ;
	/**
	 * Copy the current falling chunks, blockfields, etc. into the 
	 * provided structures.  Copy with the falling format conventions:
	 * Group chunks together based on the distance they will fall.
	 * For unique distances d1, d2, d3, ..., set
	 * 
	 * fallDistance[0] = -1
	 * fallDistance[1] = d1
	 * fallDistance[2] = d2
	 * ...
	 * 
	 * bf_blockfields[i] is those blocks "locked" AFTER a fall of length
	 * fallDistance[i].  i.e., bf_blockfields[0] is guaranteed to be ONLY
	 * those blocks which are currently locked.  If some chunks have a distance
	 * of 0 to fall, then fallDistances[1] will = 0 and bf_blockfields[1] will
	 * represent the blockfield after those chunks lock.
	 * 
	 * bf_fallingComponents[i] holds exactly those components which have fallen a distance
	 * of fallDistances[i].  Thus bf_fallingComponents[0] will always be empty.
	 * 
	 * Note that as of 2/7/2012, this behavior has changed.  Previously bf_fallingComponents[i]
	 * held components at their original heights, before they fall fallDistances[i].
	 * New behavior hold them at their final heights, having fallen fallDistances[i].
	 * 
	 * 'rowOffset' and 'colOffset' describe the number of rows/cols to skip
	 * when copying into the provided arrays.  Basically, bf_blockfields[i]
	 * and bf_fallingComponents[i] may be larger than blockField (the game's
	 * representation of its blocks); the offset provided describes how many
	 * rows/cols to initially skip when copying.  No matter the setting of these
	 * offsets, it is assumed that the dimensions are at least [2][R+rowOffset][C+colOffset],
	 * and that rowOffset,colOffset >= 0.
	 * 
	 * @param bf_blockfields
	 * @param bf_fallingComponents
	 * @param fallDistance
	 * @param rowOffset
	 * @param colOffset
	 * @return
	 * @throws QOrientationConflictException
	 * @throws GameSystemException
	 */
	private synchronized int copyFallingChunks(
			LooselyBoundedArray lba_pre_blockfield,
			LooselyBoundedArray[] lba_blockfields,
			LooselyBoundedArray[] lba_fallingComponents,
			int [] fallDistance, boolean [] newToBlockField, int rowOffset, int colOffset) {
		
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		try {
			// Set pre_blockfield to current chunk positions.  Because this is a full-size
			// copy op., we set full bounds and let the copy be our clear.
			lba_pre_blockfield.boundAll() ;
			ArrayOps.copyInto( s.blockField, (byte[][][])lba_pre_blockfield.array(), rowOffset, colOffset ) ;
			for ( int i = 0; i < s.numChunks; i++ ) {
				copyFallingChunks_offset.takeVals( s.chunkOriginalOffsets.get(i) ) ;
				copyFallingChunks_offset.x += colOffset ;
				copyFallingChunks_offset.y += rowOffset ;
				ls.lock((byte[][][])lba_pre_blockfield.array(), s.chunks.get(i), copyFallingChunks_offset) ;
			}
			
			return copyFallingPieceChunks( s.numChunks, s.chunks, s.chunkOriginalOffsets, s.chunkFellOffsets, s.chunkIsNewToBlockField, true,
					lba_blockfields, lba_fallingComponents, null, fallDistance, newToBlockField, rowOffset, colOffset ) ;
		} catch ( Exception e ) {
			e.printStackTrace() ;
			// A hideous error occurred!  But we don't really care, because these
			// "copy" functions should never be taken as error-checking or 
			// ways of updating game state.
		}
		return 0 ;
	}
	
	
	private Offset copyFallingChunks_offset2 = new Offset() ;
	/**
	 * Copy the current falling chunks, blockfields, etc. into the 
	 * provided structures.  Copy with the falling format conventions:
	 * Group chunks together based on the distance they will fall.
	 * For unique distances d1, d2, d3, ..., set
	 * 
	 * fallDistance[0] = -1
	 * fallDistance[1] = d1
	 * fallDistance[2] = d2
	 * ...
	 * 
	 * bf_blockfields[i] is those blocks "locked" AFTER a fall of length
	 * fallDistance[i].  i.e., bf_blockfields[0] is guaranteed to be ONLY
	 * those blocks which are currently locked.  If some chunks have a distance
	 * of 0 to fall, then fallDistances[1] will = 0 and bf_blockfields[1] will
	 * represent the blockfield after those chunks lock.
	 * 
	 * bf_fallingComponents[i] holds exactly those components which have fallen a distance
	 * of fallDistances[i].  Thus bf_fallingComponents[0] will always be empty.
	 * 
	 * Note that as of 2/7/2012, this behavior has changed.  Previously bf_fallingComponents[i]
	 * held components at their original heights, before they fall fallDistances[i].
	 * New behavior hold them at their final heights, having fallen fallDistances[i].
	 * 
	 * 'rowOffset' and 'colOffset' describe the number of rows/cols to skip
	 * when copying into the provided arrays.  Basically, bf_blockfields[i]
	 * and bf_fallingComponents[i] may be larger than blockField (the game's
	 * representation of its blocks); the offset provided describes how many
	 * rows/cols to initially skip when copying.  No matter the setting of these
	 * offsets, it is assumed that the dimensions are at least [2][R+rowOffset][C+colOffset],
	 * and that rowOffset,colOffset >= 0.
	 * 
	 * @param bf_blockfields
	 * @param bf_fallingComponents
	 * @param fallDistance
	 * @param rowOffset
	 * @param colOffset
	 * @return
	 * @throws QOrientationConflictException
	 * @throws GameSystemException
	 */
	private synchronized int copyFallingChunks(
			LooselyBoundedArray lba_pre_blockfield,
			LooselyBoundedArray[] lba_blockfields,
			LooselyBoundedArray[] lba_fallingComponents,
			LooselyBoundedArray[] lba_fallingComponentsAggregated,
			int [] fallDistance, boolean [] newToBlockField, int rowOffset, int colOffset) {
		
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		try {
			// Set pre_blockfield to current chunk positions.  For convenience, and
			// the fact that we include a direct copy, we use full bounds and let
			// the copy be the clear.
			lba_pre_blockfield.boundAll() ;
			ArrayOps.copyInto( s.blockField, (byte[][][])lba_pre_blockfield.array(), rowOffset, colOffset ) ;
			for ( int i = 0; i < s.numChunks; i++ ) {
				copyFallingChunks_offset2.takeVals( s.chunkOriginalOffsets.get(i) ) ;
				copyFallingChunks_offset2.x += colOffset ;
				copyFallingChunks_offset2.y += rowOffset ;
				ls.lock((byte[][][])lba_pre_blockfield.array(), s.chunks.get(i), copyFallingChunks_offset2) ;
			}
			
			return copyFallingPieceChunks( s.numChunks, s.chunks, s.chunkOriginalOffsets, s.chunkFellOffsets, s.chunkIsNewToBlockField, true,
					lba_blockfields, lba_fallingComponents, lba_fallingComponentsAggregated, fallDistance, newToBlockField, rowOffset, colOffset ) ;
		} catch ( Exception e ) {
			System.err.println("caught an exception?") ;
			e.printStackTrace() ;
			// A hideous error occurred!  But we don't really care, because these
			// "copy" functions should never be taken as error-checking or 
			// ways of updating game state.
			
			// note: some weird exceptions are happening here, related to tightened piece bounds.
			// print some stuff.
			System.err.println("blockfield:") ;
			System.err.println(arrayAsString(s.blockField)) ;
			System.err.println() ;
			for ( int i = 0; i < s.numChunks; i++ ) {
				Piece c = s.chunks.get(i) ;
				System.err.println("chunk " + i) ;
				System.err.println("bounds are x(" + c.boundsLL.x + " - " + c.boundsUR.x + "), y(" + c.boundsLL.y + " - " + c.boundsUR.y + ")") ;
				System.err.println("orig offset is " + s.chunkOriginalOffsets.get(i).toString()) ;
				System.err.println("fell offset is " + s.chunkFellOffsets.get(i).toString()) ;
				System.err.println(arrayAsString(c.blocks)) ;
				System.err.println() ;
			}
		}
		return 0 ;
	}
	
	
	private Offset copyFallingPieceChunks_offset = new Offset() ;
	
	private synchronized int copyFallingPieceChunks(
			int numChunks,
			ArrayList<Piece> chunks,
			ArrayList<Offset> originalOffsets,
			ArrayList<Offset> fellOffsets,
			ArrayList<Boolean> isNewToBlockField,
			boolean lockChunksIntoBlockfields,
			LooselyBoundedArray[] lba_blockfields,
			LooselyBoundedArray[] lba_fallingComponents,
			LooselyBoundedArray[] lba_fallingComponentsAggregated,
			int [] fallDistance, boolean [] newToBlockField, int rowOffset, int colOffset) throws QOrientationConflictException {
		
		try {
			//System.err.println("Game.copyFallingPieceChunks: start") ;
			// Copy the blockfield directly (use all bounds).  we don't bother
			// with a 'clear' as we assume the direct copy handles that.
			lba_blockfields[0].boundAll() ;
			ArrayOps.copyInto(s.blockField, (byte[][][])lba_blockfields[0].array(), rowOffset, colOffset) ;
			// No falling components for 0.  Clear and reset the current bounds.
			lba_fallingComponents[0].clear().boundNone() ;
			fallDistance[0] = -1 ;
			newToBlockField[0] = false ;
			
			// What are the fall distances?  Simple insertion sort.
			int num = 0 ;
			for ( int i = 0; i < numChunks; i++ ) {
				
				// HACK: For some reason we are getting empty fields with very high fall
				// distances.  This seems like a recent bug.
				if ( ArrayOps.isEmpty( chunks.get(i).blocks ) ) {
					System.err.println("Game.copyFallingPieceChunks: empty chunk " + i + " for recording distance.") ;
					continue ;
				}
				
				Offset oOrig = originalOffsets.get(i) ;
				Offset oFell = fellOffsets.get(i) ;
				
				int dist = oOrig.y - oFell.y ;
				boolean isNew = isNewToBlockField == null ? false : isNewToBlockField.get(i) ;
				//System.err.println("chunk " + i + " isNew:  "+ isNew + " with dist " + dist) ;
				
				boolean unique = true ;
				for ( int j = 0; j < num+1; j++ ) {
					if ( fallDistance[j] == dist && isNew == newToBlockField[j] ) {
						unique = false ;
					}
				}
				
				if ( unique ) {
					//System.err.println("is unique") ;
					// Insert
					num++ ;
					for ( int j = num-1; j >= 0; j-- ) {
						if ( fallDistance[j] >= dist ) {
							fallDistance[j+1] = fallDistance[j] ;
							newToBlockField[j+1] = newToBlockField[j] ;
						}
						else {
							fallDistance[j+1] = dist ;
							newToBlockField[j+1] = isNew ;
							break ;
						}
					}
				}
			}
			
			// Okay.  Lock the components in at the appropriate distances.
			// We lock the components in, at their original offsets, in bf_fallingComponents;
			// we also lock them into the bf_blockfields, which should be initialized to
			// fit the blockfield.
			for ( int i = 1; i < num+1; i++ ) {
				// Here we clear out the 'num' blockfields we use below.  
				// fallingComponents should be completely empty (we use clear.boundNone)
				// bf_blockfields should start as a copy of the stationary blockfield,
				// meaning we use full-bounds and copy the whole thing directly.
				lba_fallingComponents[i].clear().boundNone() ;
				lba_blockfields[i].boundAll() ;
				ArrayOps.copyInto(
						(byte[][][])lba_blockfields[0].array() ,
						(byte[][][])lba_blockfields[i].array() ) ;
			}
			for ( int i = 0; i < numChunks; i++ ) {
				
				// HACK: For some reason we are getting empty fields with very high fall
				// distances.  This seems like a recent bug.
				if ( ArrayOps.isEmpty( chunks.get(i).blocks ) ) {
					System.err.println("Game.copyFallingPieceChunks: skipping empty chunk " + i + " for copying.") ;
					continue ;
				}
				
				Offset oOrig = originalOffsets.get(i) ;
				Offset oFell = fellOffsets.get(i) ;
				copyFallingPieceChunks_offset.takeVals(oFell) ;
				copyFallingPieceChunks_offset.x += colOffset ;
				copyFallingPieceChunks_offset.y += rowOffset ;
				
				int dist = oOrig.y - oFell.y ;
				boolean isNew = isNewToBlockField == null ? false : isNewToBlockField.get(i) ;
				
				Piece c ;
				
				//System.err.println("Locking chunk " + i + " with dist " + dist) ;
				
				// Find the place for it...
				int distEntry = -1 ;
				for ( int j = 1; j < num+1; j++ ) {
					if ( fallDistance[j] == dist && distEntry == -1 )
						distEntry = j ;
					if ( fallDistance[j] == dist && newToBlockField[j] == isNew ) {
						//System.err.println("Lock chunk " + i) ;
						c = chunks.get(i) ;
						// lock into falling components and grow the bounds to accomodate.
						c.boundLBA( lba_fallingComponents[j], copyFallingPieceChunks_offset ) ;
						ls.lock((byte[][][])lba_fallingComponents[j].array(), c, copyFallingPieceChunks_offset) ;
						break ;
					}
				}
				
				if ( distEntry > -1 ) {
					// Lock in for each blockfield at this or greater distance.  We have 
					// already set the bounds of these to full, so no need to adjust them.
					for ( int k = distEntry; k < num+1; k++ )
						if ( lockChunksIntoBlockfields )
							ls.lock((byte[][][])lba_blockfields[k].array(), chunks.get(i), copyFallingPieceChunks_offset) ;
				}
			}
			
			if ( lba_fallingComponentsAggregated != null ) {
				for ( int i = 1; i < num+1; i++ ) {
					// empty them.  We use actual bounds for these.
					lba_fallingComponentsAggregated[i].clear().boundNone() ;
				}
				for ( int i = 0; i < numChunks; i++ ) {
					
					// HACK: For some reason we are getting empty fields with very high fall
					// distances.  This seems like a recent bug.
					if ( ArrayOps.isEmpty( chunks.get(i).blocks ) ) {
						System.err.println("Game.copyFallingPieceChunks: skipping empty chunk " + i + " for copying.") ;
						continue ;
					}
					
					Offset oOrig = originalOffsets.get(i) ;
					Offset oFell = fellOffsets.get(i) ;
					copyFallingPieceChunks_offset.takeVals(oOrig) ;
					copyFallingPieceChunks_offset.x += colOffset ;
					copyFallingPieceChunks_offset.y += rowOffset ;
					
					int dist = oOrig.y - oFell.y ;
					
					// Find the place for it...
					for ( int j = 1; j < num+1; j++ ) {
						if ( fallDistance[j] <= dist ) {
							// include the bounds for this and lock into place.  We have
							// set the bounds as empty earlier on, so we can UNION safely.
							copyFallingPieceChunks_offset.y = oOrig.y - fallDistance[j] + rowOffset ;		// BUG: we didn't include "row offset" before, causing weird errors.
							Piece c = chunks.get(i) ;
							c.boundLBA(lba_fallingComponentsAggregated[j], copyFallingPieceChunks_offset) ;
							ls.lock((byte[][][])lba_fallingComponentsAggregated[j].array(), chunks.get(i), copyFallingPieceChunks_offset) ;
							//System.err.println("Game.copyFallingPieceChunks: locking " + i + " into aggregate " + j + " at distance " + fallDistance[j]) ;
						}
					}
				}
			}
			// finally, set each aggregated chunk set according to the component at
			// the specified fall distance.  It should include that component, and ALL
			// future components, positioned after a fall of the distance of the
			// setting chunk.
			
			return num+1 ;
		} catch( Exception e ) {
			e.printStackTrace() ;
			// Error error bo-berror bananafana fo-ferror
			// me-mi-mo-merror
			// error
			// note: some weird exceptions are happening here, related to tightened piece bounds.
			// print some stuff.
			System.err.println("blockfield:") ;
			System.err.println(arrayAsString(s.blockField)) ;
			System.err.println() ;
			for ( int i = 0; i < s.numChunks; i++ ) {
				Piece c = chunks.get(i) ;
				System.err.println("chunk " + i) ;
				System.err.println("bounds are x(" + c.boundsLL.x + " - " + c.boundsUR.x + "), y(" + c.boundsLL.y + " - " + c.boundsUR.y + ")") ;
				System.err.println("orig offset is " + originalOffsets.get(i).toString()) ;
				System.err.println("fell offset is " + fellOffsets.get(i).toString()) ;
				System.err.println(arrayAsString(c.blocks)) ;
				System.err.println() ;
			}
		} 
		return 0 ;
	}
	
	
	private synchronized void copyMetamorphosisBlockfields(
			LooselyBoundedArray lba_pre_blockfield,
			LooselyBoundedArray lba_post_blockfield,
			int rowOffset, int colOffset ) {
		
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		lba_pre_blockfield.boundAll() ;
		lba_post_blockfield.boundAll() ;
		ArrayOps.copyInto(s.blockFieldBefore, (byte[][][])lba_pre_blockfield.array(), rowOffset, colOffset) ;
		ArrayOps.copyInto(s.blockField, (byte[][][])lba_post_blockfield.array(), rowOffset, colOffset) ;
		
		// done.  simple enough.
	}
	
	
	
	private synchronized void copyPushingRowsBlockfields(
			LooselyBoundedArray lba_pre_blockfield,
			LooselyBoundedArray lba_post_blockfield,
			int rowOffset, int colOffset ) {
		
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		lba_pre_blockfield.boundAll() ;
		lba_post_blockfield.boundAll() ;
		ArrayOps.copyInto(s.blockFieldBefore, (byte[][][])lba_pre_blockfield.array(), rowOffset, colOffset) ;
		ArrayOps.copyInto(s.blockField, (byte[][][])lba_post_blockfield.array(), rowOffset, colOffset) ;
		
		// done.  simple enough.
	}
	
	
	
	private synchronized void copyClearedBlockfieldsAndRows(
			LooselyBoundedArray lba_pre_blockfield,
			LooselyBoundedArray lba_post_blockfield,
			LooselyBoundedArray lba_cleared_blockfield,
			int [] chromatic_clears, boolean [] monochromatic_clears,
			int rowOffset, int colOffset) {
		
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		
		// Set blockfields to appropriate values.  We use full-copies, because we don't necessarily
		// have complete information.  Well - technically - we DO have complete information
		// abound blockFieldInverseClear (exactly those rows which are cleared), but I don't
		// care to implement that now.  Maybe eventually if I need to.
		lba_pre_blockfield.boundAll() ;
		lba_post_blockfield.boundAll() ;
		lba_cleared_blockfield.boundAll() ;
		ArrayOps.copyInto( s.blockFieldBefore, (byte[][][])lba_pre_blockfield.array(), rowOffset, colOffset ) ;
		ArrayOps.copyInto( s.blockField, (byte[][][])lba_post_blockfield.array(), rowOffset, colOffset ) ;
		ArrayOps.copyInto( s.blockFieldInverseClear, (byte[][][])lba_cleared_blockfield.array(), rowOffset, colOffset ) ;
		
		// Copy the clear values
		for ( int r = 0; r < s.R; r++ ) {
			chromatic_clears[r+rowOffset] = s.clearedRowsChromatic[r] ;
			monochromatic_clears[r+rowOffset] = s.clearedRowsMonochromatic[r] ;
		}
	}
	
	
	public synchronized int copyUnlockedColumnOffsets(
			ArrayList<Offset> offsets ) {
		
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		
		if ( s.numUnlockColumnAboveAlreadyUnlocked == 0 )
			return 0 ;
		
		// Otherwise, copy everything that has ACTUALLY been unlocked,
		// using 0 to numUnlockColumnAboveAlreadyUnlocked.
		int numOffsetsCopied = s.numUnlockColumnAboveAlreadyUnlocked ;
		for ( int i = 0; i < s.numUnlockColumnAboveAlreadyUnlocked; i++ ) {
			Offset o ;
			if ( offsets.size() > i )
				o = offsets.get(i) ;
			else {
				o = new Offset() ;
				offsets.add(o) ;
			}
			o.takeVals( s.unlockColumnAbove.get(i) ) ;
		}
		
		// Now adjust the list of unlocked offsets, moving everything
		// down and resetting "numUnlockColumnAboveAlreadyUnlocked.
		for ( int i = 0; i < s.numUnlockColumnAbove; i++ ) {
			s.unlockColumnAbove.get(i).takeVals(
					s.unlockColumnAbove.get(i + s.numUnlockColumnAboveAlreadyUnlocked ) ) ;
		}
		s.numUnlockColumnAboveAlreadyUnlocked = 0 ;
		
		return numOffsetsCopied ;
	}
	
	
	public synchronized void copyCurrentBlockField( byte [][][] ar ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		ArrayOps.copyInto(s.blockField, ar) ;
 	}
	
	public synchronized void copyCurrentDisplacementRows( byte [][][] ar ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		ArrayOps.copyInto(s.blockFieldDisplacementRows, ar) ;
 	}
	
	
	
	private synchronized void copyCurrentBlockField( LooselyBoundedArray lba, int rowOffset, int colOffset ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		lba.boundAll() ;
		ArrayOps.copyInto(s.blockField, (byte[][][])lba.array(), rowOffset, colOffset) ;
	} 
	
	private synchronized void copyCurrentBlockFieldWithPiece( byte [][][] ar ) throws QOrientationConflictException {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		copyCurrentBlockField(ar) ;
		try {
			if ( s.state == STATE_FALLING || s.state == STATE_ENTERING ) {
				ls.lock(ar, s.piece, s.offset) ;
			}
		} catch( Exception e ) {
			// Problem here - probably a piece collision.  We don't
			// care though - errors are the Game object's problem, not
			// the copy method's.
		}
	}
	
	public synchronized void copyNextPieceGameBlocksSlice( GameBlocksSlice slice ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
	
		slice.setBlocksState(GameBlocksSlice.BLOCKS_PIECE_PREVIEW) ;
		ArrayOps.setEmpty(slice.getBlockfieldStable()) ;
		ArrayOps.setEmpty(slice.getBlockfieldMinimumProfile()) ;
		
		
		if ( s.nextPieces[0] != null && s.nextPieces[0].type > -1 ) {
			s.nextPieces[0].type = s.nextPieces[0].defaultType ;
			s.nextPieces[0].rotation = s.nextPieces[0].defaultRotation ;
			rs.turn0(s.nextPieces[0]) ;
			
			tempPiece.takeVals(s.nextPieces[0]) ;
			rs.turnMinimumHorizontalProfile(tempPiece) ;
			
			ArrayOps.copyInto(tempPiece.blocks, slice.getBlockfieldMinimumProfile(), slice.getEdge(), slice.getEdge()) ;
			ArrayOps.copyInto(s.nextPieces[0].blocks, slice.getBlockfieldStable(), slice.getEdge(), slice.getEdge()) ;
			slice.setPieceType(s.nextPieces[0].type) ;
		}
		else {
			ArrayOps.setEmpty(slice.getBlockfieldStable()) ;
			ArrayOps.setEmpty(slice.getBlockfieldMinimumProfile()) ;
		}
	}
	
	public synchronized void copyReservePieceGameBlocksSlice( GameBlocksSlice slice ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;

		
		slice.setBlocksState(GameBlocksSlice.BLOCKS_PIECE_PREVIEW) ;
		ArrayOps.setEmpty(slice.getBlockfieldStable()) ;
		ArrayOps.setEmpty(slice.getBlockfieldMinimumProfile()) ;
		
		if ( GameModes.reserveBehavior(ginfo) == GameModes.RESERVE_BEHAVIOR_SPECIAL ) {
			if ( sps.getSpecialPreviewPieceType() != -1 ) {
				tempPiece.type = sps.getSpecialPreviewPieceType() ;
				tempPiece.rotation = 0 ;
				rs.turn0(tempPiece) ;
				ArrayOps.copyInto(tempPiece.blocks, slice.getBlockfieldStable(), slice.getEdge(), slice.getEdge()) ;
				
				rs.turnMinimumHorizontalProfile(tempPiece) ;
				ArrayOps.copyInto(tempPiece.blocks, slice.getBlockfieldMinimumProfile(), slice.getEdge(), slice.getEdge()) ;
				slice.setPieceType(tempPiece.type) ;
			}
		}
		else if ( s.reservePieces[0] != null && s.reservePieces[0].type > -1 ) {
			s.reservePieces[0].type = s.reservePieces[0].defaultType ;
	 	        s.reservePieces[0].rotation = s.reservePieces[0].defaultRotation ;
			rs.turn0(s.reservePieces[0]) ;

			tempPiece.takeVals(s.reservePieces[0]) ;
			rs.turnMinimumHorizontalProfile(tempPiece) ;			
			
			ArrayOps.copyInto(tempPiece.blocks, slice.getBlockfieldMinimumProfile(), slice.getEdge(), slice.getEdge()) ;
			ArrayOps.copyInto(s.reservePieces[0].blocks, slice.getBlockfieldStable(), slice.getEdge(), slice.getEdge()) ;
			slice.setPieceType(s.reservePieces[0].type) ;
		}
	}
	
	public synchronized int R() {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		return s.R ;
	}
	
	public synchronized int C() {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		return s.C ;
	}
	
	
	public synchronized int copyAndClearOutgoingAttackQueue(
			ArrayList<AttackDescriptor> queue, int numDescriptors) {
		return as.copyAndClearOutgoingAttackQueue(queue, numDescriptors) ;
	}
	
	public synchronized int aggregateAndClearOutgoingAttackQueue(
			ArrayList<AttackDescriptor> queue, int numDescriptors) {
		return as.aggregateAndClearOutgoingAttackQueue(queue, numDescriptors) ;
	}
	
	/**
	 * Adds the provided attack to the "incoming attack queue."
	 * If 'destructive' is true, then this call might alter the
	 * contents of the provided 'ad', if doing so makes the operation
	 * more efficient.
	 * 
	 * @param ad
	 * @param destructive
	 */
	public synchronized void enqueueIncomingAttack(
			AttackDescriptor ad, boolean destructive) {
		as.enqueueIncomingAttack(ad, destructive) ;
	}
	
	/**
	 * Copies the current game state into the ActionCycleStateDescriptor.
	 * 
	 * @param acsd
	 * @param obeyDescriptorInclusions
	 */
	public synchronized void copyStateIntoActionCycleStateDescriptor(
			ActionCycleStateDescriptor acsd ) {
		
		assert( s.R == acsd.R ) ;
		assert( s.C == acsd.C ) ;
		
		this.copyCurrentBlockField( acsd.blockField ) ;
		
		this.copyCurrentDisplacementRows( acsd.displacementRows ) ;
		
		for ( int i = 0; i < GameState.PIECE_LOOKAHEAD; i++ ) {
			acsd.nextPieces[i] = s.nextPieces[i].defaultType ;
			acsd.nextPiecesDefaultRotation[i] = (byte)s.nextPieces[i].defaultRotation ;
		}
		
		for ( int i = 0; i < GameState.PIECE_LOOKAHEAD; i++ ) {
			acsd.reservePieces[i] = s.reservePieces[i].defaultType ;
			acsd.reservePiecesDefaultRotation[i] = (byte)s.reservePieces[i].defaultRotation ;
		}
		
		acsd.ginfo.takeVals(ginfo) ;
		
		acsd.numAttackDescriptors = as.copyIncomingAttackQueue( acsd.attackDescriptors ) ;
		
		// By default, we do dequeue an attack.
		acsd.dequeueAttackThisCycle = acsd.numAttackDescriptors > 0 ;
		// Previously, we transfered displacement if it's greater than or equal to 1.
		// However, we'd now prefer to displace "out of sight" if at all possible.
		// Therefore, we transfer displacement if the displacement is greater than
		// or equal to -1 (yes, that means we typically have a negative displacement,
		// usually between -2 and -1).
		if ( ds.displaces() ) {
			double disp = ds.getDisplacedRows() ;
			acsd.displacedRowsToTransferThisCycle = (byte)(disp < -1 ? 0 : Math.floor(disp) + 2) ;
		} else {
			acsd.displacedRowsToTransferThisCycle = 0 ;
		}
	}
	
	

	
	
	public synchronized void setStateFromActionCycleStateDescriptor(
			ActionCycleStateDescriptor acsd) {
		
		assert( s.R == acsd.R ) ;
		assert( s.C == acsd.C ) ;
		
		// Update blockfield
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = 0; r < s.R; r++ ) {
				for ( int c = 0; c < s.C; c++ ) {
					s.blockField[q][r][c] = acsd.blockField[q][r][c] ;
				}
			}
		}
		
		// Update displaced rows
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = 0; r < s.R; r++ ) {
				for ( int c = 0; c < s.C; c++ ) {
					s.blockFieldDisplacementRows[q][r][c] = acsd.displacementRows[q][r][c] ;
				}
			}
		}
		
		// Update pieces
		/*if ( adapter.game_shouldUseTimingSystem(this) )
			System.out.print("Included: next pieces: ") ;*/
		if ( s.nextPieces[0].type != acsd.nextPieces[0] || s.nextPieces[0].rotation != acsd.nextPiecesDefaultRotation[0] ) {
			gevents.setHappened(GameEvents.EVENT_NEXT_PIECE_CHANGED) ;
			s.geventsLastTick.setHappened(GameEvents.EVENT_NEXT_PIECE_CHANGED) ;
		}
		for ( int i = 0; i < GameState.PIECE_LOOKAHEAD; i++ ) {
			s.nextPieces[i].type = s.nextPieces[i].defaultType = acsd.nextPieces[i] ;
			s.nextPieces[i].rotation = s.nextPieces[i].defaultRotation = acsd.nextPiecesDefaultRotation[i] ;
			if ( s.nextPieces[i].type > 0 )
				rs.turn0(s.nextPieces[i]) ;
		}
		pieceHistory.setPieceHeldInBag(s.nextPieces[0].type, PieceHistory.PIECE_BAG) ;
		/*if ( adapter.game_shouldUseTimingSystem(this) )
			System.out.println("") ;*/
	
		if ( s.reservePieces[0].type != acsd.reservePieces[0] || s.reservePieces[0].rotation != acsd.reservePiecesDefaultRotation[0] ) {
			gevents.setHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED) ;
			s.geventsLastTick.setHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED) ;
		}
		for ( int i = 0; i < GameState.PIECE_LOOKAHEAD; i++ ) {
			s.reservePieces[i].type = s.reservePieces[i].defaultType = acsd.reservePieces[i] ;
			s.reservePieces[i].rotation = s.reservePieces[i].defaultRotation = acsd.reservePiecesDefaultRotation[i] ;
			if ( s.reservePieces[i].type > 0 )
				rs.turn0(s.reservePieces[i]) ;
		}
		pieceHistory.setPieceHeldInBag(s.reservePieces[0].type, PieceHistory.RESERVE_BAG) ;
	
		ginfo.takeVals( acsd.ginfo, GameInformation.FLAG_UPDATE_TIME_FORWARD_ONLY ) ;
	
		//System.err.println("Game.setStateFromActionCycleStateDescriptor setIncomingAttackQueue with size " + acsd.numAttackDescriptors) ;
		as.setIncomingAttackQueue(acsd.attackDescriptors, acsd.numAttackDescriptors) ;
		
		// We may or may not dequeue at this moment.  Tell us, acsd!
		s.unleashAttackThisCycle = acsd.dequeueAttackThisCycle ;
		s.numberOfDisplacedRowsToTransferThisCycle = acsd.displacedRowsToTransferThisCycle ;
		//System.err.println("setStateFromActionCycleStateDescriptor dequeue attach this cycle: " + acsd.dequeueAttackThisCycle) ;
	}
	
	
	
	/*
	 ***************************************************************************
	 *
	 * STATE TRANSITION FUNCTIONS
	 *
	 * When it is appropriate to change states (i.e. the TimingSystem agrees,
	 * we are in the previous state according to the logic of state transition,
	 * etc.), these functions handle the bookkeeping.
	 * 
	 * These transitions do NOT make any calls to ts.can*.  However, they DO
	 * make calls to ts.did*.
	 * 
	 ***************************************************************************
	 */
	
	
	/**
	 * Checks with the adapter to synchronize.  If it returns 'true',
	 * this method indicates that we should proceed (and that our state
	 * has been updated to "preparing".  Otherwise, if it returns false,
	 * we should try again after some time has passed (e.g., after 
	 * the next tick).
	 * 
	 * @return Are we ready to prepare a piece for this next cycle?
	 */
	private boolean synchronize() {
		if ( adapter.game_beginActionCycle(this) ) {
			// By returning true, the adapter has indicated
			// that we should proceed (and that we are "synchronized",
			// whatever that means - maybe our state matches that
			// of a canonical "coordinated" game, maybe we
			// ARE the coordinate game and we've extracted and
			// sent our data outward, etc.
			
			// Let the attack system know if we're unleashing any
			// attacks this cycle; we will be allowing the Attack
			// System to perform these attacks as the cycle rolls on.
			if ( s.unleashAttackThisCycle ) {
				as.dequeueIncomingAttacksThisCycle() ;
				//System.err.println("synchronize: unleashAttackThisCycle") ;
				//System.err.println("synchronize: has garbage rows? " + as.hasGarbageRows()) ;
				//System.err.println("synchronize: has level change? " + as.hasLevelChange()) ;
				//System.err.println("synchronize: can dequeue incoming attacks? " + as.canDequeueIncomingAttacks()) ;
			} //else
				//System.err.println("synchronize: DON'T unleash attack this cycle") ;
			
			s.state = STATE_PREPARING ;
			gevents.setHappened(GameEvents.EVENT_SYNCHRONIZED) ;
			s.geventsLastTick.setHappened(GameEvents.EVENT_SYNCHRONIZED) ;
			
			ss.scoreStartActionCycle() ;
			
			adapter.game_didAdvance() ;		// jump start any echoes
			return true ;
		}
		return false ;
	}
	
	
	private void preparePiece() throws InvalidPieceException {
		
		// Now adjust the list of unlocked offsets, moving everything
		// down and resetting "numUnlockColumnAboveAlreadyUnlocked.
		for ( int i = 0; i < s.numUnlockColumnAbove; i++ ) {
			s.unlockColumnAbove.get(i).takeVals(
					s.unlockColumnAbove.get(i + s.numUnlockColumnAboveAlreadyUnlocked ) ) ;
		}
		s.numUnlockColumnAboveAlreadyUnlocked = 0 ;
		
		s.clearCascadeNumber = -1 ;
		
		// Set piece to the values of nextPiece.
		s.piece.takeVals(s.nextPieces[0]) ;
		
		// Get a new piece for nextPiece.
		for ( int i = 0; i < GameState.PIECE_LOOKAHEAD -1; i++ )
			s.nextPieces[i].takeVals(s.nextPieces[i+1]) ;
		pieceBag.pop( s.nextPieces[GameState.PIECE_LOOKAHEAD-1] );
		rs.turn0(s.nextPieces[GameState.PIECE_LOOKAHEAD-1]) ;

		// Set the Offset for the current piece.
		// TODO: Better method for initial offset.  Support "phase in".
		// Support game modes.
		positionPieceForEntry( s.piece, s.offset ) ;
		// TODO: Add support (here, enterPiece, or in dequeueMoves) for pieces that are
		// rotated, slide, etc. before coming in.
		
		// Set our
		// state as STATE_ENTERNG.
		s.state = STATE_ENTERING ;
		
		// Reset whether we lock it.
		s.lockPiece = true ;
		// Reset our lock-metamorphosis behavior
		s.lockThenActivate = s.lockThenDeactivate = false ;
		
		gevents.setHappened(GameEvents.EVENT_PIECE_PREPARED) ;
		gevents.setHappened(GameEvents.EVENT_NEXT_PIECE_CHANGED) ;
		s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_PREPARED ) ;
		s.geventsLastTick.setHappened( GameEvents.EVENT_NEXT_PIECE_CHANGED ) ;
		
		trs.didPrepare(s.piece, s.offset) ;
		
		// inform the pieceHistory.
		pieceHistory.setPiecePopped(s.piece.type, PieceHistory.PIECE_BAG) ;
		pieceHistory.setPieceHeldInBag(s.nextPieces[0].type, PieceHistory.PIECE_BAG) ;
	}
	
	/**
	 * Sets the provided piece and offset (rotation, turn, blocks, and offset values)
	 * as appropriate for entry -- i.e., as if this piece will next enter the blockfield.
	 * 
	 * This involves examining the piece blocks to find the right location, and,
	 * if the standard piece position is blocked (collision) we allow a single
	 * upward step.
	 * 
	 * @param p
	 * @param o
	 */
	private void positionPieceForEntry( Piece p, Offset o ) {
		p.rotation = p.defaultRotation ;
		rs.turn0(p) ;
		
		o.setRow(s.R/2 - p.bottomRow() ) ;		// Bottom row of piece is just above top row
		o.setCol((int)Math.floor((s.C-0.5)/2) - p.centerColumn() ) ;		// Centered in field
		// We put the bottom row 1 row above visible area.
		
		// we attempt to maintain displacement at a maximum of -1.  Since we will typically
		// display the blocks with negative displacement, we move the piece upward to compensate.
		if ( ds.displaces() )
			o.y += 2 ;
		
		// align center column to the center.  For even numbers of
		// columns, align to the floor of halfway; for odd numbers, align
		// to the exact center grid column.
		
		// This position might not be allowed.  We allow a one-step-up "kick"
		// if the piece can't fit here.  It's might  also be worth some kind
		// of player alarm?
		// TODO: Player alarm on this condition?
		if ( cs.collides(s.blockField, p, o) )
			o.y++ ;		// try one step up.
	}
	
	private void enterPiece() {
		
		// The next piece just entered.  Remember that the piece was
		// previously Prepared, which sets its offset and Piece values.
		// Just update the state.
		// Behavior change 12/13/2012: we now allow one final 'kick' for
		// a new piece before it collides.
		if ( cs.collides(s.blockField, s.piece, s.offset) ) {
			System.err.println("GAME.enterPiece() : collision!  shouldUseTimingSystem is " + adapter.game_shouldUseTimingSystem(this)) ;
			// We lost, but we still "enter" the piece for better draws.
			s.state = STATE_FALLING ;		// THIS IS FOR BETTER DRAWS
			lvs.didEnterPiece() ;
			ts.didEnter(s.piece, s.offset) ;
			ss.scoreEnter(s.piece, s.offset) ;
			gevents.setHappened( GameEvents.EVENT_PIECE_ENTERED ) ;
			
			// Game lost!
			s.game_period = PERIOD_GAME_LOST ;
			gevents.setHappened(GameEvents.EVENT_GAME_LOST) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_GAME_LOST ) ;
			adapter.game_didCollide() ;
		}
		else {
			as.aboutToEnterPiece(s.piece, s.offset) ;
			
			if ( as.hasOutgoingAttacks() )
				adapter.game_hasOutgoingAttack(this) ;
			
			s.state = STATE_FALLING ;
			lvs.didEnterPiece() ;
			ts.didEnter(s.piece, s.offset) ;
			ss.scoreEnter(s.piece, s.offset) ;
			gevents.setHappened( GameEvents.EVENT_PIECE_ENTERED ) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_ENTERED ) ;
			
			trs.didEnter(s.piece, s.offset) ;
			adapter.game_didEnter() ;
		}
	}
	
	
	public void fall() {
		if ( s.state == STATE_FALLING ) {
			s.offset.y-- ;
			ts.didFall(s.offset) ;
			gevents.setHappened( GameEvents.EVENT_PIECE_FELL ) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_FELL ) ;
			trs.didFall(s.offset) ;
			adapter.game_didFall() ;
			//System.err.println(TAG + " did Fall 1 to offset y " + offset.getRow()) ;
		}
	}
	
	public boolean stateOK_fall() {
		if ( s.state != STATE_FALLING )
			return false ;
		tempOffset.takeVals(s.offset) ;
		tempOffset.y-- ;
		return !cs.collides(s.blockField, s.piece, tempOffset) ;
	}
	
	public boolean timingOK_fall() {
		if ( s.state != STATE_FALLING )
			return false ;
		return ts.canFall() ;
	}
	
	public boolean timingOK_fastFall() {
		if ( s.state != STATE_FALLING )
			return false ;
		return ts.canFastFall() ;
	}
	
	
	
	public void lockPiece() {
		if ( s.state == STATE_FALLING ) {
			// Perform locking.  Note that we don't actually lock the
			// piece itself to the blockField.  Instead this is a state
			// transition; lockComponents will take care of it.
			
			// Update "before last piece"
			ArrayOps.copyInto(s.blockField, s.blockFieldBefore) ;
			
			s.state = STATE_PROGRESSION ;
			s.progressionState = PROGRESSION_COMPONENTS_UNLOCK ;
			s.stateAfterProgression = STATE_ENDING_CYCLE ;
			ts.didLock(s.blockField, s.piece, s.offset) ;
			ss.scoreLock(s.blockField, s.piece, s.offset) ;
			gevents.setHappened( GameEvents.EVENT_PIECE_LOCKED ) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_LOCKED ) ;
			
			as.aboutToLockPiece(s.blockField, s.piece, s.offset) ;
			if ( as.hasOutgoingAttacks() )
				adapter.game_hasOutgoingAttack(this) ;
			
			if ( sps.aboutToLock(s.piece, s.offset, s.blockField) ) {
				gevents.setHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED) ;
				s.geventsLastTick.setHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED) ;
				gevents.setHappened(GameEvents.EVENT_SPECIAL_UPGRADE) ;
				s.geventsLastTick.setHappened(GameEvents.EVENT_SPECIAL_UPGRADE) ;
			}
			
			pieceHistory.setPieceLanded() ;
			
			//System.err.println("lockPiece piece is " + s.piece.type + " " + s.piece.rotation + " game is " + this) ;
			trs.willLock(s.blockField, s.piece, s.offset) ;
			
			adapter.game_didLock() ;
			adapter.game_didEndActionCycle() ;
		}
	}
	
	public boolean stateOK_lockPiece() {
		return s.state == STATE_FALLING ;
	}
	
	
	/**
	 * A complex method that applies game logic to determine its action.
	 * "autolock" is an expected control option, i.e. pressing "down" might
	 * drop the piece -- and then either autolock or not.  However, we do
	 * not assume that autolock is called at the end of a drop - it can
	 * be called at any time.
	 * 
	 * 'autolock' checks whether the piece is in a position where locking
	 * is appropriate -- for example, if it is resting on a surface --
	 * ignoring the timing system delay before that would naturally occur.
	 * On the other hand, if locking is NOT appropriate at its current location,
	 * this method will do nothing.
	 * 
	 * 'autolock' is not transmitted as an event the game performed, because
	 * its result is either equivalent to 'lock' or equivalent to doing 
	 * nothing.  Likewise either 'didLock' or no callback occurs.
	 * 
	 */
	
	public void autolock() {
		if ( s.state == STATE_FALLING ) {
			if ( ls.shouldLock(s.blockField, s.piece, s.offset) )
				lockPiece() ;
		}
	}
	
	public void fall_or_autolock() {
		if ( s.state == STATE_FALLING ) {
			if ( stateOK_fall() ) {
				fall() ;
			}
			else if ( ls.shouldLock(s.blockField, s.piece, s.offset) ) {
				lockPiece() ;
			}
		}
	}
	
	
	public boolean stateOK_autolock() {
		return s.state == STATE_FALLING ;
	}
	
	private void unlockComponents() {
		unlockComponentsNoStateUpdate() ;
		
		// Update state variables.  In particular, proceed
		// to the next progression state.
		s.progressionState = PROGRESSION_COMPONENTS_FALL ;
		if ( !s.lockThenActivate && !s.lockThenDeactivate ) {
			gevents.setHappened(GameEvents.EVENT_COMPONENTS_UNLOCKED) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_COMPONENTS_UNLOCKED ) ;
		} else {
			gevents.setHappened(GameEvents.EVENT_COMPONENTS_UNLOCKED_TOWARDS_METAMORPHOSIS) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_COMPONENTS_UNLOCKED_TOWARDS_METAMORPHOSIS ) ;
		}
	}
	
	private void unlockComponentsNoStateUpdate() {
		// Unlock the components of the piece.
		// ASSUMPTION: there are currently no floating "components"
		s.numComponents = ls.unlock(s.piece, s.components, 0) ;
		// Set original offsets (and, for now, falling offsets)
		Offset o ;
		for ( int i = 0; i < s.numComponents; i++ ) {
			if ( i < s.componentOriginalOffsets.size() ) {
				s.componentOriginalOffsets.get(i).takeVals(s.offset) ;
				s.componentFellOffsets.get(i).takeVals(s.offset) ;
			}
			else {
				o = new Offset() ;
				o.takeVals(s.offset) ;
				s.componentOriginalOffsets.add(o) ;
				o = new Offset() ;
				o.takeVals(s.offset) ;
				s.componentFellOffsets.add(o) ;
			}
		}
	}

	private void fallComponents() throws QOrientationConflictException, GameSystemException {
		// "Fall" - i.e., calculate the "FellOffsets" - of the
		// components.  Note that this is a little more complicated
		// than falling a piece, because some components may
		// end up landing on each other.
		
		// Make sure FellOffsets is up-to-date.  Assume space has
		// been allocated in componentFellOffsets and componentOriginalOffsets
		// at the time 'numComponents' was set.
		Offset o ;
		for ( int i = 0; i < s.numComponents; i++ ) {
			o = s.componentOriginalOffsets.get(i) ;
			s.componentFellOffsets.get(i).takeVals(o) ;
		}
		
		// Safely fall them to update FellOffsets.
		safelyFallPieces( s.blockField, s.components, s.componentFellOffsets, s.numComponents ) ;
		
		// Score
		for ( int i = 0; i < s.numComponents; i++ ) {
			ss.scoreComponentFall(s.components.get(i),
					s.componentOriginalOffsets.get(i), 
					s.componentFellOffsets.get(i) ) ;
		}
		
		// Update state variables.
		s.progressionState = PROGRESSION_COMPONENTS_LOCK ;
		
		if ( !s.lockThenActivate && !s.lockThenDeactivate ) {
			gevents.setHappened(GameEvents.EVENT_COMPONENTS_FELL) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_COMPONENTS_FELL ) ;
		} else {
			gevents.setHappened(GameEvents.EVENT_COMPONENTS_FELL_TOWARDS_METAMORPHOSIS) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_COMPONENTS_FELL_TOWARDS_METAMORPHOSIS ) ;
		}
	}
	
	
	private void lockComponents() throws QOrientationConflictException {
		// Lock the components at their current "FellOffsets."  This just puts
		// everything in the blockField.
		Piece p ;
		Offset o ;
		
		if ( s.lockPiece ) {
			
			try {
				if ( s.numComponents > 0 )
					trs.willLockComponents(s.blockField, s.components, s.componentFellOffsets, s.numComponents) ;
				for ( int i = 0; i < s.numComponents; i++ ) {
					p = s.components.get(i) ;
					o = s.componentFellOffsets.get(i) ;
					ls.lock(s.blockField, p, o) ;
				}
				if ( s.numComponents > 0 )
					trs.didLockComponents(s.blockField, s.components, s.componentFellOffsets, s.numComponents) ;
			} catch (QOrientationConflictException qoce) {
				qoce.printStackTrace() ;
				// HACK: There is a multiplayer bug causing a sometimes-crash here.
				// TODO: redo multiplayer communication to avoid these issues.
				if ( adapter.game_shouldUseTimingSystem(this) )
					throw qoce ;
			}
			
			// Score
			for ( int i = 0; i < s.numComponents; i++ ) {
				ss.scoreComponentLock(s.blockField,
						s.components.get(i),
						s.componentFellOffsets.get(i) ) ;
			}
		}
		
		// Work is done.  Update state variables.
		if ( !s.lockThenActivate && !s.lockThenDeactivate ) {
			s.progressionState = PROGRESSION_CHUNKS_UNLOCK ;
			gevents.setHappened(GameEvents.EVENT_COMPONENTS_LOCKED) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_COMPONENTS_LOCKED ) ;
		} else {
			s.progressionState = PROGRESSION_TRIGGERED_METAMORPHOSIS ;
			gevents.setHappened(GameEvents.EVENT_COMPONENTS_LOCKED_TOWARDS_METAMORPHOSIS) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_COMPONENTS_LOCKED_TOWARDS_METAMORPHOSIS ) ;
		}
	}
	
	
	private void triggeredMetamorphosis() {
		// There are 2 types of triggered metamorphoses: lock-then-activate,
		// and lock-then-deactivate.
		if ( s.lockThenActivate ) {
			ArrayOps.copyInto(s.blockField, s.blockFieldBefore) ;
			if ( ms.metamorphosize(MetamorphosisSystem.METAMORPHOSIS_ACTIVATE, s.blockField) ) {
				gevents.setHappened( GameEvents.EVENT_PIECE_LOCK_THEN_METAMORPHOSIS ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_LOCK_THEN_METAMORPHOSIS ) ;
				gevents.setHappened( GameEvents.EVENT_METAMORPHOSIS_DID_ACTIVATE ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_METAMORPHOSIS_DID_ACTIVATE ) ;
				
				trs.didMetamorphis(s.blockFieldBefore, s.blockField) ;
				as.metamorphosis(s.blockField, s.blockFieldBefore) ;
				if ( as.hasOutgoingAttacks() )
					adapter.game_hasOutgoingAttack(this) ;
			}
		} else if ( s.lockThenDeactivate ) {
			ArrayOps.copyInto(s.blockField, s.blockFieldBefore) ;
			if ( ms.metamorphosize(MetamorphosisSystem.METAMORPHOSIS_DEACTIVATE, s.blockField) ) {
				gevents.setHappened( GameEvents.EVENT_PIECE_LOCK_THEN_METAMORPHOSIS ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_PIECE_LOCK_THEN_METAMORPHOSIS ) ;
				gevents.setHappened( GameEvents.EVENT_METAMORPHOSIS_DID_DEACTIVATE ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_METAMORPHOSIS_DID_DEACTIVATE ) ;
				
				trs.didMetamorphis(s.blockFieldBefore, s.blockField) ;
				as.metamorphosis(s.blockField, s.blockFieldBefore) ;
				if ( as.hasOutgoingAttacks() )
					adapter.game_hasOutgoingAttack(this) ;
			}
		}
		
		// unlike an end-cycle metamorphosis, this one remains within the progression.
		// proceed to CHUNKS_UNLOCK.
		s.progressionState = PROGRESSION_CHUNKS_UNLOCK ;
	}
	
	private void clear() {
		// Perform a clear on the current block field.
		// TODO: Support monochrome clears (probably with a delayed trigger)
		
		boolean hasCleared = cls.clearable( s.blockField, s.clearedRowsChromatic ) ;
		boolean hasClearedMono = cls.clearableMonochrome( s.blockField, s.clearedRowsMonochromatic) ;
		
		// Note: monochromatic clears TRUMP chromatic ones.  If something qualifies
		// for both, perform the monochromatic clear.
		if ( hasClearedMono ) {
			hasCleared = false ;
			for ( int r = 0; r < s.R; r++ ) {
				if ( s.clearedRowsMonochromatic[r] )
					s.clearedRowsChromatic[r] = QOrientations.NO ;
				hasCleared = hasCleared || s.clearedRowsChromatic[r] != QOrientations.NO ;
			}
		}
		
		// Our behavior depends on whether anything actually cleared.
		if ( hasCleared || hasClearedMono ) {
			// Perform the clear!  A bit of complication.  We copy the current blockfield into
			// Before, and into "inverse clear."  Then we perform the clears on blockField (giving
			// the up-to-date blockfield data) and "inverse" (giving a record of those blocks
			// which were cleared).
			ArrayOps.copyInto(s.blockField, s.blockFieldBefore) ;
			ArrayOps.copyInto(s.blockField, s.blockFieldInverseClear) ;
			
  			cls.clear(s.blockField, s.clearedRowsChromatic) ;
			cls.clearMonochrome(s.blockField, s.clearedRowsMonochromatic) ;
			for ( int r = 0; r < s.R; r++ ) {
				if ( s.clearedRowsMonochromatic[r] )
					cls.inverseClearMonochrome(s.blockFieldInverseClear, r, true) ;
				else
					cls.inverseClear(s.blockFieldInverseClear, r, s.clearedRowsChromatic[r]) ;
			}
			
			// note cascade
			s.clearCascadeNumber++ ;
			
			// Trigger before score: the trigger might increase the score for this event!
			trs.didClear(s.blockFieldBefore, s.blockField, s.clearedRowsChromatic, s.clearedRowsMonochromatic) ;
			
			// Note in the history
			pieceHistory.setClear(s.clearedRowsChromatic, s.clearedRowsMonochromatic) ;
			
			// Tell the attack system.  It needs to see the state of things before the clear,
			// so pass it blockFieldBefore.
			if ( s.stateAfterProgression == STATE_PREPARING )
				as.aboutToClearWithoutPiece(s.blockFieldBefore, s.blockField, s.blockFieldInverseClear, s.clearedRowsChromatic, s.clearedRowsMonochromatic) ;
			else
				as.aboutToClear(s.piece, s.offset, s.blockFieldBefore, s.blockField, s.blockFieldInverseClear, s.clearedRowsChromatic, s.clearedRowsMonochromatic) ;
			if ( as.hasOutgoingAttacks() )
				adapter.game_hasOutgoingAttack(this) ;
			
			Piece p = s.stateAfterProgression == STATE_PREPARING ? null : s.piece ;
			Offset o = s.stateAfterProgression == STATE_PREPARING ? null : s.offset ;
			boolean specialChanged = sps.aboutToClear(p, o,
					s.blockFieldBefore,
					s.blockField,
					s.blockFieldInverseClear,
					s.clearedRowsChromatic, s.clearedRowsMonochromatic) ;
			
			if ( specialChanged ) {
				gevents.setHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED) ;
				s.geventsLastTick.setHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED) ;
				gevents.setHappened(GameEvents.EVENT_SPECIAL_UPGRADE) ;
				s.geventsLastTick.setHappened(GameEvents.EVENT_SPECIAL_UPGRADE) ;
			}
			
			// Score the clear, and note other things!
			ss.scoreClear(s.clearedRowsChromatic, s.clearedRowsMonochromatic) ;
			ts.didClear(s.clearedRowsChromatic, s.clearedRowsMonochromatic) ;
			ginfo.recordClears(s.clearedRowsChromatic, s.clearedRowsMonochromatic) ;
 			
			// Because we cleared, chunks may be floating.  Advance to that state.
			s.progressionState = PROGRESSION_CHUNKS_UNLOCK ;
			gevents.setHappened(GameEvents.EVENT_CLEAR) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_CLEAR ) ;
		}
		else if ( s.numUnlockColumnAbove > 0 || s.numBlocksForValleys > 0 ) {
			// There is nothing to clear, BUT there may be things to unlock!
			s.progressionState = PROGRESSION_CHUNKS_UNLOCK ;
		}
		else {
			// No clear to perform.  End the progression.
			s.state = s.stateAfterProgression ;
		}
	}
	
	private void unlockChunks() {
		// Unlock the chunks from the blockfield.
		s.numChunks = 0 ;
		
		// What is the maximum row in the blockfield?
		int maxRowInBlockfield = -1 ;
		for ( int qp = 0; qp < 2 && maxRowInBlockfield < 0; qp++ ) 
			for ( int r = s.R-1; r >= 0 && maxRowInBlockfield < 0; r-- )
				for ( int c = 0; c < s.C && maxRowInBlockfield < 0; c++ )
					if ( s.blockField[qp][r][c] != QOrientations.NO )
						maxRowInBlockfield = r ;
		maxRowInBlockfield = Math.max(maxRowInBlockfield, s.R/2) ;
		
		// Should we unlock any columns first?
		if ( s.numUnlockColumnAbove > 0 ) {
			for ( int i = s.numUnlockColumnAboveAlreadyUnlocked; i < s.numUnlockColumnAbove+s.numUnlockColumnAboveAlreadyUnlocked; i++ ) {
				s.numChunks = ls.unlockColumnAbove(
						s.blockField, s.unlockColumnAbove.get(i),
						s.chunks, s.chunkOriginalOffsets, s.numChunks) ;
			}
			
			gevents.setHappened( GameEvents.EVENT_COLUMN_UNLOCKED ) ;
			
			// TODO: Note the unlocking of each such column in GameEvents gevents.
			// Be sure to retain some indication of WHERE the unlocks took place,
			// since we empty the field storing it below.
			
			s.numUnlockColumnAboveAlreadyUnlocked = s.numUnlockColumnAbove ;
			s.numUnlockColumnAbove = 0 ;
		}
		
		// This should always be called.
		s.numChunks = ls.unlock(s.blockField, s.chunks, s.chunkOriginalOffsets, s.numChunks) ;
		
		for ( int i = 0; i < s.numChunks; i++ ) {
			if ( s.chunkIsNewToBlockField.size() <= i )
				s.chunkIsNewToBlockField.add(Boolean.FALSE) ;
			else
				s.chunkIsNewToBlockField.set(i, Boolean.FALSE) ;
		}
		
		int chunksFallen = 0 ;
		
		int minRow = maxRowInBlockfield + 2 ;
		// four types of blocks to try dropping!
		for ( int t = 0; t < 5; t++ ) {
			int numBlocks = 0 ;
			ValleySystem.Type type = null ;
			switch( t ) {
			case 0:
				numBlocks = s.numBlocksForValleys ;
				s.numBlocksForValleys = 0 ;
				type = ValleySystem.Type.VALLEY ;
				break ;
			case 1:
				numBlocks = s.numBlocksForJunctions ;
				s.numBlocksForJunctions = 0 ;
				type = ValleySystem.Type.JUNCTION ;
				break ;
			case 2:
				numBlocks = s.numBlocksForPeaks ;
				s.numBlocksForPeaks = 0 ;
				type = ValleySystem.Type.PEAK ;
				break ;
			case 3:
				numBlocks = s.numBlocksForCorners ;
				s.numBlocksForCorners = 0 ;
				type = ValleySystem.Type.CORNER ;
				break ;
			case 4:
				numBlocks = s.numBlocksForTroll ;
				s.numBlocksForTroll = 0 ;
				break ;
			}
			
			if ( numBlocks > 0 ) {
				// fall the chunks to get the "after" result.
				Offset o ;
				for ( int i = chunksFallen; i < s.numChunks; i++ ) {
					if ( i < s.chunkFellOffsets.size() ) {
						o = s.chunkFellOffsets.get(i) ;
						o.takeVals( s.chunkOriginalOffsets.get(i) ) ;
					}
					else {
						o = new Offset() ;
						o.takeVals( s.chunkOriginalOffsets.get(i) ) ;
						s.chunkFellOffsets.add(o) ;
					}
				}
				safelyFallPieces( s.blockField, s.chunks, s.chunkFellOffsets, s.numChunks ) ;
				chunksFallen = s.numChunks ;
				
				// lock them in tempField at that offset...
				ArrayOps.copyInto(s.blockField, tempField) ;
				for ( int i = 0; i < s.numChunks; i++ )
					ls.lock(tempField, s.chunks.get(i), s.chunkFellOffsets.get(i)) ;
				
				// now add our blocks.
				if ( type != null ) {
					s.numChunks = vs.dropBlocks(
							0, numBlocks, minRow,
							tempField,
							s.chunks, s.chunkOriginalOffsets, s.numChunks,
							type ) ;
				} else {
					// troll blocks!
					s.numChunks = vs.dropBlocks(
							numBlocks, numBlocks, minRow,
							tempField,
							s.chunks, s.chunkOriginalOffsets, s.numChunks,
							ValleySystem.Type.JUNCTION, ValleySystem.Type.PEAK, ValleySystem.Type.CORNER ) ;
				}
				
				// these are new to blockfield...
				for ( int i = chunksFallen; i < s.numChunks; i++ ) {
					if ( s.chunkIsNewToBlockField.size() <= i )
						s.chunkIsNewToBlockField.add(Boolean.TRUE) ;
					else
						s.chunkIsNewToBlockField.set(i, Boolean.TRUE) ;
				}
				
				// increment minrow...
				minRow += numBlocks ;
			}
		}
		
		// Reset fell offsets.
		Offset o ;
		for ( int i = 0; i < s.numChunks; i++ ) {
			if ( i < s.chunkFellOffsets.size() ) {
				o = s.chunkFellOffsets.get(i) ;
				o.takeVals( s.chunkOriginalOffsets.get(i) ) ;
			}
			else {
				o = new Offset() ;
				o.takeVals( s.chunkOriginalOffsets.get(i) ) ;
				s.chunkFellOffsets.add(o) ;
			}
		}
		
		if ( s.numChunks > 0 ) {
			gevents.setHappened(GameEvents.EVENT_CHUNKS_UNLOCKED) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_CHUNKS_UNLOCKED ) ;
		}
		
		// Update state
		s.progressionState = PROGRESSION_CHUNKS_FALL ;
	}
	
	private void fallChunks() throws QOrientationConflictException, GameSystemException {
		// "Fall" - i.e., calculate the "FellOffsets" - of the
		// chunks.  Note that this is a little more complicated
		// than falling a piece, because some components may
		// end up landing on each other.
		
		// Make sure FellOffsets is up-to-date.  Assume space has
		// been allocated in chunkFellOffsets and chunkOriginalOffsets
		// at the time 'numChunks' was set.
		Offset o ;
		for ( int i = 0; i < s.numChunks; i++ ) {
			o = s.chunkOriginalOffsets.get(i) ;
			s.chunkFellOffsets.get(i).takeVals(o) ;
		}
		
		// Safely fall them to update FellOffsets.
		safelyFallPieces( s.blockField, s.chunks, s.chunkFellOffsets, s.numChunks ) ;
		
		// Score
		for ( int i = 0; i < s.numChunks; i++ ) {
			ss.scoreChunkFall(s.chunks.get(i),
					s.chunkOriginalOffsets.get(i), 
					s.chunkFellOffsets.get(i) ) ;
		}
		
		// Update state variables.  After a fall we lock the chunks.
		s.progressionState = PROGRESSION_CHUNKS_LOCK ;
		
		if ( s.numChunks > 0 ) {
			gevents.setHappened(GameEvents.EVENT_CHUNKS_FELL) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_CHUNKS_FELL ) ;
		}
	}
	
	private void lockChunks() throws QOrientationConflictException {
		// Lock the chunks at their current "FellOffsets."  This just puts
		// everything in the blockField.
		Piece p ;
		Offset o ;
		for ( int i = 0; i < s.numChunks; i++ ) {
			p = s.chunks.get(i) ;
			o = s.chunkFellOffsets.get(i) ;
			ls.lock(s.blockField, p, o) ;
		}
		
		// No Scoring function for locking chunks.
		// Work is done.  Update state variables.  After locking chunks,
		// we attempt to clear.
		s.progressionState = PROGRESSION_CLEAR ;
		
		if ( s.numChunks > 0 ) {
			gevents.setHappened(GameEvents.EVENT_CHUNKS_LOCKED) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_CHUNKS_LOCKED ) ;
		}
	}
	
	/**
	 * Ends the current cycle, officially.
	 */
	private void endCycle() {
		
		ArrayOps.copyInto(s.blockField, s.blockFieldBefore) ;
		MetamorphosisSystem.Result result = ms.endCycle(s.blockField) ;
		if ( result.didAny() ) {
			gevents.setHappened( GameEvents.EVENT_METAMORPHOSIS ) ;
			s.geventsLastTick.setHappened( GameEvents.EVENT_METAMORPHOSIS ) ;
			
			if ( result.did(MetamorphosisSystem.METAMORPHOSIS_ACTIVATE) ) {
				gevents.setHappened( GameEvents.EVENT_METAMORPHOSIS_DID_ACTIVATE ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_METAMORPHOSIS_DID_ACTIVATE ) ;
			}
			if ( result.did(MetamorphosisSystem.METAMORPHOSIS_DEACTIVATE) ) {
				gevents.setHappened( GameEvents.EVENT_METAMORPHOSIS_DID_DEACTIVATE ) ;
				s.geventsLastTick.setHappened( GameEvents.EVENT_METAMORPHOSIS_DID_DEACTIVATE ) ;
			}
		}
		
		if ( sps.endCycle() ) {
			gevents.setHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED) ;
			s.geventsLastTick.setHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED) ;
			gevents.setHappened(GameEvents.EVENT_SPECIAL_UPGRADE) ;
			s.geventsLastTick.setHappened(GameEvents.EVENT_SPECIAL_UPGRADE) ;
		}
		
		as.endCycle() ;
		if ( as.hasOutgoingAttacks() )
			adapter.game_hasOutgoingAttack(this) ;
		
		s.usedReserve = false ;
		
		s.numActionCycles++ ;
	}
	
	/*
	 ***************************************************************************
	 *
	 * HELPFUL CALCULATION FUNCTIONS
	 *
	 * These functions change the internal state of the game, especially
	 * the "tertiary variables" such as component offsets and such.
	 * 
	 ***************************************************************************
	 */
	
	/**
	 * safelyFallPieces: given ArrayLists of pieces and offsets, and 
	 * a field to place them in, safely calculates the fall distance
	 * for each piece and updates their offsets.
	 * 
	 * Why use this?  Because if multiple pieces are falling, one
	 * might land ON TOP OF another.  Simply calculating their fall
	 * distances independently will give an incorrect result.  This
	 * function will instead determine where the pieces will land
	 * of allowed to fall freely, even if they land on each other.
	 * 
	 * Note: updates temporary storage.  tempField, tempBooleans, tempInts.
	 * 
	 * PRECONDITION: the provided pieces do not separate upon landing, at least
	 * 		not in this step.  They also do not "combine" in any complex way.
	 * 
	 * POSTCONDITION: 'offsets' has been updated to the pieces' new, "fallen" positions.
	 * 		Temporary variables have been altered.
	 * 
	 * @param field The block field in which the pieces are falling
	 * @param pieces The pieces falling within the field
	 * @param offsets The pieces's current offsets.  Will be updated.
	 * @param numPieces If < pieces.size(), only this many pieces will be considered "in play".
	 * @throws GameSystemException 
	 */
	private void safelyFallPieces( byte [][][] field, ArrayList<Piece> pieces, ArrayList<Offset> offsets, int numPieces ) throws QOrientationConflictException, GameSystemException {
		
		/*
		// Once you're satisfied this is sufficiently optimized, make the necessary changes.
		if ( safelyFallPieces_optimizationTestOffsets == null ) {
			safelyFallPieces_optimizationTestOffsets = new ArrayList<Offset>() ;
			safelyFallPieces_optimizationOriginalOffsets = new ArrayList<Offset>() ;
		}
		while ( safelyFallPieces_optimizationTestOffsets.size() < numPieces ) {
			safelyFallPieces_optimizationTestOffsets.add ( new Offset() ) ;
			safelyFallPieces_optimizationOriginalOffsets.add( new Offset() ) ;
		}
		for ( int i = 0; i < numPieces; i++ ) {
			safelyFallPieces_optimizationTestOffsets.get(i).takeVals( offsets.get(i) ) ;
			safelyFallPieces_optimizationOriginalOffsets.get(i).takeVals( offsets.get(i) ) ;
		}
		
		safelyFallPieces_superEfficient( field, pieces, safelyFallPieces_optimizationTestOffsets, numPieces ) ;
		safelyFallPieces_efficient( field, pieces, offsets, numPieces ) ;
		
		for ( int i = 0; i < numPieces; i++ ) {
			Offset oEfficient = offsets.get(i) ;
			Offset oSuper = safelyFallPieces_optimizationTestOffsets.get(i) ;
			Offset oOriginal = safelyFallPieces_optimizationOriginalOffsets.get(i) ;
			if ( !oSuper.equals(oEfficient) ) {
				System.err.println("error START") ;
				System.err.println("OPTIMIZATION ERROR!  In safelyFallPieces, piece " + i + " gets super : " + oSuper + " vs efficient " + oEfficient ) ;
				System.err.println("OPTIMIZATION ERROR!  In safelyFallPieces, piece " + i + " gets super : " + oSuper + " vs efficient " + oEfficient ) ;
				System.err.println("OPTIMIZATION ERROR!  In safelyFallPieces, piece " + i + " gets super : " + oSuper + " vs efficient " + oEfficient ) ;
				System.err.println("OPTIMIZATION ERROR!  In safelyFallPieces, piece " + i + " gets super : " + oSuper + " vs efficient " + oEfficient ) ;
				System.err.println("OPTIMIZATION ERROR!  In safelyFallPieces, piece " + i + " gets super : " + oSuper + " vs efficient " + oEfficient ) ;
				System.err.println("OPTIMIZATION ERROR!  In safelyFallPieces, piece " + i + " gets super : " + oSuper + " vs efficient " + oEfficient ) ;
				System.err.println("OPTIMIZATION ERROR!  In safelyFallPieces, piece " + i + " gets super : " + oSuper + " vs efficient " + oEfficient ) ;
				System.err.println("OPTIMIZATION ERROR!  In safelyFallPieces, piece " + i + " gets super : " + oSuper + " vs efficient " + oEfficient ) ;
				System.err.println("OPTIMIZATION ERROR!  In safelyFallPieces, piece " + i + " gets super : " + oSuper + " vs efficient " + oEfficient ) ;
				System.err.println("piece had original offset " + oOriginal + " with LL bound " + pieces.get(i).boundsLL + ", UR bound " + pieces.get(i).boundsUR ) ;
				
				System.err.println("piece itself is: ") ;
				System.err.println("" + Game.arrayAsString(pieces.get(i).blocks)) ;
				
				System.err.println("error END") ;
				throw new RuntimeException("ERROR ERROR ERROR ERROR") ;
			}
		}
		*/
		
		if ( ls.lockKeepsQOrientation() && cs.exactlyNonzeroQOrientationsCollide() )
			safelyFallPieces_superEfficient( field, pieces, offsets, numPieces ) ;
		else if ( ls.lockKeepsQOrientation() )
			safelyFallPieces_efficient( field, pieces, offsets, numPieces ) ;
		else
			safelyFallPieces_general( field, pieces, offsets, numPieces ) ;
	}
	
	
	private ArrayList<Offset> safelyFallPieces_optimizationTestOffsets = null ;
	private ArrayList<Offset> safelyFallPieces_optimizationOriginalOffsets = null ;
	
	
	/**
	 * A super-efficient implementation of this method.
	 * 
	 * A more efficient take on safelyFallPieces_efficient.  Uses essentially the
	 * same procedure, except in the sense that it builds up a boolean structure
	 * of those blocks which have landed, to reduce the number of total comparisons.
	 * 
	 * PRECONDITION: ls.lockKeepsQOrientation() && cs.exactlyNonzeroQOrientationsCollide()
	 * 
	 * @param field
	 * @param pieces
	 * @param offsets
	 * @param numPieces
	 * @throws QOrientationConflictException
	 * @throws GameSystemException
	 */
	private void safelyFallPieces_superEfficient( byte [][][] field, ArrayList<Piece> pieces, ArrayList<Offset> offsets, int numPieces ) throws QOrientationConflictException, GameSystemException {
		
		// OPTIMIZATION: The original implementation took a significant # of CPU cycles.
		// It worked as follows: we copied every block in 'field' into 'tempField', then
		// iterated through the 'still falling' pieces.  After we found the 'minimum fall
		// distance' among them, we locked it into place (in the tempField) and re-fell
		// the rest.  Thus, in the worst case, we get K! comparisons and K field-copies.
		
		// However, note that 'copyInto' is BY FAR the biggest overhead here (74.5% vs.
		// ~12% for cs.spaceBelow).  We noted that under certain QI assumptions - specifically,
		// that block types do not "combine" or "transform" in the process of locking
		// - this procedure can be greatly optimized.
		
		// A revised algorithm was devised to perform this optimized approach.  Unfortunately,
		// it still ended up being a very-high overhead method for a large number of disconnected
		// blocks.
		
		// We resolve this through copying "presence of a block" as a boolean into tempFieldBoolean.
		// Mostly, however, we follow the same procedure as the standard _efficient versions:
		
		// 1. Set finished <- {}, recent <- {field}, falling <- {pieces[0], pieces[1], ...}
		// 2. Set fallDistance[piece] = MAX_INT for all in 'falling.'
		// 3. While 'falling != {}':
		//		a) for p in falling:
		//			i. for f in recent:
		//				A. fallDistance[p] = min( fallDistance[p], spaceBelow( f, p ) )
		//		b) Set minDistPieces <- { p : p in falling, fallDistance[p] = min( fallDistance ) }.
		//		c) Set fallDistance[p in falling] to minDistPieces.
		//		d) Set finished <- finished UNION recent
		//		e) Set recent <- minDistPieces
		//		f) set falling <- falling DIFFERENCE minDistPieces
		
		ArrayOps.setEmpty(tempFieldBoolean) ;
		
		for ( int q = 0; q < 2; q++ )
			for ( int r = 0; r < s.R; r++ )
				for ( int c = 0; c < s.C; c++ )
					tempFieldBoolean[q][r][c] = field[q][r][c] != QOrientations.NO ;
		

		if ( numPieces > tempBooleans.length )		// this is "piece is still falling"
			tempBooleans = new boolean[(numPieces*2)] ;
		if ( numPieces > tempInts.length )
			tempInts = new int[numPieces*2] ;		// 'fall distance.'
		
		// Step 1: initialize piece / blockfield set membership.
		// Put all pieces in "falling."
		for ( int i = 0; i < numPieces; i++ )
			tempBooleans[i] = true ;
		
		// Step 2: initialize fall distances
		for ( int i = 0; i < numPieces; i++ )
			tempInts[i] = 0 ;
		
		// Step 3: the loop.
		boolean hasFalling = numPieces > 0 ;
		while ( hasFalling ) {
			// step a), for each piece that is falling, find the space currently available.
			for ( int i = 0; i < numPieces; i++ ) {
				if ( tempBooleans[i] ) {
					Piece p = pieces.get(i) ;
					Offset o = offsets.get(i) ;
					// how much room to fall?
					int spaceAvailable = cs.spaceBelow(tempFieldBoolean, p, o, true) ;
					// adjust position AND distance fallen.
					tempInts[i] = spaceAvailable ;
				}
			}
			
			// step b), determine the minimum space available amongst those
			// pieces which are still falling.
			int minDistFallen = Integer.MAX_VALUE ;
			for ( int i = 0; i < numPieces; i++ ) {
				if ( tempBooleans[i] && tempInts[i] < minDistFallen ) {
					minDistFallen = tempInts[i] ;
				}
			}
			
			// step c), move each piece down by that amount.
			for ( int i = 0; i < numPieces; i++ ) {
				if ( tempBooleans[i] )
					offsets.get(i).y -= minDistFallen ;
			}
			
			// step c), two final loop through.  For each pieces that DOES have
			// the minimum fall distance, copy it into our temporary boolean field
			// and set tempBoolean to false (it is no longer falling).  If a piece
			// which does NOT have the min fall distance, set hasFalling to true.
			// Note: we do the latter task FIRST; the former task can be skipped if
			// nothing is falling anymore.
			hasFalling = false ;
			for ( int i = 0; i < numPieces; i++ ) {
				if ( tempBooleans[i] && tempInts[i] > minDistFallen )
					hasFalling = true ;
			}
			if ( hasFalling ) {
				for ( int i = 0; i < numPieces; i++ ) {
					if ( tempBooleans[i] && tempInts[i] == minDistFallen ) {
						tempBooleans[i] = false ;
						// copy over.
						Piece p = pieces.get(i) ;
						Offset o = offsets.get(i) ;
						int fRmin = Math.max(0, o.y) ;
						int fRmax = Math.min(s.R, o.y + p.boundHeight()) ;
						int fCmin = Math.max(0, o.x) ;
						int fCmax = Math.min(s.C, o.x + p.boundWidth()) ;
						
						int pr, pc ;
						
						for ( int q = 0; q < 2; q++ ) {
							for ( int r = fRmin; r < fRmax; r++ ) {
								for ( int c = fCmin; c < fCmax; c++ ) {
									tempFieldBoolean[q][r][c] |= p.blocks[q][r - o.y + p.boundsLL.y][c - o.x + p.boundsLL.x] != QOrientations.NO ;
								}
							}
						}			
					}
				}
			}
		}
	}
	
	
	/**
	 * An efficient implementation of this method.
	 * 
	 * PRECONDITION: ls.lockKeepsQOrientation() is true.
	 * 
	 * @param field
	 * @param pieces
	 * @param offsets
	 * @param numPieces
	 * @throws QOrientationConflictException
	 * @throws GameSystemException
	 */
	private void safelyFallPieces_efficient( byte [][][] field, ArrayList<Piece> pieces, ArrayList<Offset> offsets, int numPieces ) throws QOrientationConflictException, GameSystemException {
		// OPTIMIZATION: The original implementation took a significant # of CPU cycles.
		// It worked as follows: we copied every block in 'field' into 'tempField', then
		// iterated through the 'still falling' pieces.  After we found the 'minimum fall
		// distance' among them, we locked it into place (in the tempField) and re-fell
		// the rest.  Thus, in the worst case, we get K! comparisons and K field-copies.
		
		// However, note that 'copyInto' is BY FAR the biggest overhead here (74.5% vs.
		// ~12% for cs.spaceBelow).  We note that under certain QI assumptions - specifically,
		// that block types do not "combine" or "transform" in the process of locking
		// - this procedure can be greatly optimized.
		
		// 1. Set finished <- {}, recent <- {field}, falling <- {pieces[0], pieces[1], ...}
		// 2. Set fallDistance[piece] = MAX_INT for all in 'falling.'
		// 3. While 'falling != {}':
		//		a) for p in falling:
		//			i. for f in recent:
		//				A. fallDistance[p] = min( fallDistance[p], spaceBelow( f, p ) )
		//		b) Set minDistPieces <- { p : p in falling, fallDistance[p] = min( fallDistance ) }.
		//		c) Set fallDistance[p in falling] to minDistPieces.
		//		d) Set finished <- finished UNION recent
		//		e) Set recent <- minDistPieces
		//		f) set falling <- falling DIFFERENCE minDistPieces
		
		// This procedure will, in the worst case, remove 1 piece per iteration and compare
		// the remaining pieces against it - again, K! comparisons.  However, it performs
		// no field-copies and no piece locks.
		
		// This optimization only works when ls.lockKeepsQOrientation(); in other words,
		// when it is safe to assume that the result of a lock is the union of the
		// components locking.
		
		// We use tempBooleans to represent piece status (recent, falling sequentially)
		// and '0' to represent the provided blockfield (for a general-purpose loop,
		// as described above).
		
		int BLOCKFIELD_INDEX = 0 ;
		int PIECE_INDEX_OFFSET = 1 ;
		int FALLING_OFFSET = numPieces + PIECE_INDEX_OFFSET ;
		int TEMP_BOOLEANS_LENGTH = (FALLING_OFFSET + PIECE_INDEX_OFFSET + numPieces) ;
		
		if ( FALLING_OFFSET + numPieces + PIECE_INDEX_OFFSET > tempBooleans.length )
			tempBooleans = new boolean[(FALLING_OFFSET + numPieces + PIECE_INDEX_OFFSET)*2] ;
		if ( numPieces > tempInts.length )
			tempInts = new int[numPieces*2] ;		// 'fall distance.'
		
		// Step 1: initialize piece / blockfield set membership.
		for ( int i = 0; i < TEMP_BOOLEANS_LENGTH; i++ )
			tempBooleans[i] = false ;
		// put the blockfield in 'recent' (first 1/2) and all pieces in 'falling' (second 1/2)
		tempBooleans[BLOCKFIELD_INDEX] = true ;
		for ( int i = 0; i < numPieces; i++ )
			tempBooleans[i + PIECE_INDEX_OFFSET + FALLING_OFFSET] = true ;
		
		// Step 2: initialize fall distances
		for ( int i = 0; i < numPieces + PIECE_INDEX_OFFSET; i++ )
			tempInts[i] = Integer.MAX_VALUE ;
		
		// Step 3: the loop.
		int distanceFallen = 0 ;
		boolean hasFalling = numPieces > 0 ;
		while ( hasFalling ) {
			// step a), for each piece that is falling...
			for ( int i = 0; i < numPieces; i++ ) {
				int pFallingIndex = i + PIECE_INDEX_OFFSET + FALLING_OFFSET ;
				int pArrayListIndex = i ;
				if ( tempBooleans[pFallingIndex] ) {
					// step i., for each f that is 'recent'...
					for ( int fRecentIndex = 0; fRecentIndex < FALLING_OFFSET; fRecentIndex++ ) {
						int fArrayListIndex = fRecentIndex - PIECE_INDEX_OFFSET ;		// is negative for blockfield
						if ( tempBooleans[fRecentIndex] ) {
							// We have already fallen 'distanceFallen', and offsets have been adjusted
							// accordingly.  Determine how much FURTHER p can fall w.r.t. f; if the
							// sum of this and distanceFallen is less than its current minimum fall
							// distance, update the minimum.
							
							// Floor landings: if fRecentIndex is BLOCKFIELD_INDEX, then compare including
							// 'walls' - a solid boundary around the blockfield edges.  Otherwise, set
							// walls to false.
							
							// Relative offsets:  if using BLOCKFIELD, take the current offset of p.  Otherwise,
							// if 'f' is a recently landed piece, calculate the "relative offset" from 
							// p to f as p's offset minus f's offset (offset p -> field, then reverse( f -> field )).
							int spaceAvailable ;
							if ( fRecentIndex == BLOCKFIELD_INDEX )
								spaceAvailable = cs.spaceBelow( field,
										pieces.get(pArrayListIndex),
										offsets.get(pArrayListIndex),
										true) ;		// include walls
							else {
								// EDIT 4/27: we're trying to constrict piece boundaries as best we can,
								// so (unlike before this edit) fieldPiece may have boundLL.x/.y set to
								// nonzero.  This is fine, but we need to adjust the tempOffset to take it
								// into account.  fArrayListOffset provides the location of boundLL's element.
								// In other words, if boundLL.x is 1, then we will compare against fieldPiece's
								// 0th column when we should compare against 1st.  Add the boundLL back in
								// to compensate.
								Piece fieldPiece = pieces.get(fArrayListIndex) ;
								tempOffset.takeVals( offsets.get(pArrayListIndex) ) ;
								tempOffset.x -= offsets.get(fArrayListIndex).x ; //+ fieldPiece.boundsLL.x ;
								tempOffset.y -= offsets.get(fArrayListIndex).y ; // + fieldPiece.boundsLL.y ;
								// offset from p to f
								
								spaceAvailable = cs.spaceBelow( fieldPiece.blocks,
										fieldPiece.boundsLL, fieldPiece.boundsUR,
										pieces.get(pArrayListIndex),
										tempOffset,
										false) ;
							}
							
							if ( spaceAvailable + distanceFallen < tempInts[pArrayListIndex + PIECE_INDEX_OFFSET])
								tempInts[pArrayListIndex + PIECE_INDEX_OFFSET] = spaceAvailable + distanceFallen ;
						}
					}
				}
			}
			
			// step b) Determine the 'minimum distance' among all currently falling pieces.
			int minDist = Integer.MAX_VALUE ;
			for ( int i = PIECE_INDEX_OFFSET; i < numPieces + PIECE_INDEX_OFFSET; i++ )
				if ( tempBooleans[i + FALLING_OFFSET] )		// if currently falling...
					minDist = Math.min(minDist, tempInts[i]) ;
			
			// step c) set the fall distance (offset) for all currently falling to this new level.
			for ( int i = 0; i < numPieces; i++ )
				if ( tempBooleans[i + FALLING_OFFSET + PIECE_INDEX_OFFSET] )
					offsets.get(i).y -= (minDist - distanceFallen) ;	// move by the change
			distanceFallen = minDist ;
			
			// step d) unset 'recent'
			for ( int i = 0; i < FALLING_OFFSET; i++ )
				tempBooleans[i] = false ;
			
			// step e) set recent to those falling the minimum distance
			for ( int i = 0; i < FALLING_OFFSET; i++ )
				if ( tempBooleans[i + FALLING_OFFSET] && tempInts[i] == minDist )
					tempBooleans[i] = true ;
			
			// step f) set falling to current DIFFERENCE recent.
			for ( int i = 0; i < FALLING_OFFSET; i++ )
				if ( tempBooleans[i + FALLING_OFFSET] && tempBooleans[i] )
					tempBooleans[i + FALLING_OFFSET] = false ;
			
			// lastly, determine if any are still falling.
			hasFalling = false ;
			for ( int i = 0; i < numPieces; i++ )
				hasFalling = hasFalling || tempBooleans[i + PIECE_INDEX_OFFSET + FALLING_OFFSET] ;
		}
	}
	
	
	/**
	 * A general-purpose implementation of this method.  Makes no
	 * assumptions about lock system, clear system, or QInteraction behavior.
	 * @param field
	 * @param pieces
	 * @param offsets
	 * @param numPieces
	 * @throws QOrientationConflictException
	 * @throws GameSystemException
	 */
	private void safelyFallPieces_general( byte [][][] field, ArrayList<Piece> pieces, ArrayList<Offset> offsets, int numPieces ) throws QOrientationConflictException, GameSystemException {
		// Prepare the temporary field
		ArrayOps.copyInto(field, tempField) ;
		
		//System.err.println(TAG + " safelyFallPieces:\n" + Game.arrayAsString(tempField)) ;
		
		// Prepare booleans, ints
		if ( numPieces > tempBooleans.length )
			tempBooleans = new boolean[numPieces*2] ;
		if ( numPieces > tempInts.length )
			tempInts = new int[numPieces*2] ;
		for ( int i = 0; i < numPieces; i++ ) {
			tempBooleans[i] = true ;		// Indicates "still falling"
		}
		
		// Process.  It goes like this: calculate (at current offset) the
		// fall distance for every "still falling" piece.  Note the minimum.
		// Then, fall all pieces by the minimum amount, and lock those which
		// have no further to fall in place.  Repeat until nothing is still falling.
		//
		// By falling everything the minimum and only locking those that have
		// no further to go, we ensure that as soon as a piece lands, other
		// things can start landing on it.
		
		int minDist ;		
		boolean stillFalling = numPieces > 0 ;
		Offset o ;		// Temporary reference for convenience
		while ( stillFalling ) {
			// Calculate the distance each can fall.
			minDist = s.R*10 ;		// Hopefully nothing is higher than this!
			for ( int i = 0; i < numPieces; i++ ) {
				if ( tempBooleans[i] ) {
					// If this piece is still falling, find its distance.
					tempInts[i] = cs.spaceBelow(tempField, pieces.get(i), offsets.get(i), true) ;		// DO have a wall.
					//System.err.println(TAG + " spaceBelow " + tempInts[i]) ;
					if ( tempInts[i] < minDist ) {
						minDist = tempInts[i] ;
					}
				}	
			}
			
			// We have all the fall distances, AND the minimum.  Drop them all
			// (update offsets) and lock those which put us at minimum distance.
			// Note if there are any others left.
			stillFalling = false ;
			for ( int i = 0; i < numPieces; i++ ) {
				if ( tempBooleans[i] ) {
					// Drop it.
					o = offsets.get(i) ;
					o.y -= minDist ;
					
					// Lock if it's bottomed out, otherwise note that it's still falling.
					if ( tempInts[i] == minDist ) {
						// Fell as far as it will
						try {
							ls.lock(tempField, pieces.get(i), o) ;
							tempBooleans[i] = false ;
						} catch (Exception e) {
							if ( !adapter.game_shouldUseTimingSystem(this) ) {
								System.out.println("error in safely fall pieces (lock)") ;
								System.out.println(arrayAsString(s.blockField)) ;
								System.out.println("tempField") ;
								System.out.println(arrayAsString(tempField)) ;
							}
							//e.printStackTrace() ;
							GameSystemException exc = new GameSystemException(s.blockField, s.piece,
									"Exception from LockSystem in safelyFallPieces with info " + e.getMessage() + " statetrace\n" + e.getStackTrace().toString()) ;
							throw exc ;
						}
					}
					else {
						// Still falling
						stillFalling = true ;
					}
				}
			}
		}
	}
	
	
	
	/*
	 ***************************************************************************
	 *
	 * MISC. UTILITY FUNCTIONS
	 *
	 * Some convenient utility functions for examining Pieces, Offsets, etc.
	 * 
	 ***************************************************************************
	 */
	
	/**
	 * findQOrientationBlocks Examines the provided Piece for the specified
	 * QOrientation.  The offsets of every such block found in the piece will
	 * be noted in the ArrayList, which is extended as-needed.  Differs from
	 * findQCombinationBlocks in that it doesn't matter what QOrientation is
	 * in the "other" q-slot for the particular row/col.  The parameter
	 * 'offset' is used to describe the current offset of the Piece relative some
	 * blockField; the resulting blockOffsets will maintain this offset, placing
	 * the blocks in the same original position.  In other words, every (row,col)
	 * in blockOffsets references a (row,col) within the blockField.
	 * 
	 * NOTE: If a particular QOrientation occurs in both q-slots for a row/col,
	 * it will be noted only once in the blockOffsets.
	 * 
	 * @param piece The Piece whose .blocks will be examined.
	 * @param offset The current Offset of the Piece
	 * @param qo The QOrientation to look for.
	 * @param blockOffsets An ArrayList to which the discovered blockOffsets are placed.
	 * @param numOffsets The number of offsets already in-use in blockOffsets.  n >= 0.
	 * @return The number of offsets in-use after the call, N >= n.
	 * @throws Exception 
	 */
	public static final int findQOrientationBlocks(
			Piece piece, Offset offset, int qo, ArrayList<Offset> blockOffsets, int numOffsets) throws Exception {

		// Examine.
		int rows = piece.boundsUR.y - piece.boundsLL.y ;
		int cols = piece.boundsUR.x - piece.boundsLL.x ;
		for ( int r = 0; r < rows; r++ ) {
			int pr = r + piece.boundsLL.y ;
			for ( int c = 0; c < cols; c++ ) {
				int pc = c + piece.boundsLL.x ;
				if ( piece.blocks[0][pr][pc] == qo || piece.blocks[1][pr][pc] == qo ) {
					// Add this one.
					Offset o ;
					if ( numOffsets >= blockOffsets.size() ) {
						o = new Offset() ;
						blockOffsets.add(o) ;
					}
					else {
						o = blockOffsets.get(numOffsets) ;
					}
					o.setRow(r + offset.getRow()) ;
					o.setCol(c + offset.getCol()) ;
					numOffsets++ ;
				}
			}
		}
		return numOffsets ;
	}
	
	/**
	 * findQCombinationBlocks Examines the provided Piece for the specified
	 * QCombination.  The offsets of every such block found in the piece will
	 * be noted in the ArrayList, which is extended as-needed.  The parameter
	 * 'offset' is used to describe the current offset of the Piece relative some
	 * blockField; the resulting blockOffsets will maintain this offset, placing
	 * the blocks in the same original position.  In other words, every (row,col)
	 * in blockOffsets references a (row,col) within the blockField.
	 * 
	 * @param piece The Piece whose .blocks will be examined.
	 * @param offset The current Offset of the Piece
	 * @param qc The QCombination to look for.
	 * @param blockOffsets An ArrayList to which the discovered blockOffsets are placed.
	 * @param numOffsets The number of offsets already in-use in blockOffsets.  n >= 0.
	 * @return The number of offsets in-use after the call, N >= n.
	 * @throws Exception 
	 */
	public static final int findQCombinationBlocks(
			Piece piece, Offset offset, int qc, ArrayList<Offset> blockOffsets, int numOffsets) throws Exception {
		// Examine.
		int rows = piece.boundsUR.y - piece.boundsLL.y ;
		int cols = piece.boundsUR.x - piece.boundsLL.x ;
		for ( int r = 0; r < rows; r++ ) {
			int pr = r + piece.boundsLL.y ;
			for ( int c = 0; c < cols; c++ ) {
				int pc = c + piece.boundsLL.x ;
				if ( QCombinations.encode(piece.blocks, pr, pc) == qc ) {
					// Add this one.
					Offset o ;
					if ( numOffsets >= blockOffsets.size() ) {
						o = new Offset() ;
						blockOffsets.add(o) ;
					}
					else {
						o = blockOffsets.get(numOffsets) ;
					}
					o.setRow(r + offset.getRow()) ;
					o.setCol(c + offset.getCol()) ;
					numOffsets++ ;
				}
			}
		}
		return numOffsets ;
	}
	
	/**
	 * bestColumnToUnlock Determines the best column to unlock within the field,
	 * where "best" is defined as the unlock which results in the most "block-distance"
	 * covered by the resulting fall.  If there is no column where unlocking it
	 * produces a fall, returns -1
	 * @param field The field within which columns are checked
	 * @return The column to unlock, or -1 if no options produce a fall
	 * @throws QOrientationConflictException
	 * @throws GameSystemException
	 */
	public final int bestColumnToUnlock( byte [][][] field ) throws QOrientationConflictException, GameSystemException {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		
		// Within the provided field, which column provides the maximum
		// benefit when unlocked?
		
		// "Best" is a difficult consideration.  As a heuristic, the "best"
		// unlock is the one that results in greatest block-distance fallen
		// (e.g. if 2 blocks fall 3, and 1 block falls 1, that is a block-distance
		// of 7).
		
		// A few things worth considering.  First, the field given might
		// not be in a stable state, and blocks will fall regardless of
		// whether we intervene.  To correct this, perform a fall of these
		// components BEFORE checking the columns.
		
		// Next, use the resulting version of the field to test the effects
		// of unlocking each column (remember to revert after each).  Perform
		// a safe fall upon each unlock and check the distance, multiplied by
		// the number of blocks.
		
		// Finally, return an integer indicating which column was the best option.
		int fieldRows = field[0].length ;
		int fieldCols = field[0][0].length ;
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = 0; r < fieldRows; r++ ) {
				for ( int c = 0; c < fieldCols; c++ ) {
					bctuRevertToField[q][r][c] = field[q][r][c] ;
				}
			}
		}
		
		int numPieces ;
		
		// Perform the fall.
		numPieces = ls.unlock(bctuRevertToField, bctuPieces, bctuOffsets, 0) ;
		if ( numPieces > 0 ) {
			this.safelyFallPieces(bctuRevertToField, bctuPieces, bctuOffsets, numPieces) ;
			// Offsets have been updated; place them.
			for ( int i = 0; i < numPieces; i++ ) {
				ls.lock(bctuRevertToField, bctuPieces.get(i), bctuOffsets.get(i)) ;
			}
		}
		
		// We have the "stable state," kindof, for the block field.
		// TODO: Consider the state after any clears, maybe?
		int maxBlockDistance = 0 ;
		int blockDistance ;
		int bestColumn = -1 ;
		Offset coord = new Offset() ;
		coord.setRow(-1) ;
		for ( int columnToUnlock = 0; columnToUnlock < fieldCols; columnToUnlock++ ) {
			// Copy the revertTo...
			for ( int q = 0; q < 2; q++ ) {
				for ( int r = 0; r < fieldRows; r++ ) {
					for ( int c = 0; c < fieldCols; c++ ) {
						bctuField[q][r][c] = bctuRevertToField[q][r][c] ;
					}
				}
			}
			
			// Unlock the column, and the resulting floating blocks.
			coord.setCol(columnToUnlock) ;
			numPieces = ls.unlockColumnAbove(bctuField, coord, bctuPieces, bctuOffsets, 0) ;
			numPieces = ls.unlock(bctuField, bctuPieces, bctuOffsets, numPieces) ;
			// Copy the offsets so we can get fall distance later
			for ( int i = 0; i < bctuOffsets.size(); i++ ) {
				if ( i < bctuOffsetsAfterFall.size() )
					bctuOffsetsAfterFall.get(i).takeVals(bctuOffsets.get(i)) ;
				else {
					Offset o = new Offset() ;
					o.takeVals(bctuOffsets.get(i)) ;
					bctuOffsetsAfterFall.add(o) ;
				}
			}
			
			// Calculate the "weight" of this unlock.
			safelyFallPieces(bctuField, bctuPieces, bctuOffsetsAfterFall, numPieces) ;
			blockDistance = 0 ;
			for ( int i = 0; i < numPieces; i++ ) {
				blockDistance += bctuPieces.get(i).numBlocks()
				                        * ( bctuOffsets.get(i).getRow()
				                        		- bctuOffsetsAfterFall.get(i).getRow() ) ;
			}
			
			// Is this better than the best?
			if ( blockDistance > maxBlockDistance ) {
				maxBlockDistance = blockDistance ;
				bestColumn = columnToUnlock ;
			}
		}
		
		// Returns -1 if no column qualifies.
		return bestColumn ;
	}
	
	
	/*
	 ***************************************************************************
	 *
	 * ERROR CHECKING UTILITY FUNCTIONS
	 *
	 * Some convenient utility functions for error checking and debugging.
	 * 
	 ***************************************************************************
	 */
	public static String arrayAsString( byte [][][] ar ) {
		// Represent blockField as a string...
		String bfs = "" ;
		for ( int r = ar[0].length-1; r >= 0; r-- ) {
			for ( int c = 0; c < ar[0][0].length; c++ ) {
				/*try {
					bfs = bfs + QCombinations.encode(ar, r, c) + "\t" ;
				} catch (Exception e) {
					bfs = bfs + "_" + "\t" ;
				}*/
				int q0val = ar[0][r][c] ;
				int q1val = ar[1][r][c] ;
				bfs = bfs + (q0val < 10 ? "." + q0val : q0val) + ":" + (q1val < 10 ? q1val + "." : q1val) + "\t" ;
			}
			bfs = bfs + "\n" ;
		}
		return bfs ;
	}
	
	public static String arrayAsString( boolean [][][] ar ) {
		// Represent blockField as a string...
		String bfs = "" ;
		for ( int r = ar[0].length-1; r >= 0; r-- ) {
			for ( int c = 0; c < ar[0][0].length; c++ ) {
				if ( !ar[0][r][c] && !ar[1][r][c] )
					bfs = bfs + "0\t" ;
				if ( ar[0][r][c] && !ar[1][r][c] )
					bfs = bfs + "1\t" ;
				if ( !ar[0][r][c] && ar[1][r][c] )
					bfs = bfs + "2\t" ;
				if ( ar[0][r][c] && ar[1][r][c] )
					bfs = bfs + "3\t" ;
			}
			bfs = bfs + "\n" ;
		}
		return bfs ;
	}
	
	
	/*
	 ***************************************************************************
	 *
	 * TRIGGERABLE FUNCTIONS
	 *
	 * Some convenient utility functions for error checking and debugging.
	 * 
	 ***************************************************************************
	 */
	
	@Override
	public void pullTrigger(int triggerNum) {
		pullTrigger( triggerNum, (Object [])null ) ;
	}

	@Override
	public void pullTrigger(int triggerNum, Object... params) {
		
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		
		// Might want to print that a trigger was pulled.
		
		boolean didUnlockColumn = false ;
		
		if ( triggerNum == TRIGGER_SUCCESS_UNLOCK_BEST_COLUMN || triggerNum == TRIGGER_SUCCESS_UNLOCK_BEST_COLUMN_OR_DROP_BLOCKS_IN_VALLEYS ) {
			// Find the best column to unlock, and note that it
			// should be unlocked.  We can do this with a delayed
			// column unlock offset set to (-1,c).
			int column = -10 ;
			try {
				column = bestColumnToUnlock(s.blockField) ;
				if ( column > -1 ) {
					// Add it to unlockColumnAbove
					Offset o ;
					if ( s.unlockColumnAbove.size() > s.numUnlockColumnAbove+s.numUnlockColumnAboveAlreadyUnlocked )
						o = s.unlockColumnAbove.get(s.numUnlockColumnAbove+s.numUnlockColumnAboveAlreadyUnlocked) ;
					else {
						o = new Offset() ;
						s.unlockColumnAbove.add(o) ;
					}
					
					o.setRow(-1) ;
					o.setCol(column) ;
					s.numUnlockColumnAbove++ ;
					didUnlockColumn = true ;
					
					gevents.setHappened(GameEvents.EVENT_PIECE_SUCCESS) ;
					s.geventsLastTick.setHappened(GameEvents.EVENT_PIECE_SUCCESS) ;
					pieceHistory.setSuccess() ;
				}
			} catch( Exception e ) {
				e.printStackTrace() ;
			} finally {
				// Might want to print something here
			}
		}
		
		if ( triggerNum == TRIGGER_SUCCESS_DROP_BLOCKS_IN_VALLEYS || (triggerNum == TRIGGER_SUCCESS_UNLOCK_BEST_COLUMN_OR_DROP_BLOCKS_IN_VALLEYS && !didUnlockColumn) ) {
			s.numBlocksForValleys += (Integer)(params[0]) ;
			gevents.setHappened(GameEvents.EVENT_PIECE_SUCCESS) ;
			s.geventsLastTick.setHappened(GameEvents.EVENT_PIECE_SUCCESS) ;
			pieceHistory.setSuccess() ;
		}
		
		else if ( triggerNum == TRIGGER_SUCCESS_IF_UNLOCK_COLUMN_ABOVE_F_PIECE_BLOCKS ) {
			// Find the flash blocks within the piece at its current
			// position, and set everything to unlock ABOVE them.
			// Luckily we have a function that does exactly that.
			// Check for both F0 and F1, it could be either.
			try {
				int num = s.numUnlockColumnAbove ;
				s.numUnlockColumnAbove = 
					Game.findQOrientationBlocks(s.piece, s.offset, QOrientations.F0,
							s.unlockColumnAbove, s.numUnlockColumnAbove+s.numUnlockColumnAboveAlreadyUnlocked) 
							- s.numUnlockColumnAboveAlreadyUnlocked;
				s.numUnlockColumnAbove = 
					Game.findQOrientationBlocks(s.piece, s.offset, QOrientations.F1,
							s.unlockColumnAbove, s.numUnlockColumnAbove+s.numUnlockColumnAboveAlreadyUnlocked)
							- s.numUnlockColumnAboveAlreadyUnlocked;
				for ( int i = num; i < s.numUnlockColumnAbove; i++ ) {
					ArrayOps.copyInto(s.blockField, this.tempField) ;
					int numChunks = ls.unlockColumnAbove(
							tempField, s.unlockColumnAbove.get(i),
							s.chunks, s.chunkOriginalOffsets, s.numChunks) ;
					// should be safe, because we don't increment numChunks.
					if ( numChunks > s.numChunks ) {
						gevents.setHappened(GameEvents.EVENT_PIECE_SUCCESS) ;
						s.geventsLastTick.setHappened(GameEvents.EVENT_PIECE_SUCCESS) ;
						pieceHistory.setSuccess() ;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		else if ( triggerNum == TRIGGER_CANCEL_PIECE_LOCK ) {
			s.lockPiece = false ;
		}
		
		else if ( triggerNum == TRIGGER_SUCCESS_LOCK_AND_METAMORPHOSIS_ACTIVATE ) {
			// FIRST do all the steps to lock the piece in place.  Two possible
			// approaches: one, we manually perform all the steps right here,
			// or two, we set a flag indicating that we should proceed through
			// the normal lock steps and then Activate before returning from
			// the tick.
			
			// We prefer the second approach.
			s.lockThenActivate = true ;
			gevents.setHappened(GameEvents.EVENT_PIECE_SUCCESS) ;
			s.geventsLastTick.setHappened(GameEvents.EVENT_PIECE_SUCCESS) ;
			pieceHistory.setSuccess() ;
		}
		
		else if ( triggerNum == TRIGGER_FAILURE_LOCK_AND_METAMORPHOSIS_DEACTIVATE ) {
			// FIRST do all the steps to lock the piece in place.  Two possible
			// approaches: one, we manually perform all the steps right here,
			// or two, we set a flag indicating that we should proceed through
			// the normal lock steps and then Activate before returning from
			// the tick.
			
			// We prefer the second approach.
			s.lockThenDeactivate = true ;
			pieceHistory.setFailure() ;
		}
		
		else if ( triggerNum == TRIGGER_METAMORPHOSIS_ACTIVATE_BEFORE_END_CYCLE ) {
			s.activateThenEndCycle = true ;
		}
		
		else if ( triggerNum == TRIGGER_SUCCESS_NO_OTHER_EFFECT ) {
			gevents.setHappened(GameEvents.EVENT_PIECE_SUCCESS) ;
			s.geventsLastTick.setHappened(GameEvents.EVENT_PIECE_SUCCESS) ;
			pieceHistory.setSuccess() ;
		}
		
		else if ( triggerNum == TRIGGER_FAILURE_NO_OTHER_EFFECT ) {
			pieceHistory.setFailure() ;
		}
		
		
	}
	
	private void makeScreencapQuantroTypes() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// suspend above the top to avoid drawing bottom edges.
		QCombinations.setAs(s.blockField, 2, 0, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 2, 4, QCombinations.F0) ;
		QCombinations.setAs(s.blockField, 2, 5, QCombinations.ST_INACTIVE) ;
		QCombinations.setAs(s.blockField, 2, 6, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 2, 7, QCombinations.SL_INACTIVE) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_SCREW, 0, QCombinations.SL) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeScreencapQuantroTypesSpread() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// suspend above the top to avoid drawing bottom edges.
		QCombinations.setAs(s.blockField, 2, 0, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 4, 1, QCombinations.F0) ;
		QCombinations.setAs(s.blockField, 4, 3, QCombinations.ST_INACTIVE) ;
		QCombinations.setAs(s.blockField, 4, 5, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 4, 7, QCombinations.SL_INACTIVE) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_SCREW, 0, QCombinations.SL) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	
	
	
	private void makeScreencapRetroTypes() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// suspend above the top to avoid drawing bottom edges.
		QCombinations.setAs(s.blockField, 2, 0, QCombinations.R0) ;
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.R1) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.R2) ;
		QCombinations.setAs(s.blockField, 2, 3, QCombinations.R3) ;
		QCombinations.setAs(s.blockField, 2, 4, QCombinations.R4) ;
		QCombinations.setAs(s.blockField, 2, 5, QCombinations.R5) ;
		QCombinations.setAs(s.blockField, 2, 6, QCombinations.R6) ;
		
		QCombinations.setAs(s.blockField, 4, 1, QCombinations.PUSH_DOWN_ACTIVE) ;
		QCombinations.setAs(s.blockField, 4, 3, QCombinations.PUSH_UP_ACTIVE) ;
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeScreencapQuantroBullets() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// suspend above the top to avoid drawing bottom edges.
		QCombinations.setAs(s.blockField, 1, 0, QOrientations.S0) ;
		QCombinations.setAs(s.blockField, 1, 1, QOrientations.F0) ;
		QCombinations.setAs(s.blockField, 1, 3, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.SL_ACTIVE) ;
		QCombinations.setAs(s.blockField, 2, 0, QOrientations.ST_INACTIVE) ;
		QCombinations.setAs(s.blockField, 2, 1, QOrientations.S1) ;
		QCombinations.setAs(s.blockField, 3, 1, QCombinations.F1) ;
		QCombinations.setAs(s.blockField, 3, 3, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 3, 5, QCombinations.SL_INACTIVE) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_FLASH, QCombinations.F0) ;
		try {
			rs.turnCW(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeScreencapRainbowBullets() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// suspend above the top to avoid drawing bottom edges.
		QCombinations.setAs(s.blockField, 0, 0, QOrientations.R0) ;
		QCombinations.setAs(s.blockField, 0, 1, QOrientations.R1) ;
		QCombinations.setAs(s.blockField, 0, 2, QOrientations.R2) ;
		QCombinations.setAs(s.blockField, 0, 3, QOrientations.R3) ;
		QCombinations.setAs(s.blockField, 1, 0, QOrientations.R4) ;
		QCombinations.setAs(s.blockField, 1, 1, QOrientations.R5) ;
		QCombinations.setAs(s.blockField, 1, 2, QOrientations.R6) ;
		QCombinations.setAs(s.blockField, 1, 3, QOrientations.RAINBOW_BLAND) ;
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeScreencapLogo() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// suspend above the top to avoid drawing bottom edges.
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.ST_INACTIVE) ;
		QCombinations.setAs(s.blockField, 2, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 3, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 2, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 4, 1, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 4, 2, QCombinations.S0) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_FLASH, QCombinations.F0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeScreencapMonochromeClear() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// suspend above the top to avoid drawing bottom edges.
		QCombinations.setAs(s.blockField, 2, 0, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 2, 3, QCombinations.SL_ACTIVE) ;
		QCombinations.setAs(s.blockField, 2, 4, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 2, 5, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 2, 6, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 2, 7, QCombinations.S1) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_SCREW, 0, QCombinations.SL) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeScreencap180Turn() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// a place for a 7 to fit...
		
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.RAINBOW_BLAND) ;
		
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.RAINBOW_BLAND) ;
		
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 1, 3, QCombinations.RAINBOW_BLAND) ;
		
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 1, 6, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 1, 7, QCombinations.RAINBOW_BLAND) ;
		
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.RAINBOW_BLAND) ;
		
		QCombinations.setAs(s.blockField, 2, 5, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 2, 6, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 2, 7, QCombinations.RAINBOW_BLAND) ;
		
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GUN, QCombinations.R0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeScreencapKickTurn() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// a place for a gamma to fit...
		
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.RAINBOW_BLAND) ;
		
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.RAINBOW_BLAND) ;
	
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.RAINBOW_BLAND) ;
		
		QCombinations.setAs(s.blockField, 1, 4, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 1, 6, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 1, 7, QCombinations.RAINBOW_BLAND) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GAMMA, QCombinations.R0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeScreencapLeanTurn() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// a place for a gamma to fit...
		
		QCombinations.setAs(s.blockField, 0, 4, QCombinations.RAINBOW_BLAND) ;
		QCombinations.setAs(s.blockField, 1, 4, QCombinations.RAINBOW_BLAND) ;
		
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_T, QCombinations.R0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeTestBlackjack() {
		ginfo.level = 21 ;
	}
	
	
	private void makeTestJackpot() {
		makeTestSuperCritical() ;
		ginfo.multiplier = 20 ;
	}
	
	
	private void makeTestSuperCritical() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// Verify that supercritical is possible (though difficult) on Quantro.
		// we make a setup with an ideal "unlock" column (far right).  This
		// setup is a stair-step or "Teeth" configuration, such that unlocking
		// from the rightmost column and falling will cause a massive number
		// of line clears.
		
		// we do 14 rows of this.
		for ( int r = 0; r < 14; r++ ) {
			int emptyC = 6 - (r % 7) ;
			for ( int c = 0; c < this.C(); c++ ) {
				QCombinations.setAs(s.blockField, r, c, c == emptyC ? QCombinations.NO : QCombinations.SS) ;
			}
		}
		
		// Next, a place for a square.
		for ( int r = 14; r < 16; r++ ) {
			for ( int c = 0; c < this.C(); c++ ) {
				int qc ;
				if ( c == 3 || c == 4 ) {
					qc = QCombinations.NO ;
				} else {
					qc = QCombinations.SS ;
				}
				QCombinations.setAs(s.blockField, r, c, qc) ;
			}
		}
		
		s.nextPieces[0].defaultType = s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_SQUARE, QCombinations.ST) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeTestQuantroAllGlows() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// This setup allows for pretty much every glow effect: locks
		// in both panes, clears in both, enters in both, unlocks, activations,
		// etc.
		
		// There are three basic tiers here.  At the bottom we have full, except
		// for two obvious gaps.  These gaps (with stuff above them) allows for
		// two good opportunities to unlock the content.
		// Above this, we have almost a full row of blocks, with an obvious
		// position for new blocks (we use Ts).  This lets us test 'lock-in-place.'
		// Above this, we have two pillars: one in each pane, offset by 2
		// columns.  This allows a FUSED BEND to lock between and trigger a
		// metamorphosis.
		
		// First: fill except for two columns (4 and 6)
		for ( int r = 0; r < 2; r++ ) {
			for ( int c = 0; c < this.C(); c++ ) {
				if ( c != 4 && c != 6 )
					QCombinations.setAs(s.blockField, r, c, QCombinations.SS) ;
			}
		}
		
		// Second: fill except for an easily filled T-space.
		for ( int c = 0; c < this.C(); c++ ) {
			if ( c != 1 )
				QCombinations.setAs(s.blockField, 2, c, QCombinations.SS) ;
		}
		for ( int c = 0; c < this.C(); c++ ) {
			if ( c > 2 )
				QCombinations.setAs(s.blockField, 3, c, QCombinations.SS) ;
		}
		// One more row to clear with ST.
		for ( int c = 0; c < this.C(); c++ ) {
			if ( c > 3 )
				QCombinations.setAs(s.blockField, 4, c, QCombinations.SS) ;
		}
		
		// Third: pile up some columns.  Connect them on the sides
		// so there's space below.
		QCombinations.setAs(s.blockField, 5, 5, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 6, 5, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 5, 7, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 6, 7, QCombinations.S1) ;
		
		for ( int r = 6; r < 12; r++ ) {
			QCombinations.setAs(s.blockField, r, 4, QCombinations.S0) ;
			QCombinations.setAs(s.blockField, r, 6, QCombinations.S1) ;
		}
		
		// Now, the pieces we'll use for this.  In the nextPiece queue, we
		// use 2 Ts, one of each color.  In the reservePiece queue, we
		// use ST-line, then FLASH, then any SL (doesn't matter), then
		// an SL bend to activate.
		s.nextPieces[0].defaultType = s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_T, QCombinations.S0) ;
		s.nextPieces[1].defaultType = s.nextPieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_T, QCombinations.S1) ;
		s.reservePieces[0].defaultType = s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QCombinations.ST) ;
		s.reservePieces[1].defaultType = s.reservePieces[1].type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_FLASH, QCombinations.F0) ;
		s.reservePieces[2].defaultType = s.reservePieces[2].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_BRANCH, 0, QCombinations.SL) ;
		s.reservePieces[3].defaultType = s.reservePieces[3].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_S, 0, QCombinations.SL) ;
		
		try {
			for ( int i = 0; i < 4; i++ ) {
				if ( i < 2 )
					rs.turn0(s.nextPieces[i]) ;
				rs.turn0(s.reservePieces[i]) ;
			}
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeTestRetroAllGlows() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// This setup allows for pretty much every glow effect: locks,
		// clears, enters.
		
		// Line one: lots of testingRandomom blocks.
		for ( int c = 0; c < this.C(); c++ ) {
			if ( c != 0 )
				QCombinations.setAs(s.blockField, 0, c, QCombinations.R0 + testingRandom.nextInt(7)) ;
		}
		
		// Line two and three: lots of testingRandomom blocks, with space for S blocks.
		for ( int r = 1; r < 3; r++ ) {
			for ( int c = 0; c < this.C(); c++ ) {
				if ( c != r && c != r + 1 && c != r + 4 && c != r + 5 ) {
					QCombinations.setAs(s.blockField, r, c, QCombinations.R0 + testingRandom.nextInt(7)) ;
				}
			}
		}
		
		// Up above: big columns of blocks, so the clear glow can overlap above.
		for ( int r = 3; r < 10; r++ ) {
			for ( int c = 0; c < this.C(); c++ ) {
				if ( c == 0 || ( 2 < c && c < 5 ) || 6 < c ) {
					QCombinations.setAs(s.blockField, r, c, QCombinations.R0 + testingRandom.nextInt(7)) ;
				}
			}
		}
			
		s.nextPieces[0].defaultType = s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_S, QCombinations.R0) ;
		s.nextPieces[1].defaultType = s.nextPieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_S, QCombinations.R3) ;
	
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.nextPieces[1]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeTestFallingChunks() {
		ArrayOps.setEmpty(s.blockField) ;
		
		int qc = GameModes.numberQPanes( this.ginfo.mode ) == 1
				? QCombinations.R4 : QCombinations.SS ;
		for ( int c = 0; c < this.C() - 1; c++ ) {
			QCombinations.setAs(s.blockField, 0, c, qc ) ;
			QCombinations.setAs(s.blockField, 2, c, qc ) ;
			
			QCombinations.setAs(s.blockField, 1, c, c % 2 == 0 ? 0 : qc ) ;
			QCombinations.setAs(s.blockField, 3, c, c % 2 == 1 ? 0 : qc ) ;
		}
		
		int qc0 = GameModes.numberQPanes( this.ginfo.mode ) == 1
				? QCombinations.R0 : QCombinations.S0 ;
		int qc1 = GameModes.numberQPanes( this.ginfo.mode ) == 1
				? QCombinations.R0 : QCombinations.S1 ;

		s.nextPieces[0].defaultType = s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, qc0) ;
		s.reservePieces[0].defaultType = s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, qc1) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.reservePieces[0]) ;
			
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestPentoST() {
		ArrayOps.setEmpty(s.blockField) ;
		
		
		// make something with an unlockable section
		for ( int c = 0; c < 8; c++ ) {
			QCombinations.setAs(s.blockField, 0, c, c % 2 == 0 ? QCombinations.S0 : QCombinations.S1) ;
			QCombinations.setAs(s.blockField, 1, c, c == 0 ? QCombinations.NO : QCombinations.SS) ;
			QCombinations.setAs(s.blockField, 2, c, c < 3 ? QCombinations.S0 : QCombinations.NO) ;
		}
		
		s.nextPieces[0].type = PieceCatalog.encodePentomino(PieceCatalog.PENTO_CAT_I, QCombinations.ST) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestPentoUL() {
		ArrayOps.setEmpty(s.blockField) ;
		
		
		// make something with an unlockable section
		for ( int c = 0 ; c < 8; c++ ) {
			for ( int r = 0; r < c; r++ ) {
				QCombinations.setAs(s.blockField, r, c, QCombinations.SS) ;
			}
		}
		
		s.nextPieces[0].type = PieceCatalog.encodePentomino(PieceCatalog.PENTO_CAT_I, QCombinations.UL) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeTestStrategicGarbageRetro1() {
		ArrayOps.setEmpty(s.blockField) ;
		
		for ( int c = 1; c < 10; c++ )
			QCombinations.setAs(s.blockField, 0, c, QCombinations.R0) ;
		for ( int c = 1; c < 10; c++ )
			QCombinations.setAs(s.blockField, 1, c, QCombinations.R0) ;
		for ( int c = 2; c < 10; c++ )
			QCombinations.setAs(s.blockField, 2, c, QCombinations.R0) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GAMMA, QCombinations.R3) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	
	
	private void makeTestStrategicGarbageRetro2() {
		ArrayOps.setEmpty(s.blockField) ;
		
		for ( int r = 0; r < 6; r++ )
			for ( int c = 0; c < 9; c++ )
				QCombinations.setAs(s.blockField, r, c, QCombinations.R0) ;
		
		for ( int c = 1; c < 10; c++ )
			QCombinations.setAs(s.blockField, 6, c, QCombinations.R0) ;
		for ( int c = 1; c < 10; c++ )
			QCombinations.setAs(s.blockField, 7, c, QCombinations.R0) ;
		for ( int c = 2; c < 10; c++ )
			QCombinations.setAs(s.blockField, 8, c, QCombinations.R0) ;
		
		for ( int r = 9; r < 15; r++ )
			QCombinations.setAs(s.blockField, r, 9, QCombinations.R0) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GAMMA, QCombinations.R3) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeTestStrategicGarbageQuantro1() {
		ArrayOps.setEmpty(s.blockField) ;
		
		for ( int c = 0; c < 8; c++ ) {
			if ( c != 4 && c != 5 )
				QCombinations.setAs(s.blockField, 0, c, QCombinations.SS) ;
			if ( c != 3 && c != 4 )
				QCombinations.setAs(s.blockField, 1, c, QCombinations.SS) ;
		}
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.S0) ;
		s.nextPieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.S1) ;
		s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.ST) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.nextPieces[1]) ;
			rs.turn0(s.reservePieces[0]) ;
			
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestStrategicGarbageQuantro2() {
		ArrayOps.setEmpty(s.blockField) ;
		
		for ( int c = 0; c < 8; c++ ) {
			if ( c != 4 && c != 5 )
				QCombinations.setAs(s.blockField, 0, c, QCombinations.SS) ;
			if ( c != 3 && c != 4 )
				QCombinations.setAs(s.blockField, 1, c, QCombinations.S0) ;
		}
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.S0) ;
		s.nextPieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.S1) ;
		s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.ST) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.nextPieces[1]) ;
			rs.turn0(s.reservePieces[0]) ;
			
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestStrategicGarbageQuantro3() {
		ArrayOps.setEmpty(s.blockField) ;
		
		for ( int r = 0; r < 6; r++ )
			for ( int c = 0; c < 7; c++ )
				QCombinations.setAs(s.blockField, r, c, QCombinations.SS) ;
		
		for ( int c = 0; c < 8; c++ ) {
			if ( c != 4 && c != 5 )
				QCombinations.setAs(s.blockField, 6, c, QCombinations.SS) ;
			if ( c != 3 && c != 4 )
				QCombinations.setAs(s.blockField, 7, c, QCombinations.SS) ;
		}
		
		for ( int r = 8; r < 14; r++ )
			QCombinations.setAs(s.blockField, r, 7, QCombinations.SS) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.S0) ;
		s.nextPieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.S1) ;
		s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.ST) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.nextPieces[1]) ;
			rs.turn0(s.reservePieces[0]) ;
			
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestStrategicGarbageQuantro4() {
		ArrayOps.setEmpty(s.blockField) ;
		
		for ( int r = 0; r < 6; r++ )
			for ( int c = 0; c < 7; c++ )
				QCombinations.setAs(s.blockField, r, c, QCombinations.SS) ;
		
		for ( int c = 0; c < 8; c++ ) {
			if ( c != 4 && c != 5 )
				QCombinations.setAs(s.blockField, 6, c, QCombinations.SS) ;
			if ( c != 3 && c != 4 )
				QCombinations.setAs(s.blockField, 7, c, QCombinations.S0) ;
			
			if ( c == 6 || c == 7 )
				QCombinations.setAs(s.blockField, 7, c, QCombinations.SS) ;
		}
		
		for ( int r = 8; r < 14; r++ )
			QCombinations.setAs(s.blockField, r, 7, QCombinations.SS) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.S0) ;
		s.nextPieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.S1) ;
		s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.ST) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.nextPieces[1]) ;
			rs.turn0(s.reservePieces[0]) ;
			
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestSpecialTrigger() {
		ArrayOps.setEmpty(s.blockField) ;
		
		for ( int row = 0; row < 2; row++ ) {
			for ( int col = 0; col < 6; col++ )
				QCombinations.setAs(s.blockField, row, col, QCombinations.SS) ;
			QCombinations.setAs(s.blockField, row, 6, QCombinations.S0) ;
		}
		for ( int col = 0; col < 7; col++ )
			QCombinations.setAs(s.blockField, 2, col, QCombinations.S0) ;
		for ( int col = 6; col < 8; col++ )
			QCombinations.setAs(s.blockField, 3, col, QCombinations.S0) ;
		
		s.reservePieces[0].type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_FLASH, QCombinations.F0) ;
		s.reservePieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_SQUARE, QCombinations.UL) ;
		
		try {
			rs.turn0(s.reservePieces[0]) ;
			rs.turn0(s.reservePieces[1]) ;
			
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void makeTestTurnLine() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// X X X X X X
		// X X X X X X X X
		// X X X X X X
		// _ _ _ _ _ _ _ _ _ _ 
		
		for ( int row = 0; row < 3; row++ ) {
			for ( int i = 0; i < 8; i++ ) {
				if ( i < 6 || row == 1 )
					QCombinations.setAs(s.blockField, row, i, QCombinations.R1) ;
			}
		}
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QCombinations.R3) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeTestTurnShake1() {
		ArrayOps.setEmpty(s.blockField) ;
		
		//                 X X
		//                 X X
		//                   X
		//               X   X
		// _ _ _ _ _ _ _ _ _ _ 
		
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.R0) ;
		QCombinations.setAs(s.blockField, 0, 9, QCombinations.R0) ;
		QCombinations.setAs(s.blockField, 1, 9, QCombinations.R0) ;
		QCombinations.setAs(s.blockField, 2, 8, QCombinations.R0) ;
		QCombinations.setAs(s.blockField, 2, 9, QCombinations.R0) ;
		QCombinations.setAs(s.blockField, 3, 8, QCombinations.R0) ;
		QCombinations.setAs(s.blockField, 3, 9, QCombinations.R0) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_S, QCombinations.R3) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestTurnShake2() {
		ArrayOps.setEmpty(s.blockField) ;
		
		//               X X X
		//               X   X
		//                   X
		//             X   X X
		// _ _ _ _ _ _ _ _ _ _ 
		
		for ( int i = 0; i < 7; i++ )
			QCombinations.setAs(s.blockField, 0, i, QCombinations.R1) ;
		QCombinations.setAs(s.blockField, 0, 8, QCombinations.R1) ;
		QCombinations.setAs(s.blockField, 0, 9, QCombinations.R1) ;
		QCombinations.setAs(s.blockField, 1, 9, QCombinations.R1) ;
		QCombinations.setAs(s.blockField, 2, 9, QCombinations.R1) ;
		QCombinations.setAs(s.blockField, 3, 7, QCombinations.R1) ;
		QCombinations.setAs(s.blockField, 3, 8, QCombinations.R1) ;
		QCombinations.setAs(s.blockField, 3, 9, QCombinations.R1) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.R5) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestTurnShake3() {
		ArrayOps.setEmpty(s.blockField) ;
		
		//                 X X
		//                   X
		// X X X X X X X     X
		// _ _ _ _ _ _ _ _ _ _ 
		
		for ( int i = 0; i < 7; i++ )
			QCombinations.setAs(s.blockField, 0, i, QCombinations.R2) ;
		QCombinations.setAs(s.blockField, 0, 9, QCombinations.R2) ;
		QCombinations.setAs(s.blockField, 1, 9, QCombinations.R2) ;
		QCombinations.setAs(s.blockField, 2, 8, QCombinations.R2) ;
		QCombinations.setAs(s.blockField, 2, 9, QCombinations.R2) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_SQUARE, QCombinations.R6) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestTurnShake4() {
		ArrayOps.setEmpty(s.blockField) ;
		
		//               X X X
		//                 X X
		// X X X X X X       X
		// _ _ _ _ _ _ _ _ _ _ 
		
		for ( int i = 0; i < 6; i++ )
			QCombinations.setAs(s.blockField, 0, i, QCombinations.R3) ;
		QCombinations.setAs(s.blockField, 0, 9, QCombinations.R3) ;
		QCombinations.setAs(s.blockField, 1, 8, QCombinations.R3) ;
		QCombinations.setAs(s.blockField, 1, 9, QCombinations.R3) ;
		QCombinations.setAs(s.blockField, 2, 7, QCombinations.R3) ;
		QCombinations.setAs(s.blockField, 2, 8, QCombinations.R3) ;
		QCombinations.setAs(s.blockField, 2, 9, QCombinations.R3) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_T, QCombinations.R0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestLockClip() {
		ArrayOps.setEmpty(s.blockField) ;
		
		for ( int i = 1; i < 7; i++ ) 
			QCombinations.setAs(s.blockField, 0, i, QCombinations.S0) ;
		for ( int i = 1; i < 6; i++ ) 
			QCombinations.setAs(s.blockField, 1, i, QCombinations.S0) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_T, QCombinations.S0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeTestSTFailure() {
		ArrayOps.setEmpty(s.blockField) ;
		
		for ( int i = 0; i < 2; i++ )
			for ( int j = 1; j < 8; j++ ) 
				QCombinations.setAs(s.blockField, i, j, i == 0 || j == 3 ? QCombinations.SS : QCombinations.S0) ;
		
		QCombinations.setAs(s.blockField, 2, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 3, QCombinations.S1) ;
		
		
		s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QCombinations.ST) ;
		try {
			rs.turn0(s.reservePieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestSTFailureAlt() {
		ArrayOps.setEmpty(s.blockField) ;
		
		for ( int i = 0; i < 5; i++ )
			for ( int j = 0; j < 7; j++ )
				QCombinations.setAs(s.blockField, i, j, QCombinations.SS) ;
		
		for ( int i = 5; i < 7; i++ )
			for ( int j = 1; j < 8; j++ ) 
				QCombinations.setAs(s.blockField, i, j, i == 5 || j == 3 ? QCombinations.SS : QCombinations.S0) ;
		
		QCombinations.setAs(s.blockField, 7, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 8, 3, QCombinations.S1) ;
		
		
		s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QCombinations.ST) ;
		try {
			rs.turn0(s.reservePieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	
	
	private void makeTestLockGlow() {
		ArrayOps.setEmpty(s.blockField) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(
				PieceCatalog.TETRO_CAT_LINE,
				GameModes.numberQPanes(ginfo) == 1 ? QCombinations.R0 : QCombinations.S0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void makeTestComplexLockGlow() {
		ArrayOps.setEmpty(s.blockField) ;
		
		if ( GameModes.numberQPanes(ginfo) == 2 ) {
			s.blockField[0][0][1] = QOrientations.S0 ;
			s.blockField[0][0][2] = QOrientations.S0 ;
			s.blockField[0][0][3] = QOrientations.S0 ;
			s.blockField[0][0][4] = QOrientations.S0 ;
			s.blockField[0][0][5] = QOrientations.S0 ;
			s.blockField[0][0][6] = QOrientations.S0 ;
			
			s.blockField[0][1][3] = QOrientations.S0 ;
			s.blockField[0][1][4] = QOrientations.S0 ;
			s.blockField[0][1][5] = QOrientations.S0 ;
			s.blockField[0][1][6] = QOrientations.S0 ;
			
			s.blockField[0][2][4] = QOrientations.S0 ;
			s.blockField[0][2][5] = QOrientations.S0 ;
			s.blockField[0][2][6] = QOrientations.S0 ;
		}
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(
				PieceCatalog.TETRO_CAT_Z,
				GameModes.numberQPanes(ginfo) == 1 ? QCombinations.R0 : QCombinations.S0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	private void makeTestMassiveCascadeQuantro() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// every other row is full in both
		for ( int r = 1; r < 32; r+= 2 )
			for ( int c = 0; c < 8; c++ )
				QCombinations.setAs(s.blockField, r, c, QCombinations.SS) ;
		
		// other rows cascade to a S1, then S0, clear.
		for ( int r = 0; r < 32; r+= 2 ) {
			for ( int c = 0 + ( r / 2 )%2; c < 8; c+= 2 ) {
				if ( c % 4 == (r / 2)%4 )
					QCombinations.setAs(s.blockField, r, c, QCombinations.UL) ;
				else
					QCombinations.setAs(s.blockField, r, c, QCombinations.S1) ;
			}
		}
		
		s.state = STATE_PROGRESSION ;
		s.progressionState = PROGRESSION_CLEAR ;
	}
	
	
	private void makeTestMassiveCascadeRetro() {
		ArrayOps.setEmpty(s.blockField) ;

		// every other row is full
		int qc = testingRandom.nextInt(QCombinations.R6 - QCombinations.R0) + QCombinations.R0 ;
		for ( int r = 1; r < 40; r+= 2 ) {
			for ( int c = 0; c < 10; c++ ) {
				QCombinations.setAs(s.blockField, r, c, qc) ;
				if ( testingRandom.nextFloat() < 0.4 )
					qc = testingRandom.nextInt(QCombinations.R6 - QCombinations.R0) + QCombinations.R0 ;
			}
		}
		
		// other rows cascade to clears.
		for ( int r = 0; r < 40; r+= 2 ) {
			for ( int c = 0 + ( r / 2 )%2; c < 10; c+= 2 ) {
				QCombinations.setAs(s.blockField, r, c, qc) ;
				if ( testingRandom.nextFloat() < 0.4 )
					qc = testingRandom.nextInt(QCombinations.R6 - QCombinations.R0) + QCombinations.R0 ;
			}
		}
		
		s.state = STATE_PROGRESSION ;
		s.progressionState = PROGRESSION_CLEAR ;
	}
	
	
	private void makeTestNormalFallingRetro() {
		ArrayOps.setEmpty(s.blockField) ;
		
		int qc = testingRandom.nextInt(QCombinations.R6 - QCombinations.R0) + QCombinations.R0 ;
		
		// rows have a 1/6 chance of being empty, 1/2 chance of being full.
		float sixth = 1.0f / 6.0f ;
		float half = 1.0f / 2.0f ;
		for ( int r = 0 ; r < 10; r++ ) {
			float m = testingRandom.nextFloat() ;
			for ( int c = 0; c < 10; c++ ) {
				if ( sixth < m && ( m < half + sixth || testingRandom.nextFloat() < 0.7 ) ) {
					QCombinations.setAs(s.blockField, r, c, qc) ;
					if ( testingRandom.nextFloat() < 0.4 )
						qc = testingRandom.nextInt(QCombinations.R6 - QCombinations.R0) + QCombinations.R0 ;
				}
			}
		}
		
		s.state = STATE_PROGRESSION ;
		s.progressionState = PROGRESSION_CLEAR ;
	}
	
	private void makeTestNormalFallingQuantro() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// rows have a 1/6 chance of being empty, 1/2 chance of being full.
		float sixth = 1.0f / 6.0f ;
		float half = 1.0f / 2.0f ;
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = 0 ; r < 10; r++ ) {
				float m = testingRandom.nextFloat() ;
				for ( int c = 0; c < 8; c++ ) {
					if ( sixth < m && ( m < sixth + half || testingRandom.nextFloat() < 0.7 ) ) {
						s.blockField[q][r][c] = q == 0 ? QOrientations.S0 : QOrientations.S1 ;
					}
				}
			}
		}
		
		s.state = STATE_PROGRESSION ;
		s.progressionState = PROGRESSION_CLEAR ;
	}
	
	
	private void makeTestTightenedFallingChunk() {
		ArrayOps.setEmpty(s.blockField);
		
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 4, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 5, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.SS) ;
		
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 4, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 1, 6, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 1, 6, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 3, 2, QCombinations.S0) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_SQUARE, QCombinations.UL) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestTightenedFallingChunk2() {
		ArrayOps.setEmpty(s.blockField);
		
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.U0) ;
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.U0) ;
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.U0) ;
		QCombinations.setAs(s.blockField, 3, 1, QCombinations.U0) ;
		QCombinations.setAs(s.blockField, 4, 1, QCombinations.U0) ;
		
		QCombinations.setAs(s.blockField, 5, 1, QCombinations.UL) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QCombinations.S0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestQuantroGarbageClears() {
		ArrayOps.setEmpty(s.blockField) ;
		
		for ( int i = 0; i < 7; i++ )
			QCombinations.setAs(s.blockField, 0, i, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 7, QCombinations.S0) ;
		
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PROGRESSION ;
		s.progressionState = PROGRESSION_CHUNKS_UNLOCK ;
	}
	
	private void makeTestRetroGarbageClears() {
		ArrayOps.setEmpty(s.blockField) ;
		
		for ( int i = 0; i < 9; i++ )
			QCombinations.setAs(s.blockField, 0, i, QCombinations.R0) ;
		QCombinations.setAs(s.blockField, 1, 9, QCombinations.R3) ;
		
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PROGRESSION ;
		s.progressionState = PROGRESSION_CHUNKS_UNLOCK ;
	}
	
	
	private void makeTestReserveInsertionKickFailure() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// had a box over on the right...
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 6, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 7, QCombinations.S0) ;
		
		// a falling seven...
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GUN, QCombinations.S0) ;
		// and a ST line in reserve.
		s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QCombinations.ST) ;
		
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.reservePieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeTestFallPieceComponents() {
		ArrayOps.setEmpty(s.blockField) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_L, 0, QCombinations.SS) ;
		s.nextPieces[1].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_L, 0, QCombinations.SS) ;
		s.nextPieces[2].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_L, 0, QCombinations.SS) ;
		s.nextPieces[3].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_L, 0, QCombinations.SS) ;
		
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.nextPieces[1]) ;
			rs.turn0(s.nextPieces[2]) ;
			rs.turn0(s.nextPieces[3]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	/**
	 * Tests a scenario observed to fail.
	 */
	private void makeTestEfficientSafelyFallPieces() {
		ArrayOps.setEmpty(s.blockField) ;
		
		for ( int i = 0; i < 8; i++ )
			QCombinations.setAs(s.blockField, 0, i, QCombinations.SS) ;
		for ( int i = 0; i < 4; i++ )
			QCombinations.setAs(s.blockField, 1, i, QCombinations.S0) ;
		for ( int i = 4; i < 8; i++ )
			QCombinations.setAs(s.blockField, 1, i, QCombinations.SS) ;
		for ( int i = 0; i < 8; i++ )
			QCombinations.setAs(s.blockField, 2, i, QCombinations.S0) ;
		for ( int i = 0; i < 8; i++ )
			QCombinations.setAs(s.blockField, 3, i, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 4, 1, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 4, 4, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 5, 1, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 5, 4, QCombinations.S0) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_SCREW, 0, QCombinations.SS) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeTestLeaningKick1() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// space for a Z
		QCombinations.setAs(s.blockField, 0, 0, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.S0) ;
		
		QCombinations.setAs(s.blockField, 1, 0, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 6, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 7, QCombinations.S0) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_Z, QOrientations.S0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	/**
	 * Tests a condition we have been shown fails under certain circumstances.
	 * EDIT: this bug has been fixed.  Caused by improperly nested-iffs in EarlyCollisionSystem.collides
	 */
	private void makeTestLeaningKick2() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// space for a Gamma
		QCombinations.setAs(s.blockField, 0, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 4, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 5, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 1, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 4, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 6, QCombinations.S1) ;
		
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GAMMA, QOrientations.S1) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	/**
	 * Tests a condition we have been shown fails under certain circumstances.
	 */
	private void makeTestLeaningKick3() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// space for a Line
		QCombinations.setAs(s.blockField, 0, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 4, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 5, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 1, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 4, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 7, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 2, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 3, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 3, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 4, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 5, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 7, QCombinations.S1) ;
		
		
		
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QOrientations.S1) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	/**
	 * Tests a condition we have been shown fails under certain circumstances.
	 */
	private void makeTestLeaningKick4() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// space for a Line
		QCombinations.setAs(s.blockField, 0, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 1, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 4, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 7, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 2, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 4, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 5, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 7, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 3, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 4, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 5, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 7, QCombinations.S1) ;
		
		
		
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QOrientations.S1) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestFallingChunks1() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// blue/red stacks
		for ( int i = 0; i < 4; i++ ) {
			for ( int j = 0; j < 4; j++ )
				QCombinations.setAs(s.blockField, i, j, QCombinations.S0) ;
			for ( int j = 4; j < 8; j++ )
				QCombinations.setAs(s.blockField, i, j, QCombinations.S1) ;
			
		}
		
		s.nextPieces[0].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_BRANCH, 0, QCombinations.SS) ;
		s.nextPieces[1].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_CORNER, 0, QCombinations.SS) ;
		s.nextPieces[2].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_L, 0, QCombinations.SS) ;
		
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.nextPieces[1]) ;
			rs.turn0(s.nextPieces[2]) ;
			
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
 	}
	
	private void makeTestFallingChunks2() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// blue/red stacks
		for ( int i = 0; i < 10; i++ ) {
			for ( int j = 0; j < 7; j++ )
				QCombinations.setAs(s.blockField, i, j, QCombinations.SS) ;
		}
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QOrientations.S0) ;
		s.nextPieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QOrientations.S1) ;
		
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.nextPieces[1]) ;
			
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
 	}
	
	private void makeTestFallingChunks3() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// blue/red stacks
		for ( int j = 0; j < 7; j++ )
			QCombinations.setAs(s.blockField, 0, j, QCombinations.SS) ;
		for ( int i = 1; i < 10; i++ ) {
			for ( int j = 0; j < 7; j++ )
				QCombinations.setAs(s.blockField, i, j, QOrientations.S0) ;
		}
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QOrientations.S0) ;
		s.nextPieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QOrientations.S1) ;
		
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.nextPieces[1]) ;
			
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
 	}
	
	
	private void makeTestFallingChunks4() {
		ArrayOps.setEmpty(s.blockField) ;
		
		// tests transition between ground rising and chunks falling.
		// large # of blocks falling first, then switch to small # of blocks.
		
		// blue/red stacks
		for ( int i = 0; i < 2; i++ )
			for ( int j = 0; j < 7; j++ )
				QCombinations.setAs(s.blockField, i, j, QCombinations.SS) ;
		for ( int i = 2; i < 4; i++ ) {
			for ( int j = 0; j < 6; j++ )
				QCombinations.setAs(s.blockField, i, j, QCombinations.SS) ;
			QCombinations.setAs(s.blockField, i, 6, QCombinations.S1) ;
		}
		QCombinations.setAs(s.blockField, 4, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 5, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 4, 3, QCombinations.S1) ;
		
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QCombinations.UL) ;
		
		try {
			rs.turn0(s.nextPieces[0]) ;
			
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
 	}
	
	
	
	
	
	
	private void makeTestUnlockBug1() {
		ArrayOps.setEmpty(s.blockField) ;
		for ( int r = 0; r < 1; r++ ) {
			for ( int c = 0; c < 4; c++ ) {
				QCombinations.setAs(s.blockField, r, c, QCombinations.UL) ;
			}
		}
		
		s.reservePieces[0].type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_FLASH, QOrientations.F0) ;
		try {
			rs.turn0(s.reservePieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestUnlockBug2() {
		ArrayOps.setEmpty(s.blockField) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 3, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 2, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 4, 3, QCombinations.S0) ;
		
		QCombinations.setAs(s.blockField, 0, 4, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 1, 4, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 2, 4, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 3, 4, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 4, 4, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 5, 4, QCombinations.S0) ;
		
		s.reservePieces[0].type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_FLASH, QOrientations.F0) ;
		try {
			rs.turn0(s.reservePieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestUnlockBug3() {
		ArrayOps.setEmpty(s.blockField) ;
		
		s.reservePieces[0].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_CORNER, 0, QOrientations.SL) ;
		s.reservePieces[1].type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_FLASH, QOrientations.F0) ;
		try {
			rs.turn0(s.reservePieces[0]) ;
			rs.turn0(s.reservePieces[1]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	/**
	 * Test ST by filling the board with red and giving a sticky tetromino.
	 */
	private void makeTestST1() {
		ArrayOps.setEmpty(s.blockField) ;
		for ( int r = 0; r < 4; r++ ) {
			for ( int c = 0; c < s.C-1; c++ ) {
				s.blockField[0][r][c] = QOrientations.S0 ;
			}
		}
		
		s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QOrientations.ST) ;
		try {
			rs.turn0(s.reservePieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	/**
	 * Test ST by filling the board with blue and giving a sticky tetromino.
	 */
	private void makeTestST2() {
		ArrayOps.setEmpty(s.blockField) ;
		for ( int r = 0; r < 4; r++ ) {
			for ( int c = 0; c < s.C-1; c++ ) {
				s.blockField[1][r][c] = QOrientations.S1 ;
			}
		}
		
		s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QOrientations.ST) ;
		try {
			rs.turn0(s.reservePieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	/**
	 * Test ST by filling the board with blue and giving a sticky tetromino.
	 */
	private void makeTestST3() {
		ArrayOps.setEmpty(s.blockField) ;
		for ( int r = 0; r < 4; r++ ) {
			for ( int c = 0; c < s.C-1; c++ ) {
				s.blockField[0][r][c] = QOrientations.S0 ;
				s.blockField[1][r][c] = QOrientations.S1 ;
			}
		}
		
		s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QOrientations.ST) ;
		try {
			rs.turn0(s.reservePieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	/**
	 * Replicates a scenario which caused a crash (when the result was displayed,
	 * an attempt was made to qi.encode "0:4", NO:ST, which should never occur).
	 */
	private void makeTestST4() {
		ArrayOps.setEmpty(s.blockField) ;
		QCombinations.setAs(s.blockField, 0, 0, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 4, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 5, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.NO) ;
		
		QCombinations.setAs(s.blockField, 1, 0, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 1, 3, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 4, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.NO) ;
		QCombinations.setAs(s.blockField, 1, 6, QCombinations.NO) ;
		QCombinations.setAs(s.blockField, 1, 7, QCombinations.NO) ;
		
		QCombinations.setAs(s.blockField, 2, 0, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 2, 3, QCombinations.NO) ;
		QCombinations.setAs(s.blockField, 2, 4, QCombinations.NO) ;
		QCombinations.setAs(s.blockField, 2, 5, QCombinations.NO) ;
		QCombinations.setAs(s.blockField, 2, 6, QCombinations.NO) ;
		QCombinations.setAs(s.blockField, 2, 7, QCombinations.NO) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GUN, QOrientations.S0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	/**
	 * Replicates the ST4 scenario, but adds some UL and SL blocks.
	 * Also makes the next piece a flash.
	 */
	private void makeTestST5() {
		makeTestST4() ;
		
		QCombinations.setAs(s.blockField, 3, 0, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 3, 1, QCombinations.UL) ;
		
		QCombinations.setAs(s.blockField, 2, 4, QCombinations.SL) ;
		QCombinations.setAs(s.blockField, 2, 5, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 6, QCombinations.S1) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_FLASH, QOrientations.F0) ;
		s.nextPieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QOrientations.ST) ;
		
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.nextPieces[1]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Big green area in the middle.
	 */
	private void makeTestST6() {
		ArrayOps.setEmpty(s.blockField) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 0, 4, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 0, 5, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.ST) ;
		
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 1, 3, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 1, 4, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 1, 6, QCombinations.ST) ;
		
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 2, 3, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 2, 4, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 2, 5, QCombinations.ST) ;
		QCombinations.setAs(s.blockField, 2, 6, QCombinations.ST) ;

		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GUN, QOrientations.S0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeTestST7() {
		ArrayOps.setEmpty(s.blockField) ;
		for ( int r = 0; r < 4; r++ )
			for ( int c = 0; c < 7; c++ )
				QCombinations.setAs(s.blockField, r, c, QCombinations.S0) ;
		
		// Add some suspended blocks
		QCombinations.setAs(s.blockField, 5, 2, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 5, 4, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 5, 5, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 7, 2, QCombinations.S0) ;
		
		for ( int r = 0; r < 8; r++ )
			QCombinations.setAs(s.blockField, r, 3, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 7, 4, QCombinations.S1) ;
		
		
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QOrientations.ST) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestST8() {
		// Test the new ST features.
		ArrayOps.setEmpty(s.blockField) ;
		for ( int r = 0; r < 2; r++ )
			for ( int c = 0; c < 7; c++ )
				QCombinations.setAs(s.blockField, r, c, QCombinations.S0) ;
		for ( int c = 0; c < 6; c++ )
			QCombinations.setAs(s.blockField, 2, c, QCombinations.S0) ;
		
		s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GAMMA, QOrientations.ST) ;
		s.reservePieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GUN, QOrientations.ST) ;
		
		try {
			rs.turn0(s.reservePieces[0]) ;
			rs.turn0(s.reservePieces[1]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestSTLock() {
		// Lock lots of ST together.
		ArrayOps.setEmpty(s.blockField) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GAMMA, QOrientations.ST) ;
		s.nextPieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GUN, QOrientations.ST) ;
		s.nextPieces[2].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_SQUARE, QOrientations.ST) ;
		s.nextPieces[3].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QOrientations.ST) ;
		
		s.reservePieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GAMMA, QOrientations.ST) ;
		s.reservePieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GUN, QOrientations.ST) ;
		s.reservePieces[2].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_SQUARE, QOrientations.ST) ;
		s.reservePieces[3].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QOrientations.ST) ;
		
		try {
			for ( int i = 0; i < 4; i++ ) {
				rs.turn0(s.nextPieces[i]) ;
				rs.turn0(s.reservePieces[i]) ;
			}
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	/**
	 * Ready for some clears
	 */
	private void makeTestClear1() {
		ArrayOps.setEmpty(s.blockField) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 4, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 5, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.SS) ;
		
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 4, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 6, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 7, QCombinations.S1) ;
		
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GAMMA, QOrientations.S1) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestUL1() {
		ArrayOps.setEmpty(s.blockField) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.U1) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.U1) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.U1) ;
		QCombinations.setAs(s.blockField, 0, 4, QCombinations.U0) ;
		QCombinations.setAs(s.blockField, 0, 5, QCombinations.U0) ;
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.U0) ;
		
		
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 1, 3, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 1, 4, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 1, 6, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 1, 7, QCombinations.S0) ;
		
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 2, 3, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 2, 4, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 2, 5, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 2, 6, QCombinations.UL) ;
		QCombinations.setAs(s.blockField, 2, 7, QCombinations.S0) ;

				
		
		s.nextPieces[0].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_S, 0, QCombinations.SS) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeTestUL2() {
		ArrayOps.setEmpty(s.blockField) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.U0) ;
		QCombinations.setAs(s.blockField, 0, 4, QCombinations.U0) ;
		QCombinations.setAs(s.blockField, 0, 5, QCombinations.U0) ;
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.U0) ;
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.UU) ;
		
		
		//QCombinations.setAs(s.blockField, 1, 1, QCombinations.S0) ;
		//QCombinations.setAs(s.blockField, 1, 2, QCombinations.S0) ;
		//QCombinations.setAs(s.blockField, 1, 3, QCombinations.S0) ;
		//QCombinations.setAs(s.blockField, 1, 4, QCombinations.S0) ;
		//QCombinations.setAs(s.blockField, 1, 5, QCombinations.S0) ;
		//QCombinations.setAs(s.blockField, 1, 6, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 7, QCombinations.U1) ;
		
		//QCombinations.setAs(s.blockField, 2, 1, QCombinations.NO) ;
		//QCombinations.setAs(s.blockField, 2, 2, QCombinations.NO) ;
		//QCombinations.setAs(s.blockField, 2, 3, QCombinations.NO) ;
		//QCombinations.setAs(s.blockField, 2, 4, QCombinations.NO) ;
		//QCombinations.setAs(s.blockField, 2, 5, QCombinations.NO) ;
		QCombinations.setAs(s.blockField, 2, 6, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 2, 7, QCombinations.U1) ;

		//QCombinations.setAs(s.blockField, 3, 6, QCombinations.S0) ;
		//QCombinations.setAs(s.blockField, 3, 7, QCombinations.S0) ;

				
		
		//nextPiece.type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_S, 0, QCombinations.SS) ;
		s.nextPieces[0].type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_FLASH, QOrientations.F0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	// One position unlocks for a clear, the other just unlocks.  Is there a difference?
	private void makeTestFL1() {
		ArrayOps.setEmpty(s.blockField) ;

		QCombinations.setAs(s.blockField, 0, 0, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 4, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 5, QCombinations.NO) ;
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.S0) ;
		
		QCombinations.setAs(s.blockField, 1, 0, QCombinations.NO) ;
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.NO) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 3, QCombinations.NO) ;
		QCombinations.setAs(s.blockField, 1, 4, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 6, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 1, 7, QCombinations.NO) ;
		
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.S0) ;


				
		
		//nextPiece.type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_S, 0, QCombinations.SS) ;
		s.nextPieces[0].type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_FLASH, QOrientations.F0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestFL0() {
		ArrayOps.setEmpty(s.blockField) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 4, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 5, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.SS) ;
		
		QCombinations.setAs(s.blockField, 1, 1, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 1, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 4, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 5, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 1, 6, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 2, 1, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 2, 2, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 2, 3, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 2, 4, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 2, 5, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 2, 6, QCombinations.SS) ;
		
		QCombinations.setAs(s.blockField, 3, 1, QCombinations.SS) ;
		QCombinations.setAs(s.blockField, 3, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 3, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 4, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 5, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 3, 6, QCombinations.S1) ;
		
		

		
				
		
		s.nextPieces[0].type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_FLASH, QOrientations.F0) ;
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}

	
	private void makeTestSL0() {
		ArrayOps.setEmpty(s.blockField) ;
		QCombinations.setAs(s.blockField, 0, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 0, 5, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.S0) ;
		
		s.reservePieces[0].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_S, 0, QCombinations.SL) ;
		s.reservePieces[1].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_BRANCH, 0, QCombinations.SL) ;
		
		try {
			for ( int i = 0; i < 2; i++ )
				rs.turn0(s.reservePieces[i]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestSL1() {
		ArrayOps.setEmpty(s.blockField) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 3, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 0, 5, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.S0) ;
		
		s.reservePieces[0].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_S, 0, QCombinations.SL) ;
		s.reservePieces[1].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_BRANCH, 0, QCombinations.SL) ;
		
		try {
			for ( int i = 0; i < 2; i++ )
				rs.turn0(s.reservePieces[i]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	private void makeTestSL2() {
		ArrayOps.setEmpty(s.blockField) ;
		QCombinations.setAs(s.blockField, 0, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.S1) ;

		QCombinations.setAs(s.blockField, 0, 6, QCombinations.S0) ;
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.S0) ;
		
		s.reservePieces[0].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_S, 0, QCombinations.SL) ;
		s.reservePieces[1].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_BRANCH, 0, QCombinations.SL) ;
		
		try {
			for ( int i = 0; i < 2; i++ )
				rs.turn0(s.reservePieces[i]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	private void makeTestSL3() {
		ArrayOps.setEmpty(s.blockField) ;
		QCombinations.setAs(s.blockField, 0, 0, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 1, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 2, QCombinations.S1) ;
		
		QCombinations.setAs(s.blockField, 0, 5, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 6, QCombinations.S1) ;
		QCombinations.setAs(s.blockField, 0, 7, QCombinations.S1) ;
		
		s.reservePieces[0].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_S, 0, QCombinations.SL) ;
		s.reservePieces[1].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_BRANCH, 0, QCombinations.SL) ;
		
		try {
			for ( int i = 0; i < 2; i++ )
				rs.turn0(s.reservePieces[i]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s.state = STATE_PREPARING ;
	}
	
	
	
	
	
	
	private void makeTestHurdle0() {
		for ( int c = 0; c < s.C - 1; c++ )
			QCombinations.setAs(s.blockField, 0, c, QCombinations.R0) ;
		
		for ( int c = 0; c < s.C - 2; c++ )
			QCombinations.setAs(s.blockField, 1, c, QCombinations.R1) ;
		
		for ( int c = 0; c < s.C - 2; c++ )
			QCombinations.setAs(s.blockField, 2, c, QCombinations.R1) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GUN, QCombinations.R4) ;
		
		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void makeTestHurdle1() {
		for ( int c = 0; c < s.C - 1; c++ )
			QCombinations.setAs(s.blockField, 0, c, QCombinations.R0) ;
		
		for ( int c = 0; c < s.C - 2; c++ )
			QCombinations.setAs(s.blockField, 1, c, QCombinations.R1) ;
		
		for ( int c = 0; c < s.C - 2; c++ )
			QCombinations.setAs(s.blockField, 2, c, QCombinations.R1) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_S, QCombinations.R6) ;
		s.nextPieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GUN, QCombinations.R4) ;
		
		
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.nextPieces[1]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Tests the trigger for piece-clear-immobility.  Lock a piece
	 * into an immobile position and clear it.
	 */
	private void makeTestImmobile0() {
		for ( int c = 0; c < s.C - 3; c++ )
			QCombinations.setAs(s.blockField, 0, c, QCombinations.R0) ;
		
		for ( int c = 0; c < s.C - 3; c++ )
			QCombinations.setAs(s.blockField, 1, c, QCombinations.R1) ;
		
		for ( int c = 0; c < s.C - 2; c++ )
			QCombinations.setAs(s.blockField, 2, c, QCombinations.R3) ;
		
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_S, QCombinations.R6) ;
		s.nextPieces[1].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_GUN, QCombinations.R4) ;
		
		
		try {
			rs.turn0(s.nextPieces[0]) ;
			rs.turn0(s.nextPieces[1]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	private void makeTestRetroGarbage0( int rows ) {
		for ( int r = 0; r < rows; r++ ) {
			for ( int c = 0; c < s.C - 1; c++ )
				QCombinations.setAs(s.blockField, r, c, QCombinations.R0) ;
		}
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_LINE, QCombinations.R5) ;

		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void makeTestQuantroGarbage0( int rows ) {
		for ( int r = 0; r < rows; r++ ) {
			for ( int c = 0; c < s.C - 1; c++ )
				QCombinations.setAs(s.blockField, r, c, QCombinations.SS) ;
		}
		s.nextPieces[0].type = PieceCatalog.encodeTetracube(PieceCatalog.TETRA_CAT_RECT, 0, QCombinations.SS) ;

		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	private void makeTestRetroGarbage1( ) {
		for ( int r = 0; r < 2; r++ ) {
			for ( int c = 0; c < s.C - 2; c++ )
				QCombinations.setAs(s.blockField, r, c, QCombinations.R0) ;
		}
		s.nextPieces[0].type = PieceCatalog.encodeTetromino(PieceCatalog.TETRO_CAT_S, QCombinations.R5) ;

		try {
			rs.turn0(s.nextPieces[0]) ;
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	
	////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE STATE
	//
	// These methods provide the implementation of the SerializableState
	// interface.
	//
	////////////////////////////////////////////////////////////////
	
	/**
	 * A call to this method transitions the object from "configuration"
	 * phase to "stateful use" phase.  Although this is not programatically
	 * enforced, classes implementing this interface should refuse
	 * (e.g. by throwing an exception) any calls to "stateful use" methods
	 * before this method is called, and likewise, any calls to 
	 * "configuration" methods afterwards.
	 * 
	 * Calls to set or retrieve object state should be considered
	 * "stateful use" - i.e., those methods should be refused if
	 * calls occur before this method.
	 * 
	 * @throws IllegalStateException If called more than once, or
	 * 				before necessary configuration is complete.
	 */
	public Game finalizeConfiguration() throws IllegalStateException {
		if ( configured )
			throw new IllegalStateException("finalizeConfiguration() should only be called once!") ;
		configured = true ;
		return this ;
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
	public Serializable getStateAsSerializable() throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before retrieving state!") ;
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
		return new GameState( s ) ;
	}
	
	
	/**
	 * Sets the object state according to the Serializable provided,
	 * which can be assumed to have been returned by 'getStateAsSerializable()'
	 * called on an object of the same class which underwent the same
	 * pre-"finalizeConfiguration" config. process.
	 * 
	 * POST-CONDITION: The receiver will have identical state and functionality
	 * to the object upon which "getStateAsParcelable" was called.
	 * 
	 * @param in A Serializable state from an object
	 * 
	 * @throws IllegalStateException If called before 'finalizeConfiguration'
	 */
	public SerializableState setStateAsSerializable( Serializable in ) throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		s = (GameState)in ;
		return this ;
	}
	
	
	/**
	 * Writes the current state, as a Serializable object, to the provided "outStream".
	 * The same assumptions and requirements of getStateAsParcelable are true here
	 * as well.
	 * 
	 * @param outStream	An output stream for the Serialized object
	 * @throws IllegalStateException	If called before 'finalizeConfiguration'
	 * @throws IOException	If writing to the stream fails
	 */
	public void writeStateAsSerializedObject( ObjectOutputStream outStream ) throws IllegalStateException, IOException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		outStream.writeObject(s) ;
	}
	
	/**
	 * Reads the current state, as a Serializable object, from the provided "inStream".
	 * The same assumptions and requirements of setStateAsParcelable are true here
	 * as well.
	 * 
	 * @param inStream An input stream for the Serialized object
	 * @throws IllegalStateException	If called before 'finalizeConfiguration'
	 * @throws IOException	If reading from the stream fails
	 * @throws ClassNotFoundException	If the stream does not contain the class representing
	 * 			this object's Serializable state.
	 */
	public Game readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		s = (GameState)inStream.readObject() ;
		return this ;
	}
	
	
}




