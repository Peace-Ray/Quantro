package com.peaceray.quantro.communications.messagepassing;

import java.net.SocketAddress;
import java.util.ArrayList;

import com.peaceray.quantro.communications.Message;
import com.peaceray.quantro.communications.nonce.Nonce;

/**
 * The MessagePassingPairedConnection is intended to simplify the expansive
 * communication code that appears to be the source of numerous hard-to-trace
 * bugs.
 * 
 * This class represents a purely "local" connection.  MPPCs are created in
 * pairs, as the name implies, and function as a permanently linked end-to-end
 * connection.  However, the ends of this connection can be activated,
 * deactivate, connected, disconnected, etc. w.r.t. the other end.  In every
 * way a particular end of this connection can be treated as a typical
 * remote connection; aside from construction, there is no need for users to
 * be aware of the specifics of local communication.
 * 
 * Internally, MPPCs each contain an ArrayList of Message instances, which are
 * instantiated as-needed to represent a queue.  These Messages are rotated
 * through as below:
 * 
 * queue0	<- 	queue1 	<-	queue2 	<-	queue3	<--
 * 	|											  |
 * 	--------->	currentlyRead ---------------------
 * 
 * In other words, instances are "popped" from the front of the ArrayList
 * when getMessage is called, and are placed back at the end of the list
 * when the next message is retrieved.  At no time are the Messages serialized
 * or transferred as raw bytes, removing the need for any dedicated 'Read Threads.'
 * 
 * The particular Message subclass is determined the first time either
 * end of the paired connection sends a message.  We store a reference
 * to its class so that new instances can be generated, and any future
 * message is checked against the class to verify it fits the model
 * and 'setAs,' which is used extensively, can still function.
 * 
 * We assume that the ends of the connection will be accessed by different
 * threads.  To that end, we use a single shared
 * synchronization object between the pairs; 'synchronized' private methods
 * and 'synchronized' blocks on individual instance vars (different vars
 * for the two paired objects) could easily result in deadlocks.
 * 
 * 'synchronized' public methods are OK, but are probably not needed
 * if mLock is used appropriately.
 * 
 * Here are the rules for synchronizing instance variable access:
 * 
 * mIncomingMessageQueue: synchronize reads/writes
 * mCurrentMessage: only touched by this object
 * mMessageClass: only touched by this object
 * 
 * mDelegate: synchronize read/writes
 * 
 * mStatus: synchronize read/writes.  This is important in the
 * 		case of establishing connections.  e.g., if one partner
 * 		is told to 'deactivate', it will check that it is not
 * 		connected and then deactivate.  If simultaneously told
 * 		to 'connect', the other partner will check both status
 * 		and then attempt to move them to a 'connected' status
 * 		state.  It is easy to see how the 'deactivate' method
 * 		needs to synchronize its entire operation to avoid
 * 		potential race-conditions.
 * 
 * 
 * 
 * @author Jake
 *
 */
public class MessagePassingPairedConnection extends MessagePassingConnection {
	
	// PAIRING
	private Object mLock ;	
	private MessagePassingPairedConnection mPartner ;
	
	// MESSAGES
	private ArrayList<Message> mIncomingMessageQueue ;
	private int mIncomingMessageQueueLength ;
	private Message mCurrentMessage ;
	private Class<?> mMessageClass ;
	
	// DELEGATE
	private Delegate mDelegate ;
	
	// STATUS
	private Status mStatus ;
	
	// ADDITIONAL INFORMATION
	private Nonce mNonce ;
	private Nonce mLocalPersonalNonce ;
	private Nonce mRemotePersonalNonce ;
	private String mLocalName ;
	private String mRemoteName ;
	
