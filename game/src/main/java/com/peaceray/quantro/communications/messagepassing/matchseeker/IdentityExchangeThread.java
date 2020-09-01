package com.peaceray.quantro.communications.messagepassing.matchseeker;

import java.nio.channels.WritableByteChannel;
import java.util.concurrent.BlockingQueue;

import com.peaceray.quantro.communications.MatchmakingMessage;
import com.peaceray.quantro.communications.Message;
import com.peaceray.quantro.communications.MessageReader;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;
import com.peaceray.quantro.utils.Debug;


/**
 * A simpler, non-client/server based interrogation thread.  This thread
 * is based on the idea that both parties already know the nonce.  Each
 * sends the other their personal nonce and name.
 * 
 * @author Jake
 *
 */
public class IdentityExchangeThread extends Thread {
	
	private static final boolean DEBUG_LOG = true && Debug.LOG ;
	private static final String TAG = "IdentityExchangeThread" ;
	
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
	
	/**
	 * We notify the listener when we're finished.
	 * @author Jake
	 *
	 */
	public interface Listener {
		
		public void ietl_informationReceived( IdentityExchangeThread thread, WrappedSocket ws, Nonce personalNonce, String name ) ;
		
		public void ietl_informationNotReceived( IdentityExchangeThread thread ) ;
		
	}
	
	private Listener mListener ;
	private WrappedSocket mSocket ;
	
	long mStartTime ;
	
	int mMaxCommunicationTime ;
	
	Nonce mNonce ;
	Nonce mLocalPersonalNonce ;
	String mLocalName ;
	Nonce mRemotePersonalNonce ;
	String mRemoteName ;
	
	boolean mRunning ;
	
	String mTag ;
	
	
	public IdentityExchangeThread( Listener listener, WrappedSocket socket,
			Nonce nonce, Nonce personalNonce, String name ) {
		
		this( listener, socket, nonce, personalNonce, name, 10000 ) ;
	}
	
	
	public IdentityExchangeThread( Listener listener, WrappedSocket socket,
			Nonce nonce, Nonce personalNonce, String name,
			int maxCommunicationTime ) {
		
		mListener = listener ;
		mSocket = socket ;
		mNonce = nonce ;
		mLocalPersonalNonce = personalNonce ;
		mLocalName = name ;
		mRemotePersonalNonce = null ;
		mRemoteName = null ;
		
		mMaxCommunicationTime = maxCommunicationTime ;
		
		mRunning = true ;
	}
	
	
	public String getTag() { return mTag ; }
	public void setTag( String tag ) { mTag = tag ; }
	
	
	private class SocketIO {
		WrappedSocket mWrappedSocket ;
		
		// read/write for byte-aware
		MessageReader mMessageReader ;
		WritableByteChannel mWBC ;
		
		// read/write for message-aware
		BlockingQueue<Message> mSendQueue ;
		BlockingQueue<Message> mReceiveQueue ;
		
		public SocketIO( WrappedSocket ws ) {
			mWrappedSocket = ws ;
			if ( mWrappedSocket.isObjectAware(Message.class) ) {
				mSendQueue = (BlockingQueue<Message>)mWrappedSocket.getDataObjectSinkQueue() ;
				mReceiveQueue = (BlockingQueue<Message>)mWrappedSocket.getDataObjectSourceQueue() ;
			} else {
				mWBC = mWrappedSocket.getSinkChannel() ;
				mMessageReader = new MessageReader(
						newMessageInstance(),
						mWrappedSocket.getSourceChannel() ) ;
			}
		}
		
		public void stop() {
			try {
				if ( mMessageReader != null )
					mMessageReader.stop() ;
			} catch( Exception e ) { }
		}
		
		public Message newMessageInstance() {
			if ( mWrappedSocket.isObjectAware(Message.class) ) {
				return (Message) mWrappedSocket.getDataObjectEmptyInstance() ;
			} else {
				return new MatchmakingMessage() ;
			}
		}
		
		public boolean send( Message m ) {
			if ( mWBC != null ) {
				try {
					m.write(mWBC) ;
					return true ;
				} catch( Exception e ) {
					return false ;
				}
			} else {
				Message mSend = (Message)mWrappedSocket.getDataObjectEmptyInstance() ;
				mSend.setAs(m) ;
				mSendQueue.add(mSend) ;
				mWrappedSocket.dataObjectAvailable() ;
				return !mWrappedSocket.isClosed() ;
			}
		}
		
