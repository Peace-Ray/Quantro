package com.peaceray.quantro.view.game.blocks;

import com.peaceray.quantro.q.QOrientations;

public class Consts {

	/**
	 * The Type of blocks contained in the provided field.  Skins may have
	 * different draw behavior depending on the block type.
	 * @author Jake
	 *
	 */
	public enum FieldType {
		/**
		 * Field blocks: just stuff lying around.
		 */
		FIELD,
		
		/**
		 * The currently controlled piece.
		 */
		PIECE,
		
		/**
		 * The 'ghost' of the currently controlled piece.
		 */
		GHOST
	}
	
	
	
	public final static int DRAW_DETAIL_MINIMAL = 0 ;	// extremely low-fi.  Fast, but very, very ugly.
	public final static int DRAW_DETAIL_LOW = 1 ;		// very basic colors, no fancy shadows or bitmaps
	public final static int DRAW_DETAIL_MID = 2 ;		// lots of shadows and bitmaps with neat border shading
	public final static int DRAW_DETAIL_HIGH = 3 ;		// same as mid, but includes outside drop-shadows.
	
	public final static int DRAW_ANIMATIONS_NONE = 0 ;
	public final static int DRAW_ANIMATIONS_FULL = 1 ;
	
	public final static int IMAGES_SIZE_NONE = 0 ;
	public final static int IMAGES_SIZE_SMALL = 1 ;
	public final static int IMAGES_SIZE_MID = 2 ;
	public final static int IMAGES_SIZE_LARGE = 3 ;
	public final static int IMAGES_SIZE_HUGE = 4 ;		// supported for backgrounds only.  BG image is max of screen size and LARGE.
	
	
	public static final int QPANE_ALL = -2 ;
	public static final int QPANE_3D = -1 ;
	public static final int QPANE_0 = 0 ;
	public static final int QPANE_1 = 1 ;
	
	
	
	
	public static final int GLOW_LOCK = 0 ;
	public static final int GLOW_CLEAR = 1 ;
	public static final int GLOW_METAMORPHOSIS = 2 ;
	public static final int GLOW_UNLOCK = 3 ;
	public static final int GLOW_ENTER = 4 ;
	public static final int NUM_GLOWS = 5 ;
	
	
	
	public static final boolean [] QO_INCLUDED_RETRO = new boolean[QOrientations.NUM] ;
	public static final boolean [] QO_INCLUDED_QUANTRO = new boolean[QOrientations.NUM] ;
	
	static {
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			QO_INCLUDED_RETRO[qo] = false ;
			QO_INCLUDED_QUANTRO[qo] = false ;
		}
		
		// Retro.  Includes all rainbow types, and PUSH blocks.
		QO_INCLUDED_RETRO[QOrientations.NO] = true ;
		for ( int qo = QOrientations.R0; qo <= QOrientations.R6; qo++ ) {
			QO_INCLUDED_RETRO[qo] = true ;
		}
		QO_INCLUDED_RETRO[QOrientations.RAINBOW_BLAND] = true ;
		QO_INCLUDED_RETRO[QOrientations.PUSH_DOWN] = true ;
		QO_INCLUDED_RETRO[QOrientations.PUSH_DOWN_ACTIVE] = true ;
		QO_INCLUDED_RETRO[QOrientations.PUSH_UP] = true ;
		QO_INCLUDED_RETRO[QOrientations.PUSH_UP_ACTIVE] = true ;
		
		
		// Quantro.  Includes everything EXCEPT what is in Retro (although
		// both have NO).
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			QO_INCLUDED_QUANTRO[qo] = QO_INCLUDED_RETRO[qo] ;
		}
		QO_INCLUDED_QUANTRO[QOrientations.NO] = true ;
	}
	
	
	
}
