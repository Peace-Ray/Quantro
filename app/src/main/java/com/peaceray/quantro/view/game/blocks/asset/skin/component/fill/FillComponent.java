package com.peaceray.quantro.view.game.blocks.asset.skin.component.fill;

import com.peaceray.quantro.view.game.blocks.asset.skin.component.DrawComponent;

/**
 * A "FillComponent," a modular component for SkinAssets, draws the 'fill layer'
 * of the skin.  Skins are typically divided into the following layers:
 * 
 * Background (the area "covered" by a block can be drawn differently than
 * 		the area not covered).
 * Fill (a solid-color region, at whatever transparency, which fills the block).
 * Top (an insignia, probably a region, bitmap, or rect, drawn on top of the fill layer).
 * Detail (e.g., shadow layers.  Drawn over the top.)
 * Borders (an edging drawn around the entire image).
 * 
 * As a logically separate step, we draw "outer detail" before fill, top, etc.,
 * which handles placing "drop shadows" or something similar around the blocks.
 * Outer detail exists only in "empty" squares.
 * 
 * 
 * 
 * @author Jake
 *
 */
public abstract class FillComponent extends DrawComponent {
	
}
