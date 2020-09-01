package com.peaceray.quantro.view.game.blocks.asset.skin.component;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;

import com.peaceray.quantro.QuantroApplication;
import com.peaceray.quantro.QuantroPreferences;
import com.peaceray.quantro.consts.GlobalTestSettings;
import com.peaceray.quantro.utils.Analytics;
import com.peaceray.quantro.view.game.GameViewMemoryCapabilities;
import com.peaceray.quantro.view.game.blocks.Consts;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

public class Render {

	
	public enum SheetStyle {
		
		/**
		 * "Shadows" of the walls, shown as gaussian blurs.
		 */
		SHADOW,
		
		/**
		 * As 'shadow', except inset by approx. 10% of the total
		 * block width on each side.  Represents the inner shadows cast
		 * by inner borders.
		 */
		SHADOW_OVERSIZED
	}
	
	
	
	/**
	 * In the most general sense, what type of shadow is being portrayed?  Is it
	 * cast "inward" by a block onto itself, or "outward" onto other blocks?
	 * 
	 * This value is very useful in rendering custom shadows for non-joining block
	 * types.
	 */
	public enum ShadowDirection {
		
		/**
		 * This shadow is projected "inward" towards the block's interior.
		 */
		IN,
		
		/**
		 * This shadow is projected "outward" towards other blocks.
		 */
		OUT,
	}
	
	
	/**
	 * Renders the specified sheet, which must be one of SHADOW_*, to
	 * the provided canvas.
	 * 
	 * @param context
	 * @param canvas
	 * @param shadowSheetStyle
	 * @param blockWidth
	 * @param blockHeight
	 * @param xOffset
	 * @param yOffset
	 * @param radius
	 * @param alpha
	 */
	public static void shadowSheet( Context context, Canvas canvas, Bitmap scratch,
			int loadImageSize, SheetStyle shadowSheetStyle,
			int blockWidth, int blockHeight,
			float xOffset, float yOffset, float radius, int alpha ) throws IOException {
		
		if ( shadowSheetStyle != SheetStyle.SHADOW && shadowSheetStyle != SheetStyle.SHADOW_OVERSIZED )
			throw new IllegalArgumentException("Shadow sheet style must be SHADOW_*") ;
		
		// retrieve from cache...
		Bitmap sheetBitmap = loadRawSheetFromAssets(context,
				loadImageSize, shadowSheetStyle, blockWidth, xOffset, yOffset, radius );
		
		// We have the image.
		int bitmapBlockWidth = sheetBitmap.getWidth() / 16;
		int bitmapBlockHeight = sheetBitmap.getHeight() / 16;
		
		
		boolean scratchProvided ;
		if ( scratch == null || scratch.getWidth() < blockWidth || scratch.getHeight() < blockHeight ) {
			// we will recycle this bitmap at the end of the call.
			scratchProvided = false ;
			scratch = Bitmap.createBitmap(blockWidth, blockHeight, Bitmap.Config.ARGB_8888) ;
		} else {
			scratchProvided = true ;
		}

		// now.... COUPLE!
		ColorMatrixColorFilter filter = new ColorMatrixColorFilter(new float[] {
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, alpha/255.0f, 0 });
		Paint paint = new Paint() ;
		paint.setColorFilter(filter);
		
		Canvas scratchCanvas = new Canvas(scratch) ;
		Paint clearPaint = new Paint() ;
		clearPaint.setColor(0x00000000);
		clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		
		Rect r = new Rect(0, 0, blockWidth, blockHeight);
		
		Rect srcRect = new Rect() ;
		Rect dstRect = new Rect() ;

		for (int i = 0; i < 256; i++) {
			sheetIndexByNeighbors_sheetRect(srcRect, i, bitmapBlockWidth,
					bitmapBlockHeight);
			sheetIndexByNeighbors_sheetRect(dstRect, i, blockWidth, blockHeight);

			// draw
			scratchCanvas.drawPaint(clearPaint);
			scratchCanvas.drawBitmap(sheetBitmap, srcRect, r, null);
			canvas.drawBitmap(scratch, r, dstRect, paint);
		}

		sheetBitmap.recycle();
		if ( !scratchProvided ) {
			scratch.recycle() ;
		}
		// that's it, we done.
	}
	
	
	
