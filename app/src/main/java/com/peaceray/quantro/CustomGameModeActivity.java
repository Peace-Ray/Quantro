package com.peaceray.quantro;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.peaceray.quantro.dialog.DialogManager;
import com.peaceray.quantro.model.modes.CustomGameModeSettings;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.view.button.strip.DialogButtonStrip;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.colors.GameModeColors;
import com.peaceray.quantro.view.dialog.AlertDialog;
import com.peaceray.quantro.view.generic.LabeledSeekBarAdapter;

public class CustomGameModeActivity extends QuantroActivity implements OnItemSelectedListener {
	
	private static final String TAG = "CustomGameModeActivity" ;
	
	////////////////////////////////////////////////////////////////////////////
	// INTENT STRINGS
	public static final String INTENT_EXTRA_CGMS			 = "com.peaceray.quantro.CustomGameModeActivity.INTENT_EXTRA_CGMS" ;
	public static final String INTENT_EXTRA_CGMS_ID			 = "com.peaceray.quantro.CustomGameModeActivity.INTENT_EXTRA_CGMS_ID" ;
	
	public static final String INTENT_EXTRA_HAS_SAVES		 = "com.peaceray.quantro.CustomGameModeActivity.INTENT_EXTRA_HAS_SAVES" ;
	
	
	// INTENT RESULT
	public static final String INTENT_RESULT_EXTRA_ACTION	= "com.peaceray.quantro.CustomGameModeActivity.INTENT_RESULT_EXTRA_ACTION" ;
	public static final int INTENT_RESULT_EXTRA_ACTION_SAVE = 	0x10 ;
	public static final int INTENT_RESULT_EXTRA_ACTION_DELETE = 0x11 ;
	// We don't handle the actual database or storage operations ourselves.  All we do is
	// pass back a result - either Cancel or OK - and if OK, the action we wish to perform.
	//
	// In either case, we also include the CGMS as an extra.
	
	
	////////////////////////////////////////////////////////////////////////////
	// DIALOGS
	private static final int DIALOG_ID_SAVE_ERROR_NO_PIECES = 0 ;
	private static final int DIALOG_ID_SAVE_ERROR_NO_NAME_OR_SUMMARY = 1 ;
	private static final int DIALOG_ID_CONFIRM_SAVE_CHANGES = 2 ;
	private static final int DIALOG_ID_CONFIRM_DELETE = 3 ;
	
	// fake constants, loaded from resources
	private int DIALOG_INSET ;
	private int DEFAULT_Q_PANES ;
	private int MIN_ROWS, MAX_ROWS ;
	private int MIN_COLS, MAX_COLS ;
	private int DEFAULT_ROWS, DEFAULT_COLS ;
	private boolean DEFAULT_HAS_TROMINOES, DEFAULT_HAS_TETROMINOES, DEFAULT_HAS_PENTOMINOES ;
	private boolean DEFAULT_HAS_ROTATION, DEFAULT_HAS_REFLECTION ;
	private int MAX_NAME_LENGTH ;
	private int MAX_SUMMARY_LENGTH ;
	private int MAX_DESCRIPTION_LENGTH ;
	
	
	
	private DialogManager mDialogManager ;
	
	private QuantroSoundPool mQuantroSoundPool ;
	private boolean mSoundControls ;
	private ColorScheme mColorScheme ;
	
	// we always need an ID.
	private int mID ;
	private CustomGameModeSettings mGivenCGMS ;
	private boolean mHasSaves ;
	
	
	// VIEW REFERENCES
	// checks
	private CheckBox mHasTrominoesCheckBox ;
	private CheckBox mHasTetrominoesCheckBox ;
	private CheckBox mHasPentominoesCheckBox ;
	// spinners
	private Spinner mControlsSpinner ;
	private ArrayAdapter<CharSequence> mControlsSpinnerAdapter ;
	private Spinner mBaseGameSpinner ;
	private ArrayAdapter<CharSequence> mBaseGameSpinnerAdapter ;
	// labeled seek bars
	private LabeledSeekBarAdapter mRowsLabeledSeekBarAdapter ;
	private LabeledSeekBarAdapter mColsLabeledSeekBarAdapter ;
	
	// texts
	private EditText mNameEditText ;
	private EditText mSummaryEditText ;
	private EditText mDescriptionEditText ;
	
	private TextView mActionText ;
	
