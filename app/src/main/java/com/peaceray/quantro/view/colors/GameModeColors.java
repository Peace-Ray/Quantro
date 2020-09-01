package com.peaceray.quantro.view.colors;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.q.QOrientations;

public class GameModeColors {

	
	public static boolean hasPrimary( int gameMode ) {
		return GameModes.has(gameMode) ;
	}
	
	public static int primary( ColorScheme colorScheme, int gameMode ) {
		if ( GameModes.isCustom(gameMode) ) {
			int customTemplate = GameModes.gameModeToCustomTemplate(gameMode) ;
			switch ( customTemplate ) {
			case GameModes.CUSTOM_TEMPLATE_1V1_RETRO_A:
				return primary( colorScheme, GameModes.GAME_MODE_1V1_RETRO_A ) ;
			case GameModes.CUSTOM_TEMPLATE_SP_RETRO_A:
				return primary( colorScheme, GameModes.GAME_MODE_SP_RETRO_A ) ;
			case GameModes.CUSTOM_TEMPLATE_SP_RETRO_B:
				return primary( colorScheme, GameModes.GAME_MODE_SP_RETRO_B ) ;
			case GameModes.CUSTOM_TEMPLATE_SP_RETRO_C:
				return primary( colorScheme, GameModes.GAME_MODE_SP_RETRO_C ) ;
			case GameModes.CUSTOM_TEMPLATE_1V1_QUANTRO_A:
				return primary( colorScheme, GameModes.GAME_MODE_1V1_QUANTRO_A ) ;
			case GameModes.CUSTOM_TEMPLATE_SP_QUANTRO_A:
				return primary( colorScheme, GameModes.GAME_MODE_SP_QUANTRO_A ) ;
			case GameModes.CUSTOM_TEMPLATE_SP_QUANTRO_B:
				return primary( colorScheme, GameModes.GAME_MODE_SP_QUANTRO_B ) ;
			case GameModes.CUSTOM_TEMPLATE_SP_QUANTRO_C:
				return primary( colorScheme, GameModes.GAME_MODE_SP_QUANTRO_C ) ;
			
			
			default:
				throw new IllegalArgumentException("No primary color for game mode " + gameMode + " with custom template " + customTemplate ) ;
			}
		}
		
		switch ( gameMode ) {
		case GameModes.GAME_MODE_SP_QUANTRO_A:
		case GameModes.GAME_MODE_1V1_QUANTRO_A:
		case GameModes.GAME_MODE_FREE4ALL_QUANTRO_A:
			return colorScheme.getBorderColorSimplified(QOrientations.S0, 0) ;
		case GameModes.GAME_MODE_SP_QUANTRO_B:
			return colorScheme.getBorderColorSimplified(QOrientations.S1, 1) ;
		case GameModes.GAME_MODE_SP_QUANTRO_C:
		case GameModes.GAME_MODE_1V1_QUANTRO_C:
		case GameModes.GAME_MODE_FREE4ALL_QUANTRO_C:
			return colorScheme.getBorderColorSimplified(QOrientations.F0, 0) ;
		case GameModes.GAME_MODE_SP_RETRO_A:
		case GameModes.GAME_MODE_1V1_RETRO_A:
		case GameModes.GAME_MODE_FREE4ALL_RETRO_A:
			return colorScheme.getBorderColorSimplified(QOrientations.R2, 0) ;
		case GameModes.GAME_MODE_SP_RETRO_B:
			return colorScheme.getBorderColorSimplified(QOrientations.R5, 0) ;
		case GameModes.GAME_MODE_SP_RETRO_C:
		case GameModes.GAME_MODE_1V1_RETRO_C:
		case GameModes.GAME_MODE_FREE4ALL_RETRO_C:
			return colorScheme.getBorderColorSimplified(QOrientations.R3, 0) ;
		case GameModes.GAME_MODE_1V1_QUANTRO_BITTER_PILL:
			return colorScheme.getFillColor(QOrientations.ST_INACTIVE, 0) ;
		case GameModes.GAME_MODE_1V1_RETRO_GRAVITY:
			return colorScheme.getDetailColor(QOrientations.PUSH_DOWN, 0, 0) ;
		default:
			throw new IllegalArgumentException("No primary color for game mode " + gameMode) ;
		}
	}
	
	public static boolean hasSecondary( int gameMode ) {
		return GameModes.has(gameMode) ;
	}
	
