package com.peaceray.quantro.dialog;

import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.Hashtable;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Shows dialogs, specified by a dialog_id, and constructed / adapted by
 * a particular Activity.
 * 
 * This class simplifies the process of displaying dialogs, especially "tiered" dialogs
 * such that (e.g.) dialog B should replace dialog A entirely, but dialog C should appear
 * "on top of" A or B, and when dismissed, A or B should return.
 * 
 * An Activity which intends to show dialogs should instantiate a single DialogManager,
 * providing itself as a parameter to the constructor.  The constructor also requires
 * that the dialogIDs to be displayed be formally established in "tiers", with higher
 * tiers superceding lower tiers.  The DialogManager maintains a list of the currently
 * displayed Dialog at each tier; if a top-tier dialog is dismissed, the next-highest-tier
 * dialog is displayed in its place automatically.
 * 
 * If you have no need for "covering" dialogs, and want all dialogs to display at the same
 * level of priority (each replacing the last) then instantiate a manager with a single tier
 * of dialog IDs.
 * 
 * Usage:
 * 
 * ALL dialogs should be shown and dismissed through the Manager.  Use:
 * 
 * showDialog( dialogID )
 * 		to establish that dialog as the appropriate display for its tier.
 * 		Will dismiss the current dialog displayed at this tier, if one exists.
 * 
 * dismissDialog( dialogID )
 * 		to dismiss the specified dialog
 * 
 * dismissDialog()
 * 		will dismiss the "top" dialog at the time the statement is executed.  Because all
 * 		dialog operations are run in the UIThread, there is no guarantee that the dialog
 * 		"on top" 
 * 
 * dismissDialogAtTier( tier )
 * 		to dismiss the current dialog displayed at the specified tier
 * 
 * dismissAllDialogs()
 * 		to dismiss all currently displayed dialogs.
 * 
 * For "self-closing" dialogs, e.g. anything with buttons that cause the dialog to
 * 		disappear, one should ensure that dialogManager.dismissDialog( this.id ) 
 * 		occurs in its onCancel and onClick methods. 
 * 
 * All operations will be performed in the UI thread of the provided Activity, using
 * runOnUIThread.
 * 
 * NEW FUNCTIONALITY 11/7/11: hideDialogs, revealDialogs.
 * 
 * Previously, DialogManager allowed "tiered" dialogs, which automatically replaced
 * each other, with the topmost dialog being displayed.  However, it offered no
 * sophisticated functionality w.r.t. removing ALL dialog tiers, and then redisplaying
 * them in their previous state, let alone CHANGING the dialogs while they are off-screen,
 * then redisplaying them in their NEW state.
 * 
 * hideDialog and revealDialog allow this.  DialogManagers are instantiated in a "revealing"
 * state, so by default you can show and dismiss dialog however you want.  hideDialog() will
 * retain the current list of all displayed dialogs, but will remove them from the screen.
 * From that point on the showDialog and dismiss*() methods will update the current list of
 * the displayed without showing anything to the screen.  Finally, calling "revealDialog" will
 * show whatever dialog is appropriate (if any) based on all show/dismiss calls that have occurred.
 * 
 * REQUIREMENTS: The only conditions placed on dialogIDs is that each dialogID must belong
 * to exactly one tier (any dialogID not specified at construction is assumed to belong to tier 0),
 * and that all dialogIDs used are non-negative.
 * 
 * @author Jake
 *
 */
public class DialogManager implements Parcelable {

	WeakReference<Activity> mwrActivity ;
	
	Hashtable<Integer,Integer> dialogIDToTier ;
	int [] displayedDialogIDAtTier ;
	
	boolean revealed ;
	