	public static void shadowSquare( Context context, Canvas canvas, Bitmap scratch,
			int loadImageSize, SheetStyle shadowSheetStyle,
			int blockWidth, int blockHeight, Rect insetToRect,
			float radius, int alpha ) throws IOException {
		
		if ( shadowSheetStyle != SheetStyle.SHADOW )
			throw new IllegalArgumentException("Shadow sheet style must be SHADOW") ;
		
		// retrieve from cache...
		Bitmap srcBitmap = loadExactShadowSquareFromAssets(context,
				loadImageSize, blockWidth, radius);
		
		Paint p = new Paint();
		ColorMatrixColorFilter filterEdgeShadow = new ColorMatrixColorFilter(
				new float[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, alpha / 255.0f, 0 });
		// RED -> 0 GREEN -> 0 BLUE -> 0 ALPHA scaled by draw settings.

		p.setColorFilter(filterEdgeShadow);
		p.setDither(false);
		Rect srcRect = new Rect();
		srcRect.left = srcBitmap.getWidth() / 3;
		srcRect.right = 2 * srcBitmap.getWidth() / 3;
		srcRect.top = srcBitmap.getHeight() / 3;
		srcRect.bottom = 2 * srcBitmap.getHeight() / 3;
		
		canvas.drawBitmap(srcBitmap, srcRect, insetToRect, p);
		
		srcBitmap.recycle() ;
	}
	
	
	/**
	 * Renders a "drop shadow square" which includes a black fill.  If you don't want
	 * the fill, then set the canvas clip to exclude it before this call.
	 * 
	 * 'blockBounds' gives the full, block-size bounds, describing exactly the
	 * block within which the square is "inset."
	 * 
	 * 'shadowInset' provides the symmetrical "around the edge" inset for the block.
	 * 
	 * Note that there is no way to explicitly set the inset within the provided
	 * canvas for the block to be drawn.  Make do.
	 * 
	 * @param context
	 * @param canvas
	 * @param scratch
	 * @param loadImageSize
	 * @param shadowSheetStyle
	 * @param blockBounds
	 * @param radius
	 * @param alpha
	 * @param shadowInset
	 * @throws IOException
	 */
	public static void shadowDropSquare( Context context, Canvas canvas, Bitmap scratch,
			int loadImageSize, SheetStyle shadowSheetStyle,
			Rect blockBounds,
			int xOffset, int yOffset, float radius, int alpha, float shadowInset ) throws IOException {
		
		// closest block width?
		int blockWidth;
		if (GlobalTestSettings.FORCE_LOAD_LARGE_RESOURCES)
			blockWidth = 80;
		else if (blockBounds.width() < 25)
			blockWidth = 20;
		else if (blockBounds.width() < 50)
			blockWidth = 40;
		else
			blockWidth = 80;

		String insetStr;
		if (shadowInset <= 0.1)
			insetStr = "0.1";
		else
			insetStr = "0.15";
		String assetName = "inset_" + insetStr + ".png";

		// render it ourselves.
		// approximate radius!
		String radiusStr;
		if (radius < 0.05)
			radiusStr = "0";
		else if (radius < 0.1)
			radiusStr = "0.05";
		else
			radiusStr = "0.1";

		File shadowDir = new File(
				new File(new File("game"), "shadows"), "drop");
		File fileDir = new File(new File(shadowDir, "" + blockWidth
				+ "x" + blockWidth), radiusStr);

		String assetPath = new File(fileDir, assetName).getPath();
		AssetManager as = context.getAssets();

		BufferedInputStream buf = new BufferedInputStream(
				as.open(assetPath));
		Bitmap bshad = BitmapFactory.decodeStream(buf);
		buf.close();

		// Set source and dest rectangles, then offset dest.
		Rect srcRect = new Rect(0, 0, bshad.getWidth(), bshad.getHeight());
		Rect dstRect = new Rect(blockBounds) ;
		dstRect.offset(xOffset, yOffset);

		Paint p = new Paint();
		p.setAlpha(alpha);

		canvas.drawBitmap(bshad, srcRect, dstRect, p);

		bshad.recycle();
	}
	
	
	
