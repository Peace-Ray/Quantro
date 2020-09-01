package com.peaceray.quantro.model.descriptors;

import java.io.IOException;
import java.util.ArrayList;

import com.peaceray.quantro.q.QUpconvert;
import com.peaceray.quantro.utils.ByteArrayOps;

/**
 * RETAINED ONLY FOR BACKWARDS-COMPATIBILITY
 * 
 * DO NOT USE
 * 
 * @author Jake
 *
 */
public class AttackDescriptor {
	
	// Things we expect to be constant for the lifetime of the object
	public int R ;
	public int C ;
	
	public final static int INDEX_GARBAGE_ROWS_CASCADE_NUMBER = 0 ;
	public final static int INDEX_GARBAGE_ROWS_QCOMBINATION = 1 ;
	public final static int INDEX_GARBAGE_ROWS_NUMBER = 2 ;
	
	// CLEARED AND SENT ROWS: 
	public int [][] clearedAndSent_garbageRows ;
	public int clearedAndSent_garbageRowPieceType ;
	public int clearedAndSent_garbageRowPieceColumn ;
	public int clearedAndSent_numGarbageRows ;
	public boolean clearedAndSent_hasGarbageRows() { return clearedAndSent_numGarbageRows > 0; }
	
	// ROWS FROM A LEVEL UP:
	public int [][] levelUp_garbageRows ;
	public int levelUp_numGarbageRows ;
	public boolean levelUp_hasGarbageRows() { return levelUp_numGarbageRows > 0; }
	

	// SYNCHRONIZED LEVEL-UP:
	public int syncLevelUp_level ;
	public int syncLevelUp_levelDifference ;
	public boolean syncLevelUp_hasLevelUp() { return syncLevelUp_level > 0; }
	
	// Constructor: we require the number of rows / columns in the game.
	public AttackDescriptor( int rows, int cols ) {
		R = rows ;
		C = cols ;
		
		clearedAndSent_garbageRows = new int [INDEX_GARBAGE_ROWS_NUMBER][R*2] ;
		clearedAndSent_numGarbageRows = 0 ;
		
		levelUp_garbageRows = new int[INDEX_GARBAGE_ROWS_NUMBER][R*2] ;
		levelUp_numGarbageRows = 0 ;
		
		syncLevelUp_level = 0 ;
		syncLevelUp_levelDifference = 0 ;
	}
	
	
	public AttackDescriptor( byte [] ar, int index ) {
		R = ar[index] ;
		C = ar[index+1] ;
		
		clearedAndSent_garbageRows = new int [INDEX_GARBAGE_ROWS_NUMBER][R*2] ;
		clearedAndSent_numGarbageRows = 0 ;
		
		levelUp_garbageRows = new int [INDEX_GARBAGE_ROWS_NUMBER][R*2] ;
		levelUp_numGarbageRows = 0 ;
		
		syncLevelUp_level = 0 ;
		syncLevelUp_levelDifference = 0 ;
		
		// Read in the array!
		this.readFromByteArray(ar, index) ;
	}
	
	public AttackDescriptor( AttackDescriptor ad ) {
		R = ad.R ;
		C = ad.C ;
		
		clearedAndSent_garbageRows = new int [INDEX_GARBAGE_ROWS_NUMBER][R*2] ;
		clearedAndSent_numGarbageRows = 0 ;
		
		levelUp_garbageRows = new int [INDEX_GARBAGE_ROWS_NUMBER][R*2] ;
		levelUp_numGarbageRows = 0 ;
		
		syncLevelUp_level = 0 ;
		syncLevelUp_levelDifference = 0 ;
		
		this.copyValsFrom(ad) ;
	}
	
	
	public int R ( ) {
		return R ;
	}
	
	public int C ( ) {
		return C ;
	}


