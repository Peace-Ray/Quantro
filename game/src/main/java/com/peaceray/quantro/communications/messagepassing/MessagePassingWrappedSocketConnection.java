package com.peaceray.quantro.communications.messagepassing;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.BlockingQueue;

import com.peaceray.quantro.communications.Message;
import com.peaceray.quantro.communications.MessageReader;
import com.peaceray.quantro.communications.MultipleMessageReader;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;
import com.peaceray.quantro.communications.wrapper.WrappedSocket.DataObjectSenderReceiver;
import com.peaceray.quantro.utils.Debug;



/**
 * This abstract class, a direct subclass of MessagePassingConnection, implements
 * most of its abstract methods under one assumption: the connection itself is
 * in the form of a WrappedSocket.
 * 
 * We don't handle actually establishing the WrappedSocket connection, so this class
 * does not implement activate(), deactivate(), or connect().  We can only disconnect()
 * a currently connected WrappedSocket, and send/receive messages on it.
 * 
 * Subclassing this class:
 * 		In addition to implementing the missing MessagePassingConnection methods,
 * 		subclasses must do the following:
 * 
 * 		upon instantiation, call allocateIncomingMessageQueue() to provide the class
 * 			and number of queued messages for our reads.
 * 
 * 		upon connecting(), call didConnect() to provide the WrappedSocket of the connection.
 * 			we assume that at this point all handshaking is complete, and the connection is ready.
 * 
 * 		use the provided 'setConnectionStatus' method to log ALL connection status changes,
 * 			INCLUDING the change to CONNECTED (at this point, ALSO call didConnect()).
 * 
 * 		once didConnect() is called, subclass should be basically "hands-off" our connection.
 * 			It did its part - establishing the connection - now we handle things.
 * 
 * 		ALWAYS override 'disconnect' when establishing the connection - i.e., when in
 * 			'pending' status.  We don't know how to handle a disconnect under those
 * 			circumstances.  If connected, and the WrappedSocket is all we have, we can
 * 			handle things fine, so call super.disconnect() within your method in those
 * 			statuses.
 *
 * Using this class:
 * 		As a middle-ground between new instances and slow reads, the MessagePassingWrappedSocketConnection
 * 			maintains a fixed-length "queue" of messages.  At a minimum, 3 messages
 * 			are needed for the queue.  1 Message will be passed to the user with a
 * 			call to getMessage; we guarantee it will not be changed.  We read into
 *			another Message.  For ease of implementation, we wait for the next call
 *			to getMessage before moving to the next Message in the queue.
 * 
 * @author Jake
 *
 */
