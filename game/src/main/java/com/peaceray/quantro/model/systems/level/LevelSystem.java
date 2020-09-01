package com.peaceray.quantro.model.systems.level;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;

public abstract class LevelSystem implements SerializableState {

	
	public static final int TYPE_CLEARS_SINCE_LAST = 0 ;		// Concerned with # of clears since the last level-up
	public static final int TYPE_CLEARS = 1 ;					// Concerned with the total # of clears
	public static final int NUM_TYPES = 2 ;
	
	public static final int SUBTYPE_CLEARS_S0 = 0 ;				// Number of S0 clears
	public static final int SUBTYPE_CLEARS_S1 = 1 ;				// Number of S1 clears
	public static final int SUBTYPE_CLEARS_SS = 2 ;				// Number of 2-pane clears
	public static final int SUBTYPE_CLEARS_MO = 3 ;				// Number of monochrome clears
	public static final int SUBTYPE_CLEARS_ANY = 4 ;			// Clears of any single type
	public static final int SUBTYPE_CLEARS_SS_AND_MO = 5 ;		// Add SS and MO.
	public static final int SUBTYPE_CLEARS_TOTAL = 6 ;			// Total # of line clears
	public static final int NUM_SUBTYPES = 7 ;
	
	
	
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
	
	
	/**
	 * Uses the information currently stored in ginfo to determine if
	 * a level gain is appropriate at this moment.
	 * @return
	 */
	public abstract boolean shouldGainLevel() ;
	
	
	/**
	 * The method "shouldGainLevel" is meant to be called repeatedly, with no
	 * change to any game element.  This method, on the other hand, notifies the
	 * LevelSystem that a level has just been gained.
	 */
	public abstract void didGainLevel() ;
	
	
	/**
	 * Notifies the LevelSystem that a piece just entered.
	 * 
	 * This may or may not be significant.  For example, a LevelSystem may
	 * want to ignore some specific Level-Up criteria if they occur between 
	 * gaining a level (see 'didGainLevel') and the next piece appearing.
	 * 
	 * To be more specific, Progression mode features level gains after
	 * a fixed number of "clears since last level up."  It also introduces
	 * garbage row(s) in the next PREPARING state after a level up.  These
	 * garbage rows might cause a clear cascade, resulting in clears being
	 * collected towards the NEXT level-up before the PREVIOUS level-up
	 * is even indicated to the player!  This is BAD.
	 * 
	 * Therefore, such a LevelSystem may want to follow this procedure:
	 * 
	 * didGainLevel()
	 * 		set justGainedLevel=true
	 * 
	 * didEnterPiece()
	 * 		if "justGainedLevel"
	 * 			set offset = ginfo.clearsSinceLevelUp
	 * 			set justGainedLevel=false
	 * 
	 * shouldGainLevel()
	 * 		return levelsNeeded <= ginfo.clearsSinceLevelUp - offset
	 * 
	 * @param piece
	 */
	public abstract void didEnterPiece() ;
	
	
}
