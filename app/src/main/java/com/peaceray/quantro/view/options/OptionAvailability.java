package com.peaceray.quantro.view.options;

public enum OptionAvailability {

	/**
	 * This Option can be selected by the user.
	 */
	ENABLED,
	
	
	/**
	 * This Option is disabled; the user cannot select it.
	 */
	DISABLED,
	
	
	/**
	 * This Option is "locked"; the user cannot activate it.
	 * However, it is still enabled, and the user can select it.
	 * 
	 * For example, a premium Skin or Background which the 
	 * user has not purchased should be placed in this state.
	 * It will be rendered to indicate its "locked status" but
	 * user selection will proceed as normal, giving you a chance
	 * to direct them to a purchase page.
	 * 
	 */
	LOCKED_ENABLED,
	
	
	/**
	 * This option is "locked" and disabled: the user can neither
	 * activate it or select it.  Distinguished from DISABLED in
	 * that it is rendered to indicate "locked" status.
	 */
	LOCKED_DISABLED,
	
	
	/**
	 * This option is "timed": the user can activated it,
	 * but only for a certain period of time.
	 * 
	 * For example, a premium Game Mode which the user has not
	 * purchased should be placed in this state.  It will be
	 * rendered to indicate its "timed status" but user selection
	 * will proceed as normal, giving you a chance to direct them
	 * to a purchase page or a timed trial.
	 */
	TIMED_ENABLED,
	
	
	/**
	 * This option is "timed" and disabled.
	 * 
	 * The user can't touch this.  It is rendered as if it is TIMED_ENABLED,
	 * except the button is disabled.
	 */
	TIMED_DISABLED,
	
	
	/**
	 * The option is enabled, having been unlocked "by-proxy."  This implies
	 * that the user does not have a permanent unlock of the options, but instead
	 * has temporary access to it in an unlocked state.  That access can be revoked
	 * for reasons such as 1. time expiring (note the difference between this and
	 * 'timed enabled'), 2. connection to the unlocking party being broken, etc.
	 * 
	 * One example usage is multiplayer game modes: if one member of a lobby has
	 * a game mode unlocked (i.e. it is in ENABLED status), all other members will
	 * have that game mode "PROXY UNLOCKED" on their devices as long as they --
	 * and the owner of the game mode -- remain in the lobby.
	 */
	PROXY_UNLOCKED_ENABLED, 
	
	
	
	/**
	 * This option is "proxy unlocked" but disabled.
	 */
	PROXY_UNLOCKED_DISABLED ;
	
	
	
	public boolean isEnabled() {
		return this == ENABLED || this == LOCKED_ENABLED || this == TIMED_ENABLED || this == PROXY_UNLOCKED_ENABLED ;
	}
	
	public boolean isLocked() {
		return this == LOCKED_DISABLED || this == LOCKED_ENABLED ;
	}
	
	public boolean isTimed() {
		return this == TIMED_ENABLED || this == TIMED_DISABLED ;
	}
	
	public boolean isProxyUnlocked() {
		return this == PROXY_UNLOCKED_ENABLED || this == PROXY_UNLOCKED_DISABLED ;
	}
	
}
