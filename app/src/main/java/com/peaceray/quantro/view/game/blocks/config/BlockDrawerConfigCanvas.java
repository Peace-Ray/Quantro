package com.peaceray.quantro.view.game.blocks.config;

import android.graphics.Rect;

/**
 * How we blit and/or draw our content to the provided canvas.  The content
 * itself is largely determined by other Config objects, including alignment
 * (center?  0 at bottom? etc.).
 * 
 * Canvas config provides the region of the canvas to which content is drawn,
 * the capacity to offset the content based on slice content, and to adjust 
 * the background drawn (standard or fully transparent).
 * 
 * @author Jake
 *
 */
public class BlockDrawerConfigCanvas {
	
	public enum Scale {
		/**
		 * Scales the content to fit exactly within the region.  Aspect ratio
		 * is NOT maintained.
		 */
		FIT_EXACT,
		
		/**
		 * Scales the content to fit either X or Y exactly, maintaining aspect ratio.
		 * The other dimension will either match, or extend beyond the bounds of the region.
		 */
		FIT_X_OR_Y,
		
		/**
		 * Scales the content to fit horizontally within the region, maintaining aspect ratio.
		 * May extend beyond or fit within vertically.
		 */
		FIT_X,
		
		/**
		 * Scales the content to fit vertically within the region, maintaining aspect ratio.
		 * May extend beyond or fit within horizontally.
		 */
		FIT_Y,
		
		/**
		 * No scaling; use the block size.
		 */
		NONE
	}
	
	public enum Alignment {
		
		/**
		 * Attempts to center the provided content within the region.
		 */
		CONTENT_CENTER,
		
		/**
		 * Attempts to Y-Offset the content so that the top portion of the
		 * region is empty of blocks.
		 */
		CONTENT_TOP_EMPTY,
		
		/**
		 * No custom alignment.  Whatever aligment is used to draw the blocks
		 * will be used to blit them.
		 */
		NONE, 
	}
	
	public enum Background {
		
		/**
		 * Draw whatever background is configured.  Could be a solid color,
		 * an image, etc.
		 */
		DEFAULT,
		
		/**
		 * Clear the canvas and draw w/o the background.  If the Canvas
		 * represents an image with an alpha channel, this area will
		 * be left transparent.
		 */
		CLEAR,
		
		/**
		 * Do not draw the background at all; don't clear the canvas or
		 * draw any BG pixels.  The blocks will be drawn on top of 
		 * the current canvas content.
		 */
		NONE
	}

	
	/**
	 * The scale type to apply in the draw.
	 */
	public Scale scale ;
	
	/**
	 * The alignment type to use in the draw.
	 */
	public Alignment alignment ;
	
	/**
	 * Whether the background should be drawn to the canvas.  If 'false',
	 * 
	 */
	public Background background ;
	
	/**
	 * The region of the canvas within which we attempt to draw.
	 * Scale and alignment will be applied relative to this region.
	 */
	public Rect region ;
	
	/**
	 * If non-null, this region of the canvas will be clipped with INTERSECT
	 * before the draw.
	 */
	public Rect clipRegion ;
	
	
	
	@Override
	public boolean equals( Object obj ) {
		if ( this == obj )
			return true ;
		if ( !( obj instanceof BlockDrawerConfigCanvas ) )
			return false ;
		
		BlockDrawerConfigCanvas config = (BlockDrawerConfigCanvas)obj ;
		if ( scale != config.scale )
			return false ;
		if ( alignment != config.alignment )
			return false ;
		if ( background != config.background )
			return false ;
		if ( !region.equals( config.region ) )
			return false ;
		if ( ( (clipRegion == null) != (config.clipRegion == null) )
				|| ( clipRegion != null && !clipRegion.equals(config.clipRegion)) )
			return false ;
		return true ;
	}
	
	
	public BlockDrawerConfigCanvas( Rect region ) {
		this(region.left, region.top, region.right, region.bottom) ;
	}
	
	public BlockDrawerConfigCanvas( int left, int top, int right, int bottom ) {
		scale = Scale.FIT_EXACT ;
		alignment = Alignment.NONE ;
		background = Background.DEFAULT ;
		
		region = new Rect( left, top, right, bottom ) ;
		clipRegion = new Rect( left, top, right, bottom ) ;
	}
	
	
	public BlockDrawerConfigCanvas( BlockDrawerConfigCanvas bdcc ) {
		scale = bdcc.scale ;
		alignment = bdcc.alignment ;
		background = bdcc.background ;
		
		region = new Rect( bdcc.region ) ;
		clipRegion = bdcc.clipRegion == null ? null : new Rect(bdcc.clipRegion) ;
	}
	
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// ACCESSORS
	
	public Scale getScale() {
		return scale ;
	}
	
	public Alignment getAlignment() {
		return alignment ;
	}
	
	public Background getBackground() {
		return background ;
	}
	
	public Rect getRegion() {
		return region ;
	}
	
	public Rect getClipRegion() {
		return clipRegion ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// DIRECT SETTERS
	
	public void setScale( Scale s ) {
		scale = s ;
	}
	
	public void setAlignment( Alignment a ) {
		alignment = a ; 
	}
	
	public void setBackground( Background b ) {
		background = b ;
	}
	
	public void setRegion( Rect r ) {
		region.set(r) ;
	}
	
	public void setRegion( int left, int top, int right, int bottom ) {
		region.set( left, top, right, bottom ) ;
	}
	
	public void setRegionAndClip( Rect r ) {
		setRegionAndClip( r.left, r.top, r.right, r.bottom ) ;
	}
	
	public void setRegionAndClip( int left, int top, int right, int bottom ) {
		region.set( left, top, right, bottom ) ;
		if ( clipRegion != null )
			clipRegion.set( left, top, right, bottom ) ;
		else
			clipRegion = new Rect( left, top, right, bottom ) ;
	}
	
	
	
	public void setClipRegion( Rect r ) {
		if ( r == null )
			setNoClip() ;
		else
			setClipRegion( r.left, r.top, r.right, r.bottom ) ;
	}
	
	public void setClipRegion( int left, int top, int right, int bottom ) {
		if ( clipRegion != null )
			clipRegion.set( left, top, right, bottom ) ;
		else
			clipRegion = new Rect( left, top, right, bottom ) ;
	}
	
	public void setNoClip( ) {
		clipRegion = null ;
	}
}