	// buttons 	
	private DialogButtonStrip mDialogButtonStrip ;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupQuantroActivity( QUANTRO_ACTIVITY_UNKNOWN, QUANTRO_ACTIVITY_CONTENT_PERMANENT_DIALOG ) ;
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        
		// Set content view.  This takes 3 steps: load our generic "dialog" layout,
		// place our custom content within it, and set the title according to getTitle().
		setContentView(R.layout.dialog) ;
		ViewGroup content = (ViewGroup)findViewById(R.id.dialog_content) ;
		getLayoutInflater().inflate(R.layout.custom_game_mode_activity, content) ;
		
		// load our fake constants
		loadConstantsFromResources() ;
		
		// Set initial values for important stuff.
		mDialogManager = new DialogManager(this) ;
		mQuantroSoundPool = ((QuantroApplication)getApplication()).getSoundPool(this) ;
		mSoundControls = false ;
		mColorScheme = new ColorScheme(
				this,
				QuantroPreferences.getSkinQuantro(this).getColor(),
				QuantroPreferences.getSkinRetro(this).getColor() ) ;
		
		// VIEW REFERENCES
		// check boxes
		mHasTrominoesCheckBox = (CheckBox)findViewById(R.id.custom_game_mode_has_trominoes_checkbox) ;
		mHasTetrominoesCheckBox = (CheckBox)findViewById(R.id.custom_game_mode_has_tetrominoes_checkbox) ;
		mHasPentominoesCheckBox = (CheckBox)findViewById(R.id.custom_game_mode_has_pentominoes_checkbox) ;
		// spinners
		mBaseGameSpinner = (Spinner)findViewById(R.id.custom_game_mode_game_base) ;
		mControlsSpinner = (Spinner)findViewById(R.id.custom_game_mode_controls) ;
		// labeled seek bars
		mRowsLabeledSeekBarAdapter = new LabeledSeekBarAdapter(this, findViewById(R.id.custom_game_mode_rows_labeled_seekbar)) ;
		mColsLabeledSeekBarAdapter = new LabeledSeekBarAdapter(this, findViewById(R.id.custom_game_mode_cols_labeled_seekbar)) ;
		// text
		mNameEditText = (EditText)findViewById(R.id.custom_game_mode_name) ;
		mSummaryEditText = (EditText)findViewById(R.id.custom_game_mode_summary) ;
		mDescriptionEditText = (EditText)findViewById(R.id.custom_game_mode_description) ;
		mActionText = (TextView)findViewById(R.id.custom_game_mode_action_description) ;
		
		
		// SPINNER: Base Game.
		// set up spinner
		mBaseGameSpinnerAdapter = ArrayAdapter.createFromResource(this,
				R.array.custom_game_mode_base_array, android.R.layout.simple_spinner_item) ;
		mBaseGameSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) ;
		// apply the adapter
		mBaseGameSpinner.setAdapter(mBaseGameSpinnerAdapter) ;
		mBaseGameSpinner.setSelection( this.baseGameSpinnerValueToPosition(2) ) ;	// value: quantro
		mBaseGameSpinner.setOnItemSelectedListener(this) ;
		
		// SPINNER: Controls
		// set up spinner
		mControlsSpinnerAdapter = ArrayAdapter.createFromResource(this,
				R.array.custom_game_mode_controls_array, android.R.layout.simple_spinner_item) ;
		mControlsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) ;
		// apply the adapter
		mControlsSpinner.setAdapter(mControlsSpinnerAdapter) ;
		mControlsSpinner.setSelection( this.controlsSpinnerValueToPosition(true, false) ) ;
		
		// SET VALUES!  This is based on our provided CGMS or ID.
		Intent i = getIntent() ;
		setValuesFrom(i) ;
		
		// if we were given a CGMS, we need to disable the base-game spinner.
		if ( mGivenCGMS != null )
			mBaseGameSpinner.setEnabled( false ) ;
		
		// We don't really need to explicitly listen to text changes,
		// but we need to be able to disable stuff, and limit the lengths allowed.
		// We separate this into its own method.
		setEditTextFilters() ;
		
		    
		// Configure buttons
		int qpanes = this.baseGameSpinnerPositionToValue( mBaseGameSpinner.getSelectedItemPosition() ) ;
		Resources res = getResources() ;
		mDialogButtonStrip = (DialogButtonStrip)content.findViewById(R.id.dialog_content_button_strip) ;
		mDialogButtonStrip.setButton(
				Dialog.BUTTON_POSITIVE,
				res.getString( R.string.custom_game_mode_button_save ),
				GameModeColors.customPrimary(mColorScheme, qpanes),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if ( mSoundControls )
							mQuantroSoundPool.menuButtonClick() ;
						if ( mHasSaves && mGivenCGMS != null && !mGivenCGMS.isEquivalent(makeCGMSFromViews()) )
							mDialogManager.showDialog(DIALOG_ID_CONFIRM_SAVE_CHANGES) ;
						else
							saveMode() ;
					}
				}) ;
		mDialogButtonStrip.configureButton(Dialog.BUTTON_POSITIVE, true, true) ;
		if ( mGivenCGMS != null ) {
			mDialogButtonStrip.setButton(
					Dialog.BUTTON_NEUTRAL,
					res.getString( R.string.custom_game_mode_button_delete ),
					GameModeColors.customSecondary(mColorScheme, qpanes),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if ( mSoundControls )
								mQuantroSoundPool.menuButtonBack() ;
							if ( mHasSaves )
								mDialogManager.showDialog(DIALOG_ID_CONFIRM_DELETE) ;
							else
								deleteMode() ;
						}
					}) ;
			mDialogButtonStrip.configureButton(Dialog.BUTTON_NEUTRAL, true, true) ;
		} else
			mDialogButtonStrip.configureButton(Dialog.BUTTON_NEUTRAL, false, false) ;
		mDialogButtonStrip.setButton(
				Dialog.BUTTON_NEGATIVE,
				res.getString( R.string.custom_game_mode_button_cancel ),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if ( mSoundControls )
							mQuantroSoundPool.menuButtonBack() ;
						cancel() ;
					}
				}) ;
		mDialogButtonStrip.configureButton(Dialog.BUTTON_NEGATIVE, true, true) ;
		
		mDialogButtonStrip.refresh() ;
		
        ////////////////////////////////////////////////////////////////////////
		// DISABLE ANDROID BEAM
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		if ( nfcAdapter != null ) {
			nfcAdapter.setNdefPushMessage(null, this) ;
		}
	}
	
	
	@Override
	protected void onResume() {
		super.onResume() ;
		
		mSoundControls = QuantroPreferences.getSoundControls(this) ;
		mDialogManager.revealDialogs() ;
	}
	
	@Override
	protected void onPause() {
		super.onPause() ;
		
		mDialogManager.hideDialogs() ;
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        	if ( mSoundControls )
        		mQuantroSoundPool.menuButtonBack() ;
        }

        return super.onKeyDown(keyCode, event);
    }
	
	
	@Override
    protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this) ;
		builder.setInset(DIALOG_INSET) ;
		
		int qpanes ;
		
		switch ( id ) {
		case DIALOG_ID_SAVE_ERROR_NO_PIECES:
			builder.setMessage(R.string.custom_game_mode_dialog_save_error_no_pieces_message) ;
			builder.setCancelable(true) ;
			builder.setNegativeButton(
					R.string.custom_game_mode_dialog_save_error_button_ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mDialogManager.dismissDialog(DIALOG_ID_SAVE_ERROR_NO_PIECES) ;
						}
					}) ;
			builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_SAVE_ERROR_NO_PIECES) ;
 				}
			}) ;
			return builder.create() ;
			
		case DIALOG_ID_SAVE_ERROR_NO_NAME_OR_SUMMARY:
			builder.setMessage(R.string.custom_game_mode_dialog_save_error_no_name_or_summary_message) ;
			builder.setCancelable(true) ;
			builder.setNegativeButton(
					R.string.custom_game_mode_dialog_save_error_button_ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mDialogManager.dismissDialog(DIALOG_ID_SAVE_ERROR_NO_NAME_OR_SUMMARY) ;
						}
					}) ;
			builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_SAVE_ERROR_NO_NAME_OR_SUMMARY) ;
 				}
			}) ;
			return builder.create() ;
			
			
		case DIALOG_ID_CONFIRM_SAVE_CHANGES:
			qpanes = this.baseGameSpinnerPositionToValue( mBaseGameSpinner.getSelectedItemPosition() ) ;
			builder.setTitle(R.string.custom_game_mode_dialog_confim_save_changes_title) ;
			builder.setMessage(R.string.custom_game_mode_dialog_confim_save_changes_message) ;
			builder.setCancelable(true) ;
			builder.setPositiveButton(
					R.string.custom_game_mode_dialog_confirm_save_changes_yes,
					GameModeColors.customSecondary(mColorScheme, qpanes),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							saveMode() ;
							mDialogManager.dismissDialog(DIALOG_ID_CONFIRM_SAVE_CHANGES) ;
						}
					}) ;
			builder.setNegativeButton(
					R.string.custom_game_mode_dialog_confirm_save_changes_no,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mDialogManager.dismissDialog(DIALOG_ID_CONFIRM_SAVE_CHANGES) ;
						}
					}) ;
			builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_CONFIRM_SAVE_CHANGES) ;
 				}
			}) ;
			return builder.create() ;
		
		case DIALOG_ID_CONFIRM_DELETE:
			qpanes = this.baseGameSpinnerPositionToValue( mBaseGameSpinner.getSelectedItemPosition() ) ;
			builder.setTitle(R.string.custom_game_mode_dialog_confim_delete_title) ;
			builder.setMessage(R.string.custom_game_mode_dialog_confim_delete_message) ;
			builder.setCancelable(true) ;
			builder.setPositiveButton(
					R.string.custom_game_mode_dialog_confirm_delete_yes,
					GameModeColors.customSecondary(mColorScheme, qpanes),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							deleteMode() ;
							mDialogManager.dismissDialog(DIALOG_ID_CONFIRM_DELETE) ;
						}
					}) ;
			builder.setNegativeButton(
					R.string.custom_game_mode_dialog_confirm_delete_no,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mDialogManager.dismissDialog(DIALOG_ID_CONFIRM_DELETE) ;
						}
					}) ;
			builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_CONFIRM_DELETE) ;
 				}
			}) ;
			return builder.create() ;
			
		}
		
		return null ;
	}
	
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	int qpanes;
    	// only a few types require preperation.  Generally,
    	// we want to set dialog colors according to our current base game.
    	
    	switch( id ) {
    	case DIALOG_ID_CONFIRM_SAVE_CHANGES:
			qpanes = this.baseGameSpinnerPositionToValue( mBaseGameSpinner.getSelectedItemPosition() ) ;
			// we use the PRIMARY color for positive; this is
			// the color of the "save" button.
			((AlertDialog)dialog).setButtonColor(
					Dialog.BUTTON_POSITIVE,
					GameModeColors.customPrimary(mColorScheme, qpanes)) ;
			break ;
			
    	case DIALOG_ID_CONFIRM_DELETE:
			qpanes = this.baseGameSpinnerPositionToValue( mBaseGameSpinner.getSelectedItemPosition() ) ;
			// we use the SECONDARY color for positive; this is the color
			// of the "delete" button.
			((AlertDialog)dialog).setButtonColor(
					Dialog.BUTTON_POSITIVE,
					GameModeColors.customSecondary(mColorScheme, qpanes)) ;
			break ;
    	}
    }
	

	private void loadConstantsFromResources() {
		Resources res = getResources() ;
		
		DIALOG_INSET = res.getDimensionPixelSize(R.dimen.margin_inset_dialog) ;
		MIN_ROWS = res.getInteger(R.integer.custom_game_mode_min_rows) ;
		MAX_ROWS = res.getInteger(R.integer.custom_game_mode_max_rows) ;
		MIN_COLS = res.getInteger(R.integer.custom_game_mode_min_cols) ;
		MAX_COLS = res.getInteger(R.integer.custom_game_mode_max_cols) ;
		MAX_NAME_LENGTH = res.getInteger(R.integer.custom_game_mode_max_name_length) ;
		MAX_SUMMARY_LENGTH = res.getInteger(R.integer.custom_game_mode_max_summary_length) ;
		MAX_DESCRIPTION_LENGTH = res.getInteger(R.integer.custom_game_mode_max_description_length) ;
		
		DEFAULT_Q_PANES = res.getInteger(R.integer.custom_game_mode_default_qpanes) ;
		DEFAULT_ROWS = res.getInteger(R.integer.custom_game_mode_default_rows) ;
		DEFAULT_COLS = res.getInteger(R.integer.custom_game_mode_default_cols) ;
		DEFAULT_HAS_TROMINOES = res.getBoolean(R.bool.custom_game_mode_default_has_trominoes) ;
		DEFAULT_HAS_TETROMINOES = res.getBoolean(R.bool.custom_game_mode_default_has_tetrominoes) ;
		DEFAULT_HAS_PENTOMINOES = res.getBoolean(R.bool.custom_game_mode_default_has_pentominoes) ;
		DEFAULT_HAS_ROTATION = res.getBoolean(R.bool.custom_game_mode_default_has_rotation) ;
		DEFAULT_HAS_REFLECTION = res.getBoolean(R.bool.custom_game_mode_default_has_reflection) ;
	}
	
	
	private void setValuesFrom( Intent i ) {
		if ( i.hasExtra(INTENT_EXTRA_CGMS) && i.getSerializableExtra(INTENT_EXTRA_CGMS) != null )
			setValuesFrom( (CustomGameModeSettings)i.getSerializableExtra(INTENT_EXTRA_CGMS) ) ;
		else if ( i.hasExtra(INTENT_EXTRA_CGMS_ID) )
			setValuesFrom( i.getIntExtra(INTENT_EXTRA_CGMS_ID, -1) ) ;
		else
			throw new IllegalStateException("Requires an CGMS or CGMS_ID.") ;
		
		mHasSaves = i.getBooleanExtra(INTENT_EXTRA_HAS_SAVES, false) ;
		
		TextView titleView = (TextView)findViewById(R.id.dialog_content_title) ;
		if ( titleView != null )
			titleView.setText(
					mGivenCGMS == null
					? getResources().getString(R.string.custom_game_mode_title_new)
					: getResources().getString(R.string.custom_game_mode_title_edit)) ;
		
		boolean hasFlood = getPremiumLibrary().hasGameMode(GameModes.GAME_MODE_SP_QUANTRO_C) ;
		if ( mActionText != null )
			mActionText.setText(
					getResources().getString(
					mGivenCGMS == null
					? ( hasFlood ? R.string.custom_game_mode_action_new_has_flood : R.string.custom_game_mode_action_new )
					: ( hasFlood ? R.string.custom_game_mode_action_edit_has_flood : R.string.custom_game_mode_action_edit ) ) ) ;
	}
	
	private void setValuesFrom( CustomGameModeSettings cgms ) {
		mID = cgms.getID() ;
		mGivenCGMS = cgms ;
		
		// set values within
		// pieces
		mHasTrominoesCheckBox.setChecked( cgms.getHasTrominoes() ) ;
		mHasTetrominoesCheckBox.setChecked( cgms.getHasTetrominoes() ) ;
		mHasPentominoesCheckBox.setChecked( cgms.getHasPentominoes() ) ;
		// base game
		mBaseGameSpinner.setSelection(
				baseGameSpinnerValueToPosition(
						cgms.getNumberQPanes())) ;
		// movement
		mControlsSpinner.setSelection(
				controlsSpinnerValueToPosition(
						cgms.getHasRotation(),
						cgms.getHasReflection()) ) ;
		// dimensions
		setLabeledSeekBarRanges() ;
		mRowsLabeledSeekBarAdapter.setProgress( cgms.getRows() - MIN_ROWS ) ;
		mColsLabeledSeekBarAdapter.setProgress( cgms.getCols() - MIN_COLS ) ;
		// text
		mNameEditText.setText(			cgms.getName() ) ;
		mSummaryEditText.setText(		cgms.getSummary() ) ;
		mDescriptionEditText.setText(	cgms.getDescription() ) ;
	}
	
	private void setValuesFrom( int cgms_id ) {
		mID = cgms_id ;
		mGivenCGMS = null ;
		
		// set values within
		// pieces
		mHasTrominoesCheckBox.setChecked( DEFAULT_HAS_TROMINOES ) ;
		mHasTetrominoesCheckBox.setChecked( DEFAULT_HAS_TETROMINOES ) ;
		mHasPentominoesCheckBox.setChecked( DEFAULT_HAS_PENTOMINOES ) ;
		// base game
		mBaseGameSpinner.setSelection(baseGameSpinnerValueToPosition(DEFAULT_Q_PANES)) ;
		// movement
		mControlsSpinner.setSelection(
				controlsSpinnerValueToPosition(
						DEFAULT_HAS_ROTATION,
						DEFAULT_HAS_REFLECTION) ) ;
		// dimensions
		setLabeledSeekBarRanges() ;
		mRowsLabeledSeekBarAdapter.setProgress( DEFAULT_ROWS - MIN_ROWS ) ;
		mColsLabeledSeekBarAdapter.setProgress( DEFAULT_COLS - MIN_COLS ) ;
		// text
		mNameEditText.setText(			generateAutoName() ) ;
		mSummaryEditText.setText(		null ) ;
		mDescriptionEditText.setText(	null ) ;
	}
	
	private void setLabeledSeekBarRanges() {
		int [] vals = range( MIN_ROWS, MAX_ROWS ) ;
		String [] valLabels = toStrings( vals ) ;
		mRowsLabeledSeekBarAdapter.setExplicitValues(
				vals,
				getResources().getString(R.string.custom_game_mode_rows_label),
				valLabels,
				valLabels[0],
				valLabels[valLabels.length-1]) ;
		
		vals = range( MIN_COLS, MAX_COLS ) ;
		valLabels = toStrings( vals ) ;
		mColsLabeledSeekBarAdapter.setExplicitValues(
				vals,
				getResources().getString(R.string.custom_game_mode_cols_label),
				valLabels,
				valLabels[0],
				valLabels[valLabels.length-1]) ;
	}
	
	private void setEditTextFilters() {
		mNameEditText.setFilters(new InputFilter[] {
        		new InputFilter() {
        			public CharSequence filter( CharSequence source, int start, int end, Spanned dest, int dstart, int dend ) {
        				boolean spaceRemaining = dest.length() - (dend - dstart) + (end - start) <= MAX_NAME_LENGTH ;
        				
        				if ( spaceRemaining )
        					return null ;
        				else 
        					return dest.subSequence(dstart, dend) ;
        			}
        		}}) ;
		mSummaryEditText.setFilters(new InputFilter[] {
        		new InputFilter() {
        			public CharSequence filter( CharSequence source, int start, int end, Spanned dest, int dstart, int dend ) {
        				boolean spaceRemaining = dest.length() - (dend - dstart) + (end - start) <= MAX_SUMMARY_LENGTH ;
        				
        				if ( spaceRemaining )
        					return null ;
        				else 
        					return dest.subSequence(dstart, dend) ;
        			}
        		}}) ;
		mDescriptionEditText.setFilters(new InputFilter[] {
        		new InputFilter() {
        			public CharSequence filter( CharSequence source, int start, int end, Spanned dest, int dstart, int dend ) {
        				boolean spaceRemaining = dest.length() - (dend - dstart) + (end - start) <= MAX_DESCRIPTION_LENGTH ;
        				
        				if ( spaceRemaining )
        					return null ;
        				else 
        					return dest.subSequence(dstart, dend) ;
        			}
        		}}) ;
		
	}
	
	private void recolorButtons() {
		int qpanes = this.baseGameSpinnerPositionToValue( mBaseGameSpinner.getSelectedItemPosition() ) ;
		mDialogButtonStrip.setButtonColor(
				DialogButtonStrip.BUTTON_POSITIVE,
				GameModeColors.customPrimary(mColorScheme, qpanes) ) ;
		mDialogButtonStrip.setButtonColor(
				DialogButtonStrip.BUTTON_NEUTRAL,
				GameModeColors.customSecondary(mColorScheme, qpanes) ) ;
	}

	private String generateAutoName() {
		Resources res = getResources() ;
		String mAutoNameTemplate = res.getString(R.string.custom_game_mode_auto_name) ;
		return mAutoNameTemplate.replace(res.getString(R.string.placeholder_custom_game_mode_settings_id),
				"" + mID) ;
	}
	
	
	private String mAutoSummaryTemplate = null ;
	private String mAutoSummaryTemplateNoPieces = null ;
	private String mAutoSummaryTemplateOnePieceSet = null ;
	private String mAutoSummaryTemplateTwoPieceSets = null ;
	private String mAutoSummaryTemplateAllPieces = null ;
	private String mAutoSummaryTemplateTwoPieceSetsSeparator = null ;
	private String mAutoSummaryTemplateTrominoes = null ;
	private String mAutoSummaryTemplateTetrominoes = null ;
	private String mAutoSummaryTemplatePentominoes = null ;
	private String mAutoSummaryTemplatePlaceholderRows = null ;
	private String mAutoSummaryTemplatePlaceholderCols = null ;
	private String mAutoSummaryTemplatePlaceholderPieces = null ;
	private String mAutoSummaryTemplatePlaceholderPiecesName = null ;
	private String mAutoSummaryTemplatePlaceholderPiecesList = null ;
	
	
	private String generateAutoSummary() {
		if ( mAutoSummaryTemplate == null ) {
			Resources res = getResources() ;
			mAutoSummaryTemplate = res.getString(R.string.custom_game_mode_auto_summary) ;
			mAutoSummaryTemplateNoPieces = res.getString(R.string.custom_game_mode_auto_summary_no_pieces) ;
			mAutoSummaryTemplateOnePieceSet = res.getString(R.string.custom_game_mode_auto_summary_one_set_pieces) ;
			mAutoSummaryTemplateTwoPieceSets = res.getString(R.string.custom_game_mode_auto_summary_two_set_pieces) ;
			mAutoSummaryTemplateAllPieces = res.getString(R.string.custom_game_mode_auto_summary_all_pieces) ;
			mAutoSummaryTemplateTwoPieceSetsSeparator = res.getString(R.string.custom_game_mode_auto_summary_two_set_pieces_list_separator) ;
			mAutoSummaryTemplateTrominoes = res.getString(R.string.custom_game_mode_auto_summary_trominoes) ;
			mAutoSummaryTemplateTetrominoes = res.getString(R.string.custom_game_mode_auto_summary_tetrominoes) ;
			mAutoSummaryTemplatePentominoes = res.getString(R.string.custom_game_mode_auto_summary_pentominoes) ;
			
			mAutoSummaryTemplatePlaceholderRows = res.getString(R.string.placeholder_custom_game_mode_settings_rows) ;
			mAutoSummaryTemplatePlaceholderCols = res.getString(R.string.placeholder_custom_game_mode_settings_cols) ;
			mAutoSummaryTemplatePlaceholderPieces = res.getString(R.string.placeholder_custom_game_mode_settings_pieces) ;
			mAutoSummaryTemplatePlaceholderPiecesName = res.getString(R.string.placeholder_custom_game_mode_settings_pieces_name) ;
			mAutoSummaryTemplatePlaceholderPiecesList = res.getString(R.string.placeholder_custom_game_mode_settings_pieces_list) ;
		}
		
		// and make it.
		String summary = mAutoSummaryTemplate
				.replace(mAutoSummaryTemplatePlaceholderRows,
						"" + mRowsLabeledSeekBarAdapter.intValue())
				.replace(mAutoSummaryTemplatePlaceholderCols,
						"" + mColsLabeledSeekBarAdapter.intValue()) ;
		
		int num = 0 ;
		if ( mHasTrominoesCheckBox.isChecked() )
			num++ ;
		if ( mHasTetrominoesCheckBox.isChecked() )
			num++ ;
		if ( mHasPentominoesCheckBox.isChecked() )
			num++ ;
		
		if ( num == 0 )
			summary = summary.replace(mAutoSummaryTemplatePlaceholderPieces,
					mAutoSummaryTemplateNoPieces) ;
		else if ( num == 1 ) {
			summary = summary.replace(mAutoSummaryTemplatePlaceholderPieces,
					mAutoSummaryTemplateOnePieceSet ) ;
			if ( mHasTrominoesCheckBox.isChecked() )
				summary = summary.replace(mAutoSummaryTemplatePlaceholderPiecesName,
						mAutoSummaryTemplateTrominoes) ;
			else if ( mHasTetrominoesCheckBox.isChecked() )
				summary = summary.replace(mAutoSummaryTemplatePlaceholderPiecesName,
						mAutoSummaryTemplateTetrominoes) ;
			else if ( mHasPentominoesCheckBox.isChecked() )
				summary = summary.replace(mAutoSummaryTemplatePlaceholderPiecesName,
						mAutoSummaryTemplatePentominoes) ;
		}
		else if ( num == 2 ) {
			String first = null, second = null ;
			if ( mHasTrominoesCheckBox.isChecked() )
				first = mAutoSummaryTemplateTrominoes ;
			if ( mHasTetrominoesCheckBox.isChecked() ) {
				if ( first == null )
					first = mAutoSummaryTemplateTetrominoes ;
				else
					second = mAutoSummaryTemplateTetrominoes ;
			}
			if ( mHasPentominoesCheckBox.isChecked() ) {
				if ( first == null )
					first = mAutoSummaryTemplatePentominoes ;
				else
					second = mAutoSummaryTemplatePentominoes ;
			}
			
			summary = summary.replace(mAutoSummaryTemplatePlaceholderPieces,
							mAutoSummaryTemplateTwoPieceSets)
					.replace(mAutoSummaryTemplatePlaceholderPiecesList,
							first + mAutoSummaryTemplateTwoPieceSetsSeparator + second) ;
		}
		else if ( num == 3 )
			summary = summary.replace(mAutoSummaryTemplatePlaceholderPieces,
					mAutoSummaryTemplateAllPieces) ;
		
		return summary ;
	}

	private void saveMode() {
		// check errthing
		if ( !mHasTrominoesCheckBox.isChecked()
				&& !mHasTetrominoesCheckBox.isChecked()
				&& !mHasPentominoesCheckBox.isChecked() )
			mDialogManager.showDialog(DIALOG_ID_SAVE_ERROR_NO_PIECES) ;
		
		else if ( mNameEditText.getText() == null || mNameEditText.getText().toString().trim().length() == 0 )
			mDialogManager.showDialog(DIALOG_ID_SAVE_ERROR_NO_NAME_OR_SUMMARY) ;
		
		else {
			// everything seems ok
			CustomGameModeSettings cgms = makeCGMSFromViews() ;
			
			Log.d(TAG, "created cgms with number qpanes " + cgms.getNumberQPanes()) ;
			
			Intent data = new Intent() ;
			data.putExtra(INTENT_EXTRA_CGMS, cgms) ;
			data.putExtra(INTENT_RESULT_EXTRA_ACTION, INTENT_RESULT_EXTRA_ACTION_SAVE) ;
			
			setResult(RESULT_OK, data) ;
			finish() ;
		}
	}
	
	private void deleteMode() {
		CustomGameModeSettings cgms = makeCGMSFromViews() ;
		
		Intent data = new Intent() ;
		data.putExtra(INTENT_EXTRA_CGMS, cgms) ;
		data.putExtra(INTENT_RESULT_EXTRA_ACTION, INTENT_RESULT_EXTRA_ACTION_DELETE) ;
		
		setResult(RESULT_OK, data) ;
		finish() ;
	}
	
	/**
	 * Makes a CGMS object from the current view contents.
	 * Returns 'null' if the current settings are invalid.
	 * @return
	 */
	private CustomGameModeSettings makeCGMSFromViews() {
		CustomGameModeSettings.Builder builder = new CustomGameModeSettings.Builder( mID ) ;
		builder.setHasTrominoes(	mHasTrominoesCheckBox.isChecked()) ;
		builder.setHasTetrominoes(	mHasTetrominoesCheckBox.isChecked()) ;
		builder.setHasPentominoes(	mHasPentominoesCheckBox.isChecked()) ;
		
		int spinnerPos = mBaseGameSpinner.getSelectedItemPosition() ;
		builder.setNumberQPanes( baseGameSpinnerPositionToValue(spinnerPos) ) ;
		
		spinnerPos = mControlsSpinner.getSelectedItemPosition() ;
		builder.setHasRotation( controlsSpinnerPositionToHasRotation(spinnerPos) ) ;
		builder.setHasReflection( controlsSpinnerPositionToHasReflection(spinnerPos) ) ;
		
		builder.setRows( mRowsLabeledSeekBarAdapter.intValue() ) ;
		builder.setCols( mColsLabeledSeekBarAdapter.intValue() ) ;
		
		
		String name = mNameEditText.getText().toString().trim() ;
		String summary = mSummaryEditText.getText().toString().trim() ;
		String description = mDescriptionEditText.getText().toString().trim() ;
		
		builder.setName( name ) ;
		builder.setSummary( summary ) ;
		builder.setDescription( description ) ;
		
		builder.setAllowMultiplayer(false) ;
		
		try {
			return builder.build() ;
		} catch( IllegalStateException ise ) {
			ise.printStackTrace() ;
			return null ;
		}
	}
	
	private void cancel() {
		this.setResult(RESULT_CANCELED) ;
		finish() ;
	}
	
	
	private int [] range(int min, int max) {
		int [] vals = new int[max-min+1] ;
		for ( int i = 0; i < vals.length; i++ )
			vals[i] = i + min ;
		return vals ;
	}
	
	private String [] toStrings( int [] vals ) {
		String [] s = new String[vals.length] ;
		for ( int i = 0; i < s.length; i++ ) {
			if ( vals[i] == Integer.MIN_VALUE )
				s[i] = "-\u221E" ;
			else if ( vals[i] == Integer.MAX_VALUE )
				s[i] = "\u221E" ;
			else
				s[i] = "" + vals[i] ;
		}
		return s ;
	}
	
	
	private boolean controlsSpinnerPositionToHasRotation( int position ) {
		// order is 'turn', 'flip', 'turns & flips', 'moves only'
		return position == 0 || position == 2 ;
	}
	
	
	private boolean controlsSpinnerPositionToHasReflection( int position ) {
		// order is 'turn', 'flip', 'turns & flips', 'moves only'
		return position == 1 || position == 2 ;
	}
	
	
	
	private int controlsSpinnerValueToPosition( boolean hasRotation, boolean hasReflection ) {
		// order is 'turn', 'flip', 'turns & flips', 'moves only'
		if ( hasRotation && !hasReflection )
			return 0 ;
		else if ( !hasRotation && hasReflection )
			return 1 ;
		if ( hasRotation && hasReflection )
			return 2 ;
		return 3 ;
	}
	
	private int baseGameSpinnerPositionToValue( int position ) {
		// Spinner lists quantro, then retro (2 qpanes, then 1).
		return position == 0 ? 2 : 1 ;
	}
	
	private int baseGameSpinnerValueToPosition( int qpanes ) {
		// Spinner lists quantro, then retro (2 qpanes, then 1)
		return qpanes == 2 ? 0 : 1 ;
	}


	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		if ( arg0 == mBaseGameSpinner )
			recolorButtons() ;
		
	}


	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// do nothing
	}
}
