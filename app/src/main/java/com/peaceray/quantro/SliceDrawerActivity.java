package com.peaceray.quantro;

import java.io.File;
import java.io.FileOutputStream;

import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.model.game.GameBlocksSlice;
import com.peaceray.quantro.model.game.GameBlocksSliceSequence;
import com.peaceray.quantro.view.game.DrawSettings;
import com.peaceray.quantro.view.game.SliceSequenceView;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigCanvas;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;


/**
 * SliceDrawerActivity
 * 
 * The primary purpose of this activity is to draw an arbitrary slice or sequence,
 * as large as reasonable, to the screen.  The slice or sequence must be provided as
 * an Intent extra, along with the Skin to use in its display.
 * 
 * Other options include whether the image should be drawn to both the screen and
 * saved in the users shared storage, and if so, how frequently screencaps
 * should be taken (and how many).
 * 
 * @author Jake
 *
 */
public class SliceDrawerActivity extends QuantroActivity {
	
	private static final String TAG = "SliceDrawerActivity" ;

	// the source slice / sequence.  At least one MUST be provided.
	// Behavior is unspecified if both are provided.
	public static final String INTENT_EXTRA_SLICE_BYTES = "com.peaceray.quantro.SliceDrawerActivity.INTENT_EXTRA_SLICE" ;
	public static final String INTENT_EXTRA_SEQUENCE_BYTES = "com.peaceray.quantro.SliceDrawerActivity.INTENT_EXTRA_SEQUENCE" ;
	
	public static final String INTENT_EXTRA_SLICE_NAME = "com.peaceray.quantro.SliceDrawerActivity.INTENT_EXTRA_SLICE_NAME" ;
	public static final String INTENT_EXTRA_SKIN_STRING_ENCODING = "com.peaceray.quantro.SliceDrawerActivity.INTENT_EXTRA_SKIN_STRING_ENCODING" ;
	
	SliceSequenceView mSliceSequenceView ;
	
	GameBlocksSliceSequence mSliceSequence ;
	GameBlocksSlice mSlice ;
	
	String mSliceName ;
	
	int mR, mC ;
	
	Skin mSkin ;
	
	DrawSettings mDrawSettings ;
	