public abstract class MessagePassingWrappedSocketConnection
	extends MessagePassingConnection
	implements MessageReader.Delegate, MultipleMessageReader.Delegate, DataObjectSenderReceiver<Message> {
	
	private static final boolean DEBUG_LOG = true && Debug.LOG ;
	private static final String TAG = "MPWSocketConnection" ;
	
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
	
	// Here's our connection.
	private boolean didCallDoneReceivingThisConnect ;
	private Status connectionStatus ;
	private WrappedSocket wsock ;
	private SocketAddress wsock_remoteAddress ;
	
	private boolean wsock_messageAware ;
	
	protected Class<?> messageClass ;
	
	// MESSAGE-AWARE WRAPPED SOCKETS
	// Wrapped sockets which are message-aware get these data structures.
	private BlockingQueue<Message> wsock_rbq ;
	private BlockingQueue<Message> wsock_wbq ;
	private Message lastMessageRead ;
	private boolean wsock_rbqExhausted ;
	
	// BYTE-ARRAY WRAPPED SOCKETS
	// Wrapped sockets which are byte-aware, but not object-aware for our message
	// type, get these data structures.
	private ReadableByteChannel wsock_rbc ;
	private WritableByteChannel wsock_wbc ;
	private MessageReader mreader ;
	private MultipleMessageReader mmreader ;
	private Object mmreaderToken ;
	
	// Here's messages for reading.
	private Message myMessage ;
	private Message [] incomingMessageQueue ;
	private int lastMessageIndexRead, lastMessageIndexRetrieved ;
		// Indexes into incomingMessageQueue; lastMessageRead indicates the
		// index of the last message we read from the WrappedSocket, whereas
		// lastMessageRetrieved is the last returned by a call to getMessage().
	
		// We need at least a 3-message queue.
	
	// Here's our delegate.
	private MessagePassingConnection.Delegate delegate = null ;
	
	
	
	private static final int MAXIMUM_CLOSE_DELAY = 1000 ;
	
	
	////////////////////////////////////////////////////////////////////////////
	// SET MULTIPLE MESSAGE READER
	//
	// If not set, we use our own internal MessageReader for processing incoming
	// messages.  This MessageReader runs its own thread, meaning that this MPC
	// uses both all threads used by WrappedSocket (2 for Autonomous) and the MR
	// (1).
	//
	// This is OK if we act as a client and have only one MPC.  Servers, however,
	// can have up to 5 active connections, meaning 15 threads competing for CPU
	// time just for communication (30 if we host both a lobby and a game!)
	// For servers, using AdministratedWrappedSockets and a MultipleMessageReader
	// can provide the same basic functionality at dramatically reduced thread 
	// cost.  The only downside is that instances of the objects must be provided
	// from outside.
	
	/**
	 * Sets the current MultipleMessageReader for this Connection.  By default
	 * we use a standard MessageReader (our own) rather than share a MMR with
	 * possibly several other Connections.  Setting 'null' with this method 
	 * will revert to this behavior.
	 * 
	 * Calling this method on an Active connection is an error; an exception
	 * will be thrown.
	 * 
	 * @param reader The MultipleMessageReader we will use for reading Messages
	 * 		from our WrappedSocket innards.
	 */
	public void setMultipleMessageReader( MultipleMessageReader reader ) {
		if ( isActive() ) {
			throw new IllegalStateException("Can only change MultipleMessageReader when now active.") ;
		}
		
		mmreader = reader ;
		mmreaderToken = null ;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// SUBCLASSING
	// 
	// To provide correct functionality, subclasses MUST call these methods at the
	// appropriate times.
	
	
	/**
	 * A necessary call by any subclass; determines the message class to be used for receives,
	 * and specifies the queue length.
	 * 
	 * @throws IllegalArgumentException if the class provided is not a Message class, or the queueLength is < 2.
	 * @throws InstantiationException if we fail to instantiate instances of the Message class.
	 * @throws IllegalAccessException if we fail to instantiate instances of the Message class.
	 */
	protected synchronized void allocateIncomingMessageQueue( Class<?> messageClass, int queueLength ) throws IllegalArgumentException, InstantiationException, IllegalAccessException {
		// check message class!
		if ( !Message.class.isAssignableFrom(messageClass) ) 
			// the provided class is NOT a subclass of message!
			throw new IllegalArgumentException("MessagePassingWrappedSocketConnection: in allocateIncomingMessageQueue, Message is not assignable from provided messageClass.") ;
		
		// check queue length!
		if ( queueLength < 3 )
			throw new IllegalArgumentException("MessagePassingWrappedSocketConnection: in allocateIncomingMessageQueue, queueLength must be at least 2: " + queueLength + " provided.") ;
		
		// Allocate our stuff fo-shizzle.
		this.messageClass = messageClass ;
		myMessage = (Message) this.messageClass.newInstance() ;
		this.incomingMessageQueue = new Message[queueLength] ;
		for ( int i = 0; i < queueLength; i++ )
			this.incomingMessageQueue[i] = (Message) this.messageClass.newInstance() ;
	}
	
	
	/**
	 * We have successfully connected!  Awesome.
	 * Sets our connection status to 'connected' and accepts the wsock as the connection itself.
	 * @param wsock
	 * @throws IllegalAccessException If call to newInstance fails
	 * @throws InstantiationException If call to newInstance fails
	 * @throws InterruptedException 
	 */
	protected synchronized void didConnect( WrappedSocket wsock ) throws InstantiationException, IllegalAccessException, InterruptedException {
		log("didConnect") ;
		
		this.wsock = wsock ;
		this.wsock_remoteAddress = wsock.getRemoteSocketAddress() ;
		
		this.wsock_messageAware = wsock.isObjectAware(messageClass) ;
		
		if ( wsock_messageAware ) {
			log("configuring message awareness") ;
			this.wsock_rbq = (BlockingQueue<Message>)wsock.getDataObjectSourceQueue() ;
			this.wsock_wbq = (BlockingQueue<Message>)wsock.getDataObjectSinkQueue() ;
			wsock_rbqExhausted = false ;
			// put our "incoming message queue" objects into our shared pool.
			lastMessageRead = null ;
			
			// aaaand ready
			wsock.setDataObjectSenderReceiver(this) ;
		} else {
			log("configuring byte-awareness") ;
			this.wsock_rbc = wsock.getSourceChannel() ;
			this.wsock_wbc = wsock.getSinkChannel() ;
			
			// We need a message reader.
			if ( mmreader == null ) {
				this.mreader = new MessageReader( (Message) this.messageClass.newInstance(), this.wsock_rbc ) ;
				this.mreader.setDelegate(this) ;
				this.mreader.okToReadNextMessage() ;		// start reading now
				this.mmreaderToken = null ;
			} else {
				mmreaderToken = new Object() ;
				this.mmreader.addClient(mmreaderToken, this, (Message)this.messageClass.newInstance(), ((Pipe.SourceChannel)this.wsock_rbc) ) ;
				this.mmreader.okToReadNextMessage(mmreaderToken) ;
				this.mreader = null ;
			}
		}
		
		// Set our message queue position at the end.  This should ensure that
		// the next message read will go in position 0.
		this.lastMessageIndexRead = this.lastMessageIndexRetrieved = this.incomingMessageQueue.length - 1 ;
	}
	
	
	/**
	 * A method for this class and its subclasses to set its connection status.
	 * @param status The new connection status.
	 * @throws IllegalStateException If called when the connection is not Active
	 * 			for any status other than Status.INACTIVE
	 */
	protected void setConnectionStatus( Status status ) throws IllegalStateException {
		if ( !this.isActive() && status != MessagePassingConnection.Status.INACTIVE ) 
			throw new IllegalStateException("MessagePassingWrappedSocketConnection: don't set status on in inactive connection!") ;
		
		if ( status == MessagePassingConnection.Status.PENDING
				|| status == MessagePassingConnection.Status.CONNECTED ) {
			didCallDoneReceivingThisConnect = false ;
		}
		
		this.connectionStatus = status ;
		
		if ( status == Status.DISCONNECTED )
			log(new Exception("made for stacktrace"), "setConnectionStatus to " + status) ;
		else
			log("setConnectionStatus to " + status) ;
		
		// Tell the delegate.
		if ( delegate != null ) {
			boolean noLongerConnected = false ;
			switch( status ) {
			case CONNECTED:
				delegate.mpcd_messagePassingConnectionDidConnect(this) ;
				break ;
			case FAILED:
				delegate.mpcd_messagePassingConnectionDidFailToConnect(this) ;
				noLongerConnected = true ;
				break ;
			case BROKEN:
				delegate.mpcd_messagePassingConnectionDidBreak(this) ;
				noLongerConnected = true ;
				break ;
			case PEER_DISCONNECTED:
				delegate.mpcd_messagePassingConnectionDidDisconnectByPeer(this) ;
				noLongerConnected = true ;
				break ;
			}
			
			if ( noLongerConnected ) {
				callDoneReceivingMessagesIfAppropriate() ;
			}
		}
	}
	
	/**
	 * Tells an 'Active' Connection to disconnect.  This call will fail (throwing an
	 * exception) for an inactive connection, one that has not connect()ed, or one
	 * which has already disconnect()ed.  Will NOT throw an exception if the Connection
	 * is connected, pending, broken, or disconnected by peer.  In other words, if YOU
	 * call connect(), YOU should expect to call disconnect() once.
	 * 
	 * @throws IllegalStateException Under the circumstances described.
	 */
	public synchronized void disconnect() throws IllegalStateException {
		if ( !this.isActive() )
			throw new IllegalStateException("Can't disconnect an inactive connection.") ;
		Status status =  this.connectionStatus() ;
		if ( status == MessagePassingConnection.Status.NEVER_CONNECTED
				|| status == MessagePassingConnection.Status.DISCONNECTED )
			throw new IllegalStateException("Can't disconnect a non-connected connection.") ;
		// We send a 'disconnect' message on the socket.
		
		if ( status == MessagePassingConnection.Status.CONNECTED
				|| status == MessagePassingConnection.Status.BROKEN ) {
			if ( this.wsock_messageAware ) {
				try {
					wsock_wbq.add( ((Message)wsock.getDataObjectEmptyInstance()).setAsDisconnectMessagePassingConnection() ) ;
					wsock.dataObjectAvailable() ;
					// drop references to the queues.
					wsock_wbq = null ;
					wsock_rbq = null ;
				} catch ( Exception e ) { }
			} else {
				try {
					myMessage.setAsDisconnectMessagePassingConnection() ;
					myMessage.write( this.wsock_wbc ) ;
					// drop references to pipes
					this.wsock_wbc = null ;
					this.wsock_rbc = null ;
				} catch( IOException e ) {
					// Do nothing.  we're disconnecting anyway.
				}
			}
		}
		
		// Stop reading.
		try {
			if ( mreader != null ) {
				this.mreader.setDelegate(null) ;
				this.mreader.stop();
			}
		} catch( Exception e) {
			e.printStackTrace() ;
		}
		try {
			if ( mmreader != null ) {
				this.mmreader.remove(mmreaderToken) ;
			}
		} catch( Exception e ) {
			e.printStackTrace() ;
		}
		
		// Close.  Safe to call in any state.  Block for outgoing data, but not too long.
		
		if ( wsock != null )
			wsock.close( true, MAXIMUM_CLOSE_DELAY );
		//long timeAfter = System.currentTimeMillis() ;
		////System.out.println("MessagePassingWSConnection: closed after blocking for " + (timeAfter - time) + " milliseconds") ;
		
		// Set our status.
		this.setConnectionStatus(MessagePassingConnection.Status.DISCONNECTED) ;
	}

	@Override
	public synchronized Status connectionStatus() {
		if ( !this.isActive() ) 
			return MessagePassingConnection.Status.INACTIVE ;
		
		// Take this opportunity to check the state of our connection.
		if ( connectionStatus == MessagePassingConnection.Status.CONNECTED ) {
			// Disconnect if we've been waiting too long for a message; SYN, DATA or ACK.
			// TODO: Extract the literal here and set it as a parameter instead.
			// REMEMBER: timeWaitingForAck is MILLISECONDS, not SECONDS!
			long waitingACK = wsock.timeWaitingForAck() ;
			long waitingMSG = wsock.timeSinceLastReceived() ;
			if ( wsock.isClosed() || !wsock.isConnected() || waitingACK > 10000 || waitingMSG > 10000 ) {
				// one last try - any messages we could get?
				if ( connectionStatus != MessagePassingConnection.Status.PEER_DISCONNECTED ) {
					/*
					if ( wsock.isClosed() )
						//System.err.println("in connectionStatus: changing to broken: socket closed") ;
					else if ( !wsock.isConnected() )
						//System.err.println("in connectionStatus: socket not connected") ;
					else if ( waitingACK > 10000 )
						//System.err.println("in connectionStatus: waiting for ACK for " + waitingACK) ;
					else
						//System.err.println("in connectionStatus: waiting for DATA or SYN for " + waitingMSG) ;
						*/
					
					// Terminate our connection.
					// mreader.stop() ;
					// wsock.close() ;
					this.setConnectionStatus(MessagePassingConnection.Status.BROKEN) ;
				}
			}
		}
		
		return connectionStatus ;
	}
	
	
	
	

	@Override
	public synchronized boolean hasMessage() throws IllegalStateException {
		if ( !this.isActive() )
			throw new IllegalStateException("Inactive Connections never have messages.") ;
		Status status =  this.connectionStatus() ;
		if ( status == MessagePassingConnection.Status.NEVER_CONNECTED
				|| status == MessagePassingConnection.Status.PENDING
				|| status == MessagePassingConnection.Status.FAILED ) {
			throw new IllegalStateException("Must wait for a connection before reading messages.") ;
		}
		
		if ( wsock_messageAware ) {
			pollDataObjectSourceQueueForMetaMessages() ;
			return this.wsock_rbq.size() > 0 ;
		}
		return this.lastMessageIndexRead != this.lastMessageIndexRetrieved ;
	}
	
	
	/**
	 * Returns 'true' there could possibly, at some point in the future, be more
	 * messages on this connection (before a call to connect()).
	 * 
	 * @return
	 * @throws IllegalStateException If the Connection is inactive, or has not yet been connected.
	 */
	public synchronized boolean moreMessages() throws IllegalStateException {
		if ( !this.isActive() )
			throw new IllegalStateException("Inactive Connections never have messages.") ;
		Status status =  this.connectionStatus() ;
		if ( status == MessagePassingConnection.Status.NEVER_CONNECTED
				|| status == MessagePassingConnection.Status.PENDING
				|| status == MessagePassingConnection.Status.FAILED ) {
			return false ;
		}
		
		if ( wsock_messageAware ) {
			pollDataObjectSourceQueueForMetaMessages() ;
			return this.wsock_rbq.size() > 0 || !this.wsock.isClosed() ;
		} else {
			// If the reader is still OK, there might be messages forthcoming.
			// Otherwise, check whether we currently have a message.
			if ( mmreader != null ) {
				int mmreader_status = mmreader.status(mmreaderToken) ;
				if ( mmreader_status == MultipleMessageReader.STATUS_STOPPED
						|| mmreader_status == MultipleMessageReader.STATUS_INPUT_SOURCE_BROKEN
						|| mmreader_status == MultipleMessageReader.STATUS_INPUT_SOURCE_NULL
						|| mmreader_status == MultipleMessageReader.STATUS_UNSPECIFIED_ERROR
						|| mmreader_status == MultipleMessageReader.STATUS_NOT_FOUND ) {
					////System.out.println("MPWSConnection, mreader has status " + mreader_status + " and we have message:" + hasMessage()) ;
					return hasMessage() ;
				}
			} else if ( mreader != null ) {
				int mreader_status = mreader.status() ;
				if ( mreader_status == MessageReader.STATUS_STOPPED
						|| mreader_status == MessageReader.STATUS_INPUT_SOURCE_BROKEN
						|| mreader_status == MessageReader.STATUS_INPUT_SOURCE_NULL
						|| mreader_status == MessageReader.STATUS_UNSPECIFIED_ERROR ) {
					////System.out.println("MPWSConnection, mreader has status " + mreader_status + " and we have message:" + hasMessage()) ;
					return hasMessage() ;
				}
			} 
		}
		return true ;
	}

	@Override
	public synchronized Message getMessage() throws IllegalStateException {
		if ( !this.isActive() )
			throw new IllegalStateException("Inactive Connections never have messages.") ;
		Status status =  this.connectionStatus() ;
		if ( status == MessagePassingConnection.Status.NEVER_CONNECTED
				|| status == MessagePassingConnection.Status.PENDING
				|| status == MessagePassingConnection.Status.FAILED ) {
			throw new IllegalStateException("Must wait for a connection before reading messages.") ;
		}
		
		if ( wsock_messageAware ) {
			if ( lastMessageRead != null ) {
				// put in the pool...
				try {
					wsock.recycleDataObjectInstance(lastMessageRead) ;
				} catch (Exception e) {
					e.printStackTrace() ;
					throw new IllegalStateException("Failure returning a message to pool") ;
				}
				lastMessageRead = null ;
			}
			pollDataObjectSourceQueueForMetaMessages() ;
			if ( wsock_rbq.size() > 0 ) {
				lastMessageRead = (Message) wsock_rbq.poll() ;
				if ( lastMessageRead != null ) {
					if ( lastMessageRead.getType() == Message.TYPE_MY_NAME && lastMessageRead.getName() != null )
						updateRemoteName( lastMessageRead.getName() ) ;
					pollDataObjectSourceQueueForMetaMessages() ;
					return lastMessageRead ;
				}
			}
		} else {
			boolean hadRoom = roomForMessage() ;
			// If we have a message available, return it.
			if ( this.lastMessageIndexRead != this.lastMessageIndexRetrieved ) {
				this.lastMessageIndexRead += 1 ;
				this.lastMessageIndexRead %= this.incomingMessageQueue.length ;
				Message m = this.incomingMessageQueue[this.lastMessageIndexRead] ;
				
				// Note: in normal operation, the message reader delegate method
				// will inform it to read the next message upon the previous
				// message becoming ready.  However, it will ONLY do this if there
				// was room for another message at the time the delegate method
				// was called.  If 'hadRoom' (set above) is false, then we know
				// this delegate call returned 'false' on its last call and the message reader
				// is waiting for the indication to go forward.  Give it now.
				if ( mmreader != null ) {
					int mmreader_status = mmreader.status(mmreaderToken) ;
					if ( !hadRoom && ( mmreader_status == MultipleMessageReader.STATUS_MESSAGE_READY || mmreader_status == MultipleMessageReader.STATUS_WAITING_FOR_MESSAGE ) ) {
						////System.err.println("MessagePassingWrappedSocketConnection: telling multiple message reader to read next message.") ;
						mmreader.okToReadNextMessage(mmreaderToken) ;
					}
				} else if ( mreader != null ) {
					int mreader_status = mreader.status() ;
					if ( !hadRoom && ( mreader_status == MessageReader.STATUS_MESSAGE_READY || mreader_status == MessageReader.STATUS_WAITING_FOR_MESSAGE ) ) {
						////System.err.println("MessagePassingWrappedSocketConnection: telling message reader to read next message.") ;
						mreader.okToReadNextMessage() ;
					}
				}
				
				// Autoupdate remote name?
				if ( m.getType() == Message.TYPE_MY_NAME && m.getName() != null )
					 updateRemoteName( m.getName() ) ;
				
				return m ; 
			}
		}
		
		throw new IllegalStateException("No message available - did hasMessage() return true?") ;
	}

	
	
	@Override
	public synchronized boolean sendMessage(Message m) throws IllegalStateException {
		if ( !this.isActive() )
			throw new IllegalStateException("Inactive Connections cannot send messages.") ;
		Status status =  this.connectionStatus() ;
		if ( status == MessagePassingConnection.Status.NEVER_CONNECTED
				|| status == MessagePassingConnection.Status.PENDING
				|| status == MessagePassingConnection.Status.FAILED ) {
			throw new IllegalStateException("Must wait for a connection before sending messages.") ;
		}
		if ( status == MessagePassingConnection.Status.DISCONNECTED )
			throw new IllegalStateException("You disconnected this Connection!  Why you trying to send messages on it, dog?") ;
		
		////System.err.println("call in sendMessage with status " + status) ;
		
		// take local name from this, if appropriate.
		if ( m.getType() == Message.TYPE_MY_NAME && m.getName() != null )
			updateLocalName( m.getName() ) ;
		
		// Take the opportunity to read-ahead a bit.
		if ( status == MessagePassingConnection.Status.CONNECTED ) {
			if ( this.wsock_messageAware ) {
				// copy this mesage data to a new instance.  Retrieve from the pool,
				// or create a new instance ourselves.
				try {
					Message outgoing = (Message)wsock.getDataObjectEmptyInstance() ;
					outgoing.setAs(m) ;
					this.wsock_wbq.add(outgoing) ;
					this.wsock.dataObjectAvailable() ;
					// we expect the WrappedSocket to return this message to the pool
					// when possible, so don't bother retaining a reference to it.
					return true ;
				} catch ( Exception e ) {
					// Whoops.  Something went wrong.  Don't know what, so...
					this.wsock.close();
					this.setConnectionStatus(MessagePassingConnection.Status.BROKEN) ;
					////System.err.println("in sendMessage: changing to broken") ;
					return false ;
				}
			} else {
				try {
					////System.err.println("writing message type " + m.getType()) ;
					m.write( wsock_wbc ) ;
					return true ;
				} catch( IOException e ) {
					// Whoops.  Something went wrong.  Don't know what, so...
					e.printStackTrace() ;
					if ( mmreader != null ) {
						mmreader.remove(mmreaderToken) ;
					} else if ( mreader != null ) {
						this.mreader.stop();
					}
					this.wsock.close();
					this.setConnectionStatus(MessagePassingConnection.Status.BROKEN) ;
					////System.err.println("in sendMessage: changing to broken") ;
					return false ;
				}
			}
		}
	
		return false ;
	}

	
	
	private synchronized boolean roomForMessage() {
		if ( this.wsock_messageAware )
			throw new IllegalStateException("roomForMessage() only appropriate for byte-based wrapped sockets") ;
		return (( this.lastMessageIndexRetrieved + 2 ) % this.incomingMessageQueue.length) != this.lastMessageIndexRead ;
	}
	
	@Override
	public synchronized SocketAddress getRemoteSocketAddress() {
		if ( wsock != null && wsock.isConnected() )
			return this.wsock_remoteAddress ;
		return null ;
	}
	
	protected abstract void updateLocalName( String name ) ;
	protected abstract void updateRemoteName( String name ) ;
	
	///////////////////////////////////////////////////////////////////////////
	// 
	// SETTING A DELEGATE
	//
	///////////////////////////////////////////////////////////////////////////
	
	
	public synchronized void setDelegate( MessagePassingConnection.Delegate delegate ) {
		this.delegate = delegate ;
	}
	
	/**
	 * This method call the DoneReceivingMessages delegate method if appropriate.
	 * 
	 * It is "appropriate" to make this call if our message reader has stopped,
	 * and our connection is not open.
	 */
	private synchronized void callDoneReceivingMessagesIfAppropriate() {
		// it is only appropriate to make this call if we have a delegate (obviously)
		// and if our connection status is broken in some way.
		if ( delegate == null )
			return ;
		if ( !( connectionStatus == MessagePassingConnection.Status.BROKEN
							|| connectionStatus == MessagePassingConnection.Status.FAILED
							|| connectionStatus == MessagePassingConnection.Status.PEER_DISCONNECTED ) )
			return ;
		if ( didCallDoneReceivingThisConnect )
			return ;		// only call once.
		
		//System.err.println("callDoneReceivingMessagesIfAppropriate...") ;
		if ( wsock_messageAware ) {
			//System.err.println("message aware") ;
			if ( (wsock.isClosed() || wsock_rbqExhausted) && this.wsock_rbq.size() == 0 )
				delegate.mpcd_messagePassingConnectionDoneReceivingMessages(this) ;
		} else {
			if ( mmreader != null ) {
				//System.err.println("mmreader status check...") ;
				int mmreader_status = mmreader == null ? MultipleMessageReader.STATUS_STOPPED : mmreader.status(mmreaderToken) ;
				//System.err.println("mmreader_status is " + mmreader_status) ;
				if ( delegate != null
						&& ( mmreader_status != MultipleMessageReader.STATUS_MESSAGE_READY
								&& mmreader_status != MultipleMessageReader.STATUS_WAITING_FOR_MESSAGE
								&& mmreader_status != MultipleMessageReader.STATUS_READING_MESSAGE ) ) {
					// It looks like we should send the update.
					didCallDoneReceivingThisConnect = true ;
					delegate.mpcd_messagePassingConnectionDoneReceivingMessages(this) ;
					//System.err.println("did call") ;
				}
			}
			else if ( mreader != null ) {
				//System.err.println("mreader status check...") ;
				int mreader_status = mreader == null ? MessageReader.STATUS_STOPPED : mreader.status() ;
				if ( delegate != null
						&& ( mreader_status == MessageReader.STATUS_STOPPED
								|| mreader_status == MessageReader.STATUS_INPUT_SOURCE_BROKEN
								|| mreader_status == MessageReader.STATUS_UNSPECIFIED_ERROR ) ) {
					// It looks like we should send the update.
					didCallDoneReceivingThisConnect = true ;
					delegate.mpcd_messagePassingConnectionDoneReceivingMessages(this) ;
					//System.err.println("did call") ;
				}
			}
			else {
				//System.err.println("no message reader") ;
				didCallDoneReceivingThisConnect = true ;
				delegate.mpcd_messagePassingConnectionDoneReceivingMessages(this) ;
				//System.err.println("did call") ;
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	// MESSAGE READER DELEGATE METHODS
	// 
	// This methods provide the MessageReader.Delegate interface.
	
	/**
	 * The message being read by the MessageReader is ready.
	 * This method is called upon successful read of a new Message,
	 * if the Delegate is set at that time.
	 * 
	 * @param mr The MessageReader.
	 * @return Whether the MessageReader should immediately begin
	 * 		to read the next message.  Equivalent to a call to
	 * 		okToReadNextMessage().
	 */
	public synchronized boolean mrd_messageReaderMessageIsReady( MessageReader mr ) {
		if ( mr == mreader ) {
			Message m = this.mreader.getMessage();
			boolean readImmediately = this.readMessageIntoIncomingMessageQueue(m) ;
			return readImmediately ;
		}
		////System.err.println("MessagePassingWrappedSocketConnection: telling message reader to NOT read message.") ;
		return false ;
	}
	
	/**
	 * The MessageReader has encountered an error while reading
	 * a message.  Most likely the InputStream is broken or empty.
	 * This method will be followed soon after by a call to 
	 * mrd_messageReaderStopped.
	 * 
	 * @param mr
	 */
	public synchronized void mrd_messageReaderError( MessageReader mr ) {
		// Error: disconnect.
		if ( connectionStatus() == MessagePassingConnection.Status.CONNECTED ) {
			//System.err.println("in mrd_messageReaderError: changing to broken") ;
			setConnectionStatus( MessagePassingConnection.Status.BROKEN ) ;
		} else {
			//System.err.println("in mrd_messageReaderError: was broken") ;
			callDoneReceivingMessagesIfAppropriate() ;
		}
	}
	
	/**
	 * The MessageReader has stopped.  This method is called as
	 * the last operation of the MessageReaderThread, and will 
	 * occur whether stopped from inside (
	 * 
	 * @param mr
	 */
	public synchronized void mrd_messageReaderStopped( MessageReader mr ) {
		// Stopped.
		//System.out.println("message reader stopped.  Status is " + connectionStatus()) ;
		if ( connectionStatus() == MessagePassingConnection.Status.CONNECTED ) {
			////System.out.println("message reader stopped.  setting status as broken") ;
			setConnectionStatus( MessagePassingConnection.Status.BROKEN ) ;
			//System.err.println("in mrd_messageReaderError: changing to broken") ;
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// MULTIPLE MESSAGE READER DELEGATE METHODS
	// 
	// This methods provide the MultipleMessageReader.Delegate interface.
	
	
	/**
	 * The message being read by the MultipleMessageReader is ready.
	 * This method is called upon successful read of a new Message,
	 * if the Delegate is set at that time.
	 * 
	 * @param mr The MultipleMessageReader.
	 * @return Whether the MultipleMessageReader should immediately begin
	 * 		to read the next message.  Equivalent to a call to
	 * 		okToReadNextMessage().
	 */
	@Override
	public synchronized boolean mmrd_messageReaderMessageIsReady( MultipleMessageReader mmr, Object token ) {
		if ( mmr == mmreader && token == mmreaderToken ) {
			Message m = mmr.getMessage(mmreaderToken) ;
			boolean readImmediately = this.readMessageIntoIncomingMessageQueue(m) ;
			return readImmediately ;
		}
		//System.err.println("MessagePassingWrappedSocketConnection: telling message reader to NOT read message.") ;
		return false ;
	}
	
	/**
	 * The MultipleMessageReader has encountered an error while reading
	 * a message.  Most likely the InputStream is broken or empty.
	 * This method will be followed soon after by a call to 
	 * mmrd_messageReaderStopped.
	 * 
	 * @param mr
	 */
	@Override
	public synchronized void mmrd_messageReaderError( MultipleMessageReader mmr, Object token ) {
		// Error: disconnect.
		if ( mmr == mmreader && token == mmreaderToken ) {
			if ( connectionStatus() == MessagePassingConnection.Status.CONNECTED ) {
				//System.err.println("in mmrd_messageReaderError: changing to broken") ;
				setConnectionStatus( MessagePassingConnection.Status.BROKEN ) ;
			} else {
				//System.err.println("in mmrd_messageReaderError: was broken") ;
				callDoneReceivingMessagesIfAppropriate() ;
			}
		}
	}
	
	/**
	 * The MultipleMessageReader has stopped.  This method is called as
	 * the last operation of the MultipleMessageReaderThread, and will 
	 * occur whether stopped from inside (
	 * 
	 * @param mr
	 */
	@Override
	public synchronized void mmrd_messageReaderStopped( MultipleMessageReader mmr, Object token ) {
		// Stopped.
		//System.out.println("message reader stopped.  Status is " + connectionStatus()) ;
		if ( mmr == mmreader && token == mmreaderToken ) {
			if ( connectionStatus() == MessagePassingConnection.Status.CONNECTED ) {
				////System.out.println("message reader stopped.  setting status as broken") ;
				setConnectionStatus( MessagePassingConnection.Status.BROKEN ) ;
				// //System.err.println("in mmrd_messageReaderError: changing to broken") ;
			}
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// OBJECT AWARENESS
	// 
	// This methods provide the interface for interacting with Object Aware wrapped sockets
	
	@Override
	public void dosr_dataObjectAvailable( WrappedSocket ws, Object dataObject ) {
		// for byte-based connections, we do the work of reading the message
		// out into our own arrays.  Here we just leave it in the queue until
		// the delegate reads it.
		if ( wsock == ws ) {
			// disconnect message?
			if ( ((Message)dataObject).getType() == Message.TYPE_DISCONNECT_MESSAGE_PASSING_CONNECTION ) {
				this.setConnectionStatus(MessagePassingConnection.Status.PEER_DISCONNECTED) ;
				wsock.close();
				
				// Inform the delegate: we will receive no more messages, and the connection
				// is no longer active.
				callDoneReceivingMessagesIfAppropriate() ;
			} else {
				// If not a disconnect, this is something to pass on to the delegate.
				if ( delegate != null ) {
					delegate.mpcd_messagePassingConnectionDidReceiveMessage(this) ;
				}
			}
		}
	}
	
	/**
	 * The DOSR will never again receive a data object from this socket.
	 * @param ws
	 */
	public void dosr_dataObjectsExhaustedForever( WrappedSocket ws ) {
		if ( wsock == ws ) {
			if ( connectionStatus() == MessagePassingConnection.Status.CONNECTED ) {
				setConnectionStatus( MessagePassingConnection.Status.BROKEN ) ;
			}
			wsock_rbqExhausted = true ;
			this.callDoneReceivingMessagesIfAppropriate() ;
		}
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	/**
	 * Certain messages (e.g. DISCONNECT_MESSAGE_PASSING_CONNECTION) are "meta"
	 * messages, created by and consumed by MessagePassingConnection (and not
	 * their delegates).  We handle incoming Meta messages as they arrive
	 * in dosr_dataObjectAvailable, but they still exist in the read queue.
	 * This message peeks at the queue, removing any such messages with a poll,
	 * leaving any other message in place.
	 */
	private void pollDataObjectSourceQueueForMetaMessages() {
		Message m = (Message)wsock_rbq.peek() ;
		while ( m != null ) {
			switch( m.getType() ) {
			case Message.TYPE_DISCONNECT_MESSAGE_PASSING_CONNECTION:
				wsock_rbq.poll() ;
				m = (Message)wsock_rbq.peek() ;
				break ;
			default:
				m = null ;
				break ;
			}
		}
	}
	
	
	
	/**
	 * Reads the new message object (whose instance we will not retain,
	 * and can be safely changed after this call) into our message queue.
	 * 
	 * This method potentially changes the connection status, and potentially
	 * results in a delegate call (Done Receiving Messages).  It also (potentially)
	 * closes sockets and message readers if necessary.
	 * 
	 * REQUIRES THAT ROOM IS AVAILABLE: Generally speaking, you shouldn't
	 * even be retrieving a message when room is not available.
	 * 
	 * Returns whether it is appropriate to immediately retrieve another message.
	 * This method is robust to cases where we expect the Delegate to result
	 * in 'next message' prompts, and will return false in that case (to prevent
	 * a double message retrieval).
	 */
	private boolean readMessageIntoIncomingMessageQueue( Message m ) {
		this.lastMessageIndexRetrieved += 1 ;
		this.lastMessageIndexRetrieved %= this.incomingMessageQueue.length ;
		this.incomingMessageQueue[ this.lastMessageIndexRetrieved ].setAs(m) ; 
		////System.out.println("MessagePassingWrappedSocketConnection: has message of type " + m.getType() + " = " + this.incomingMessageQueue[this.lastMessageRetrieved].getType()) ;
		
		// Quick update: if the message was a "disconnect", then set our
		// status as a peer disconnect and back up.
		if ( this.incomingMessageQueue[ this.lastMessageIndexRetrieved ].getType() == Message.TYPE_DISCONNECT_MESSAGE_PASSING_CONNECTION ) {
			this.setConnectionStatus(MessagePassingConnection.Status.PEER_DISCONNECTED) ;
			if ( mmreader != null )
				mmreader.remove(mmreaderToken) ;
			else if ( mreader != null )
				mreader.close() ;
			wsock.close();
			this.lastMessageIndexRetrieved -= 1 ;
			if ( this.lastMessageIndexRetrieved < 0 )
				this.lastMessageIndexRetrieved = this.incomingMessageQueue.length - 1 ;
			
			// Inform the delegate: we will receive no more messages, and the connection
			// is no longer active.
			callDoneReceivingMessagesIfAppropriate() ;
			return false ;
		} else {
			// Quick note: this delegate call might affect the number of messages available.
			// Specifically, it might eat a few messages off the queue and thus free up
			// space for more messages.  However, if it does this, then under some very
			// specific circumstances it will prompt the MultipleMessageReader for a new message.
			// We don't want to do so again.  Therefore, check whether we should
			// tell the mreader to read another message BEFORE the delegate call.
			boolean hasRoomBeforeDelegateCall = roomForMessage() ;
			if ( delegate != null )
				delegate.mpcd_messagePassingConnectionDidReceiveMessage(this) ;
			////System.err.println("MessagePassingWrappedSocketConnection: telling message read to read message:" + hasRoomBeforeDelegateCall) ;
			return hasRoomBeforeDelegateCall ;
			// If we had room, we still have room.  If we DIDN'T have room, 
			// then either we still don't (and we should return false) or 
			// the delegate freed up some space with a call to getMessage,
			// and THIS call informed the MultipleMessageReader that we should get
			// another message.  Either way, we should return 'false' here.
		}
	}
	
}
