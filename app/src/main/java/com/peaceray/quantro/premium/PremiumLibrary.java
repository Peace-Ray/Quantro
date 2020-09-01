package com.peaceray.quantro.premium;

import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.content.Music;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.keys.KeyRing;
import com.peaceray.quantro.model.modes.GameModes;

/**
 * The PremiumLibrary describes the library of premium
 * features available to users.  For a given Background, Skin,
 * Music, Game Mode, or certain specific features, the
 * PremiumLibrary uses a KeyRing to determine whether it
 * is supported.
 * 
 * A PremiumLibrary instance describes those 
 * 
 * @author Jake
 *
 */
public class PremiumLibrary {
	
	
	/**
	 * Returns the PremiumSKU.* field associated with the provided premium background.
	 * If no SKU is associated with the background, for example if the background is
	 * not premium, returns 'null.'
	 * @param background
	 * @return
	 */
	public static String getPremiumSKU( Background background ) {
		if ( !isPremium( background ) )
			return null ;
		// only one premium background SKU right now...
		return PremiumSKU.BACKGROUND_001 ;
	}
	
	
	/**
	 * Returns the PremiumSKU.* field associated with the provided premium music.
	 * If no SKU is associated with the music, for example if the music is
	 * not premium, returns 'null.'
	 * @param background
	 * @return
	 */
	public static String getPremiumSKU( Music music ) {
		if ( !isPremium( music ) )
			return null ;
		// no premium music SKU...
		return null ;
	}
	
	
	/**
	 * Returns the PremiumSKU.* field associated with the provided premium skin.
	 * If no SKU is associated with the skin, for example if the skin is
	 * not premium, returns 'null.'
	 * @param background
	 * @return
	 */
	public static String getPremiumSKU( Skin skin ) {
		if ( !isPremium( skin ) )
			return null ;
		// only one premium skin SKU right now...
		return PremiumSKU.SKIN_001 ;
	}
	
	
	/**
	 * Returns the PremiumSKU.* field associated with the provided premium game mode.
	 * If no SKU is associated with the game mode, for example if the game mode is
	 * not premium, returns 'null.'
	 * @param background
	 * @return
	 */
	public static String getPremiumGameModeSKU( int gameMode ) {
		if ( !isPremiumGameMode( gameMode ) )
			return null ;
		
		String itemNeeded = null ;
		
		int classCode = GameModes.classCode(gameMode) ;
		
		if ( itemNeeded == null ) {
			// Flood game modes need GAME_MODE_001.
			if ( classCode == GameModes.CLASS_CODE_FLOOD )
				itemNeeded = PremiumSKU.GAME_MODE_001 ;
			else {
				// check game mode to make sure.  Class code is easy to attack.
				switch( gameMode ) {
				case GameModes.GAME_MODE_1V1_QUANTRO_C:
				case GameModes.GAME_MODE_1V1_RETRO_C:
				case GameModes.GAME_MODE_SP_QUANTRO_C:
				case GameModes.GAME_MODE_SP_RETRO_C:
					itemNeeded = PremiumSKU.GAME_MODE_001 ;
					break ;
				}
			}
		}
		
		if ( itemNeeded == null ) {
			// Special game modes need GAME_MODE_002.
			if ( classCode == GameModes.CLASS_CODE_SPECIAL )
				itemNeeded = PremiumSKU.GAME_MODE_002 ;
			else {
				// check game mode to make sure.  Class code is easy to attack.
				switch( gameMode ) {
				case GameModes.GAME_MODE_1V1_QUANTRO_BITTER_PILL:
				case GameModes.GAME_MODE_1V1_RETRO_GRAVITY:
					itemNeeded = PremiumSKU.GAME_MODE_002 ;
					break ;
				}
			}
		}
		
		return itemNeeded ;
	}
	
	
	/**
	 * Returns whether the provided item SKU is available to people who have
	 * a valid Quantro XL key.
	 * 
	 * @param itemSKU
	 * @return
	 */
	public static boolean isUnlockedByQuantroXL( String itemSKU ) {
		// Quantro XL unlocks game modes 001 and 002.
		return PremiumSKU.GAME_MODE_001.equals(itemSKU)
				|| PremiumSKU.GAME_MODE_002.equals(itemSKU) ;
	}
	
	
	
	
	/**
	 * Is the specified content Premium only?  Returns whether
	 * we expect the average user to have to pay something to
	 * get access to this content.
	 * 
	 * @param background
	 * @return
	 */
	public static boolean isPremium( Background background ) {
		// The four premium Templates are Argyle, Rhombi, Tartan, and Tilted Tartan.
		switch( background.getTemplate() ) {
		case ARGYLE:
		case RHOMBI:
		case TARTAN:
		case TILTED_TARTAN:
			return true ;
		}
		
		return false ;
	}
	
	
	/**
	 * Is the specified content Premium only?  Returns whether
	 * we expect the average user to have to pay something to
	 * get access to this content.
	 * 
	 * @param background
	 * @return
	 */
	public static boolean isPremium( Music music ) {
		// STUB: no premiums yet.
		return false ;
	}

	
	/**
	 * Is the specified content Premium only?  Returns whether
	 * we expect the average user to have to pay something to
	 * get access to this content.
	 * 
	 * @param background
	 * @return
	 */
	public static boolean isPremium( Skin skin ) {
		Skin.Game game = skin.getGame() ;
		Skin.Template template = skin.getTemplate() ;
		Skin.Color color = skin.getColor() ;
		
		// Non-premium skins are those with one of the three
		// colorblind colors (protonopia, deuteranopia, tritanopia)
		// as well as standard Retro, Ne0N, standard Quantro, and Nile.
		// For Quantro, Nile, and the Quantro colorblind colors,
		// skin template allows both standard and Colorblind.
		switch( game ) {
		case RETRO:
			// non-premium if color is one of the colorblind colors,
			// is NEON, or Standard Retro.
			switch( template ) {
			case STANDARD:
				// based on color: retro, or one of the colorblinds.
				switch( color ) {
				case RETRO:
				case PROTANOPIA:
				case DEUTERANOPIA:
				case TRITANOPIA:
					return false ;
				}
				break ;
				
			case NEON:
				// only if color is Neon.
				switch( color ) {
				case NEON:
					return false ;
				}
				break ;
			}
			
			break ;
			
			
		case QUANTRO:
			// non-premium if color is one of the colorblind colors,
			// is Nile, or standard Quantro.  Only qualifies if using the
			// Standard or Colorblind template.
			switch( template ) {
			case STANDARD:
			case COLORBLIND:
				switch( color ) {
				case QUANTRO:
				case NILE:
				case PROTANOPIA:
				case DEUTERANOPIA:
				case TRITANOPIA:
					return false ;
				}
				break ;
			}
			break ;
		}
		
		// otherwise, it is premium.
		return true ;
	}

	
	/**
	 * Is the specified content Premium only?  Returns whether
	 * we expect the average user to have to pay something to
	 * get access to this content.
	 * 
	 * @param background
	 * @return
	 */
	public static boolean isPremiumGameMode( int gameMode ) {
		// Premium game modes are those with FLOOD or SPECIAL class codes.
		// Additionally, because these are easy attack vectors (class code is
		// defined in raw XML) we test for exact game mode types.
		int classCode = GameModes.classCode(gameMode) ;
		
		switch( classCode ) {
		case GameModes.CLASS_CODE_ENDURANCE:
		case GameModes.CLASS_CODE_PROGRESSION:
			// verify that it is not a specific, disallowed game mode.
			switch( gameMode ) {
			case GameModes.GAME_MODE_1V1_QUANTRO_BITTER_PILL:
			case GameModes.GAME_MODE_1V1_QUANTRO_C:
			case GameModes.GAME_MODE_1V1_RETRO_C:
			case GameModes.GAME_MODE_1V1_RETRO_GRAVITY:
			case GameModes.GAME_MODE_SP_QUANTRO_C:
			case GameModes.GAME_MODE_SP_RETRO_C:
				return true ;
			}
			break ;

			
		case GameModes.CLASS_CODE_FLOOD:
		case GameModes.CLASS_CODE_SPECIAL:
			return true ;
		}
		
		return false ;
	}
	
	
	