	/**
	 * Instantiates a new DialogManager for the specified Activity.
	 * The array provided, 'dialogIDsByTier', establishes that:
	 * 
	 * 		There are dialogIDsByTier.length tiers of dialogs.
	 * 		There are dialogIDsByTier[i].length unique dialogIDs in tier 'i'
	 * 		dialogIDsByTier[i][j] is the j'th unique dialogID belonging to tier 'i'.
	 * 
	 * As long as the appropriate DialogManager methods are used, displays
	 * of dialogs "on top of" each other will be taken care of by matching
	 * their dialogIDs to the appropriate tier.  Any dialog ID which is
	 * displayed or dismissed that did NOT appear in the tier array is assumed
	 * to be tier 0 - lowest possible tier.
	 * 
	 * Preconditions:
	 * 			No dialogID appears more than once in dialogIDsByTier.
	 * 			Only positive dialogIDs are used.
	 * 
	 * @param activity
	 * @param dialogIDsByTier
	 */
	public DialogManager( Activity activity, int [][] dialogIDsByTier ) throws IllegalArgumentException {
		this.mwrActivity = new WeakReference<Activity>(activity) ;
		this.revealed = true ;
		
		dialogIDToTier = new Hashtable<Integer,Integer>() ;
		if ( dialogIDsByTier != null ) {
			for ( int i = 0; i < dialogIDsByTier.length; i++ ) {
				Integer tier = new Integer(i) ;
				for ( int j = 0; j < dialogIDsByTier[i].length; j++ ) {
					int dID = dialogIDsByTier[i][j] ;
					if ( dID < 0 )
						throw new IllegalArgumentException("Provided dialog ID " + dID + " is negative!") ;
					
					Integer dIDKey = new Integer(dID) ;
					if ( dialogIDToTier.containsKey(dIDKey) )
						throw new IllegalArgumentException("DialogID " + dID + " appears more than once in dialogIDsByTier.") ;
					
					dialogIDToTier.put(dIDKey, tier) ;
				}
			}
			displayedDialogIDAtTier = new int[dialogIDsByTier.length] ;
		}
		else
			displayedDialogIDAtTier = new int[1] ;
		
		for ( int i = 0; i < displayedDialogIDAtTier.length; i++ )
			displayedDialogIDAtTier[i] = -1 ;
	}
	
	
	public DialogManager( Activity activity ) {
		this(activity, null) ;
	}
	
