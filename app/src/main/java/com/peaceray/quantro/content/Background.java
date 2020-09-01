package com.peaceray.quantro.content;

import android.graphics.Color;

public class Background {

	public enum Template {
		/**
		 * A slanted display of nested retro pieces.
		 * Low contrast and slightly blurred.
		 */
		PIECES,
		
		/**
		 * A 'spinning' display of game blacks.  White
		 * and Black are high-contrast; Light and Dark
		 * are low-contrast by comparison, but still more than
		 * PIECES.
		 */
		SPIN,
		
		/**
		 * Like a fancy sweater.  Or a sock.
		 */
		ARGYLE,
		
		/**
		 * Diamond-shaped scales layered over each other.
		 */
		RHOMBI,
		
		/**
		 * Fabric pattern; grid-aligned.
		 */
		TARTAN,
		
		/**
		 * Fabric pattern; tilted off the X/Y grid.
		 */
		TILTED_TARTAN,
		
		/**
		 * No image; displays a solid color.
		 */
		NONE
	}
	
	
	public enum Shade {
		
		/**
		 * The darkest available; typically will include many
		 * pixels of 00 00 00.
		 */
		BLACK,
		
		
		/**
		 * A dark-grey color; lighter than BLACK.
		 * This is the preferred background color for menus;
		 * the bright-white, dropshadowed text shows up best
		 * against a dark grey background.
		 */
		DARK,
		
		/**
		 * A light-grey color; darker than WHITE.
		 * This is the preferred background color for games.
		 */
		LIGHT,
		
		/**
		 * The brightest available; typically will include
		 * many pixels of FF FF FF.
		 */
		WHITE,
	}
	
	private Template mTemplate ;
	private Shade mShade ;
	
	private String mName ;
	
	private String mImageName ;
	private int mColor ;
	
	private Background( Template t, Shade s ) {
		mTemplate = t ;
		mShade = s ;
		
		mName = Background.getName( mTemplate ) + " (" + Background.getName(mShade) + ")" ;
		
		mImageName = Background.hasImage(t, s) ? Background.getImageName(t, s) : null ;
		mColor = Background.hasColor(t, s) ? Background.getColor(s) : Color.MAGENTA ;
	}
	
	@Override
	public boolean equals( Object o ) {
		if ( o == this )
			return true ;
		if ( o instanceof Background ) {
			Background bg = (Background)o ;
			if ( mTemplate != bg.mTemplate )
				return false ;
			if ( mShade != bg.mShade )
				return false ;
			// color, image name, etc. are
			// DETERMINISTIC based on template and shade.
			
			return true ;
		}
		
		return false ;
	}
	
	@Override
	public String toString() {
		return Background.toStringEncoding(this) ;
	}
	
	public String getName() {
		return mName ;
	}
	
	public Template getTemplate() {
		return mTemplate ;
	}
	
	public Shade getShade() {
		return mShade ;
	}
	
	public String getImageName() {
		return mImageName ;
	}
	
	public int getColor() {
		return mColor ;
	}
	
	public boolean hasImage() {
		return mImageName != null ;
	}
	
	public boolean hasColor() {
		return mImageName == null ;
	}
	
	
	
	public static final boolean isBackground( Template t, Shade s ) {
		// as of right now, all combinations are valid.
		if ( t == null || s == null )
			return false ;
		
		return true ;
	}
	
	
	private static final Background [][] CACHED_BACKGROUNDS
			= new Background[Template.values().length][Shade.values().length] ;
	
	public static Background get( Template t, Shade s ) {
		if ( t == null || s == null )
			return null ;
		
		Background b = CACHED_BACKGROUNDS[t.ordinal()][s.ordinal()] ;
		if ( b == null ) {
			b = new Background(t, s) ;
			CACHED_BACKGROUNDS[t.ordinal()][s.ordinal()] = b ;
		}
		
		return b ;
	}
	
	
	public static final boolean equals( Background bg1, Background bg2 ) {
		if ( (bg1 == null) != (bg2 == null) )
			return false ;
		if ( bg1 != null )
			return bg1.equals(bg2) ;
		
		// otherwise, both null.
		return true ;
	}
	
