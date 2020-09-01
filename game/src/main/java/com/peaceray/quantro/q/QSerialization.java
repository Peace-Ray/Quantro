package com.peaceray.quantro.q;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Methods for Serializing QCombinations and arrays thereof.
 * 
 * Serialization can be as serialized objects or as a String.
 * String representation uses a few control characters; specifically,
 * "[" and "]".  Arrays are represented as
 * 
 * [ encoding R C
 * 		[ qc qc qc qc qc
 * 		  qc qc qc qc qc ] ]
 * 
 * (whitespace will be spaces and newlines, so exact formatting may not match)
 * 
 * 
 * USAGE NOTES: If you want to limit memory allocation when reading Serializations,
 * 		the recommended usage is pre-allocated arrays reading from ObjectInputStreams
 * 		(not strings).  String reads may include allocation beyond that happening
 * 		"behind the scenes" in an ObjectInputStream.
 * 
 * @author Jake
 *
 */
public class QSerialization {

	private static final int ENCODING_FULL = 0 ;		// a full list of all values
	private static final int ENCODING_SPARSE = 1 ;		// sparse encoding: val [#vals, #nonvals, {nonvals}]*.
	private static final int ENCODING_SPARSE_DIFFERENCE = 2 ;	// a sparse encoding: [#same, #diff, {differences}]*.
	
	
	private static final int COUNT_SEQUENTIAL_EQUAL_INT = 0 ;
	private static final int COUNT_SEQUENTIAL_INEQUAL_INT = 1 ;
	private static final int COUNT_SEQUENTIAL_EQUAL_TEMPLATE = 2 ;
	private static final int COUNT_SEQUENTIAL_INEQUAL_TEMPLATE = 3 ;
	
	private static final String BRACE_OPEN = "[" ;
	private static final String BRACE_CLOSE = "]" ;
	
	
	// Counts array: used to determine the most common QCombination.
	private static final int [] qc_counts = new int[QCombinations.NUM] ; 
	
	
	
	/**
	 * Using the minimum-length encoding, writes the provided 'val' array
	 * to the provided ObjectOutputStream.
	 * 
	 * All encodings include an explicit representation of row and column size.
	 * 
	 * The result may be read back in to any 3d array of the same dimensions, or a
	 * larger array with pre-specified boundaries that exactly match.  Because of
	 * this "exact match" condition, it is recommended that some redundancy be
	 * used in representation if you wish to use pre-allocated arrays (rather
	 * than arrays allocated at the time the object is read).
	 * 
	 * @param val
	 * @param template
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @param oos
	 * @param sb
	 * @param encoding
	 * @throws IOException
	 */
	public static final void write( byte [][][] val, ObjectOutputStream oos ) throws IOException {
		write( val, 0, val[0].length, 0, val[0][0].length, oos ) ;
	}
	
	
	/**
	 * Using the minimum-length encoding, writes the provided 'val' array
	 * to the provided ObjectOutputStream.
	 * 
	 * All encodings include an explicit representation of row and column size; these
	 * values represent the row/column dimensions specified by *Start and *Lim, which
	 * may be smaller than 'val' itself.
	 * 
	 * The result may be read back in to any 3d array of the same dimensions, or a
	 * larger array with pre-specified boundaries that exactly match.  Because of
	 * this "exact match" condition, it is recommended that some redundancy be
	 * used in representation if you wish to use pre-allocated arrays (rather
	 * than arrays allocated at the time the object is read).
	 * 
	 * @param val
	 * @param template
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @param oos
	 * @param sb
	 * @param encoding
	 * @throws IOException
	 */
	public static final void write(
			byte [][][] val,
			int rowStart, int rowLim, int colStart, int colLim,
			ObjectOutputStream oos ) throws IOException {
		
		if ( rowLim < rowStart )
			throw new IllegalArgumentException("Limits must include >= 0 rows") ;
		if ( colLim < colStart )
			throw new IllegalArgumentException("Limits must include >= 0 cols") ;

		int encoding = shortestEncoding( val, null, rowStart, rowLim, colStart, colLim ) ;
		writeQCombinations( val, null, rowStart, rowLim, colStart, colLim, oos, null, encoding ) ;
	}
	
	
	/**
	 * Using the minimum-length encoding, writes the provided 'val' array
	 * to the provided ObjectOutputStream.  The provided 'template', if non-null,
	 * must ALWAYS be provided when this value is read.
	 * An encoding which assumes the existence of these values might be used by
	 * this method.
	 * 
	 * All encodings include an explicit representation of row and column size.
	 * 
	 * The result may be read back in to any 3d array of the same dimensions, or a
	 * larger array with pre-specified boundaries that exactly match.  Because of
	 * this "exact match" condition, it is recommended that some redundancy be
	 * used in representation if you wish to use pre-allocated arrays (rather
	 * than arrays allocated at the time the object is read).
	 * 
	 * @param val
	 * @param template
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @param oos
	 * @param sb
	 * @param encoding
	 * @throws IOException
	 */
	public static final void write( byte [][][] val, byte [][][] template,  ObjectOutputStream oos ) throws IOException {
		write( val, template, 0, val[0].length, 0, val[0][0].length, oos ) ;
	}
	
	
	/**
	 * Using the minimum-length encoding, writes the provided 'val' array
	 * to the provided ObjectOutputStream.  The provided 'template', if non-null,
	 * must ALWAYS be provided when this value is read back using fromString.
	 * An encoding which assumes the existence of these values might be used by
	 * this method.
	 * 
	 * All encodings include an explicit representation of row and column size; these
	 * values represent the row/column dimensions specified by *Start and *Lim, which
	 * may be smaller than 'val' itself.
	 * 
	 * The result may be read back in to any 3d array of the same dimensions, or a
	 * larger array with pre-specified boundaries that exactly match.  Because of
	 * this "exact match" condition, it is recommended that some redundancy be
	 * used in representation if you wish to use pre-allocated arrays (rather
	 * than arrays allocated at the time the object is read).
	 * 
	 * @param val
	 * @param template
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @param oos
	 * @param sb
	 * @param encoding
	 * @throws IOException
	 */
	public static final void write(
			byte [][][] val, byte [][][] template, 
			int rowStart, int rowLim, int colStart, int colLim,
			ObjectOutputStream oos ) throws IOException {
		
		if ( rowLim < rowStart )
			throw new IllegalArgumentException("Limits must include >= 0 rows") ;
		if ( colLim < colStart )
			throw new IllegalArgumentException("Limits must include >= 0 cols") ;
		
		int encoding = shortestEncoding( val,  template, rowStart, rowLim, colStart, colLim ) ;
		writeQCombinations( val, template, rowStart, rowLim, colStart, colLim, oos, null, encoding ) ;
	}
	
	
	/**
	 * Using the minimum-length encoding, returns a String representation of the
	 * provided 'val' QCombination array.
	 * 
	 * All encodings include an explicit representation of row and column size.
	 * 
	 * The result may be read back in to any 3d array of the same dimensions, or a
	 * larger array with pre-specified boundaries that exactly match.  Because of
	 * this "exact match" condition, it is recommended that some redundancy be
	 * used in representation if you wish to use pre-allocated arrays (rather
	 * than arrays allocated at the time the object is read).
	 * 
	 * @param val
	 * @param template
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @param oos
	 * @param sb
	 * @param encoding
	 * @throws IOException
	 */
	public static final String toString( byte [][][] val ) throws IOException {
		return toString( val, 0, val[0].length, 0, val[0][0].length ) ;
	}
	