	public void setActivity( Activity activity ) {
		this.mwrActivity = new WeakReference<Activity>(activity) ;
	}
	
	
	public boolean isShowingDialog() {
		for ( int i = 0; i < displayedDialogIDAtTier.length; i++ )
			if ( displayedDialogIDAtTier[i] >= 0 )
				return true ;
		return false ;
	}
	
	
	/**
	 * Shows the specified dialogID on the UIThread, replacing the dialogID
	 * at its tier.  Will only actually call mwrActivity.showDialog() if that tier
	 * is the top currently displayed.
	 * 
	 * However, this call will ensure that the specified dialog will be displayed
	 * if, at some point in the future, it becomes top-tier.
	 * 
	 * The tier of the dialog is determined according to the constructor parameters.
	 * If the dialogID did not appear there, it is assumed to be tier 0 - lowest priority.
	 * 
	 * @param dialogID The dialog to show.  Must be >= 0.
	 */
	public void showDialog( int dialogID ) {
		if ( dialogID < 0 )
			throw new IllegalArgumentException("Dialog ID must be non-negative") ;
		
		// determine tier...
		int tier = 0 ;
		Integer dIDObj = new Integer( dialogID ) ;
		if ( dialogIDToTier.containsKey( dIDObj ) )
			tier = dialogIDToTier.get( dIDObj ).intValue() ;
		
		Activity activity = mwrActivity.get() ;
		if ( activity != null )
			activity.runOnUiThread( new DialogShower( dialogID, tier ) ) ;
	}

	
	/**
	 * Dismisses (on the UIThread) the specified dialog.  If this dialog is
	 * not currently displayed - i.e., if it was not the most recently displayed
	 * dialog at its tier or if it was already dismissed - this method call has
	 * no effect.
	 * 
	 * @param dialogID The dialog to dismiss.  Must be >= 0.
	 */
	public void dismissDialog( int dialogID ) {
		if ( dialogID < 0 )
			throw new IllegalArgumentException("Dialog ID must be non-negative") ;
		
		// determine tier...
		int tier = 0 ;
		Integer dIDObj = new Integer( dialogID ) ;
		if ( dialogIDToTier.containsKey( dIDObj ) )
			tier = dialogIDToTier.get( dIDObj ).intValue() ;
		
		Activity activity = mwrActivity.get() ;
		if ( activity != null )
			activity.runOnUiThread( new DialogDismisser( dialogID, tier ) ) ;
	}
	
	
	/**
	 * Dismisses (on the UIThread) the current "top" dialog, i.e., the dialog
	 * which is currently shown to the user.  If any dialogs are waiting at
	 * lower tiers, the next highest among them will be displayed.
	 * 
	 * This method has no effect if no dialogs are displayed at the time the
	 * 'dismiss' runnable activates on the UIThread.
	 */
	public void dismissDialog() {
		Activity activity = mwrActivity.get() ;
		if ( activity != null )
			activity.runOnUiThread( new TopTierDialogDismisser() ) ;
	}
	
	
	/**
	 * Dismisses whatever dialog is currently displayed on the specified
	 * tier, if any, and - if that dialog was on top - replaces it with
	 * the next highest (if any).
	 * 
	 * PRECONDITION: 'tier' is a non-negative number not greater than 
	 * #tiers - 1.  There is always at least 1 tier, so tier=0 is safe.
	 * If additional tiers are desired they must be specified in the
	 * constructor.
	 * 
	 * @param tier
	 */
	public void dismissDialogAtTier( int tier ) {
		if ( tier < 0 )
			throw new IllegalArgumentException("Provided tier " + tier + " is negative!") ;
		if ( tier >= displayedDialogIDAtTier.length )
			throw new IllegalArgumentException("Provided tier " + tier + " is too big; only have " + displayedDialogIDAtTier.length + " 0-indexed tiers!") ;
		
		Activity activity = mwrActivity.get() ;
		if ( activity != null )
			activity.runOnUiThread( new TierDialogDismisser( tier ) ) ;
	}
	
	
	/**
	 * Dismisses ALL dialogs - the Dialog currently displayed (if any) and any
	 * dialogs at lower tiers waiting for display.
	 * 
	 * NOTE: This method is NOT equivalent to repeated calls to dismissDialog.
	 * With this method, the dialogs below the current top tier will never be displayed;
	 * with repeated calls to dismissDialog, each dialog will be displayed for a very
	 * short period of time before being dismissed.
	 */
	public void dismissAllDialogs() {
		Activity activity = mwrActivity.get() ;
		if ( activity != null )
			activity.runOnUiThread( new AllDialogDismisser() ) ;
	}
	
	
	
	/**
	 * Forgets the specified dialog.  This has no effect on any dialogs
	 * displayed and is only run on the UI thread for easy synchronization
	 * with other methods.
	 * 
	 * NOTE: It is good practice to "forget" a dialog in its OnDismissListener.
	 * Since the dialog is already being dismissed, it may produce an error
	 * to call "dismissDialog" at that time.
	 * 
	 * @param dialogID The dialog to dismiss.  Must be >= 0.
	 */
	public void forgetDialog( int dialogID ) {
		if ( dialogID < 0 )
			throw new IllegalArgumentException("Dialog ID must be non-negative") ;
		
		// determine tier...
		int tier = 0 ;
		Integer dIDObj = new Integer( dialogID ) ;
		if ( dialogIDToTier.containsKey( dIDObj ) )
			tier = dialogIDToTier.get( dIDObj ).intValue() ;
		
		Activity activity = mwrActivity.get() ;
		if ( activity != null )
			activity.runOnUiThread( new DialogForgetter( dialogID, tier ) ) ;
	}
	
	
	
	
	public void hideDialogs() {
		Activity activity = mwrActivity.get() ;
		if ( activity != null )
			activity.runOnUiThread( new DialogHider() ) ;
	}
	
	public void revealDialogs() {
		Activity activity = mwrActivity.get() ;
		if ( activity != null )
			activity.runOnUiThread( new DialogRevealer() ) ;
	}
	
	
	/**
	 * Shows the specified dialog ID at the specified tier.
	 * @author Jake
	 *
	 */
	private class DialogShower implements Runnable {
		
		int dialogID ;
		int tier ;
		