	// CONSTRUCTOR
	private MessagePassingPairedConnection(
			Nonce nonce,
			Nonce localPersonalNonce, String localName,
			Nonce remotePersonalNonce, String remoteName ) {
		
		mLock = null ;
		mPartner = null ;
		mIncomingMessageQueue = new ArrayList<Message>() ;
		mIncomingMessageQueueLength = 0 ;
		mCurrentMessage = null ;
		mMessageClass = null ;
		mDelegate = null ;
		mStatus = Status.INACTIVE ;
		
		mNonce = nonce ;
		mLocalPersonalNonce = localPersonalNonce ;
		mRemotePersonalNonce = remotePersonalNonce ;
		mLocalName = localName ;
		mRemoteName = remoteName ;
	}
	
	
	// These are provided purely as a convenience.  There is no strict
	// "client/server" model in a paired Connection.  However, using
	// these indices allows consistent access to a length-2 array
	// of mppcs.
	public static final int PAIR_INDEX_CS_CLIENT = 0 ;
	public static final int PAIR_INDEX_CS_SERVER = 1 ;
	
	
	// FACTORY METHOD
	public static MessagePassingPairedConnection [] newPair(
			Nonce nonce, Nonce [] personalNonces, String [] names ) {
		
		MessagePassingPairedConnection [] pair = new MessagePassingPairedConnection[2] ;
		newPair( pair, nonce, personalNonces, names ) ;
		return pair ;
	}
	
	public static void newPair(
			MessagePassingPairedConnection [] pair,
			Nonce nonce, Nonce [] personalNonces, String [] names ) {
		
		// Allocate MPPCs
		for ( int i = 0; i < 2; i++ ) {
			pair[i] = new MessagePassingPairedConnection(
					nonce,
					personalNonces[i], names[i],
					personalNonces[(i+1)%2], names[(i+1)%2] ) ;
		}
		
		// Link them together by providing a lock object and references
		// to each other.
		Object obj = new Object() ;
		for ( int i = 0; i < 2; i++ ) {
			pair[i].mLock = obj ;
			pair[i].mPartner = pair[(i+1)%2] ;
		}
	}
	

	@Override
	public void setDelegate(Delegate delegate) {
		synchronized ( mLock ) {
			mDelegate = delegate ;
		}
	}

	
	/**
	 * Activates this connection, which must not be currently active.
	 * 
	 * @throws IllegalStateException if the connection is already active.
	 */
	@Override
	public void activate() throws IllegalStateException {
		synchronized( mLock ) {
			if ( mStatus != Status.INACTIVE )
				throw new IllegalStateException("Cannot activate an active MPPC") ;

			mStatus = Status.NEVER_CONNECTED ;
		}
	}

	@Override
	public void deactivate() throws IllegalStateException {
		synchronized( mLock ) {
			if ( mStatus == Status.INACTIVE )
				throw new IllegalStateException("Cannot deactivate an inactive MPPC") ;
			
			if ( mStatus == MessagePassingConnection.Status.PENDING
					|| mStatus == MessagePassingConnection.Status.CONNECTED
					|| mStatus == MessagePassingConnection.Status.BROKEN
					|| mStatus == MessagePassingConnection.Status.PEER_DISCONNECTED
					|| mStatus == MessagePassingConnection.Status.FAILED )
				throw new IllegalStateException("Connected, or at least not disconnected.") ;
			
			// Deactivate.  We don't need to inform our partner, as we were not connected
			// to them.
			mStatus = Status.INACTIVE ;
		}
	}

	@Override
	public boolean isActive() {
		// partner will never alter the result of this comparison
		return mStatus != Status.INACTIVE ;
	}
	

	@Override
	public void connect() throws IllegalStateException {
		synchronized( mLock ) {
			// Whoops!
			if ( mStatus == MessagePassingConnection.Status.PENDING
					|| mStatus == MessagePassingConnection.Status.CONNECTED
					|| mStatus == MessagePassingConnection.Status.BROKEN
					|| mStatus == MessagePassingConnection.Status.PEER_DISCONNECTED
					|| mStatus == MessagePassingConnection.Status.FAILED )
				throw new IllegalStateException("Connected, or at least not disconnected.") ;
			if ( mStatus == MessagePassingConnection.Status.INACTIVE )
				throw new IllegalStateException("Must activate before connecting") ;
			
			// transition to 'pending' status.
			mStatus = MessagePassingConnection.Status.PENDING ;
			
			// if partner is ALSO pending, it is time to connect.
			// note that we both should make delegate calls.
			// This should also be done inside the mLock synchronization.
			// We don't want any crazy-ass threading issues here.
			if ( mPartner.mStatus == MessagePassingConnection.Status.PENDING ) {
				// HEY GUESS WHAT!
				mStatus = MessagePassingConnection.Status.CONNECTED ;
				mPartner.mStatus = MessagePassingConnection.Status.CONNECTED ;
				
				// tell delegate(s).  The first call might alter the status of this
				// or our partner, so re-check before second call.
				if ( mDelegate != null )
					mDelegate.mpcd_messagePassingConnectionDidConnect(this) ;
				if ( mPartner.mDelegate != null && mPartner.mStatus == Status.CONNECTED )
					mPartner.mDelegate.mpcd_messagePassingConnectionDidConnect(mPartner) ;
			}
		}
	}