	private KeyRing mKeyRing ;
	
	/**
	 * Constructs a new PremiumLibrary for the provided KeyRing.
	 * 
	 * A local copy of the KeyRing is kept; changes made to the provided
	 * object after this call will not affect this premium library.
	 * 
	 * @param keyRing
	 */
	public PremiumLibrary( KeyRing keyRing ) {
		if ( keyRing == null )
			throw new NullPointerException("Must provide a valid KeyRing.") ;
		mKeyRing = new KeyRing( keyRing ) ;
	}
	
	
	/**
	 * Can the user hide the status bar?
	 * 
	 * @return
	 */
	public boolean hasHideStatusBar() {
		// return mKeyRing.signedAny() ;
		// EDIT FOR 1.0.4: we are adding support for Immersive mode in
		// Android 4.4.  Since this mode is now a standard Android feature,
		// we don't want to lock it behind a paywall... and we don't want
		// < API 19 users to be left out.  Hiding the status bar is now an
		// option available for all users.
		return true ;
	}
	
	
	/**
	 * Should we hide advertisements?
	 * @return
	 */
	public boolean hasHideAds() {
		// EDIT FOR 1.0.5: Quantro is now free.  We never show ads.
		return true; // mKeyRing.signedAny() ;
	}
	

	
	/**
	 * Does the user have access to this Background?  Equivalent
	 * to "!isPremium( bg ) || hasPremium( bg )"
	 * @param bg
	 * @return
	 */
	public boolean has( Background bg ) {
		// EDIT FOR 1.0.5: Quantro is now free.  The user gets all premium content.
		return true;
		
		/*
		if ( !isPremium(bg) ) {
			return true ;
		}
		
		// otherwise, check whether we have this premium content.
		// For now, all premium backgrounds are covered by SKU BACKGROUND_001.
		String sku = getPremiumSKU(bg) ;
		return mKeyRing.signedPremium( sku )
				|| (isUnlockedByQuantroXL(sku) && mKeyRing.signedXL())
				|| mKeyRing.signedPromotion( sku );
				*/
	}
	
