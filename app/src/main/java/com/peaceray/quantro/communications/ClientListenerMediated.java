package com.peaceray.quantro.communications;

import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;
import com.peaceray.quantro.communications.wrapper.AutonomousWrappedSocketFactory;
import com.peaceray.quantro.utils.ThreadSafety;

/**
 * A mediated client listener requires an active connection to a mediator,
 * which we communicate with through a server.  We allow only one mediator
 * per ClientListenerMediated, which is defined by an address, port, and
 * a special mediator nonce.
 * 
 * We do not accept incoming sockets using standard socket methods.  Instead,
 * we listen on the mediator socket for specific instructions - either to
 * open a pass-through socket (the mediator provides port, address, and special
 * nonce to provide when setting up the socket) or using TCP hole-punching.  In
 * either case, we then pass the thread to a client interrogation thread for normal
 * interrogation.
 * 
 * @author Jake
 *
 */
public class ClientListenerMediated extends ClientListener {

	
	class ClientListenerMediatedThread extends Thread {
		public static final String TAG = "ClientListenerMediatedThread" ;
		
		ClientListenerMediated listener ;
		
		boolean listening ;
		
		ServerSocket ss ;
		
		CoordinationMessage out_m ;
		
		public ClientListenerMediatedThread( ClientListenerMediated listener ) {
			this.listener = listener ;
			
			listening = true ;
			
			ss = null ;
		}
		
		
		@Override
		public void run() {
			out_m = new CoordinationMessage() ;
			// We attempt to reopen mediator sockets as long as we're listening.
			while ( listening ) {
				
				Socket mediatorSocket = null ;
				OutputStream mediatorOS = null ;
				MessageReader mediatorMR = null ;
				
				while ( listening ) {
					try {
						Log.d(TAG, "Opening mediator socket to " + listener.mediatorAddress + ":" + listener.mediatorPort) ;
						mediatorSocket = new Socket() ;
						mediatorSocket.connect( new InetSocketAddress( listener.mediatorAddress, listener.mediatorPort ), 2000 ) ;	// 2-second timeout
						mediatorOS = mediatorSocket.getOutputStream() ;
						mediatorMR = new MessageReader( new CoordinationMessage(), mediatorSocket.getInputStream() ) ;
						mediatorMR.okToReadNextMessage() ;
						break ;
					} catch ( Exception e ) {
						
						// Close stuff.
						try {
							mediatorMR.stop();
						} catch ( Exception ex ) {}
						try {
							mediatorSocket.close();
						} catch ( Exception ex ) {}
					}
					
					// Brief delay before we reattempt
					try {
						Thread.sleep(100) ;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace() ;
					}
				}
				
				Log.d(TAG,"socket open to mediator") ;
				
				// Now that we have a mediator connection, we listen for instructions.
				while ( listening ) {
					int status = mediatorMR.status();
					if ( status == MessageReader.STATUS_MESSAGE_READY ) {
						CoordinationMessage m = (CoordinationMessage) mediatorMR.getMessage() ;
						if ( !handleMessage( m, mediatorOS ) ) {
							// Something failed.  Disconnect?
							break ;
						}
						mediatorMR.okToReadNextMessage() ;
					}
					else if ( status == MessageReader.STATUS_STOPPED
							|| status == MessageReader.STATUS_INPUT_SOURCE_BROKEN
							|| status == MessageReader.STATUS_INPUT_SOURCE_NULL
							|| status == MessageReader.STATUS_UNSPECIFIED_ERROR ) {
						// Connection is broken.
						break ;
					}
					
					// Brief delay before we reattempt
					try {
						Thread.sleep(100) ;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace() ;
					}
				}
				
				// Having reached here, it's likely that something went wrong
				// (or 'listening' was set to false).  Either way, close up our connections.
				try {
					mediatorSocket.close();
				} catch( Exception e ) { }
				try {
					mediatorMR.stop();
				} catch( Exception e ) { }
			}
		}
		
		
		public boolean handleMessage( CoordinationMessage m, OutputStream os ) {
			
			boolean hasOutgoing = false;
			switch( m.getType() ) {
			case Message.TYPE_NONCE_REQUEST:
				Log.d(TAG, "received nonce request") ;
				out_m.setAsNonce(listener.mediatorNonce) ;
				hasOutgoing = true ;
				break ;
				
			case Message.TYPE_SERVER_CLOSING:
				Log.d(TAG, "received server closing") ;
				// Okeydokey.  Disconnect and try reconnecting.
				return false ;
				
			case Message.TYPE_SERVER_CLOSING_FOREVER:
				Log.d(TAG, "received server closing forever") ;
				// Whoops!
				listening = false ;
				return false ;
				
			case Message.TYPE_PERSONAL_NONCE_REQUEST:
				Log.d(TAG, "received personal nonce request") ;
				// Send our personal nonce.
				out_m.setAsPersonalNonce(-1, listener.hostPersonalNonce) ;
				hasOutgoing = true ;
				break ;
				
			case Message.TYPE_NAME_REQUEST:
				// We don't have a name!
				Log.d(TAG, "received name request") ;
				out_m.setAsMyName("") ;
				hasOutgoing = true ;
				break ;
				
			/*
			case CoordinationMessage.TYPE_OPEN_PASS_THROUGH:
				Log.d(TAG, "received open pass-through") ;
				// The mediator wants us to open a pass-through socket. 
				// It has specified the port, address and nonce we should
				// use when opening it.
				new PassThroughOpenerThread( listener, 5000,	// at most 5 seconds allowed for pass-through setup.
						m.getAddress(), m.getPort(), m.getNonce() ).start() ;
				break ;
				
			case CoordinationMessage.TYPE_ATTEMPT_TCP_HOLE_PUNCH:
				Log.d(TAG, "received attempt hole punch") ;
				// The mediator wants us to attempt a TCP-hole punch.
				// It has specified the port, address and targetID for
				// use in mediating the hole punch attempt.
				// TODO: Launch a TCP-hole punch thread
				
				break ;
			*/
			case CoordinationMessage.TYPE_PING:
				Log.d(TAG, "received ping") ;
				// Send a pong.
				out_m.setAsPong();
				hasOutgoing = true ;
				break ;
			}
			
			// Do we have an outgoing message?
			if ( hasOutgoing ) {
				try {
					out_m.write(os) ;
				} catch( Exception e ) {
					return false ;
				}
			}
			
			return true ;
		}
	}
	
	
	