	public static int secondary( ColorScheme colorScheme, int gameMode ) {
		if ( GameModes.isCustom(gameMode) ) {
			int customTemplate = GameModes.gameModeToCustomTemplate(gameMode) ;
			switch ( customTemplate ) {
			case GameModes.CUSTOM_TEMPLATE_1V1_RETRO_A:
				return secondary( colorScheme, GameModes.GAME_MODE_1V1_RETRO_A ) ;
			case GameModes.CUSTOM_TEMPLATE_SP_RETRO_A:
				return secondary( colorScheme, GameModes.GAME_MODE_SP_RETRO_A ) ;
			case GameModes.CUSTOM_TEMPLATE_SP_RETRO_B:
				return secondary( colorScheme, GameModes.GAME_MODE_SP_RETRO_B ) ;
			case GameModes.CUSTOM_TEMPLATE_SP_RETRO_C:
				return secondary( colorScheme, GameModes.GAME_MODE_SP_RETRO_C ) ;
			case GameModes.CUSTOM_TEMPLATE_1V1_QUANTRO_A:
				return secondary( colorScheme, GameModes.GAME_MODE_1V1_QUANTRO_A ) ;
			case GameModes.CUSTOM_TEMPLATE_SP_QUANTRO_A:
				return secondary( colorScheme, GameModes.GAME_MODE_SP_QUANTRO_A ) ;
			case GameModes.CUSTOM_TEMPLATE_SP_QUANTRO_B:
				return secondary( colorScheme, GameModes.GAME_MODE_SP_QUANTRO_B ) ;
			case GameModes.CUSTOM_TEMPLATE_SP_QUANTRO_C:
				return secondary( colorScheme, GameModes.GAME_MODE_SP_QUANTRO_C ) ;
			
			default:
				throw new IllegalArgumentException("No secondary color for game mode " + gameMode + " with custom template " + customTemplate ) ;
			}
		}
		
		switch ( gameMode ) {
		case GameModes.GAME_MODE_SP_QUANTRO_A:
		case GameModes.GAME_MODE_1V1_QUANTRO_A:
		case GameModes.GAME_MODE_FREE4ALL_QUANTRO_A:
			return colorScheme.getBorderColorSimplified(QOrientations.F0, 0) ;
		case GameModes.GAME_MODE_SP_QUANTRO_B:
			return colorScheme.getBorderColorSimplified(QOrientations.ST, 1) ;
		case GameModes.GAME_MODE_SP_QUANTRO_C:
		case GameModes.GAME_MODE_1V1_QUANTRO_C:
		case GameModes.GAME_MODE_FREE4ALL_QUANTRO_C:
			return colorScheme.getFillColorSimplified(QOrientations.ST_INACTIVE, 1) ;
		case GameModes.GAME_MODE_SP_RETRO_A:
		case GameModes.GAME_MODE_1V1_RETRO_A:
		case GameModes.GAME_MODE_FREE4ALL_RETRO_A:
			return colorScheme.getBorderColorSimplified(QOrientations.R0, 0) ;
		case GameModes.GAME_MODE_SP_RETRO_B:
			return colorScheme.getBorderColorSimplified(QOrientations.R4, 0) ;
		case GameModes.GAME_MODE_SP_RETRO_C:
		case GameModes.GAME_MODE_1V1_RETRO_C:
		case GameModes.GAME_MODE_FREE4ALL_RETRO_C:
			return colorScheme.getBorderColorSimplified(QOrientations.RAINBOW_BLAND, 0) ;
		case GameModes.GAME_MODE_1V1_QUANTRO_BITTER_PILL:
			return colorScheme.getBorderColorSimplified(QOrientations.ST_INACTIVE, 0) ;
		case GameModes.GAME_MODE_1V1_RETRO_GRAVITY:
			return colorScheme.getDetailColor(QOrientations.PUSH_UP, 0, 0) ;
		default:
			throw new IllegalArgumentException("No secondary color for game mode " + gameMode) ;
		}
	}
	
	
	public static boolean hasTertiary( int gameMode ) {
		return GameModes.isCustom(gameMode) ;
	}
	
	public static int tertiary( ColorScheme colorScheme, int gameMode ) {
		if ( GameModes.isCustom(gameMode) )
			return customTertiary( colorScheme, GameModes.numberQPanes(gameMode) ) ;
		
		throw new IllegalArgumentException("No tertiary color for game mode " + gameMode) ;
	}
	
	
	
	public static int customPrimary( ColorScheme colorScheme, int qpanes ) {
		return qpanes == 2 ? colorScheme.getBorderColorSimplified(QOrientations.ST, 0)
				: colorScheme.getBorderColorSimplified(QOrientations.R1, 0) ;
	}
	
	public static int customSecondary( ColorScheme colorScheme, int qpanes ) {
		return qpanes == 2 ? colorScheme.getBorderColorSimplified(QOrientations.F1, 1)
				: colorScheme.getBorderColorSimplified(QOrientations.R6, 0) ;
	}
	
	public static int customTertiary( ColorScheme colorScheme, int qpanes ) {
		return qpanes == 2 ? colorScheme.getBorderColorSimplified(QOrientations.SL, 0)
				: colorScheme.getBorderColorSimplified(QOrientations.RAINBOW_BLAND, 0) ;
	}
	
	
	
	
	/**
	 * Returns a block_background drawable for the specified game mode, under the provided
	 * color scheme, or 'null' if none can be constructed.
	 * 
	 * We recommend sparse use of customized block drawables, e.g. for dialogs directly relevant to
	 * the game mode, such as New Game settings, Examine Game windows, etc.  Most dialogs - especially
	 * those which are generic ("connecting to server," etc.) should be neutrally-colored.
	 * 
	 * @param colorScheme
	 * @param gameMode
	 * @return
	 */
	public static Drawable blockBackgroundDrawable( Context context, ColorScheme colorScheme, int gameMode ) {
		
		// TODO
		return null ;
		
	}
	
}
