package com.peaceray.quantro.utils;

import java.util.ArrayList;
import java.util.Random;

public class ArrayOps {
	
	//private static final String TAG = "ArrayOps" ;
	
	private static final Random r = new Random() ;
	
	public static void randomize( int [] ar ) {
		int len = ar.length ;
		
		int el ;
		int rn ;
		for ( int it = 0; it < len; it++ ) {
			el = ar[it] ;
			rn = r.nextInt(len) ;
			ar[it] = ar[rn] ;
			ar[rn] = el ;
		}
	}
	
	
	public static int [] range(int min, int max) {
		int [] vals = new int[max-min+1] ;
		for ( int i = 0; i < vals.length; i++ )
			vals[i] = i + min ;
		return vals ;
	}
	
	public static boolean [] duplicate( boolean [] src ) {
		if ( src == null )
			return null ;
		boolean [] dst = new boolean[src.length] ;
		return copyInto(src, dst) ;
	}
	
	public static boolean [] copyInto( boolean [] src, boolean [] dst ) {
		int L = src.length ; 
		for ( int i = 0; i < L; i++ )
			dst[i] = src[i] ;
		return dst ;
	}
	
	public static int [] duplicate( int [] src ) {
		if ( src == null )
			return null ;
		int [] dst = new int[src.length] ;
		return copyInto(src, dst) ;
	}
	
	public static int [] copyInto( int [] src, int [] dst ) {
		int L = src.length ; 
		if ( dst == null )
			dst = new int[L] ;
		for ( int i = 0; i < L; i++ )
			dst[i] = src[i] ;
		return dst ;
	}
	
	public static byte [] copyInto( byte [] src, byte [] dst ) {
		int L = src.length ; 
		for ( int i = 0; i < L; i++ )
			dst[i] = src[i] ;
		return dst ;
	}
	
	
	public static int [][] duplicate( int [][] src ) {
		if ( src == null )
			return null ;
		int [][] dst = new int[src.length][src[0].length] ;
		return copyInto( src, dst ) ;
	}
	
	
	
	public static int [][] copyInto( int [][] src, int [][] dst ) {
		copyInto(src,dst,0,0) ;
		return dst ;
	}
	
	public static byte [][] duplicate( byte [][] src ) {
		if ( src == null )
			return null ;
		byte [][] dst = new byte[src.length][src[0].length] ;
		return copyInto( src, dst ) ;
	}
	
	
	
	public static byte [][] copyInto( byte [][] src, byte [][] dst ) {
		copyInto(src,dst,0,0) ;
		return dst ;
	}
	
	
	public static boolean [][] duplicate( boolean [][] src ) {
		if ( src == null )
			return null ;
		boolean [][] dst = new boolean[src.length][src[0].length] ;
		return copyInto( src, dst ) ;
	}
	
	
	
	public static boolean [][] copyInto( boolean [][] src, boolean [][] dst ) {
		copyInto(src,dst,0,0) ;
		return dst ;
	}
	
	public static int [][] copyInto( int [][] src, int [][] dst, int dstFirstRow, int dstFirstCol ) {
		int R = src.length ;
		int C = src[0].length ;
		for ( int r = 0; r < R; r++ ) {
			for ( int c = 0; c < C; c++ ) {
				dst[r+dstFirstRow][c+dstFirstCol] = src[r][c] ;
			}
		}
		return dst ;
	}
	
	public static byte [][] copyInto( byte [][] src, byte [][] dst, int dstFirstRow, int dstFirstCol ) {
		int R = src.length ;
		int C = src[0].length ;
		for ( byte r = 0; r < R; r++ ) {
			for ( byte c = 0; c < C; c++ ) {
				dst[r+dstFirstRow][c+dstFirstCol] = src[r][c] ;
			}
		}
		return dst ;
	}
	
	
	public static boolean [][] copyInto( boolean [][] src, boolean [][] dst, int dstFirstRow, int dstFirstCol ) {
		int R = src.length ;
		int C = src[0].length ;
		for ( byte r = 0; r < R; r++ ) {
			for ( byte c = 0; c < C; c++ ) {
				dst[r+dstFirstRow][c+dstFirstCol] = src[r][c] ;
			}
		}
		return dst ;
	}
	
	
	public static int [][][] duplicate( int [][][] src ) {
		if ( src == null )
			return null ;
		int [][][] dst = new int[src.length][src[0].length][src[0][0].length] ;
		return copyInto( src, dst ) ;
	}
	
	
	
