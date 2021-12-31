package com.peaceray.quantro.lobby;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import android.content.Context;
import android.util.Log;

import com.peaceray.quantro.communications.MessageReader;
import com.peaceray.quantro.lobby.communications.LobbyMessage;
import com.peaceray.quantro.utils.AndroidCommunication;
import com.peaceray.quantro.utils.Communication;
import com.peaceray.quantro.utils.ThreadSafety;

public class WiFiLobbyFinder {
	
	public static final String TAG = "LobbyFinder" ;

	/**
	 * Delegate taking responsibility for organizing and
	 * maintaining a list of available lobbies.  This is optional;
	 * one can ask LobbyFinder for an up-to-date list at any time.
	 * 
	 * However, in that case, it is your responsibility to collect
	 * and process updated lists.
	 * 
	 * @author Jake
	 *
	 */
	public interface Delegate {
		
		public void lobbyFinderFoundNewLobby( WiFiLobbyFinder finder, String key, WiFiLobbyDetails details ) ;
		public void lobbyFinderHasLobbyUpdate( WiFiLobbyFinder finder, String key, WiFiLobbyDetails details ) ;
		public void lobbyFinderLobbyVanished( WiFiLobbyFinder finder, String key ) ;
		
	}
	
	
	WeakReference<Context> mwrContext ;
	
	WeakReference<Delegate> mwrDelegate ;
	Hashtable<String,WiFiLobbyDetails> lobbies ;
	LobbyFinderCoordinatorThread thread ;
	
	ArrayList<SocketAddress> targetedAddresses ;
	ArrayList<Long> targetedTimeLastResponse ;
	
	int announcementPort ;
	int queryPort ;
	int queryResponsePort ;
	
	long timeToExpire ;
	long minTimeBetweenTargetQueries ;
	
	public WiFiLobbyFinder( Delegate delegate, int announcementPort, int queryPort, int queryResponsePort, long timeToExpire, long minTimeBetweenTargetQueries, Context context ) {
		lobbies = new Hashtable<String,WiFiLobbyDetails>() ;
		thread = null ;
		
		if ( delegate == null )
			throw new NullPointerException("Null delegate given!") ;
		if ( context == null )
			throw new NullPointerException("Null context given!") ;
		
		mwrDelegate = new WeakReference<Delegate>(delegate) ;
		mwrContext = new WeakReference<Context>(context) ;
		
		this.targetedAddresses = new ArrayList<SocketAddress>() ;
		this.targetedTimeLastResponse = new ArrayList<Long>() ;
		
		this.announcementPort = announcementPort ;
		this.queryPort = queryPort ;
		this.queryResponsePort = queryResponsePort ;
		
		this.timeToExpire = timeToExpire ;
		this.minTimeBetweenTargetQueries = minTimeBetweenTargetQueries ;
	}
	
	public boolean start() {
		if ( thread == null ) {
			// empty lobbies
			lobbies.clear() ;
			thread = new LobbyFinderCoordinatorThread( this, 5000, 15000 ) ;	// 5 seconds, target for 15.
			thread.start() ;
			return true ;
		}
		return false ;
	}
	
	public boolean resume() {
		return start() ;
	}
	
	public boolean stop() {
		if ( thread != null ) {
			thread.running = false ;
			// DON'T wait for the thread to die, it can
			// cause a frozen app: stop() is called by
			// a synchronized method, and the finder might
			// also be accessing a synchronized method from
			// within its thread.
			thread = null ;
			return true ;
		}
		return false ;
	}
	
	public boolean isStarted() {
		return thread != null && thread.running ;
	}
	
	public synchronized boolean targetIP( String ip ) {
		InetAddress inet;
		try {
			inet = InetAddress.getByName(ip);
			return targetIP( inet ) ;
		} catch (UnknownHostException e) {
			return false ;
		}
	}
	
