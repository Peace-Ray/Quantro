package com.peaceray.quantro.communications.messagepassing.matchseeker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.AdministratedWrappedSocketAdministrator;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;
import com.peaceray.quantro.communications.wrapper.AutonomousWrappedSocketFactory;
import com.peaceray.quantro.utils.Communication;
import com.peaceray.quantro.utils.Debug;


/**
 * The 2nd stage of a MatchMaking connection.  The matchedSocketMaker takes
 * the information from a match ( local and remote addresses, etc. ) and
 * performs UDP hole-punching on it.
 * 
 * When this is done, it provides a listener with a WrappedSocket - or an error
 * message, depending.
 * 
 * We make only one UDP hole-punch attempt; we can only afford to make one,
 * because we don't have a way of getting new attempt nonces.
 * 
 * Our attempt nonces come from the hash of localUserID + "+" + localRequestID.
 * 
 * @author Jake
 *
 */
public class MatchedSocketMaker extends Thread {
	
	private static final boolean DEBUG_LOG = true && Debug.LOG ;
	private static final String TAG = "MatchedSocketMaker" ;
	
	private static final void log(String str) {
		if ( DEBUG_LOG ) {
			System.out.println(TAG + " : " + str) ;
		}
	}
	
	private static final void log(Exception e, String str) {
		if ( DEBUG_LOG ) {
			System.err.println(TAG + " : " + str + " : exception below") ;
			e.printStackTrace() ;
		}
	}
	

	public interface Listener {
		public void msml_success( MatchedSocketMaker msm, WrappedSocket ws ) ;
		
		public void msml_failure( MatchedSocketMaker msm ) ;
	}
	
	
	Listener mListener ;
	
	// here's the information passed in
	Nonce mAttemptNonceLocal ;
	Nonce mAttemptNonceRemote ;
	
	// userIDs, requestIDs.
	String mLocalUserID, mRemoteUserID ;
	String mLocalRequestID, mRemoteRequestID ;
	
	int mLocalPort ;
	SocketAddress [] mRemoteAddresses ;
	
	boolean mRunning = true ;
	
	DatagramChannel mDChannel ;
	
	AdministratedWrappedSocketAdministrator mAdministrator ;
	Class<?> mAdministratorMessageClass ;
	
	
	/**
	 * We need to know a bunch of information to proceed.
	 */
	public MatchedSocketMaker(
			Listener listener,
			String localUserID, String localRequestID, int localPort, 
			String remoteUserID, String remoteRequestID, SocketAddress [] remoteAddress,
			AdministratedWrappedSocketAdministrator admin,
			Class<?> adminMessageClass ) {
		
		mListener = listener ;
		
		mLocalUserID = localUserID ;
		mLocalRequestID = localRequestID ;
		mRemoteUserID = remoteUserID ;
		mRemoteRequestID = remoteRequestID ;
		
		mAttemptNonceLocal = Nonce.newNonceAsStringHash(
				localUserID + "+" + localRequestID, 32) ;
		mAttemptNonceRemote = Nonce.newNonceAsStringHash(
				remoteUserID + "+" + remoteRequestID, 32) ;
		
		mLocalPort = localPort ;
		mRemoteAddresses = new SocketAddress[remoteAddress.length] ;
		for ( int i = 0; i < mRemoteAddresses.length; i++ )
			mRemoteAddresses[i] = remoteAddress[i] ;
		
		mDChannel = null ;
		
		mAdministrator = admin ;
		mAdministratorMessageClass = adminMessageClass ;
		
		mRunning = true ;
	}
	
	
	public MatchedSocketMaker(
			Listener listener,
			String localUserID, String localRequestID, DatagramChannel dChannel,
			String remoteUserID, String remoteRequestID, SocketAddress [] remoteAddress,
			AdministratedWrappedSocketAdministrator admin,
			Class<?> adminMessageClass ) {
		
		mListener = listener ;
		
		mLocalUserID = localUserID ;
		mLocalRequestID = localRequestID ;
		mRemoteUserID = remoteUserID ;
		mRemoteRequestID = remoteRequestID ;
		
		mAttemptNonceLocal = Nonce.newNonceAsStringHash(
				localUserID + "+" + localRequestID, 32) ;
		mAttemptNonceRemote = Nonce.newNonceAsStringHash(
				remoteUserID + "+" + remoteRequestID, 32) ;
		
		mLocalPort = -1 ;
		mRemoteAddresses = new SocketAddress[remoteAddress.length] ;
		for ( int i = 0; i < mRemoteAddresses.length; i++ )
			mRemoteAddresses[i] = remoteAddress[i] ;
		
		mDChannel = dChannel ;
		
		mAdministrator = admin ;
		mAdministratorMessageClass = adminMessageClass ;
		
		mRunning = true ;
	}
	
	
	public String getLocalUserID() { return mLocalUserID ; }
	public String getLocalRequestID() { return mLocalRequestID ; }
	public String getRemoteUserID() { return mRemoteUserID ; }
	public String getRemoteRequestID() { return mRemoteRequestID ; }
	
	
	
