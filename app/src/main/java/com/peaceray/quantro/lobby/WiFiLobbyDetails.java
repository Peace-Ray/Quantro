package com.peaceray.quantro.lobby;

import java.io.IOException;
import java.net.SocketAddress;

import android.os.Parcel;
import android.os.Parcelable;

import com.peaceray.quantro.communications.nonce.Nonce;


/**
 * A way of describing the details of a WiFi Lobby.
 * 
 * These details cover things like Nonce, address, host name,
 * population, etc.
 * 
 * It is intended as a partial replacement for WifiLobbyDetails,
 * incorporating more metadata that is not strictly relevant
 * for a WiFiLobbyFinder, such as "Source."
 * 
 * @author Jake
 *
 */
public class WiFiLobbyDetails implements Parcelable {
	
	/**
	 * Indicates the 'source' of this lobby in our records.  This is
	 * not the source of the lobby per se, but rather the source of
	 * our RECORD of this lobby.
	 * 
	 * Most lobbies will be discovered by a LobbyFinder automatically,
	 * without user interaction.  However, some will be the result of
	 * an 'invitation' (over Android Beam) and some manually located
	 * by entering an IP address.
	 * 
	 * In cases of multiple sources -- for example, a user receives
	 * an invitation for a lobby already on the list -- we allow
	 * one source to "override" another.
	 * 
	 * @author Jake
	 *
	 */
	public enum Source {
		/**
		 * Discovered using the standard listen / request broadcast packets.
		 */
		DISCOVERED,
		
		/**
		 * Invited via some method or another (e.g. Android Beam).
		 * The user has chosen to look for this lobby, but stopped just
		 * short of manually entering an IP address.
		 */
		INVITATION,
		
		/**
		 * Targeted by the user entering a specific IP address.
		 */
		TARGETED,
		
		/**
		 * We intend to create this lobby.
		 */
		INTENTION ;
		
		
		public boolean compatible( Source source ) {
			return this != INTENTION && source != INTENTION ;
		}
		
		/**
		 * Does this Source "override" the provided status?
		 * 
		 * One Source "overrides" another if it required more user
		 * interaction to put in place.
		 * @param status
		 * @return
		 */
		public boolean overrides( Source source ) {
			return compatible(source) && ordinal() > source.ordinal() ;
		}
		
		/**
		 * Merges the two sources into one, according to which
		 * overrides the other.
		 * @param source1
		 * @param source2
		 * @return
		 */
		public static Source merge( Source source1, Source source2 ) {
			if ( !source1.compatible(source2) )
				throw new IllegalArgumentException("" + source1 + " is not compatible with " + source2) ;
			if ( source1.overrides(source2) )
				return source1 ;
			return source2 ;
		}
	}
	
	
	public static abstract class Status implements Parcelable {
		public abstract String getLobbyName() ;
		public abstract String getHostName() ;
		
		public abstract int getMaxPeople() ;
		
		public abstract String getIPAddress() ;
	}
	
	
	public static class TargetStatus extends Status {
		private String mLobbyName ;
		private String mHostName ;
		
		private String mHostIPAddress ;
		
		private long mTargetedAt ;
		
		private TargetStatus( String lobbyName, String hostName, String hostIPAddress, long targetedAt ) {
			mLobbyName = lobbyName ;
			mHostName = hostName ;
			mHostIPAddress = hostIPAddress ;
			mTargetedAt = targetedAt ;
		}
		
		public String getLobbyName() {
			return mLobbyName ;
		}
		
		public String getHostName() {
			return mHostName ;
		}
		
		public int getMaxPeople() {
			throw new IllegalStateException("TargetStatus does not support getMaxPeople()") ;
		}
		
		public String getIPAddress() {
			return mHostIPAddress ;
		}
		
		public long getTargetedAt() {
			return mTargetedAt ;
		}
		
