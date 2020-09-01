package com.peaceray.quantro.view.drawable;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.peaceray.quantro.R;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

/**
 * Shades an area using diagonal lines.  Lines have a width, a
 * color, an angle, and a distance between them.
 * @author Jake
 *
 */
public class LineShadingDrawable extends Drawable {
	
	private Rect mLastBounds ;
	private Paint mPaint ;
	private float [] mLinePoints ;
	
	// Line information
	private float mLineWidth ;
	private float mLineSeparation ;
	private float [] mLineVector ;		// as a convention, has a positive X component
	private float [] mOrigin ;
	private float [] mOriginSnapOffsetComponents ;			// where to put (0,0)?
	private float mLineOffset ;			// lines are offset from (0,0) by this amount, in a direction perpendicular to the vector.
										// We favor movement towards the positive direction - (bottom-right).
	private int mLineColor ;
	private ColorFilter mColorFilter ;
	
	private boolean mLinePointsStale ;
	
	
	public LineShadingDrawable() {
		mLastBounds = null ;
		mPaint = null ;
		mLinePoints = null ;
		mLineWidth = 1 ;
		mLineSeparation = 5 ;
		mLineVector = new float[]{(float)Math.sqrt(0.5f), -(float)Math.sqrt(0.5f)} ;
		mOrigin = new float[]{ 0, 0 } ;
		mOriginSnapOffsetComponents = new float[]{ 0, 0 } ;		// POSITIVE components showing the line-spaced difference between (0,0) and the set origin.
		mLineOffset = 0 ;
		mLineColor = 0xffffffff ;
		mColorFilter = null ;
		mLinePointsStale = true ;
	}
	
