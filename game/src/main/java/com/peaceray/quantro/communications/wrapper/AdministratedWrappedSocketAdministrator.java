package com.peaceray.quantro.communications.wrapper;

import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

public interface AdministratedWrappedSocketAdministrator {
	
	/**
	 * Wraps the provided DatagramChannel into an administrated WrappedSocket,
	 * returning the result.
	 * 
	 * This object will likely be the administrator.
	 * 
	 * @param messageClass
	 * @param channel
	 * @param dest
	 * @param prefix
	 * @return
	 */
	public WrappedSocket wrap( Class<?> messageClass, DatagramChannel channel, SocketAddress dest, byte [] prefix ) ;
	
}