		/**
		 * Returns a new TargetStatus that attempts to merge the data in each
		 * provided.
		 * 
		 * @param ts1
		 * @param ts2
		 * @return
		 */
		private static TargetStatus merge( TargetStatus ts1, TargetStatus ts2 ) {
			// We prefer non-nulls to nulls.  In cases where both have non-nulls,
			// we take the more recent value.
			TargetStatus tsPrimary, tsSecondary ;
			if ( ts1.getTargetedAt() >= ts2.getTargetedAt() ) {
				tsPrimary = ts1 ;
				tsSecondary = ts2 ;
			} else {
				tsPrimary = ts2 ;
				tsSecondary = ts1 ;
			}
			
			return new TargetStatus(
					tsPrimary.mLobbyName != null 		? tsPrimary.mLobbyName 		: tsSecondary.mLobbyName,
					tsPrimary.mHostName != null 		? tsPrimary.mHostName 		: tsSecondary.mHostName,
					tsPrimary.mHostIPAddress != null 	? tsPrimary.mHostIPAddress 	: tsSecondary.mHostIPAddress,
					tsPrimary.mTargetedAt ) ;
		}
		
		
		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel out, int flags) {
			// in order: lobby name, host name, IP, targetedAt
			out.writeString(mLobbyName) ;
			out.writeString(mHostName) ;	
			out.writeString(mHostIPAddress) ;		
			out.writeLong(mTargetedAt) ;
		}

		public static final Parcelable.Creator<TargetStatus> CREATOR
				= new Parcelable.Creator<TargetStatus>() {
			public TargetStatus createFromParcel(Parcel in) {
				return new TargetStatus(in);
			}
			
			public TargetStatus[] newArray(int size) {
				return new TargetStatus[size];
			}
		};
	     
