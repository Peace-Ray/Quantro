package com.peaceray.quantro.communications;

import com.peaceray.quantro.communications.wrapper.WrappedSocket;

public interface AcceptsClientSockets {

	/**
	 * Attempts to set the provided socket as a Player's socket.  Will return
	 * the playerSlot designated for this player; '-1' if no playerSlot was
	 * assigned and the set failed.
	 * 
	 * @param wsock 	The client socket we have already interrogated.
	 * @param ci 	Information about the client socket.
	 * @return
	 */
	public int setClientSocket( WrappedSocket wsock, ClientInformation ci ) ;
	
}
