package com.peaceray.quantro.communications.wrapper;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

/**
 * A Factory class for constructing SocketWrappers.  Because Wrappers
 * require working connections, these 'wrap' methods will only
 * return a new SocketWrapper if the connection appears OK
 * (although we don't go to too much trouble to check it out).
 * 
 * If the method returns 'null', then something went wrong when
 * wrapping the connection.
 * 
 * @author Jake
 *
 */
public class AutonomousWrappedSocketFactory {

	public static WrappedSocket wrap( Socket sock ) {
		if ( sock == null || sock.isConnected() != true ) {
			return null ;
		}
		
		try {
			// HACK: TEST FAKE SOCKETS
			// return new FakeWrappedTCPSocket(sock) ;
			
			AutonomousWrappedSocket s = new AutonomousWrappedTCPSocket(sock) ;
			s.start();
			return s ;
			
		} catch( IOException e ) {
			return null ;
		}
	}
	
	public static WrappedSocket wrap( DatagramSocket listenSocket, SocketAddress dest, byte [] prefix ) {
		if ( listenSocket == null || dest == null ) 
			return null ;
		
		try {
			listenSocket.setSoTimeout(0) ;
			AutonomousWrappedSocket s = new AutonomousWrappedUDPSocket( listenSocket, dest, prefix ) ;
			s.start() ;
			return s ;
		} catch( IOException e ) {
			return null ;
		}
	}
	
	public static WrappedSocket wrap( DatagramChannel channel, SocketAddress dest, byte [] prefix ) {
		if ( channel == null || dest == null ) 
			return null ;
		
		try {
			channel.socket().setSoTimeout(0) ;
			AutonomousWrappedSocket s = new AutonomousWrappedUDPSocketChannel( channel, dest, prefix ) ;
			s.start() ;
			return s ;
		} catch( IOException e ) {
			return null ;
		}
	}
}