	public synchronized boolean targetIP( InetAddress inet ) {
		SocketAddress saddr = new InetSocketAddress( inet, queryPort ) ;
		synchronized( targetedAddresses ) {
			if ( !targetedAddresses.contains(saddr) ) {
				targetedAddresses.add(saddr) ;
				targetedTimeLastResponse.add(Long.valueOf(0)) ;	
				
				// send an immediate target, if possible
				if ( thread != null && thread.running ) {
					thread.targetAddress(saddr) ;
				}
			}
		}
		return true ;
	}
	
	public synchronized boolean untargetIP( String ip ) {
		InetAddress inet;
		try {
			inet = InetAddress.getByName(ip);
			return untargetIP( inet ) ;
		} catch (UnknownHostException e) {
			return false ;
		}
	}
	
	public synchronized boolean untargetIP( InetAddress inet ) {
		SocketAddress saddr = new InetSocketAddress( inet, queryPort ) ;
		synchronized( targetedAddresses ) {
			int i = targetedAddresses.indexOf(saddr) ;
			if ( i >= 0 ) {
				targetedAddresses.remove(i) ;
				targetedTimeLastResponse.remove(i) ;
				return true ;
			}
		}
		return false ;
	}
	
	
	synchronized void lobbyReceived( WiFiLobbyDetails desc ) {
		String key = desc.getNonce().toString() ;
		
		Delegate delegate = mwrDelegate.get() ;
		
		if ( lobbies.containsKey( key ) ) {
			lobbies.put(key, desc) ;
			if ( delegate != null )
				delegate.lobbyFinderHasLobbyUpdate(this, key, desc) ;
		}
		else {
			lobbies.put(key, desc) ;
			if ( delegate != null )
				delegate.lobbyFinderFoundNewLobby(this, key, desc) ;
		}
		
		// mark received targets
		String ipReported = desc.getReceivedStatus().getReportedAddressHostName() ;
		String ipReceivedFrom = desc.getReceivedStatus().getReceivedFromAddressHostName() ;
		synchronized( this.targetedAddresses ) {
			for ( int i = 0; i < targetedAddresses.size(); i++ ) {
				String ip = ((InetSocketAddress)targetedAddresses.get(i)).getAddress().getHostAddress() ;
				if ( ip.equals(ipReported) || ip.equals(ipReceivedFrom) ) {
					this.targetedTimeLastResponse.set(i, Long.valueOf(System.currentTimeMillis())) ;
				}
			}
		}
	}
	
	synchronized void lobbyVanished( WiFiLobbyDetails desc ) {
		String key = desc.getNonce().toString() ;
		
		Delegate delegate = mwrDelegate.get() ;
		
		if ( lobbies.containsKey( key ) ) {
			lobbies.remove(key) ;
			if ( delegate != null )
				delegate.lobbyFinderLobbyVanished(this, key) ;
		}
	}
	
	@SuppressWarnings("unchecked")
	public synchronized Hashtable<String,WiFiLobbyDetails> getLobbies() {
		//Log.d(TAG, "allowing retrieval of lobbies with " + lobbies.size() + " entries ") ;
		return (Hashtable<String,WiFiLobbyDetails>) lobbies.clone();
	}
	
	
	class LobbyFinderCoordinatorThread extends Thread {
		
		public static final String TAG = "LFCoordinatorThread" ;
		
		boolean running ;
		
		WiFiLobbyFinder finder ;
		int timeBetweenUpdates ;
		int timeTargetingAddress ;
		
		Hashtable<String,WiFiLobbyDetails> lobbies ;
		Hashtable<String,Date> lastDiscovery ;
		
		LobbyFinderAnnouncementListenerThread announcementListenerThread ;
		LobbyFinderQueryThread queryThread ;
		
		public LobbyFinderCoordinatorThread( WiFiLobbyFinder finder, int timeBetweenUpdates, int timeTargetingAddress ) {
			//Log.d(TAG, "constructor") ;
			this.finder = finder ;
			this.timeBetweenUpdates = timeBetweenUpdates ;
			this.timeTargetingAddress = timeTargetingAddress ;
			lobbies = new Hashtable<String,WiFiLobbyDetails>() ;
			lastDiscovery = new Hashtable<String,Date>() ;
			
			running = true ;
		}
		
