package com.peaceray.quantro.lobby;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.lobby.communications.LobbyMessage;
import com.peaceray.quantro.utils.AndroidCommunication;
import com.peaceray.quantro.utils.AppVersion;
import com.peaceray.quantro.utils.WifiMonitor;


/**
 * Lobby Announcer is meant to send out information regarding a currently
 * running WiFi lobby.
 * 
 * The announcer pairs with LobbyFinder, operating in two ways: 1, it regularly
 * sends out announcement packets, and 2 it listens for query packets and responds
 * directly with a targetted announcement.
 * 
 * As of 12/6/12, a bug has been identified: if the WiFi connection is
 * lost during operation, the announcement thread can crash and no future
 * announcements will be made.  Note that this is not a huge problem,
 * except that typically usage is to start the announcer when the lobby
 * opens and never stop or restart it, meaning a brief WiFi outlet
 * permanently kills the announcer.
 * 
 * We are changing this to operate in the following way: upon starting,
 * the user may optionally provide a Handler for automatic restarts.
 * Restarts will proceed as long as the Handler is available; the caller
 * may provide it as a reference or a WeakReference.  In the latter case,
 * the caller is responsible for holding its own reference, but gains
 * the security of knowing that if it crashes or is collected without
 * calling stop(), the next restart of announcer will fail as it will
 * lack a handler to start itself.
 * 
 * @author Jake
 *
 */
public class LobbyAnnouncer {
	public static final String TAG = "LobbyAnnouncer" ;
	
	
	private boolean setLobbyStatus( LobbyMessage lm, Lobby lobby, int versionCode, Nonce nonce, String ip, int port ) {
		int numPeople = lobby.getNumPeople() ;
		int maxPeople = lobby.getMaxPeople() ;
		long age = lobby.getAge() ;
		String name = lobby.getLobbyName() ;
		int localPlayer = lobby.getLocalPlayerSlot() ;
		String hostName = localPlayer >= 0 ? lobby.getPlayerName(localPlayer) : null ;
 		
		// sanity check
		if ( maxPeople <= 0 || numPeople < 0 || age < 0 || name == null )
			return false ;
		
		lm.setAsLobbyStatus(versionCode, nonce, numPeople, maxPeople, age, new InetSocketAddress(ip, lobbyPort), name, hostName) ;
		return true ;
	}
	
	class LobbyAnnouncerThread extends Thread {
		
		
		
		LobbyAnnouncer la ;
		
		boolean running ;
		
		DatagramChannel dchannel_broadcast ;
		DatagramChannel dchannel_listen ;
		Selector selector ;
		SelectionKey selectionKey ;
		
		int announcementInterval ;
		