	/**
	 * A thread whose purpose is to open a socket to a specified
	 * location, and hang on to it until it is declared
	 * (by the one we're connected to) as a pass-through socket.
	 * This thread has an absolute time-out, and can be terminated prematurely.
	 * @author Jake
	 *
	 */
	class PassThroughOpenerThread extends Thread {
		
		private static final String TAG = "PassThroughOpenerThread" ;
 		
		WeakReference<ClientListener> listener ;
		long absoluteTimeAvailable ;
		
		String address ;
		int port ;
		Nonce nonce ;
		
		public boolean running ;
		
		public boolean hasPassedThrough ;
		
		CoordinationMessage out_m ;
		
		public PassThroughOpenerThread( ClientListener listener, long timeAvailable,
				String address, int port, Nonce nonce ) {
			this.listener = new WeakReference<ClientListener>( listener ) ;
			this.absoluteTimeAvailable = timeAvailable ;
			this.address = address ;
			this.port = port ;
			this.nonce = nonce ;
			
			running = true ;
			hasPassedThrough = false ;
		}
		
		@Override
		public void run() {
			long startTime = System.currentTimeMillis() ;
			out_m = new CoordinationMessage() ;
			
			Socket sock = null ;
			OutputStream os = null ;
			MessageReader mr = null ;
			
			while( !hasPassedThrough && running && startTime + absoluteTimeAvailable > System.currentTimeMillis() ) {
				
				while ( !hasPassedThrough && running && startTime + absoluteTimeAvailable > System.currentTimeMillis() && listener.get() != null ) {
					sock = null ;
					os = null ;
					mr = null ;
					
					try {
						Log.d(TAG, "Attempting to open new pass-through connection to " + address + ":" + port) ;
						sock = new Socket( ) ;
						sock.connect( new InetSocketAddress(address, port), 2000 ) ;	// 2 second timeout
						os = sock.getOutputStream() ;
						mr = new MessageReader( new CoordinationMessage(), sock.getInputStream() ) ;
						mr.okToReadNextMessage() ;
						break ;
					} catch( Exception e ) {
						e.printStackTrace() ;
						try{
							sock.close();
						} catch( Exception ex ) { }
						try {
							mr.stop();
						} catch( Exception ex ) { }
					}
					
					// Sleep a bit before the next attempt.
					try {
						Thread.sleep(100) ;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				// Either we've stopped (because running is false or we ran out of time)
				// or we have successfully opened a socket.
				while ( !hasPassedThrough && running && startTime + absoluteTimeAvailable > System.currentTimeMillis() && listener.get() != null ) {
					int status = mr.status() ;
					if ( status == MessageReader.STATUS_MESSAGE_READY ) {
						CoordinationMessage m = (CoordinationMessage) mr.getMessage() ;
						if ( !handleMessage( m, sock, os ) ) {
							// Something failed.  Disconnect?
							break ;
						}
						if ( !hasPassedThrough )
							mr.okToReadNextMessage() ;
					}
					else if ( status == MessageReader.STATUS_STOPPED
							|| status == MessageReader.STATUS_INPUT_SOURCE_BROKEN
							|| status == MessageReader.STATUS_INPUT_SOURCE_NULL
							|| status == MessageReader.STATUS_UNSPECIFIED_ERROR ) {
						// Connection is broken.
						break ;
					}
					
					// Brief delay before we reattempt
					try {
						Thread.sleep(100) ;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace() ;
					}
				}
				
				// Close connections.  If we passed through, we don't close the socket.
				if ( !hasPassedThrough ) {
					try {
						sock.close();
					} catch( Exception e ) { }
				}
				try {
					mr.stop();
				} catch( Exception e ) { }
			}
		}
		
		
		public boolean handleMessage( CoordinationMessage m, Socket sock, OutputStream os ) {
			ClientListener cl = listener.get() ;
			if ( cl == null )
				return false ;
			
			boolean hasOutgoing = false;
			switch( m.getType() ) {
			case Message.TYPE_NONCE_REQUEST:
				Log.d(TAG,"received nonce request") ;
				out_m.setAsNonce(this.nonce) ;
				hasOutgoing = true ;
				break ;
				
			case Message.TYPE_SERVER_CLOSING:
				Log.d(TAG,"received server closing") ;
				// Okeydokey.  Disconnect and try reconnecting.
				return false ;
				
			case Message.TYPE_SERVER_CLOSING_FOREVER:
				Log.d(TAG,"received server closing forever") ;
				// Whoops!
				running = false ;
				return true ;
				
			case Message.TYPE_PERSONAL_NONCE_REQUEST:
				Log.d(TAG,"received personal nonce request") ;
				// Send our host nonce
				out_m.setAsPersonalNonce(-1, ((ClientListenerMediated)cl).hostPersonalNonce) ;
				hasOutgoing = true ;
				break ;
				
			case Message.TYPE_NAME_REQUEST:
				// We don't have a name!
				Log.d(TAG,"received name request") ;
				out_m.setAsMyName("") ;
				hasOutgoing = true ;
				break ;
				
			case CoordinationMessage.TYPE_BECOME_PASS_THROUGH:
				// This is now a pass-through socket.  Pass it to a client
				// interrogation thread.
				Log.d(TAG,"received become pass-through") ;
				hasPassedThrough = true ;
				WrappedSocket wsock = AutonomousWrappedSocketFactory.wrap(sock) ;
				if ( wsock != null  ) {
					new ClientListenerClientInterrogationThread(cl, wsock).start();
					return true ;
				}
				else {
					return false ;
				}
				
			case CoordinationMessage.TYPE_PING:
				// Send a pong.
				out_m.setAsPong();
				hasOutgoing = true ;
				break ;
			}
			
			// Do we have an outgoing message?
			if ( hasOutgoing ) {
				try {
					out_m.write(os) ;
				} catch( Exception e ) {
					return false ;
				}
			}
			
			return true ;
		}
		
	}
	
	
	
	
	// We do most of our operation in a thread.
	ClientListenerMediatedThread thread ;
	
	Nonce hostPersonalNonce ;
	
	String mediatorAddress ;
	int mediatorPort ;
	Nonce mediatorNonce ;
	
	public ClientListenerMediated( long minTime, long maxTime, Nonce hostPersonalNonce,
			String mediatorAddress, int mediatorPort, Nonce mediatorNonce ) {
		super( minTime, maxTime ) ;
		
		this.hostPersonalNonce = hostPersonalNonce ;
		
		this.mediatorAddress = mediatorAddress ;
		this.mediatorPort = mediatorPort ;
		this.mediatorNonce = mediatorNonce ;
		
		thread = null ;
	}
	
	
	@Override
	public boolean startListening(Nonce nonce, Nonce personalNonce, AcceptsClientSockets acs) {
		if ( personalNonce == null ) {
			if ( this.hasTarget( nonce ) )
				return false ;
			this.addNonceToTargets(nonce, acs) ;
		}
		else {
			if ( this.hasSpecializedTarget(nonce, personalNonce) )
				return false ;
			this.addSpecializedTarget(nonce, personalNonce, acs) ;
		}
		
		if ( thread == null ) {
			thread = new ClientListenerMediatedThread( this ) ;
			thread.start() ;
		}
		
		return true ;
	}

	@Override
	public boolean isListening(Nonce nonce, Nonce personalNonce) {
		if ( personalNonce == null )
			return this.hasTarget( nonce ) && thread != null ;
		else
			return this.hasSpecializedTarget(nonce, personalNonce) ;
	}

	@Override
	public boolean stopListening(Nonce nonce, Nonce personalNonce) {
		if ( personalNonce == null ) {
			if ( !this.hasTarget( nonce ) )
				return false ;
			this.removeNonceFromTargets(nonce) ;
		}
		else {
			if ( !this.hasSpecializedTarget( nonce, personalNonce ) )
				return false ;
			this.removeFromSpecializedTargets(nonce, personalNonce) ;
		}
		
		
		if ( this.numberOfTargets() == 0 && this.numberOfSpecializedTargets() == 0 ) {
			thread.listening = false ;
			if ( thread.ss != null ) {
				try{
					thread.ss.close() ;
				} catch( Exception e ) {
				}
			}
			ThreadSafety.waitForThreadToTerminate(thread) ;
			thread = null ;
		}
		return true ;
	}

	@Override
	public void stopListening() {
		if ( thread != null ) {
			thread.listening = false ;
			if ( thread.ss != null ) {
				try{
					thread.ss.close() ;
				} catch( Exception e ) {
				}
			}
			ThreadSafety.waitForThreadToTerminate(thread) ;
			
			thread = null ;
			removeAllTargets() ;
			removeAllSpecializedTargets() ;
		}
	}

}