	@Override
	public void disconnect() throws IllegalStateException {
		synchronized ( mLock ) {
			if ( mStatus == Status.INACTIVE )
				throw new IllegalStateException("Cannot disconnect an inactive MPPC") ;
			if ( mStatus == MessagePassingConnection.Status.NEVER_CONNECTED
					|| mStatus == MessagePassingConnection.Status.DISCONNECTED )
				throw new IllegalStateException("Can't disconnect a non-connected MPPC.") ;
			
			// Remember, disconnecting empties the message queue.
			mIncomingMessageQueueLength = 0 ;
			
			// if connected, this is complicated: we need to actually disconnect, which
			// is relevant to both MPPCs.  For any other status, things are relatively
			// simple; we just change our state.  There is no delegate call for that,
			// since this is an external event.
			if ( mStatus != Status.CONNECTED ) {
				// easy peasy lemon squeezy.
				mStatus = Status.DISCONNECTED ;
				return ;
			}
			
			// Okay, now the hard part.  Transition THIS to disconnected, and
			// the partner to PEER_DISCONNECTED.  We have no delegate call, but
			// our partner does.  We first call PeerDisconnected, then DoneReceivingMessages.
			// Note that the first delegate call might make changes, so re-check
			// status and delegate existence before the second call.
			mStatus = Status.DISCONNECTED ;
			mPartner.mStatus = Status.PEER_DISCONNECTED ;
			
			// First call...
			if ( mPartner.mDelegate != null )
				mPartner.mDelegate.mpcd_messagePassingConnectionDidDisconnectByPeer(mPartner) ;
			// Recheck for second call...
			if ( mPartner.mDelegate != null && mPartner.mStatus == Status.PEER_DISCONNECTED )
				mPartner.mDelegate.mpcd_messagePassingConnectionDoneReceivingMessages(mPartner) ;
		}
	}

	@Override
	public Status connectionStatus() {
		// no point in synchronizing a single read operation; this is atomic.
		return mStatus ;
	}

	@Override
	public boolean hasMessage() throws IllegalStateException {
		// partner will never transition us out of these states, so no reason to synchronize.
		if ( mStatus == Status.INACTIVE || mStatus == Status.NEVER_CONNECTED )
			throw new IllegalArgumentException("Inactive or never connected") ;
		// atomic.
		return mIncomingMessageQueueLength > 0 ;
	}

	@Override
	public boolean moreMessages() throws IllegalStateException {
		if ( mStatus == Status.INACTIVE )
			throw new IllegalArgumentException("Inactive") ;
		
		// There are more messages if we have any messages now, or the connection
		// is pending or connected.  Be sure to perform these checks inside a lock;
		// I think they are relatively atomic, but it never hurts to be sure.
		synchronized( mLock ) {
			return mStatus == Status.PENDING || mStatus == Status.CONNECTED
					|| mIncomingMessageQueueLength > 0 ;
		}
	}