		public Message getMessage() {
			if ( mMessageReader != null ) {
				try {
					mMessageReader.okToReadNextMessage() ;
					
					// Wait for the response.
					while( !mMessageReader.messageReady() && System.currentTimeMillis() - mStartTime < mMaxCommunicationTime ) {
						// sleep
						//System.out.println("ClientInterrogatorThread waiting for response.  mr status is " + mr.status()) ;
						Thread.sleep(50) ;
					}
					if ( !mMessageReader.messageReady() ) {
						return null ;
					}
					
					return (Message) mMessageReader.getMessage() ;
				} catch ( Exception e ) {
					return null ;
				}
			} else {
				try {
					while( System.currentTimeMillis() - mStartTime < mMaxCommunicationTime ) {
						if ( mReceiveQueue.size() > 0 )
							return mReceiveQueue.poll() ;
						Thread.sleep(50) ;
					}
					return null ;
				} catch ( Exception e ) {
					return null ;
				}
			}
		}
	}
	
	
	@Override
	public void run() {
		// We send 2 messages: name and personal nonce.  Upon receipt of
		// these two messages, we tell the listener.  If we end up waiting 
		// longer than maxCommunicationTime, we close the thread.
		
		mStartTime = System.currentTimeMillis(); 
		
		boolean failed = false ;
		
		try {
			log("Preparing Ident exchange") ;
			SocketIO socketIO = new SocketIO( mSocket ) ;
			Message out_m = socketIO.newMessageInstance() ;
			
			// Send messages!
			if ( !failed ) {
				log("sending name...") ;
				out_m.setAsMyName( mLocalName ) ;
				failed = failed || !socketIO.send(out_m) ;
			}
			
			if ( !failed ) {
				log("sending personal nonce...") ;
				out_m.setAsPersonalNonce( 0, mLocalPersonalNonce ) ;
				failed = failed || !socketIO.send(out_m) ;
			}
			
			if ( failed ) {
				try {
					log("failed; reason unknown") ;
					mSocket.close() ;
					socketIO.stop() ;
				} catch (Exception e2 ){ }
				mListener.ietl_informationNotReceived(this) ;
				return ;
			}
			
			while( mRunning && ( mRemoteName == null || mRemotePersonalNonce == null ) ) {
				if ( System.currentTimeMillis() - mStartTime > mMaxCommunicationTime ) {
					try {
						log("timeout; closing") ;
						mSocket.close() ;
						socketIO.stop() ;
					} catch (Exception e2 ){ }
					mListener.ietl_informationNotReceived(this) ;
					return ;
				}
				
				Message in_m = socketIO.getMessage() ;
				if ( in_m == null ) {
					try {
						log("got null message (timeout?)") ;
						mSocket.close() ;
					} catch (Exception e2 ){ }
					mListener.ietl_informationNotReceived(this) ;
					return ;
				}
				
				switch ( in_m.getType() ) {
				case Message.TYPE_MY_NAME:
					log("received name") ;
					mRemoteName = in_m.getName() ;
					break ;
					
				case Message.TYPE_PERSONAL_NONCE:
					log("received pnonce") ;
					mRemotePersonalNonce = in_m.getNonce() ;
					break ;
				}
			}
			
			// Done.  No matter what, we stop teh message reader.
			log("stopping socketIO with success") ;
			socketIO.stop() ;
			
			if ( mRunning )
				mListener.ietl_informationReceived(this, mSocket, mRemotePersonalNonce, mRemoteName) ;
		} catch( Exception e ) {
			log(e, "exception during exchange") ;
			try {
				mSocket.close() ;
			} catch (Exception e2 ){ }
			try {
				mListener.ietl_informationNotReceived(this) ;
			} catch (Exception e2 ){ }
		} finally {
			mSocket = null ;
			mListener = null ;
		}
	}
	
	
	public void halt() {
		mRunning = false ;
		if( mSocket != null ) {
			try {
				mSocket.close() ;
			} catch( Exception e ) { }
		}
	}
	
}