	public static Background [] getBackgrounds() {
		// count, then return.
		int count = 0 ;
		
		Template [] templates = Template.values() ;
		Shade [] shades = Shade.values() ;
		
		for ( int t = 0; t < templates.length; t++ ) {
			Template template = templates[t] ;
			for ( int s = 0; s < shades.length; s++ ) {
				Shade shade = shades[s] ;
				
				if ( Background.isBackground(template, shade) )
					count++ ;
			}
		}
		
		Background [] bgs = new Background[count] ;
		count = 0 ;
		for ( int t = 0; t < templates.length; t++ ) {
			Template template = templates[t] ;
			for ( int s = 0; s < shades.length; s++ ) {
				Shade shade = shades[s] ;
				
				if ( Background.isBackground(template, shade) )
					bgs[count++] = Background.get(template, shade) ;
			}
		}
		
		return bgs ;
	}
	
	public static Background [] getBackgroundsWithImage() {
		// count, then return.
		int count = 0 ;
		
		Template [] templates = Template.values() ;
		Shade [] shades = Shade.values() ;
		
		for ( int t = 0; t < templates.length; t++ ) {
			Template template = templates[t] ;
			for ( int s = 0; s < shades.length; s++ ) {
				Shade shade = shades[s] ;
				
				if ( Background.isBackground(template, shade)
						&& Background.hasImage(template, shade) )
					count++ ;
			}
		}
		
		Background [] bgs = new Background[count] ;
		count = 0 ;
		for ( int t = 0; t < templates.length; t++ ) {
			Template template = templates[t] ;
			for ( int s = 0; s < shades.length; s++ ) {
				Shade shade = shades[s] ;
				
				if ( Background.isBackground(template, shade)
						&& Background.hasImage(template, shade) )
					bgs[count++] = Background.get(template, shade) ;
			}
		}
		
		return bgs ;
	}
	
	
	public static final String getName( Template t ) {
		switch( t ) {
		case PIECES:
			return "Pieces" ;
		case SPIN:
			return "Spin" ;
		case ARGYLE:
			return "Argyle" ;
		case RHOMBI:
			return "Rhombi" ;
		case TARTAN:
			return "Tartan" ;
		case TILTED_TARTAN:
			return "Tilted Tartan" ;
		case NONE:
			return "None" ;
		}
		
		throw new IllegalArgumentException("Don't have a name for Template "+ t) ;
	}
	
	public static final String getName( Shade s ) {
		switch( s ) {
		case BLACK:
			return "black" ;
		case DARK:
			return "dark" ;
		case LIGHT:
			return "light" ;
		case WHITE:
			return "white" ;
		}
		
		throw new IllegalArgumentException("Don't have a name for Template "+ s) ;
	}
	
	
	