	public static int [][][] copyInto( int [][][] src, int [][][] dst ) {
		copyInto(src,dst,0,0) ;
		return dst ;
	}
	
	public static byte [][][] duplicate( byte [][][] src ) {
		if ( src == null )
			return null ;
		byte [][][] dst = new byte[src.length][src[0].length][src[0][0].length] ;
		return copyInto( src, dst ) ;
	}
	
	
	
	public static byte [][][] copyInto( byte [][][] src, byte [][][] dst ) {
		copyInto(src,dst,0,0) ;
		return dst ;
	}
	
	
	
	public static int [][][] copyInto( int [][][] src, int [][][] dst, int dstFirstRow, int dstFirstCol ) {
		int Q = src.length ;
		int R = src[0].length ;
		int C = src[0][0].length ;
		for ( int q = 0; q < Q; q++ ) {
			for ( int r = 0; r < R; r++ ) {
				for ( int c = 0; c < C; c++ ) {
					dst[q][r+dstFirstRow][c+dstFirstCol] = src[q][r][c] ;
				}
			}
		}
		return dst ;
	}
	
	public static byte [][][] copyInto( byte [][][] src, byte [][][] dst, int dstFirstRow, int dstFirstCol ) {
		int Q = src.length ;
		int R = src[0].length ;
		int C = src[0][0].length ;
		for ( byte q = 0; q < Q; q++ ) {
			for ( byte r = 0; r < R; r++ ) {
				for ( byte c = 0; c < C; c++ ) {
					dst[q][r+dstFirstRow][c+dstFirstCol] = src[q][r][c] ;
				}
			}
		}
		return dst ;
	}
	
	public static int [][][][] copyInto( int [][][][] src, int [][][][] dst ) {
		for ( int i = 0; i < dst.length; i++ ) {
			copyInto(src[i], dst[i]) ;
		}
		return dst ;
	}
	
	public static byte [][][][] copyInto( byte [][][][] src, byte [][][][] dst ) {
		for ( int i = 0; i < dst.length; i++ ) {
			copyInto(src[i], dst[i]) ;
		}
		return dst ;
	}
	
	/**
	 * arrayToArrayList - Constructs and returns an ArrayList of Integers,
	 * of the same dimensions and contents of the given array of "int"s.
	 * 
	 * Precondition: "ar" is a 3D array with consistent size, i.e. all
	 * 					rows have the same length, as do all columns, etc.
	 * 				It's size is at least 1 in all directions.
	 * 
	 * @param ar
	 * @return
	 */
	public static ArrayList<ArrayList<ArrayList<Integer>>> arrayToArrayList( int [][][] ar ) {
		// For efficiency, read these once (no redundant indexing)
		int outerSize = ar.length ;
		int middleSize = ar[0].length ;
		int innerSize = ar[0][0].length ;
		
		ArrayList<ArrayList<ArrayList<Integer>>> result = new ArrayList<ArrayList<ArrayList<Integer>>>() ;
		for ( int i = 0; i < outerSize; i++ ) {
			// Make the inner 2D array.
			ArrayList<ArrayList<Integer>> inner2d = new ArrayList<ArrayList<Integer>>() ;
			for ( int j = 0; j < middleSize; j++ ) {
				// Make the inner-inner 1D array.
				ArrayList<Integer> inner1d = new ArrayList<Integer>() ;
				for ( int k = 0; k < innerSize; k++ ) {
					inner1d.add( new Integer(ar[i][j][k]) ) ;
				}
				inner2d.add( inner1d ) ;
			}
			result.add( inner2d ) ;
		}
		
		return result ;
	}
	