	/**
	 * Using the minimum-length encoding, returns a String representation of the
	 * provided 'val' QCombination array.
	 * 
	 * All encodings include an explicit representation of row and column size; these
	 * values represent the row/column dimensions specified by *Start and *Lim, which
	 * may be smaller than 'val' itself.
	 * 
	 * The result may be read back in to any 3d array of the same dimensions, or a
	 * larger array with pre-specified boundaries that exactly match.  Because of
	 * this "exact match" condition, it is recommended that some redundancy be
	 * used in representation if you wish to use pre-allocated arrays (rather
	 * than arrays allocated at the time the object is read).
	 * 
	 * @param val
	 * @param template
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @param oos
	 * @param sb
	 * @param encoding
	 * @throws IOException
	 */
	public static final String toString(
			byte [][][] val,
			int rowStart, int rowLim, int colStart, int colLim ) throws IOException {
		
		if ( rowLim < rowStart )
			throw new IllegalArgumentException("Limits must include >= 0 rows") ;
		if ( colLim < colStart )
			throw new IllegalArgumentException("Limits must include >= 0 cols") ;

		int encoding = shortestEncoding( val, null, rowStart, rowLim, colStart, colLim ) ;
		StringBuilder sb = new StringBuilder() ;
		writeQCombinations( val, null, rowStart, rowLim, colStart, colLim, null, sb, encoding ) ;
		return sb.toString() ;
	}
	
	
	/**
	 * Using the minimum-length encoding, returns a String representation of the
	 * provided 'val' QCombination array.  The provided 'template', if non-null,
	 * must ALWAYS be provided when this value is read back using fromString.
	 * An encoding which assumes the existence of these values might be used by
	 * this method.
	 * 
	 * All encodings include an explicit representation of row and column size.
	 * 
	 * The result may be read back in to any 3d array of the same dimensions, or a
	 * larger array with pre-specified boundaries that exactly match.  Because of
	 * this "exact match" condition, it is recommended that some redundancy be
	 * used in representation if you wish to use pre-allocated arrays (rather
	 * than arrays allocated at the time the object is read).
	 * 
	 * @param val
	 * @param template
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @param oos
	 * @param sb
	 * @param encoding
	 * @throws IOException
	 */
	public static final String toString( byte [][][] val, byte [][][] template ) throws IOException {
		return toString( val, template, 0, val[0].length, 0, val[0][0].length ) ;
	}
	
