package com.peaceray.quantro.view.game;

import com.peaceray.quantro.view.colors.ColorScheme;

import android.graphics.Canvas;
import android.graphics.Rect;

public abstract class ScoreDrawer {
	
	private final static String TAG = "ScoreDrawer" ;
	
	private static final char [] DIGITS_AS_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'} ;
	
	public abstract float titleSize() ;
	
	public abstract int height() ;
	
	public abstract Rect size() ;
	
	public abstract void update( long currentTime ) ;
	
	public abstract boolean needsToDraw() ;
	
	public abstract void drawToCanvas(Canvas canvas, long currentTime) ;
	
	public abstract void setColorScheme( ColorScheme colorScheme ) ;
	
	protected static int digits( float val ) {
		val = Math.abs(val) ;
		return digits( (int)Math.ceil(val) ) ;
	}
	
	protected static int digits( long val ) {
		val = Math.abs(val) ;
		int digits = 1 ;
		while ( val / 10 > 0 ) {
			digits++ ;
			val /= 10 ;
		}
		return digits ;
	}
	
	protected static long longMask( int digits ) {
		long val = 1 ;
		for ( int i = 0; i < digits; i++ )
			val *= 10 ;
		return val ;
	}
	
	protected static int intToCharArray( int val, char [] ar, int index, int max, String lt, String gt ) {
		return longToCharArray( val, ar, index, max, lt, gt ) ;
	}
	