	/**
	 * arrayToArrayList - Constructs and returns an ArrayList of Integers,
	 * of the same dimensions and contents of the given array of "int"s.
	 * 
	 * Precondition: "ar" is a 3D array with consistent size, i.e. all
	 * 					rows have the same length, as do all columns, etc.
	 * 
	 * @param ar
	 * @return
	 */
	public static int [][][] arrayListToArray( ArrayList<ArrayList<ArrayList<Integer>>> arList ) {
		// For efficiency, read these once (no redundant indexing)
		int outerSize = arList.size() ;
		int middleSize = arList.get(0).size() ;
		int innerSize = arList.get(0).get(0).size() ;
		
		int [][][] ar = new int[outerSize][middleSize][innerSize] ;
		
		for ( int i = 0; i < outerSize; i++ ) {
			// Retrieve inner2d
			ArrayList<ArrayList<Integer>> inner2d = arList.get(i) ;
			for ( int j = 0; j < middleSize; j++ ) {
				// Retrieve inner1d
				ArrayList<Integer> inner1d = inner2d.get(j) ;
				for ( int k = 0; k < innerSize; k++ ) {
					ar[i][j][k] = inner1d.get(k).intValue() ;
				}
			}
		}
		
		return ar ;
	}
	
	/**
	 * Uses "." and "," as special characters.
	 * Represents the array in this format:
	 * outerDim.middleDim.innerDim.matrix,,,matrix,,,matrix
	 *		where "matrix": row,,row,,row
	 *		and "row":		col,col,col
	 * @param ar
	 * @return
	 */
	public static String arrayToString( int [][][] ar ) {
		// For efficiency, read these once (no redundant indexing)
		int outerSize = ar.length ;
		int middleSize = ar[0].length ;
		int innerSize = ar[0][0].length ;
		
		String result = "" ;
		
		for ( int i = 0; i < outerSize; i++ ) {
			// Make the inner 2D array.
			String inner2d = "" ;
			for ( int j = 0; j < middleSize; j++ ) {
				// Make the inner-inner 1D array.
				String inner1d = "" + ar[i][j][0] ;
				for ( int k = 1; k < innerSize; k++ ) {
					inner1d = inner1d + "," + ar[i][j][k] ;
				}
				
				if ( inner2d.length() > 0 )
					inner2d = inner2d + ",," + inner1d ;
				else
					inner2d = inner1d ;
			}
			
			if ( result.length() > 0 )
				result = result + ",,," + inner2d ;
			else
				result = inner2d ;
		}
		
		return "" + outerSize + "." + middleSize + "." + innerSize + "." + result ;
	}
	
	/**
	 * Uses "." and "," as special characters.
	 * Represents the array in this format:
	 * outerDim.middleDim.innerDim.matrix,,,matrix,,,matrix
	 *		where "matrix": row,,row,,row
	 *		and "row":		col,col,col
	 * @param ar
	 * @return
	 */
	public static String arrayToString( byte [][][] ar ) {
		// For efficiency, read these once (no redundant indexing)
		int outerSize = ar.length ;
		int middleSize = ar[0].length ;
		int innerSize = ar[0][0].length ;
		
		String result = "" ;
		
		for ( int i = 0; i < outerSize; i++ ) {
			// Make the inner 2D array.
			String inner2d = "" ;
			for ( int j = 0; j < middleSize; j++ ) {
				// Make the inner-inner 1D array.
				String inner1d = "" + ar[i][j][0] ;
				for ( int k = 1; k < innerSize; k++ ) {
					inner1d = inner1d + "," + ar[i][j][k] ;
				}
				
				if ( inner2d.length() > 0 )
					inner2d = inner2d + ",," + inner1d ;
				else
					inner2d = inner1d ;
			}
			
			if ( result.length() > 0 )
				result = result + ",,," + inner2d ;
			else
				result = inner2d ;
		}
		
		return "" + outerSize + "." + middleSize + "." + innerSize + "." + result ;
	}
	