	/**
	 * Returns the bitmap indicated by the sheet parameters. This method
	 * guarantees that the bitmap returned will be in the provided cache. If it
	 * is present, that reference is returned. If not, it is loaded from assets
	 * and placed in the provided cache before being returned.
	 * 
	 * @param cache
	 * @param blockSize
	 * @param blurRadius
	 * @param sheetSet
	 * @param xOffset
	 * @param yOffset
	 * @throws IOException
	 */
	private static final Bitmap loadRawSheetFromAssets(Context context,
			int loadSize, SheetStyle sheetStyle,
			int blockSize, float xOffset, float yOffset, float radius) throws IOException {

		Bitmap b = null;
		String assetPath = null;
		int attempt = 0;

		while (b == null) {
			try {
				attempt++;
				assetPath = getRawSheetAssetPath(blockSize, radius,
						sheetStyle, xOffset, yOffset, loadSize);
				// Log.d(TAG, "retrieving asset " + assetPath) ;
				AssetManager as = context.getAssets();

				BufferedInputStream buf = new BufferedInputStream(
						as.open(assetPath));
				Bitmap b2 = BitmapFactory.decodeStream(buf);
				buf.close();
				b = b2.extractAlpha();
				b2.recycle();
			} catch (OutOfMemoryError oome) {
				if (QuantroPreferences.getAnalyticsActive(context)) {
					GameViewMemoryCapabilities gvmc = null;
					gvmc = ((QuantroApplication) context
							.getApplicationContext()).getGameViewMemoryCapabilities(null);

					Analytics.logInGameOutOfMemory(gvmc, assetPath, attempt);
				}

				throw oome;
			}
		}

		return b;
	}
	
	
	public static final String getRawSheetAssetPath(int blockSize,
			float blurRadius, SheetStyle style, float xOffset, float yOffset,
			int loadImagesSize) {

		String resString = nextLargestResolutionString(blockSize, blockSize,
				loadImagesSize);
		String radString = nearestBlurRadiusString(blurRadius);
		String setString = sheetSetString(style);
		String offString = nearestOffsetString(xOffset, yOffset);

		File detailsDir = new File(new File(new File("game"), "shadows"),
				"sheets");
		return new File(new File(new File(detailsDir, resString), radString),
				setString + offString + ".png").getPath();
	}
	
	private static final Bitmap loadExactShadowSquareFromAssets(Context context,
			int loadImagesSize, int blockWidth, float radius)
			throws IOException {

		String assetPath = getExactShadowSquareAssetPath(
				loadImagesSize, blockWidth, radius );
		AssetManager as = context.getAssets();

		BufferedInputStream buf = new BufferedInputStream(as.open(assetPath));
		Bitmap b = BitmapFactory.decodeStream(buf);
		buf.close();

		return b;
	}
	
	
	private static final String getExactShadowSquareAssetPath(
			int loadImagesSize, int blockSize, float blurRadius ) {

		String resString = nearestResolutionString(blockSize, blockSize,
				loadImagesSize);
		String radString = nearestBlurRadiusString(blurRadius);

		File shadowDir = new File(new File(
				new File(new File("game"), "shadows"), "white"), "exact");
		String assetPath = new File(new File(new File(shadowDir, resString),
				radString), "set_0.png").getPath();

		return assetPath;
	}
	
	private static String nextLargestResolutionString(int width, int height,
			int loadSize) {

		int dimension = nextLargestLoadDimension(width, height, loadSize);
		return "" + dimension + "x" + dimension;
	}

	private static int nextLargestLoadDimension(int width, int height,
			int loadSize) {
		if (GlobalTestSettings.FORCE_LOAD_LARGE_RESOURCES)
			return 80;

		int max = Math.max(width, height);
		if (max <= 20)
			return 20;
		else if (max <= 80)
			return loadSize >= Consts.IMAGES_SIZE_MID ? 40 : 20;
		else
			return loadSize >= Consts.IMAGES_SIZE_LARGE ? 80 : 40;
	}
	
	private static String nearestResolutionString(int width, int height,
			int loadSize) {

		int dimension = nearestResolutionDimension(width, height, loadSize);
		return "" + dimension + "x" + dimension;
	}
	
	private static int nearestResolutionDimension(int width, int height,
			int loadSize) {

		if (GlobalTestSettings.FORCE_LOAD_LARGE_RESOURCES)
			return 80;

		int avg = (width + height) / 2;
		if (avg < 40)
			return 20;
		else if (avg < 80)
			return loadSize >= Consts.IMAGES_SIZE_MID ? 40 : 20;
		else
			return loadSize >= Consts.IMAGES_SIZE_LARGE ? 80 : 40; // "80x80"
																			// ;
	}
	
	private static String nearestBlurRadiusString(float radius) {
		if (radius == 0)
			return "0";
		else if (radius < 0.075)
			return "0.05";
		else if (radius < 0.15)
			return "0.1";
		else if (radius < 0.25)
			return "0.2";
		else if (radius < 0.35)
			return "0.3";
		else if (radius < 0.45)
			return "0.4";
		else if (radius < 0.63)
			return "0.5";
		else if (radius < 0.88)
			return "0.75";
		else
			return "1.0";
	}
	
	private static String sheetSetString(SheetStyle style) {
		if (style == SheetStyle.SHADOW)
			return "exact";
		else if (style == SheetStyle.SHADOW_OVERSIZED)
			return "oversized";
		return null;
	}

