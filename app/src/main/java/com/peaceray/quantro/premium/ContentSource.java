package com.peaceray.quantro.premium;

public enum ContentSource {
	/**
	 * The user has directly purchased this piece of content.
	 */
	DIRECT_PURCHASE,
	
	/**
	 * The user has been given a promotional key (NOT XL) that allowed
	 * access to this piece of content.
	 */
	PROMO,
	
	
	/**
	 * The user previously purchased Quantro XL, and so has been
	 * given access to this premium content.
	 */
	QUANTRO_XL_DIRECT_PURCHASE,
	
	/**
	 * The user has a QuantroXL promotional key, and so has been
	 * given access to this premium content.
	 */
	QUANTRO_XL_PROMO
}