	public String toString() {
		StringBuilder sb = new StringBuilder() ;
		sb.append("AttackDescriptor R:" + R + " C:" + C) ;
		sb.append(" numCASRows:" + clearedAndSent_numGarbageRows) ;
		sb.append(" numLURows:" + levelUp_numGarbageRows) ;
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
		sb.append("sync level up " + syncLevelUp_level + " difference " + syncLevelUp_levelDifference) ;
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
	 * @param ad
	 */
	public void aggregateFrom( AttackDescriptor ad ) {
		if ( R != ad.R || C != ad.C ) {
			System.err.println("AttackDescriptor.  aggregateFrom - " + ad.R + ad.C + " to " + R + C ) ;
			throw new RuntimeException() ;
		}
		assert( R == ad.R ) ;
		assert( C == ad.C ) ;
		
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
		
		if ( !this.syncLevelUp_hasLevelUp() && ad.syncLevelUp_hasLevelUp() ) {
			this.syncLevelUp_level = ad.syncLevelUp_level ;
			this.syncLevelUp_levelDifference = ad.syncLevelUp_levelDifference ;
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
	 * @retrun 
	 */
	public static int aggregateIntoQueue( ArrayList<AttackDescriptor> queue, int queueLength, AttackDescriptor ad ) {
		
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
		}
		
		this.clearedAndSent_numGarbageRows = ad.clearedAndSent_numGarbageRows ;
		if ( ad.clearedAndSent_hasGarbageRows() ) {
			this.clearedAndSent_garbageRowPieceType = ad.clearedAndSent_garbageRowPieceType ;
			this.clearedAndSent_garbageRowPieceColumn = ad.clearedAndSent_garbageRowPieceColumn ;
			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < clearedAndSent_garbageRows[0].length; r++ ) {
					this.clearedAndSent_garbageRows[tag][r] = ad.clearedAndSent_garbageRows[tag][r] ;
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
		
		this.syncLevelUp_level = ad.syncLevelUp_level ;
		this.syncLevelUp_levelDifference = ad.syncLevelUp_levelDifference ;
		
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
		return !clearedAndSent_hasGarbageRows() && !levelUp_hasGarbageRows() && !syncLevelUp_hasLevelUp() ;
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
		return clearedAndSent_hasGarbageRows() && levelUp_hasGarbageRows() && syncLevelUp_hasLevelUp() ;
	}
	
	
	/**
	 * Makes this attack descriptor empty.
	 */	
	public void makeEmpty() {
		this.clearedAndSent_numGarbageRows = 0 ;
		this.levelUp_numGarbageRows = 0 ;
		this.syncLevelUp_level = 0 ;
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
		
		if ( b == null )
			ind += 2;
		else {
			b[ind++] = (byte)R ;
			b[ind++] = (byte)C ;
		}
		
		// Write garbage rows...
		if ( b == null ) {
			ind++ ;
			if ( clearedAndSent_hasGarbageRows() ) {
				ind += 8 ;
				ind += 4 * INDEX_GARBAGE_ROWS_NUMBER * clearedAndSent_numGarbageRows ;
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
		
		if ( b == null ) 
			ind += 2 ;
		else {
			b[ind++] = (byte)syncLevelUp_level ;
			b[ind++] = (byte)syncLevelUp_levelDifference ;
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
		
		byte adR = b[ind++] ;
		byte adC = b[ind++] ;
		
		if ( R != adR || C != adC ) {
			System.err.println("AttackDescriptor.  readFromByteArray - " + adR + adC + " to " + R + C ) ;
			throw new RuntimeException() ;
		}
		
		assert( R == adR ) ;
		assert( C == adC ) ;
		
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
		
		syncLevelUp_level = b[ind++] ;
		syncLevelUp_levelDifference = b[ind++] ;
		
		return ind - indOrig ;
	}
	
	
	/**
	 * Write to parcel.  Writes the contents of this AttackDescriptor to a parcel.
	 * @param parcel
	 */
	public void writeObject( java.io.ObjectOutputStream stream) throws IOException {
		
		stream.writeInt( clearedAndSent_numGarbageRows ) ;
		if ( clearedAndSent_hasGarbageRows() ) {
			stream.writeInt(clearedAndSent_garbageRowPieceType) ;
			stream.writeInt(clearedAndSent_garbageRowPieceColumn) ;
  			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < clearedAndSent_numGarbageRows; r++ ) {
					stream.writeInt( clearedAndSent_garbageRows[tag][r] ) ;
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
		
		stream.writeInt( syncLevelUp_level ) ;
		stream.writeInt( syncLevelUp_levelDifference) ;
	}
	
	/**
	 * Read from parcel.  Reads the contents of this AttackDescriptor to a parcel.
	 * @param parcel
	 */
	public void readObject( java.io.ObjectInputStream stream ) throws IOException {
		
		clearedAndSent_numGarbageRows = stream.readInt() ;
		if ( clearedAndSent_numGarbageRows > 0 ) {
			clearedAndSent_garbageRowPieceType = stream.readInt() ;
			clearedAndSent_garbageRowPieceColumn = stream.readInt() ;
			for ( int tag = 0; tag < INDEX_GARBAGE_ROWS_NUMBER; tag++ ) {
				for ( int r = 0; r < clearedAndSent_numGarbageRows; r++ ) {
					clearedAndSent_garbageRows[tag][r] = stream.readInt() ;
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
		
		syncLevelUp_level = stream.readInt();
		syncLevelUp_levelDifference = stream.readInt();
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
	
	
}
