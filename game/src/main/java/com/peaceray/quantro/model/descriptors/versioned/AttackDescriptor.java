package com.peaceray.quantro.model.descriptors.versioned ;

import java.io.IOException;
import java.util.ArrayList;

import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.q.QUpconvert;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.ByteArrayOps;

/**
 * A descriptor file for holding up to 1 cycle's worth of attacks;
 * i.e., a set of attacks that will be deployed during a single
 * cycle.  However, combining these descriptors is not as simple as
 * concatenating them, since two different attacks might actually
 * trigger in the same cycle (e.g. if Player 2 triggers a garbage
 * row attack, and then a poison piece attack, they may hit 
 * Player 1 at the same time).  We leave the specific logic for
 * how AttackDescriptors should be combined to the AttackSystem,
 * but one should bear this in mind when constructing / processing
 * AttackDescriptor queues.
 * 
 * The main purpose of AttackDescriptor is to provide a simple package
 * for passing Attacks between AttackSystems and, in byte-array form,
 * passing them between phones and Game objects for multiplayer.
 * 
 * @author Jake
 *
 */
public class AttackDescriptor {
	
	private static final int VERSION = 4 ;
	// 0: Exact copy of unversioned 'version', with the addition of a "preferredBlocks" field.
	// 1: Adds dropBlocks_numInValleys, dropBlocks_numOnJunctions.
	// 2: Adds pushing.  Push down is just a number; push up is explicit rows.
	//			the attack is a combination of the two.  Also includes penalty rows.
	// 3: Adds 'displacement' accel.
	// 4: Adds 'target.'  Set by the attack system when generating descriptors.
	//			also allows "diviser" to be set.  This is the number of targets
	//			among which this target is divided.
	
	
	// Things we expect to be constant for the lifetime of the object
	private int R = -1 ;
	private int C = -1 ;
	
	
	// TARGET!  This AttackDescriptor is being sent to a particular target.
	public static final int TARGET_UNSET = -1 ;
	public static final int TARGET_INCOMING = 0 ;
	public static final int TARGET_CYCLE_NEXT = 1 ;
	public static final int TARGET_CYCLE_PREVIOUS = 2 ;
	public static final int TARGET_ALL = 3 ;
	public static final int TARGET_ALL_OTHERS = 4 ;
	public static final int TARGET_ALL_DIVIDED = 5 ;
	public static final int TARGET_ALL_OTHERS_DIVIDED = 6 ;
	public static final int NUM_TARGET_CODES = 7 ;
	private static final boolean targetCodeIsSet( int targetCode ) {
		switch( targetCode ) {
		case TARGET_INCOMING:
		case TARGET_CYCLE_NEXT:
		case TARGET_CYCLE_PREVIOUS:
		case TARGET_ALL:
		case TARGET_ALL_OTHERS:
		case TARGET_ALL_DIVIDED:
		case TARGET_ALL_OTHERS_DIVIDED:
			return true ;
		}
		return false ;
	}
	
	public int target_code ;
	
	public final static int INDEX_GARBAGE_ROWS_CASCADE_NUMBER = 0 ;
	public final static int INDEX_GARBAGE_ROWS_QCOMBINATION = 1 ;
	public final static int INDEX_GARBAGE_ROWS_NUMBER = 2 ;
	
	// CLEARED AND SENT ROWS: 
	public int [][] clearedAndSent_garbageRows ;
	public int [][] clearedAndSent_garbageRowPreferredBlocks ;		// encoded, size is [R][2].
						// although sized the same as _garbageRows, this does NOT exactly
						// correspond 1-to-1 by row index.  Instead, it represents the PREFERRED
						// blocks (and gaps), in order.  This field is only used by certain 
						// behaviors; others determine their own blocks by their own means.
	public int clearedAndSent_garbageRowPieceType ;
	public int clearedAndSent_garbageRowPieceColumn ;
	public int clearedAndSent_numGarbageRows ;
	public boolean clearedAndSent_hasGarbageRows() { return clearedAndSent_numGarbageRows > 0; }
	
	// ROWS FROM A LEVEL UP:
	public int [][] levelUp_garbageRows ;
	public int levelUp_numGarbageRows ;
	public boolean levelUp_hasGarbageRows() { return levelUp_numGarbageRows > 0; }
	
	// PENALTY ROWS
	public int [][] penalty_garbageRowPreferredBlocks ;		// encoded; size is [R][2]
	public int penalty_numGarbageRows ;
						// penalty rows, at present, are always represented
						// as RAINBOW_BLAND (if 1 qpane) and SL_INACTIVE (if 2 qpanes).
	public boolean penalty_hasGarbageRows() { return penalty_numGarbageRows > 0 ; }
	
	// PUSHED ROWS
	public int push_numRowsOut ;
	public int push_numRowsIn ;
	public ArrayList<byte [][]> push_rowsIn ;
	public boolean push_hasRows() { return push_numRowsOut > 0 || push_numRowsIn > 0 ; }
	
	// DISPLACED ROWS
	public double displace_accelerateRows ;
	public boolean displace_hasRows() { return displace_accelerateRows != 0 ; }

	// SYNCHRONIZED LEVEL-UP:
	public int syncLevelUp_level ;
	public int syncLevelUp_levelDifference ;
	public boolean syncLevelUp_hasLevelUp() { return syncLevelUp_level > 0; }
	
	
	