		public LobbyAnnouncerThread( LobbyAnnouncer parent, int announcementInterval ) throws SocketException {
			this.la = parent ;
			
			running = true ;
			
			// Announcement socket
			try {
				dchannel_broadcast = DatagramChannel.open() ;
				dchannel_broadcast.socket().setBroadcast(true) ;
			} catch (IOException e) {
				throw new SocketException("Failed opening broadcast channel") ;
			}

			try {
				dchannel_listen = DatagramChannel.open() ;
				dchannel_listen.socket().bind( new InetSocketAddress( la.listenPort ) ) ;
				dchannel_listen.configureBlocking(false) ;
				selector = Selector.open() ;
				selectionKey = dchannel_listen.register(selector, SelectionKey.OP_READ) ;
			} catch (IOException e) {
				throw new SocketException("Failed opening listen channel") ;
			}
			
			this.announcementInterval = announcementInterval ;
		}
		
		
		@Override
		public void run() {
			
			Log.d(TAG, "Thread.run()") ;
			
			try {
				LobbyMessage out_m = new LobbyMessage() ;
				
				LobbyMessage in_m = new LobbyMessage() ;
				
				// Our basic procedure is:
				// Listen on listen_socket for UDP messages.  If one is received,
				// and is a LobbyStatusRequest message, spin off a TCP response
				// socket to provide the information.
				// Every announcementInterval milliseconds, broadcast our status
				// on the broadcast port.
				
				ByteBuffer bb = ByteBuffer.allocate(512) ;
				
				while( running ) {
					
					try {
						// Broadcast
						Context c = mwrContext.get() ;
						if ( c == null )
							return ;
						String ip = WifiMonitor.ipAddressToString( WifiMonitor.getWifiIpAddress(c) ) ;
						int version_code = AppVersion.code(c) ;
						InetAddress bcast_addr = AndroidCommunication.getWifiBroadcastAddress(c) ;
						c = null ;		// null out context so we don't keep a strong reference.
						
						if ( setLobbyStatus( out_m, lobby, version_code, nonce, ip, lobbyPort ) ) {
							bb.clear() ;
							out_m.write(bb) ;
							bb.flip() ;
	
							// Package up announcement.
							dchannel_broadcast.send(bb, new InetSocketAddress(bcast_addr, la.announcePort)) ;
							//Log.d(TAG, "announcement broadcast") ;
							
							
							// Okay.  Now, for a limited time, listen for incoming requests.
							long startTime = System.currentTimeMillis() ;
							SocketAddress addr ;
							while ( System.currentTimeMillis() - startTime + 5 < announcementInterval ) {
								
								// select for a receive.  Stupid use for a Selector, right?  WRONG!
								// Android 2.3 has a SERIOUS bug where timeouts fail on blocking channels,
								// and non-blocking channels block forever!  There is no good way to
								// receive directly if we don't know there is a packet available!
								// Selection is ABSOLUTELY NECESSARY.
								addr = null ;
								selectionKey.selector().selectNow() ;
								Set<SelectionKey> selectedKeys = selectionKey.selector().selectedKeys();
								Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
								while(keyIterator.hasNext()) { 
								    keyIterator.next();
								    bb.clear() ;
									addr = ((DatagramChannel)selectionKey.channel()).receive(bb) ; 		// non-blocking; should return immediately.
									bb.flip() ;
								    keyIterator.remove();
								}
								
								if ( addr == null ) {
									// sleep!
									try {
										Thread.sleep(5) ;
									} catch( InterruptedException e ) {
										break ;
									}
								} else {
									try {
										in_m.resetForRead() ;
										in_m.read(bb) ;
										if ( in_m.getType() == LobbyMessage.TYPE_LOBBY_STATUS_REQUEST ) {
											// Send the lobby status.
											//Log.d(TAG, "received lobby status request") ;
											new LobbyAnnouncerTCPMessage( in_m.getPort(), ((InetSocketAddress)addr).getAddress(), la ).start() ;
										}
									} catch (ClassNotFoundException cnfe) {
										// nothing
										continue ;
									}
								}
							}
						} else {
							// Lobby invalid.  Wait.
							//Log.v(TAG, "Delaying broadcast; lobby is not configured") ;
							Thread.sleep(1000) ;
						}
					} catch( SocketException e ) {
						e.printStackTrace() ;
						running = false ;
					} catch( IOException e ) {
						e.printStackTrace() ;
						running = false ;
					} catch (InterruptedException e) {
						e.printStackTrace();
						running = false ;
					}
				}
				
				try {
					dchannel_broadcast.close();
				} catch (IOException e) { /* nothing */ }
				try {
					dchannel_listen.close() ;
				} catch (IOException e) { /* nothing */ }
			} finally {
				// if started, post a restart.
				Log.d(TAG, "Thread finally") ;
				if ( started ) {
					Log.d(TAG, "Thread posting mRestartRunnable") ;
					postRunnable( mRestartRunnable, RESTART_DELAY ) ;
				}
			}
		}
		
	}


	class LobbyAnnouncerTCPMessage extends Thread {
		
		int port ;
		InetAddress addr ;
		LobbyAnnouncer la ;
		
		public LobbyAnnouncerTCPMessage( int port, InetAddress addr, LobbyAnnouncer la ) {
			this.port = port ;
			this.addr = addr ;
			this.la = la ;
		}
		
		@Override
		public void run() {
			// Try to open a connection and send the message.
			Socket sock = null ;
			try {
				Context c = mwrContext.get() ;
				if ( c == null )
					return ;
				
				LobbyMessage lm = new LobbyMessage() ;
				String ip = WifiMonitor.ipAddressToString( WifiMonitor.getWifiIpAddress(c) ) ;
				int version_code = AppVersion.code(c) ;
				c = null ;	// null out the Context so we don't keep a strong reference.
				
				if ( setLobbyStatus( lm, lobby, version_code, nonce, ip, lobbyPort ) ) {
					sock = new Socket() ;
					sock.setSoTimeout(5000) ;	// 5 second max
					
					sock.connect( new InetSocketAddress(addr, port) ) ;
					lm.write( sock.getOutputStream() ) ;
					Log.d(TAG, "sent announcement to " + addr.toString()) ;
				}
			} catch( IOException e ) {
				e.printStackTrace() ;
				// Nothing
			} catch( Exception e ) {
				e.printStackTrace() ;
				// Nothing
			} finally {
				try {
					if ( sock != null ) 
						sock.close() ;
				} catch ( Exception e ) { }
			}
		}
		
	}
	
	
	private static final int RESTART_DELAY = 7000 ;		// 7 seconds
	