	protected static int longToCharArray( long val, char [] ar, int firstIndex, int max, String lt, String gt ) {
		if ( firstIndex >= max )
			return firstIndex ;
		
		int index = firstIndex ;
		if ( val == 0 ) {
			ar[index] = '0' ;
			return index+1 ;
		}
		
		int digits = digits(val) ;
		
		//Log.d(TAG, "val:" + val + " digits:" + digits) ;
		
		// Determine in advance the maximum length we will store, and round
		// the figure to that many digits.  Call "rem" digits % 3, and "sets"
		// digits / 3.  It takes rem + 4*sets characters to represent the full
		// integer.  Truncating to trunc < sets takes rem + 4*trunc + 1 characters.
		
		int digits_rem = (digits-1) % 3 + 1 ;
		int digits_sets = (digits-1) / 3 ;
		
		int digits_trunc_sets = digits_sets ;
		if ( index + digits_rem + 4*digits_trunc_sets > max ) {
			while( digits_trunc_sets > 0 && index + digits_rem + 4 * digits_trunc_sets + 1 > max )
				digits_trunc_sets-- ;
		}
		// trunc_sets is now the number of sets to display.  If == digit_sets,
		// there is no truncation.
		boolean isLT = false, isGT = false ;
		long origVal = val ;
		if ( digits_trunc_sets != digits_sets ) {
			int remove_sets = digits_sets - digits_trunc_sets ;
			long mask = longMask( remove_sets*3 ) ;
			long rem = val % mask ;
			long newVal = (val / mask)*mask + (rem > (mask/2) ? mask : 0) ;
			isLT = newVal > val ;
			isGT = newVal < val ;
			val = newVal ;
		}
		
		if ( origVal > 0 && val == 0 || digits_rem + index > max + 1 ) {
			// special case!
			ar[index] = '0' ;
			int digitsTaken = max > index ? 1 : 0 ;
			if ( origVal < 1000 ) {
				ar[index] = DIGITS_AS_CHARS[(int)(origVal / 100)] ;
				ar[index+digitsTaken] = 'c' ;
			}
			else if ( origVal < 1000000 )
				ar[index+digitsTaken] = 'M' ;
			else if ( origVal < 1000000000 )
				ar[index+digitsTaken] = 'G' ;
			else if ( origVal < 1000000000000L )
				ar[index+digitsTaken] = 'T' ;
			else if ( origVal < 1000000000000000L )
				ar[index+digitsTaken] = 'P' ;
			else if ( origVal < 1000000000000000000L )
				ar[index+digitsTaken] = 'E' ;
				
			if ( max > index+digitsTaken + 1 ) {
				if ( origVal < 1000 ) {
					ar[index+digitsTaken+1] = 'c' ;
					ar[index+digitsTaken] = ar[index] ;
					ar[index] = '>' ;
					return index+digitsTaken+2 ;
				} else {
					ar[index+digitsTaken+1] = ar[index+digitsTaken] ;
					ar[index+digitsTaken] = '1' ;
					ar[index] = '<' ;
					return index+digitsTaken+2 ;
				}
			}
			
			return index + digitsTaken + 1 ;
		}
		
		// sometimes abbereviation can increase the number of digits.  Recalc.
		digits = digits(val) ;
		
		int commas = 0 ;
		for ( int i = 0; i < digits; i++ ) {
			// get this digit?
			long magnitude = longMask( digits - i - 1 ) ;
			int digit = (int)(val / magnitude) ;
			val %= magnitude ;
			//Log.d(TAG, "i:" + i + " digits:" + digits + " magnitude:" + magnitude + " digit:" + digit + " val:" + val ) ;
			ar[i+commas+index] = DIGITS_AS_CHARS[ digit ] ;
			
			// Are we moving across a commaed-area?  One way to tell is
			// if the number of digits remaining (digits-1-i) is evenly
			// divisible by 3.
			int digitsLeft = digits - 1 - i ;
			if ( i < digits-1 && digitsLeft % 3 == 0 ) {
				// We intend to continue, and this marks the transition
				// to a new commaed area.  Determine if we have
				// the space for more text.  The space needed for
				// more text is the minimum of 5 (e.g. ,123k) and
				// the number of digits remaining times 4/3
				int moreChars = Math.min(5, 4*(digitsLeft)/3) ;
				
				if ( i + commas + index + moreChars >= max ) {
					// abbreviate here!
					switch( digitsLeft ) {
					case 3:
						ar[i+commas+index+1] = 'k' ;
						break ;
					case 6:
						ar[i+commas+index+1] = 'M' ;
						break ;
					case 9:
						ar[i+commas+index+1] = 'G' ;
						break ;
					case 12:
						ar[i+commas+index+1] = 'T' ;
						break ;
					case 15:
						ar[i+commas+index+1] = 'P' ;
						break ;
					case 18:
						ar[i+commas+index+1] = 'E' ;
						break ;
					case 21:
						ar[i+commas+index+1] = 'Z' ;
						break ;
					}
					
					// attempt to fit a > or <.
					String prefix = null ;
					if ( isLT )
						prefix = lt ;
					if ( isGT ) 
						prefix = gt ;
					if ( prefix != null && i + commas + index + 2 + prefix.length() <= max ) {
						// put 'prefix' first.
						for ( int j = i + commas + index + 2 + prefix.length() -1; j >= firstIndex; j-- ) {
							if ( j >= firstIndex + prefix.length() )
								ar[j] = ar[j - prefix.length()] ;
							else
								ar[j] = prefix.charAt(j - firstIndex) ;
						}
						i += prefix.length() ;
					}
					return i+commas+index+2 ;
				} else {
					// add a comma.
					ar[i+commas+index+1] = ',' ;
					commas++ ;
				}
			}
		}
		
		
		return digits + commas + index ;
	}
	