	// DROP BLOCKS ON JUNCTIONS
	public int dropBlocks_numInValleys ;
	public int dropBlocks_numOnJunctions ;
	public int dropBlocks_numOnPeaks ;
	public int dropBlocks_numOnCorners ;
	public int dropBlocks_numTroll ;
	public boolean dropBlocks_hasBlocks() { return dropBlocks_numInValleys > 0 || dropBlocks_numOnJunctions > 0 || dropBlocks_numOnPeaks > 0 || dropBlocks_numOnCorners > 0 || dropBlocks_numTroll > 0 ; }
	public boolean dropBlocks_hasBlocksInValleys() { return dropBlocks_numInValleys > 0 ; }
	public boolean dropBlocks_hasBlocksOnJunctions() { return dropBlocks_numOnJunctions > 0 ; }
	public boolean dropBlocks_hasBlocksOnPeaks() { return dropBlocks_numOnPeaks > 0 ; }
	public boolean dropBlocks_hasBlocksOnCorners() { return dropBlocks_numOnCorners > 0 ; }
	public boolean dropBlocks_hasBlocksTroll() { return dropBlocks_numTroll > 0 ; }
	
	
	
	
	private void init() {
		target_code = TARGET_UNSET ;
		
		clearedAndSent_garbageRows = new int [INDEX_GARBAGE_ROWS_NUMBER][R*2] ;
		clearedAndSent_garbageRowPreferredBlocks = new int [R*2][2] ;
		clearedAndSent_numGarbageRows = 0 ;
		
		levelUp_garbageRows = new int[INDEX_GARBAGE_ROWS_NUMBER][R*2] ;
		levelUp_numGarbageRows = 0 ;
		
		penalty_garbageRowPreferredBlocks = new int[R*2][2] ;
		penalty_numGarbageRows = 0 ;
		
		push_numRowsOut = 0 ;
		push_numRowsIn = 0 ;
		push_rowsIn = new ArrayList<byte[][]>() ;
		
		displace_accelerateRows = 0 ;
		
		syncLevelUp_level = 0 ;
		syncLevelUp_levelDifference = 0 ;
		
		dropBlocks_numInValleys = 0 ;
		dropBlocks_numOnJunctions = 0 ;
		dropBlocks_numOnPeaks = 0 ;
		dropBlocks_numOnCorners = 0 ;
		dropBlocks_numTroll = 0 ;
	}
	
	// Constructor: we require the number of rows / columns in the game.
	public AttackDescriptor( int rows, int cols ) {
		R = rows ;
		C = cols ;
		
		init() ;
	}
	
	
	public AttackDescriptor( byte [] ar, int index ) {
		int version = ByteArrayOps.readIntAsBytes(ar, index) ;
		if ( version != VERSION )
			throw new IllegalArgumentException("Don't know how to process version " + version) ;
		
		R = ar[index+4] ;
		C = ar[index+5] ;
		
		init() ;
		
		// Read in the array!
		this.readFromByteArray(ar, index) ;
	}
	
	public AttackDescriptor( AttackDescriptor ad ) {
		R = ad.R ;
		C = ad.C ;
		
		init() ;
		
		this.copyValsFrom(ad) ;
	}
	
	
	public AttackDescriptor( com.peaceray.quantro.model.descriptors.AttackDescriptor ad ) {
		R = ad.R ;
		C = ad.C ;
		
		init() ;
		
		this.copyValsFrom(ad) ;
	}
	
	
	public int R ( ) {
		return R ;
	}
	
	public int C ( ) {
		return C ;
	}
	
	
	/**
	 * Divides the attack among a specified number of players.  This
	 * is appropriate for targeted attacks with DIVIDED targets.
	 * 
	 * This method will throw an exception if the descriptor is not
	 * divisible.
	 * 
	 * @param players
	 * @return
	 */
	public void divideAmong( int players ) {
		if ( !canDivide() ) {
			throw new IllegalStateException("Can't divide this attack descriptor.") ;
		} else if ( players == 0 ) {
			throw new IllegalArgumentException("Can't divide among zero players.") ;
		}
		
		// only displacement can be divided.
		if ( displace_hasRows() ) {
			this.displace_accelerateRows /= players ;
		}
	}
	
	
	public boolean canDivide() {
		switch( target_code ) {
		case TARGET_ALL_DIVIDED:
		case TARGET_ALL_OTHERS_DIVIDED:
			// okay so far....
			break ;
		default:
			return false ;
		}
		
		if ( clearedAndSent_hasGarbageRows() )
			return false ;
		
		if ( levelUp_hasGarbageRows() )
			return false ;
		
		if ( penalty_hasGarbageRows() )
			return false ;
		
		if ( push_hasRows() )
			return false ;
		
		if ( syncLevelUp_hasLevelUp() )
			return false ;
		
		if ( dropBlocks_hasBlocks() )
			return false ;
		
		return true ;
	}