	/**
	 * Using the minimum-length encoding, returns a String representation of the
	 * provided 'val' QCombination array.  The provided 'template', if non-null,
	 * must ALWAYS be provided when this value is read back using fromString.
	 * An encoding which assumes the existence of these values might be used by
	 * this method.
	 * 
	 * All encodings include an explicit representation of row and column size; these
	 * values represent the row/column dimensions specified by *Start and *Lim, which
	 * may be smaller than 'val' itself.
	 * 
	 * The result may be read back in to any 3d array of the same dimensions, or a
	 * larger array with pre-specified boundaries that exactly match.  Because of
	 * this "exact match" condition, it is recommended that some redundancy be
	 * used in representation if you wish to use pre-allocated arrays (rather
	 * than arrays allocated at the time the object is read).
	 * 
	 * @param val
	 * @param template
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @param oos
	 * @param sb
	 * @param encoding
	 * @throws IOException
	 */
	public static final String toString(
			byte [][][] val, byte [][][] template, 
			int rowStart, int rowLim, int colStart, int colLim ) throws IOException {
		
		if ( rowLim < rowStart )
			throw new IllegalArgumentException("Limits must include >= 0 rows") ;
		if ( colLim < colStart )
			throw new IllegalArgumentException("Limits must include >= 0 cols") ;
		
		int encoding = shortestEncoding( val,  template, rowStart, rowLim, colStart, colLim ) ;
		StringBuilder sb = new StringBuilder() ;
		writeQCombinations( val, template, rowStart, rowLim, colStart, colLim, null, sb, encoding ) ;
		return sb.toString() ;
	}
	
	
	/**
	 * Using the specified encoding, writes the provided QCombination array 'val'
	 * to all non-null output objects.
	 * 
	 * All encoding include an explicit representation of row and column size; these
	 * values represent the row/column dimensions specified by *Start and *Lim, which
	 * may be smaller than 'val' itself.
	 * 
	 * The result may be read back in to any 3d array of the same dimensions, or a
	 * larger array with pre-specified boundaries that exactly match.  Because of
	 * this "exact match" condition, it is recommended that some redundancy be
	 * used in representation if you wish to use pre-allocated arrays (rather
	 * than arrays allocated at the time the object is read).
	 * 
	 * @param val
	 * @param template
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @param oos
	 * @param sb
	 * @param encoding
	 * @throws IOException
	 */
	private static final void writeQCombinations(
			byte [][][] val, byte [][][] template, 
			int rowStart, int rowLim, int colStart, int colLim,
			ObjectOutputStream oos, StringBuilder sb, int encoding ) throws IOException {
		
		int R = rowLim - rowStart ;
		int C = colLim - colStart ;
		
		int r, c ;
		
		switch( encoding ) {
		case ENCODING_FULL:
			// write full encoding
			if ( sb != null )
				sb.append(BRACE_OPEN).append(" ") ;
			writeInt( oos, sb, encoding ) ;
			writeInt( oos, sb, R ) ;
			writeInt( oos, sb, C ) ;
			if ( sb != null )
				sb.append(BRACE_OPEN).append("\n") ;
			
			for ( r = rowLim-1; r >= rowStart; r-- ) {
				for ( c = colStart; c < colLim; c++ ) {
					writeInt( oos, sb, QCombinations.encode(val, r, c) ) ;
				}
				if ( sb != null )
					sb.append("\n") ;
			}
			if ( sb != null )
				sb.append(BRACE_CLOSE).append(" ").append(BRACE_CLOSE).append("\n") ;
			return ;
			
		case ENCODING_SPARSE:
			// sparse encoding.  val [#val, #nonvals, {nonvals}]*
			// until all entries are full.
			if ( sb != null )
				sb.append(BRACE_OPEN).append(" ") ;
			writeInt( oos, sb, encoding ) ;
			writeInt( oos, sb, R ) ;
			writeInt( oos, sb, C ) ;
			if ( sb != null )
				sb.append(BRACE_OPEN).append("\n") ;
			// determine the most common value and write it
			int mostCommonQC = mostCommonQCombination( val, rowStart, rowLim, colStart, colLim ) ;
			writeInt( oos, sb, mostCommonQC ) ;
			if ( sb != null )
				sb.append("\n") ;
			
			// sparse representation: count the number of sequential 
			// 'mostCommonQCs,' then the number of non-'mostCommonQCs.'
			// Then write both numbers, and the sequence of non-'mostCommonQCs.'
			r = rowStart ;
			c = colStart ;
			while ( r < rowLim ) {
				int numMC = countSequentialEquality( val, mostCommonQC, r, c, rowStart, rowLim, colStart, colLim ) ;
				c += numMC ;
				while( c >= colLim ) {
					c -= C ;
					r += 1 ;
				}
				int numNMC = countSequentialInequality( val, mostCommonQC, r, c, rowStart, rowLim, colStart, colLim ) ;
				// write
				writeInt( oos, sb, numMC ) ;
				writeInt( oos, sb, numNMC ) ;
				for ( int i = 0; i < numNMC; i++ ) {
					writeInt( oos, sb, QCombinations.encode(val, r, c) ) ;
					c++ ;
					if ( c >= colLim ) {
						c -= C ;
						r += 1 ;
					}
				}
				if ( sb != null )
					sb.append("\n") ;
			}
			if ( sb != null )
				sb.append(BRACE_CLOSE).append(" ").append(BRACE_CLOSE).append("\n") ;
			return ;
			
		case ENCODING_SPARSE_DIFFERENCE:
			// sparse difference.  [#match, #nonmatch, {nonmatches}]
			if ( sb != null )
				sb.append(BRACE_OPEN).append(" ") ;
			writeInt( oos, sb, encoding ) ;
			writeInt( oos, sb, R ) ;
			writeInt( oos, sb, C ) ;
			if ( sb != null )
				sb.append(BRACE_OPEN).append("\n") ;
			
			r = rowStart ;
			c = colStart ;
			while ( r < rowLim ) {
				int numMC = countSequentialEquality( val, template, r, c, rowStart, rowLim, colStart, colLim ) ;
				c += numMC ;
				while( c >= colLim ) {
					c -= C ;
					r += 1 ;
				}
				int numNMC = countSequentialInequality( val, template, r, c, rowStart, rowLim, colStart, colLim ) ;
				// write
				writeInt( oos, sb, numMC ) ;
				writeInt( oos, sb, numNMC ) ;
				for ( int i = 0; i < numNMC; i++ ) {
					writeInt( oos, sb, QCombinations.encode(val, r, c) ) ;
					c++ ;
					if ( c >= colLim ) {
						c -= C ;
						r += 1 ;
					}
				}
				if ( sb != null )
					sb.append("\n") ;
			}
			if ( sb != null )
				sb.append(BRACE_CLOSE).append(" ").append(BRACE_CLOSE).append("\n") ;
			return ;
			
		}
		
		throw new IllegalArgumentException("Encoding " + encoding + " is not a supported encoding type") ;
	}
	
	
	/**
	 * Writes the specified integer value to all non-null output objects.
	 * 
	 * @param oos
	 * @param sb
	 * @param val
	 * @throws IOException
	 */
	private static final void writeInt( ObjectOutputStream oos, StringBuilder sb, int val ) throws IOException {
		
		if ( oos != null )
			oos.writeInt(val) ;
		
		if ( sb != null )
			sb.append(val).append(" ") ;
		
	}
	
	
	/**
	 * Returns one of ENCODING_*, representing the shortest encoding possible.
	 * If 'template' is null, this will be one of ENCODING_FULL and
	 * ENCODING_SPARSE.  If not, then ENCODING_SPARSE_DIFFERENCE is also possible.
	 * 
	 * @param val
	 * @param template
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @return
	 */
	private static final int shortestEncoding(
			byte [][][] val, byte [][][] template,
			int rowStart, int rowLim, int colStart, int colLim ) {
		
		// encoding lengths:
		// full is R * C + 3.
		// sparse requires finding the most common, then counting
		// 	 the non-most-common contiguous blocks: length is the 
		// 	 total number of non-most-common entries, plus 2*#contiguous,
		//	 plus 4.
		// sparse difference is the same, except instead of comparing
		//	 against a single "most common" value, we compare against 
		//   the template.  If template is null, do not attempt this encoding.
		
		int R = (rowLim - rowStart) ;
		int C = (colLim - colStart) ;
		
		int r, c ;
		
		int fullLen = R * C + 3 ;
		
		int sparseLen = 4 ;
		int mostCommonQC = mostCommonQCombination( val, rowStart, rowLim, colStart, colLim ) ;
		r = rowStart ;
		c = colStart ;
		while ( r < rowLim ) {
			int numMC = countSequentialEquality( val, mostCommonQC, r, c, rowStart, rowLim, colStart, colLim ) ;
			c += numMC ;
			while( c >= colLim ) {
				c -= C ;
				r += 1 ;
			}
			int numNMC = countSequentialInequality( val, mostCommonQC, r, c, rowStart, rowLim, colStart, colLim ) ;
			c += numNMC ;
			sparseLen += 2 + numNMC ;
			while( c >= colLim ) {
				c -= C ;
				r += 1 ;
			}
		}
		
		int sparseDiffLen = Integer.MAX_VALUE ;
		if ( template != null ) {
			sparseDiffLen = 3 ;
			r = rowStart ;
			c = colStart ;
			while ( r < rowLim ) {
				int numMC = countSequentialEquality( val, template, r, c, rowStart, rowLim, colStart, colLim ) ;
				c += numMC ;
				while( c >= colLim ) {
					c -= C ;
					r += 1 ;
				}
				int numNMC = countSequentialInequality( val, template, r, c, rowStart, rowLim, colStart, colLim ) ;
				c += numNMC ;
				sparseDiffLen += 2 + numNMC ;
				while( c >= colLim ) {
					c -= C ;
					r += 1 ;
				}
			}
		}
		
		if ( fullLen <= sparseLen && fullLen <= sparseDiffLen ) {
			//System.out.println("-------------------------------------- encoding FULL") ;
			return ENCODING_FULL ;
		}
		
		if ( sparseLen <= sparseDiffLen ) {
			//System.out.println("-------------------------------------- encoding SPARSE") ;
			return ENCODING_SPARSE ;
		}
		
		//System.out.println("-------------------------------------- encoding SPARSE DIFFERENCE") ;
		return ENCODING_SPARSE_DIFFERENCE ;
	}
	
	
	/**
	 * Returns the QCombination that is most common within the provided array
	 * (within the specified limits).
	 * 
	 * One possible use is to determine a default value for sparse encoding.
	 * 
	 * @param val
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @return
	 */
	synchronized private static final int mostCommonQCombination( byte [][][] val, int rowStart, int rowLim, int colStart, int colLim ) {
		for ( int i = 0; i < QCombinations.NUM; i++ )
			qc_counts[i] = 0 ;
		
		for ( int r = rowStart; r < rowLim; r++ )
			for ( int c = colStart; c < colLim; c++ )
				qc_counts[ QCombinations.encode( val, r, c ) ]++ ;
		
		int max = 0, maxQC = 0 ;
		for ( int i = 0; i < QCombinations.NUM; i++ ) {
			if ( qc_counts[i] > max ) {
				max = qc_counts[i] ;
				maxQC = i ;
			}
		}
		
		return maxQC ;
	}
	
	
	/**
	 * Beginning with r,c in 'val', counts and returns the number
	 * of sequential values (moving rightward and then downward within
	 * the specified limits) that are EQUAL to 'compareTo'
	 * 
	 * @param val
	 * @param compareTo
	 * @param r
	 * @param c
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @return
	 */
	private static final int countSequentialEquality(
			byte [][][] val, int compareTo,
			int r, int c, int rowStart, int rowLim, int colStart, int colLim ) {
		
		return countSequential( val, null, compareTo, COUNT_SEQUENTIAL_EQUAL_INT, r, c, rowStart, rowLim, colStart, colLim ) ;
	}
	
	
	/**
	 * Beginning with r,c in 'val', counts and returns the number
	 * of sequential values (moving rightward and then downward within
	 * the specified limits) that are NOT EQUAL to 'compareTo'
	 * 
	 * @param val
	 * @param compareTo
	 * @param r
	 * @param c
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @return
	 */
	private static final int countSequentialInequality( 
			byte [][][] val, int compareTo,
			int r, int c, int rowStart, int rowLim, int colStart, int colLim ) {
		
		return countSequential( val, null, compareTo, COUNT_SEQUENTIAL_INEQUAL_INT, r, c, rowStart, rowLim, colStart, colLim ) ;
	}
	
	
	/**
	 * Beginning with r,c in 'val', counts and returns the number
	 * of sequential values (moving rightward and then downward within
	 * the specified limits) that are EQUAL to the corresponding value
	 * in 'template.'
	 * 
	 * @param val
	 * @param template
	 * @param r
	 * @param c
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @return
	 */
	private static final int countSequentialEquality(
			byte [][][] val, byte [][][] template,
			int r, int c, int rowStart, int rowLim, int colStart, int colLim ) {
		
		return countSequential( val, template, -1, COUNT_SEQUENTIAL_EQUAL_TEMPLATE, r, c, rowStart, rowLim, colStart, colLim ) ;
	}
	
