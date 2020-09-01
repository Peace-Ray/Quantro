package com.peaceray.quantro.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;

import com.peaceray.quantro.QuantroPreferences;
import com.peaceray.quantro.R;
import com.peaceray.quantro.utils.AppVersion;
import com.peaceray.quantro.view.dialog.AlertDialog;
import com.peaceray.quantro.view.dialog.WebViewDialog;

public class GlobalDialog {
	
	
	/**
	 * Users of GlobalDialog must implement this interface, or provide
	 * an object which does.  At a minimum, getContext() must return
	 * non-null.  It is perfectly OK if it returns the same instance
	 * -- for example, Applications using this class can implement
	 * the function as 
	 * 
	 * public Context getContext() { return this ; }
	 * 
	 * getHelpDialogHTMLRelativePath() is only necessary if DIALOG_ID_HELP
	 * is called.  Returning 'null' to this is an error, but only if
	 * DIALOG_ID_HELP is used.
	 * 
	 * @author Jake
	 *
	 */
	public interface DialogContext {
		
		/**
		 * Provide a Context suitable for constructing new Dialog objects.
		 * Most users will be Activities and should thus implement this function
		 * as 'return this'.
		 * @return
		 */
		public Context getContext() ;
		
		/**
		 * Provide a path, relative to file:///android_asset/html/,
		 * to this Context's "Help" html file.  Returning 'null' is
		 * acceptable, but only if DIALOG_ID_HELP is never provided as
		 * the dialog id.
		 * @return
		 */
		public String getHelpDialogHTMLRelativePath() ;
		
		
		/**
		 * Provide a name for the Help context to display in the
		 * "Help" dialog's title.  Returning 'null' is acceptable, but
		 * only if DIALOG_ID_HELP is never provided as the dialog ID.
		 * @return
		 */
		public String getHelpDialogContextName() ;
		
	}
	
	public static final int DIALOG_ID_WHATS_NEW = 10000 ;
	public static final int DIALOG_ID_HELP = 10002 ;
	
	private static final int [] DIALOGS = new int[] {
		DIALOG_ID_WHATS_NEW,
		DIALOG_ID_HELP
	} ;
	
	public static final String PREFERENCE_LAST_WHATS_NEW = "com.peaceray.quantro.dialog.GlobalDialog.PREFERENCE_LAST_WHATS_NEW" ;
	public static final String PREFERENCE_HAS_SHOWN_FIRST_TIME_PREMIUM_UNLOCK = "com.peaceray.quantro.dialog.GlobalDialog.PREFERENCE_HAS_SHOWN_FIRST_TIME_PREMIUM_UNLOCK" ;
	
	
	public static boolean hasDialog( int dialogID ) {
		for ( int i = 0; i < DIALOGS.length; i++ ) {
			if ( DIALOGS[i] == dialogID ) {
				return true ;
			}
		}
		return false ;
	}
	
	public static Dialog onCreateDialog( DialogContext dc, int dialogID, DialogManager dialogManager ) {
		AlertDialog.Builder adBuilder ;
		WebViewDialog.Builder wvBuilder ;
		String url ;
		
		String title ;
		
		Context context = dc.getContext() ;
		Resources res = context.getResources() ;
		
		AutonomousDismissingListener adListener ;
		
		switch( dialogID ) {
		case DIALOG_ID_WHATS_NEW:
			WhatsNewListener listener = new WhatsNewListener( context, dialogManager ) ;
			wvBuilder = new WebViewDialog.Builder(context) ;
			int titleID ;
			if ( AppVersion.isAlpha(context) )
				titleID = R.string.global_whats_new_dialog_title_alpha ;
			else if ( AppVersion.isBeta(context) )
				titleID = R.string.global_whats_new_dialog_title_beta ;
			else if ( AppVersion.isReleaseCandidate(context) )
				titleID = R.string.global_whats_new_dialog_title_release_candidate ;
			else
				titleID = R.string.global_whats_new_dialog_title ;
    		wvBuilder.setTitle(titleID) ;
    		url = "file:///android_asset/html/whats_new/"
    				+ "version_" + AppVersion.code(context) + "_" + AppVersion.name(context)
    				+ ".html" ;
    		wvBuilder.setURL(url) ;
    		wvBuilder.setCancelable(true) ;
    		wvBuilder.setNegativeButton(
    				R.string.global_whats_new_dialog_button_ok,
    				listener) ;
    		wvBuilder.setOnCancelListener(listener) ;
    		return wvBuilder.create() ;
    		
    	
		case DIALOG_ID_HELP:
			title = res.getString(R.string.global_help_dialog_title) ;
			title = title.replace(
					res.getString(R.string.placeholder_global_dialog_context_name),
					dc.getHelpDialogContextName()) ;
			url = "file:///android_asset/html/" + dc.getHelpDialogHTMLRelativePath() ;
			adListener = new AutonomousDismissingListener( context, dialogID, dialogManager ) ;
			wvBuilder = new WebViewDialog.Builder(context) ;
    		wvBuilder.setTitle(title) ;
    		
    		wvBuilder.setURL(url) ;
    		wvBuilder.setCancelable(true) ;
    		wvBuilder.setNegativeButton(
    				R.string.global_help_dialog_button_ok,
    				adListener) ;
    		wvBuilder.setOnCancelListener(adListener) ;
    		return wvBuilder.create() ;
		}
		
		return null ;
	}
	