	@Override
	public Message getMessage() throws IllegalStateException {
		// synchronize for safety; we will be touching mIncomingMessageQueue.
		synchronized( mLock ) {
			if ( mStatus == Status.INACTIVE )
				throw new IllegalArgumentException("Inactive") ;
			if ( mStatus == Status.NEVER_CONNECTED
					|| mStatus == Status.PENDING
					|| mStatus == Status.DISCONNECTED )
				throw new IllegalArgumentException("Disconnected or never connected") ;
			
			if (  mIncomingMessageQueueLength == 0 )
				throw new IllegalStateException("No message available - did hasMessage() return true?") ;
			
			// Everything seems okay.  This is where we cycle our messages.  If
			// mCurrentMessage is not null, add it to the queue at the end
			// (do NOT increment messageQueueLength).
			//
			// Then pop (.remove()) the first item in the array list into
			// mCurrentMessage, and decrement the queue length.  Finally,
			// return mCurrentMessage.
			
			if ( mCurrentMessage != null )
				mIncomingMessageQueue.add(mCurrentMessage) ;
			
			mCurrentMessage = mIncomingMessageQueue.remove(0) ;
			mIncomingMessageQueueLength-- ;
			
			// Autoupdate remote name?
			if ( mCurrentMessage.getType() == Message.TYPE_MY_NAME )
				this.mRemoteName = mCurrentMessage.getName() == null ?
						this.mRemoteName : mCurrentMessage.getName() ;
			
			// We won't touch this reference until the next time getMessage
			// is called, fulfilling the guarantees made by MessagePassingConnection.
			return mCurrentMessage ;
		}
	}

	@Override
	public boolean sendMessage(Message m) throws IllegalStateException {
		synchronized( mLock ) {
			// can only send messages when connected.  However, it is NOT
			// an IllegalState error if we were once connected but are
			// no longer (assuming the user did not disconnect this end);
			// we simply fail to send.
			if ( mStatus == Status.INACTIVE )
				throw new IllegalArgumentException("Inactive") ;
			if ( mStatus == Status.NEVER_CONNECTED
					|| mStatus == Status.PENDING
					|| mStatus == Status.DISCONNECTED )
				throw new IllegalArgumentException("Disconnected or never connected") ;
			
			if ( mStatus != Status.CONNECTED )
				return false ;
			
			// Time to 'send.'  We do this by adding the message to our
			// partner's message queue, and then calling its appropriate
			// delegate method.
			
			// There is some bookkeeping to take care of first.  Check
			// the class of this message instance.  If this is our first
			// Message, keep a record of its class; if not, compare its
			// class against that stored.
			if ( mMessageClass == null ) {
				mMessageClass =  m.getClass() ;
				mPartner.mMessageClass = m.getClass() ;
			} else {
				if ( mMessageClass != m.getClass() )
					throw new IllegalArgumentException("Provided Message has class " + m.getClass() + "; expecting class " + mMessageClass) ;
			}
			
			// All right, all seems well.  Copy this message into the end of the
			// PARTNER's queue.  We may need to allocate a new instance for this.
			if ( mPartner.mIncomingMessageQueueLength == mPartner.mIncomingMessageQueue.size() ) {
				try {
					mPartner.mIncomingMessageQueue.add( (Message)(mMessageClass.newInstance()) ) ;
				} catch( IllegalAccessException iae ) {
					iae.printStackTrace() ;
					throw new IllegalArgumentException("Failed allocating a new Message instance of subclass " + mMessageClass) ;
				} catch (InstantiationException e) {
					e.printStackTrace();
					throw new IllegalArgumentException("Failed allocating a new Message instance of subclass " + mMessageClass) ;
				}
			}
			
			// If we get here, we are transmitting 'm'.  If it's a MyName, update our
			// local name.
			if ( m.getType() == Message.TYPE_MY_NAME && m.getName() != null )
				mLocalName = m.getName() ;
			
			// Now copy.
			mPartner.mIncomingMessageQueue.get(mPartner.mIncomingMessageQueueLength).setAs(m) ;
			mPartner.mIncomingMessageQueueLength++ ;
			
			// Okay, inform the partner's delegate.  This is our last operation.
			if ( mPartner.mDelegate != null )
				mPartner.mDelegate.mpcd_messagePassingConnectionDidReceiveMessage(mPartner) ;
			
			return true ;
		}
	}

	@Override
	public Nonce getNonce() {
		return mNonce ;
	}

	@Override
	public Nonce getLocalPersonalNonce() {
		return mLocalPersonalNonce ;
	}

	@Override
	public String getLocalName() {
		return mLocalName ;
	}

	@Override
	public Nonce getRemotePersonalNonce() {
		return mRemotePersonalNonce ;
	}

	@Override
	public String getRemoteName() {
		return mRemoteName ;
	}
	
	@Override
	public SocketAddress getRemoteSocketAddress() {
		// there is no meaning to this; we connect locally,
		// on the same device.
		return null ;
	}

}
