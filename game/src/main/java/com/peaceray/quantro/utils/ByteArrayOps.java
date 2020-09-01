package com.peaceray.quantro.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Some static methods for working with byte arrays.
 * @author Jake
 *
 */
public class ByteArrayOps {
	
	// Used for reading Floats
	private static final int MASK = 0xff ;
	
	
	// Java is retarded, doesn't allow either unsigned byte representations, or
	// any sort of precise bit-fiddling.  Even worse, the byte value of bits 0xff
	// (for example) is not equivalent to the int value specified by the string "0xff",
	// meaning an all-ones byte sent by a server can be easily verified as such through
	// comparison.
	//
	// Our work-around exploits the fact that java uses twos-complement to store bytes and
	// integers.  In other words, the byte value 0xff is -1, 0xfe is -2.  We can explicitly
	// check for those values using the comparison operator.
	//
	// For performing the 1s-complement we XOR (using '^') with the byte -1,
	// and checking / setting specific byte values.
	public static final byte BYTE_0xFF = -1 ;
	public static final byte BYTE_0xFE = -2 ;
	public static final byte BYTE_0xFD = -3 ;
	public static final byte BYTE_0xFC = -4 ;
	
	
	/**
	 * Writes the provided integer value as four bytes into
	 * the provided byte array, using little-endian notation.
	 * 
	 * @param val
	 * @param ar
	 * @param index
	 */
	public static void writeIntAsBytes( int val, byte [] ar, int index ) {
		for ( int b_off = 3; b_off >= 0; b_off-- ) {
			ar[index+b_off] = (byte)( val >>> (8 * (3-b_off)) ) ;
		}
	}
	
	public static int readIntAsBytes( byte [] ar, int index ) {
		return (ar[index+0] << 24)
	        + ((ar[index+1] & MASK) << 16)
	        + ((ar[index+2] & MASK) << 8)
	        +  (ar[index+3] & MASK);
	}
	
	
	/**
	 * Writes the provided integer value as four bytes into
	 * the provided byte array, using big-endian notation.
	 * 
	 * @param val
	 * @param ar
	 * @param index
	 */
	public static void writeLongAsBytes( long val, byte [] ar, int index ) {
		ByteBuffer bb = ByteBuffer.wrap(ar, index, 8) ;
		bb.putLong(val) ;
	}
	
	public static long readLongAsBytes( byte [] ar, int index ) {
		ByteBuffer bb = ByteBuffer.wrap(ar, index, 8) ;
		return bb.getLong() ;
	}
	
	
	

	/**
	 * convert float to byte array (of size 4)
	 * @param f
	 * @return
	 */
	public static void writeFloatAsBytes(float f, byte [] ar, int index ) {
		int i = Float.floatToRawIntBits(f);
		ByteArrayOps.writeIntAsBytes(i, ar, index) ;
	}
	
	/**
	 * convert byte array (of size 4) to float
	 * @param test
	 * @return
	 */
	public static float readFloatAsBytes(byte [] ar, int index) {
		int i = ByteArrayOps.readIntAsBytes(ar, index) ;
		return Float.intBitsToFloat(i);
	}

	
	/**
	 * convert float to byte array (of size 4)
	 * @param f
	 * @return
	 */
	public static void writeDoubleAsBytes(double d, byte [] ar, int index ) {
		long l = Double.doubleToRawLongBits(d) ;
		ByteArrayOps.writeLongAsBytes(l, ar, index) ;
	}
	
