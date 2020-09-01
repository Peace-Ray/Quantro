package com.peaceray.quantro.lobby;

import com.peaceray.quantro.communications.nonce.Effort;
import com.peaceray.quantro.communications.nonce.Nonce;

/**
 * MatchTickets, like WorkOrders, represent a collection of data
 * which we must operate over (i.e., perform Effort on) before being
 * allowed to proceed.  MatchTickets are specifically required before
 * connecting to a particular lobby or game (being "matched" with a 
 * host or client).
 * 
 * However, unlike WorkOrders, MatchTickets do not contain any important
 * information for us.  We never need to unpack or process the information
 * contained within -- that info is created by the server for later
 * processing by that same server.
 * 
 * Instead, we treat the matchticket as an arbitrary base-64 string.
 * We perform some Effort on it, but the actual contents are irrelevant
 * to us.
 * 
 * @author Jake
 *
 */
public class MatchTicketOrder {

	private Effort mEffort ;
	
	private int mEffortAmount ;
	private String mMatchTicketString ;
	
	
	public MatchTicketOrder( String ticketString, int effortBits ) {
		mMatchTicketString = ticketString ;
		mEffortAmount = effortBits ;
		
		mEffort = new Effort( mEffortAmount, mMatchTicketString ) ;
	}
	
	
	public String getTicket() {
		return mMatchTicketString ;
	}
	
	/**
	 * Performs the work indicated in the work order.
	 * If this work order is already complete, will return immediately.
	 */
	synchronized public void performWork() {
		mEffort.getSalt() ;
	}
	
	/**
	 * Performs the work indicated in the work order, taking
	 * a maximum of maxMillis to do so.  If maxMillis is <= 0,
	 * this is equivalent to performWork().
	 * 
	 * @param maxMillis
	 * @return Whether the work is complete.  If 'false',
	 * 	continue to make granular calls to this method.
	 */
	synchronized public boolean performWork( long maxMillis ) {
		return mEffort.getSalt(maxMillis) != null ;
	}
	
	/**
	 * Returns whether this work order is complete.
	 * @return
	 */
	public boolean isComplete() {
		return mEffort.hasSalt() ;
	}
	
	
	public Nonce getProof() {
		return mEffort.getSalt() ;
	}
}