	public static int [][][] arrayFromString( String arString ) {
		String [] items = arString.split("\\.") ;

		
		int outerSize = Integer.parseInt(items[0]) ;
		int middleSize = Integer.parseInt(items[1]) ;
		int innerSize = Integer.parseInt(items[2]) ;
		
		int [][][] ar = new int[outerSize][middleSize][innerSize] ;
		
		String [] matrices = items[3].split(",,,") ;
		for ( int i = 0; i < outerSize; i++ ) {
			String [] rows = matrices[i].split(",,") ;
			for ( int j = 0; j < middleSize; j++ ) {
				String [] elems = rows[j].split(",") ;
				for ( int k = 0; k < innerSize; k++ ) {
					ar[i][j][k] = Integer.parseInt(elems[k]) ;
				}
			}
		}
		
		return ar ;
	}
	
	
	public static byte [][][] byteArrayFromString( String arString ) {
		String [] items = arString.split("\\.") ;

		
		int outerSize = Integer.parseInt(items[0]) ;
		int middleSize = Integer.parseInt(items[1]) ;
		int innerSize = Integer.parseInt(items[2]) ;
		
		byte [][][] ar = new byte[outerSize][middleSize][innerSize] ;
		
		String [] matrices = items[3].split(",,,") ;
		for ( int i = 0; i < outerSize; i++ ) {
			String [] rows = matrices[i].split(",,") ;
			for ( int j = 0; j < middleSize; j++ ) {
				String [] elems = rows[j].split(",") ;
				for ( int k = 0; k < innerSize; k++ ) {
					ar[i][j][k] = Byte.parseByte(elems[k]) ;
				}
			}
		}
		
		return ar ;
	}
	
	
	
	
	/**
	 * allocateToMatchDimensions: if 'prev' does not match the dimensions of 'model',
	 * allocate a new array which does and return it.  Otherwise, return prev.
	 * @param prev		A previous attempt at allocation; kept if it matches the dimensions of model
	 * @param model		An array whose dimensions 'prev' is checked against
	 * @return			'prev', if its dimensions match 'model'; a new array if not.
	 */
	public static final boolean [][][] allocateToMatchDimensions( boolean [][][] prev, int [][][] model ) {
		if ( prev == null || prev.length != model.length
				|| prev[0].length != model[0].length
				|| prev[0][0].length != model[0][0].length )
			return new boolean[model.length][model[0].length][model[0][0].length] ;
		return prev ;
	}
	
	/**
	 * allocateToMatchDimensions: if 'prev' does not match the dimensions of 'model',
	 * allocate a new array which does and return it.  Otherwise, return prev.
	 * @param prev		A previous attempt at allocation; kept if it matches the dimensions of model
	 * @param model		An array whose dimensions 'prev' is checked against
	 * @return			'prev', if its dimensions match 'model'; a new array if not.
	 */
	public static final boolean [][][] allocateToMatchDimensions( boolean [][][] prev, byte [][][] model ) {
		if ( prev == null || prev.length != model.length
				|| prev[0].length != model[0].length
				|| prev[0][0].length != model[0][0].length )
			return new boolean[model.length][model[0].length][model[0][0].length] ;
		return prev ;
	}
	
	
	
	/**
	 * allocateToMatchDimensions: if 'prev' does not match the dimensions of 'model',
	 * allocate a new array which does and return it.  Otherwise, return prev.
	 * @param prev		A previous attempt at allocation; kept if it matches the dimensions of model
	 * @param model		An array whose dimensions 'prev' is checked against
	 * @return			'prev', if its dimensions match 'model'; a new array if not.
	 */
	public static final int [][][] allocateToMatchDimensions( int [][][] prev, int [][][] model ) {
		if ( prev == null || prev.length != model.length
				|| prev[0].length != model[0].length
				|| prev[0][0].length != model[0][0].length )
			return new int[model.length][model[0].length][model[0][0].length] ;
		return prev ;
	}
	