	/**
	 * convert byte array (of size 4) to float
	 * @param test
	 * @return
	 */
	public static double readDoubleAsBytes(byte [] ar, int index) {
		long l = ByteArrayOps.readLongAsBytes(ar, index) ;
		return Double.longBitsToDouble(l) ;
	}
	
	
	public static int writeSocketAddressAsBytes( SocketAddress addr, byte [] ar, int index ) throws IllegalArgumentException {
		// Only supports InetSocketAddresses...
		if ( !(addr instanceof InetSocketAddress) ) 
			throw new IllegalArgumentException("SocketAddress provided is not an instance of a supported class (InetSocketAddress)") ;
		
		// We write the address in a special format.
		
		// For IPv4:
		// [ 0x00 [4 bytes IP] 0 [2 bytes unsigned short in least-sig-first notation] ]
		// 		This is 8 bytes long.
		// For IPv6:
		// [ 0x01 [16 bytes IP] 0 [2 bytes unsigned short in least-sig-first notation] ]
		// 		This is 20 bytes long.
		
		// Then, we apply 1s-complement to this value.
		// This processing matches that of readSocketAddressAsBytes
		// and the behavior of the python-implemented whereami server.
		
		// We need the port number and the direct internet address.
		byte [] ip = ((InetSocketAddress)addr).getAddress().getAddress() ;
		int port = ((InetSocketAddress)addr).getPort() ;
		
		// Encode.
		if ( ip.length != 4 && ip.length != 16 )
			throw new IllegalArgumentException("Cannot encode an InetAddress that is not 4 or 16 bytes.") ;
		
		// Where do things go?
		int codeOffset = 0 ;
		int sepOffset = ip.length + 1 ;
		
		// Write code
		ar[index + codeOffset] = (byte)(ip.length == 4 ? 0x00 : 0x01) ;
		ar[index + sepOffset] = 0x00 ;
		
		// Write IP.
		for ( int i = 0; i < ip.length; i++ )
			ar[i+1+index] = ip[i] ;
		
		// Write port.
		int lSigVal = port % 256 ;
		int mSigVal = port / 256 ;
		ar[index + sepOffset + 1] = (byte)(lSigVal < 128 ? lSigVal : (lSigVal - 256)) ;
		ar[index + sepOffset + 2] = (byte)(mSigVal < 128 ? mSigVal : (mSigVal - 256)) ;
		
		// Take the ones-complement.
		for ( int i = 0; i < 8; i++ )
			ar[index+i] = (byte) (BYTE_0xFF ^ ar[index+i]) ;
		
		// 8 or 20 bytes written
		return ip.length == 4 ? 8 : 20 ;
	}
	
	
	public static SocketAddress readSocketAddressAsBytes( byte [] ar, int index ) {
		// Quick check for length and data
		// Extend this to BYTE_0xFE when necessary.
		if ( !((ar.length - index >= 8 && ar[index] == BYTE_0xFF) || (ar.length - index >= 20 && ar[index] == BYTE_0xFE)) )
			return null ;
		
		int length = ar[index] == BYTE_0xFF ? 8 : 20 ;
		
		// Get the bytes and perform ones-complement.
		byte [] b = new byte[length] ;
		for ( int i = 0; i < b.length; i++ )
			b[i] = ar[i+index] ;
		
		// Take the ones-complement
		for ( int i = 0; i < b.length; i++ )
			b[i] = (byte) (BYTE_0xFF ^ b[i]) ;
		
		int sepIndex = length - 3 ;
		
		// extract the ip
		byte [] ip_b = new byte[length - 4] ;
		for ( int i = 0; i < ip_b.length; i++ )
			ip_b[i] = b[i+1] ;
		
		// Convert the port
		int lSigVal = b[sepIndex+1] >= 0 ? b[sepIndex+1] : 256 + b[sepIndex+1] ;
		int mSigVal = b[sepIndex+2] >= 0 ? b[sepIndex+2] : 256 + b[sepIndex+2] ;
		int port = lSigVal + mSigVal * 256 ;
		
		// Make the SocketAddress.
		InetAddress addr ;
		try {
			addr = InetAddress.getByAddress(ip_b) ;
		} catch (UnknownHostException e) {
			e.printStackTrace() ;
			return null ;
		}
		
		return new InetSocketAddress( addr, port ) ;
	}
	
	
	public static boolean isSocketAddressAsBytes( byte [] ar, int index ) {
		if ( !((ar.length - index >= 8 && ar[index] == BYTE_0xFF) || (ar.length - index >= 20 && ar[index] == BYTE_0xFE)) )
			return false ;
		
		// IPv4 packets have a zero in bytes 0 and 5 - but they are ones-complemented.
		// Check for 0xFF in byte 0, indicating IPv4, then examine byte 5.
		if ( ar[index] == BYTE_0xFF && ar[index+5] != BYTE_0xFF )
			return false ;
		
		// IPv6 packets have a one in byte 0 and a zero in byte 17 - but they are ones-complimented.
		if ( ar[index] == BYTE_0xFE && ar[index+17] != BYTE_0xFF ) ;
		
		// Everything checks out.
		return true ;
	}
	
	
	public static int lengthOfSocketAddressAsBytes( SocketAddress addr ) {
		// Only supports InetSocketAddresses...
		if ( !(addr instanceof InetSocketAddress) ) 
			throw new IllegalArgumentException("SocketAddress provided is not an instance of a supported class (InetSocketAddress)") ;
		
		// Examine write/readSocketAddressAsBytes to formatting information.
		
		// We need the port number and the direct internet address, plus 2 code bytes.
		byte [] ip = ((InetSocketAddress)addr).getAddress().getAddress() ;
		
		if ( ip.length == 4 )
			return 8 ;
		if ( ip.length == 16 ) 
			return 20 ;

		throw new IllegalArgumentException("SocketAddress provided does not have the 4-byte address expected for IPv4 or the 16-byte for IPv6, instead is " + addr) ;
	}
	
	
	
