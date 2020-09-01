package com.peaceray.quantro.view.generic;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * This class was initially copy-pasted from a blog post by 
 * Pete Houston at http://xjaphx.wordpress.com/2011/06/22/image-processing-convolution-matrix/
 * 
 * Not seeing any license information, I assume it is public-domain.
 * 
 * @author Pete Houston
 * @author Jake Rosin (1/4/2012)
 *
 */
public class ConvolutionMatrix
{
	// Ways to handle boundaries during convolution.
	// NONE: only convolve those areas of the Bitmap which our matrix
	//				can fully fit within.
	// SNAP: edge pixels will be repeated into empty space.
	// NORMALIZE: we attempt to normalize the output.
	//				behavior is only specified when all matrix values are >= 0 and the center value is > 0.
	// FILL: provide a color and we will around the image with it.
	//				a fully-transparent color may be provided for faded edges.
	//				By default, black is used.
	public static final int BOUNDARY_NONE = 0 ;
	public static final int BOUNDARY_SNAP = 1 ;
	public static final int BOUNDARY_NORMALIZE = 2 ;
	public static final int BOUNDARY_FILL = 3 ;
	
	public static final int ALPHA_KEEP = 4 ;
	public static final int ALPHA_CONVOLVE = 5 ;
    
	private double [][] mMatrix ;
	private double [] mMatrixHoriz ;
	private double [] mMatrixVert ;
	private boolean mSeparable ;	// if true, use Horiz and Vert, not the matrix itself.
	
	// horiz/vert offset.  Index into the center of the Convolution.
	private int mOffsetHoriz ;
	private int mOffsetVert ;
	
	
	/**
	 * Constructs and returns a ConvolutionMatrix for applying a Gaussian
	 * blur of the specified radius.  "Radius" is equivalent to "standard definition,"
	 * in pixels.
	 * 
	 * The convolution will be truncated and normalized at >=3*radius.
	 * 
	 * @param radius
	 */
	public static ConvolutionMatrix makeGaussianBlur( double radius, int maxDist ) {
		
		// First construct, in decreasing order of intensity,
		// the gaussian values at each pixel.
		int dist = (int)Math.ceil( 3 * radius ) ;
		double [] vals = makeNormalizedGaussianValuesByDistance( radius, dist+1 ) ;
		
		System.err.println("gaussian radius " + radius + " blur vals ") ;
		for ( int i = 0; i < vals.length; i++ )
			System.err.println(vals[i]) ;
		System.err.println() ;
		
		// Make a new ConvolutionMatrix and set its values appropriately.
		int width = Math.min(maxDist*2 -1, vals.length *2 -1 ) ;
		int lim = (width +1) /2 ;
		ConvolutionMatrix matrix = new ConvolutionMatrix(width) ;
		// horiz and vert offset are set.  Fill values in the matrices.
		for ( int i = 0; i < matrix.mOffsetHoriz; i++ ) {
			matrix.mMatrixHoriz[i] = vals[lim - i - 1] ;
			matrix.mMatrixVert[i] = vals[lim - i - 1] ;
		}
		for ( int i = 0; i < lim; i++ ) {
			matrix.mMatrixHoriz[i+matrix.mOffsetHoriz] = vals[i] ;
			matrix.mMatrixVert[i+matrix.mOffsetVert] = vals[i] ;
		}
		matrix.mSeparable = true ;
		return matrix ;
	}
	
	
	/**
	 * Constructs and returns a ConvolutionMatrix for applying a Gaussian
	 * blur of the specified radius.  "Radius" is equivalent to "standard definition,"
	 * in pixels.
	 * 
	 * The convolution will be truncated and normalized at >=3*radius.
	 * 
	 * @param radius
	 */
	public static ConvolutionMatrix makeGaussianBlur( double xRadius, double yRadius ) {
		// First construct, in decreasing order of intensity,
		// the gaussian values at each pixel.
		int xdist = (int)Math.ceil( 3 * xRadius ) ;
		double [] xvals = makeNormalizedGaussianValuesByDistance( xRadius, xdist+1 ) ;
		
		int ydist = (int)Math.ceil( 3 * yRadius ) ;
		double [] yvals = makeNormalizedGaussianValuesByDistance( yRadius, ydist+1 ) ;
		
		
		
		// Make a new ConvolutionMatrix and set its values appropriately.
		int width = xvals.length *2 -1 ;
		int height = yvals.length *2 -1 ;
		ConvolutionMatrix matrix = new ConvolutionMatrix(width, height) ;
		// horiz and vert offset are set.  Fill values in the matrices.
		for ( int i = 0; i < matrix.mOffsetHoriz; i++ )
			matrix.mMatrixHoriz[i] = xvals[xvals.length - i - 1] ;
		for ( int i = 0; i < xvals.length; i++ )
			matrix.mMatrixHoriz[i+matrix.mOffsetHoriz] = xvals[i] ;
		
		for ( int i = 0; i < matrix.mOffsetVert; i++ )
			matrix.mMatrixVert[i] = yvals[yvals.length - i - 1] ;
		for ( int i = 0; i < yvals.length; i++ )
			matrix.mMatrixVert[i+matrix.mOffsetVert] = yvals[i] ;
		
		matrix.mSeparable = true ;
		return matrix ;
	}
	
	
	/**
	 * Constructs and returns an array representing the normalized
	 * Gaussian values at the specified distances from 0.  The result
	 * is truncated at 'max'
	 * @return
	 */
	private static double [] makeNormalizedGaussianValuesByDistance( double radius, int max ) {
		double [] vals = new double[max+1] ;
		double normalizingTerm = 1.0 / (Math.sqrt( 2 * Math.PI * radius * radius ) ) ;
		double expMult = - 1.0 / (2 * radius * radius) ;
		for ( int i = 0; i < max+1; i++ )
			vals[i] = normalizingTerm * Math.exp( i*i*expMult ) ;
		
		// normalize these...
		double sum = 0 ;
		for ( int i = 0; i < max+1; i++ )
			sum += vals[i] ;
		for ( int i = 0; i < max+1; i++ )
			vals[i] /= sum ;
		
		return vals ;
	}
	
	
	