	public static final int [][][][] allocateToMatchDimensions( int [][][][] prev, int [][][][] model ) {
		if ( prev == null || prev.length != model.length
				|| prev[0] == null || prev[0].length != model[0].length
				|| prev[0][0] == null || prev[0][0].length != model[0][0].length
				|| prev[0][0][0] == null || prev[0][0][0].length != model[0][0][0].length )
			return new int[model.length][model[0].length][model[0][0].length][model[0][0][0].length] ;
		return prev ;
	}
	
	/**
	 * allocateToMatchDimensions: if 'prev' does not match the dimensions of 'model',
	 * allocate a new array which does and return it.  Otherwise, return prev.
	 * @param prev		A previous attempt at allocation; kept if it matches the dimensions of model
	 * @param model		An array whose dimensions 'prev' is checked against
	 * @return			'prev', if its dimensions match 'model'; a new array if not.
	 */
	public static final byte [][][] allocateToMatchDimensions( byte [][][] prev, byte [][][] model ) {
		if ( prev == null || prev.length != model.length
				|| prev[0].length != model[0].length
				|| prev[0][0].length != model[0][0].length )
			return new byte[model.length][model[0].length][model[0][0].length] ;
		return prev ;
	}
	
	public static final byte [][][][] allocateToMatchDimensions( byte [][][][] prev, byte [][][][] model ) {
		if ( prev == null || prev.length != model.length
				|| prev[0] == null || prev[0].length != model[0].length
				|| prev[0][0] == null || prev[0][0].length != model[0][0].length
				|| prev[0][0][0] == null || prev[0][0][0].length != model[0][0][0].length )
			return new byte[model.length][model[0].length][model[0][0].length][model[0][0][0].length] ;
		return prev ;
	}