		private TargetStatus(Parcel in) {
			// in order: lobby name, host name, IP, targetedAt
			mLobbyName = in.readString() ;
			mHostName = in.readString() ;
			mHostIPAddress = in.readString() ;
			mTargetedAt = in.readLong() ;
		}
	}
	
	
	/**
	 * Time-sensitive lobby "status," received from the host.
	 * 
	 * This status message includes lobby name, host name, the
	 * time the lobby was started, population info, and its
	 * current IP address as described by host.
	 * 
	 * It also features the source IP address, if different from
	 * that reported by the host.
	 * 
	 * Since this class represents the status received from a lobby
	 * in a single message, it is immutable.
	 * 
	 * @author Jake
	 *
	 */
	public static class ReceivedStatus extends Status {
		private String mLobbyName ;
		private String mHostName ;
		private long mTimeStarted ;
		private int mNumPeople ;
		private int mMaxPeople ;
		
		private SocketAddress mReportedAddress ;
		private String mReportedAddressHostName ;
		
		private SocketAddress mReceivedFromAddress ;
		private String mReceivedFromAddressHostName ;
		
		private long mReceivedAt ;
		
		
		private ReceivedStatus( String lobbyName, String hostName, long timeStarted,
				int numPeople, int maxPeople, 
				SocketAddress reportedAddress, String reportedIP,
				SocketAddress receivedFromAddress, String receivedFromIP,
				long receivedAt ) {
			mLobbyName = lobbyName ;
			mHostName = hostName ;
			mTimeStarted = timeStarted ;
			mNumPeople = numPeople ;
			mMaxPeople = maxPeople ;
			
			mReportedAddress = reportedAddress ;
			mReportedAddressHostName = reportedIP ;
			
			mReceivedFromAddress = receivedFromAddress ;
			mReceivedFromAddressHostName = receivedFromIP ;
			
			mReceivedAt = receivedAt ;
		}
		
		
		////////////////////////////////////////////////////////////////////////
		//
		// ACCESSORS
		//
		// Direct and indirect access to inner data
		//
		
		public String getLobbyName() {
			return mLobbyName ;
		}
		
		public String getHostName() {
			return mHostName ;
		}
		
		public int getNumPeople() {
			return mNumPeople ;
		}
		
		public int getMaxPeople() {
			return mMaxPeople ;
		}
		
		public long getTimeStarted() {
			return mTimeStarted ;
		}
		
		public long getAge() {
			return System.currentTimeMillis() - mTimeStarted ;
		}
		
		public SocketAddress getReportedAddress() {
			return mReportedAddress ;
		}
		
		public String getReportedAddressHostName() {
			return mReportedAddressHostName ;
		}
		
		public SocketAddress getReceivedFromAddress() {
			return mReceivedFromAddress ;
		}
		
		public String getReceivedFromAddressHostName() {
			return mReceivedFromAddressHostName ;
		}
		
		public SocketAddress getAddress() {
			return getReceivedFromAddress() ;
		}
		
		public String getIPAddress() {
			return getReceivedFromAddressHostName() ;
		}
		
		/**
		 * Did we receive this status message from the same
		 * (locally visible) IP address as the host remotely
		 * reported?
		 * @return
		 */
		public boolean getReceivedFromReportedAddress() {
			return mReportedAddressHostName.equals(mReceivedFromAddressHostName) ;
		}
		
		public long getReceivedAt() {
			return mReceivedAt ;
		}
		
		public long getTimeSinceReceived() {
			return System.currentTimeMillis() - mReceivedAt ;
		}
		
		
		////////////////////////////////////////////////////////////////////////
		//
		// PARCELABLE
		//
		
		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel out, int flags) {
			// in order:
			// lobby reported state (names, time started, people)
			// reported location,
			// received location,
			// "received at"
			out.writeString(mLobbyName) ;
			out.writeString(mHostName) ;
			out.writeLong(mTimeStarted) ;
			out.writeInt(mNumPeople) ;
			out.writeInt(mMaxPeople) ;
			
			out.writeSerializable(mReportedAddress) ;
			out.writeString(mReportedAddressHostName) ;
			
			out.writeSerializable(mReceivedFromAddress) ;
			out.writeString(mReceivedFromAddressHostName) ;
			
			out.writeLong(mReceivedAt) ;
		}

		public static final Parcelable.Creator<ReceivedStatus> CREATOR
				= new Parcelable.Creator<ReceivedStatus>() {
			public ReceivedStatus createFromParcel(Parcel in) {
				return new ReceivedStatus(in);
			}
			
			public ReceivedStatus[] newArray(int size) {
				return new ReceivedStatus[size];
			}
		};
	     
		private ReceivedStatus(Parcel in) {
			// in order:
			// lobby reported state (names, time started, people)
			// reported location,
			// received location,
			// "received at"
			mLobbyName = in.readString() ;
			mHostName = in.readString() ;
			mTimeStarted = in.readLong() ;
			mNumPeople = in.readInt() ;
			mMaxPeople = in.readInt() ;
			
			mReportedAddress = (SocketAddress)in.readSerializable() ;
			mReportedAddressHostName = in.readString() ;
			
			mReceivedFromAddress = (SocketAddress)in.readSerializable() ;
			mReceivedFromAddressHostName = in.readString() ;
			
			mReceivedAt = in.readLong() ;
		}
		
		//
		////////////////////////////////////////////////////////////////////////
		
	}
	
	
	public static class IntentionStatus extends Status {
		private String mLobbyName ;
		private String mHostName ;
		
		private int mMaxPeople ;
		
		private IntentionStatus( String lobbyName, String hostName, int maxPeople ) {
			mLobbyName = lobbyName ;
			mHostName = hostName ;
			mMaxPeople = maxPeople ;
		}
		
		public String getLobbyName() {
			return mLobbyName ;
		}
		
		public String getHostName() {
			return mHostName ;
		}
		
		public String getIPAddress() {
			return null ;
		}
		
		public int getMaxPeople() {
			return mMaxPeople ;
		}
		
		////////////////////////////////////////////////////////////////////////
		//
		// PARCELABLE
		//
		
		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel out, int flags) {
			// in order: lobby name, host name, maxPeople
			out.writeString(mLobbyName) ;
			out.writeString(mHostName) ;			
			out.writeInt(mMaxPeople) ;
		}

		public static final Parcelable.Creator<IntentionStatus> CREATOR
				= new Parcelable.Creator<IntentionStatus>() {
			public IntentionStatus createFromParcel(Parcel in) {
				return new IntentionStatus(in);
			}
			
			public IntentionStatus[] newArray(int size) {
				return new IntentionStatus[size];
			}
		};
	     
		private IntentionStatus(Parcel in) {
			// in order: lobby name, host name, max people
			mLobbyName = in.readString() ;
			mHostName = in.readString() ;
			mMaxPeople = in.readInt() ;
		}
		
		//
		////////////////////////////////////////////////////////////////////////
	}
	
	
	private Source mSource ;
	private Nonce mNonce ;
	
	private Status mStatus ;
	
	private WiFiLobbyDetails( Source source, Nonce nonce, Status status ) {
		mSource = source ;
		mNonce = nonce ;
		mStatus = status ;
	}
	
	
	@Override
	public WiFiLobbyDetails clone() {
		// source, nonce, and status are all immutable objects.
		// Only the assignment of these variables within a WiFiLobbyDetails
		// object can change.  We can safely reference the same objects.
		return new WiFiLobbyDetails( mSource, mNonce, mStatus ) ;
	}
	
	
	public static WiFiLobbyDetails newDiscoveredInstance( 
			SocketAddress receivedFromSocketAddress, String receivedFromIpAddress,
			Nonce nonce, SocketAddress reportedAddress, String reportedIP,
			String lobbyName, String hostName, int numPlayers, int maxPlayers, long age ) {
		if ( nonce == null )
			throw new NullPointerException("Discovered instances must include a nonce") ;
		if ( receivedFromIpAddress == null )
			throw new NullPointerException("Discovered instances must include a 'received from' ip address") ;
		if ( reportedAddress == null )
			throw new NullPointerException("Discovered instances must include a 'reported as' address") ;
		if ( reportedIP == null )
			throw new NullPointerException("Discovered instances must include a 'reported as' ip address") ;
		
		
		Source source = Source.DISCOVERED ;
		long currentTime = System.currentTimeMillis() ;
		Status status = new ReceivedStatus( lobbyName, hostName, currentTime - age,
				numPlayers, maxPlayers, 
				reportedAddress, reportedIP,
				receivedFromSocketAddress, receivedFromIpAddress,
				currentTime ) ;
		return new WiFiLobbyDetails( source, nonce, status ) ;
	}
	
	public static WiFiLobbyDetails newInvitedInstance( Nonce nonce, String ipAddress, String lobbyName, String hostName ) {
		Source source = Source.INVITATION ;
		Status status = new TargetStatus( lobbyName, hostName, ipAddress, System.currentTimeMillis() ) ;
		return new WiFiLobbyDetails( source, nonce, status ) ;
	}
	
	public static WiFiLobbyDetails newTargetedInstance( String ipAddress ) {
		Source source = Source.TARGETED ;
		Status status = new TargetStatus( null, null, ipAddress, System.currentTimeMillis() ) ;
		return new WiFiLobbyDetails( source, null, status ) ;
	}
	
	public static WiFiLobbyDetails newIntentionInstance( String lobbyName, String hostName, int maxPeople ) {
		Source source = Source.INTENTION ;
		Status status = new IntentionStatus( lobbyName, hostName, maxPeople ) ;
		return new WiFiLobbyDetails( source, new Nonce(), status ) ;
	}
	
	
	public Source getSource() {
		return mSource ;
	}
	
	public Nonce getNonce() {
		return mNonce ;
	}
	
	public Status getStatus() {
		return mStatus ;
	}
	
	public boolean hasReceivedStatus() {
		return mStatus instanceof ReceivedStatus ;
	}
	
	public ReceivedStatus getReceivedStatus() {
		return hasReceivedStatus() ? (ReceivedStatus)mStatus : null ;
	}
	
	
	/**
	 * Do these two LobbyDetails represent, as best as we can tell, the same lobby?
	 * 
	 * Two Details are the same lobby if the share a Nonce, or, in the absence of Nonce
	 * for one (or both), have the same IP address.
	 * 
	 * @param details
	 * @return
	 */
	public boolean isSameLobby( WiFiLobbyDetails details ) {
		return isSameLobby( this, details ) ;
	}
	
	
	/**
	 * Do these two LobbyDetails represent, as best as we can tell, the same lobby?
	 * 
	 * Two Details are the same lobby if the share a Nonce, or, in the absence of Nonce
	 * for one (or both), have the same IP address.
	 * 
	 * No two intention represent the same lobby: each intention represents a "new lobby to be,"
	 * and thus when created they will be different.
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static boolean isSameLobby( WiFiLobbyDetails d1, WiFiLobbyDetails d2 ) {
		if ( d1 == null || d2 == null )
			return false ;
		
		if ( d1.mSource == Source.INTENTION || d2.mSource == Source.INTENTION )
			return false ;
		
		if ( d1.mNonce != null && d2.mNonce != null )
			return d1.mNonce.equals(d2.mNonce) ;
		
		// At least one has a null nonce, meaning at least one was entered
		// by the user.  Compare by examining IP addresses.
		String ip ;
		Status compareAgainstStatus ;
		if ( d1.mNonce == null ) {
			ip = d1.mStatus.getIPAddress() ;
			compareAgainstStatus = d2.mStatus ;
		} else {
			ip = d2.mStatus.getIPAddress() ;
			compareAgainstStatus = d1.mStatus ;
		}
		
		// These are the same lobby if the targeted IP address
		// (ip) matches the address of 'compareAgainst.'
		if ( compareAgainstStatus instanceof TargetStatus ) {
			// only one ip to compare
			return ip.equals(compareAgainstStatus.getIPAddress()) ;
		} else if ( compareAgainstStatus instanceof ReceivedStatus ) {
			// Received: we have two IPs to consider; the one reported by
			// the user (their personally-visible IP) and the source of
			// the status message (our locally-visible IP).  We assume
			// first that the user's REPORTED IP address is the address
			// we attempted to target (either by receiving an invitation
			// from that user, or by manually entering an IP address they
			// told us about).  We compare that first.
			ReceivedStatus rs = (ReceivedStatus)compareAgainstStatus ;
			if ( ip.equals(rs.getReportedAddressHostName()) )
				return true ;
			return ip.equals(rs.getReceivedFromAddressHostName()) ;
		}
		
		throw new IllegalArgumentException("Comparison failed for unknown reasons; is " + compareAgainstStatus + " a valid status?") ;
	}
	
	
	/**
	 * Returns a new WiFiLobbyDetails instance, representing the "merging" of
	 * the two provided.
	 * 
	 * Merging is appropriate only when both details represent the same lobby;
	 * this method will throw an Exception if isSameLobby( d1, d2 ) is false.
	 * 
	 * The merged result attempts to combine all information in both details
	 * in an intelligent way.  The new 'Source' will be the overriding source
	 * among the two provided, the most recently ReceivedStatus will take priority,
	 * a Nonce from either will overwrite 'null', etc.
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static WiFiLobbyDetails merge( WiFiLobbyDetails d1, WiFiLobbyDetails d2 ) {
		WiFiLobbyDetails destination = new WiFiLobbyDetails( null, null, null ) ;
		merge( d1, d2, destination ) ;
		return destination ;
	}
	
	/**
	 * Performs a merge from the provided parameter into this one; this object
	 * will represent the "merging" of its old values and the provided update.
	 * 
	 * Merging is appropriate only when both details represent the same lobby;
	 * this method will throw an Exception if isSameLobby( this, update ) is false.
	 * 
	 * The merged result attempts to combine all information in both details
	 * in an intelligent way.  This instance's 'source' will be the overriding source
	 * among the two provided, the most recently ReceivedStatus will take priority,
	 * a Nonce from the update will replace a local 'null', etc.
	 * 
	 * This operation will not affect 'update' in any way; it will have exactly
	 * the same values after this call.
	 * 
	 * @param source
	 */
	public void mergeFrom( WiFiLobbyDetails update ) {
		merge( this, update, this ) ;
	}
	
	/**
	 * Performs a merge from this instance into the provided parameter; the
	 * object provided will represent the "merging" of its old values and this
	 * instance.
	 * 
	 * Merging is appropriate only when both details represent the same lobby;
	 * this method will throw an Exception if isSameLobby( this, destination ) is false.
	 * 
	 * The merged result attempts to combine all information in both details
	 * in an intelligent way.  destination's 'source' will be the overriding source
	 * among the two provided, the most recently ReceivedStatus will take priority,
	 * a Nonce from this object will replace a 'null' in destination, etc.
	 * 
	 * This operation will not affect this instance in any way; it will have exactly
	 * the same values after this call.
	 * 
	 * @param destination
	 */
	public void mergeInto( WiFiLobbyDetails destination ) {
		merge( this, destination, destination ) ;
	}
	
	
	/**
	 * Our merge workhorse.  When finished, the provided object 'dest' will
	 * contain the "merged details" of the two provided sources.
	 * 
	 * This method is private, and is called from other, public methods;
	 * see their documentation for details regarding a merge operation.
	 * 
	 * POSTCONDITION:
	 * 		src1 will be unchanged IFF src1 != dest
	 * 		src2 will be unchanged IFF src2 != dest
	 * 		dest will represent the merged version of src1, src2
	 * 
	 * @param src1
	 * @param src2
	 * @param dest
	 */
	private static void merge( WiFiLobbyDetails src1, WiFiLobbyDetails src2, WiFiLobbyDetails dest ) {
		if ( !isSameLobby( src1, src2 ) )
			throw new IllegalArgumentException("Provided WiFiLobbyDetails instances do not represent the same lobby.") ;
		
		// set source to the overriding source ('Source' provides a merge operation).
		dest.mSource = Source.merge(src1.mSource, src2.mSource) ;
		// set the nonce; they are either the same between sources, or at least 1 is null.
		dest.mNonce = src1.mNonce != null ? src1.mNonce : src2.mNonce ;
		// set the status.
		// 1. If both are Received, take the most recent.
		// 2. If one is Received, take it.
		// 3. If neither is Received, make a new TargetStatus by
		// 		merging the two sources (this operation is provided by TargetStatus).
		if ( src1.mStatus instanceof ReceivedStatus && src2.mStatus instanceof ReceivedStatus ) {
			ReceivedStatus rs1 = (ReceivedStatus) src1.mStatus ;
			ReceivedStatus rs2 = (ReceivedStatus) src2.mStatus ;
			
			dest.mStatus = rs1.getReceivedAt() >= rs2.getReceivedAt() ? rs1 : rs2 ;
		} else if ( src1.mStatus instanceof ReceivedStatus || src2.mStatus instanceof ReceivedStatus ) {
			dest.mStatus = src1.mStatus instanceof ReceivedStatus ? src1.mStatus : src2.mStatus ;
		} else {
			TargetStatus ts1 = (TargetStatus) src1.mStatus ;
			TargetStatus ts2 = (TargetStatus) src2.mStatus ;
			
			dest.mStatus = TargetStatus.merge(ts1, ts2) ;
		}
	}
	
	
	public int describeContents() {
		return 0;
	}
	
	public void writeToParcel(Parcel out, int flags) {
		// source, nonce, status
		out.writeInt(mSource.ordinal()) ;
		out.writeString(mNonce == null ? null : mNonce.toString()) ;
		out.writeString(mStatus.getClass().getName()) ;
		mStatus.writeToParcel(out, flags) ;
	}
	
	public static final Parcelable.Creator<WiFiLobbyDetails> CREATOR
	        = new Parcelable.Creator<WiFiLobbyDetails>() {
	    public WiFiLobbyDetails createFromParcel(Parcel in) {
	        return new WiFiLobbyDetails(in);
	    }
	
	    public WiFiLobbyDetails[] newArray(int size) {
	        return new WiFiLobbyDetails[size];
	    }
	};
	
	private WiFiLobbyDetails(Parcel in) {
		
	    // source, nonce, status
		mSource = Source.values()[in.readInt()] ;
		String nonceStr = in.readString() ;
		try {
			mNonce = nonceStr == null ? null : new Nonce(nonceStr) ;
		} catch ( IOException ioe ) {
			mNonce = null ;
		}
		String statusClassName = in.readString() ;
		if ( statusClassName.equals(IntentionStatus.class.getName()) ) {
			mStatus = IntentionStatus.CREATOR.createFromParcel(in) ;
		} else if ( statusClassName.equals(TargetStatus.class.getName()) ) {
			mStatus = TargetStatus.CREATOR.createFromParcel(in) ;
		} else if ( statusClassName.equals(ReceivedStatus.class.getName()) ) {
			mStatus = ReceivedStatus.CREATOR.createFromParcel(in) ;
		} else {
			throw new IllegalStateException("Can't recreate with status class " + statusClassName) ;
		}
		
	}
	
}
