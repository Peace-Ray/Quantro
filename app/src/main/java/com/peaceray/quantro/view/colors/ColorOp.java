package com.peaceray.quantro.view.colors;

import android.graphics.Color;

public class ColorOp {
	
	
	public static String toString( int color ) {
		StringBuilder sb = new StringBuilder();
		sb.append("argb(");
		sb.append(Color.alpha(color)).append(",");
		sb.append(Color.red(color)).append(",");
		sb.append(Color.green(color)).append(",");
		sb.append(Color.blue(color)).append(")");
		return sb.toString();
	}
	
	public static int setAlphaForColor( int alpha, int color ) {
		return Color.argb( alpha,
				Color.red(color),
				Color.green(color),
				Color.blue(color)) ;
	}

	public static int composeAOverB( int colorA, int colorB ) {
		int alphaB = Color.alpha(colorB) ;
		if ( alphaB < 255 ) {
			float aA = Color.alpha(colorA) / 255.0f ;
			float aB = Color.alpha(colorB) / 255.0f ;
			float a0 = (aA + aB * (1 - aA)) ;
			int r = Math.round( (Color.red(  colorA) * aA + Color.red(  colorB) * aB * (1 - aA)) / a0 ) ;
			int g = Math.round( (Color.green(colorA) * aA + Color.green(colorB) * aB * (1 - aA)) / a0 ) ;
			int b = Math.round( (Color.blue( colorA) * aA + Color.blue( colorB) * aB * (1 - aA)) / a0 ) ;
			
			int a = Math.round( 255 * a0 ) ;
			
			return Color.argb(a, r, g, b) ;
		} else {
			return composeAOverOpaqueB( colorA, colorB ) ;
		}
	}
	
	/**
	 * Functions as composeAOverB, but makes the explicit assumption that
	 * B is opaque.
	 * 
	 * @param colorA
	 * @param colorB
	 * @return
	 */
	public static int composeAOverOpaqueB( int colorA, int colorB ) {
		float aA = Color.alpha(colorA) / 255.0f ;
		return Color.argb(
				255,
				(int)(Color.red(colorA) * aA + Color.red(colorB ) * (1 - aA) ),
				(int)(Color.green(colorA) * aA + Color.green(colorB ) * (1 - aA) ),
				(int)(Color.blue(colorA) * aA + Color.blue(colorB ) * (1 - aA) ) ) ;
	}
	
	/**
	 * An version of composeAOverB, optimized for when B has value 0xff000000:
	 * fully-opaque black.
	 * 
	 * @param colorA
	 * @return
	 */
	public static int composeAOverBlack( int colorA ) {
		int alpha = Color.alpha(colorA) ;
		return Color.argb(
				255,		// result is fully-opaque
				(int)( Color.red(colorA) * alpha / 255.0f ), 
				(int)( Color.green(colorA) * alpha / 255.0f ),
				(int)( Color.blue(colorA) * alpha / 255.0f )) ;
	}
	
	
	public static int colorAverage( int A, int B ) {
		return Color.argb(
				Math.round(Color.alpha(A)*0.5f + Color.alpha(B)*0.5f),
				Math.round(Color.red(A)*0.5f + Color.red(B)*0.5f),
				Math.round(Color.green(A)*0.5f + Color.green(B)*0.5f),
				Math.round(Color.blue(A)*0.5f + Color.blue(B)*0.5f)) ;
	}
	
	
	/**
	 * Returns a Color object representing a shade of grey, indicated by
	 * 'proportion.'
	 * 
	 * 'shadeColor' functions by moving the color towards white, or towards
	 * black, depending on the proportion given (positive or negative, with
	 * +1 / -1 being full white / black).
	 * 
	 * 'shade' has similar functionality, but different behavior.  For nonnegative
	 * proportion, we move towards white from black, meaning proportion 0 is black
	 * and 1 is white.
	 * 
	 * For negative proportion, we move towards black from white, meaning -1 is
	 * black and approaching 0 from below moves towards white.
	 * 
	 * @param proportion
	 * @return
	 */
	public static int shade( float proportion ) {
		if ( proportion >= 0 )
			return whitenColor( proportion, 0xff000000 ) ;
		else
			return blackenColor( -proportion, 0xffffffff ) ;
	}
	
	/**
	 * If proportion is > 0, equivalent to calling whitenColor( proportion, color ).
	 * 
	 * Else, equivalent to calling blackenColor( -proportion, color ).
	 * 
	 * @param proportion
	 * @param color
	 * @return
	 */
	public static int shadeColor( float proportion, int color ) {
		if ( proportion > 0 )
			return whitenColor( proportion, color ) ;
		else if ( proportion < 0 )
			return blackenColor( -proportion, color ) ;
		return color ;
	}
	
	public static int whitenColor(float proportion, int color) {
		float q = 1 - proportion;
		return Color.argb(Color.alpha(color),
				Math.round(Color.red(color) * q + proportion * 0xff),
				Math.round(Color.green(color) * q + proportion * 0xff),
				Math.round(Color.blue(color) * q + proportion * 0xff));
	}
	
	public static int blackenColor(float proportion, int color) {
		float q = 1 - proportion;
		return Color.argb(Color.alpha(color),
				Math.round(Color.red(color) * q),
				Math.round(Color.green(color) * q),
				Math.round(Color.blue(color) * q));
	}
	
	
	
}