	int announcePort ;
	int listenPort ;
	
	int lobbyPort ;
	
	Lobby lobby ;
	Nonce nonce ;
	
	LobbyAnnouncerThread thread ;
	
	WeakReference<Context> mwrContext ;
	
	Handler mHandler ;
	WeakReference<Handler> mwrHandler ;
	
	boolean started ;
	
	Runnable mRestartRunnable ;
	
	/**
	 * Listens on the specified port for datagrams holding
	 * @param port
	 * @param lobbyPort
	 * @param lobbyAddr
	 */
	public LobbyAnnouncer( Context context, Nonce nonce, Lobby lobby, int announcePort, int listenPort, int lobbyPort ) {
		this.announcePort = announcePort ;
		this.listenPort = listenPort ;
		this.lobbyPort = lobbyPort ;
		
		this.nonce = nonce ;
		this.lobby = lobby ;
		
		this.mwrContext = new WeakReference<Context>(context) ;
		
		thread = null ;
		
		started = false ;
		
		mHandler = null ;
		mwrHandler = null ;
		
		mRestartRunnable = new RestartRunnable() ;
	}
	
	
	public boolean isStarted() {
		return thread != null && thread.running ;
	}
	
	public boolean start( Handler h ) {
		synchronized( this ) {
			mHandler = h ;
			mwrHandler = null ;
			return start() ;
		}
	}
	
	public boolean start( WeakReference<Handler> wrh ) {
		synchronized( this ) {
			mHandler = null ;
			mwrHandler = wrh ;
			return start() ;
		}
	}
	
	
	private boolean start() {
		Log.d(TAG, "LobbyAnnouncer start") ;
		synchronized( this ) {
			started = true ;
			if ( thread == null ) {
				try {
					thread = new LobbyAnnouncerThread(this, 3000) ;
					thread.start() ;
					return true ;
				} catch (SocketException e) {
					e.printStackTrace() ;
				}
			}
			return false ;
		}
	}

	
	public void stop() {
		Log.d(TAG, "LobbyAnnouncer stop") ;
		synchronized( this ) {
			mHandler = null ;
			mwrHandler = null ;
			started = false ;
			if ( thread != null ) {
				thread.running = false ;
				// wait for thread to die
				boolean retry = true ;
				while (retry) {
		            try {
		                thread.join();
		                retry = false;
		            } catch (InterruptedException e) {
		            }
		        }
				thread = null ;
			}
		}
	}
	
	public void setLobby( Lobby lobby ) {
		this.lobby = lobby ;
	}
	
	
	private boolean postRunnable( Runnable r, int delay ) {
		synchronized( this ) {
			if ( mHandler != null ) {
				Log.d(TAG, "posting on mHandler") ;
				mHandler.postDelayed(r, delay) ;
				return true ;
			}
			else if ( mwrHandler != null ) {
				Log.d(TAG, "checking mwrHandler...") ;
				Handler h = mwrHandler.get() ;
				if ( h != null ) {
					Log.d(TAG, "posting on h") ;
					h.postDelayed(r, delay) ;
					return true ;
				}
			}
			
			return false ;
		}
	}
	
	
	private class RestartRunnable implements Runnable {

		@Override
		public void run() {
			synchronized( LobbyAnnouncer.this ) {
				Log.d(TAG, "RestartRunnable running") ;
				if ( started && mwrContext.get() != null ) {
					Log.d(TAG, "RestartRunnable started with non-null context") ;
					// Announcer is started; is it running?
					if ( thread == null || !thread.isAlive() ) {
						Log.d(TAG, "RestartRunnable thread " + thread + " is null or not alive") ;
						// thread isn't running!
						thread = null ;
						Log.d(TAG, "RestartRunnable calling start()") ;
						if ( !start() ) {
							Log.d(TAG, "RestartRunnable start failed; reposting...") ;
							postRunnable( this, RESTART_DELAY ) ;
						}
					}
				}
			}
		}
		
	}
}
