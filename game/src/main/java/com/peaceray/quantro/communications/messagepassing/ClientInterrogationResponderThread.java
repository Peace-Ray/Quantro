package com.peaceray.quantro.communications.messagepassing;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import com.peaceray.quantro.communications.CoordinationMessage;
import com.peaceray.quantro.communications.MessageReader;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;


/**
 * A counterpart to ClientInterrogatorThread; the ClientInterrogationResponderThread
 * will answer exactly the interrogation questions asked by the ClientInterrogatorThread.
 * For now, the two classes should be considered linked, and any changes made to one
 * should be reflected by the other.
 * 
 * At this moment the interrogation goes like this:
 * 
 * Nonce request 		->
 * 						<-			Nonce
 * PersonalNonceRequest ->
 * 						<-			Personal nonce
 * Name request			-> 		
 * 						<- 			Name
 * InterrogationComplete->
 * 
 * and that's it.  Any change in message order, or messages sent,
 * should be reflected in BOTH files.
 * 
 * Why add this, when the users of a ClientConnection are perfectly capable
 * of answering these questions?  Because recent changes to LobbyClientCommunications
 * and LobbyCoordinator have required that a "I Am Host" / "I Am Client" message be
 * the first thing sent upon "Connection."
 * 
 * @author Jake
 *
 */
public class ClientInterrogationResponderThread extends Thread {
	
	private MessagePassingConnectionBecomesClient mpcbc ;
	private WrappedSocket socket ;
	
	long startTime ;
	
	int maxInterrogationTime ;
	
	
	public ClientInterrogationResponderThread( MessagePassingConnectionBecomesClient mpc, WrappedSocket socket, int maxTime ) {
		this.mpcbc = mpc ;
		this.socket = socket ;
		this.maxInterrogationTime = maxTime ;
	}
	
	
	@Override
	public void run() {
		startTime = System.currentTimeMillis() ;
		
		// Answer all the questions we receive from this socket.
		MessageReader mr ;
		WritableByteChannel wbc ;
		CoordinationMessage out_m ;
		CoordinationMessage in_m ;
		
		boolean failed = false ;
		boolean success = false ;
		
		try {
			ReadableByteChannel rbc = socket.getSourceChannel();
			if ( rbc == null )
				System.err.println("ClientInterrogationResponderThread: couldn't get WrappedSocket source channel!") ;
			mr = new MessageReader( new CoordinationMessage(), rbc ) ;
			wbc = socket.getSinkChannel() ;
			out_m = new CoordinationMessage() ;
			
			// Loop until out of time.
			while ( !success && !failed && System.currentTimeMillis() - this.startTime < this.maxInterrogationTime ) {
				in_m = getMessage( mr ) ;
				// returns 'null' if out of time; otherwise returns a message.
				if ( in_m == null ) {
					failed = true ;
					break ;
				}
				
				// We respond to only 4 message types; for the rest, we shut down in failure.
				switch( in_m.getType() ) {
				case CoordinationMessage.TYPE_NONCE_REQUEST:
					out_m.setAsNonce( mpcbc.getNonce() ) ;
					failed = failed || !send( out_m, wbc ) ;
					break ;
					
				case CoordinationMessage.TYPE_PERSONAL_NONCE_REQUEST:
					out_m.setAsPersonalNonce( 0, mpcbc.getLocalPersonalNonce() ) ;
					failed = failed || !send( out_m, wbc ) ;
					break ;
					
				case CoordinationMessage.TYPE_NAME_REQUEST:
					out_m.setAsMyName( mpcbc.getLocalName() ) ;
					failed = failed || !send( out_m, wbc ) ;
					break ;
					
				case CoordinationMessage.TYPE_INTERROGATION_COMPLETE:
					// Done!  Whether or not we become client, time to exit this loop.
					success = true ;
					break ;
					
				default:
					// unknown message type
					failed = true ;
					break ;
				}
			}
			
			// Done.  No matter what, we stop teh message reader.
			mr.stop();
			
			// Now report to the mpcbc whether we succeeded or failed.  Did I spell that right?
			if ( success ) {
				boolean didTake = false ;
				try {
					didTake = mpcbc.becomeClient(this, socket) ;
				} catch( Exception e ) { }
				if ( !didTake ) {
					System.err.println("ClientInterrogationResponderThread: Did NOT TAKE!") ;
					try {
						socket.close() ;
					} catch( Exception e ) { }
				}
			}
			else {
				try {
					socket.close() ;
				} catch( Exception e ) { }
				try {
					mpcbc.failedToBecomeClient(this) ;
				} catch( Exception e ) { }
			}
			
			// done.
			return ;
			
		} catch( Exception e ) {
			e.printStackTrace() ;
			try {
				socket.close() ;
			} catch (Exception e2 ){ }
			try {
				mpcbc.failedToBecomeClient(this) ;
			}  catch (Exception e2 ){ }
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
	
	public CoordinationMessage getMessage( MessageReader mr ) {
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