		public DialogShower( int dialogID, int tier ) {
			this.dialogID = dialogID ;
			this.tier = tier ;
		}
		
		/**
		 * The run method of DialogShower.  Ultimately, this will place the
		 * provided dialogID at the specified tier, replacing whatever dialog
		 * is currently there (if any).
		 */
		public void run() {
			// Determine the "top" dialog, and the one in the specified tier.  If
			// they are the same, dismiss it.
			int topDialogTier = -1 ;
			for ( int i = displayedDialogIDAtTier.length -1 ; i >= 0; i-- ) {
				if ( displayedDialogIDAtTier[i] >= 0 ) {
					topDialogTier = i ;
					break ;
				}
			}
			if ( topDialogTier >= 0 && topDialogTier <= tier && displayedDialogIDAtTier[tier] != dialogID && revealed ) {
				//System.err.println("DialogManager DialogShower: dismiss dialog " + displayedDialogIDAtTier[topDialogTier]) ;
				Activity activity = mwrActivity.get() ;
				if ( activity != null )
					activity.dismissDialog(displayedDialogIDAtTier[topDialogTier]) ;
			}
			// Note that 'tier' must be non-negative.
			
			// If we are currently showing this dialog, don't do anything.
			if ( displayedDialogIDAtTier[tier] == dialogID )
				return ;
			
			displayedDialogIDAtTier[tier] = dialogID ;
			
			// Display the dialog IF it is currently the top.
			if ( topDialogTier <= tier && revealed ) {
				//System.err.println("DialogManager DialogShower: show dialog " + dialogID) ;
				Activity activity = mwrActivity.get() ;
				if ( activity != null )
					activity.showDialog(dialogID) ;
			}
			
			// That's it!
		}
	}
	
	
	private class DialogDismisser implements Runnable {
		int dialogID ;
		int tier ;
		
		public DialogDismisser( int dialogID, int tier ) {
			this.dialogID = dialogID ;
			this.tier = tier ;
		}
		
		/**
		 * The run method of DialogDismisser.  Dismisses the specified
		 * dialog, if it is currently displayed at the specified tier,
		 * and replaces it with the next-higher (if any).
		 */
		public void run() {
			// Determine the "top" dialog.  If the specified dialogID is the
			// top dialog, dismiss it.
			int topDialogTier = -1 ;
			for ( int i = displayedDialogIDAtTier.length -1 ; i >= 0; i-- ) {
				if ( displayedDialogIDAtTier[i] >= 0 ) {
					topDialogTier = i ;
					break ;
				}
			}
			
			try {
				Activity activity = mwrActivity.get() ;
				if ( activity != null )
					activity.dismissDialog(dialogID) ;
			} catch( IllegalArgumentException iae ) {
				// whelp...
			}
			// Note that 'tier' must be non-negative.
			
			// Only change the displayed dialog if, at this moment, we are displaying
			// the specified dialog.  Otherwise nothing will change.
			if ( displayedDialogIDAtTier[tier] == dialogID ) {
				displayedDialogIDAtTier[tier] = -1 ;
				
				// Show the "next lowest" dialog!
				for ( int i = tier-1 ; i >= 0; i-- ) {
					if ( displayedDialogIDAtTier[i] >= 0 ) {
						if ( revealed ) {
							//System.err.println("DialogManager DialogDismisser: show dialog " + displayedDialogIDAtTier[i]) ;
							Activity activity = mwrActivity.get() ;
							if ( activity != null )
								activity.showDialog( displayedDialogIDAtTier[i] ) ;
						}
						break ;
					}
				}
			}
			
			// That's it!
		}
	}
	
	private class TopTierDialogDismisser implements Runnable {
		
		public TopTierDialogDismisser() {
			
		}
		
