package com.peaceray.quantro.utils.clipboard;

import android.content.Context;

import com.peaceray.quantro.utils.VersionCapabilities;


/**
 * Android clipboards changed in version 11, from a simple text-holding
 * clipboard to one that retains "ClipData" objects.
 * 
 * This class provides a static method for version-safe copying of
 * Strings.  The method 'setText' will store to the provided label and
 * text to the primaryClip if advanced clipping (i.e. android.content.ClipboardManager)
 * is available.  Otherwise, the label is discarded and the text is
 * stored to android.text.ClipboardManager.
 * 
 * @author Jake
 *
 */
public class Clipboard {

	
	/**
	 * If API >= 11, stores this label / text combination as the primary
	 * clip in android.content.ClipboardManager.  Otherwise, stores the
	 * text only (label is discarded) as the clip in android.text.ClipboardManager.
	 * @param context
	 * @param label
	 * @param text
	 */
	public static void setText( Context context, String label, String text ) {
		if ( VersionCapabilities.supportsContentClipboardManager() )
			new ContentClipboard(context).setText(label, text) ;
		else
			new Clipboard(context).setText(text) ;
	}
	
	
	@SuppressWarnings("deprecation")
	private android.text.ClipboardManager clipboardManager ;
	
	@SuppressWarnings("deprecation")
	private Clipboard( Context context ) {
		clipboardManager = (android.text.ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE) ;
	}
	
	@SuppressWarnings("deprecation")
	private void setText( String text ) {
		clipboardManager.setText(text) ;
	}
	
}