	/**
	 * Beginning with r,c in 'val', counts and returns the number
	 * of sequential values (moving rightward and then downward within
	 * the specified limits) that are NOT EQUAL to the corresponding value
	 * in 'template.'
	 * 
	 * @param val
	 * @param template
	 * @param r
	 * @param c
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @return
	 */
	private static final int countSequentialInequality(
			byte [][][] val, byte [][][] template,
			int r, int c, int rowStart, int rowLim, int colStart, int colLim ) {
		
		return countSequential( val, template, -1, COUNT_SEQUENTIAL_INEQUAL_TEMPLATE, r, c, rowStart, rowLim, colStart, colLim ) ;
	}
	
	
	
	/**
	 * Beginning from 'r', 'c' in 'val', counts and returns the number of sequential
	 * (reading across columns and then down rows) values which fit the specified
	 * sequential type.
	 * 
	 * @param val
	 * @param template
	 * @param equalTo
	 * @param countSequentialType
	 * @param r
	 * @param c
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @return
	 */
	private static final int countSequential(
			byte [][][] val, byte [][][] template, int equalTo, int countSequentialType,
			int r, int c, int rowStart, int rowLim, int colStart, int colLim ) {
		
		int count = 0 ;
		
		int C = (colLim - colStart) ;
		
		while( c >= colLim ) {
			c -= C ;
			r++ ;
		}
		
		while ( r >= rowStart && r < rowLim ) {
			int qc = QCombinations.encode(val, r, c) ;
			if ( countSequentialType == COUNT_SEQUENTIAL_EQUAL_INT && qc != equalTo )
				break ;
			if ( countSequentialType == COUNT_SEQUENTIAL_INEQUAL_INT && qc == equalTo )
				break ;
			if ( countSequentialType == COUNT_SEQUENTIAL_EQUAL_TEMPLATE && qc != QCombinations.encode(template, r, c) )
				break ;
			if ( countSequentialType == COUNT_SEQUENTIAL_INEQUAL_TEMPLATE && qc == QCombinations.encode(template, r, c) )
				break ;
			
			count++ ;
			c++ ;
			
			if ( c >= colLim ) {
				c = colStart ;
				r++ ;
			}
		}
		
		return count ;
	}
	
	
	
