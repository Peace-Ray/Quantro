package com.peaceray.quantro.communications;

import android.os.Handler;

/**
 * A SlottedHandler extends handler with 'slotted messages': a slotted
 * message is one where 'obj' is set to Integer.valueOf(slot).
 * 
 * The purpose of this extension is to easily schedule and remove messages
 * for particular "slots": for example, a server handling multiple user connections
 * can refer to each user as a slot number and schedule their own operations.
 * 
 * arg1 / arg2 are still available for you to use for whatever.
 * 
 * Slots begin at 0 and are numbered { 0, ..., numSlot-1 }
 * 
 * @author Jake
 *
 */
public class SlottedHandler extends Handler {
	
	// We don't know for sure whether Handler.removeMessages()
	// compares using == or .equals(), or if this behavior is
	// even standardized across all Android versions.  To avoid
	// dealing with this issue, we create an array of Integer objects
	// to use for slot objs.
	
	private Integer [] mSlotObjs ;
	
	public SlottedHandler(int slots) {
		super() ;
		mSlotObjs = new Integer[slots] ;
		for ( int i = 0; i < slots; i++ )
			mSlotObjs[i] = Integer.valueOf(i) ;
	}
	
	/**
	 * Returns the slot set for this message, -1 if none.
	 * @param m
	 * @return
	 */
	public int getSlot(android.os.Message m) {
		if ( m.obj instanceof Integer )
			return ((Integer)m.obj).intValue() ;
		return -1 ;
	}
	
	public void sendSlottedMessage( int what, int slot ) {	
		sendMessage( obtainMessage(what, 0, 0, mSlotObjs[slot]) ) ;
	}
	
	public void sendSlottedMessage( int what, int slot, int arg1, int arg2 ) {
		sendMessage( obtainMessage(what, 0, 0, mSlotObjs[slot]) ) ;
	}
	
	public void sendSlottedMessageDelayed( int what, int slot, long delay ) {
		sendMessageDelayed(obtainMessage(what, 0, 0, mSlotObjs[slot]),
				delay) ;
	}
	
	public void sendSlottedMessageDelayed( int what, int slot, int arg1, int arg2, long delay ) {
		sendMessageDelayed(obtainMessage(what, 0, 0, mSlotObjs[slot]),
				delay) ;
	}
	
	public void removeSlottedMessages( int what, int slot ) {
		removeMessages(what, mSlotObjs[slot]) ;
	}
	
}