		@Override
		public void run() {
			while ( running ) {
				//Log.d(TAG, "main loop") ;
				lobbies.clear() ;
				
				// Launch threads; wait for them to stop.
				announcementListenerThread = new LobbyFinderAnnouncementListenerThread( this ) ;
				announcementListenerThread.start() ;
				queryThread = new LobbyFinderQueryThread( this ) ;
				queryThread.start() ;
				
				// Busy wait...
				while( running && ( announcementListenerThread.running || queryThread.running ) ) {
					try {
						Thread.sleep(10) ;
					} catch (InterruptedException e) {
		            }
				}
				
				// If still running, process.  Otherwise cleanup.
				if ( !running ) {
					announcementListenerThread.running = false ;
					queryThread.running = false ;
					//Log.d(TAG, "waiting for aLT") ;
					ThreadSafety.waitForThreadToTerminate(announcementListenerThread) ;
					//Log.d(TAG, "waiting for qT") ;
					ThreadSafety.waitForThreadToTerminate(queryThread) ;
					
					return ;
				}
				
				// Otherwise!  These objects have been performing edits to 'lobbies'.
				// Wait for completion...
				//Log.d(TAG, "waiting for aLT without setting running") ;
				ThreadSafety.waitForThreadToTerminate(announcementListenerThread) ;
				//Log.d(TAG, "waiting for qT without setting running") ;
				ThreadSafety.waitForThreadToTerminate(queryThread) ;
				
				// Every key in our lobbies has been updated.  Every key in finder.lobbies 
				// that is not in our lobbies has been removed.
				//Log.d(TAG, "reviewing updates") ;
				for ( Enumeration<String> e = lobbies.keys(); e.hasMoreElements(); ) {
					String key = e.nextElement() ;
					//Log.d(TAG, "telling finder about received update for key " + key) ;
					finder.lobbyReceived(lobbies.get(key)) ;
				}
				Date now = new Date() ;
				for ( Enumeration<String> e = lastDiscovery.keys(); e.hasMoreElements(); ) {
					String key = e.nextElement() ;
					if ( now.getTime() - lastDiscovery.get(key).getTime() > finder.timeToExpire ) {
						lastDiscovery.remove(key) ;
						finder.lobbyVanished(finder.lobbies.get(key)) ;
					}
				}
			}
		}
		
		
		public synchronized boolean targetAddress( SocketAddress saddr ) {
			// just send a message.  The query response thread will listen
			// for a response, one hopes...
			if ( running ) {
				// send an immediate query
				DatagramSocket socket_target = null ;
				LobbyMessage out_m = new LobbyMessage() ;
				out_m.setAsLobbyStatusRequest(finder.queryResponsePort) ;
				ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
				try {
					out_m.write(bos) ;
				} catch (IOException e) {
					return false ;
				}
				
				// Send the broadcast request.
				byte [] out_b = bos.toByteArray() ;
				try {
					socket_target = new DatagramSocket() ;
					socket_target.send( new DatagramPacket( out_b, out_b.length, saddr ) ) ;
				} catch( SocketException e ) {
					return false ;
				} catch (IOException e) {
					return false ;
				} finally {
					try {
						socket_target.close() ;
					} catch ( Exception e ) { }
				}
				
				
				return true ;
			}
			return false ;
		}
		
		
		public synchronized void addDescription( WiFiLobbyDetails desc ) {
			// We only include OTHER lobbies.
			if ( !desc.getStatus().getIPAddress().equals( Communication.getLocalIpAddress() ) ) {
				//Log.d(TAG, "adding description for key " + desc.getKey()) ;
				//Log.d(TAG, "has name " + desc.getName()) ;
				lobbies.put(desc.getNonce().toString(), desc) ;
				lastDiscovery.put(desc.getNonce().toString(), new Date()) ;
				
				finder.lobbyReceived(desc) ;
			}
		}
		
		
		// Three types of subthreads!  We always run AnnouncementListener and
		// Query threads; the third type, TargetedQuery, is prompted by a specific
		// user action.
		
		
		class LobbyFinderAnnouncementListenerThread extends Thread {
			public final static String TAG = "LFAListenerThread" ;
 			
