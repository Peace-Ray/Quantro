package com.peaceray.quantro.view.game.blocks.asset.skin;

import android.content.Context;

import com.peaceray.quantro.content.Skin;

public class SkinAssetBuilderFactory {
	
	private static final boolean FORCE_STUB = true ;

	/**
	 * Returns an appropriate builder (BackgroundBuilder<SkinAsset>) for the
	 * specified Skin.
	 * 
	 * @param skin
	 * @return
	 */
	public static SkinAsset.Builder getBuilder( Context context, Skin skin ) {
		Skin.Game g = skin.getGame() ;
		Skin.Template t = skin.getTemplate() ;
		
		if ( FORCE_STUB )
			return new StubSkinAsset.Builder(context, skin) ;
		
		switch( g ) {
		case RETRO:
			switch( t ) {
			case STANDARD:
				return new RetroStandardSkinAsset.Builder( context, skin ) ;
			case NEON:
				return new RetroNeonSkinAsset.Builder( context, skin ) ;
			}
			break ;
			
		case QUANTRO:
			switch( t ) {
			case STANDARD:
			case COLORBLIND:
				return new QuantroStandardSkinAsset.Builder( context, skin ) ;
			}
			break ;
		}
		
		throw new IllegalArgumentException("Don't know how to create a builder for skin " + skin) ;
	}
	
}