	/**
	 * Reads a QCombination array from the provided ObjectInputStream.  If
	 * 'val' is provided, its dimensions must exactly match those specified
	 * in ois; it will be populated and a reference to it returned.
	 * 
	 * If 'val' is null, a new array will be allocated with dimensions as specified
	 * by ois, populated, and returned.
	 * 
	 * @param val
	 * @param ois
	 * @return
	 * @throws IOException
	 */
	public static final byte [][][] read( byte [][][] val, ObjectInputStream ois ) throws IOException {
		int rowLim = val != null ? val[0].length : -1 ;
		int colLim = val != null ? val[0][0].length : -1 ;
		return read( val, null, 0, rowLim, 0, colLim, ois, null ) ;
	}
	
	
	/**
	 * Reads a QCombination array from the provided ObjectInputStream.
	 * The limits specified by *Start, *Lim must exactly match those of
	 * the array read from ois.
	 * 
	 * @param val
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @param ois
	 * @return
	 * @throws IOException
	 */
	public static final void read( byte [][][] val, int rowStart, int rowLim, int colStart, int colLim, ObjectInputStream ois ) throws IOException {
		if ( rowLim <= rowStart )
			throw new IllegalArgumentException("Limits must include > 0 rows") ;
		if ( colLim <= colStart )
			throw new IllegalArgumentException("Limits must include > 0 cols") ;
		
		read( val, null, rowStart, rowLim, colStart, colLim, ois, null ) ;
	}
	