	protected static int floatToCharArray( float val, char [] ar, int index, int max, int additionalDigits ) {
		if ( val == 1 )
			return intToCharArray( 1, ar, index, max, null, null ) ;
		if ( val == 0 )
			return intToCharArray( 0, ar, index, max, null, null ) ;
		
		// Do this using the given number of  significant digits past the decimal
		for ( int i = 0; i < additionalDigits; i++ ) {
			val *= 10 ;
		}
		int valInt = (int)Math.round(val) ;
		
		if ( valInt < 0 ) {
			ar[index] = '-' ;
			index++ ;
			valInt = -valInt ;
		}
		
		if ( val == 0 )
			return intToCharArray( 0, ar, index, max, null, null ) ;
		
		// truncate value to have only that many additional digits
		
		int digits = (int)Math.floor( Math.log10((double)valInt) ) + 1 ;
		for ( int i = digits; i >= 0; i-- ) {
			if ( i == digits - additionalDigits ) {
				ar[i+index] = '.' ;
			}
			else {
				// We are moving from least-significant to most-significant.
				ar[i+index] = DIGITS_AS_CHARS[ valInt % 10 ] ;
				valInt /= 10 ;
			}
		}
		
		return digits+1+index ;
	}
	
	
	/**
	 * Formats the provided time (in milliseconds) into the provided char array.
	 * Returns number of 'in use' elements (i.e. the first available index,
	 * or 'total length' of the string).  This is firstIndex + numNewChars.
	 * 
	 * The time format is basically:
	 * 
	 * M:SS.m
	 * 
	 * Minutes, seconds, milliseconds.
	 * 
	 * If minutes >= 60, formatted as
	 * H:MM:SS.m
	 * 
	 * If minutes < 1, formatted as
	 * 
	 * SS.m
	 * 
	 * We simply truncate to the nearest "section" if we run out of space.
	 * However, we allow milliseconds to truncate within the space.
	 * 
	 * @param val
	 * @param ar
	 * @param firstIndex
	 * @param max
	 * @return
	 */
	protected static int timeToCharArray( long milliseconds, char [] ar, int firstIndex, int max ) {
		if ( firstIndex >= max )
			return firstIndex ;
		
		int hours = (int)( milliseconds / (1000 * 60 * 60) ) ;
		int minutes = (int)( ( milliseconds / (1000 * 60) ) % 60 ) ;
		int seconds = (int)( ( milliseconds / (1000) ) % 60 ) ;
		int millis = (int) ( milliseconds % 1000 ) ;
		
		int hoursLen = hours <= 0 ? 0 : digits(hours) + 1 ;		// if present, "H:"
		int minutesLen = minutes <= 0 && hours <= 0 ? 0 		// if hours, "MM:".  Otherwise, "M:"/"MM:"
				: ( hours > 0 ? 3 : digits(minutes) + 1 ) ;			
		int secondsLen = ( seconds <= 0 && minutes <= 0 && hours <= 0 ) ? 2		// if hours/minutes, SS.  Otherwise, S.
				: ( hours > 0 || minutes > 0 ? 3 : digits(seconds) + 1 ) ;	
		
		int spaceLeft = max - firstIndex ;
		
		if ( hoursLen > spaceLeft ) {
			return longToCharArray( milliseconds, ar, firstIndex, max, null, null ) ;
		}
		
		// convert hours to "H:"
		if ( hoursLen > 0 ) {
			int digits = hoursLen - 1 ;
			for ( int d = digits-1; d >= 0; d-- ) {
				ar[firstIndex+d] = DIGITS_AS_CHARS[hours % 10] ;
				hours /= 10 ;
			}
			ar[firstIndex+digits] = ':' ;
			firstIndex += hoursLen ;
			spaceLeft = max - firstIndex ;
		}
		
		// convert minutes  to "M...:"
		if ( hoursLen > 0 || minutesLen > 0 ) {
			int digits =  minutesLen - 1 ;
			for ( int d = digits-1; d >= 0; d-- ) {
				ar[firstIndex+d] = DIGITS_AS_CHARS[minutes % 10] ;
				minutes /= 10 ;
			}
			ar[firstIndex+digits] = ':' ;
			firstIndex += minutesLen ;
			spaceLeft = max - firstIndex ;
		}
		
		// convert seconds to "S...."
		if ( hoursLen > 0 || minutesLen > 0 || secondsLen > 0 ) {
			int digits =  secondsLen - 1 ;
			for ( int d = digits-1; d >= 0; d-- ) {
				ar[firstIndex+d] = DIGITS_AS_CHARS[seconds % 10] ;
				seconds /= 10 ;
			}
			ar[firstIndex+digits] = '.' ;
			firstIndex += secondsLen ;
			spaceLeft = max - firstIndex ;
		}
		
		// convert milliseconds to "mmm"
		if ( firstIndex < max )
			ar[firstIndex] = DIGITS_AS_CHARS[(millis / 100)] ;
		
		return Math.min(max, firstIndex + 1) ;
	}
	
}