	private static String nearestOffsetString(float xOffset, float yOffset) {
		if (xOffset < 0)
			return "-0.15-0.15";
		else if (xOffset == 0)
			return "";
		else if (xOffset < 0.1)
			return "+0.05+0.05";
		else
			return "+0.2+0.25";
	}
	
	
	/**
	 * ENCODED_OPEN is the encoding for fully-connected to all 8 neighbors.
	 * For our motivating example (inner shadows and drop shadows) this results
	 * in no shadows being drawn.
	 */
	public static final short SHEET_INDEX_ENCODED_OPEN = 255 ;
	
	private static final int IBN_BIT_1 = 0x80;
	private static final int IBN_BIT_2 = 0x40;
	private static final int IBN_BIT_3 = 0x20;
	private static final int IBN_BIT_4 = 0x10;
	private static final int IBN_BIT_5 = 0x8;
	private static final int IBN_BIT_6 = 0x4;
	private static final int IBN_BIT_7 = 0x2;
	private static final int IBN_BIT_8 = 0x1;
	
	// alias these
	private static final int IBN_BIT_L = IBN_BIT_1 ;
	private static final int IBN_BIT_D = IBN_BIT_2 ;
	private static final int IBN_BIT_R = IBN_BIT_3 ;
	private static final int IBN_BIT_U = IBN_BIT_4 ;
	private static final int IBN_BIT_LD = IBN_BIT_5 ;
	private static final int IBN_BIT_RD = IBN_BIT_6 ;
	private static final int IBN_BIT_RU = IBN_BIT_7 ;
	private static final int IBN_BIT_LU = IBN_BIT_8 ;
	
	
	/**
	 * For 0 <= index <= 255, sets the provided Rect's fields to enclose exactly
	 * the region of a shadow sheet bitmap specified by the index. We assume
	 * 'blockWidth, blockHeight' are the dimensions of each shadow sheet
	 * element, and there is no space between elements (pixels are adjacent
	 * between adjacent elements, with no overlap).
	 */
	public static void sheetIndexByNeighbors_sheetRect(Rect rect, int index,
			int blockWidth, int blockHeight) {
		int row = index / 16;
		int col = index % 16;

		rect.left = blockWidth * col;
		rect.top = blockHeight * row;
		rect.right = rect.left + blockWidth;
		rect.bottom = rect.top + blockHeight;
	}
	
	
	/**
	 * Returns the IBN_BIT_* value representing the direction indicated.
	 * Negative/positive colOffset indicates left/right.  Negative/positive rowOffset
	 * indicates down/up.
	 * 
	 * Parameters have only 3 values of significance: zero, positive, and negative.
	 * 
	 * @param xOffset
	 * @param yOffset
	 * @return
	 */
	public static final int sheetIndexByNeighbors_directionBit( int rowOffset, int colOffset ) {
		if ( colOffset == 0 ) {
			if ( rowOffset == 0 )
				return 0 ;
			else if ( rowOffset < 0 )
				return IBN_BIT_D ;		// DOWN.
			else
				return IBN_BIT_U ; 		// UP.
		} else if ( colOffset < 0 ) {
			if ( rowOffset == 0 )
				return IBN_BIT_L ;		// LEFT.
			else if ( rowOffset < 0 )
				return IBN_BIT_LD ;		// LEFT-DOWN.
			else
				return IBN_BIT_LU ;		// LEFT_UP.
		} else {
			if ( rowOffset == 0 )
				return IBN_BIT_R ;		// RIGHT.
			else if ( rowOffset < 0 )
				return IBN_BIT_RD ;		// DOWN-RIGHT.
			else
				return IBN_BIT_RU ;		// UP-RIGHT.
		}
	}
	
	public static short[] sheetIndexByNeighbors_sanityFix_cache = new short[256];
	static {
		boolean[][] connected = new boolean[3][3];
		for (int index = 0; index < 256; index++) {
			sheetNeighborsByIndex(index, connected, 1, 1);
			sheetIndexByNeighbors_sanityFix(connected, 1, 1);
			sheetIndexByNeighbors_sanityFix_cache[index] = sheetIndexByNeighbors(
					connected, 1, 1);
		}
	}
	