	int mNumScreencaps ;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState) ;
		setupQuantroActivity( QUANTRO_ACTIVITY_MENU, QUANTRO_ACTIVITY_CONTENT_FULL ) ;
        
		
		// content view
		setContentView(R.layout.slice_drawer) ;
		mSliceSequenceView = (SliceSequenceView) findViewById(R.id.slice_drawer_slice_sequence_view) ;
		
		// load the slice or sequence
		Intent intent = getIntent() ;
		if ( intent.hasExtra(INTENT_EXTRA_SEQUENCE_BYTES) ) {
			mSliceSequence = new GameBlocksSliceSequence( intent.getByteArrayExtra(INTENT_EXTRA_SEQUENCE_BYTES) ) ;
			mR = mSliceSequence.rows() ;
			mC = mSliceSequence.cols() ;
		} else if ( intent.hasExtra( INTENT_EXTRA_SLICE_BYTES ) ) {
			mSlice = new GameBlocksSlice( intent.getByteArrayExtra(INTENT_EXTRA_SLICE_BYTES) ) ;
			mR = mSlice.rows() ;
			mC = mSlice.cols() ;
		}
		
		mSliceName = intent.getStringExtra(INTENT_EXTRA_SLICE_NAME) ;
		
		// load skin
		if ( intent.hasExtra(INTENT_EXTRA_SKIN_STRING_ENCODING) )
			mSkin = Skin.fromStringEncoding( intent.getStringExtra(INTENT_EXTRA_SKIN_STRING_ENCODING) ) ;
		else {
			mSkin = QuantroPreferences.getSkinQuantro(this) ;
		}
		
		// make draw settings according to the intent, skin, and slice size.
		mDrawSettings = new DrawSettings(
				new Rect(0,0,100,100),
				mR, 		// rows
				mC, 		// cols
				mR,		// displayed rows
				QuantroPreferences.getGraphicsGraphicalDetail(this),
				DrawSettings.DRAW_ANIMATIONS_ALL,
				mSkin,
				this ) ;
		mDrawSettings.setBlit( DrawSettings.BLIT_NONE, 1 ) ;
		mDrawSettings.behavior_background = DrawSettings.BEHAVIOR_BACKGROUND_BLOCK_FILL ;
		mDrawSettings.horizontalMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)30, getResources().getDisplayMetrics()) ;
		mDrawSettings.verticalMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)30, getResources().getDisplayMetrics()) ;
		mDrawSettings.behavior_align_horizontal = DrawSettings.BEHAVIOR_ALIGN_CENTER_BLOCKS ;
		mDrawSettings.behavior_align_vertical = DrawSettings.BEHAVIOR_ALIGN_CENTER_BLOCKS ;
		
		mNumScreencaps = 0 ;
		
		mSliceSequenceView.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// make a Bitmap, Canvas, and ConfigCanvas to match the
				// current view size.
				int w = mSliceSequenceView.getWidth() ;
				int h = mSliceSequenceView.getHeight() ;
				Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) ;
				Canvas canvas = new Canvas(bitmap) ;
				BlockDrawerConfigCanvas configCanvas = new BlockDrawerConfigCanvas( 0, 0, w, h ) ;
				configCanvas.setScale(
						BlockDrawerConfigCanvas.Scale.NONE) ;
				configCanvas.setAlignment(
						BlockDrawerConfigCanvas.Alignment.NONE) ;
				configCanvas.setBackground(
						BlockDrawerConfigCanvas.Background.CLEAR) ;
				
				// pass the canvas / config to the view to capture a screencap
				mSliceSequenceView.redrawFrameTo(canvas, configCanvas) ;
				
				// pass the bitmap to a function to save it to shared storage.
				String filename = mSkin.getGame() + "_" + mSkin.getTemplate() + "_" + mSkin.getColor() ;
				filename = filename + "_" + mNumScreencaps + ".png" ;
				if ( mSliceName != null )
					filename = mSliceName + "_" + filename ;
				saveBitmapToExternalStorageAsPNG( bitmap, filename ) ;
			}
		}) ;
	}
	
	
	@Override
	public void onResume() {
		super.onResume() ;
		
		mSliceSequenceView.setContentSizeInBlocks(mC, mR) ;
		if ( mSliceSequence != null )
			mSliceSequenceView.setSequence(mDrawSettings, mSliceSequence) ;
		else
			mSliceSequenceView.setContent(mDrawSettings, mSlice) ;
	}
	
	
	private boolean saveBitmapToExternalStorageAsPNG( Bitmap bitmap, String filename ) {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			Toast.makeText(this, "Media is read-only", Toast.LENGTH_SHORT).show() ;
		    return false ;
		} else if ( !Environment.MEDIA_MOUNTED.equals(state) ) {
			Toast.makeText(this, "Something wrong with media", Toast.LENGTH_SHORT).show() ;
			return false ;
		}
		
		File externalRoot = Environment.getExternalStorageDirectory() ;
		// this is the EXTERNAL STORAGE root.  Navigate
		// to /Android/data/<package name>/files/
		File dataDir = new File( new File( 
				externalRoot,
					"Android"),
						"data") ;
		
		File packageDir =
			new File( dataDir, getApplicationContext().getPackageName() ) ;
		File screencapDir = new File( new File( packageDir, "files" ), "screencaps" ) ;
		if ( screencapDir.exists() && !screencapDir.isDirectory() ) {
			Toast.makeText(this, "screencap directory is not a dir: " + screencapDir.getAbsolutePath(), Toast.LENGTH_SHORT).show() ;
			return false ;
		} else if ( !screencapDir.exists() ) {
			if ( !screencapDir.mkdirs() ) {
				Toast.makeText(this, "can't make screencap directory: " + screencapDir.getAbsolutePath(), Toast.LENGTH_SHORT).show() ;
				return false ;
			}
		}
		
		// directory exists.  Filename?
		File file = new File( screencapDir, filename ) ;
		
		boolean overwritten = file.exists() ;
		
		// get the right format.
		Bitmap.CompressFormat compressFormat ;
		compressFormat = Bitmap.CompressFormat.PNG ;
		
		// write
		try {
			FileOutputStream out = new FileOutputStream(file) ;
			bitmap.compress(compressFormat, 90, out) ;
			out.flush() ;
			out.close() ;
			new SingleMediaScanner(this, file.getAbsolutePath()) ;
			
			mNumScreencaps++ ;
		} catch( Exception e ) {
			Toast.makeText(this, "Exception thrown; check log", Toast.LENGTH_SHORT).show() ;
			e.printStackTrace() ;
			return false ;
		}
		
		if ( overwritten )
			Toast.makeText(this, "Screencap overwritten as " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show() ;
		else
			Toast.makeText(this, "Screencap saved as " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show() ;
		
		// done
		return true ;
	}
	
	private class SingleMediaScanner implements MediaScannerConnectionClient 
    { 
        private MediaScannerConnection mMs; 
        private String mPath; 
        SingleMediaScanner(Context context, String f) 
        { 
            mPath = f; 
            mMs = new MediaScannerConnection(context, this); 
            mMs.connect(); 
        } 
        @Override 
        public void onMediaScannerConnected() 
        { 
            mMs.scanFile(mPath, null); 
        } 
        @Override 
        public void onScanCompleted(String path, Uri uri) 
        { 
            mMs.disconnect(); 
        } 
    }
	
}