	public static final void onPrepareDialog( DialogContext dc, int id, Dialog dialog ) {
		// nothing to do
	}
	
	
	/**
	 * An object we can instantiate to hold context and dialog manager
	 * references (since our onCreateDialog is static, and they are passed in 
	 * as object parameters).
	 * 
	 * @author Jake
	 *
	 */
	static abstract class AutonomousListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
		
		Context mContext ;
		DialogManager mDialogManager ;
		
		public AutonomousListener( Context c, DialogManager dm ) {
			mContext = c ;
			mDialogManager = dm ;
		}
		
		protected Context getContext() {
			return mContext ;
		}
		
		protected DialogManager getDialogManager() {
			return mDialogManager ;
		}
	}
	
	
	/**
	 * A GlobalListener implementation that implements two pieces of
	 * functionality:
	 * 
	 * 1. Auto-dismisses the associated dialog upon button press and cancel.
	 * 2. Allows hot-swappable "onClick" and "onCancel" supplements that
	 * 		perform additional functionality (besides dismissing the
	 * 		associated dialog).
	 * 
	 * @author Jake
	 *
	 */
	static class AutonomousDismissingListener extends AutonomousListener {
		
		public interface OnClickListener {
			public abstract void onClick(Context c, DialogInterface dialog, int which) ;
		}
		
		public interface OnCancelListener {
			public abstract void onCancel(Context c, DialogInterface dialog) ;
		}
		
		
		
		int mDialogID; 
		
		OnClickListener [] mOnClickListener ;
		OnCancelListener mOnCancelListener = null ;
		
		public AutonomousDismissingListener( Context c, int dialogID, DialogManager dm ) {
			super( c, dm ) ;
			mDialogID = dialogID ;
			
			mOnClickListener = new OnClickListener[3] ;
		}
		
		
		private int buttonNumToIndex( int buttonNum ) {
			switch( buttonNum ) {
			case DialogInterface.BUTTON_POSITIVE:
				return 0 ;
			case DialogInterface.BUTTON_NEUTRAL:
				return 1 ;
			case DialogInterface.BUTTON_NEGATIVE:
				return 2 ;
			}
			return -1 ;
		}
		
		// MUTATE: Set listener supplements.
		public AutonomousDismissingListener setOnClickListener(
				int buttonNum,
				OnClickListener listener ) {
			mOnClickListener[buttonNumToIndex(buttonNum)] = listener ;
			return this ;
		}
		
		public AutonomousDismissingListener setOnClickListenerAllButtons(
				OnClickListener listener ) {
			for ( int i = 0; i < mOnClickListener.length; i++ )
				mOnClickListener[i] = listener ;
			return this ;
		}
		
		public AutonomousDismissingListener setOnClickListenerPositiveButton(
				OnClickListener listener ) {
			return setOnClickListener( DialogInterface.BUTTON_POSITIVE, listener ) ;
		}
		
		public AutonomousDismissingListener setOnClickListenerNeutralButton(
				OnClickListener listener ) {
			return setOnClickListener( DialogInterface.BUTTON_NEUTRAL, listener ) ;
		}
		
		public AutonomousDismissingListener setOnClickListenerNegativeButton(
				OnClickListener listener ) {
			return setOnClickListener( DialogInterface.BUTTON_NEGATIVE, listener ) ;
		}
		
		public AutonomousDismissingListener setOnCancelListener(
				OnCancelListener listener ) {
			mOnCancelListener = listener ;
			return this ;
		}

		
	
		
		// Automate: the actual listeners.  Dismiss the dialog and do
		// whatever hot-swapped function is currently in place (if any).
		

		@Override
		public void onClick(DialogInterface dialog, int which) {
			getDialogManager().dismissDialog(mDialogID) ;
			int buttonIndex = buttonNumToIndex( which ) ;
			if ( mOnClickListener[buttonIndex] != null )
				mOnClickListener[buttonIndex].onClick(getContext(), dialog, which) ;
		}


		@Override
		public void onCancel(DialogInterface dialog) {
			getDialogManager().dismissDialog(mDialogID) ;
			if ( mOnCancelListener != null )
				mOnCancelListener.onCancel(getContext(), dialog) ;
		}
	
		
	}
	

	static final class WhatsNewListener extends AutonomousDismissingListener {
		public WhatsNewListener( Context c, DialogManager dm ) {
			super( c, DIALOG_ID_WHATS_NEW, dm ) ;
			
			// set our on-cancel and on-click.  Dialog dismissal happens
			// automatically.
			this.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(Context context, DialogInterface dialog) {
					QuantroPreferences.setPrivateSettingInt(
							context,
							PREFERENCE_LAST_WHATS_NEW,
							AppVersion.code(mContext)) ;
				}
			}) ;
			
			this.setOnClickListenerAllButtons(new OnClickListener() {
				@Override
				public void onClick(Context context, DialogInterface dialog, int which) {
					QuantroPreferences.setPrivateSettingInt(
							context,
							PREFERENCE_LAST_WHATS_NEW,
							AppVersion.code(mContext)) ;
				}
			}) ;
		}
	}
}