	/**
	 * Returns the asset name for the specified template / color
	 * combination, if available.
	 * 
	 * @param t
	 * @param c
	 * @return
	 */
	public static final String getImageName( Template t, Shade s ) {
		switch ( t ) {
    	case PIECES:
    		switch( s ) {
    		case BLACK:
    			return "bg_pieces_black" ;
    		case DARK:
    			return "bg_pieces_dark" ;
    		case LIGHT:
    			return "bg_pieces_light" ;
    		case WHITE:
    			return "bg_pieces_white" ;
    		}
    		return null ;
    	case SPIN:
    		switch( s ) {
    		case BLACK:
    			return "bg_spin_black" ;
    		case DARK:
    			return "bg_spin_dark" ;
    		case LIGHT:
    			return "bg_spin_light" ;
    		case WHITE:
    			return "bg_spin_white" ;
    		}
    		return null ;
    	case ARGYLE:
    		switch( s ) {
    		case BLACK:
    			return "bg_argyle_black" ;
    		case DARK:
    			return "bg_argyle_dark" ;
    		case LIGHT:
    			return "bg_argyle_light" ;
    		case WHITE:
    			return "bg_argyle_white" ;
    		}
    		return null ;
    	
    	case RHOMBI:
    		switch( s ) {
    		case BLACK:
    			return "bg_rhombi_black" ;
    		case DARK:
    			return "bg_rhombi_dark" ;
    		case LIGHT:
    			return "bg_rhombi_light" ;
    		case WHITE:
    			return "bg_rhombi_white" ;
    		}
    		return null ;
    	
    	case TARTAN:
    		switch( s ) {
    		case BLACK:
    			return "bg_tartan_black" ;
    		case DARK:
    			return "bg_tartan_dark" ;
    		case LIGHT:
    			return "bg_tartan_light" ;
    		case WHITE:
    			return "bg_tartan_white" ;
    		}
    		return null ;
    	
    	case TILTED_TARTAN:
    		switch( s ) {
    		case BLACK:
    			return "bg_tilted_tartan_black" ;
    		case DARK:
    			return "bg_tilted_tartan_dark" ;
    		case LIGHT:
    			return "bg_tilted_tartan_light" ;
    		case WHITE:
    			return "bg_tilted_tartan_white" ;
    		}
    		return null ;
    	
    		
    	}
    	
    	return null ;
	}
	
	/**
	 * Converts color code into a drawable color.
	 * @param c
	 * @return
	 */
	public static final int getColor( Shade s ) {
		switch( s ) {
		case WHITE:
			return 0xffffffff ;
		case LIGHT:
			return 0xffaaaaaa ;
		case DARK:
			return 0xff555555 ;
		case BLACK:
			return 0xff000000 ;
		}
		
		// magenta; very apparent.
		return 0xffff00ff ;
	}
	
	
	/**
	 * Will 'getName()' return non-null?  Will return 'false'
	 * for any invalid template / color combination, or
	 * for templates which represent solid colors.
	 * 
	 * @param t
	 * @param c
	 * @return
	 */
	public static final boolean hasImage( Template t, Shade s ) {
		switch( t ) {
		case PIECES:
		case SPIN:
		case ARGYLE:
		case RHOMBI:
		case TARTAN:
		case TILTED_TARTAN:
			return true ;
		}
		
		return false ;
	}
	
	
	/**
	 * Will 'getColor()' return a valid color?  Will return 'false'
	 * for any invalid template / color combination.
	 * 
	 * @param t
	 * @param c
	 * @return
	 */
	public static final boolean hasColor( Template t, Shade s ) {
		return Background.isBackground(t, s) ;
	}
	
	
	private static final String ENCODED_PREFIX = "BG" ;
	private static final String ENCODED_OPEN = "(" ;
	private static final String ENCODED_CLOSE = ")" ;
	private static final String ENCODED_SEPARATOR = "," ;
	
	private static final String [] ENCODED_TEMPLATE = new String[Template.values().length] ;
	private static final String [] ENCODED_SHADE = new String[Shade.values().length] ;
	
	static {
		ENCODED_TEMPLATE[Template.PIECES.ordinal()] 		= "p" ;
		ENCODED_TEMPLATE[Template.SPIN.ordinal()] 			= "s" ;
		ENCODED_TEMPLATE[Template.ARGYLE.ordinal()] 		= "a" ;
		ENCODED_TEMPLATE[Template.RHOMBI.ordinal()] 		= "r" ;
		ENCODED_TEMPLATE[Template.TARTAN.ordinal()] 		= "t" ;
		ENCODED_TEMPLATE[Template.TILTED_TARTAN.ordinal()] 	= "T" ;
		ENCODED_TEMPLATE[Template.NONE.ordinal()]			= "n" ;
		
		ENCODED_SHADE[Shade.BLACK.ordinal()]	= "b" ;
		ENCODED_SHADE[Shade.DARK.ordinal()]		= "d" ;
		ENCODED_SHADE[Shade.LIGHT.ordinal()]	= "l" ;
		ENCODED_SHADE[Shade.WHITE.ordinal()]	= "w" ;
		
		
	}
	