		/**
		 * The run method of TopTierDialogDismisser.  Will dismiss
		 */
		public void run() {
			// Determine the "top" dialog.
			int topDialogTier = -1 ;
			for ( int i = displayedDialogIDAtTier.length -1 ; i >= 0; i-- ) {
				if ( displayedDialogIDAtTier[i] >= 0 ) {
					topDialogTier = i ;
					break ;
				}
			}
			
			if ( topDialogTier >= 0 ) {
				// Dismiss this dialog, and show the next highest.
				if ( revealed ) {
					//System.err.println("DialogManager TopTierDialogDismisser: dismiss dialog " + displayedDialogIDAtTier[topDialogTier]) ;
					Activity activity = mwrActivity.get() ;
					if ( activity != null )
						activity.dismissDialog(displayedDialogIDAtTier[topDialogTier]) ;
				}
				
				for ( int i = topDialogTier-1 ; i >= 0; i-- ) {
					if ( displayedDialogIDAtTier[i] >= 0 ) {
						if ( revealed ) {
							//System.err.println("DialogManager TopTierDialogDismisser: show dialog " + displayedDialogIDAtTier[i]) ;
							Activity activity = mwrActivity.get() ;
							if ( activity != null )
								activity.showDialog( displayedDialogIDAtTier[i] ) ;
						}
						break ;
					}
				}
			}
		}
	}
	
	
	
	private class TierDialogDismisser implements Runnable {
		int tier ;
		
		public TierDialogDismisser( int tier ) {
			this.tier = tier ;
		}
		
		/**
		 * The run method of TierDialogDismisser.  Dismisses whatever dialog
		 * is shown at the specified tier
		 */
		public void run() {
			// Determine the "top" dialog.  If the specified tier is the
			// top dialog tier, dismiss it.
			int topDialogTier = -1 ;
			for ( int i = displayedDialogIDAtTier.length -1 ; i >= 0; i-- ) {
				if ( displayedDialogIDAtTier[i] >= 0 ) {
					topDialogTier = i ;
					break ;
				}
			}
			//System.err.println("DialogManager: TierDialogDismisser.  Top tier is " + topDialogTier + " showing dialog " + (topDialogTier < 0  ? "none" : displayedDialogIDAtTier[topDialogTier]) + " dismissing tier " + tier + " which has dialog " + displayedDialogIDAtTier[tier]) ;
			if ( topDialogTier == tier && revealed ) {
				//System.err.println("DialogManager TierDialogDismisser: dismiss dialog " + displayedDialogIDAtTier[tier]) ;
				Activity activity = mwrActivity.get() ;
				if ( activity != null )
					activity.dismissDialog(displayedDialogIDAtTier[tier]) ;
			}
			// Note that 'tier' must be non-negative.
			
			// Set the displayed dialog at that tier to -1.
			displayedDialogIDAtTier[tier] = -1 ;
			
			// Finally, show the next-highest... IF there is one, and IF that
			// was the top dialog we dismissed.
			if ( topDialogTier == tier ) {
				// Show the "next lowest" dialog!
				for ( int i = tier-1 ; i >= 0; i-- ) {
					if ( displayedDialogIDAtTier[i] >= 0 ) {
						if ( revealed ) {
							//System.err.println("DialogManager TierDialogDismisser: show dialog " + displayedDialogIDAtTier[i]) ;
							Activity activity = mwrActivity.get() ;
							if ( activity != null )
								activity.showDialog( displayedDialogIDAtTier[i] ) ;
						}
						break ;
					}
				}
			}
			
			// That's it!
		}
	}
	
	
	
	private class AllDialogDismisser implements Runnable {
		
		public AllDialogDismisser( ) {
			
		}
		
		/**
		 * The run method of AllDialogDismisser.  Dismisses all dialogs shown.
		 */
		public void run() {
			boolean hasDismissedShown = false ;
			for ( int i = displayedDialogIDAtTier.length -1 ; i >= 0; i-- ) {
				// Dismiss if we haven't before...
				if ( displayedDialogIDAtTier[i] >= 0 && !hasDismissedShown ) {
					if ( revealed ) {
						//System.err.println("DialogManager AllDialogDismisser: dismiss dialog " + displayedDialogIDAtTier[i]) ;
						Activity activity = mwrActivity.get() ;
						if ( activity != null )
							activity.dismissDialog( displayedDialogIDAtTier[i] ) ;
					}
					hasDismissedShown = true ;
				}
				displayedDialogIDAtTier[i] = -1 ;
			}
			// That's it!
		}
	}
	
