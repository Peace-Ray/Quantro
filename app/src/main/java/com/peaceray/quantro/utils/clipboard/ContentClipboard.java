package com.peaceray.quantro.utils.clipboard;

import android.content.ClipData;
import android.content.Context;

class ContentClipboard {

	private android.content.ClipboardManager clipboardManager ;
	
	ContentClipboard( Context context ) {
		clipboardManager = (android.content.ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE) ;
	}
	
	void setText( String label, String text ) {
		ClipData clipData = ClipData.newPlainText( label, text ) ;
		clipboardManager.setPrimaryClip(clipData) ;
	}
	
}