	/**
	 * Reads a QCombination array from the provided ObjectInputStream.  If
	 * 'val' is provided, its dimensions must exactly match those specified
	 * in ois; it will be populated and a reference to it returned.
	 * 
	 * If 'val' is null, a new array will be allocated with dimensions as specified
	 * by ois, populated, and returned.
	 * 
	 * If 'template' is provided (template MUST be provided if the array was written
	 * to the stream using a template!), its dimensions must match both 'val' (if provided)
	 * and those written in the ois.
	 * 
	 * @param val
	 * @param ois
	 * @return
	 * @throws IOException
	 */
	public static final byte [][][] read( byte [][][] val, byte [][][] template, ObjectInputStream ois ) throws IOException {
		if ( val == null && template != null )
			val = new byte[2][template[0].length][template[0][0].length] ;
		if ( template != null && ( val[0].length != template[0].length || val[0][0].length != template[0][0].length ) )
			throw new IllegalArgumentException("'val' and 'template' dimensions must match") ;
		
		return read( val, template, 0, val[0].length, 0, val[0][0].length, ois, null ) ;
	}
	
	
	/**
	 * Reads a QCombination array from the provided ObjectInputStream.
	 * The limits specified by *Start, *Lim must exactly match those of
	 * the array read from ois.
	 * 
	 * If 'template' is provided (template MUST be provided if the array was written
	 * to the stream using a template!), its dimensions must match both 'val'
	 * and those written in the ois.
	 * 
	 * @param val
	 * @param ois
	 * @return
	 * @throws IOException
	 */
	public static final byte [][][] read( byte [][][] val, byte [][][] template, int rowStart, int rowLim, int colStart, int colLim, ObjectInputStream ois ) throws IOException {
		if ( rowLim < rowStart )
			throw new IllegalArgumentException("Limits must include >= 0 rows") ;
		if ( colLim < colStart )
			throw new IllegalArgumentException("Limits must include >= 0 cols") ;
		if ( val == null && template != null )
			val = new byte[2][template[0].length][template[0][0].length] ;
		if ( template != null && ( val[0].length != template[0].length || val[0][0].length != template[0][0].length ) )
			throw new IllegalArgumentException("'val' and 'template' dimensions must match") ;
			
		return read( val, template, rowStart, rowLim, colStart, colLim, ois, null ) ;
	}
	
	
	
	/**
	 * Reads a QCombination array from the provided ObjectInputStream.  If
	 * 'val' is provided, its dimensions must exactly match those specified
	 * in ois; it will be populated and a reference to it returned.
	 * 
	 * If 'val' is null, a new array will be allocated with dimensions as specified
	 * by ois, populated, and returned.
	 * 
	 * @param val
	 * @param ois
	 * @return
	 * @throws IOException
	 */
	public static final byte [][][] read( byte [][][] val, String str ) throws IOException {
		int rowLim = val != null ? val[0].length : -1 ;
		int colLim = val != null ? val[0][0].length : -1 ;
		return read( val, null, 0, rowLim, 0, colLim, null, str ) ;
	}
	
	
	/**
	 * Reads a QCombination array from the provided ObjectInputStream.
	 * The limits specified by *Start, *Lim must exactly match those of
	 * the array read from ois.
	 * 
	 * @param val
	 * @param rowStart
	 * @param rowLim
	 * @param colStart
	 * @param colLim
	 * @param ois
	 * @return
	 * @throws IOException
	 */
	public static final void read( byte [][][] val, int rowStart, int rowLim, int colStart, int colLim, String str ) throws IOException {
		if ( rowLim <= rowStart )
			throw new IllegalArgumentException("Limits must include > 0 rows") ;
		if ( colLim <= colStart )
			throw new IllegalArgumentException("Limits must include > 0 cols") ;
		
		read( val, null, rowStart, rowLim, colStart, colLim, null, str ) ;
	}
	