	public String toString() {
		StringBuilder sb = new StringBuilder() ;
		sb.append("AttackDescriptor R:" + R + " C:" + C) ;
		sb.append(" target:" + target_code) ;
		sb.append(" numCASRows:" + clearedAndSent_numGarbageRows) ;
		sb.append(" numLURows:" + levelUp_numGarbageRows) ;
		sb.append(" numPRows:" + penalty_numGarbageRows) ;
		sb.append("\n") ;
		for ( int i = 0; i < INDEX_GARBAGE_ROWS_NUMBER; i++ ) {
			for ( int j = 0; j < clearedAndSent_numGarbageRows; j++ ) {
				sb.append(clearedAndSent_garbageRows[i][j] + "\t") ;
			}
			sb.append("\n") ;
		}
		for ( int i = 0; i < INDEX_GARBAGE_ROWS_NUMBER; i++ ) {
			for ( int j = 0; j < levelUp_numGarbageRows; j++ ) {
				sb.append(levelUp_garbageRows[i][j] + "\t") ;
			}
			sb.append("\n") ;
		}
		for ( int j = 0; j < penalty_numGarbageRows; j++ ) {
			sb.append(penalty_garbageRowPreferredBlocks[j] + "\t") ;
		}
		sb.append("\n") ;
		sb.append("push out:" + push_numRowsOut).append(" ") ;
		sb.append("push in:" + push_numRowsIn).append(" ") ;
		for ( int i = 0 ; i < push_numRowsIn; i++ ) {
			sb.append(" " + push_rowsIn.get(i)[0] + "|" + push_rowsIn.get(i)[1]) ;
		}
		sb.append("\n") ;
		sb.append("displacement : " + displace_accelerateRows).append(" ") ;
		sb.append("\n") ;
		sb.append(" sync level up " + syncLevelUp_level + " difference " + syncLevelUp_levelDifference) ;
		sb.append("blocks to drop ( ") ;
		sb.append("valleys:" + dropBlocks_numInValleys).append(" ") ;
		sb.append("junctions:" + dropBlocks_numOnJunctions).append(" ") ;
		sb.append("peaks:" + dropBlocks_numOnPeaks).append(" ") ;
		sb.append("corner:" + dropBlocks_numOnCorners).append(" ") ;
		sb.append("troll:" + dropBlocks_numTroll).append(" )\t") ;
		return sb.toString() ;
	}
	
	
	/**
	 * Attempts to aggregate attacks.  There is not much fancy here; different
	 * attacks of the same type are NOT aggregated.  It is assumed that if an
	 * AttackDescriptor is formed with, for example, 'k' garbage rows, than
	 * exactly 'k' garbage rows should appear during the cycle where it is used,
	 * and aggregating another attack with 'l' garbage rows should fail.
	 * 
	 * However, certain attacks may be allowed to occur "in-parallel": for example,
	 * garbage rows and poisoned pieces could reasonable be combined into a single 
	 * attack cycle.
	 * 
	 * The result of this method is that, wherever possible, attacks in 'ad' are
	 * added to this attack descriptor, to occur "in-parallel."  Those attacks within
	 * 'ad' are marked as non-occurring ( i.e., "has*****" set to false).  The
	 * attacks in 'ad' which can NOT be moved into 'this' will remain.
	 * 
	 * To add a new AttackDescriptor to a queue 'q' of upcoming attacks, call
	 * 
	 * while ( !ad.empty() )
	 * 		q[i++].aggregateFrom( ad ) ;
	 * 
	 * The call ad.empty() will return True once all its contents have been
	 * aggregated into other attack descriptors.
	 * 
	 * TARGETTING NOTE: If this AttackDescriptor has its target 'UNSET', 
	 * 		it will copy the target of the provided descriptor.  Otherwise,
	 * 		it checks that the two descriptors have matching targets.
	 * 		It is an error to attempt to aggregate an UNSET attack descriptor
	 * 		into others.
	 * 
	 * @param ad
	 */
	public void aggregateFrom( AttackDescriptor ad ) {
		if ( ad.isEmpty() )
			return ;
		
		if ( R != ad.R || C != ad.C ) {
			System.err.println("AttackDescriptor.  aggregateFrom - " + ad.R + ad.C + " to " + R + C ) ;
			throw new RuntimeException() ;
		}
		assert( R == ad.R ) ;
		assert( C == ad.C ) ;
		
		if ( !ad.isOK() ) {
			System.err.println("AttackDescriptor is not ok.  Has " + ad.toString()) ;
			throw new RuntimeException() ;
		}
		
		if ( !AttackDescriptor.targetCodeIsSet(ad.target_code) ) {
			System.err.println("AttackDescriptor.  aggregateFrom target - " + ad.target_code + " not set" ) ;
			System.err.println("AttackDescriptor is " + ad.toString()) ;
			throw new RuntimeException() ;
		}
		
		if ( !AttackDescriptor.targetCodeIsSet( this.target_code ) ) {
			this.target_code = ad.target_code ;
		} else if ( this.target_code != ad.target_code ) {
			// don't aggregate this.
			return ;
		}
		
		// Only aggregate clearedAndSent if we have no garbage rows; neither cleared
		// and sent nor levelUp.
		if ( this.clearedAndSent_numGarbageRows == 0
				&& this.levelUp_numGarbageRows == 0 && ad.clearedAndSent_numGarbageRows > 0 ) {
			// Move garbage rows into this.
			this.clearedAndSent_garbageRowPieceType = ad.clearedAndSent_garbageRowPieceType ;
			this.clearedAndSent_garbageRowPieceColumn = ad.clearedAndSent_garbageRowPieceColumn ;
			this.clearedAndSent_numGarbageRows = ad.clearedAndSent_numGarbageRows ;
			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < clearedAndSent_garbageRows[0].length; r++ ) {
					this.clearedAndSent_garbageRows[tag][r] = ad.clearedAndSent_garbageRows[tag][r] ;
				}
			}
			for ( int r = 0; r < R; r++ ) {
				for ( int qp = 0; qp < 2; qp++ ) {
					this.clearedAndSent_garbageRowPreferredBlocks[r][qp] = ad.clearedAndSent_garbageRowPreferredBlocks[r][qp] ;
				}
			}
			
