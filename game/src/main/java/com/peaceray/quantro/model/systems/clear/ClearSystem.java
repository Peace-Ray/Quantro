package com.peaceray.quantro.model.systems.clear;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;

public abstract class ClearSystem implements SerializableState {
	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this ClearSystem.
	 * 
	 * @return			An instance of GameInformation
	 */
	public abstract GameInformation getGameInformation() ;
	
	/**
	 * getQInteractions: Clears are determined using a QInteractions
	 * implementation.  This gets the implementation used for this system.
	 * 
	 * @return		An instance of QInteractions
	 */
	public abstract QInteractions getQInteractions() ;


	////////////////////////////////////////////////////////////////
	// CHROMATIC CLEARS
	
	/**
	 * clearable: sets the entries in 'clearableArray' to the type
	 * of clear which is appropriate for each row of field.  Returns
	 * 'true' if any row is set to something other than NO.  Other options
	 * are S0, S1 and SL.
	 * 
	 * PRECONDITION:
	 * 		'clearableArray' has length equal to field[0].length
	 * 		field is [2][*][*] of QOrientations.
	 * 
	 * POSTCONDITION:
	 * 		the elements of clearableArray are set to the appropriate type
	 * 				of clear for the row in field.  Available types are:
	 * 				NO:		Clear neither pane
	 * 				S0:		Clear pane 0, not 1
	 * 				S1:		Clear pane 1, not 0
	 * 				SL:		Clear both panes
	 * 
	 * @param field				A block field to be cleared (eventually)
	 * @param clearableArray	An array with length = field[0].length.
	 * @return					Whether any entry in clearableArray was set to non-NO.
	 */
	public abstract boolean clearable( byte [][][] field, int [] clearableArray ) ;
	
	/**
	 * clearable: returns the appropriate type of clear for the given 
	 * row in field, where row is [][row][].  Available types are:
	 * 				NO:		Clear neither pane
	 * 				S0:		Clear pane 0, not 1
	 * 				S1:		Clear pane 1, not 0
	 * 				SL:		Clear both panes
	 * 
	 * @param field			A block field to be cleared (eventually)
	 * @param row			The row in block field to investigate
	 * @return				The type of clear appropriate for the row
	 */
	public abstract int clearable( byte [][][] field, int row ) ;
	
	/**
	 * clear: performs all the clears listed in clearableArray, i.e.
	 * replaces the appropriate entries in 'field' by NO.
	 * 
	 * PRECONDITION:
	 * 		clearableArray is as set by 'clearable'.
	 * 
	 * POSTCONDITION
	 * 		the appropriate blocks in 'field' have been replaced by NO
	 * 
	 * @param field				A blockfield to clear
	 * @param clearableArray	Indicates how to clear each row
	 */
	public abstract void clear( byte [][][] field, int [] clearableArray ) ;
	
	/**
	 * clear: performs all clear specified by row, clearType:
	 * i.e., replaces the appropriate blocks in field[*][row][*]
	 * by NO according to clearType, which is as returned by 'clearable'.
	 * 
	 * PRECONDITION:
	 * 		clearType is as set by 'clearable' for the given row
	 * 
	 * POSTCONDITION
	 * 		the appropriate blocks in 'field' have been replaced by NO
	 * 
	 * @param field				A blockfield to clear
	 * @param row				Indexes a particular row in field
	 * @param clearType			Which type of clear to employ?
	 */
	public abstract void clear( byte [][][] field, int row, int clearType ) ;
	
	/**
	 * inverseClear: keeps only those entries which ARE cleared by the provided
	 * clear array.
	 * @param field
	 * @param clearableArray
	 */
	public abstract void inverseClear( byte [][][] field, int [] clearableArray ) ;
	
	/**
	 * inverseClear: keeps only those entries which ARE cleared by the provided
	 * clear array.
	 * @param field
	 * @param clearableArray
	 */
	public abstract void inverseClear( byte [][][] field, int row, int clearType ) ;
	
	
	
	
	
	
	
	////////////////////////////////////////////////////////////////
	// MONOCHROME CLEARS
	
	/**
	 * clearableMonochrome: sets the entries in 'clearableArray' to true
	 * if a monochromatic clear is appropriate for the equivalent row
	 * in field.
	 * 
	 * PRECONDITION:
	 * 		'clearableArray' has length equal to field[0].length
	 * 		field is [2][*][*] of QOrientations.
	 * 
	 * POSTCONDITION:
	 * 		the elements of clearableArray are set according to whether
	 * 				a monochromatic clear is appropriate for the row.
	 * 
	 * @param field				A block field to be cleared (eventually)
	 * @param clearableArray	An array with length = field[0].length.
	 * @return					Whether any entry in clearableArray was set to true.
	 */
	public abstract boolean clearableMonochrome( byte [][][] field, boolean [] clearableArray ) ;
	
	/**
	 * clearableMonochrome: returns whether the indicated row is susceptible
	 * to a monochromatic clear.
	 * 
	 * @param field				A block field to be cleared (eventually)
	 * @param row				Which row should be investigated
	 * @return					Whether the row is (monochromatically) clearable
	 */
	public abstract boolean clearableMonochrome( byte [][][] field, int row ) ;
	
	/**
	 * clearMonochrome: performs a monochromatic clear on the rows indicated
	 * by clearableArray
	 * 
	 * PRECONDITION:
	 * 		'clearableArray' is as set by clearableMonochrome
	 * 
	 * POSTCONDITION:
	 * 		where 'clearableArray' is True, the corresponding row in
	 * 				field has been cleared.
	 * 
	 * @param field				A block field to be cleared
	 * @param clearableArray	Boolean array indicating which rows to clear.
	 */
	public abstract void clearMonochrome( byte [][][] field, boolean [] clearableArray ) ;
	
	/**
	 * clearMonochrome: performs a monochromatic clear on the given row, if
	 * shouldClear is true.
	 * 
	 * @param field				A block field to be cleared
	 * @param row				The row to clear
	 * @param shouldClear		Should we actually perform a monochrome clear?
	 */
	public abstract void clearMonochrome( byte [][][] field, int row, boolean shouldClear ) ;
	
	
	/**
	 * inverseClear: keeps only those entries which ARE cleared by the provided
	 * clear array.
	 * @param field
	 * @param clearableArray
	 */
	public abstract void inverseClearMonochrome( byte [][][] field, boolean [] clearableArray ) ;
	
	/**
	 * inverseClear: keeps only those entries which ARE cleared by the provided
	 * clear array.
	 * @param field
	 * @param clearableArray
	 */
	public abstract void inverseClearMonochrome( byte [][][] field, int row, boolean shouldClear ) ;
	
	
}
