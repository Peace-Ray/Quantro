package com.peaceray.quantro.model.game;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 * A sequence of GameBlocksSlices.  The Sequence is, in effect,
 * an animation; it holds blockfield states in a form that can
 * be animated, without relying on any game logic such as
 * Systems or a Game object.
 * 
 * Sequences are externally simple.  They may be queried for
 * a slice by, in order of necessary caution,
 * 
 * 	1. allocation of a new slice,
 * 	2. copying into a provided slice instance,
 *  3. providing a Slice reference for use by the caller
 *  
 * Assuming the Sequence itself is well-constructed, the 
 * first approach, allocation of a new slice, will always
 * succeed and the Slice returned may be used for any purpose
 * you wish.  However, it requires allocating a significant 
 * chunk of memory every time it is called, and for that
 * reason is probably inappropriate for use in animations.
 * 
 * The second approach allows for very limited, if any, memory allocation
 * (in particular, there is allocation if String representations are
 * internally used to serialize Slices; OO streams are probably safer).
 * However, there is some risk involved: one must take care that
 * the provided Slice instance matches the dimensions (R, C, num, edge)
 * of the Slices used in animation.  One way of ensuring that is
 * to use method 1, the new slice allocation, for the first slice
 * instance, and from there on copy into that same instance when
 * a new slice is needed.
 * 
 * The third approach may seem simpler on the surface, in that you
 * are not required to store your own slice instances when you
 * wish to retrieve one.  However, the Slice instance returned by
 * the sequence should be treated as both immutable and volatile;
 * the instance itself is retained by the Sequence, and may be used
 * for its own internal processing.  This means two things: changes 
 * made to the Slice may break the Sequence, and the Sequence itself
 * may silently change the Slice while it is still in use.  For safety,
 * this approach should be used only when 1. the caller is the only
 * program area touching the Sequence, and 2. the Slice will only
 * be used up to the retrieval of the next Slice.
 * 
 * Finally, it's worth noting how the Sequence controls timed events.
 * Sequences are meant to be paired with a BlockDrawer; the Drawer
 * (and its DrawSettings) determine the length of particular animations,
 * whereas a Slice is meant to represent an animated event as a single,
 * atomic object.  In some cases, though, such as pieces falling, there
 * is no animation to display but they must still be displayed for
 * a certain period of time.  Therefore, the amount of time a Slice
 * should be displayed is the MAXIMUM of the animation time and the
 * 'display time' of the Slice in the Sequence.
 * 
 * *****************************************************************************
 * 
 * Instances of the GameBlocksSliceSequence are partially immutable; although
 * they retain a record of the current slice and can thus produce the next slice,
 * but slices cannot be added, removed, or re-ordered.
 * 
 * Slice sequences are read from Strings or ObjectInputStreams, or files
 * pointing to the above.  If a file, the Sequence can loop on its own,
 * by closing and re-opening the file to read.  Alternately, if provided
 * with an ObjectInputStream (or String) that represents a sufficiently 
 * short Sequence, the entire sequence can be pre-loaded and stored within
 * this instance (in a space-limiting format).  If this is undesired,
 * don't do it.
 * 
 * For this implementation (2/9/12), all animations are completely loaded in
 * advance, either when constructed or with setSequence().
 * 
 * @author Jake
 *
 */
public class GameBlocksSliceSequence {

	// Is an animation set?
	protected boolean mSet ;
	
	// Here's the animation!
	protected byte [] mBytes ;
	
	// If we've got an animation, then we need a stream
	// to read, and some extra information about where we
	// are and where we're going.
	protected ObjectInputStream mOIS ;
	protected int mCurrentSliceNumber ;
	protected int mTotalSliceNumber ;
	protected long mCurrentSliceDuration ;
	protected long mTotalDuration ;
	protected boolean mLoops ;
	// Some extra information about the sequence.
	protected int mSliceR ;
	protected int mSliceC ;
	protected int mSliceNum ;
	protected int mSliceEdge ;
	
	
	// We store two slice instances: one being the "currently
	// displayed slice," and another which can be provided 
	// to interested parties.  New slices are loaded based on
	// the previous slice as a template.
	protected GameBlocksSlice mSliceCurrent ;
	protected GameBlocksSlice mSliceConvenient ;
	
	

	public GameBlocksSliceSequence() {
		mSet = false ;
		mBytes = null ;
		mOIS = null ;
	}
	