    public ConvolutionMatrix(int size) {
        mMatrix = new double[size][size];
        mMatrixHoriz = new double[size] ;
        mMatrixVert = new double[size] ;
        
        // for now, at least, this is separable.
        mSeparable = true ;
        
        int center = (int)Math.floor(size / 2) ;
        mOffsetHoriz = mOffsetVert = center ;
        
        mMatrix[center][center] = 1 ;
        mMatrixHoriz[center] = 1 ;
        mMatrixVert[center] = 1 ;
        
    }
    
    
    public ConvolutionMatrix( int width, int height ) {
    	mMatrix = new double[width][height];
    	
        mMatrixHoriz = new double[width] ;
        mMatrixVert = new double[height] ;
        
        // for now, at least, this is separable.
        mSeparable = true ;
        
        mOffsetHoriz = (int)Math.floor(width/2) ;
        mOffsetVert = (int)Math.floor(height/2) ;
        
        mMatrix[mOffsetHoriz][mOffsetVert] = 1 ;
        mMatrixHoriz[mOffsetHoriz] = 1 ;
        mMatrixVert[mOffsetVert] = 1 ;
    }
    
    
    public int getWidth() {
    	return mMatrixHoriz.length ; 
    }
    
    public int getHeight() {
    	return mMatrixVert.length ;
    }
    

    public void setAll(double value) {
        for (int x = 0; x < mMatrixHoriz.length; ++x) {
            for (int y = 0; y < mMatrixVert.length; ++y) {
                mMatrix[x][y] = value;
            }
        }
        
        mSeparable = false ;
    }
    