	/**
	 * setEmpty: set all elements in the given array to 'false'.
	 * @param ar	The boolean array
	 */
	public static final void setEmpty( boolean [][][] ar ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				for ( int k = 0; k < ar[0][0].length; k++ ) {
					ar[i][j][k] = false ;
				}
			}
		}
	}
	
	public static final void setEmpty( boolean [][] ar ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				ar[i][j] = false ;
			}
		}
	}
	
	public static final void setEmpty( boolean [] ar ) {
		fill( ar, false ) ;
	}
	
	public static final void fill( boolean [] ar, boolean val ) {
		for ( int i = 0; i < ar.length; i++ ) {
			ar[i] = val ;
		}
	}
	
	
	public static final void fill( int [][][][] ar, int val ) {
		for ( int i = 0; i < ar.length; i++ ) {
			fill( ar[i], val ) ;
		}
	}
	
	public static final void fill( int [][][] ar, int val ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				for ( int k = 0; k < ar[0][0].length; k++ ) {
					ar[i][j][k] = val ;
				}
			}
		}
	}
	
	/**
	 * setEmpty: set all elements in the given array to 0.
	 * @param ar	The integer array
	 */
	public static final void setEmpty( int [][][] ar ) {
		fill( ar, 0 ) ;
	}

	public static final void fill( byte [][][][] ar, byte val ) {
		for ( int i = 0; i < ar.length; i++ ) {
			fill( ar[i], val ) ;
		}
	}
	
	public static final void fill( byte [][][] ar, byte val ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				for ( int k = 0; k < ar[0][0].length; k++ ) {
					ar[i][j][k] = val ;
				}
			}
		}
	}
	
	/**
	 * setEmpty: set all elements in the given array to 0.
	 * @param ar	The integer array
	 */
	public static final void setEmpty( short [][][] ar ) {
		fill( ar, (short)0 ) ;
	}
	
	public static final void fill( short [][][] ar, short val ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				for ( int k = 0; k < ar[0][0].length; k++ ) {
					ar[i][j][k] = val ;
				}
			}
		}
	}
	
	public static final void fill( short [][][][] ar, short val ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				for ( int k = 0; k < ar[0][0].length; k++ ) {
					for ( int m = 0; m < ar[0][0][0].length; m++ ) {
						ar[i][j][k][m] = val ;
					}
				}
			}
		}
	}
	
	
	
	/**
	 * setEmpty: set all elements in the given array to 0.
	 * @param ar	The integer array
	 */
	public static final void setEmpty( byte [][][] ar ) {
		fill( ar, (byte)0 ) ;
	}
	
	public static final void fill( int [][] ar, int val ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				ar[i][j] = val ;
			}
		}
	}
	
	public static final void setEmpty( int [][] ar ) {
		fill( ar, 0 ) ;
	}
	
	public static final void fill( byte [][] ar, byte val ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				ar[i][j] = val ;
			}
		}
	}
	
	public static final void setEmpty( short [][] ar ) {
		fill( ar, (short)0 ) ;
	}
	
	public static final void fill( short [][] ar, short val ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				ar[i][j] = val ;
			}
		}
	}
	
	public static final void setEmpty( byte [][] ar ) {
		fill( ar, (byte)0 ) ;
	}
	
	public static final void setEmpty( byte [] ar ) {
		fill( ar, (byte)0 ) ;
	}
	
	public static final void fill( byte [] ar, byte val ) {
		for ( int i = 0; i < ar.length; i++ ) {
			ar[i] = val ;
		}
	}
	
	public static final boolean areEqual( byte [] ar0, byte [] ar1 ) {
		int Q = ar0.length ;
		if ( Q != ar1.length )
			return false ;
		
		for ( int i = 0; i < Q; i++ ) 
			if ( ar0[i] != ar1[i] )
				return false ;
		return true ;
	}
	
	public static final boolean areEqual( byte [][] ar0, byte [][] ar1 ) {
		int Q = ar0.length ;
		int R = ar0[0].length ;
		if ( Q != ar1.length )
			return false ;
		if ( R != ar1[0].length )
			return false ;
		
		for ( int i = 0; i < Q; i++ ) 
			for ( int j = 0; j < R; j++ ) 
				if ( ar0[i][j] != ar1[i][j] )
					return false ;
		return true ;
	}
	
	public static final boolean areEqual( byte [][][] ar0, byte [][][] ar1 ) {
		int Q = ar0.length ;
		int R = ar0[0].length ;
		int C = ar0[0][0].length ;
		if ( Q != ar1.length )
			return false ;
		if ( R != ar1[0].length )
			return false ;
		if ( C != ar1[0][0].length )
			return false ;
		
		for ( int i = 0; i < Q; i++ ) 
			for ( int j = 0; j < R; j++ ) 
				for ( int k = 0; k < C; k++ ) 
					if ( ar0[i][j][k] != ar1[i][j][k] )
						return false ;
		return true ;
	}
	
	public static final boolean areEqual( int [][][] ar0, int [][][] ar1 ) {
		int Q = ar0.length ;
		int R = ar0[0].length ;
		int C = ar0[0][0].length ;
		if ( Q != ar1.length )
			return false ;
		if ( R != ar1[0].length )
			return false ;
		if ( C != ar1[0][0].length )
			return false ;
		
		for ( int i = 0; i < Q; i++ ) 
			for ( int j = 0; j < R; j++ ) 
				for ( int k = 0; k < C; k++ ) 
					if ( ar0[i][j][k] != ar1[i][j][k] )
						return false ;
		return true ;
	}
	
	public static final boolean isEmpty( boolean [][][] ar ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				for ( int k = 0; k < ar[0][0].length; k++ ) {
					if ( ar[i][j][k] )
						return false ;
				}
			}
		}
		return true ;
	}
	
	public static final boolean isEmpty( boolean [][] ar ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				if ( ar[i][j] )
					return false ;
			}
		}
		return true ;
	}
	
	public static final boolean isEmpty( int [][][] ar ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				for ( int k = 0; k < ar[0][0].length; k++ ) {
					if ( ar[i][j][k] != 0 )
						return false ;
				}
			}
		}
		return true ;
	}
	
	public static final boolean isEmpty( int [][] ar ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				if ( ar[i][j] != 0 )
					return false ;
			}
		}
		return true ;
	}
	
	public static final boolean isEmpty( byte [][][] ar ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				for ( int k = 0; k < ar[0][0].length; k++ ) {
					if ( ar[i][j][k] != 0 )
						return false ;
				}
			}
		}
		return true ;
	}
	
	public static final boolean isEmpty( byte [][] ar ) {
		for ( int i = 0; i < ar.length; i++ ) {
			for ( int j = 0; j < ar[0].length; j++ ) {
				if ( ar[i][j] != 0 )
					return false ;
			}
		}
		return true ;
	}
	
	public static final boolean isEmpty( byte [] ar ) {
		for ( int i = 0; i < ar.length; i++ ) {
			if ( ar[i] != 0 )
				return false ;
		}
		return true ;
	}
	
	
	/**
	 * Counts the 'true' entries in the provided array.
	 * @param ar
	 * @return
	 */
	public static final int count( boolean [] ar ) {
		int num = 0 ;
		for ( int i = 0; i < ar.length; i++ ) {
			if ( ar[i] ) {
				num++  ;
			}
		}
		return num ;
	}
	
	
	/**
	 * A useful method for converting between multidimensional and
	 * 1-d arrays.  Given a list of index pairs, where each pair
	 * is "index, length" (e.g. "0, 10" to "9, 10"), returns a single
	 * unique index into a length1 * length2 * ... * lengthN array.
	 * 
	 * @param indexOutOf
	 * @return
	 */
	public static final int flattenIndices( int ... indexOutOf ) {
		if ( indexOutOf.length % 2 == 1 )
			throw new IllegalArgumentException("Must provide pairs (Index, Length) as arguments -- i.e, an even number.") ;
		
		int flatIndex = 0 ;
		for ( int i = 0; i < indexOutOf.length / 2; i++ ) {
			int index_index = i*2 ;
			int length_index = i*2 + 1 ;
			int index = indexOutOf[i*2] ;
			int length = indexOutOf[i*2 + 1] ;
			
			// previous index selects a set of size 'length.'
			flatIndex *= ( length ) ;
			// new index provides an offset within that set
			flatIndex += index ;
			
			if ( index < 0 || index >= length )
				throw new IllegalArgumentException(
						"Index " + index + " is invalid into array of length " + length
						+ "params " + index_index + ", " + length_index ) ;
		}
		
		return flatIndex ;
	}
	
	/**
	 * A companion method to flattenIndices; returns the length of the flattened
	 * array.
	 * 
	 * The parameter should be the list of nD array lengths; in other words,
	 * 1/2 the length of the arguments to be used with flattenIndices.
	 * 
	 * @param lengths
	 * @return
	 */
	public static final int flattenLength( int ... lengths ) {
		int flatLength = 1 ;
		for ( int i = 0; i < lengths.length; i++ ) {
			int length = lengths[i] ;
			if ( length <= 0 )
				throw new IllegalArgumentException("Cannot flatten an array of length <= 0: " + length) ;
			
			flatLength *= length ;
		}
		
		return flatLength ;
	}
	
	
	/**
	 * A null-safe "clone" method; will not throw an exception if
	 * 'null' is provided, unlike ar.clone().
	 * 
	 * Equivalent to 
	 * 
	 * ar == null ? null : ar.clone() ;
	 * 
	 * @param ar
	 * @return
	 */
	public static final String [] clone( String [] ar ) {
		return ar == null ? null : ar.clone() ;
	}
	
	/**
	 * A null-safe "clone" method; will not throw an exception if
	 * 'null' is provided, unlike ar.clone().
	 * 
	 * Equivalent to 
	 * 
	 * ar == null ? null : ar.clone() ;
	 * 
	 * @param ar
	 * @return
	 */
	public static final boolean [] clone( boolean [] ar ) {
		return ar == null ? null : ar.clone() ;
	}
	
	
	
}