			LobbyFinderCoordinatorThread coordinator ;
			
			boolean running = true ;
			
			public LobbyFinderAnnouncementListenerThread( LobbyFinderCoordinatorThread parentThread ) {
				coordinator = parentThread ;
			}
			
			@Override
			public void run() {
				long startTime = System.currentTimeMillis() ;
				DatagramSocket socket = null ;
				try {
					
					LobbyMessage lm = new LobbyMessage() ;
					// Listen for announcements coming in as UDP broadcasts.
					socket = new DatagramSocket( coordinator.finder.announcementPort ) ;
					socket.setSoTimeout(500) ;
					
					// Receive!
					DatagramPacket packet_in = new DatagramPacket( new byte[1024], 1024 ) ;
					
					//Log.d(TAG, "Entering announcment listen loop") ;
					while( running && System.currentTimeMillis() - startTime + 5 < coordinator.timeBetweenUpdates ) {
						try {
							socket.receive(packet_in) ;
							
							// Read in data
							ByteArrayInputStream bais = new ByteArrayInputStream( packet_in.getData() ) ;
							lm.resetForRead() ;
							
							if ( lm.read(bais) && lm.getType() == LobbyMessage.TYPE_LOBBY_STATUS ) {
								if ( lm.getName() != null && lm.getAge() > 0 && lm.getMaxPlayers() > 0
										&& lm.getSocketAddress() != null && lm.getNonce() != null && lm.getNumPlayers() >= 0 ) {
									try {
										SocketAddress reportedSA = lm.getSocketAddress() ;
										String reportedIP ;
										int reportedPort ;
										if ( reportedSA instanceof InetSocketAddress ) {
											reportedIP = ((InetSocketAddress)reportedSA).getAddress().getHostAddress() ;
											reportedPort = ((InetSocketAddress)reportedSA).getPort() ;
										} else {
											reportedIP = null ;
											reportedPort = 0 ;
										}
										String receivedFromIP = packet_in.getAddress().getHostAddress() ;
										SocketAddress receivedFromSA = new InetSocketAddress(receivedFromIP, reportedPort) ;
										
										WiFiLobbyDetails details
												= WiFiLobbyDetails.newDiscoveredInstance(
														receivedFromSA, receivedFromIP, 
														lm.getNonce(), reportedSA, reportedIP,
														lm.getName(), lm.getHostPlayerName(),
														lm.getNumPlayers(), lm.getMaxPlayers(), lm.getAge()) ;
										coordinator.addDescription(details) ;
									} catch ( Exception e ) {
										e.printStackTrace() ;
									}
								}
							}
							
						} catch( SocketTimeoutException e ) { 
							continue ;
						} catch( IOException e ) {
							Log.d(TAG, "IO Exception") ;
							e.printStackTrace() ;
							continue ;
						} catch (ClassNotFoundException e) {
							Log.d(TAG, "ClassNotFoundException") ;
							continue ;
						}
					}
				} catch ( SocketException e ) {
					// Hrmm
					Log.d(TAG, "SocketException") ;
					e.printStackTrace() ;
					running = false ;
				} finally {
					if ( socket != null )
						socket.close();
				}
				
				running = false ;
			}
		}
		
		
		class LobbyFinderQueryThread extends Thread {
			LobbyFinderCoordinatorThread coordinator ;
			
			boolean running = true ;
			
			public LobbyFinderQueryThread( LobbyFinderCoordinatorThread parentThread ) {
				coordinator = parentThread ;
			}
			