	/**
	 * Writes the provided blockfield as bytes to the provided byte array.
	 * 
	 * Only those rows indicated in 'rowIncluded' are written; the rest are silently
	 * skipped.
	 * 
	 * If 'rowIncluded' is null, all rows are written (equivalent to rowIncluded being
	 * set to all 'true').
	 * 
	 * @param field
	 * @param include
	 * @param byteArray
	 * @param index
	 * @return
	 */
	public static int toBytes( byte [][][] field, boolean [] rowIncluded, byte [] byteArray, int index ) {
		int R = field[0].length ;
		int C = field[0][0].length ;
		
		int indexOrig = index ;
		
		for ( int q = 0; q < field.length; q++ ) {
			for ( int r = 0; r < R; r++ ) {
				if ( rowIncluded == null || rowIncluded[r] ) {
					if ( byteArray != null ) {
						for ( int c = 0; c < C; c++ ) {
							byteArray[index++] = field[q][r][c] ;
						}
					} else {
						index += C ;
					}
				}
			}
		}
		
		return index - indexOrig ;
	}
	
	
	/**
	 * Reads into the provided blockfield as bytes from the provided byte array.
	 * 
	 * Only those rows indicated in 'rowIncluded' are read; the rest are silently
	 * skipped.
	 * 
	 * If 'rowIncluded' is null, all rows are written (equivalent to rowIncluded being
	 * set to all 'true').
	 * 
	 * @param field
	 * @param include
	 * @param byteArray
	 * @param index
	 * @return
	 */
	public static int fromBytes( byte [][][] field, boolean [] rowIncluded, byte [] byteArray, int index ) {
		int R = field[0].length ;
		int C = field[0][0].length ;
		
		int indexOrig = index ;
		
		for ( int q = 0; q < field.length; q++ ) {
			for ( int r = 0; r < R; r++ ) {
				if ( rowIncluded == null || rowIncluded[r] ) {
					for ( int c = 0; c < C; c++ ) {
						field[q][r][c] = byteArray[index++] ;
					}
				}
			}
		}
		
		return index - indexOrig ;
	}
	
	
	private static final byte [] BIT = new byte[8] ;
	static {
		for ( int i = 0; i < 8; i++ ) {
			BIT[i] = (byte)(0x1 << i) ;
		}
	}
	
	
	/**
	 * Packs the specified boolean array into bytes (8 bools per byte).
	 * Returns the number of bytes written to 'byteArray.'
	 * 
	 * @param ar The boolean array to convert to bytes.
	 * @param byteArray The byte array to which we write the result.  If 'null',
	 * 		we perform no writes; however, our return value will be correct and 
	 * 		consistent with the case where a byte array is provided.
	 * @param index Resulting bytes are written beginning at this index (inclusive).
	 * @return The total number of bytes written by this call.
	 */
	public static int toBytes( boolean [] ar, byte [] byteArray, int index ) {
		int numBytes = (int)Math.ceil(ar.length / 8.0) ;
		if ( byteArray != null ) {
			for ( int byteCount = 0; byteCount < numBytes; byteCount++ ) {
				byteArray[index + byteCount] = booleanArrayToByte(ar, byteCount) ;
			}
		}
		return numBytes ;
	}
	
	/**
	 * Unpacks from the provide byte array into the boolean array provided
	 * (8 bools per byte).  Returns the number of bytes read from 'byteArray.'
	 * 
	 * @param ar The boolean array to convert from bytes.
	 * @param byteArray The byte array from which we read the packed booleans.
	 * @param index Encoded bytes are read beginning at this index (inclusive).
	 * @return The total number of bytes read by this call.
	 */
	public static int fromBytes( boolean [] ar, byte [] byteArray, int index ) {
		int numBytes = (int)Math.ceil(ar.length / 8.0) ;
		for ( int byteCount = 0; byteCount < numBytes; byteCount++ ) {
			byteToBooleanArray( byteArray[index + byteCount], ar, byteCount ) ;
		}
		return numBytes ;
	}
	
	/**
	 * A byte can store up to 8 booleans.  Returns a 'byte' which
	 * represents the 8 consecutive booleans in the specified array.
	 * 
	 * 'byteCount' is the number of bytes previously generated from
	 * this array.  If 0, we use booleans [0,8).  If 1, we use booleans
	 * [8,16).  Etc.
	 * 
	 * @param ar
	 * @return
	 */
	private static byte booleanArrayToByte( boolean [] ar, int byteCount ) {
		byte out = 0x0 ;
		int index = byteCount * 8 ;
		for ( int i = 0; i < 8; i++ ) {
			if ( index + i < ar.length && ar[index+i] ) {
				out |= BIT[i] ;
			}
		}
		return out ;
	}
	
	
	/**
	 * Reverses 'booleanArrayToByte.'  Sets 'ar' according to the bits in 'b',
	 * with those elements in 'ar' numbering byteCount*8 to byteCount*8 + 7 being
	 * affected.
	 * 
	 * @param b
	 */
	private static void byteToBooleanArray( byte b, boolean [] ar, int byteCount ) {
		int index = byteCount * 8 ;
		for ( int i = 0; i < 8; i++ ) {
			if ( index + i < ar.length ) {
				ar[index+i] = (b & BIT[i]) != 0 ;
			}
		}
	}
	
}