	public void run() {
		try {
			// make a UDP attempt!
			WrappedSocket ws = null ;
			try {
				if ( mRunning ) {
					if ( mDChannel == null ) {
						SocketAddress myAddr = new InetSocketAddress( mLocalPort ) ;
						
						mDChannel = DatagramChannel.open() ;
						// bind
						mDChannel.socket().bind(myAddr) ;
					}
					
					//System.err.println("MatchedSocketMaker: attempting UDP hole punch with channel " + mDChannel) ;
					//System.err.print("MatchedSocketMaker: addresses:") ;
					//for ( int i = 0; i < mRemoteAddresses.length; i++ )
					//	System.err.print(" " + mRemoteAddresses[i]) ;
					//System.err.println() ;
					//System.err.println("MatchedSocketMaker: nonces " + mAttemptNonceLocal + ", " + mAttemptNonceRemote) ;
						
					log("Attempting UDP hole punch") ;
					SocketAddress udpAddr = Communication.punchUDPHole(
							mDChannel, mAttemptNonceLocal, mAttemptNonceRemote,
							mRemoteAddresses) ;
					log("Hole punch ended with udpAddr " + udpAddr) ;
					
					//System.err.println("MatchedSocketMaker: after UDP hole punch, has socket " + udpAddr) ;
					
					// if non-null, we have punched a hole through to udpAddr.
					if ( udpAddr != null ) {
						// We use an 8-byte prefix.  Prefix is defined as the first 4 bytes
						// of each nonce, concatenated.  We put the small nonce first.
						byte [] prefix = new byte[8] ;
						Nonce [] nonces = mAttemptNonceLocal.compareTo(mAttemptNonceRemote) < 0
								? new Nonce[]{ mAttemptNonceLocal, mAttemptNonceRemote }
								: new Nonce[]{ mAttemptNonceRemote, mAttemptNonceLocal } ;
						for ( int i = 0; i < 4; i++ ) {
							prefix[i] 	= nonces[0].directByteAccess(i) ;
							prefix[i+4]	= nonces[1].directByteAccess(i) ;
						}
						
						if ( mAdministrator == null )
							ws = AutonomousWrappedSocketFactory.wrap(mDChannel, udpAddr, prefix) ;
						else
							ws = mAdministrator.wrap(mAdministratorMessageClass, mDChannel, udpAddr, prefix) ;
					}
				}
			} catch( Exception e ) {
				e.printStackTrace() ;
				if ( ws != null ) {
					try {
						ws.close() ;
					} catch( Exception e2 ) { }
				}
				ws = null ;
				if ( mDChannel != null ) {
					try {
						mDChannel.close() ;
					} catch (IOException ioe) { }
				}
			}
			
			if ( !mRunning || ws == null ) {
				if ( ws != null ) {
					try {
						ws.close() ;
					} catch( Exception e2 ) { }
				}
				if ( mDChannel != null ) {
					try {
						mDChannel.close() ;
					} catch (IOException ioe) { }
				}
				
				try {
					mListener.msml_failure(this) ;
				} catch( Exception e ) { e.printStackTrace() ; }
			}
			else
				mListener.msml_success(this, ws) ;
		} finally {
			// null everything that matters
			mListener = null ;
			SocketAddress [] mRemoteAddresses = null;
			
			DatagramChannel mDChannel = null ;
			
			mAdministrator = null ;
		}
	}
	
	
	public void halt() {
		mRunning = false ;
		if ( mDChannel != null ) {
			try {
				mDChannel.close() ;
			} catch (IOException e) { }
		}
	}
	
}