	/**
	 * Reads a QCombination array from the provided ObjectInputStream.  If
	 * 'val' is provided, its dimensions must exactly match those specified
	 * in ois; it will be populated and a reference to it returned.
	 * 
	 * If 'val' is null, a new array will be allocated with dimensions as specified
	 * by ois, populated, and returned.
	 * 
	 * If 'template' is provided (template MUST be provided if the array was written
	 * to the stream using a template!), its dimensions must match both 'val' (if provided)
	 * and those written in the ois.
	 * 
	 * @param val
	 * @param ois
	 * @return
	 * @throws IOException
	 */
	public static final byte [][][] read( byte [][][] val, byte [][][] template, String str ) throws IOException {
		if ( val == null && template != null )
			val = new byte[2][template[0].length][template[0][0].length] ;
		if ( template != null && ( val[0].length != template[0].length || val[0][0].length != template[0][0].length ) )
			throw new IllegalArgumentException("'val' and 'template' dimensions must match") ;
		
		return read( val, template, 0, val[0].length, 0, val[0][0].length, null, str ) ;
	}
	
	
	/**
	 * Reads a QCombination array from the provided String.
	 * The limits specified by *Start, *Lim must exactly match those of
	 * the array read from ois.
	 * 
	 * If 'template' is provided (template MUST be provided if the array was written
	 * to the stream using a template!), its dimensions must match both 'val'
	 * and those written in the ois.
	 * 
	 * @param val
	 * @param ois
	 * @return
	 * @throws IOException
	 */
	public static final byte [][][] read( byte [][][] val, byte [][][] template, int rowStart, int rowLim, int colStart, int colLim, String str ) throws IOException {
		if ( rowLim < rowStart )
			throw new IllegalArgumentException("Limits must include >= 0 rows") ;
		if ( colLim < colStart )
			throw new IllegalArgumentException("Limits must include >= 0 cols") ;
		if ( val == null && template != null )
			val = new byte[2][template[0].length][template[0][0].length] ;
		if ( template != null && ( val[0].length != template[0].length || val[0][0].length != template[0][0].length ) )
			throw new IllegalArgumentException("'val' and 'template' dimensions must match") ;
			
		return read( val, template, rowStart, rowLim, colStart, colLim, null, str ) ;
	}
	
	
	
