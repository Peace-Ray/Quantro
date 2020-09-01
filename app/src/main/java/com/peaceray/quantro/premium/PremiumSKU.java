package com.peaceray.quantro.premium;

import java.util.ArrayList;
import java.util.List;

import com.peaceray.quantro.R;

import android.content.Context;
import android.content.res.Resources;

public class PremiumSKU {
	
	/**
	 * The "Flood" pack.  Includes single-player "Flood" game modes,
	 * and their multiplayer (VS) equivalents.
	 */
	public static final String GAME_MODE_001 = "game_mode_001" ;

	
	/**
	 * The "VS mode special game" pack.  Includes Bitter Pill and
	 * Gravity.
	 */
	public static final String GAME_MODE_002 = "game_mode_002" ;
	
	
	/**
	 * Extra color pack for Retro / Quantro skins.
	 */
	public static final String SKIN_001 = "skin_001" ;
	
	
	/**
	 * Extra backgrounds.
	 */
	public static final String BACKGROUND_001 = "background_001" ;
	
	
	public static final String [] ALL = new String[] {
		GAME_MODE_001,
		GAME_MODE_002,
		SKIN_001,
		BACKGROUND_001
	} ;
	
	
	public static final List<String> all() {
		ArrayList<String> result = new ArrayList<String>() ;
		for ( int i = 0; i < ALL.length; i++ )
			result.add(ALL[i]) ;
		return result ;
	}
	
	public static final boolean isSKU( String SKU ) {
		for ( int i = 0; i < ALL.length; i++ ) {
			if ( ALL[i].equals(SKU) ) {
				return true ;
			}
		}
		return false ;
	}
	
	
	public static final int getNameResID( String SKU ) {
		if ( GAME_MODE_001.equals(SKU) )
			return R.string.premium_content_name_game_mode_001 ;
		if ( GAME_MODE_002.equals(SKU) )
			return R.string.premium_content_name_game_mode_002 ;
		if ( SKIN_001.equals(SKU) )
			return R.string.premium_content_name_skin_001 ;
		if ( BACKGROUND_001.equals(SKU) )
			return R.string.premium_content_name_background_001 ;
		
		// not found
		throw new IllegalArgumentException("No such SKU as " + SKU) ;
	}
	
	public static final String getName( Context context, String SKU ) {
		return getName( context.getResources(), SKU ) ;
	}
	
	public static final String getName( Resources res, String SKU ) {
		return res.getString( getNameResID(SKU) ) ;
	}
	
	
	public static final int getDescriptionResID( String SKU ) {
		if ( GAME_MODE_001.equals(SKU) )
			return R.string.premium_content_description_game_mode_001 ;
		if ( GAME_MODE_002.equals(SKU) )
			return R.string.premium_content_description_game_mode_002 ;
		if ( SKIN_001.equals(SKU) )
			return R.string.premium_content_description_skin_001 ;
		if ( BACKGROUND_001.equals(SKU) )
			return R.string.premium_content_description_background_001 ;
		
		// not found
		throw new IllegalArgumentException("No such SKU as " + SKU) ;
	}
	
	public static final String getDescription( Context context, String SKU ) {
		return getDescription( context.getResources(), SKU ) ;
	}
	
	public static final String getDescription( Resources res, String SKU ) {
		return res.getString( getDescriptionResID(SKU) ) ;
	}
	
	
	public static final int getDescriptionShortResID( String SKU ) {
		if ( GAME_MODE_001.equals(SKU) )
			return R.string.premium_content_description_short_game_mode_001 ;
		if ( GAME_MODE_002.equals(SKU) )
			return R.string.premium_content_description_short_game_mode_002 ;
		if ( SKIN_001.equals(SKU) )
			return R.string.premium_content_description_short_skin_001 ;
		if ( BACKGROUND_001.equals(SKU) )
			return R.string.premium_content_description_short_background_001 ;
		
		// not found
		throw new IllegalArgumentException("No such SKU as " + SKU) ;
	}
	
	public static final String getDescriptionShort( Context context, String SKU ) {
		return getDescriptionShort( context.getResources(), SKU ) ;
	}
	
	public static final String getDescriptionShort( Resources res, String SKU ) {
		return res.getString( getDescriptionShortResID(SKU) ) ;
	}
	
	
	
	public static final int getColorResID( String SKU ) {
		if ( GAME_MODE_001.equals(SKU) )
			return R.color.premium_content_color_game_mode_001 ;
		if ( GAME_MODE_002.equals(SKU) )
			return R.color.premium_content_color_game_mode_002 ;
		if ( SKIN_001.equals(SKU) )
			return R.color.premium_content_color_skin_001 ;
		if ( BACKGROUND_001.equals(SKU) )
			return R.color.premium_content_color_background_001 ;
		
		// not found
		throw new IllegalArgumentException("No such SKU as " + SKU) ;
	}
	
	public static final int getColor( Context context, String SKU ) {
		return getColor( context.getResources(), SKU ) ;
	}
	
	public static final int getColor( Resources res, String SKU ) {
		return res.getColor( getColorResID(SKU) ) ;
	}
	
}