	@Override
	public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, IOException {
		super.inflate(r, parser, attrs) ;
		
		TypedArray a = r.obtainAttributes(attrs, R.styleable.LineShadingDrawable) ;
		
		float slopeX = 0, slopeY = 0 ;
		boolean slopeXSet = false, slopeYSet = false ;
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			case R.styleable.LineShadingDrawable_lsd_line_width:
				this.setLineWidth(a.getDimension(attr, 1)) ;
				break ;
			case R.styleable.LineShadingDrawable_lsd_line_separation:
				this.setLineSeparation(a.getDimension(attr, 1)) ;
				break ;
			case R.styleable.LineShadingDrawable_lsd_line_vector_x:
			case R.styleable.LineShadingDrawable_lsd_line_vector_y:
				if ( attr == R.styleable.LineShadingDrawable_lsd_line_vector_x ) {
					slopeX = a.getFloat(attr, 0) ;
					slopeXSet = true ;
				} else {
					slopeY = a.getFloat(attr, 0) ;
					slopeYSet = true ;
				}
				if ( slopeXSet && slopeYSet )
					this.setLineVector( new float[]{slopeX, slopeY} ) ;
				break ;
			case R.styleable.LineShadingDrawable_lsd_line_color:
				this.setColor(a.getColor(attr, 0xffffffff)) ;
				break ;
			}
		}
	}
	
	private void makePaint() {
		mPaint = new Paint() ;
		mPaint.setColor(mLineColor) ;
		mPaint.setColorFilter(mColorFilter) ;
		mPaint.setStrokeWidth(mLineWidth) ;
		mPaint.setStyle(Paint.Style.STROKE) ;
		mPaint.setAntiAlias(true) ;
		mPaint.setDither(true) ;
	}
	
	
	private void makeLinePoints() {
		// Lines are placed according to this formula:
		// A line intersects the origin, default (0,0).  mLineVector determines
		// the direction of this line; perpendicular to it is the 
		// separation vector.
		
		// To avoid strange line endpoint draw effects, we set our
		// lines outside the current draw boundaries by mLineWidth + mLineSeparation.
		// If the line vector goes up-right, start from the top left corner
		// of said rect and step down the left side placing lines, then
		// across the bottom.  Otherwise, start at bottom-left, step up, then across.
		mLastBounds = new Rect(getBounds()) ;
		
		
		setOriginSnapOffsetComponents() ;
		
		boolean upRight = mLineVector[1] < 0 ;
		
		float [] point = new float[2] ;
		float [] sep = getSeparationComponents() ;
		float [] offset = new float[2] ;
		for ( int i = 0; i < offset.length; i++ )
			offset[i] = sep[i] * ( mLineOffset / mLineSeparation ) ;
		float step ;
		
		// Get the first point along the outer bounds.
		Rect outerBounds = new Rect(mLastBounds) ;
		float margin = mLineWidth + mLineSeparation + Math.abs(mLineOffset) ;
		outerBounds.left -= margin ;
		outerBounds.top -= margin ;
		outerBounds.right += margin ;
		outerBounds.bottom += margin ;
		
		float outerDiag = (float)
			Math.sqrt(
				Math.pow(outerBounds.right - outerBounds.left, 2)
				+ Math.pow(outerBounds.bottom - outerBounds.top, 2) ) ;
		float outerDiagX = mLineVector[0] * outerDiag ;
		float outerDiagY = mLineVector[1] * outerDiag ;
		
		ArrayList<float []> al_points = new ArrayList<float []>() ;
		
		// First step along the left edge, if the lines are not
		// vertical.
		if ( mLineVector[0] != 0 ) {
			if ( upRight ) {
				// reminder: we step down, then across.  Start at top-left.
				point[0] = outerBounds.left ;
				point[1] = outerBounds.top ;
				snapPointDown(point) ;
			}
			else {
				point[0] = outerBounds.left ;
				point[1] = outerBounds.bottom ;
				snapPointUp(point) ;
			}
			step = upRight ? sep[1] : -sep[1] ;
			
			// apply the vertical offset.
			point[1] += offset[1] ;
			
			while ( outerBounds.top <= point[1] && point[1] <= outerBounds.bottom ) {
				al_points.add( new float[]{point[0], point[1], point[0] + outerDiagX, point[1] + outerDiagY} ) ;
				point[1] += step ;
			}
		}
		
		// First step along the top (or bottom) edge, if the lines are not
		// horizontal.
		if ( mLineVector[1] != 0 ) {
			if ( upRight ) {
				// reminder: we step down, then across.  Start at bottom-left.
				point[0] = outerBounds.left ;
				point[1] = outerBounds.bottom ;
			}
			else {
				point[0] = outerBounds.left ;
				point[1] = outerBounds.top ;
			}
			snapPointRight(point) ;
			step = sep[0] ;
			
			// apply the horizontal offset.
			point[0] += offset[0] ;
			
			// Draw lines.
			
			while ( outerBounds.left <= point[0] && point[0] <= outerBounds.right ) {
				al_points.add( new float[]{point[0], point[1], point[0] + outerDiagX, point[1] + outerDiagY} ) ;
				point[0] += step ;
			}
		}
		
		// points!
		mLinePoints = new float[4 * al_points.size()] ;
		for ( int i = 0; i < al_points.size(); i++ ) {
			float [] f = al_points.get(i) ;
			for ( int j = 0; j < 4; j++ )
				mLinePoints[i*4 + j] = f[j] ;
		}
	}
	
	
	/**
	 * From mOrigin, sets mOriginSnapOffsetComponents, the positive-valued
	 * directional movement from (0,0) to a line in-line with mOrigin.
	 * In other words, applying X as a positive offset puts us in line with
	 * mOrigin; the same is true with applying Y as a positive offset.
	 * 
	 * This number is obviously dependent on how lines are drawn.
	 * One approach is to set mOriginSnapOffsetComponents to (0,0)
	 * and snap mOrigin in-line using our private methods.  This reveals
	 * an appropriate offset to use: mOrigin.X - mOriginSnapped.X is the
	 * value which, when added to a (0,0)-aligned point, places it in-line
	 * with mOrigin.  Perform some modification such that this number
	 * 1. is positive, and 2. is <= separation.X, as it must be.
	 * 
	 */
	private void setOriginSnapOffsetComponents() {
		// This value is used in snap****, so make sure it has no effect.
		mOriginSnapOffsetComponents[0] = 0 ;
		mOriginSnapOffsetComponents[1] = 0 ;
		float xOffset, yOffset ;
		
		float [] separation = getSeparationComponents() ;
		float [] originSnapped = new float[2] ;
		
		originSnapped[0] = mOrigin[0] ;
		originSnapped[1] = mOrigin[1] ;
		snapPointRight( originSnapped ) ;
		xOffset = mOrigin[0] - originSnapped[0] ;
		while ( xOffset < 0 )
			xOffset += separation[0] ;
		while ( xOffset > separation[0] )
			xOffset -= separation[0] ;
		
		originSnapped[0] = mOrigin[0] ;
		originSnapped[1] = mOrigin[1] ;
		snapPointDown( originSnapped ) ;
		yOffset = mOrigin[1] - originSnapped[1] ;
		while ( yOffset < 0 )
			yOffset += separation[1] ;
		while ( yOffset > separation[1] )
			yOffset -= separation[1] ;
		
		mOriginSnapOffsetComponents[0] = xOffset ;
		mOriginSnapOffsetComponents[1] = yOffset ;
	}
	
	
	/**
	 * Given the provided point, snaps it to the nearest line by moving right.
	 * @param point
	 * @return
	 */
	private void snapPointRight( float [] point ) {
		// Horizontal lines have already been snapped.
		if ( mLineVector[1] == 0 )
			return ;
		
		// A line passing through this point intersects the X axis somewhere.
		// Find this point, snap to the nearest true line (to the right),
		// then step back to our previous point.
		boolean moveRightToXAxis = !(mLineVector[1] > 0 == point[1] > 0) ;
		float magnitudeToXAxis = Math.abs( point[1] / mLineVector[1] ) ;
		// moving right means applying the line vector.
		// moving left means applying the negative vector.
		float velocityToXAxis = moveRightToXAxis ? magnitudeToXAxis : -magnitudeToXAxis ;
		float [] axisPoint = new float[]{
				point[0] + velocityToXAxis * mLineVector[0],
				point[1] + velocityToXAxis * mLineVector[1] } ;
		// snap to the right!  Find the x separation.
		float [] separation = getSeparationComponents() ;
		// we are guaranteed to have a positive X value in separation
		// taking the ceil after division will move towards the right.
		float snappedX = (float)Math.ceil(axisPoint[0] / separation[0]) * separation[0] ;
		
		// axisPoint has been moved downward so that it occurs an a line shading
		// that intersects (0,0).  What if we have a different origin?
		// The answer is trivial if origin has only vertical components, but it might
		// not.  One approach: snap the originPoint down.  This reveals the appropriate
		// vertical offset to apply to 'axisPoint' to be in-line with the new offset.
		snappedX += mOriginSnapOffsetComponents[0] ;
		if ( Math.abs( snappedX - axisPoint[0] ) > separation[0] )
			snappedX = snappedX >= 0 ? snappedX - separation[0]
			                         : snappedX + separation[0] ;
		
		// Now move back.
		axisPoint[0] = snappedX ;
		point[0] = axisPoint[0] - velocityToXAxis * mLineVector[0] ;
	}
	
	private void snapPointDown( float [] point ) {
		// Vertical lines have already been snapped.
		if ( mLineVector[0] == 0 )
			return ;
		
		// A line passing through this point intersects the Y axis somewhere.
		// Find this point, snap to the nearest true line (downward),
		// then step back to our previous point.
		boolean moveRightToYAxis = point[0] < 0 ;
		float magnitudeToYAxis = Math.abs( point[0] / mLineVector[0] ) ;
		// moving right means applying the line vector.
		// moving left means applying the negative vector.
		float velocityToYAxis = moveRightToYAxis ? magnitudeToYAxis : -magnitudeToYAxis ;
		float [] axisPoint = new float[]{
				point[0] + velocityToYAxis * mLineVector[0],
				point[1] + velocityToYAxis * mLineVector[1] } ;
		// snap downward!  Find the y separation.
		float [] separation = getSeparationComponents() ;
		// We are guaranteed a positive separation.  Taking the ceiling
		// will move us downward.
		float snappedY = (float)Math.ceil(axisPoint[1] / separation[1]) * separation[1] ;
		
		// axisPoint has been moved downward so that it occurs an a line shading
		// that intersects (0,0).  What if we have a different origin?
		// The answer is trivial if origin has only vertical components, but it might
		// not.  One approach: snap the originPoint down.  This reveals the appropriate
		// vertical offset to apply to 'axisPoint' to be in-line with the new offset.
		snappedY += mOriginSnapOffsetComponents[1] ;
		if ( Math.abs( snappedY - axisPoint[1] ) > separation[1] )
			snappedY = snappedY >= 0 ? snappedY - separation[1]
			                         : snappedY + separation[1] ;
		
		// Now move back.
		axisPoint[1] = snappedY ;
		point[1] = axisPoint[1] - velocityToYAxis * mLineVector[1] ;
		
		// Finally: all of those calculations involved an origin of (0,0).  Offset
		// by up to separationComponents (downward again) to align with mOrigin.
		
	}
	
	private void snapPointUp( float [] point ) {
		// Vertical lines have already been snapped.
		if ( mLineVector[0] == 0 )
			return ;
		
		// A line passing through this point intersects the Y axis somewhere.
		// Find this point, snap to the nearest true line (downward),
		// then step back to our previous point.
		boolean moveRightToYAxis = point[0] < 0 ;
		float magnitudeToYAxis = Math.abs( point[0] / mLineVector[0] ) ;
		// moving right means applying the line vector.
		// moving left means applying the negative vector.
		float velocityToYAxis = moveRightToYAxis ? magnitudeToYAxis : -magnitudeToYAxis ;
		float [] axisPoint = new float[]{
				point[0] + velocityToYAxis * mLineVector[0],
				point[1] + velocityToYAxis * mLineVector[1] } ;
		// snap downward!  Find the y separation.
		float [] separation = getSeparationComponents() ;
		// We are guaranteed a positive separation.  Taking the floor
		// will move us upward.
		float snappedY = (float)Math.floor(axisPoint[1] / separation[1]) * separation[1] ;
		
		// axisPoint has been moved upward so that it occurs an a line shading
		// that intersects (0,0).  What if we have a different origin?
		// The answer is trivial if origin has only vertical components, but it might
		// not.  One approach: snap the originPoint down.  This reveals the appropriate
		// vertical offset to apply to 'axisPoint' to be in-line with the new offset.
		snappedY += mOriginSnapOffsetComponents[1] ;
		snappedY -= separation[1] ;
		if ( Math.abs( snappedY - axisPoint[1] ) > separation[1] )
			snappedY = snappedY >= 0 ? snappedY - separation[1]
			                         : snappedY + separation[1] ;
		
		// Now move back.
		axisPoint[1] = snappedY ;
		point[1] = axisPoint[1] - velocityToYAxis * mLineVector[1] ;
	}
	
	
	
	
	private float [] getSeparationComponents() {
		boolean upRight = mLineVector[1] < 0 ;
		float sepX ;
		float sepY ;
		float [] tempPoint = new float[2] ;
		// get the horizontal and vertical steps between lines.
		// Our line vector has a positive X component.  Step in positive
		// x to the next line from (0,0), then move along that line
		// until we have only X-axis movement or Y-axis movement.
		tempPoint[0] = mLineSeparation * (upRight ? -mLineVector[1] :  mLineVector[1]) ;
		tempPoint[1] = mLineSeparation * (upRight ?  mLineVector[0] : -mLineVector[0]) ;
		// How many line vectors must we travel to reach y == 0?  Apply this many
		// of the x component.  Do the reverse for y.
		
		sepX = mLineVector[1] == 0 ? 0 : Math.abs(tempPoint[0]) + Math.abs((tempPoint[1] / mLineVector[1]) * mLineVector[0]) ;
		sepY = mLineVector[0] == 0 ? 0 : Math.abs(tempPoint[1]) + Math.abs((tempPoint[0] / mLineVector[0]) * mLineVector[1]) ;
		
		
		// This appears to fail, and having come back later to investigate,
		// doesn't make any sense to me.
		/*
		sepX = mLineVector[1] == 0 ? 0 : Math.abs((tempPoint[1] / mLineVector[1]) * mLineVector[0]) ;
		sepY = mLineVector[0] == 0 ? 0 : Math.abs((tempPoint[0] / mLineVector[0]) * mLineVector[1]) ;
		*/
		
		tempPoint[0] = sepX ;
		tempPoint[1] = sepY ;
		
		return tempPoint ;
	}
	

	@Override
	public void draw(Canvas canvas) {
		if ( mPaint == null )
			makePaint() ;
		
		if ( !(getBounds().equals(mLastBounds)) || mLinePointsStale ) {
			makeLinePoints() ;
			mLinePointsStale = false ;
		}
		
		canvas.drawLines(mLinePoints, mPaint) ;
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT ;
	}
	
	public void setColor( int color ) {
		mLineColor = color ;
		if ( mPaint == null )
			makePaint() ;
		else
			mPaint.setColor(color) ;
	}

	@Override
	public void setAlpha(int alpha) {
		mLineColor = Color.argb(
				alpha,
				Color.red(mLineColor),
				Color.green(mLineColor),
				Color.blue(mLineColor)) ;
		
		if ( mPaint == null )
			makePaint() ;
		else
			mPaint.setAlpha(alpha) ;
	}
	
	public void setShadowLayer( float radius, int color ) {
		if ( mPaint == null )
			makePaint() ;
		mPaint.setShadowLayer(radius, 0, 0, color) ;
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		mColorFilter = cf ;
		makePaint() ;
	}

	public void setLineWidth( float width ) {
		mLineWidth = width ;
		if ( mPaint == null )
			makePaint() ;
		mPaint.setStrokeWidth(width) ;
	}
	
	public void setLineSeparation( float sep ) {
		mLineSeparation = sep ;
		mLinePointsStale = true ;
	}
	
	public void setLineVector( float [] v ) {
		// normalize this vector
		float len = (float)Math.sqrt(v[0] * v[0] + v[1] * v[1]) ;
		mLineVector[0] = v[0] / len ;
		mLineVector[1] = v[1] / len ;
		
		// ensure a nonnegative X component.
		if ( mLineVector[0] < 0 ) {
			mLineVector[0] *= -1 ;
			mLineVector[1] *= -1 ;
		}
		
		mLinePointsStale = true ;
	}
	
	public void setLineOffset( float offset ) {
		mLineOffset = offset ;
		mLinePointsStale = true; 
	}
	
	public void setLineSlope( float slope ) {
		if ( slope == Float.POSITIVE_INFINITY || slope == Float.NEGATIVE_INFINITY )
			setLineVector( new float[]{0, 1} ) ;
		else
			setLineVector( new float[]{1, slope} ) ;
	}
	
	public void setOrigin( float [] origin ) {
		setOrigin( origin[0], origin[1] ) ;
	}
	
	public void setOrigin( float X, float Y ) {
		if ( mOrigin[0] != X || mOrigin[1] != Y ) {
			mOrigin[0] = X ;
			mOrigin[1] = Y ;
			mLinePointsStale = true ;
		}
	}
}