	private static final byte [][][] read(
			byte [][][] val, byte [][][] template,
			int rowStart, int rowLim, int colStart, int colLim,
			ObjectInputStream ois, String str ) throws IOException {
		
		int r, c ;
		
		// First: if a string is provided, we should break it up on whitespace
		String [] strArray = null ;
		int strArrayIndex = 0 ;
		if ( str != null ) {
			strArray = str.split("\\s+") ;
			while ( strArray[strArrayIndex].length() == 0 )
				strArrayIndex++ ;
			// sanity check: [?
			eat( BRACE_OPEN, strArray, strArrayIndex++ ) ;
		}
		
		// Second thing: read the encoding, rows, and columns.
		int encoding = readInt( ois, strArray, strArrayIndex++ ) ;
		int R = readInt( ois, strArray, strArrayIndex++ ) ;
		int C = readInt( ois, strArray, strArrayIndex++ ) ;
		eat( BRACE_OPEN, strArray, strArrayIndex++ ) ;
		
		
		// valid rows , cols?
		if ( R < 0 )
			throw new IllegalArgumentException("R:" + R + " is not valid") ;
		if ( C < 0 )
			throw new IllegalArgumentException("C:" + C + " is not valid") ;
		
		// Check R, C, encoding against the parameters provided.
		if ( encoding == ENCODING_SPARSE_DIFFERENCE && template == null )
			throw new IllegalArgumentException("Must proved a template for sparse difference encoding") ;
		
		// If val is null and limits are -1, allocate.  Make sure
		// template is also null (otherwise we would have allocated 'val'
		// already).
		if ( val == null ) {
			if ( template != null )
				throw new IllegalArgumentException("Template non-null, val null; should have been preallocated by caller") ;
			if ( rowLim >= 0 || colLim >= 0 )
				throw new IllegalArgumentException("Limits inappropriate for null 'val'") ;
			val = new byte[2][R][C] ;
			rowStart = colStart = 0 ;
			rowLim = R ;
			colLim = C ;
		}
		
		// Check that limits exactly match R, C.
		int rows = rowLim - rowStart ;
		int cols = colLim - colStart ;
		if ( rows != R || cols != C )
			throw new IllegalArgumentException("Limits " + rows + ", " + cols + " do not match read R:" + R + ", C:" + C) ;
		
		// ready to read.
		switch( encoding ) {
		case ENCODING_FULL:	
			// complete list of QCombinations.
			for ( r = rowLim-1; r >= rowStart; r-- )
				for ( c = colStart; c < colLim; c++ )
					QCombinations.setAs( val, r, c, readInt( ois, strArray, strArrayIndex++ ) ) ;
			eat( BRACE_CLOSE, strArray, strArrayIndex++ ) ;
			eat( BRACE_CLOSE, strArray, strArrayIndex++ ) ;
			return val ;
			
		case ENCODING_SPARSE:
			// first read the default value
			int defaultVal = readInt( ois, strArray, strArrayIndex++ ) ;
			// now start reading.  If goes like this: as long as there
			// are values yet unfilled, read the number of default values
			// (and write them), then read the number of non-default values,
			// and read-write that many.
			r = rowStart ;
			c = colStart ;
			while ( r < rowLim ) {
				int numDefault = readInt( ois, strArray, strArrayIndex++ ) ;
				for ( int i = 0; i < numDefault; i++ ) {
					QCombinations.setAs(val, r, c, defaultVal) ;
					c++ ;
					if ( c >= colLim ) {
						c = colStart ;
						r++ ;
					}
				}
				int numNon = readInt( ois, strArray, strArrayIndex++ ) ;
				for ( int i = 0; i < numNon; i++ ) {
					QCombinations.setAs( val, r, c, readInt( ois, strArray, strArrayIndex++ ) ) ;
					c++ ;
					if ( c >= colLim ) {
						c = colStart ;
						r++ ;
					}
				}
			}
			eat( BRACE_CLOSE, strArray, strArrayIndex++ ) ;
			eat( BRACE_CLOSE, strArray, strArrayIndex++ ) ;
			return val ;
			
		case ENCODING_SPARSE_DIFFERENCE:
			// as long as there are values yet unfilled, read the number
			// of matching values (and write them), then read the number
			// of non-matching values and read-write that many.
			r = rowStart ;
			c = colStart ;
			while ( r < rowLim ) {
				int numMatching = readInt( ois, strArray, strArrayIndex++ ) ;
				for ( int i = 0; i < numMatching; i++ ) {
					for ( int q = 0; q < 2; q++ )
						val[q][r][c] = template[q][r][c] ;
					c++ ;
					if ( c >= colLim ) {
						c = colStart ;
						r++ ;
					}
				}
				int numNon = readInt( ois, strArray, strArrayIndex++ ) ;
				for ( int i = 0; i < numNon; i++ ) {
					QCombinations.setAs( val, r, c, readInt( ois, strArray, strArrayIndex++ ) ) ;
					c++ ;
					if ( c >= colLim ) {
						c = colStart ;
						r++ ;
					}
				}
			}
			eat( BRACE_CLOSE, strArray, strArrayIndex++ ) ;
			eat( BRACE_CLOSE, strArray, strArrayIndex++ ) ;
			return val ;
		}
		
		// this shouldn't ever happen.
		throw new IllegalArgumentException("Encoding " + encoding + " is not valid") ;
	}
	
	
	/**
	 * Reads and returns an integer value represented in the provided input objects.
	 * For string representations, both explicit integers and QCombinations "string"
	 * representations are acceptable.
	 * 
	 * If exactly one provided input source is non-null, it will be read from; in
	 * more than one is non-null, than exactly one will be read from and it will
	 * be deterministically chosen, but we do not specify which, so this may change
	 * between implementations.
	 * 
	 * Throws an exception if a valid integer is not found in the non-null input stream
	 * from which we read, or if all inputs are null.
	 * 
	 * @param ois
	 * @param strArray
	 * @param strArrayIndex
	 * @return
	 * @throws IOException 
	 */
	private static final int readInt( ObjectInputStream ois, String [] strArray, int strArrayIndex ) throws IOException {
		
		if ( ois != null )
			return ois.readInt() ;
		
		if ( strArray != null ) {
			try {
				return Integer.parseInt( strArray[strArrayIndex] ) ;
			} catch (NumberFormatException nfe) {
				return QCombinations.decodeString( strArray[strArrayIndex] ) ;
			}
		}
		
		throw new NullPointerException("Only null object inputs given") ;
	}
	
	
	/**
	 * If strArrayIndex is equal to the expected string, do nothing.  If strArray
	 * is null, do nothing.  If strArray is non-null but does not match, throw an exception.
	 * 
	 * @param expected
	 * @param strArray
	 * @param strArrayIndex
	 */
	private static final void eat( String expected, String [] strArray, int strArrayIndex ) {
		if ( strArray == null )
			return ;
		if ( !expected.equals(strArray[strArrayIndex]) )
			throw new IllegalArgumentException("Excepted string " + expected + " not found; instead " + ( (strArray == null || strArrayIndex < 0 || strArray.length <= strArrayIndex) ? "invalid size" : strArray[strArrayIndex] ) ) ;
	}
	
}