	/**
	 * Does the user have access to this Music?  Equivalent
	 * to "!isPremium( music ) || hasPremium( music )"
	 * @param music
	 * @return
	 */
	public boolean has( Music music ) {
		// EDIT FOR 1.0.5: Quantro is now free.  The user gets all premium content.
		return true;
				
		/*
		if ( !isPremium(music) )
			return true ;

		String sku = getPremiumSKU(music) ;
		return mKeyRing.signedPremium( sku )
				|| (isUnlockedByQuantroXL(sku) && mKeyRing.signedXL())
				|| mKeyRing.signedPromotion( sku );
				*/
	}
	
	/**
	 * Does the user have access to this Skin?  Equivalent
	 * to "!isPremium( skin ) || hasPremium( skin )"
	 * @param skin
	 * @return
	 */
	public boolean has( Skin skin ) {
		// EDIT FOR 1.0.5: Quantro is now free.  The user gets all premium content.
		return true;
		
		/*
		if ( !isPremium(skin) ) {
			return true ;
		}
		
		// otherwise, check whether we have this premium content.
		// For now, all premium skins are covered by SKU SKIN_001.
		String sku = getPremiumSKU(skin) ;
		return mKeyRing.signedPremium( sku )
				|| (isUnlockedByQuantroXL(sku) && mKeyRing.signedXL())
				|| mKeyRing.signedPromotion( sku );
				*/
	}
	
	
	/**
	 * Does the user have access to this GameMode?  Equivalent
	 * to "!isPremium( gameMode ) || hasPremium( gameMode )"
	 * @param skin
	 * @return
	 */
	public boolean hasGameMode( int gameMode ) {
		// EDIT FOR 1.0.5: Quantro is now free.  The user gets all premium content.
		return true;
		
		/*
		if ( !isPremiumGameMode(gameMode) )
			return true ;
		
		// otherwise, check whether we have this premium content.
		// For now, all premium skins are covered by SKU SKIN_001.
		String sku = getPremiumGameModeSKU(gameMode) ;
		return mKeyRing.signedPremium( sku )
				|| (isUnlockedByQuantroXL(sku) && mKeyRing.signedXL())
				|| mKeyRing.signedPromotion( sku );
				*/
	}
	
	
	/**
	 * Does the user have ANY premium content?
	 * @return
	 */
	public boolean hasAnyPremium() {
		// EDIT FOR 1.0.5: Quantro is now free.  The user gets all premium content.
		return true;
				
				/*
		for ( int i = 0; i < PremiumSKU.ALL.length; i++ ) {
			String item = PremiumSKU.ALL[i] ;
			if ( mKeyRing.signedPremium(item) )
				return true ;
			if ( mKeyRing.signedPromotion(item) )
				return true ;
		}
		return mKeyRing.signedXL() ;
		*/
	}
	
	
	/**
	 * Does the user have access to this piece of Premium content?
	 * 
	 * Returns 'false' if the provided content is not Premium content,
	 * or the user does not own it.
	 * 
	 * To check access to content without regard to whether it is
	 * Premium, call 'has()'.
	 * 
	 * @param background
	 * @return
	 */
	public boolean hasPremium( Background background ) {
		return isPremium(background) && has(background) ;
	}
	
	
	/**
	 * Does the user have access to this piece of Premium content?
	 * 
	 * Returns 'false' if the provided content is not Premium content,
	 * or the user does not own it.
	 * 
	 * To check access to content without regard to whether it is
	 * Premium, call 'has()'.
	 * 
	 * @param background
	 * @return
	 */
	public boolean hasPremium( Music music ) {
		return isPremium(music) && has(music) ;
	}

	
	/**
	 * Does the user have access to this piece of Premium content?
	 * 
	 * Returns 'false' if the provided content is not Premium content,
	 * or the user does not own it.
	 * 
	 * To check access to content without regard to whether it is
	 * Premium, call 'has()'.
	 * 
	 * @param background
	 * @return
	 */
	public boolean hasPremium( Skin skin ) {
		return isPremium(skin) && has(skin) ;
	}
	
	
	/**
	 * Does the user have access to this piece of Premium content?
	 * 
	 * Returns 'false' if the provided content is not Premium content,
	 * or the user does not own it.
	 * 
	 * To check access to content without regard to whether it is
	 * Premium, call 'has()'.
	 * 
	 * @param background
	 * @return
	 */
	public boolean hasPremiumGameMode( int gameMode ) {
		return isPremiumGameMode(gameMode) && hasGameMode(gameMode) ;
	}

	
	
	
	/**
	 * Certain premium game modes are available as a timed trail even
	 * before purchase.  For example, single player Flood game modes
	 * allow games to be played up to a certain number of seconds before
	 * gameplay is paused and the player is prompted to purchase the game
	 * mode before they can continue.
	 * 
	 * If a player has a game mode, this method will always return false
	 * -- it is "hasTrialONLY."
	 * 
	 * @param gameMode
	 * @return
	 */
	public boolean hasTrialOnlyGameMode( int gameMode ) {
		// EDIT FOR 1.0.5: Quantro is now free.  The user gets all premium content.
		return false;
				
				/*
		boolean isFlood = false ;
		if ( GameModes.classCode(gameMode) == GameModes.CLASS_CODE_FLOOD && GameModes.maxPlayers(gameMode) == 1 )
			isFlood = true ;
		else {
			switch( gameMode ) {
			case GameModes.GAME_MODE_SP_QUANTRO_C:
			case GameModes.GAME_MODE_SP_RETRO_C:
				isFlood = true ;
			}
		}

		return isFlood && !hasPremiumGameMode(gameMode);
		*/
	}
	
	/**
	 * If we have a trial only game mode, i.e. one for which hasTrialOnlyGameMode
	 * returns true, this method will return the length of said trial in seconds.
	 * 
	 * @param gameMode
	 * @return
	 */
	public int numTrialSecondsGameMode( int gameMode ) {
		boolean isFlood = false ;
		if ( GameModes.classCode(gameMode) == GameModes.CLASS_CODE_FLOOD && GameModes.maxPlayers(gameMode) == 1 )
			isFlood = true ;
		else {
			switch( gameMode ) {
			case GameModes.GAME_MODE_SP_QUANTRO_C:
			case GameModes.GAME_MODE_SP_RETRO_C:
				isFlood = true ;
			}
		}
		
		// 2.0 minutes, or not a trial.  After 120 seconds, we will have
		// introduced 21 rows on hard, 13.5 on normal.
		return isFlood ? 120 : -1 ;		
	}
	
	
}
