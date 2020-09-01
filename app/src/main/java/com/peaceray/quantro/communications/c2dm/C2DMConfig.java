package com.peaceray.quantro.communications.c2dm;

public class C2DMConfig {

	/**
     * Cloud sender - replace with your one email, should match the account
     * used for authentication by the cloud sender.
     */
    public static final String C2DM_SENDER = "quantro.peaceray@gmail.com";
    public static final String C2DM_ACTION_EXTRA = "action";
    public static final String C2DM_CHALLENGE_NONCE_EXTRA = "challenge_nonce" ;
    public static final String C2DM_CHALLENGER_NAME_EXTRA = "challenger_name" ;
    	// Only supplied for "new challenge" messages.
    
    public static final String C2DM_ACTION_CHALLENGE = "c" ;
    public static final String C2DM_ACTION_ACCEPT_OR_DECLINE = "ad" ;
	public static final String C2DM_ACTION_RESCIND = "r" ;
	
	
	
}