    public void setAll(double horizValue, double vertValue ) {
    	for ( int x = 0; x < mMatrixHoriz.length; x++ )
    		mMatrixHoriz[x] = horizValue ;
    	for ( int y = 0; y < mMatrixVert.length; y++ )
    		mMatrixVert[y] = vertValue ;
    	
        mSeparable = true ;
    }
    
    
    public Bitmap convolve( Bitmap src, int boundaryStyle, int alphaStyle ) {
    	return convolve( src, boundaryStyle, alphaStyle, 0xff000000 ) ;
    }
    
    public Bitmap convolve( Bitmap src, int boundaryStyle, int alphaStyle, int fillColor ) {
    	if ( mSeparable )
    		return convolveSeparable( src, boundaryStyle, alphaStyle, fillColor ) ;
    	
    	return convolveMatrix( src, boundaryStyle, alphaStyle, fillColor,
        		mMatrix, mOffsetHoriz, mOffsetVert ) ;
    }
    
    
    private Bitmap convolveSeparable( Bitmap src, int boundaryStyle, int alphaStyle, int fillColor ) {
    	// convolves in two separate passes.  First, performs a vertical convolution
    	// into a new bitmap.  Then, performs a horizontal convolution into a 1D buffer
    	// and writes the output to the bitmap.
    	
    	System.err.println("convolveMatrix") ;
    	Bitmap bitmap = convolveMatrix( src, boundaryStyle, alphaStyle, fillColor,
    			new double[][]{ mMatrixVert }, 0, mOffsetVert ) ;
    	System.err.println("convolving rows") ;
    	// now convolve horizontally.
    	int width = bitmap.getWidth();
    	int height = bitmap.getHeight();
    	
    	int matrixWidth = mMatrixHoriz.length ;
    	double matrixSum = 0 ;
    	for ( int i = 0; i < matrixWidth; i++ )
    		matrixSum += mMatrixHoriz[i] ;
    	
    	int [] pixels = new int[matrixWidth] ;
    	
    	int A, R, G, B;
    	int sumA, sumR, sumG, sumB;
    	
    	// For each row (y) we convolve across, putting values here.
    	// Finally we copy the entire row back into the source image.
    	int [] row = new int[width] ;
    	for ( int center_y = 0; center_y < height; center_y++ ) {
    		
    		for ( int center_x = 0; center_x < width; center_x++ ) {
    			// if boundary NONE, make sure we are within bounds.
        		if ( boundaryStyle == BOUNDARY_NONE &&
        				( center_x - mOffsetHoriz < 0 || (center_x + (matrixWidth-mOffsetHoriz)) >= height) )
        			continue ;
    			
        		double outsideSourceSum = 0 ;
        		
        		// get the pixels...
        		for ( int i = 0; i < matrixWidth; i++ ) {
        			int bitmap_x = center_x + i - mOffsetHoriz ;
        			if ( bitmap_x < 0 || width <= bitmap_x ) {
    					outsideSourceSum += mMatrixHoriz[i] ;
    					pixels[i] = applyBoundaryStyle( bitmap, bitmap_x, center_y, boundaryStyle, fillColor ) ;
    				}
    				else
    					pixels[i] = src.getPixel(bitmap_x, center_y) ;
        		}
        		
        		
    			
    			// 'pixels' now represents the pixel values to use in convolution,
    			// of the exact same dimensions as the matrix.  We have handled
        		// boundary NONE, SNAP, and FILL - but not NORMALIZE.  Note that
        		// NORMALIZE boundary style will have placed zeros in pixels,
        		// so they can safely be included in the sum so long as we
        		// normalize correctly.  No matter
        		// what, though, we want to get sums across all values (except
        		// maybe Alpha, depending on alpha behavior).
        		// init color sum
    			sumR = sumG = sumB = sumA = 0;

    			// get sum of RGB on matrix
    			for(int i = 0; i < matrixWidth; ++i) {
    				sumR += (Color.red(pixels[i]) * mMatrixHoriz[i]);
					sumG += (Color.green(pixels[i]) * mMatrixHoriz[i]);
					sumB += (Color.blue(pixels[i]) * mMatrixHoriz[i]);
					sumA += (Color.alpha(pixels[i]) * mMatrixHoriz[i]);
    			}
    			
    			// Get the normalizing factor.  Will be 1 unless values were omitted
    			// from the matrix and we are in boundary style NORMALIZE.
    			double factor = 1 ;
    			if ( outsideSourceSum != 0 && boundaryStyle == BOUNDARY_NORMALIZE ) {
    				// attempt normalization.  according to our comments above,
    				// this assumes that the matrix center is > 0 and all values
    				// are >= 0.
    				factor = matrixSum - outsideSourceSum ;
    			}

    			// get final Red
    			R = (int)(sumR / factor);
    			if(R < 0) { R = 0; }
    			else if(R > 255) { R = 255; }

    			// get final Green
    			G = (int)(sumG / factor);
    			if(G < 0) { G = 0; }
    			else if(G > 255) { G = 255; }

    			// get final Blue
    			B = (int)(sumB / factor);
    			if(B < 0) { B = 0; }
    			else if(B > 255) { B = 255; }
    			
    			if ( alphaStyle == ALPHA_KEEP )
    				A = Color.alpha( src.getPixel(center_x, center_y) ) ;
    			else {
    				A = (int)(sumA / factor);
        			if(A < 0) { A = 0; }
        			else if(A > 255) { A = 255; }
    			}
    			
    			// apply new pixel
    			row[center_x] = Color.argb(A, R, G, B) ;
    		}
    		
    		// apply the row to the bitmap image.
    		bitmap.setPixels(row, 0, width, 0, center_y, width, 1) ;
    	}
    	
    	return bitmap ;
    }
    
    
    private static Bitmap convolveMatrix( Bitmap src, int boundaryStyle, int alphaStyle, int fillColor,
    		double [][] matrix, int offsetHoriz, int offsetVert ) {
    	
    	// Otherwise, do a proper convolution.
    	int width = src.getWidth();
    	int height = src.getHeight();
    	
    	int matrixWidth = matrix.length ;
    	int matrixHeight = matrix[0].length ;
    	
    	double matrixSum = 0 ;
    	for ( int i = 0; i < matrixWidth; i++ ) {
    		for ( int j = 0; j < matrixHeight; j++ ) {
    			matrixSum += matrix[i][j] ;
    		}
    	}
    	
    	Bitmap result = Bitmap.createBitmap(width, height, src.getConfig());

    	int A, R, G, B;
    	int sumA, sumR, sumG, sumB;
    	int[][] pixels = new int[matrixWidth][matrixHeight];
    	
    	for ( int center_y = 0; center_y < height; center_y++ ) {
    		// if boundary NONE, make sure we are within bounds.
    		if ( boundaryStyle == BOUNDARY_NONE &&
    				( center_y - offsetVert < 0 || (center_y + (matrixHeight-offsetVert)) >= height) )
    			continue ;
    		

    		
    		for ( int center_x = 0; center_x < width; center_x++ ) {
    			// if boundary NONE, make sure we are within bounds.
        		if ( boundaryStyle == BOUNDARY_NONE &&
        				( center_x - offsetHoriz < 0 || (center_x + (matrixWidth-offsetHoriz)) >= height) )
        			continue ;
    			
        		double outsideSourceSum = 0 ;
        		
    			// get the pixel matrix...
        		for ( int i = 0; i < matrixWidth; i++ ) {
        			int bitmap_x = center_x + i - offsetHoriz ;
        			for ( int j = 0; j < matrixHeight; j++ ) {
        				int bitmap_y = center_y + j - offsetVert ;
        				if ( bitmap_x < 0 || width <= bitmap_x || bitmap_y < 0 || height <= bitmap_y ) {
        					outsideSourceSum += matrix[i][j] ;
        					pixels[i][j] = applyBoundaryStyle( src, bitmap_x, bitmap_y, boundaryStyle, fillColor ) ;
        				}
        				else
        					pixels[i][j] = src.getPixel(bitmap_x, bitmap_y) ;
        			}
        		}
    			
    			// 'pixels' now represents the pixel values to use in convolution,
    			// of the exact same dimensions as the matrix.  We have handled
        		// boundary NONE, SNAP, and FILL - but not NORMALIZE.  Note that
        		// NORMALIZE boundary style will have placed zeros in pixels,
        		// so they can safely be included in the sum so long as we
        		// normalize correctly.  No matter
        		// what, though, we want to get sums across all values (except
        		// maybe Alpha, depending on alpha behavior).
        		// init color sum
    			sumR = sumG = sumB = sumA = 0;

    			// get sum of RGB on matrix
    			for(int i = 0; i < matrixWidth; ++i) {
    				for(int j = 0; j < matrixHeight; ++j) {
    					sumR += (Color.red(pixels[i][j]) * matrix[i][j]);
    					sumG += (Color.green(pixels[i][j]) * matrix[i][j]);
    					sumB += (Color.blue(pixels[i][j]) * matrix[i][j]);
    					sumA += (Color.alpha(pixels[i][j]) * matrix[i][j]);
    				}
    			}
    			
    			// Get the normalizing factor.  Will be 1 unless values were omitted
    			// from the matrix and we are in boundary style NORMALIZE.
    			double factor = 1 ;
    			if ( outsideSourceSum != 0 && boundaryStyle == BOUNDARY_NORMALIZE ) {
    				// attempt normalization.  according to our comments above,
    				// this assumes that the matrix center is > 0 and all values
    				// are >= 0.
    				factor = matrixSum - outsideSourceSum ;
    			}

    			// get final Red
    			R = (int)(sumR / factor);
    			if(R < 0) { R = 0; }
    			else if(R > 255) { R = 255; }

    			// get final Green
    			G = (int)(sumG / factor);
    			if(G < 0) { G = 0; }
    			else if(G > 255) { G = 255; }

    			// get final Blue
    			B = (int)(sumB / factor);
    			if(B < 0) { B = 0; }
    			else if(B > 255) { B = 255; }
    			
    			if ( alphaStyle == ALPHA_KEEP )
    				A = Color.alpha( src.getPixel(center_x, center_y) ) ;
    			else {
    				A = (int)(sumA / factor);
        			if(A < 0) { A = 0; }
        			else if(A > 255) { A = 255; }
    			}
    			
    			// apply new pixel
    			result.setPixel(center_x, center_y, Color.argb(A, R, G, B));
    		}
    	}
    	
    	return result ;
    }
    
    
    /**
     * Returns a color representing the given boundary style applied to get
     * a color for the specified pixel.
     * @param src
     * @param x
     * @param y
     * @param boundaryStyle
     * @param fillColor
     * @return
     */
    private static int applyBoundaryStyle( Bitmap src, int x, int y, int boundaryStyle, int fillColor ) {
    	if ( x >= 0 && x < src.getWidth() && y >= 0 && y < src.getHeight() )
    		return src.getPixel(x,y) ;
    	
    	switch( boundaryStyle ) {
    	case BOUNDARY_NONE:
    	case BOUNDARY_NORMALIZE:
    		return 0x00000000 ;
    	case BOUNDARY_FILL:
    		return fillColor ;
    	case BOUNDARY_SNAP:
    		// snap x, y to within image boundaries.
    		if ( x < 0 ) x = 0 ;
    		else if ( x >= src.getWidth() ) x = src.getWidth()-1 ;
    		if ( y < 0 ) y = 0 ;
    		else if ( y >= src.getHeight() ) y = src.getHeight()-1 ;
    		return src.getPixel(x,y) ;
    	}
    	
    	return 0 ;
    }
}