	/**
	 * Similar to the array-parameterized version, except corrects an index into
	 * its "sanity-fixed" equivalent.
	 * 
	 * PRECONDITION: 'index' is >= 0 and < indexByNeighbors_num().
	 * 
	 * @param index
	 * @return
	 */
	public static short sheetIndexByNeighbors_sanityFix(short index) {
		return sheetIndexByNeighbors_sanityFix_cache[index];
	}
	
	
	/**
	 * "Fixes" the connected array to remove "connected" neighbors that cannot
	 * be considered connected.
	 * 
	 * @param connected
	 * @param x
	 * @param y
	 * @return
	 */
	public static boolean sheetIndexByNeighbors_sanityFix(boolean[][] connected,
			int x, int y) {
		boolean changed = false;
		if (connected[x - 1][y + 1]
				&& !(connected[x][y + 1] || connected[x - 1][y])) {
			connected[x - 1][y + 1] = false;
			changed = true;
		}
		if (connected[x + 1][y + 1]
				&& !(connected[x][y + 1] || connected[x + 1][y])) {
			connected[x + 1][y + 1] = false;
			changed = true;
		}
		if (connected[x + 1][y - 1]
				&& !(connected[x][y - 1] || connected[x + 1][y])) {
			connected[x + 1][y - 1] = false;
			changed = true;
		}
		if (connected[x - 1][y - 1]
				&& !(connected[x][y - 1] || connected[x - 1][y])) {
			connected[x - 1][y - 1] = false;
			changed = true;
		}

		return changed;
	}
	
	
	/**
	 * Uses the boolean array indicated "connectedness" to determine the
	 * neighbor index to use. connected[row][col] is the block in question. If
	 * 'false', we assume the block is disconnected from all others. If 'true',
	 * we check the immediate neighbors (row +/- 1, col +/- 1) for
	 * connectedness.
	 * 
	 * The value returned is guaranteed to be in [-1, indexByNeighbors_num()-1],
	 * and is deterministic based on the true/false values provided.
	 * 
	 * If the value returned is -1, then the specified array is completely
	 * invalid (for whatever reason) and we refuse to process it.
	 * 
	 * @param connected
	 * @param row
	 * @param col
	 * @return
	 */
	public static short sheetIndexByNeighbors(boolean[][] connected, int x, int y) {
		if (!connected[x][y])
			return -1;

		int index = 0; // we add values to this index as we go.

		// left, down, right, up.
		if (connected[x - 1][y])
			index += IBN_BIT_1;
		if (connected[x][y + 1])
			index += IBN_BIT_2;
		if (connected[x + 1][y])
			index += IBN_BIT_3;
		if (connected[x][y - 1])
			index += IBN_BIT_4;

		// diagonal neighbors. Note that we can only connect to diagonal
		// neighbors
		// if there is a connection through adjacent neighbors!
		if (connected[x - 1][y + 1]
				&& (connected[x][y + 1] || connected[x - 1][y]))
			index += IBN_BIT_5;
		if (connected[x + 1][y + 1]
				&& (connected[x][y + 1] || connected[x + 1][y]))
			index += IBN_BIT_6;
		if (connected[x + 1][y - 1]
				&& (connected[x][y - 1] || connected[x + 1][y]))
			index += IBN_BIT_7;
		if (connected[x - 1][y - 1]
				&& (connected[x][y - 1] || connected[x - 1][y]))
			index += IBN_BIT_8;

		return (short) index;
	}
	
	/**
	 * The inverse of indexByNeighbors. Sets
	 * 'connected[xPos-1:xPos+1][yPos-1:yPos+1]' to the appropriate neighbors
	 * arrangement. Returns whether provided index was valid; if returns false,
	 * the state of 'connected' is unspecified.
	 * 
	 * @param index
	 * @param xOffset
	 * @param yOffset
	 * @return
	 */
	public static boolean sheetNeighborsByIndex(int index, boolean[][] connected,
			int x, int y) {
		if (index < 0 || index > 255)
			return false;

		connected[x][y] = true;
		connected[x - 1][y] = (index / IBN_BIT_1) % 2 == 1; // left
		connected[x][y + 1] = (index / IBN_BIT_2) % 2 == 1; // down
		connected[x + 1][y] = (index / IBN_BIT_3) % 2 == 1; // right
		connected[x][y - 1] = (index / IBN_BIT_4) % 2 == 1; // up

		connected[x - 1][y + 1] = (connected[x - 1][y] || connected[x][y + 1])
				&& (index / IBN_BIT_5) % 2 == 1; // left-down
		connected[x + 1][y + 1] = (connected[x + 1][y] || connected[x][y + 1])
				&& (index / IBN_BIT_6) % 2 == 1; // right-down
		connected[x + 1][y - 1] = (connected[x + 1][y] || connected[x][y - 1])
				&& (index / IBN_BIT_7) % 2 == 1; // right-up
		connected[x - 1][y - 1] = (connected[x - 1][y] || connected[x][y - 1])
				&& (index / IBN_BIT_8) % 2 == 1; // left-up

		return true;
	}
	
}