	private class DialogForgetter implements Runnable {
		int dialogID ;
		int tier ;
		
		public DialogForgetter( int dialogID, int tier ) {
			this.dialogID = dialogID ;
			this.tier = tier ;
		}
		
		/**
		 * The run method of DialogDismisser.  Dismisses the specified
		 * dialog, if it is currently displayed at the specified tier,
		 * and replaces it with the next-higher (if any).
		 */
		public void run() {
			// Only change the displayed dialog if, at this moment, we are displaying
			// the specified dialog.  Otherwise nothing will change.
			if ( displayedDialogIDAtTier[tier] == dialogID )
				displayedDialogIDAtTier[tier] = -1 ;
			// That's it!
		}
	}
	
	
	/**
	 * Puts the DialogManager in a state of hiding all dialogs.
	 * 
	 * @author Jake
	 *
	 */
	private class DialogHider implements Runnable {
		public DialogHider() {
			
		}
		
		public void run() {
			// Set revealed to false, and if that changed from its current state,
			// dismiss the top-shown dialog.
			boolean wasRevealed = revealed ;
			revealed = false ;
			if ( wasRevealed ) {
				for ( int i = displayedDialogIDAtTier.length - 1 ; i >= 0; i-- ) {
					if ( displayedDialogIDAtTier[i] >= 0 ) {
						if ( wasRevealed ) {
							//System.err.println("DialogManager DialogHider: dismiss dialog " + displayedDialogIDAtTier[i]) ;
							try {
								Activity activity = mwrActivity.get() ;
								if ( activity != null )
									activity.dismissDialog( displayedDialogIDAtTier[i] ) ;
							} catch( IllegalArgumentException iae ) {
								// nothing
							}
						}
						break ;
					}
				}
			}
		}
	}
	
	private class DialogRevealer implements Runnable {
		public DialogRevealer() {
			
		}
		
		public void run() {
			// Set revealed to true, and if that changed from its current state,
			// show the top-shown dialog.
			boolean wasRevealed = revealed ;
			revealed = true ;
			if ( !wasRevealed ) {
				for ( int i = displayedDialogIDAtTier.length - 1 ; i >= 0; i-- ) {
					if ( displayedDialogIDAtTier[i] >= 0 ) {
						if ( !wasRevealed ) {
							//System.err.println("DialogManager DialogRevealer: show dialog " + displayedDialogIDAtTier[i]) ;
							Activity activity = mwrActivity.get() ;
							if ( activity != null )
								activity.showDialog( displayedDialogIDAtTier[i] ) ;
						}
						break ;
					}
				}
			}
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}


	@Override
	public void writeToParcel(Parcel out, int arg1) {
		Enumeration<Integer> e = dialogIDToTier.keys() ;
		
		out.writeInt(dialogIDToTier.size()) ;
		while ( e.hasMoreElements() ) {
			Integer i = e.nextElement() ;
			out.writeInt(i.intValue()) ;
			out.writeInt(dialogIDToTier.get(i).intValue()) ;
		}
		
		out.writeInt(displayedDialogIDAtTier.length) ;
		out.writeIntArray(displayedDialogIDAtTier) ;
		out.writeInt(revealed ? 1 : 0) ;
	}
	
	public static final Parcelable.Creator<DialogManager> CREATOR
		    = new Parcelable.Creator<DialogManager>() {
		public DialogManager createFromParcel(Parcel in) {
		    return new DialogManager(in);
		}
		
		public DialogManager[] newArray(int size) {
		    return new DialogManager[size];
		}
	};
		
	private DialogManager(Parcel in) {
		dialogIDToTier = new Hashtable<Integer, Integer>() ;
		int num = in.readInt() ;
		for ( int i =0; i < num; i++ ) {
			int key = in.readInt() ;
			int val = in.readInt() ;
			dialogIDToTier.put(new Integer(key), new Integer(val)) ;
		}
		
		int len = in.readInt() ;
		displayedDialogIDAtTier = new int[len] ;
		in.readIntArray(displayedDialogIDAtTier) ;
		
		revealed = in.readInt() != 0 ;
	}

}
