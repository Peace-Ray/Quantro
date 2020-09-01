package com.peaceray.quantro.communications.messagepassing;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import com.peaceray.quantro.communications.ClientInformation;
import com.peaceray.quantro.communications.CoordinationMessage;
import com.peaceray.quantro.communications.MessageReader;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;

public class ClientInterrogatorThread extends Thread {
	
	public interface ClientInterrogatorThreadLauncher {
		
		/**
		 * Called when an interrogator thread finishes its work to pass the
		 * wrapped socket back with its client information.  Returns whether we've accepted
		 * the client socket.  If 'true', some other object now has responsibility
		 * for closing the socket.  If 'false', we will attempt to close the socket ourselves.
		 */
		public boolean takeClientSocket(WrappedSocket wsock, ClientInformation ci) ;
		
	}
		
	//private static final String TAG = "ClientListenerClientInterrogationThread" ;
	
	ClientInterrogatorThreadLauncher launcher ;
	WrappedSocket socket ;
	long startTime ;
	long minInterrogationTime ;
	long maxInterrogationTime ;
	
	public ClientInterrogatorThread (ClientInterrogatorThreadLauncher launcher, WrappedSocket socket, long minTime, long maxTime ){
		this.launcher = launcher ;
		this.socket = socket ;
		this.minInterrogationTime = minTime ;
		this.maxInterrogationTime = maxTime ;
	}
	
	@Override
	public void run() {
		startTime = System.currentTimeMillis() ;
		
		// Attempt to get a straight answer out of this socket.
		MessageReader mr ;
		WritableByteChannel wbc ;
		CoordinationMessage out_m ;
		CoordinationMessage in_m ;
		
		boolean failed = false ;
		ClientInformation ci = new ClientInformation() ;
		
		try {
			ReadableByteChannel rbc = socket.getSourceChannel();
			if ( rbc == null )
				System.err.println("ClientInterrogatorThread: couldn't get WrappedSocket readable byte channel!") ;
			mr = new MessageReader( new CoordinationMessage(), socket.getSourceChannel() ) ;
			wbc = socket.getSinkChannel() ;
			out_m = new CoordinationMessage() ;
			
			// REFACTORING: we spam our interrogation messages, then wait for responses.
			// This should speed up our interogation significantly.
			out_m.setAsNonceRequest() ;
			failed = failed || !send(out_m, wbc) ;
			out_m.setAsPersonalNonceRequest() ;
			failed = failed || !send(out_m, wbc) ;
			out_m.setAsNameRequest(0) ;
			failed = failed || !send(out_m, wbc) ;
			out_m.setAsInterrogationComplete() ;
			failed = failed || !send(out_m, wbc) ;
			
			System.out.println("data sent.  Failed:" + failed) ;
			
			// Retrieve nonce, pnonce, name.
			if ( !failed ) {
				in_m = this.getResponse(mr) ;
				if ( in_m == null ) {
					failed = true ;
				}
				else {
					ci.setNonce(in_m.getNonce()) ;
					System.out.println("got nonce") ;
				}
			}
			if ( !failed ) {
				in_m = this.getResponse(mr) ;
				if ( in_m == null ) {
					failed = true ;
				}
				else {
					ci.setPersonalNonce(in_m.getNonce()) ;
					System.out.println("got personal nonce") ;
				}
			}
			if ( !failed ) {
				in_m = this.getResponse(mr) ;
				if ( in_m == null ) {
					failed = true ;
				}
				else {
					ci.setName(in_m.getName()) ;
					System.out.println("got name") ;
				}
			}
			
			mr.stop();
			
			// Busy wait until min time
			long curTime = System.currentTimeMillis() ;
			long millisLeft = minInterrogationTime - (curTime - startTime) ;
			if ( millisLeft > 0) {
				try {
					Thread.sleep(millisLeft) ;
				} catch( Exception e ) {
					e.printStackTrace() ;
					failed = true ;
				}
			}
			
			if ( !failed ) {
				// pass it in.
				if ( !failed && !this.launcher.takeClientSocket(socket, ci) ) {
					System.err.println("ClientInterrogatorThread: launcher did not take socket; closing") ;
					wbc.close() ;
					this.socket.close() ;
				}
			}

			if ( failed ) {
				System.err.println("ClientInterrogatorThread: failed; closing") ;
				wbc.close() ;
				this.socket.close() ;
			}
		} catch ( Exception e ) {
			e.printStackTrace() ;
			return ;
		}
	}
	
	public boolean send( CoordinationMessage m, WritableByteChannel wbc ) {
		try {
			m.write(wbc) ;
			return true ;
		} catch( Exception e ) {
			return false ;
		}
	}
	
	public CoordinationMessage getResponse( MessageReader mr ) {
		try {
			mr.okToReadNextMessage() ;
			
			// Wait for the response.
			while( !mr.messageReady() && System.currentTimeMillis() - startTime < maxInterrogationTime ) {
				// sleep
				//System.out.println("ClientInterrogatorThread waiting for response.  mr status is " + mr.status()) ;
				Thread.sleep(50) ;
			}
			if ( !mr.messageReady() ) {
				return null ;
			}
			
			return (CoordinationMessage) mr.getMessage() ;
		} catch ( Exception e ) {
			return null ;
		}
	}
}
