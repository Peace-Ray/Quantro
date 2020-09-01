package com.peaceray.quantro.premium;

public enum ContentStatus {
	/**
	 * Default.  Locked.  No access.  No "partial" access.
	 */
	LOCKED,
	
	
	/**
	 * The user has a key for this content but that key is not valid.
	 */
	INVALID,
	
	/**
	 * One step up from "locked:" the content is in the process of
	 * unlocking, but in currently inactive.
	 */
	INACTIVE,
	
	/**
	 * Unlocked and available.  This item can be used and accessed by the user.
	 */
	UNLOCKED ;
	
	/**
	 * Does this ContentStatus "override" the provided status?
	 * 
	 * To be nice, one status "overrides" another if it allows MORE access.
	 * @param status
	 * @return
	 */
	public boolean overrides( ContentStatus status ) {
		// Check the values above; they are in-order.
		return ordinal() > status.ordinal() ;
	}
}