	/**
	 * Returns a "string encoded" version of this Background, designed to be
	 * as lightweight as possible.
	 * 
	 * One possible use: storing, in SharedPreferences, a "StringSet" for the
	 * currently shuffled backgrounds.
	 * 
	 * Encoding: uses the 26 alphabetical characters, in upper- and lower-case,
	 * parentheses "(" and ")", and the comma ",".
	 * 
	 * @param b
	 * @return
	 */
	public static final String toStringEncoding( Background b ) {
		return toStringEncoding( b.getTemplate(), b.getShade() ) ;
	}
	
	/**
	 * Returns a "string encoded" version of this Background, designed to be
	 * as lightweight as possible.
	 * 
	 * One possible use: storing, in SharedPreferences, a "StringSet" for the
	 * currently shuffled backgrounds.
	 * 
	 * Encoding: uses the 26 alphabetical characters, in upper- and lower-case,
	 * parentheses "(" and ")", and the comma ",".
	 * 
	 * @param t
	 * @param c
	 * @return
	 */
	public static final String toStringEncoding( Template t, Shade s ) {
		String tStr = ENCODED_TEMPLATE[t.ordinal()] ;
		String sStr = ENCODED_SHADE[s.ordinal()] ;
		
		if ( tStr == null || sStr == null )
			throw new IllegalArgumentException("Don't have an encoding for Template " + t + " or Shade " + s) ;
		
		StringBuilder sb = new StringBuilder() ;
		sb.append(ENCODED_PREFIX) ;
		sb.append(ENCODED_OPEN) ;
		sb.append(tStr) ;
		sb.append(ENCODED_SEPARATOR) ;
		sb.append(sStr) ;
		sb.append(ENCODED_CLOSE) ;
		
		return sb.toString() ;
	}
	
	/**
	 * Returns a Background instance, representing the Background object
	 * encoding in the provided string.
	 * 
	 * The provided string must be an encoding string as returned
	 * by toStringEncoding( ).  For convenience, leading and trailing
	 * whitespace is allowed, at least that supported by 'trim()'.
	 * NO OTHER STRING CONTENT IS ALLOWED.
	 * 
	 * @param str
	 * @return
	 */
	public static final Background fromStringEncoding( String str ) {
		str = str.trim() ;
		
		int index_bg = str.indexOf(ENCODED_PREFIX) ;
		int index_op = str.indexOf(ENCODED_OPEN) ;
		int index_cm = str.indexOf(ENCODED_SEPARATOR) ;
		int index_cp = str.indexOf(ENCODED_CLOSE) ;
		
		// verify
		if ( index_bg == -1 || index_op == -1 || index_cm == -1 || index_cp == -1 )
			throw new IllegalArgumentException("Encoding " + str + " does not match expected format.") ;
		if ( index_bg > index_op || index_op > index_cm || index_cm > index_cp )
			throw new IllegalArgumentException("Encoding " + str + " does not match expected format.") ;
		
		String tStr = str.substring(index_op+1, index_cm) ;
		String sStr = str.substring(index_cm+1, index_cp) ;
		
		// get the template and shade
		int t = -1, s = -1 ;
		for ( int i = 0; i < ENCODED_TEMPLATE.length; i++ ) {
			if ( tStr.equals( ENCODED_TEMPLATE[i] ) )
				t = i ;
		}
		for ( int i = 0; i < ENCODED_SHADE.length; i++ ) {
			if ( sStr.equals( ENCODED_SHADE[i] ) )
				s = i ;
		}
		
		return Background.get(
				Template.values()[t], Shade.values()[s]) ;
	}
	
}