			@Override
			public void run() {
				long startTime = System.currentTimeMillis() ;
				DatagramSocket socket_broadcast = null ;
				DatagramSocket socket_target = null ;
				ServerSocket ss = null ;
				try {
					socket_broadcast = new DatagramSocket() ;
					socket_broadcast.setBroadcast(true) ;
					
					ss = new ServerSocket( coordinator.finder.queryResponsePort ) ;
					
					// broadcast our query request.
					LobbyMessage out_m = new LobbyMessage() ;
					out_m.setAsLobbyStatusRequest(coordinator.finder.queryResponsePort) ;
					ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
					try {
						out_m.write(bos) ;
					} catch (IOException e) {
						running = false ;
					}
					
					// Send the broadcast request.
					Context c = mwrContext.get() ;
					byte [] out_b = bos.toByteArray() ;
					InetAddress bcast_addr = AndroidCommunication.getWifiBroadcastAddress(c) ;
					socket_broadcast.send( new DatagramPacket( out_b, out_b.length, bcast_addr, coordinator.finder.queryPort ) ) ;
					c = null ;
					
					// Send out all our "targetted" requests.
					synchronized( targetedAddresses ) {
						if ( targetedAddresses.size() > 0 ) {
							socket_target = new DatagramSocket() ;
							for ( int i = 0; i < targetedAddresses.size(); i++ ) {
								long timeSinceResponse = System.currentTimeMillis() - targetedTimeLastResponse.get(i) ;
								if ( timeSinceResponse >= minTimeBetweenTargetQueries )
									socket_target.send( new DatagramPacket( out_b, out_b.length, targetedAddresses.get(i) ) ) ;
							}
						}
					}
					
					// Now listen.
					ss.setSoTimeout(500) ;
					
					while( running && System.currentTimeMillis() - startTime + 5 < coordinator.timeBetweenUpdates ) {
						// Accept, receive data, process.
						try {
							Socket sock = ss.accept() ;
						
							MessageReader mr  = new MessageReader( new LobbyMessage(), sock.getInputStream() ) ;
							mr.okToReadNextMessage() ;
							
							while( running
									&& ( mr.status() == MessageReader.STATUS_READING_MESSAGE
											|| mr.status() == MessageReader.STATUS_WAITING_FOR_MESSAGE )
									&& startTime - System.currentTimeMillis() < coordinator.timeBetweenUpdates ) {
								Thread.sleep(50) ;
							}
							
							if ( mr.status() == MessageReader.STATUS_MESSAGE_READY ) {
								LobbyMessage lm = (LobbyMessage)mr.getMessage() ;
								if ( lm.getType() == LobbyMessage.TYPE_LOBBY_STATUS ) {
									// sanity check
									if ( lm.getName() != null && lm.getAge() > 0 && lm.getMaxPlayers() > 0
											&& lm.getSocketAddress() != null && lm.getNonce() != null && lm.getNumPlayers() >= 0 ) {
										try {
											SocketAddress reportedSA = lm.getSocketAddress() ;
											String reportedIP = reportedSA instanceof InetSocketAddress ? ((InetSocketAddress)reportedSA).getAddress().getHostAddress() : null ;
											SocketAddress receivedFromSA = sock.getRemoteSocketAddress() ;
											String receivedFromIP = receivedFromSA instanceof InetSocketAddress ? ((InetSocketAddress)receivedFromSA).getAddress().getHostAddress() : null ;
											
											WiFiLobbyDetails details
													= WiFiLobbyDetails.newDiscoveredInstance(
															receivedFromSA, receivedFromIP, 
															lm.getNonce(), reportedSA, reportedIP,
															lm.getName(), lm.getHostPlayerName(),
															lm.getNumPlayers(), lm.getMaxPlayers(), lm.getAge()) ;
											coordinator.addDescription(details) ;
										} catch ( Exception e ) {
											e.printStackTrace() ;
										}
									}
								}
							}
							
							// close the socket and the message reader.
							try {
								mr.close() ;
								mr.stop() ;
								sock.close() ;
							} catch ( Exception e ) { }
						} catch( SocketTimeoutException e ) {
							continue ;
						} catch( IOException e ) {
							continue ;
						} catch (InterruptedException e) {
							continue ;
						}
						
					}
				} catch( SocketException e ) {
					running = false ;
				} catch( IOException e ) {
					running = false ;
				} finally {
					if ( ss != null ) {
						try {
							ss.close() ;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if ( socket_broadcast != null )
						socket_broadcast.close() ;
				}
				
				running = false ;
			}
		}
		
	}
}
