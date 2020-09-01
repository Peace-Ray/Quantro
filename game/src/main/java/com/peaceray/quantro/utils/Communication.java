package com.peaceray.quantro.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Inet4Address ;
import java.net.Inet6Address ;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import com.peaceray.quantro.communications.nonce.Nonce;

public class Communication {

	// Default settings for getPublicUDPEndpointAsSocketAddress
	public static final String DEFAULT_WHEREAMI_SERV_ADDRESS = "peaceray.com" ;
	public static final int DEFAULT_WHEREAMI_SERV_PORT = 50000 ;
	public static final int DEFAULT_WHEREAMI_RESEND_DELAY = 1000 ;		// 1 second
	public static final int DEFAULT_WHEREAMI_TIMEOUT = 5000 ;			// 5 seconds
	
	public static final int DEFAULT_UDP_HOLE_PUNCHING_RESEND_DELAY = 250 ;		// 1/4 second
	public static final int DEFAULT_UDP_HOLE_PUNCHING_TIMEOUT = 4000 ;	// 4 seconds
	public static final int DEFAULT_UDP_HOLE_PUNCHING_CONFIRM_MESSAGE_SENDS = 4 ;		// 4 sends because why not?
	
	
	
	public static final int TYPE_ANY = 0 ;
	public static final int TYPE_IPv4 = 1 ;
	public static final int TYPE_IPv6 = 2 ;
	public static final int NUM_TYPES = 3 ;
	
	
	public static InetAddress getLocalInetAddress(int type) {
		if ( type < 0 || type >= NUM_TYPES )
			throw new IllegalArgumentException("Must specify a type from 0 to " + (NUM_TYPES-1) ) ;
		
		//new Exception("getLocalInetAddress called").printStackTrace() ;
		try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    if ( type == TYPE_ANY )
	                    	return inetAddress ;
	                    else if ( type == TYPE_IPv4 && inetAddress instanceof Inet4Address )
	                    	return inetAddress ;
	                    else if ( type == TYPE_IPv6 && inetAddress instanceof Inet6Address ) 
	                    	return inetAddress ;
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        // Log.e(LOG_TAG, ex.toString());
	    }
	    return null;
	}
	
	public static InetAddress getLocalInetAddress() {
		return getLocalInetAddress(TYPE_ANY) ;
	}
	
	public static String getLocalIpAddress(int type) {
		InetAddress addr = getLocalInetAddress(type) ;
		if ( addr == null )
			return null ;
		
		return addr.getHostAddress() ;
	}
	
	public static String getLocalIpAddress() {
		return getLocalIpAddress(TYPE_ANY) ;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////
	//
	// GETTING UDP PRIVATE AND PUBLIC ENDPOINTS
	//
	// Useful for UDP hole punching.
	//
	//////////////////////////////////////////////////////////////////////////////////
	
	
	
	/**
	 * Returns as a SocketAddress (specifically an InetSocketAddress) the
	 * PRIVATE endpoint associated with this application.  This SocketAddress
	 * is formed as the combination of the local IP address (using
	 * Communication.getLocalIpAddress()) and the provided port.
	 * 
	 * This method provides the private half of the 2 endpoints required
	 * for UDP hole punching - transmit this SocketAddress, along with the
	 * public endpoint, to the other party and listen for connections on
	 * the port provided.
	 * 
	 * @param port The port number to use for this private endpoint.
	 * @return A SocketAddress representing the private endpoint for this application
	 * 		using the provided port.  If IPv4 is available, it will be used.
	 * 		Returns 'null' upon failure.  
	 */
	public static SocketAddress getPrivateEndpointAsSocketAddress( int port ) {
		InetAddress addr = getLocalInetAddress(TYPE_IPv4) ;
		if ( addr == null )
			addr = getLocalInetAddress(TYPE_ANY) ;
		if ( addr == null )
			return null ;
		return new InetSocketAddress(addr, port) ;
	}
	
	
	
	
	
	/**
	 * Returns as a SocketAddress (specifically an InetSocketAddress) the
	 * PUBLIC udp endpoint associated with this application.  The public endpoint
	 * is determined through udp communication with an outside party (specifically
	 * the upd_whereami script running on peaceray.com).  This communication could
	 * cause a delay up to DEFAULT_WHEREAMI_TIMEOUT milliseconds.
	 *
	 * Why use this function?  Because NAT traversal can cause our externally visible
	 * IP and port number to differ from those we have direct access to.  This method
	 * opens a UDP datagram socket on the provided port (which should match the internal
	 * port desired for communication) and sends "where am I" messages to Peaceray.com.
	 * Peaceray responds with an 8-byte packet containing our public IP and the port number
	 * assigned for communication.  In rare cases, such as when no NAT is used, this will
	 * match the output of getPrivateUDPEndpointAsSocketAddress.
	 * 
	 * This method performs a call to
	 * getPublicUDPEndpointAsSocketAddress( sock, numAttempts, delayBetweenAttempts, servIP, servPort )
	 * using default values.
	 * 
	 * The most obvious use of this method is for UDP NAT hole punching, the first
	 * step of which is to provide the other party (through a mediator) our private
	 * and public endpoints.  Required for this process is the establishment of a UDP
	 * socket using the same port number as is provided to this method.  Because we
	 * used that port to communicate with the WhereAmI server, it may receive packets
	 * which originate from WhereAmI as a delayed response to this method call.  These
	 * packets are very likely to have public source endpoints equivalent to those provided
	 * (or if none are provided, the default values publically readable as static class constants).
	 * At present, all packets sent by WhereAmI begin with the byte 0xff or 0xfe.
	 * It would probably be good practice to check the first byte of any incoming packet.
	 * You should be sure to disregard any packet originating from the WhereAmI service,
	 * since they will be irrelevant to your purposes.  This class provides a public 
	 * isWhereAmIPacket for your convenience.
	 * 
	 * POSTCONDITION: This method has attempted to open a DatagramSocket to listen
	 * 			on the provided port.  If successful, it sent 1 or more requests
	 * 			to the WhereAmI server and listened for a WhereAmI response.  If
	 * 			such a response was received in the time alotted, this call returns
	 * 			the external IP address and port number reported by WhereAmI; if not,
	 * 			or some other error occurred, it returns null.
	 * 
	 * 			Whatever the result, if a DatagramSocket was successfully opened,
	 * 			then it was nicely closed before this method returned.
	 * 
	 * 			If this method returns a value, then as a necessary postcondition,
	 * 			datagrams from outside can be received on a DatagramSocket using
	 * 			the provided port.  You can use this to establish UDP hole punching
	 * 			(but do act quickly!)
	 * 
	 * @param port		The local port from which to send the whereami request.
	 * @return	'null' if we couldn't contact the whereami server; otherwise, the udp SocketAddress
	 * 			as seen by the whereami server.
	 */
	public static SocketAddress getPublicUDPEndpointAsSocketAddress( int port ) {
		try {
			DatagramChannel channel = null ;
			try {
				channel = DatagramChannel.open() ;
				channel.connect( new InetSocketAddress(port) ) ;
				channel.configureBlocking(true) ;
				return getPublicUDPEndpointAsSocketAddress( channel ) ;
			} catch ( SocketException e ) {
				e.printStackTrace() ;	// System.out.print System.err.print  <-- for finding this later
				return null ;
			} finally {
				// nicely close the socket.
				if ( channel != null )
					channel.close() ;
			}
		} catch (IOException ioe ) {
			return null ;
		}
		
	}
	
	
	/**
	 * Returns as a SocketAddress (specifically an InetSocketAddress) the
	 * PUBLIC udp endpoint associated with this application.  The public endpoint
	 * is determined through udp communication with an outside party (specifically
	 * the upd_whereami script running on peaceray.com).  Because this outside communication
	 * can cause delays of unspecified length, it is wise to specify a timeout for giving
	 * up (in this case, as numAttempts * delayBetweenAttempts)
	 * 
	 * Why use this function?  Because NAT traversal can cause our externally visible
	 * IP and port number to differ from those we have direct access to.  This method
	 * opens a UDP datagram socket on the provided port (which should match the internal
	 * port desired for communication) and sends "where am I" messages to Peaceray.com.
	 * Peaceray responds with an 8-byte packet containing our public IP and the port number
	 * assigned for communication.  In rare cases, such as when no NAT is used, this will
	 * match the output of getPrivateUDPEndpointAsSocketAddress.
	 * 
	 * This method performs a call to
	 * getPublicUDPEndpointAsSocketAddress( sock, numAttempts, delayBetweenAttempts, servIP, servPort )
	 * using default values.
	 * 
	 * The most obvious use of this method is for UDP NAT hole punching, the first
	 * step of which is to provide the other party (through a mediator) our private
	 * and public endpoints.  Required for this process is the establishment of a UDP
	 * socket using the same port number as is provided to this method.  Because we
	 * used that port to communicate with the WhereAmI server, it may receive packets
	 * which originate from WhereAmI as a delayed response to this method call.  These
	 * packets are very likely to have public source endpoints equivalent to those provided
	 * (or if none are provided, the default values publically readable as static class constants).
	 * At present, all packets sent by WhereAmI begin with the byte 0xff or 0xfe.
	 * It would probably be good practice to check the first byte of any incoming packet.
	 * You should be sure to disregard any packet originating from the WhereAmI service,
	 * since they will be irrelevant to your purposes.  This class provides a public 
	 * isWhereAmIPacket for your convenience.
	 * 
	 * POSTCONDITION: This message has sent 1 or more requests on the provided socket
	 * 			to the WhereAmI server and listened for a WhereAmI response.  If
	 * 			such a response was received in the time alotted, this call returns
	 * 			the external IP address and port number reported by WhereAmI; if not,
	 * 			or some other error occurred, it returns null.
	 * 
	 * 			If this method returns a value, then as a necessary postcondition,
	 * 			datagrams from outside can be received on the provided DatagramSocket
	 * 			You can use this to establish UDP hole punching (but do act quickly!)
	 * 
	 * @param sock		The datagram socket on which to send the request
	 * @return	'null' if we couldn't contact the whereami server; otherwise, the udp SocketAddress
	 * 			as seen by the whereami server.
	 */
	public static SocketAddress getPublicUDPEndpointAsSocketAddress( DatagramChannel channel ) {
		@SuppressWarnings("unused")
		int numAttempts = (DEFAULT_WHEREAMI_TIMEOUT <= 0) ? 0 : (int)Math.ceil(DEFAULT_WHEREAMI_TIMEOUT / DEFAULT_WHEREAMI_RESEND_DELAY) ;
		return getPublicUDPEndpointAsSocketAddress( channel,
					numAttempts, DEFAULT_WHEREAMI_RESEND_DELAY,
					DEFAULT_WHEREAMI_SERV_ADDRESS, DEFAULT_WHEREAMI_SERV_PORT) ;
	}
	
	
	
	/**
	 * Returns as a SocketAddress (specifically an InetSocketAddress) the
	 * PUBLIC udp endpoint associated with this application.  The public endpoint
	 * is determined through udp communication with an outside party (specifically
	 * the upd_whereami script running on peaceray.com).  This method could block
	 * for up to DEFAULT_WHEREAMI_TIMEOUT milliseconds.
	 * 
	 * Why use this function?  Because NAT traversal can cause our externally visible
	 * IP and port number to differ from those we have direct access to.  This method
	 * opens a UDP datagram socket on the provided port (which should match the internal
	 * port desired for communication) and sends "where am I" messages to Peaceray.com.
	 * Peaceray responds with an 8-byte packet containing our public IP and the port number
	 * assigned for communication.  In rare cases, such as when no NAT is used, this will
	 * match the output of getPrivateUDPEndpointAsSocketAddress.
	 * 
	 * This method performs a call to
	 * getPublicUDPEndpointAsSocketAddress( sock, numAttempts, delayBetweenAttempts, servIP, servPort )
	 * using default values.
	 * 
	 * The most obvious use of this method is for UDP NAT hole punching, the first
	 * step of which is to provide the other party (through a mediator) our private
	 * and public endpoints.  Required for this process is the establishment of a UDP
	 * socket using the same port number as is provided to this method.  Because we
	 * used that port to communicate with the WhereAmI server, it may receive packets
	 * which originate from WhereAmI as a delayed response to this method call.  These
	 * packets are very likely to have public source endpoints equivalent to those provided
	 * (or if none are provided, the default values publically readable as static class constants).
	 * At present, all packets sent by WhereAmI begin with the byte 0xff or 0xfe.
	 * It would probably be good practice to check the first byte of any incoming packet.
	 * You should be sure to disregard any packet originating from the WhereAmI service,
	 * since they will be irrelevant to your purposes.  This class provides a public 
	 * isWhereAmIPacket for your convenience.
	 * 
	 * POSTCONDITION: This method has attempted to open a DatagramSocket to listen
	 * 			on the provided port.  If successful, it sent 1 or more requests
	 * 			to the WhereAmI server and listened for a WhereAmI response.  If
	 * 			such a response was received in the time alotted, this call returns
	 * 			the external IP address and port number reported by WhereAmI; if not,
	 * 			or some other error occurred, it returns null.
	 * 
	 * 			Whatever the result, if a DatagramSocket was successfully opened,
	 * 			then it was nicely closed before this method returned.
	 * 
	 * 			If this method returns a value, then as a necessary postcondition,
	 * 			datagrams from outside can be received on a DatagramSocket using
	 * 			the provided port.  You can use this to establish UDP hole punching
	 * 			(but do act quickly!)
	 * 
	 * @param port		The local port from which to send the whereami request.
	 * @param whereAmIServAddress		The address, as a hostname or IP address, of
	 * 					the WhereAmI service.
	 * @param whereAmIServPort The port on which the WhereAmI service is listening.
	 * @return	'null' if we couldn't contact the whereami server; otherwise, the udp SocketAddress
	 * 			as seen by the whereami server.
	 */
	public static SocketAddress getPublicUDPEndpointAsSocketAddress( int port,
			String whereAmIServAddress, int whereAmIServPort ) {
		
		try {
			DatagramChannel channel = null ;
			try {
				channel = DatagramChannel.open() ;
				channel.connect(new InetSocketAddress(port) ) ;
				channel.configureBlocking(true) ;
				return getPublicUDPEndpointAsSocketAddress( channel, whereAmIServAddress, whereAmIServPort ) ;
			} catch ( SocketException e ) {
				return null ;
			} finally {
				// nicely close the socket.
				if ( channel != null )
					channel.close() ;
			}
		} catch (IOException ioe ) {
			return null ;
		}
	}
	
	
	/**
	 * Returns as a SocketAddress (specifically an InetSocketAddress) the
	 * PUBLIC udp endpoint associated with this application.  The public endpoint
	 * is determined through udp communication with an outside party (specifically
	 * the upd_whereami script running on peaceray.com).  Because this outside communication
	 * can cause delays of unspecified length, it is wise to specify a timeout for giving
	 * up (in this case, as numAttempts * delayBetweenAttempts)
	 * 
	 * Why use this function?  Because NAT traversal can cause our externally visible
	 * IP and port number to differ from those we have direct access to.  This method
	 * opens a UDP datagram socket on the provided port (which should match the internal
	 * port desired for communication) and sends "where am I" messages to Peaceray.com.
	 * Peaceray responds with an 8-byte packet containing our public IP and the port number
	 * assigned for communication.  In rare cases, such as when no NAT is used, this will
	 * match the output of getPrivateUDPEndpointAsSocketAddress.
	 * 
	 * This method performs a call to
	 * getPublicUDPEndpointAsSocketAddress( sock, numAttempts, delayBetweenAttempts, servIP, servPort )
	 * using default values.
	 * 
	 * The most obvious use of this method is for UDP NAT hole punching, the first
	 * step of which is to provide the other party (through a mediator) our private
	 * and public endpoints.  Required for this process is the establishment of a UDP
	 * socket using the same port number as is provided to this method.  Because we
	 * used that port to communicate with the WhereAmI server, it may receive packets
	 * which originate from WhereAmI as a delayed response to this method call.  These
	 * packets are very likely to have public source endpoints equivalent to those provided
	 * (or if none are provided, the default values publically readable as static class constants).
	 * At present, all packets sent by WhereAmI begin with the byte 0xff or 0xfe.
	 * It would probably be good practice to check the first byte of any incoming packet.
	 * You should be sure to disregard any packet originating from the WhereAmI service,
	 * since they will be irrelevant to your purposes.  This class provides a public 
	 * isWhereAmIPacket for your convenience.
	 * 
	 * POSTCONDITION: This message has sent 1 or more requests on the provided socket
	 * 			to the WhereAmI server and listened for a WhereAmI response.  If
	 * 			such a response was received in the time alotted, this call returns
	 * 			the external IP address and port number reported by WhereAmI; if not,
	 * 			or some other error occurred, it returns null.
	 * 
	 * 			If this method returns a value, then as a necessary postcondition,
	 * 			datagrams from outside can be received on the provided DatagramSocket
	 * 			You can use this to establish UDP hole punching (but do act quickly!)
	 * 
	 * @param sock		The datagram socket on which to send the request
	 * @param whereAmIServAddress	The address, as hostname or IP address, of the WhereAmI service
	 * @param whereAmIServPort		The port on which the WhereAmIService is running.
	 * @return	'null' if we couldn't contact the whereami server; otherwise, the udp SocketAddress
	 * 			as seen by the whereami server.
	 */
	public static SocketAddress getPublicUDPEndpointAsSocketAddress( DatagramChannel channel,
			String whereAmIServAddress, int whereAmIServPort ) {

		@SuppressWarnings("unused")
		int numAttempts = (DEFAULT_WHEREAMI_TIMEOUT <= 0) ? 0 : (int)Math.ceil(DEFAULT_WHEREAMI_TIMEOUT / DEFAULT_WHEREAMI_RESEND_DELAY) ;
		return getPublicUDPEndpointAsSocketAddress( channel,
					numAttempts, DEFAULT_WHEREAMI_RESEND_DELAY,
					whereAmIServAddress, whereAmIServPort) ;
	}
	
	
	
	/**
	 * Returns as a SocketAddress (specifically an InetSocketAddress) the
	 * PUBLIC udp endpoint associated with this application.  The public endpoint
	 * is determined through udp communication with an outside party (specifically
	 * the upd_whereami script running on peaceray.com).  Because this outside communication
	 * can cause delays of unspecified length, it is wise to specify a timeout for giving
	 * up.
	 * 
	 * Why use this function?  Because NAT traversal can cause our externally visible
	 * IP and port number to differ from those we have direct access to.  This method
	 * opens a UDP datagram socket on the provided port (which should match the internal
	 * port desired for communication) and sends "where am I" messages to Peaceray.com.
	 * Peaceray responds with an 8-byte packet containing our public IP and the port number
	 * assigned for communication.  In rare cases, such as when no NAT is used, this will
	 * match the output of getPrivateUDPEndpointAsSocketAddress.
	 * 
	 * This method performs a call to
	 * getPublicUDPEndpointAsSocketAddress( sock, numAttempts, delayBetweenAttempts, servIP, servPort )
	 * using default values.
	 * 
	 * The most obvious use of this method is for UDP NAT hole punching, the first
	 * step of which is to provide the other party (through a mediator) our private
	 * and public endpoints.  Required for this process is the establishment of a UDP
	 * socket using the same port number as is provided to this method.  Because we
	 * used that port to communicate with the WhereAmI server, it may receive packets
	 * which originate from WhereAmI as a delayed response to this method call.  These
	 * packets are very likely to have public source endpoints equivalent to those provided
	 * (or if none are provided, the default values publically readable as static class constants).
	 * At present, all packets sent by WhereAmI begin with the byte 0xff or 0xfe.
	 * It would probably be good practice to check the first byte of any incoming packet.
	 * You should be sure to disregard any packet originating from the WhereAmI service,
	 * since they will be irrelevant to your purposes.  This class provides a public 
	 * isWhereAmIPacket for your convenience.
	 * 
	 * POSTCONDITION: This method has attempted to open a DatagramSocket to listen
	 * 			on the provided port.  If successful, it sent 1 or more requests
	 * 			to the WhereAmI server and listened for a WhereAmI response.  If
	 * 			such a response was received in the time alotted, this call returns
	 * 			the external IP address and port number reported by WhereAmI; if not,
	 * 			or some other error occurred, it returns null.
	 * 
	 * 			Whatever the result, if a DatagramSocket was successfully opened,
	 * 			then it was nicely closed before this method returned.
	 * 
	 * 			If this method returns a value, then as a necessary postcondition,
	 * 			datagrams from outside can be received on a DatagramSocket using
	 * 			the provided port.  You can use this to establish UDP hole punching
	 * 			(but do act quickly!)
	 * 
	 * @param port		The local port from which to send the whereami request.
	 * @param timeout	The number of milliseconds to wait, in total, before giving up.
	 * 					If 0, never give up, never surrender - this method will block forever,
	 * 					or until it receives a response, whichever comes first.
	 * @return	'null' if we couldn't contact the whereami server; otherwise, the udp SocketAddress
	 * 			as seen by the whereami server.
	 */
	public static SocketAddress getPublicUDPEndpointAsSocketAddress( int port, int timeout ) {
		
		try {
			DatagramChannel channel = null ;
			try {
				channel = DatagramChannel.open() ;
				channel.connect(new InetSocketAddress(port) ) ;
				channel.configureBlocking(true) ;
				return getPublicUDPEndpointAsSocketAddress( channel, timeout ) ;
			} catch ( SocketException e ) {
				return null ;
			} finally {
				// nicely close the socket.
				if ( channel != null )
					channel.close() ;
			}
		} catch (IOException ioe ) {
			return null ;
		}
	}
	
	
	
	/**
	 * Returns as a SocketAddress (specifically an InetSocketAddress) the
	 * PUBLIC udp endpoint associated with this application.  The public endpoint
	 * is determined through udp communication with an outside party (specifically
	 * the upd_whereami script running on peaceray.com).  Because this outside communication
	 * can cause delays of unspecified length, it is wise to specify a timeout for giving
	 * up (in this case, as numAttempts * delayBetweenAttempts)
	 * 
	 * Why use this function?  Because NAT traversal can cause our externally visible
	 * IP and port number to differ from those we have direct access to.  This method
	 * opens a UDP datagram socket on the provided port (which should match the internal
	 * port desired for communication) and sends "where am I" messages to Peaceray.com.
	 * Peaceray responds with an 8-byte packet containing our public IP and the port number
	 * assigned for communication.  In rare cases, such as when no NAT is used, this will
	 * match the output of getPrivateUDPEndpointAsSocketAddress.
	 * 
	 * This method performs a call to
	 * getPublicUDPEndpointAsSocketAddress( sock, numAttempts, delayBetweenAttempts, servIP, servPort )
	 * using default values.
	 * 
	 * The most obvious use of this method is for UDP NAT hole punching, the first
	 * step of which is to provide the other party (through a mediator) our private
	 * and public endpoints.  Required for this process is the establishment of a UDP
	 * socket using the same port number as is provided to this method.  Because we
	 * used that port to communicate with the WhereAmI server, it may receive packets
	 * which originate from WhereAmI as a delayed response to this method call.  These
	 * packets are very likely to have public source endpoints equivalent to those provided
	 * (or if none are provided, the default values publically readable as static class constants).
	 * At present, all packets sent by WhereAmI begin with the byte 0xff or 0xfe.
	 * It would probably be good practice to check the first byte of any incoming packet.
	 * You should be sure to disregard any packet originating from the WhereAmI service,
	 * since they will be irrelevant to your purposes.  This class provides a public 
	 * isWhereAmIPacket for your convenience.
	 * 
	 * POSTCONDITION: This message has sent 1 or more requests on the provided socket
	 * 			to the WhereAmI server and listened for a WhereAmI response.  If
	 * 			such a response was received in the time alotted, this call returns
	 * 			the external IP address and port number reported by WhereAmI; if not,
	 * 			or some other error occurred, it returns null.
	 * 
	 * 			If this method returns a value, then as a necessary postcondition,
	 * 			datagrams from outside can be received on the provided DatagramSocket
	 * 			You can use this to establish UDP hole punching (but do act quickly!)
	 * 
	 * @param sock		The datagram socket on which to send the request
	 * @param timeout	The number of milliseconds before we give up.  If 0, try forever.
	 * @return	'null' if we couldn't contact the whereami server; otherwise, the udp SocketAddress
	 * 			as seen by the whereami server.
	 */
	public static SocketAddress getPublicUDPEndpointAsSocketAddress( DatagramChannel channel, int timeout ) {
		int numAttempts = (timeout <= 0) ? 0 : (int)Math.ceil(timeout / DEFAULT_WHEREAMI_RESEND_DELAY) ;
		return getPublicUDPEndpointAsSocketAddress( channel,
					numAttempts, DEFAULT_WHEREAMI_RESEND_DELAY,
					DEFAULT_WHEREAMI_SERV_ADDRESS, DEFAULT_WHEREAMI_SERV_PORT) ;	
	}
	
	
	/**
	 * Returns as a SocketAddress (specifically an InetSocketAddress) the
	 * PUBLIC udp endpoint associated with this application.  The public endpoint
	 * is determined through udp communication with an outside party (specifically
	 * the upd_whereami script running on peaceray.com).  Because this outside communication
	 * can cause delays of unspecified length, it is wise to specify a timeout for giving
	 * up.
	 * 
	 * Why use this function?  Because NAT traversal can cause our externally visible
	 * IP and port number to differ from those we have direct access to.  This method
	 * opens a UDP datagram socket on the provided port (which should match the internal
	 * port desired for communication) and sends "where am I" messages to Peaceray.com.
	 * Peaceray responds with an 8-byte packet containing our public IP and the port number
	 * assigned for communication.  In rare cases, such as when no NAT is used, this will
	 * match the output of getPrivateUDPEndpointAsSocketAddress.
	 * 
	 * This method performs a call to
	 * getPublicUDPEndpointAsSocketAddress( sock, numAttempts, delayBetweenAttempts, servIP, servPort )
	 * using default values.
	 * 
	 * The most obvious use of this method is for UDP NAT hole punching, the first
	 * step of which is to provide the other party (through a mediator) our private
	 * and public endpoints.  Required for this process is the establishment of a UDP
	 * socket using the same port number as is provided to this method.  Because we
	 * used that port to communicate with the WhereAmI server, it may receive packets
	 * which originate from WhereAmI as a delayed response to this method call.  These
	 * packets are very likely to have public source endpoints equivalent to those provided
	 * (or if none are provided, the default values publically readable as static class constants).
	 * At present, all packets sent by WhereAmI begin with the byte 0xff or 0xfe.
	 * It would probably be good practice to check the first byte of any incoming packet.
	 * You should be sure to disregard any packet originating from the WhereAmI service,
	 * since they will be irrelevant to your purposes.  This class provides a public 
	 * isWhereAmIPacket for your convenience.
	 * 
	 * POSTCONDITION: This method has attempted to open a DatagramSocket to listen
	 * 			on the provided port.  If successful, it sent 1 or more requests
	 * 			to the WhereAmI server and listened for a WhereAmI response.  If
	 * 			such a response was received in the time alotted, this call returns
	 * 			the external IP address and port number reported by WhereAmI; if not,
	 * 			or some other error occurred, it returns null.
	 * 
	 * 			Whatever the result, if a DatagramSocket was successfully opened,
	 * 			then it was nicely closed before this method returned.
	 * 
	 * 			If this method returns a value, then as a necessary postcondition,
	 * 			datagrams from outside can be received on a DatagramSocket using
	 * 			the provided port.  You can use this to establish UDP hole punching
	 * 			(but do act quickly!)
	 * 
	 * @param port		The local port from which to send the whereami request.
	 * @param timeout	The number of milliseconds to wait, in total, before giving up.
	 * 					If 0, never give up, never surrender - this method will block forever,
	 * 					or until it receives a response, whichever comes first.
	 * @param whereAmIServAddress	The address, as hostname or IP address, of the WhereAmI service
	 * @param whereAmIServPort		The port on which the WhereAmIService is running.
	 * @return	'null' if we couldn't contact the whereami server; otherwise, the udp SocketAddress
	 * 			as seen by the whereami server.
	 */
	public static SocketAddress getPublicUDPEndpointAsSocketAddress( int port, int timeout,
			String whereAmIServAddress, int whereAmIServPort) {
		
		try {
			DatagramChannel channel = null ;
			try {
				channel = DatagramChannel.open() ;
				channel.connect(new InetSocketAddress(port) ) ;
				channel.configureBlocking(true) ;
				return getPublicUDPEndpointAsSocketAddress( channel, timeout, whereAmIServAddress, whereAmIServPort ) ;
			} catch ( SocketException e ) {
				return null ;
			} finally {
				// nicely close the socket.
				if ( channel != null )
					channel.close() ;
			}
		} catch (IOException ioe ) {
			return null ;
		}
	}
	
	/**
	 * Returns as a SocketAddress (specifically an InetSocketAddress) the
	 * PUBLIC udp endpoint associated with this application.  The public endpoint
	 * is determined through udp communication with an outside party (specifically
	 * the upd_whereami script running on peaceray.com).  Because this outside communication
	 * can cause delays of unspecified length, it is wise to specify a timeout for giving
	 * up (in this case, as numAttempts * delayBetweenAttempts)
	 * 
	 * Why use this function?  Because NAT traversal can cause our externally visible
	 * IP and port number to differ from those we have direct access to.  This method
	 * opens a UDP datagram socket on the provided port (which should match the internal
	 * port desired for communication) and sends "where am I" messages to Peaceray.com.
	 * Peaceray responds with an 8-byte packet containing our public IP and the port number
	 * assigned for communication.  In rare cases, such as when no NAT is used, this will
	 * match the output of getPrivateUDPEndpointAsSocketAddress.
	 * 
	 * This method performs a call to
	 * getPublicUDPEndpointAsSocketAddress( sock, numAttempts, delayBetweenAttempts, servIP, servPort )
	 * using default values.
	 * 
	 * The most obvious use of this method is for UDP NAT hole punching, the first
	 * step of which is to provide the other party (through a mediator) our private
	 * and public endpoints.  Required for this process is the establishment of a UDP
	 * socket using the same port number as is provided to this method.  Because we
	 * used that port to communicate with the WhereAmI server, it may receive packets
	 * which originate from WhereAmI as a delayed response to this method call.  These
	 * packets are very likely to have public source endpoints equivalent to those provided
	 * (or if none are provided, the default values publically readable as static class constants).
	 * At present, all packets sent by WhereAmI begin with the byte 0xff or 0xfe.
	 * It would probably be good practice to check the first byte of any incoming packet.
	 * You should be sure to disregard any packet originating from the WhereAmI service,
	 * since they will be irrelevant to your purposes.  This class provides a public 
	 * isWhereAmIPacket for your convenience.
	 * 
	 * POSTCONDITION: This message has sent 1 or more requests on the provided socket
	 * 			to the WhereAmI server and listened for a WhereAmI response.  If
	 * 			such a response was received in the time alotted, this call returns
	 * 			the external IP address and port number reported by WhereAmI; if not,
	 * 			or some other error occurred, it returns null.
	 * 
	 * 			If this method returns a value, then as a necessary postcondition,
	 * 			datagrams from outside can be received on the provided DatagramSocket
	 * 			You can use this to establish UDP hole punching (but do act quickly!)
	 * 
	 * @param sock		The datagram socket on which to send the request
	 * @param timeout	The number of milliseconds before we give up.  If 0, try forever.
	 * @param whereAmIServAddress	The address, as hostname or IP address, of the WhereAmI service
	 * @param whereAmIServPort		The port on which the WhereAmIService is running.
	 * @return	'null' if we couldn't contact the whereami server; otherwise, the udp SocketAddress
	 * 			as seen by the whereami server.
	 */
	public static SocketAddress getPublicUDPEndpointAsSocketAddress( DatagramChannel channel, int timeout,
			String whereAmIServAddress, int whereAmIServPort) {
		int numAttempts = (timeout <= 0) ? 0 : (int)Math.ceil(timeout / DEFAULT_WHEREAMI_RESEND_DELAY) ;
		return getPublicUDPEndpointAsSocketAddress( channel,
					numAttempts, DEFAULT_WHEREAMI_RESEND_DELAY,
					whereAmIServAddress, whereAmIServPort) ;
	}
	
	
	
	/**
	 * Returns as a SocketAddress (specifically an InetSocketAddress) the
	 * PUBLIC udp endpoint associated with this application.  The public endpoint
	 * is determined through udp communication with an outside party (specifically
	 * the upd_whereami script running on peaceray.com).  Because this outside communication
	 * can cause delays of unspecified length, it is wise to specify a timeout for giving
	 * up (in this case, as numAttempts * delayBetweenAttempts)
	 * 
	 * Why use this function?  Because NAT traversal can cause our externally visible
	 * IP and port number to differ from those we have direct access to.  This method
	 * opens a UDP datagram socket on the provided port (which should match the internal
	 * port desired for communication) and sends "where am I" messages to Peaceray.com.
	 * Peaceray responds with an 8-byte packet containing our public IP and the port number
	 * assigned for communication.  In rare cases, such as when no NAT is used, this will
	 * match the output of getPrivateUDPEndpointAsSocketAddress.
	 * 
	 * This method performs a call to
	 * getPublicUDPEndpointAsSocketAddress( sock, numAttempts, delayBetweenAttempts, servIP, servPort )
	 * using default values.
	 * 
	 * The most obvious use of this method is for UDP NAT hole punching, the first
	 * step of which is to provide the other party (through a mediator) our private
	 * and public endpoints.  Required for this process is the establishment of a UDP
	 * socket using the same port number as is provided to this method.  Because we
	 * used that port to communicate with the WhereAmI server, it may receive packets
	 * which originate from WhereAmI as a delayed response to this method call.  These
	 * packets are very likely to have public source endpoints equivalent to those provided
	 * (or if none are provided, the default values publically readable as static class constants).
	 * At present, all packets sent by WhereAmI begin with the byte 0xff or 0xfe.
	 * It would probably be good practice to check the first byte of any incoming packet.
	 * You should be sure to disregard any packet originating from the WhereAmI service,
	 * since they will be irrelevant to your purposes.  This class provides a public 
	 * isWhereAmIPacket for your convenience.
	 * 
	 * POSTCONDITION: This method has attempted to open a DatagramSocket to listen
	 * 			on the provided port.  If successful, it sent 1 or more requests
	 * 			to the WhereAmI server and listened for a WhereAmI response.  If
	 * 			such a response was received in the time alotted, this call returns
	 * 			the external IP address and port number reported by WhereAmI; if not,
	 * 			or some other error occurred, it returns null.
	 * 
	 * 			Whatever the result, if a DatagramSocket was successfully opened,
	 * 			then it was nicely closed before this method returned.
	 * 
	 * 			If this method returns a value, then as a necessary postcondition,
	 * 			datagrams from outside can be received on a DatagramSocket using
	 * 			the provided port.  You can use this to establish UDP hole punching
	 * 			(but do act quickly!)
	 * 
	 * @param port		The local port from which to send the whereami request.
	 * @param numAttempts	We will send up to this many requests to the whereami service.
	 * @param delayBetweenAttempts	In milliseconds, the time to wait after each request.
	 * 			Total block time will not exceed approx. numAttempts * delayBetweenAttempts
	 * @return	'null' if we couldn't contact the whereami server; otherwise, the udp SocketAddress
	 * 			as seen by the whereami server.
	 */
	public static SocketAddress getPublicUDPEndpointAsSocketAddress( int port, int numAttempts, int delayBetweenAttempts ) {
		
		try {
			DatagramChannel channel = null ;
			try {
				channel = DatagramChannel.open() ;
				channel.connect(new InetSocketAddress(port) ) ;
				channel.configureBlocking(true) ;
				return getPublicUDPEndpointAsSocketAddress( channel, numAttempts, delayBetweenAttempts ) ;
			} catch ( SocketException e ) {
				return null ;
			} finally {
				// nicely close the socket.
				if ( channel != null )
					channel.close() ;
			}
		} catch (IOException ioe ) {
			return null ;
		}
	}
	
	
	/**
	 * Returns as a SocketAddress (specifically an InetSocketAddress) the
	 * PUBLIC udp endpoint associated with this application.  The public endpoint
	 * is determined through udp communication with an outside party (specifically
	 * the upd_whereami script running on peaceray.com).  Because this outside communication
	 * can cause delays of unspecified length, it is wise to specify a timeout for giving
	 * up (in this case, as numAttempts * delayBetweenAttempts)
	 * 
	 * Why use this function?  Because NAT traversal can cause our externally visible
	 * IP and port number to differ from those we have direct access to.  This method
	 * opens a UDP datagram socket on the provided port (which should match the internal
	 * port desired for communication) and sends "where am I" messages to Peaceray.com.
	 * Peaceray responds with an 8-byte packet containing our public IP and the port number
	 * assigned for communication.  In rare cases, such as when no NAT is used, this will
	 * match the output of getPrivateUDPEndpointAsSocketAddress.
	 * 
	 * This method performs a call to
	 * getPublicUDPEndpointAsSocketAddress( sock, numAttempts, delayBetweenAttempts, servIP, servPort )
	 * using default values.
	 * 
	 * The most obvious use of this method is for UDP NAT hole punching, the first
	 * step of which is to provide the other party (through a mediator) our private
	 * and public endpoints.  Required for this process is the establishment of a UDP
	 * socket using the same port number as is provided to this method.  Because we
	 * used that port to communicate with the WhereAmI server, it may receive packets
	 * which originate from WhereAmI as a delayed response to this method call.  These
	 * packets are very likely to have public source endpoints equivalent to those provided
	 * (or if none are provided, the default values publically readable as static class constants).
	 * At present, all packets sent by WhereAmI begin with the byte 0xff or 0xfe.
	 * It would probably be good practice to check the first byte of any incoming packet.
	 * You should be sure to disregard any packet originating from the WhereAmI service,
	 * since they will be irrelevant to your purposes.  This class provides a public 
	 * isWhereAmIPacket for your convenience.
	 * 
	 * POSTCONDITION: This message has sent 1 or more requests on the provided socket
	 * 			to the WhereAmI server and listened for a WhereAmI response.  If
	 * 			such a response was received in the time alotted, this call returns
	 * 			the external IP address and port number reported by WhereAmI; if not,
	 * 			or some other error occurred, it returns null.
	 * 
	 * 			If this method returns a value, then as a necessary postcondition,
	 * 			datagrams from outside can be received on the provided DatagramSocket
	 * 			You can use this to establish UDP hole punching (but do act quickly!)
	 * 
	 * @param sock		The datagram socket on which to send the request
	 * @param numAttempts	We will send up to this many requests to the whereami service.
	 * @param delayBetweenAttempts	In milliseconds, the time to wait after each request.
	 * 			Total block time will not exceed approx. numAttempts * delayBetweenAttempts
	 * @return	'null' if we couldn't contact the whereami server; otherwise, the udp SocketAddress
	 * 			as seen by the whereami server.
	 */
	public static SocketAddress getPublicUDPEndpointAsSocketAddress( DatagramChannel channel, int numAttempts, int delayBetweenAttempts ) {
		return getPublicUDPEndpointAsSocketAddress( channel,
				numAttempts, delayBetweenAttempts,
				DEFAULT_WHEREAMI_SERV_ADDRESS, DEFAULT_WHEREAMI_SERV_PORT) ;
	}
	
	
	
	
	/**
	 * Returns as a SocketAddress (specifically an InetSocketAddress) the
	 * PUBLIC udp endpoint associated with this application.  The public endpoint
	 * is determined through udp communication with an outside party (specifically
	 * the upd_whereami script running on peaceray.com).  Because this outside communication
	 * can cause delays of unspecified length, it is wise to specify a timeout for giving
	 * up (in this case, as numAttempts * delayBetweenAttempts)
	 * 
	 * Why use this function?  Because NAT traversal can cause our externally visible
	 * IP and port number to differ from those we have direct access to.  This method
	 * opens a UDP datagram socket on the provided port (which should match the internal
	 * port desired for communication) and sends "where am I" messages to Peaceray.com.
	 * Peaceray responds with an 8-byte packet containing our public IP and the port number
	 * assigned for communication.  In rare cases, such as when no NAT is used, this will
	 * match the output of getPrivateUDPEndpointAsSocketAddress.
	 * 
	 * This method performs a call to
	 * getPublicUDPEndpointAsSocketAddress( sock, numAttempts, delayBetweenAttempts, servIP, servPort )
	 * using default values.
	 * 
	 * The most obvious use of this method is for UDP NAT hole punching, the first
	 * step of which is to provide the other party (through a mediator) our private
	 * and public endpoints.  Required for this process is the establishment of a UDP
	 * socket using the same port number as is provided to this method.  Because we
	 * used that port to communicate with the WhereAmI server, it may receive packets
	 * which originate from WhereAmI as a delayed response to this method call.  These
	 * packets are very likely to have public source endpoints equivalent to those provided
	 * (or if none are provided, the default values publically readable as static class constants).
	 * At present, all packets sent by WhereAmI begin with the byte 0xff or 0xfe.
	 * It would probably be good practice to check the first byte of any incoming packet.
	 * You should be sure to disregard any packet originating from the WhereAmI service,
	 * since they will be irrelevant to your purposes.  This class provides a public 
	 * isWhereAmIPacket for your convenience.
	 * 
	 * POSTCONDITION: This method has attempted to open a DatagramSocket to listen
	 * 			on the provided port.  If successful, it sent 1 or more requests
	 * 			to the WhereAmI server and listened for a WhereAmI response.  If
	 * 			such a response was received in the time alotted, this call returns
	 * 			the external IP address and port number reported by WhereAmI; if not,
	 * 			or some other error occurred, it returns null.
	 * 
	 * 			Whatever the result, if a DatagramSocket was successfully opened,
	 * 			then it was nicely closed before this method returned.
	 * 
	 * 			If this method returns a value, then as a necessary postcondition,
	 * 			datagrams from outside can be received on a DatagramSocket using
	 * 			the provided port.  You can use this to establish UDP hole punching
	 * 			(but do act quickly!)
	 * 
	 * @param port		The local port from which to send the whereami request.
	 * @param numAttempts	We will send up to this many requests to the whereami service.
	 * @param delayBetweenAttempts	In milliseconds, the time to wait after each request.
	 * 			Total block time will not exceed approx. numAttempts * delayBetweenAttempts
	 * @param whereAmIServAddress	The address, as hostname or IP address, of the WhereAmI service
	 * @param whereAmIServPort		The port on which the WhereAmIService is running.
	 * @return	'null' if we couldn't contact the whereami server; otherwise, the udp SocketAddress
	 * 			as seen by the whereami server.
	 */
	public static SocketAddress getPublicUDPEndpointAsSocketAddress( int port,
			int numAttempts, int delayBetweenAttempts,
			String whereAmIServAddress, int whereAmIServPort ) {
		
		try {
			DatagramChannel channel = null ;
			try {
				channel = DatagramChannel.open() ;
				channel.connect(new InetSocketAddress(port) ) ;
				channel.configureBlocking(true) ;
				return getPublicUDPEndpointAsSocketAddress( channel, numAttempts, delayBetweenAttempts, whereAmIServAddress, whereAmIServPort ) ;
			} catch ( SocketException e ) {
				return null ;
			} finally {
				// nicely close the socket.
				if ( channel != null )
					channel.close() ;
			}
		} catch (IOException ioe ) {
			return null ;
		}
	}
	
	
	/**
	 * Returns as a SocketAddress (specifically an InetSocketAddress) the
	 * PUBLIC udp endpoint associated with this application.  The public endpoint
	 * is determined through udp communication with an outside party (specifically
	 * the upd_whereami script running on peaceray.com).  Because this outside communication
	 * can cause delays of unspecified length, it is wise to specify a timeout for giving
	 * up (in this case, as numAttempts * delayBetweenAttempts)
	 * 
	 * Why use this function?  Because NAT traversal can cause our externally visible
	 * IP and port number to differ from those we have direct access to.  This method
	 * opens a UDP datagram socket on the provided port (which should match the internal
	 * port desired for communication) and sends "where am I" messages to Peaceray.com.
	 * Peaceray responds with an 8-byte packet containing our public IP and the port number
	 * assigned for communication.  In rare cases, such as when no NAT is used, this will
	 * match the output of getPrivateUDPEndpointAsSocketAddress.
	 * 
	 * The most obvious use of this method is for UDP NAT hole punching, the first
	 * step of which is to provide the other party (through a mediator) our private
	 * and public endpoints.  Required for this process is the establishment of a UDP
	 * socket using the same port number as is provided to this method.  Because we
	 * used that port to communicate with the WhereAmI server, it may receive packets
	 * which originate from WhereAmI as a delayed response to this method call.  These
	 * packets are very likely to have public source endpoints equivalent to those provided
	 * (or if none are provided, the default values publically readable as static class constants).
	 * At present, all packets sent by WhereAmI begin with the byte 0xff or 0xfe.
	 * It would probably be good practice to check the first byte of any incoming packet.
	 * You should be sure to disregard any packet originating from the WhereAmI service,
	 * since they will be irrelevant to your purposes.  This class provides a public 
	 * isWhereAmIPacket for your convenience.
	 * 
	 * POSTCONDITION: This message has sent 1 or more requests on the provided socket
	 * 			to the WhereAmI server and listened for a WhereAmI response.  If
	 * 			such a response was received in the time alotted, this call returns
	 * 			the external IP address and port number reported by WhereAmI; if not,
	 * 			or some other error occurred, it returns null.
	 * 
	 * 			If this method returns a value, then as a necessary postcondition,
	 * 			datagrams from outside can be received on the provided DatagramSocket
	 * 			You can use this to establish UDP hole punching (but do act quickly!)
	 * 
	 * @param sock		The datagram socket on which to send the request
	 * @param numAttempts	We will send up to this many requests to the whereami service.
	 * @param delayBetweenAttempts	In milliseconds, the time to wait after each request.
	 * 			Total block time will not exceed approx. numAttempts * delayBetweenAttempts
	 * @param whereAmIServAddress	The address, as hostname or IP address, of the WhereAmI service
	 * @param whereAmIServPort		The port on which the WhereAmIService is running.
	 * @return	'null' if we couldn't contact the whereami server; otherwise, the udp SocketAddress
	 * 			as seen by the whereami server.
	 */
	public static SocketAddress getPublicUDPEndpointAsSocketAddress( DatagramChannel channel,
			int numAttempts, int delayBetweenAttempts,
			String whereAmIServAddress, int whereAmIServPort ) {
		
		
		try {
			InetAddress servAddress = InetAddress.getByName(whereAmIServAddress) ;
			ByteBuffer requestBuffer = ByteBuffer.allocate(0) ;
			ByteBuffer responseBuffer = ByteBuffer.allocate(32) ;
			
			boolean forever = numAttempts == 0 ;
			while( forever || numAttempts > 0 ) {
				// Each pass through this loop represents a send attempt - send our request,
				// then wait delayBetweenAttempts for a response.  If a message comes in
				// that does not match our expected format, discard it and keep listening.
				
				// Send our request.
				channel.send(requestBuffer, new InetSocketAddress(whereAmIServAddress, whereAmIServPort)) ;
				
				// Listen for the right amount of time.
				long iterStartTime = System.currentTimeMillis() ;
				int delayLeft = delayBetweenAttempts ;
				// We use this to receive again if a bad packet comes in during this loop;
				// we don't want a bad packet to completely reset our delay time.
				while ( delayLeft > 0 ) {
					SocketAddress addr ; 
					responseBuffer.clear() ;
					if ( channel.isBlocking() ) {
						System.err.println("Communication: gutPublicUDP etc. is blocking, setting to non-blocking") ;
						channel.configureBlocking(false) ;
						System.err.println("now blocking: " + channel.isBlocking()) ;
					}
					addr = channel.receive(responseBuffer) ;
					if ( addr == null ) {
						Thread.sleep(5) ;
					}
					responseBuffer.flip() ;
					
					if ( addr != null ) {
						// There appears to be a bug in Android, where even if the DatagramChannel
						// is set to blocking, it acts as non-blocking, immediately returning 'null'.
						// Spin our wheels.
						
						// We have received a packet, but it may not be from
						// the whereami service.  Perform our checks.
						if ( isWhereAmIBuffer( responseBuffer, addr, whereAmIServAddress, whereAmIServPort ) ) {
							// Get the SocketAddress from it.
							return translateWhereAmIBuffer( responseBuffer ) ;
						}
						
						// Otherwise, it's a bad packet.  Ignore, adjust delayLeft, and
						// re-loop to keep listening.  The delay left is the total
						// delay minus the time that has passed since the loop began.
					}
					delayLeft = (int)(delayBetweenAttempts - (System.currentTimeMillis() - iterStartTime)) ;
				}
			}
			
		} catch ( Exception e ) {
			e.printStackTrace() ;	// System.out.print System.err.print  <-- for finding this later
			// Some problem.  We don't really care what, but this takes us
			// out of the loop.
		}
		
		// If we haven't already returned the translated result, return null.
		return null ;
	}
	
	
	/**
	 * Performs a basic examination of the packet, returning 'true' if it appears to
	 * match the format of a response from the default WhereAmI service.
	 * 
	 * This method examines the packet source (using DEFAULT_WHEREAMI_SERV_ADDRESS
	 * and DEFAULT_WHEREAMI_SERV_PORT as expected values) and the packet contents.
	 * 
	 * Contents are not fully examined (we do not verify that it contains valid IP
	 * and port numbers, for example): instead, a very quick byte-check is performed
	 * to see if the message matches expectations.
	 * 
	 * For example, an IPv4 address is reported as an 8-byte response, encoded is
	 * the ones-complemented value [ 0x00 [4 IP address bytes] 0x00 [2 bytes unsigned short] ].
	 * We check that bytes 0 and 5 are equal to 0xFF, the ones-complement of 0.
	 * 
	 * @param packet A DatagramPacket possibly from the WhereAmI service
	 * @return Does the packet appear to be a WhereAmI message?
	 */
	public static boolean isWhereAmIBuffer( ByteBuffer buffer, SocketAddress addr ) {
		return isWhereAmIBuffer( buffer, addr, DEFAULT_WHEREAMI_SERV_ADDRESS, DEFAULT_WHEREAMI_SERV_PORT ) ;
	}
	
	
	/**
	 * Performs a basic examination of the packet, returning 'true' if it appears to
	 * match the format of a response from the default WhereAmI service.
	 * 
	 * This method examines the packet source (using the specified address and
	 * port as expected values) and the packet contents.
	 * 
	 * Contents are not fully examined (we do not verify that it contains valid IP
	 * and port numbers, for example): instead, a very quick byte-check is performed
	 * to see if the message matches expectations.
	 * 
	 * For example, an IPv4 address is reported as an 8-byte response, encoded is
	 * the ones-complemented value [ 0x00 [4 IP address bytes] 0x00 [2 bytes unsigned short] ].
	 * We check that bytes 0 and 5 are equal to 0xFF, the ones-complement of 0.
	 * 
	 * @param packet A DatagramPacket possibly from the WhereAmI service
	 * @param whereAmIServAddress The address at which the WhereAmI service was contacted,
	 * 			either by name or by IP address.
	 * @param whereAmIServPort The port on which WhereAmI was listening.
	 * @return Does the packet appear to be a WhereAmI message?
	 */
	public static boolean isWhereAmIBuffer(
			ByteBuffer buffer, SocketAddress addr, String whereAmIServAddress, int whereAmIServPort ) {
		
		try {
			InetAddress whereAmIInetAddress = InetAddress.getByName(whereAmIServAddress) ;
			SocketAddress whereAmISockAddress = new InetSocketAddress( whereAmIInetAddress, whereAmIServPort ) ;
			
			// First check - did the packet come from this address?
			if ( !whereAmISockAddress.equals( addr ) ) {
				System.err.println("" + whereAmISockAddress + " does not equal " + addr) ;
				return false ;
			}
			
			// Second check - is this an encoded SocketAddress?
			buffer.position(0) ;
			byte [] data = new byte[buffer.remaining()] ;
			buffer.get(data) ;
			boolean isSAddr = ByteArrayOps.isSocketAddressAsBytes(data, 0) ;
			if ( isSAddr )
				System.err.println("Is a socket address") ;
			else
				System.err.println("Is not a socket address") ;
			return ByteArrayOps.isSocketAddressAsBytes(data, 0) ;
			
		} catch( Exception e ) {
			return false ;
		}
	}
	
	/**
	 * Returns the SocketAddress object encoded in the provided WhereAmI packet,
	 * or null if none seems to be there.
	 * 
	 * This is a simple wrapper of ByteArrayOps.readSocketAddressFromBytes that
	 * extracts the packet's byte array and catches the IllegalArgumentException 
	 * if one is thrown (and returns 'null' in that case).
	 * 
	 * @param packet
	 * @return
	 */
	public static SocketAddress translateWhereAmIBuffer( ByteBuffer buffer ) {
		
		// Get the bytes and perform ones-complement.
		buffer.position(0) ;
		byte [] data = new byte[buffer.remaining()] ;
		buffer.get(data) ;
		try {
			return ByteArrayOps.readSocketAddressAsBytes(data, 0) ;
		} catch ( IllegalArgumentException e ) {
			return null ;
		}
	}
	
	
	
	
	//////////////////////////////////////////////////////////////////////////////////
	//
	// UDP HOLE PUNCHING
	//
	// Punches UDP holes in a NAT.
	//
	// ///////////////////////////////////////////////////////////////////////////////
	//
	// These methods avoid endless handshaking in a non-guaranteed delivery environment
	// by following this procedure:
	//
	// Send FF(myNonce) to target at all known addresses.
	// 
	// Having received F*(targetNonce) from an address, note this and change to sending
	// FE(myNonce) to that address.
	//
	// Upon receiving FE(targetNonce), immediately send a few FD(myNonce) packets
	// and terminate, using that address as the returned value.
	//
	// Upon receiving FD(targetNonce), immediately terminate, using that address as the
	// returned value.
	//
	// If we run out of time without receiving FE or FD, but we HAVE received FF,
	// terminate and return the address on which we received it.  We are guaranteed
	// to have sent at least one FE message after receiving it, so hopefully the
	// party on the other end received at least one message from us and knows the
	// connection was formed.
	//
	//////////////////////////////////////////////////////////////////////////////////
	//
	// IN SUMMARY: These methods report a SocketAddress upon apparent success, and
	// null upon apparent failure, but because UDP has no guaranteed delivery they
	// err on the side of assuming success.  Recommended usage is this:
	//
	// connectedAndVerified = false
	// my public address = <whereami>
	// while !connectedAndVerified:
	//		ask mediator for UDP
	//		myNonce, targetNonce <-- mediator provides new values
	//		target address <-- UDP_hole_punching( amountOfDelay )
	//		if target address:
	//			attempt communication.  If successful:
	//				break; 		// CONNECTION IS NOW ESTABLISHED
	//		amountOfDelay++
	//
	//////////////////////////////////////////////////////////////////////////////////
	
	
	public static SocketAddress punchUDPHole( int port, Nonce myNonce, Nonce targetNonce, SocketAddress [] targetAddrs ) {
		try {
			DatagramChannel channel = null ;
			try {
				channel = DatagramChannel.open() ;
				channel.connect( new InetSocketAddress(port) ) ;
				channel.configureBlocking(true) ;
				return punchUDPHole( channel, myNonce, targetNonce, targetAddrs ) ;
			} catch ( SocketException e ) {
				return null ;
			} finally {
				// nicely close the socket.
				if ( channel != null )
					channel.close() ;
			}
		} catch( IOException ioe ) {
			return null ;
		}
	}
	
	
	public static SocketAddress punchUDPHole( DatagramChannel channel, Nonce myNonce, Nonce targetNonce, SocketAddress [] targetAddrs ) {
		@SuppressWarnings("unused")
		int numAttempts = (DEFAULT_UDP_HOLE_PUNCHING_TIMEOUT <= 0) ? 0 : (int)Math.ceil(DEFAULT_UDP_HOLE_PUNCHING_TIMEOUT / DEFAULT_UDP_HOLE_PUNCHING_RESEND_DELAY) ;
		int maxAddressExpansion = 1 ;
		int numAttemptsWithoutAddressExpansion = numAttempts / 2 ;
		return punchUDPHole( channel, myNonce, targetNonce, targetAddrs,
				numAttempts, DEFAULT_UDP_HOLE_PUNCHING_RESEND_DELAY, maxAddressExpansion, numAttemptsWithoutAddressExpansion) ;
	}
	
	public static SocketAddress punchUDPHole( int port, Nonce myNonce, Nonce targetNonce, SocketAddress [] targetAddrs, int timeout ) {
		try {
			DatagramChannel channel = null ;
			try {
				channel = DatagramChannel.open() ;
				channel.connect( new InetSocketAddress(port) ) ;
				channel.configureBlocking(true) ;
				return punchUDPHole( channel, myNonce, targetNonce, targetAddrs, timeout ) ;
			} catch ( SocketException e ) {
				return null ;
			} finally {
				// nicely close the socket.
				if ( channel != null )
					channel.close() ;
			}
		} catch( IOException ioe ) {
			return null ;
		}
	}
	
	
	public static SocketAddress punchUDPHole( DatagramChannel channel, Nonce myNonce, Nonce targetNonce, SocketAddress [] targetAddrs, int timeout ) {
		int numAttempts = (timeout <= 0) ? 0 : (int)Math.ceil(timeout / DEFAULT_UDP_HOLE_PUNCHING_RESEND_DELAY) ;
		int maxAddressExpansion = 1 ;
		int numAttemptsWithoutAddressExpansion = numAttempts / 2 ;
		return punchUDPHole( channel, myNonce, targetNonce, targetAddrs,
				numAttempts, DEFAULT_UDP_HOLE_PUNCHING_RESEND_DELAY, maxAddressExpansion, numAttemptsWithoutAddressExpansion ) ;
	}
	
	
	
	public static SocketAddress punchUDPHole( int port, Nonce myNonce, Nonce targetNonce, SocketAddress [] targetAddrs,
			int numAttempts, int delayBetweenAttempts, int maxAddressExpansion, int numAttemptsWithoutAddressExpansion ) {
		try {
			DatagramChannel channel = null ;
			try {
				channel = DatagramChannel.open() ;
				channel.connect( new InetSocketAddress(port) ) ;
				channel.configureBlocking(true) ;
				return punchUDPHole( channel, myNonce, targetNonce, targetAddrs,
						numAttempts, delayBetweenAttempts, maxAddressExpansion, numAttemptsWithoutAddressExpansion ) ;
			} catch ( SocketException e ) {
				return null ;
			} finally {
				// nicely close the socket.
				if ( channel != null )
					channel.close() ;
			}
		} catch( IOException ioe ) {
			return null ;
		}
	}
	
	
	public static SocketAddress punchUDPHole(
			DatagramChannel channel,
			Nonce myNonce,
			Nonce targetNonce,
			SocketAddress [] targetAddrsIn,
			int numAttemptsIn, int delayBetweenAttempts, int maxAddressExpansion, int numAttemptsWithoutAddressExpansion ) {
		
		//System.err.println("Communication.punchUDPHole : " + numAttemptsIn + " attempts, with delay " + delayBetweenAttempts) ;
		//System.err.println("Communication.punchUDPHole my nonce:" + myNonce + " target nonce:" + targetNonce) ;
		
		int numAddresses = targetAddrsIn.length ;
		SocketAddress [] targetAddrs = new SocketAddress[targetAddrsIn.length + maxAddressExpansion] ;
		for ( int i = 0; i < targetAddrsIn.length; i++ )
			targetAddrs[i] = targetAddrsIn[i] ;
		
		int numAttempts = numAttemptsIn ;
		
		// This is where the magic happens.
		
		// We loop, sending at first 0xFF-prefixed messages containing
		// myNonce+targetNonce, 0xFE-prefixed messages if we have received their
		// nonce, and finally some 0xFD-prefixed message having received
		// a 0xFE-prefix from them.  We terminate immediately after sending
		// or receiving the 0xFD messages.
		
		int myLen = myNonce.lengthAsBytes() ;
		int targetLen = targetNonce.lengthAsBytes() ;
		
		// Buffers have length that is a power of two
		int myBuffLen = 2, targetBuffLen = 2 ;
		while ( myBuffLen < myLen + targetLen + 1 )
			myBuffLen *= 2 ;
		while ( targetBuffLen < targetLen + myLen + 1 )
			targetBuffLen *= 2 ;
		
		ByteBuffer myBuffer = ByteBuffer.allocate(myBuffLen) ;
		ByteBuffer targetBuffer = ByteBuffer.allocate(targetBuffLen) ;
		
		byte [] noncesBuff = new byte[ myLen + targetLen ] ;
		int len = myNonce.writeAsBytes(noncesBuff, 0) ;
		targetNonce.writeAsBytes(noncesBuff, len) ;
		myBuffer.position(1) ;
		myBuffer.put(noncesBuff) ;
		myBuffer.flip() ;
		
		boolean [] receivedOnAddr = new boolean[targetAddrs.length] ;
		
		// Selector!
		SelectionKey selectionKey ;
		try {
			channel.configureBlocking(false) ;
			selectionKey = channel.register(Selector.open(), SelectionKey.OP_READ) ;
		} catch (ClosedChannelException e1) {
			e1.printStackTrace() ;
			return null ;
		} catch (IOException e1) {
			e1.printStackTrace() ;
			return null ;
		}
		
		try {
			boolean forever = numAttempts == 0 ;
			while( forever || numAttempts > 0 ) {
				// Each pass through this loop represents a send attempt - send packets to both address,
				// then wait delayBetweenAttempts for a response.
				
				// Send out packets, one to each address.
				for ( int i = 0; i < numAddresses; i++ ) {
					if ( !receivedOnAddr[i] ) {
						// haven't received their nonce yet; 0xFF prefix
						myBuffer.position(0) ;
						myBuffer.put(ByteArrayOps.BYTE_0xFF).position(0) ;
					}
					else {
						// HAVE received their nonce; 0xFE prefix
						myBuffer.position(0) ;
						myBuffer.put(ByteArrayOps.BYTE_0xFE).position(0) ;
					}
					// send!
					//System.err.println("Communication.punchUDPHole sending attempt " +numAttempts + " to " + targetAddrs[i]) ;
					channel.send(myBuffer, targetAddrs[i]) ;
				}
				
				// Listen for the right amount of time.
				long iterStartTime = System.currentTimeMillis() ;
				int delayLeft = delayBetweenAttempts ;
				// We use this to receive again if a packet comes in that does not
				// finish the UDP hole punch; we don't want a packet to reset 
				// our wait time.
				while ( delayLeft > 0 ) {
					SocketAddress inAddr = null ;
					// DatagramChannels do not obey socket timeouts.
					try {
						// select for a receive.  Stupid use for a Selector, right?  WRONG!
						// Android 2.3 has a SERIOUS bug where timeouts fail on blocking channels,
						// and non-blocking channels block forever!  There is no good way to
						// receive directly if we don't know there is a packet available!
						// Selection is ABSOLUTELY NECESSARY.
						selectionKey.selector().selectNow() ;
						Set<SelectionKey> selectedKeys = selectionKey.selector().selectedKeys();
						Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
						while(keyIterator.hasNext()) { 
							//System.err.println( "Communication.punchUDPHole hasNext" ) ;
						    keyIterator.next();
						    targetBuffer.clear() ;
							inAddr = channel.receive(targetBuffer) ;
							targetBuffer.flip() ;
						    keyIterator.remove();
						}
					} catch( SocketTimeoutException ste ) {
						// Ran out of time.  Decrement number of attepmts, and try again.
						delayLeft = 0 ;
						continue ;
							// This goes to the top of the while, sees that
							// delayLeft is zero, then exits the inner loop.
							// If numAttempts is > 0, or we retry forever,
							// the outer loop will iterate again.
					}
					if ( inAddr == null ) {
						// there is a bug in Android causing .receive on a blocking channel to return 'null'
						// immediately.  Because of this, and the fact that (in real Java) a blocking DatagramChannel
						// does not obey soTimeout, we use non-blocking and manually sleep.
						Thread.sleep(5) ;
						delayLeft = (int)(delayBetweenAttempts - (System.currentTimeMillis() - iterStartTime)) ;
						continue ;
					}
					
					
					// We have received a packet.  Check whether it came from one of
					// the considered addresses.
					//System.err.println( "Communication.punchUDPHole: packet received from "+ inAddr) ;
					boolean fromKnownAddress = false ;
					byte [] b = null ;
					for ( int i = 0; i < numAddresses; i++ ) {
						if ( inAddr.equals( targetAddrs[i] ) ) {
							fromKnownAddress = true ;
							//System.err.println( "Communication.punchUDPHole: packet received matches a target address...") ;
							// Received a packet from address i.  Verify the nonces.
							b = (b == null || targetBuffer.remaining() != b.length) ? new byte[targetBuffer.remaining()] : b ;
							targetBuffer.get(b) ; 
							if ( targetNonce.equals(b, 1) && myNonce.equals(b, 1 + targetNonce.lengthAsBytes()) ) {
								// Looks okay to me.  Note, if we haven't already,
								// that we've received a nonce from this guy.
								receivedOnAddr[i] = true ;
								
								// Send an IMMEDIATE response.  If the prefix code
								// was 0xFF, send 0xFE: received your nonce.
								// If the prefix code was 0xFE, that they have
								// received our nonce, send k copies of a 0xFD-prefixed
								// message (confirmation of nonce receipt) and immediately
								// terminate.  If the prefix code was 0xFD, send
								// nothing, but immediately terminate.
								if ( b[0] == ByteArrayOps.BYTE_0xFF ) {
									//System.err.println( "Communication.punchUDPHole: packet received 0xFF") ;
									myBuffer.position(0) ;
									myBuffer.put(ByteArrayOps.BYTE_0xFE).position(0) ;
									channel.send(myBuffer, targetAddrs[i]) ;
								}
								else if ( b[0] == ByteArrayOps.BYTE_0xFE ) {
									//System.err.println( "Communication.punchUDPHole: packet received 0xFE") ;
									myBuffer.position(0) ;
									myBuffer.put(ByteArrayOps.BYTE_0xFD) ;
									for ( int j = 0; j < DEFAULT_UDP_HOLE_PUNCHING_CONFIRM_MESSAGE_SENDS; j++ ) {
										myBuffer.position(0) ;
										channel.send(myBuffer, targetAddrs[i]) ;
									}
									// now terminate
									selectionKey.cancel() ;
									return targetAddrs[i] ;
								}
								else if ( b[0] == ByteArrayOps.BYTE_0xFD ) {
									//System.err.println( "Communication.punchUDPHole: packet received 0xFD") ;
									// they have confirmed, meaning they have terminated their
									// loop.  Terminate now.
									selectionKey.cancel() ;
									return targetAddrs[i] ;
								}
							}
						}
					}
					
					if ( !fromKnownAddress ) {
						// we don't know this address.  Possibly we wanto expand our address list
						// and handle this packet, but only if the time is right.
						if ( numAddresses < targetAddrs.length && numAttemptsIn - numAttempts >= numAttemptsWithoutAddressExpansion ) {
							// EXPAND!  ...if this message matches what we need.
							b = (b == null || targetBuffer.remaining() != b.length) ? new byte[targetBuffer.remaining()] : b ;
							targetBuffer.get(b) ; 
							if ( targetNonce.equals(b, 1) && myNonce.equals(b, 1 + targetNonce.lengthAsBytes()) ) {
								// Looks okay to me.  Add this address to our list.
								int index = numAddresses ;
								numAddresses++ ;
								targetAddrs[index] = inAddr ;
								
								// Note that we've received a nonce from this guy.
								receivedOnAddr[index] = true ;
								// Send an IMMEDIATE response.  If the prefix code
								// was 0xFF, send 0xFE: received your nonce.
								// If the prefix code was 0xFE, that they have
								// received our nonce, send k copies of a 0xFD-prefixed
								// message (confirmation of nonce receipt) and immediately
								// terminate.  If the prefix code was 0xFD, send
								// nothing, but immediately terminate.
								if ( b[0] == ByteArrayOps.BYTE_0xFF ) {
									//System.err.println( "Communication.punchUDPHole: packet received 0xFF") ;
									myBuffer.position(0) ;
									myBuffer.put(ByteArrayOps.BYTE_0xFE).position(0) ;
									channel.send(myBuffer, targetAddrs[index]) ;
								}
								else if ( b[0] == ByteArrayOps.BYTE_0xFE ) {
									//System.err.println( "Communication.punchUDPHole: packet received 0xFE") ;
									myBuffer.position(0) ;
									myBuffer.put(ByteArrayOps.BYTE_0xFD) ;
									for ( int j = 0; j < DEFAULT_UDP_HOLE_PUNCHING_CONFIRM_MESSAGE_SENDS; j++ ) {
										myBuffer.position(0) ;
										channel.send(myBuffer, targetAddrs[index]) ;
									}
									// now terminate
									selectionKey.cancel() ;
									return targetAddrs[index] ;
								}
								else if ( b[0] == ByteArrayOps.BYTE_0xFD ) {
									//System.err.println( "Communication.punchUDPHole: packet received 0xFD") ;
									// they have confirmed, meaning they have terminated their
									// loop.  Terminate now.
									selectionKey.cancel() ;
									return targetAddrs[index] ;
								}
							}
						}
					}
					
					// Otherwise, it's a bad packet.  Ignore, adjust delayLeft, and
					// re-loop to keep listening.  The delay left is the total
					// delay minus the time that has passed since the loop began.
					delayLeft = (int)(delayBetweenAttempts - (System.currentTimeMillis() - iterStartTime)) ;
				}
				
				numAttempts-- ;
			}
			
		} catch ( Exception e ) {
			e.printStackTrace() ;	// System.out.print System.err.print  <-- for finding this later
			// Some problem.  We don't really care what, but this takes us
			// out of the loop.
		}
		
		// Maybe we received a packet, but couldn't complete the handshake?  In 
		// this case we optimistically to use the address.
		for ( int i = 0; i < targetAddrs.length; i++ )
			if ( receivedOnAddr[i] )
				return targetAddrs[i] ;
		
		// If we haven't already returned the address, return null.
		if ( selectionKey != null )
			selectionKey.cancel() ;
		return null ;
	}
	
}