			ad.clearedAndSent_numGarbageRows = 0 ;
		}
		
		// Only aggregate levelUp if we have no garbage rows; neither cleared and sent
		// nor levelUp.
		if ( this.clearedAndSent_numGarbageRows == 0 
				&& this.levelUp_numGarbageRows == 0 && ad.levelUp_numGarbageRows > 0 ) {
			// Move garbage rows into this
			this.levelUp_numGarbageRows = ad.levelUp_numGarbageRows ;
			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < levelUp_garbageRows[0].length; r++ ) {
					this.levelUp_garbageRows[tag][r] = ad.levelUp_garbageRows[tag][r] ;
				}
			}
			
			ad.levelUp_numGarbageRows = 0 ;
		}
		
		// ALWAYS aggregate penalty rows; they hit fast and hard.
		for ( int i = 0; i < ad.penalty_numGarbageRows; i++ ) {
			if ( this.penalty_numGarbageRows < R*2 ) {
				this.penalty_garbageRowPreferredBlocks[penalty_numGarbageRows] =
					ad.penalty_garbageRowPreferredBlocks[i] ;
				penalty_numGarbageRows++ ;
			}
		}
		ad.penalty_numGarbageRows = 0 ;
		
		// pushing out/in gets aggregated completely, whether we have existing pushes
		// or not.
		this.push_numRowsOut += ad.push_numRowsOut ;
		for ( int i = 0; i < ad.push_numRowsIn; i++ ) {
			while ( this.push_rowsIn.size() <= this.push_numRowsIn )
				this.push_rowsIn.add(new byte[2][C]) ;
			ArrayOps.copyInto(ad.push_rowsIn.get(i), this.push_rowsIn.get( this.push_numRowsIn )) ;
			this.push_numRowsIn++ ;
		}
		ad.push_numRowsIn = 0 ;
		ad.push_numRowsOut = 0 ;
		
		// displacement acceleration is aggregated completely.
		this.displace_accelerateRows += ad.displace_accelerateRows ;
		ad.displace_accelerateRows = 0 ;
		
		if ( !this.syncLevelUp_hasLevelUp() && ad.syncLevelUp_hasLevelUp() ) {
			this.syncLevelUp_level = ad.syncLevelUp_level ;
			this.syncLevelUp_levelDifference = ad.syncLevelUp_levelDifference ;
			
			ad.syncLevelUp_level = 0 ;
			ad.syncLevelUp_levelDifference = 0 ;
		}
		
		if ( !this.dropBlocks_hasBlocks() && ad.dropBlocks_hasBlocks() ) {
			this.dropBlocks_numInValleys = ad.dropBlocks_numInValleys ;
			this.dropBlocks_numOnJunctions = ad.dropBlocks_numOnJunctions ;
			this.dropBlocks_numOnPeaks = ad.dropBlocks_numOnPeaks ;
			this.dropBlocks_numTroll = ad.dropBlocks_numTroll ;
			
			ad.dropBlocks_numInValleys = 0 ;
			ad.dropBlocks_numOnJunctions = 0 ;
			ad.dropBlocks_numOnPeaks = 0 ;
			ad.dropBlocks_numTroll = 0 ;
		}
	}
	
	
	/**
	 * Follows the recommended method of aggregating an attack into
	 * a queue, as described in the documentation for aggregateFrom.
	 * 
	 * Individual attack elements of 'ad' are aggregated into the list,
	 * treating it as a queue.
	 * 
	 * @param queue
	 * @param queueLength The number of AttackDescriptors in the queue (which may differ from queue.size(), as queue may be intentionally over-allocated)
	 * @param ad
	 * 
	 * return The new queue length.
	 */
	public static int aggregateIntoQueue( ArrayList<AttackDescriptor> queue, int queueLength, AttackDescriptor ad ) {
		if ( ad.isEmpty() )
			return queueLength ;
		
		for ( int i = 0; i < queueLength; i++ ) {
			// Aggregate here?
			queue.get(i).aggregateFrom(ad) ;
			
			if ( ad.isEmpty() )
				return queueLength ;		// we done
		}
		
		// If we got here, then 'ad' is not empty and we have attempted to aggregate
		// into EVERY attack descriptor currently in the queue.  Extend the queue
		// (remember, queue may be overallocated and we may not need to construct
		// a new object) and put 'ad', which may have been altered since the method
		// began, in its place.
		
		if ( queue.size() == queueLength )
			queue.add( new AttackDescriptor(ad.R, ad.C) ) ;
		
		// Note: we do NOT aggregate, because this element in 'queue' was
		// not officially a member of the queue to begin with.  If it
		// was overallocated, and the queue elements had previous values,
		// we don't want them to be considered.
		queue.get(queueLength).copyValsFrom(ad) ;
		ad.makeEmpty() ;
		
		return queueLength+1 ;
	}
	
	
	/**
	 * Copies the content of ad exactly, overwriting the contents of this object.
	 * The two objects may not match exactly after this call - for example,
	 * the contents of arrays marked as unused will not be copied - but they
	 * will be functionally identical.
	 * 
	 * @param ad
	 */
	public void copyValsFrom( AttackDescriptor ad ) {
		if ( R != ad.R || C != ad.C ) {
			R = ad.R ;
			C = ad.C ;
			this.clearedAndSent_garbageRows = new int[INDEX_GARBAGE_ROWS_NUMBER][R*2] ;
			this.clearedAndSent_garbageRowPreferredBlocks = new int[R*2][2] ;
		}
		
		if ( !ad.isOK() ) {
			System.err.println("AttackDescriptor is not ok.  Has " + ad.toString()) ;
			throw new RuntimeException() ;
		}
		
		this.target_code = ad.target_code ;
		
		this.clearedAndSent_numGarbageRows = ad.clearedAndSent_numGarbageRows ;
		if ( ad.clearedAndSent_hasGarbageRows() ) {
			this.clearedAndSent_garbageRowPieceType = ad.clearedAndSent_garbageRowPieceType ;
			this.clearedAndSent_garbageRowPieceColumn = ad.clearedAndSent_garbageRowPieceColumn ;
			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < clearedAndSent_garbageRows[0].length; r++ ) {
					this.clearedAndSent_garbageRows[tag][r] = ad.clearedAndSent_garbageRows[tag][r] ;
				}
			}
			for ( int r = 0; r < R; r++ ) {
				for ( int qp = 0; qp < 2; qp++ ) {
					this.clearedAndSent_garbageRowPreferredBlocks[r][qp] = ad.clearedAndSent_garbageRowPreferredBlocks[r][qp] ;
				}
			}
		}
		
		this.levelUp_numGarbageRows = ad.levelUp_numGarbageRows ;
		if ( ad.levelUp_hasGarbageRows() ) {
			// Move garbage rows into this
			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < levelUp_garbageRows[0].length; r++ ) {
					this.levelUp_garbageRows[tag][r] = ad.levelUp_garbageRows[tag][r] ;
				}
			}
		}
		
		this.penalty_numGarbageRows = ad.penalty_numGarbageRows ;
		if ( ad.penalty_hasGarbageRows() ) {
			for ( int r = 0; r < penalty_numGarbageRows; r++ ) {
				this.penalty_garbageRowPreferredBlocks[r] = ad.penalty_garbageRowPreferredBlocks[r] ;
			}
		}
		
		this.push_numRowsOut = ad.push_numRowsOut ;
		this.push_numRowsIn = ad.push_numRowsIn ;
		while ( this.push_rowsIn.size() <= push_numRowsIn )
			this.push_rowsIn.add(new byte[2][C]) ;
		for ( int i = 0; i < push_numRowsIn; i++ ) {
			ArrayOps.copyInto( ad.push_rowsIn.get(i), this.push_rowsIn.get(i) ) ;
		}
		
		this.displace_accelerateRows = ad.displace_accelerateRows ;
		
		this.syncLevelUp_level = ad.syncLevelUp_level ;
		this.syncLevelUp_levelDifference = ad.syncLevelUp_levelDifference ;
		
		this.dropBlocks_numInValleys = ad.dropBlocks_numInValleys ;
		this.dropBlocks_numOnJunctions = ad.dropBlocks_numOnJunctions ;
		this.dropBlocks_numOnPeaks = ad.dropBlocks_numOnPeaks ;
		this.dropBlocks_numOnCorners = ad.dropBlocks_numOnCorners ;
		this.dropBlocks_numTroll = ad.dropBlocks_numTroll ;
	}
	
	
	
	
	/**
	 * Copies the content of ad exactly, overwriting the contents of this object.
	 * The two objects may not match exactly after this call - for example,
	 * the contents of arrays marked as unused will not be copied - but they
	 * will be functionally identical.
	 * 
	 * @param ad
	 */
	public void copyValsFrom( com.peaceray.quantro.model.descriptors.AttackDescriptor ad ) {
		if ( R != ad.R || C != ad.C ) {
			R = ad.R ;
			C = ad.C ;
			this.clearedAndSent_garbageRows = new int[INDEX_GARBAGE_ROWS_NUMBER][R*2] ;
			this.clearedAndSent_garbageRowPreferredBlocks = new int[R*2][2] ;
		}
		
		this.target_code = TARGET_CYCLE_NEXT ;
		
		this.clearedAndSent_numGarbageRows = ad.clearedAndSent_numGarbageRows ;
		if ( ad.clearedAndSent_hasGarbageRows() ) {
			this.clearedAndSent_garbageRowPieceType = ad.clearedAndSent_garbageRowPieceType ;
			this.clearedAndSent_garbageRowPieceColumn = ad.clearedAndSent_garbageRowPieceColumn ;
			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < clearedAndSent_garbageRows[0].length; r++ ) {
					this.clearedAndSent_garbageRows[tag][r] = ad.clearedAndSent_garbageRows[tag][r] ;
				}
			}
			for ( int r = 0; r < R; r++ ) {
				for ( int qp = 0; qp < 2; qp++ ) {
					this.clearedAndSent_garbageRowPreferredBlocks[r][qp] = 0 ;
				}
			}
		}
		
		this.penalty_numGarbageRows = 0 ;
		
		this.push_numRowsOut = 0 ;
		this.push_numRowsIn = 0 ;
		
		this.displace_accelerateRows = 0 ;
		
		this.levelUp_numGarbageRows = ad.levelUp_numGarbageRows ;
		if ( ad.levelUp_hasGarbageRows() ) {
			// Move garbage rows into this
			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < levelUp_garbageRows[0].length; r++ ) {
					this.levelUp_garbageRows[tag][r] = ad.levelUp_garbageRows[tag][r] ;
				}
			}
		}
		
		this.syncLevelUp_level = ad.syncLevelUp_level ;
		this.syncLevelUp_levelDifference = ad.syncLevelUp_levelDifference ;
		
		this.dropBlocks_numInValleys = 0 ;
		this.dropBlocks_numOnJunctions = 0 ;
		this.dropBlocks_numOnPeaks = 0 ;
		this.dropBlocks_numOnCorners = 0 ;
		this.dropBlocks_numTroll = 0 ;
	}
	
	
	
	
	/**
	 * Copies the first 'len' elements of src, into dst.  Returns the
	 * number of items in dst.
	 * 
	 * @param src
	 * @param len
	 * @param dst
	 * @return
	 */
	public static int copyIntoQueue( ArrayList<AttackDescriptor> src, int len, ArrayList<AttackDescriptor> dst ) {
		for ( int i = 0; i < len; i++ ) {
			AttackDescriptor ad = src.get(i) ;
			if ( i == dst.size() )
				dst.add( new AttackDescriptor( ad.R, ad.C )) ;
			
			dst.get(i).copyValsFrom(ad) ;
		}
		
		return len ;
	}
	
	
	/**
	 * Returns whether this attack descriptor is completely empty
	 * of attacks.
	 * 
	 * @return If this attack descriptor is used, will exactly 0 attacks trigger?
	 */
	public boolean isEmpty() {
		return !clearedAndSent_hasGarbageRows() && !levelUp_hasGarbageRows() && !penalty_hasGarbageRows() && !push_hasRows() && !displace_hasRows() && !syncLevelUp_hasLevelUp() && !dropBlocks_hasBlocks() ;
	}
	
	
	/**
	 * Returns whether this attack descriptor is absolutely full - i.e.,
	 * whether it can be guaranteed that this.aggregateFrom( ad ) will have
	 * no effect on this or 'ad', regardless of any other specific content
	 * of the two objects.
	 * 
	 * @return Does this attack descriptor describe 1 of each type of valid attack?
	 */
	public boolean isFull() {
		return false ;		// now that pushing aggregates an unlimited amount,
							// we can never guarantee this.
	}
	
	
	/**
	 * A Sanity Check.
	 * @return
	 */
	public boolean isOK() {
		return isEmpty() || targetCodeIsSet(target_code) ;
	}
	
	
	/**
	 * Makes this attack descriptor empty.
	 */	
	public void makeEmpty() {
		this.target_code = TARGET_UNSET ;
		this.clearedAndSent_numGarbageRows = 0 ;
		this.levelUp_numGarbageRows = 0 ;
		this.penalty_numGarbageRows = 0 ;
		this.push_numRowsOut = 0 ;
		this.push_numRowsIn = 0 ;
		this.displace_accelerateRows = 0 ;
		this.syncLevelUp_level = 0 ;
		this.syncLevelUp_levelDifference = 0 ;
		this.dropBlocks_numInValleys = 0 ;
		this.dropBlocks_numOnJunctions = 0 ;
		this.dropBlocks_numOnPeaks = 0 ;
		this.dropBlocks_numOnCorners = 0 ;
		this.dropBlocks_numTroll = 0 ;
	}
	
	
	// An attack can be described as up to (let's say) R*2 garbage rows.
	// Each row has a QCombination, a piece/cascade mark, an associated column,
	// and a pseudorandom value.  That's 4 ints per row.
	
	
	/**
	 * Writes the current contents of this AttackDescriptor to
	 * the provided byte array, beginning at 'ind'.
	 * 
	 * Returns the number of bytes written.  It is the caller's responsibility
	 * that the full size of the AttackDescriptor can fit in the
	 * provided byte array (especially within the "valid written region",
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
	public int writeToByteArray( byte [] b, int ind, int topBounds ) {
		
		int indOrig = ind ;
		
		
		// version, R, and C.
		if ( b == null )
			ind += 6 ;
		else {
			ByteArrayOps.writeIntAsBytes(VERSION, b, ind) ;
			ind += 4 ;
			b[ind++] = (byte)R ;
			b[ind++] = (byte)C ;
		}
		
		// target
		if ( b == null )
			ind += 4 ;
		else {
			ByteArrayOps.writeIntAsBytes(target_code, b, ind) ;
			ind += 4 ;
		}
		
		// Write garbage rows...
		if ( b == null ) {
			ind++ ;
			if ( clearedAndSent_hasGarbageRows() ) {
				ind += 8 ;
				ind += 4 * INDEX_GARBAGE_ROWS_NUMBER * clearedAndSent_numGarbageRows ;
				ind += 4 * clearedAndSent_numGarbageRows * 2 ;
			}
		} else {
			b[ind++] = (byte)clearedAndSent_numGarbageRows ;
			if ( clearedAndSent_hasGarbageRows() ) {
				ByteArrayOps.writeIntAsBytes(this.clearedAndSent_garbageRowPieceType, b, ind) ;
				ByteArrayOps.writeIntAsBytes(this.clearedAndSent_garbageRowPieceColumn, b, ind+4) ;
				ind += 8 ;
				
				for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
					for ( int r = 0; r < clearedAndSent_numGarbageRows; r++ ) {
						ByteArrayOps.writeIntAsBytes(clearedAndSent_garbageRows[tag][r], b, ind) ;
						ind += 4 ;
					}
				}
				
				for ( int r = 0; r < clearedAndSent_numGarbageRows; r++ ) {
					for ( int qp = 0; qp < 2; qp++ ) {
						ByteArrayOps.writeIntAsBytes(clearedAndSent_garbageRowPreferredBlocks[r][qp], b, ind) ;
						ind += 4 ;
					}
				}
			}
		}
		
		if ( b == null ) {
			ind++ ;
			if ( levelUp_hasGarbageRows() ) {
				ind += 4 * INDEX_GARBAGE_ROWS_NUMBER * levelUp_numGarbageRows ;
			}
		} else {
			b[ind++] = (byte)levelUp_numGarbageRows ;
			if ( levelUp_hasGarbageRows() ) {
				for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
					for ( int r = 0; r < levelUp_numGarbageRows; r++ ) {
						ByteArrayOps.writeIntAsBytes(levelUp_garbageRows[tag][r], b, ind) ;
						ind += 4 ;
					}
				}
			}
		}
		
		if ( b == null ) {
			ind++ ;
			if ( penalty_hasGarbageRows() ) {
				ind += 4 * penalty_numGarbageRows * 2 ;
			}
		} else {
			b[ind++] = (byte)penalty_numGarbageRows ;
			if ( penalty_hasGarbageRows() ) {
				for ( int r = 0; r < penalty_numGarbageRows; r++ ) {
					for ( int qp = 0; qp < 2; qp++ ) {
						ByteArrayOps.writeIntAsBytes(penalty_garbageRowPreferredBlocks[r][qp], b, ind) ;
						ind += 4 ;
					}
				}
			}
		}
		
		if ( b == null ) {
			ind += 2 ;
			ind += 2 * C * push_numRowsIn ;
		} else {
			b[ind++] = (byte)push_numRowsOut ;
			b[ind++] = (byte)push_numRowsIn ;
			for ( int i = 0; i < push_numRowsIn; i++ ) {
				byte [][] row = push_rowsIn.get(i) ;
				for ( int qp = 0; qp < 2; qp++ ) {
					for ( int c = 0; c < C; c++ ) {
						b[ind++] = row[qp][c] ;
					}
				}
			}
		}
		
		if ( b == null ) {
			ind += 8 ;
		} else {
			ByteArrayOps.writeDoubleAsBytes(displace_accelerateRows, b, ind) ;
			ind += 8 ;
		}
		
		if ( b == null ) 
			ind += 2 ;
		else {
			b[ind++] = (byte)syncLevelUp_level ;
			b[ind++] = (byte)syncLevelUp_levelDifference ;
		}
		
		if ( b == null )
			ind += 5 ;
		else {
			b[ind++] = (byte)dropBlocks_numInValleys ;
			b[ind++] = (byte)dropBlocks_numOnJunctions ;
			b[ind++] = (byte)dropBlocks_numOnPeaks ;
			b[ind++] = (byte)dropBlocks_numOnCorners ;
			b[ind++] = (byte)dropBlocks_numTroll ;
		}
		
		// Bounds check and return.
		if ( ind > topBounds )
			throw new IndexOutOfBoundsException("AttackDescriptor.writeToByteArray wrote past the provided top bounds: wrote to from " + indOrig + " to " + ind + ", with top bounds " + topBounds) ;
		
		// That's it.  Return length.
		return ind - indOrig ;
	}
	
	
	/**
	 * Reads the current contents of this AttackDescriptor from
	 * the provided byte array, beginning at 'ind'.
	 * 
	 * Returns the number of bytes written.  It is the caller's responsibility
	 * that the full size of the AttackDescriptor can fit in the
	 * provided byte array (especially within the "valid written region",
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
	public int readFromByteArray( byte [] b, int ind ) {
		
		int indOrig = ind ;
		
		int version = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		// check version number
		if ( version < 0 || version > VERSION )
			throw new RuntimeException("AttackDescriptor.  readFromByteArray - can't read version " + version) ;
				
		byte adR = b[ind++] ;
		byte adC = b[ind++] ;
		
		if ( R != adR || C != adC ) {
			System.err.println("AttackDescriptor.  readFromByteArray - " + adR + "," + adC + " to " + R + "," + C ) ;
			throw new RuntimeException() ;
		}
		
		assert( R == adR ) ;
		assert( C == adC ) ;
		
		// target
		target_code = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		// If this is 0, there are no rows.  If 1, we will overwrite this value.
		clearedAndSent_numGarbageRows = b[ind++] ;
		if ( this.clearedAndSent_hasGarbageRows() ) {
			this.clearedAndSent_garbageRowPieceType = ByteArrayOps.readIntAsBytes(b, ind) ;
			this.clearedAndSent_garbageRowPieceColumn = ByteArrayOps.readIntAsBytes(b, ind+4) ;
			ind += 8 ;
			
			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < clearedAndSent_numGarbageRows; r++ ) {
					clearedAndSent_garbageRows[tag][r] = ByteArrayOps.readIntAsBytes(b, ind) ;
					ind += 4 ;
				}
			}
			
			for ( int r = 0; r < clearedAndSent_numGarbageRows; r++ ) {
				for ( int qp = 0; qp < 2; qp++ ) {
					clearedAndSent_garbageRowPreferredBlocks[r][qp] = ByteArrayOps.readIntAsBytes(b, ind) ;
					ind += 4 ;
				}
			}
		}
		
		levelUp_numGarbageRows = b[ind++] ;
		if ( this.levelUp_hasGarbageRows() ) {
			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < levelUp_numGarbageRows; r++ ) {
					levelUp_garbageRows[tag][r] = ByteArrayOps.readIntAsBytes(b, ind) ;
					ind += 4 ;
				}
			}
		}
		
		if ( version >= 2 ) {
			penalty_numGarbageRows = b[ind++] ;
			for ( int r = 0; r < penalty_numGarbageRows; r++ ) {
				for ( int qp = 0; qp < 2; qp++ ) {
					penalty_garbageRowPreferredBlocks[r][qp] = ByteArrayOps.readIntAsBytes(b, ind) ;
					ind += 4 ;
				}
			}
		} else {
			penalty_numGarbageRows = 0 ;
		}
		
		if ( version >= 2 ) {
			push_numRowsOut = b[ind++] ;
			push_numRowsIn = b[ind++] ;
			for ( int i = 0; i < push_numRowsIn; i++ ) {
				if ( push_rowsIn.size() <= i )
					push_rowsIn.add(new byte[2][C]) ;
				byte [][] row = push_rowsIn.get(i) ;
				for ( int qp = 0; qp < 2; qp++ ) {
					for ( int c = 0; c < C; c++ ) {
						row[qp][c] = b[ind++] ;
					}
				}
			}
		} else {
			push_numRowsOut = 0 ;
			push_numRowsIn = 0 ;
		}
		
		if ( version >= 3 ) {
			displace_accelerateRows = ByteArrayOps.readDoubleAsBytes(b, ind) ;
			ind += 8 ;
		} else {
			displace_accelerateRows = 0 ;
		}
		
		syncLevelUp_level = b[ind++] ;
		syncLevelUp_levelDifference = b[ind++] ;
		
		if ( version >= 1 ) {
			// version 1 adds dropBlocks.
			dropBlocks_numInValleys = b[ind++] ;
			dropBlocks_numOnJunctions = b[ind++] ;
			dropBlocks_numOnPeaks = b[ind++] ;
			dropBlocks_numOnCorners = b[ind++] ;
			dropBlocks_numTroll = b[ind++] ;
		} else {
			dropBlocks_numInValleys = 0 ;
			dropBlocks_numOnJunctions = 0 ;
			dropBlocks_numOnPeaks = 0 ;
			dropBlocks_numOnCorners = 0 ;
			dropBlocks_numTroll = 0 ;
		}
		
		return ind - indOrig ;
	}
	
	
	/**
	 * Write to parcel.  Writes the contents of this AttackDescriptor to a parcel.
	 * @param parcel
	 */
	public void writeObject( java.io.ObjectOutputStream stream) throws IOException {
		
		// write version
		stream.writeInt(VERSION) ;
		
		// write target
		stream.writeInt( target_code ) ;
		
		stream.writeInt( clearedAndSent_numGarbageRows ) ;
		if ( clearedAndSent_hasGarbageRows() ) {
			stream.writeInt(clearedAndSent_garbageRowPieceType) ;
			stream.writeInt(clearedAndSent_garbageRowPieceColumn) ;
  			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < clearedAndSent_numGarbageRows; r++ ) {
					stream.writeInt( clearedAndSent_garbageRows[tag][r] ) ;
				}
			}
  			for ( int r = 0; r < clearedAndSent_numGarbageRows; r++ ) {
				for ( int qp = 0; qp < 2; qp++ ) {
					stream.writeInt(clearedAndSent_garbageRowPreferredBlocks[r][qp]) ;
				}
			}
		}
		
		stream.writeInt( levelUp_numGarbageRows ) ;
		if ( levelUp_hasGarbageRows() ) {
  			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < levelUp_numGarbageRows; r++ ) {
					stream.writeInt( levelUp_garbageRows[tag][r] ) ;
				}
			}
		}
		
		stream.writeInt( penalty_numGarbageRows ) ;
		for ( int r = 0; r < penalty_numGarbageRows; r++ ) {
			for ( int qp = 0; qp < 2; qp++ ) {
				stream.writeInt( penalty_garbageRowPreferredBlocks[r][qp] ) ;
			}
		}
		
		stream.writeInt( push_numRowsOut ) ;
		stream.writeInt( push_numRowsIn ) ;
		for ( int i = 0; i < push_numRowsIn; i++ ) {
			byte [][] row = push_rowsIn.get(i) ;
			for ( int qp = 0; qp < 2; qp++ ) {
				for ( int c = 0; c < C; c++ ) {
					stream.writeByte( row[qp][c] ) ;
				}
			}
		}
		
		stream.writeDouble( displace_accelerateRows ) ;
		
		stream.writeInt( syncLevelUp_level ) ;
		stream.writeInt( syncLevelUp_levelDifference) ;
		
		stream.writeByte( (byte)dropBlocks_numInValleys ) ;
		stream.writeByte( (byte)dropBlocks_numOnJunctions ) ;
		stream.writeByte( (byte)dropBlocks_numOnPeaks ) ;
		stream.writeByte( (byte)dropBlocks_numOnCorners ) ;
		stream.writeByte( (byte)dropBlocks_numTroll ) ;
		
	}
	
	/**
	 * Read from parcel.  Reads the contents of this AttackDescriptor to a parcel.
	 * @param parcel
	 */
	public void readObject( java.io.ObjectInputStream stream ) throws IOException {
		
		// Read version
		int version = stream.readInt() ;
		if ( version < 0 || version > VERSION )
			throw new IllegalStateException("Don't know how to read version " + version) ;
		
		if ( version >= 4 ) {
			target_code = stream.readInt() ;
		} else {
			target_code = TARGET_UNSET ;
		}
		
		clearedAndSent_numGarbageRows = stream.readInt() ;
		if ( clearedAndSent_numGarbageRows > 0 ) {
			clearedAndSent_garbageRowPieceType = stream.readInt() ;
			clearedAndSent_garbageRowPieceColumn = stream.readInt() ;
			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < clearedAndSent_numGarbageRows; r++ ) {
					clearedAndSent_garbageRows[tag][r] = stream.readInt() ;
				}
			}
			for ( int r = 0; r < clearedAndSent_numGarbageRows; r++ ) {
				for ( int qp = 0; qp < 2; qp++ ) {
					clearedAndSent_garbageRowPreferredBlocks[r][qp] = stream.readInt() ;
				}
			}
		}
		
		levelUp_numGarbageRows = stream.readInt();
		if ( levelUp_numGarbageRows > 0 ) {
			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < levelUp_numGarbageRows; r++ ) {
					levelUp_garbageRows[tag][r] = stream.readInt() ;
				}
			}
		}
		
		if ( version >= 2 ) {
			penalty_numGarbageRows = stream.readInt() ;
			for ( int r = 0; r < penalty_numGarbageRows; r++ ) {
				for ( int qp = 0; qp < 2; qp++ ) {
					penalty_garbageRowPreferredBlocks[r][qp] = stream.readInt() ;
				}
			}
		} else {
			penalty_numGarbageRows = 0 ;
		}
		
		if ( version >= 2 ) {
			push_numRowsOut = stream.readInt() ;
			push_numRowsIn = stream.readInt() ;
			for ( int i = 0; i < push_numRowsIn; i++ ) {
				if ( push_rowsIn.size() <= i )
					push_rowsIn.add( new byte[2][C] ) ;
				byte [][] row = push_rowsIn.get(i) ;
				for ( int qp = 0; qp < 2; qp++ ) {
					for ( int c = 0; c < C; c++ ) {
						row[qp][c] = stream.readByte() ;
					}
				}
			}
		} else {
			push_numRowsOut = 0 ;
			push_numRowsIn = 0 ;
		}
		
		if ( version >= 3 ) {
			displace_accelerateRows = stream.readDouble() ;
		} else {
			displace_accelerateRows = 0 ;
		}
		
		syncLevelUp_level = stream.readInt();
		syncLevelUp_levelDifference = stream.readInt();
		
		
		// version 1: adds dropBlocks.
		if ( version >= 1 ) {
			dropBlocks_numInValleys = stream.readByte() ;
			dropBlocks_numOnJunctions = stream.readByte() ;
			dropBlocks_numOnPeaks = stream.readByte() ;
			dropBlocks_numOnCorners = stream.readByte() ;
			dropBlocks_numTroll = stream.readByte() ;
		} else {
			dropBlocks_numInValleys = 0 ;
			dropBlocks_numOnJunctions = 0 ;
			dropBlocks_numOnPeaks = 0 ;
			dropBlocks_numTroll = 0 ;
		}
		
	}
	
	
	public AttackDescriptor qUpconvert() {
		AttackDescriptor ad = new AttackDescriptor(this) ;
		for ( int i = 0; i < ad.clearedAndSent_garbageRows.length; i++ ) {
			ad.clearedAndSent_garbageRows[i][INDEX_GARBAGE_ROWS_QCOMBINATION]
			        = QUpconvert.upConvert( ad.clearedAndSent_garbageRows[i][INDEX_GARBAGE_ROWS_QCOMBINATION] ) ;
		}
		for ( int i = 0; i < ad.levelUp_garbageRows.length; i++ ) {
			ad.levelUp_garbageRows[i][INDEX_GARBAGE_ROWS_QCOMBINATION]
			        = QUpconvert.upConvert( ad.levelUp_garbageRows[i][INDEX_GARBAGE_ROWS_QCOMBINATION] ) ;
		}
		return ad ;
	}
	
	
	
	/**
	 * Encodes as an integer the presence/absence of blocks in the
	 * specified row and qPane.  We use QOrientations.NO to indicate
	 * the absence of a block.
	 * 
	 * Encoding is fairly simple; we step across columns from right to left
	 * (so reversing this can truncate).  At each step,
	 * we right-shift the bit representation.  If a block
	 * is present, we OR the BIT into our encoding.
	 * 
	 * @return The encoding.  A '0' indicates that all blocks are absent.
	 */
	public static int encodeRowBlocksPresent( byte [][][] blocks, int qp, int row ) {
		if ( blocks[qp][row].length >= 30 )
			throw new IllegalArgumentException("Cannot encode more than 30 columns!") ;
		
		int rep = 0 ;
		
		for ( int c = blocks[qp][row].length -1 ; c >= 0; c-- ) {
			rep *= 2 ;
			if ( blocks[qp][row][c] != QOrientations.NO )
				rep += 1 ;
		}
		
		return rep ;
	}
	
	
	/**
	 * Encodes as an integer the presence / absence of blocks in the specified 
	 * qpane and row.
	 * 
	 * 'leadingEmpty' indicates the number of 'empty' blocks which should be assumed
	 * occur before the first column provided.
	 * 
	 * If 'leadingEmpty' is negative, we skip that many (left-side) columns in 'blocks'
	 * in our representation.
	 * 
	 * @param blocks
	 * @param qp
	 * @param row
	 * @param columnOffset
	 * @return
	 */
	public static int encodeRowBlocksPresent( byte [][][] blocks, int qp, int row, int leadingEmpty ) {
		if ( blocks[qp][row].length + leadingEmpty >= 30 )
			throw new IllegalArgumentException("Cannot encode more than 30 columns!") ;
		
		int rep = 0 ;
		
		for ( int c = blocks[qp][row].length -1 ; c >= 0 && c >= -leadingEmpty; c-- ) {
			// condition: c is not past the start of blocks AND 
			// is greater than or equal to the negative of leading empty.
			// Thus if leadingEmpty is zero or positive it has no effect here.
			// If negative, then we omit abs(leadingEmpty) of the first columns
			// in blocks.
			rep *= 2 ;
			if ( blocks[qp][row][c] != QOrientations.NO )
				rep += 1 ;
		}
		
		for ( int i = 0; i < leadingEmpty; i++ )
			rep *= 2 ;	// encode empty
		
		return rep ;
	}
	
	
	/**
	 * Encodes the block present as if only the specified column is filled.  Encoding
	 * is left-relative and extends indefinitely (with empty columns) to the right,
	 * so the total number of columns is irrelevant.
	 * @param columnPresent
	 * @param totalColumns
	 * @return
	 */
	public static int encodeRowBlockPresent( int columnPresent ) {
		int rep = 1 ;
	
		for ( int c = columnPresent; c > 0; c-- )
			rep *= 2 ;
		
		return rep ;
	}
	
	
	/**
	 * Inverts the specified encoding, assuming the existence of 'totalColumns' total columns.
	 * "Present" becomes "absent" and vice-versa.
	 * @param encoding
	 * @param totalColumns
	 * @return
	 */
	public static int invertEncoding( int encoding, int totalColumns ) {
		if ( totalColumns >= 30 )
			throw new IllegalArgumentException("Cannot encode more than 30 columns!") ;
		
		// Let's consider how things are encoded.  Column 'i' is represented
		// by (encoding % (2 ** (i+1))) / (2 ** i).  In other words, for column 0 (leftmost),
		// the value of the 'ith' bit represents the presence of the block.
		
		int rep = 0 ;
		
		int mod = 2 ;
		int div = 1 ;
		
		for ( int i = 0; i < totalColumns; i++ ) {
			int prev = ( encoding % mod ) / div ;
			int next = 1 - prev ;
			rep += next * div ;
			
			mod *= 2 ;
			div *= 2 ;
		}
		
		return rep ;
	}
	
	
	/**
	 * Decodes the provided integer, which represents the presence/absence of blocks in
	 * the specified row and qPane.  We use QOrientation.NO to indicate the
	 * absence of a block.
	 * 
	 * Because the encoding only covers the presence / absence of blocks, we
	 * decode by 'zeroing out' the specified blocks, leaving "present" blocks
	 * intact in the provided blockfield.
	 * 
	 * @param blocks
	 * @param qp
	 * @param row
	 * @param encoding
	 */
	public static void decodeRowBlocksPresentBySettingZeros( byte [][][] blocks, int qp, int row, int encoding ) {
		for ( int c = 0; c < blocks[qp][row].length; c++ ) {
			if ( encoding % 2 == 0 )
				blocks[qp][row][c] = QOrientations.NO ;
			else
				encoding -= 1 ;
			encoding /= 2 ;
		}
	}
	
	
}