	public GameBlocksSliceSequence( String seq ) {
		this() ;
		if ( !setSequence( seq ) ) 
			throw new IllegalArgumentException("Cannot construct with invalid sequence") ;
	}
	
	public GameBlocksSliceSequence( ObjectInputStream ois ) {
		this() ;
		if ( !setSequence( ois ) ) 
			throw new IllegalArgumentException("Cannot construct with invalid sequence") ;
	}
	
	public GameBlocksSliceSequence( byte [] bytes ) {
		this() ;
		try {
			ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( bytes ) );
			if ( !setSequence( ois ) )
				throw new IllegalArgumentException("Cannot construct with invalid sequence") ;
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot construct with invalid sequence") ;
		}
	}
	
	/**
	 * Returns a copy of the (hopefully) minimum-length byte encoding
	 * from which a new SliceSequence can be constructed.
	 * @return
	 */
	public byte [] getBytes() {
		return mBytes.clone() ;
	}
	
	
	public int rows() {
		return mSliceR ;
	}
	
	public int cols() {
		return mSliceC ;
	}
	
	public int edge() {
		return mSliceEdge ;
	}
	
	public int num() {
		return mSliceNum ;
	}
	
	
	// Information about current sequence.
	public boolean hasNext() {
		//System.err.println("hasNext?") ;
		//System.err.println("mSet: " + mSet) ;
		//System.err.println("mCurrentSliceNumber: " + mCurrentSliceNumber) ;
		//System.err.println("mTotalSliceNumber: " + mTotalSliceNumber) ;
		//System.err.println("mLoops: " + mLoops) ;
		
		
		return
				mSet
				&& ( mCurrentSliceNumber + 1 < mTotalSliceNumber
						|| ( mLoops && mTotalSliceNumber > 0 ) ) ;
	}
	
	public GameBlocksSlice newNext() {
		if ( !hasNext() )
			return null ;
		
		if ( mCurrentSliceNumber + 1 >= mTotalSliceNumber )
			resetSequence() ;
		
		// load the next slice into mSliceConvenient, and copy
		// from there into a new Slice instance.
		try {
			Object prefix = mOIS.readObject() ;
			if ( !(prefix instanceof SlicePrefix) )
				throw new IOException("SlicePrefix not found") ;
			readSlice(
					mSliceConvenient,
					mCurrentSliceNumber < 0 ? null : mSliceCurrent,
					mOIS, null ) ;
		} catch (IOException e) {
			return null ;
		} catch (ClassNotFoundException e) {
			return null ;
		}
		// we used 'current' as the template; exchange these
		// references so 'current' is the current slice.
		GameBlocksSlice temp = mSliceConvenient ;
		mSliceConvenient = mSliceCurrent ;
		mSliceCurrent = temp ;
		
		// make a NEW slice and copy over.
		temp = new GameBlocksSlice( mSliceCurrent ) ;
		return temp ;
	}
	
	public boolean next( GameBlocksSlice gbs ) {
		if ( !hasNext() )
			return false ;
		
		if ( mCurrentSliceNumber + 1 >= mTotalSliceNumber )
			resetSequence() ;
		
		// load the next slice into mSliceConvenient, and copy
		// from there into a new Slice instance.
		try {
			Object prefix = mOIS.readObject() ;
			if ( !(prefix instanceof SlicePrefix) )
				throw new IOException("SlicePrefix not found") ;
			readSlice(
					mSliceConvenient,
					mCurrentSliceNumber < 0 ? null : mSliceCurrent,
					mOIS, null ) ;
		} catch (IOException e) {
			return false ;
		} catch (ClassNotFoundException e) {
			return false ;
		}
		// we used 'current' as the template; exchange these
		// references so 'current' is the current slice.
		GameBlocksSlice temp = mSliceConvenient ;
		mSliceConvenient = mSliceCurrent ;
		mSliceCurrent = temp ;
		
		// copy into the provided structure.
		gbs.takeVals( mSliceCurrent ) ;
		return true ;
	}
	
	public GameBlocksSlice next() {
		if ( !hasNext() )
			return null ;
		
		if ( mCurrentSliceNumber + 1 >= mTotalSliceNumber )
			resetSequence() ;
		
		// load the next slice into mSliceConvenient, and copy
		// from there into a new Slice instance.
		try {
			readSlice(
					mSliceConvenient,
					mCurrentSliceNumber < 0 ? null : mSliceCurrent,
					mOIS, null ) ;
		} catch ( IOException e ) {
			return null ;
		}
		// we used 'current' as the template; copy from
		// 'convenient' into 'current' and return a reference
		// to 'convenient.'
		mSliceCurrent.takeVals( mSliceConvenient ) ;
		return mSliceCurrent ;
	}
	
	public boolean nextIsReady( long timePassed ) {
		if ( !hasNext() )
			return false ;
		return timePassed >= mCurrentSliceDuration ;
	}
	
	public long timeUntilNext() {
		if ( !hasNext() )
			return -1 ;
		return mCurrentSliceDuration ;
	}
	
	public long timeUntilNext( long timePassed ) {
		if ( !hasNext() )
			return -1 ;
		return Math.max( 0, mCurrentSliceDuration - timePassed ) ;
	}
	
	
	
	public boolean setSequence( String seq ) {
		// load this sequence!
		mSet = prereadSequence( null, seq ) ;
		//System.err.println("mSet is " + mSet) ;
		if ( !mSet )
			return false ;
		
		mSet = resetSequence() ;
		//System.err.println("reset returned " + mSet) ;
		// Open up a 
		return mSet ;
	}
	
	
	public boolean setSequence( ObjectInputStream ois ) {
		// load this sequence
		mSet = prereadSequence( ois, null ) ;
		if ( !mSet )
			return false ;
		
		mSet = resetSequence() ;
		// Open up a 
		return mSet ;
	}
	
	
	/**
	 * Prepares a new sequence for reading.  
	 */
	protected void allocateSlices() {
		mSliceCurrent = new GameBlocksSlice( mSliceR, mSliceC, mSliceNum, mSliceEdge ) ;
		mSliceConvenient = new GameBlocksSlice( mSliceR, mSliceC, mSliceNum, mSliceEdge ) ;
	}
	
	
	public boolean resetSequence() {
		//System.err.println("reset sequence with mSet " + mSet) ;
		
		if ( !mSet )
			return false ;

		
		try {
			
			mOIS = new ObjectInputStream( new ByteArrayInputStream( mBytes ) ) ;
			//System.err.println("mOIS is " + mOIS) ;
			
			mCurrentSliceNumber = -1 ;
			mCurrentSliceDuration = 0 ;
			
			// advance mOIS past the metadata to just before the first slice.
			// our metadata is already set according to this sequence, so there's
			// really no harm in reading past it and silently assigning the values.
			readMetadata( mOIS, null ) ;
			//System.err.println("metadata read") ;
			return true ;
			
		} catch( IOException ioe ) {
			//System.err.println("exception boy!") ;
			ioe.printStackTrace() ;
		}
		
		return false ;
	}
	
	
	private static final String NEWLINE = "\n" ;
	
	private static final String PREFIX_LOOPS = "LOOPS" ;
	private static final String PREFIX_R = "R" ;
	private static final String PREFIX_C = "C" ;
	private static final String PREFIX_NUM = "NUM" ;
	private static final String PREFIX_EDGE = "EDGE" ;
	
	
	private static final String PREFIX_DURATION = "DURATION" ;
	
	private static final String SEQUENCE_OPEN = "<GB_SLICE_SEQUENCE>" ;
	private static final String SEQUENCE_CLOSE = "</GB_SLICE_SEQUENCE>" ;
	
	private static final String META_OPEN = "<GB_SS_META>" ;
	private static final String META_CLOSE = "</GB_SS_META>" ;
	
	private static final String SLICES_OPEN = "<GB_SS_SLICES>" ;
	private static final String SLICES_CLOSE = "</GB_SS_SLICES>" ;
	
	private static final String SLICE_OPEN = "<GB_SS_SLICE>" ;
	private static final String SLICE_CLOSE = "</GB_SS_SLICE>" ;
	
	private static final String SLICE_META_OPEN = "<GB_SS_S_META>" ;
	private static final String SLICE_META_CLOSE = "</GB_SS_S_META>" ;
	
	private static final String SLICE_CONTENT_OPEN = "<GB_SS_S_CONTENT>" ;
	private static final String SLICE_CONTENT_CLOSE = "</GB_SS_S_CONTENT>" ;
	
	
	
	
	/**
	 * Loads the sequence provided.  If 'true' is returned, the following
	 * will be set:
	 * 
	 * 		mBytes
	 * 		mTotalSliceNumber
	 * 		mTotalDuration
	 * 		mLoops
	 * 		mSliceR 
	 * 		mSliceC 
	 * 		mSliceNum 
	 *		mSliceEdge
	 *
	 * Additionally,
	 * 
	 * 		mCurrentSlice
	 * 		mConvenientSlice
	 * 
	 * will be allocated according to the values above.
	 *
	 * The remaining values must be set by the caller.
	 * 
	 * For 'mBytes', we represent the entire sequence.  There is
	 * therefore a bit of redundancy at the beginning (it holds 
	 * sequence metadata that is already read).
	 * 
	 * @param ois
	 * @param str
	 */
	public boolean prereadSequence( ObjectInputStream ois, String str ) {
		
		try {
			
			// here's where we put data.
			ByteArrayOutputStream baos = new ByteArrayOutputStream() ;
			ObjectOutputStream oos = new ObjectOutputStream(baos) ;
			
			// first: get substrings, if possible.
			String content = substringWithin( str, SEQUENCE_OPEN, SEQUENCE_CLOSE ) ;
			String metaString = substringWithin( content, META_OPEN, META_CLOSE ) ;
			String slicesString = substringWithin( content, SLICES_OPEN, SLICES_CLOSE ) ;
			
			// Next: read-and-write the metadata.
			readMetadata( ois, metaString ) ;
			writeMetadata( oos, null ) ;
			
			// Okeydokey.  Allocate two slices.
			allocateSlices() ;
			
			// If a string is provided, break up the slices.
			String [] sliceStr = null ;
			if ( slicesString != null ) {
				int num = -1 ;
				int lastIndex = 0 ;
				while ( lastIndex != -1 ) {
					lastIndex = slicesString.indexOf( SLICE_OPEN, lastIndex+1 ) ;
					num++ ;
				}
				
				sliceStr = new String[num] ;
				int startIndex = 0 ;
				for ( int i = 0; i < num; i++ ) {
					sliceStr[i] = substringWithin( slicesString, SLICE_OPEN, SLICE_CLOSE, startIndex ) ;
					startIndex = slicesString.indexOf( SLICE_CLOSE, startIndex+1 ) ;
				}
			}
			
			// We will be counting up the total duration and total number
			// of slices in this loop...
			mTotalDuration = 0 ;
			mTotalSliceNumber = 0 ;
			
			// start reading slices.  The first slice is read without a template;
			// every subsequent slice uses the previous as a template.  Each slice
			// is written to the oos, using the same template setup.
			for ( int index = 0; ( sliceStr != null && index < sliceStr.length) || (ois != null) ; index++ ) {
				// get the slice string
				String sStr = sliceStr == null ? null : sliceStr[index] ;
				// check for a slice prefix
				if ( ois != null ) {
					Object obj = ois.readObject() ;
					if ( obj instanceof SliceSequenceTerminator )
						break ;
					else if ( !(obj instanceof SlicePrefix ) )
						throw new IllegalArgumentException("Provided ObjectInputStream does not properly use SliceSequenceTerminators and SlicePrefixes") ;
				}
				
				// write slice prefix
				oos.writeObject(SlicePrefix.get()) ;
				
				// read-and-write
				if ( index == 0 ) {
					readSlice( mSliceConvenient, null, ois, sStr ) ;
					writeSlice( mSliceConvenient, null, mCurrentSliceDuration, oos, null ) ;
				} else {
					readSlice( mSliceConvenient, mSliceCurrent, ois, sStr ) ;
					writeSlice( mSliceConvenient, mSliceCurrent, mCurrentSliceDuration, oos, null ) ;
				}
				
				// copy to 'current'
				mSliceCurrent.takeVals(mSliceConvenient) ;
				
				mTotalSliceNumber++ ;
				mTotalDuration += mCurrentSliceDuration ;
			}
			
			// write slice sequence terminator
			oos.writeObject(SliceSequenceTerminator.get()) ;
			
			// finished.
			oos.close() ;
			mBytes = baos.toByteArray() ;
			
			return true ;
			
		} catch ( Exception e ) {
			e.printStackTrace() ;
			return false ;
		}
	}
	
	
	/**
	 * Attempts to read metadata from the specified stream or string.
	 * Throws an exception upon failure.  Upon success, the following
	 * values are set:
	 * 
	 * 		mLoops
	 * 		mSliceR 
	 * 		mSliceC 
	 * 		mSliceNum 
	 *		mSliceEdge 
	 * 
	 * and the ObjectInputStream (if provided) has had those values, and
	 * only those values, retrieved from it.
	 * 
	 * @param ois
	 * @param metaStr
	 * @throws IOException 
	 */
	public void readMetadata( ObjectInputStream ois, String metaStr ) throws IOException {
		// break up
		String [] strArray = null ;
		int index = 0 ;
		if ( metaStr != null ) {
			strArray = metaStr.split("\\s+") ;
			while ( strArray[index].length() == 0 )
				index++ ;
		}
		
		// get metadata
		eat( PREFIX_LOOPS, strArray, index++ );	mLoops = readBoolean( ois, strArray, index++ ) ;
		eat( PREFIX_R, strArray, index++ ) ;	mSliceR = readInt( ois, strArray, index++ ) ;
		eat( PREFIX_C, strArray, index++ ) ;	mSliceC = readInt( ois, strArray, index++ ) ;
		eat( PREFIX_NUM, strArray, index++ ) ;	mSliceNum = readInt( ois, strArray, index++ ) ;
		eat( PREFIX_EDGE, strArray, index++ ) ;	mSliceEdge = readInt( ois, strArray, index++ ) ;
	}
	
	
	public void writeMetadata( ObjectOutputStream oos, StringBuilder sb ) throws IOException {
		// meta prefix
		append( sb, META_OPEN ).append( sb, NEWLINE ) ;
		
		// write metadata
		append( sb, PREFIX_LOOPS )	.write(oos, sb, mLoops ) 		.append( sb, NEWLINE ) ;
		append( sb, PREFIX_R )		.write(oos, sb, mSliceR )		.append( sb, NEWLINE ) ;
		append( sb, PREFIX_C )		.write(oos, sb, mSliceC ) 		.append( sb, NEWLINE ) ;
		append( sb, PREFIX_NUM )	.write(oos, sb, mSliceNum ) 	.append( sb, NEWLINE ) ;
		append( sb, PREFIX_EDGE )	.write(oos, sb, mSliceEdge ) 	.append( sb, NEWLINE ) ;
		
		// meta close
		append( sb, META_CLOSE ).append( sb, NEWLINE ) ;
	}
	
	
	/**
	 * Reads the next slice from the specified structures.  Uses 'dst' as the
	 * storage location for the slice (it MUST be allocated), and 'template'
	 * (if provided) is assumed to be the template for the stored version.
	 * 
	 * If provided, 'ois' must be exactly ready to read a slice.  If 'str' is
	 * provided instead, it should represent the entire contents of the
	 * slice-in-sequence, including duration and slice number, but NOT including
	 * the opening and closing tags.  Strip them yourself.
	 * 
	 * Upon return, the provided Slice has been initialized according to
	 * the data read, and both 'currentSliceNumber' and 'currentSliceDuration'
	 * have been set.
	 * 
	 * @param dst
	 * @param template
	 * @param ois
	 * @param str
	 * @throws IOException 
	 */
	public void readSlice( GameBlocksSlice dst, GameBlocksSlice template, ObjectInputStream ois, String str ) throws IOException {
		
		String strMeta = null , strContent = null ;
		if ( str != null ) {
			strMeta = substringWithin( str, SLICE_META_OPEN, SLICE_META_CLOSE ) ;
			strContent = substringWithin( str, SLICE_CONTENT_OPEN, SLICE_CONTENT_CLOSE ) ;
			
		}
		String [] strArray = null ;
		int index = 0 ;
		if ( str != null ) {
			strArray = strMeta.split("\\s+") ;
			while ( strArray[index].length() == 0 )
				index++ ;
		}
		
		// meta data (duration)
		eat( PREFIX_DURATION, strArray, index++ ) ; 	mCurrentSliceDuration = readLong( ois, strArray, index++ ) ;
		
		// slice itself
		if ( ois != null )
			dst.readAsSliceAfter(template, ois) ;
		if ( strArray != null )
			dst.fromStringAsSliceAfter(template, strContent) ;
		
		mCurrentSliceNumber++ ;
	}
	
	
	public void writeSlice( GameBlocksSlice src, GameBlocksSlice template, long duration, ObjectOutputStream oos, StringBuilder sb ) throws IOException {
		// slice prefix wrapper...
		append( sb, SLICE_OPEN ).append( sb, NEWLINE ) ;
		
		// write the meta (duration)
		append( sb, SLICE_META_OPEN ).append( sb, NEWLINE ) ;
		append( sb, PREFIX_DURATION ).write(oos, sb, duration) ;
		append( sb, SLICE_META_CLOSE ).append( sb, NEWLINE ) ;
		
		// write the slice
		append( sb, SLICE_CONTENT_OPEN ).append( sb, NEWLINE ) ;
		if ( oos != null )
			src.writeAsSliceAfter(template, oos) ;
		if ( sb != null )
			sb.append( src.toStringAsSliceAfter(template) ) ;
		append( sb, SLICE_CONTENT_CLOSE ).append( sb, NEWLINE ) ;
		
		// slice postfix
		append( sb, SLICE_CLOSE ).append( sb, NEWLINE ) ;
	}
	
	
	private String substringWithin( String src, String left, String right ) {
		return substringWithin( src, left, right, 0 ) ;
	}
	
	private String substringWithin( String src, String left, String right, int startIndex ) {
		if ( src == null )
			return null ;
		
		int leftIndex = src.indexOf(left, startIndex) ;
		int rightIndex = src.indexOf(right, leftIndex+1) ;
		
		if ( leftIndex == -1 || rightIndex == -1 )
			throw new IllegalArgumentException("Provided string does not have tag(s) " + left + ", " + right) ;
		
		return src.substring( leftIndex + left.length(), rightIndex ) ;
	}
	

	private GameBlocksSliceSequence write( ObjectOutputStream oos, StringBuilder sb, long num ) throws IOException {
		if ( oos != null )
			oos.writeLong(num) ;
		if ( sb != null )
			sb.append(num).append(" ") ;
		return this ;
	}
	
	private GameBlocksSliceSequence write( ObjectOutputStream oos, StringBuilder sb, int num ) throws IOException {
		if ( oos != null )
			oos.writeInt(num) ;
		if ( sb != null )
			sb.append(num).append(" ") ;
		return this ;
	}
	
	private GameBlocksSliceSequence write( ObjectOutputStream oos, StringBuilder sb, boolean b ) throws IOException {
		if ( oos != null )
			oos.writeBoolean(b) ;
		if ( sb != null )
			sb.append(b).append(" ") ;
		return this ;
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
	private final int readInt( ObjectInputStream ois, String [] strArray, int strArrayIndex ) throws IOException {
		
		if ( ois != null )
			return ois.readInt() ;
		
		if ( strArray != null ) {
			return Integer.parseInt( strArray[strArrayIndex] ) ;
		}
		
		throw new NullPointerException("Only null object inputs given") ;
	}
	
	/**
	 * Reads and returns a long value represented in the provided input objects.
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
	private final long readLong( ObjectInputStream ois, String [] strArray, int strArrayIndex ) throws IOException {
		
		if ( ois != null )
			return ois.readLong() ;
		
		if ( strArray != null ) {
			return Long.parseLong( strArray[strArrayIndex] ) ;
		}
		
		throw new NullPointerException("Only null object inputs given") ;
	}
	
	/**
	 * Reads and returns a boolean value represented in the provided input objects.
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
	private final boolean readBoolean( ObjectInputStream ois, String [] strArray, int strArrayIndex ) throws IOException {
		
		if ( ois != null )
			return ois.readBoolean() ;
		
		if ( strArray != null ) {
			return Boolean.parseBoolean(strArray[strArrayIndex]) ;
		}
		
		throw new NullPointerException("Only null object inputs given") ;
	}
	
	
	private void eat( String expected, String [] strArray, int strArrayIndex ) {
		if ( strArray == null )
			return ;
		if ( !expected.equals(strArray[strArrayIndex]) )
			throw new IllegalArgumentException("Excepted string " + expected + " not found; instead " + ( (strArray == null || strArrayIndex < 0 || strArray.length <= strArrayIndex) ? "invalid size" : strArray[strArrayIndex] ) ) ;
	}
	
	
	private GameBlocksSliceSequence append( StringBuilder sb, String s ) {
		if ( sb != null )
			sb.append(s).append(" ") ;
		return this ;
	}
	
	
	private static class SlicePrefix implements Serializable {
		
		private static SlicePrefix mSlicePrefix = new SlicePrefix() ;
		
		private static final SlicePrefix get() {
			return mSlicePrefix ;
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = -8132084570794938504L;
		
	}
	
	private static class SliceSequenceTerminator implements Serializable {
		
		private static SliceSequenceTerminator mSliceTerminator = new SliceSequenceTerminator() ;
		
		private static final SliceSequenceTerminator get() {
			return mSliceTerminator ;
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 1173551745604424796L;
		
	}
	
	
}

