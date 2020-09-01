package com.peaceray.quantro.dialog;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import android.content.Context;
import android.widget.Toast;

/**
 * A serializable class allowing for Toasts to be 
 * constructed (i.e., their data set) before a Toast
 * is instantiated, and an instance of ToastBuilder
 * passed via on Intent (or the like) to the Activity
 * or Service which will actually instantiate the
 * Toast and show it.
 * 
 * As of 11/7, these toasts are very simple, comprised
 * of only text and a duration; however, feel free to
 * extend this class to any fancy Toasts you end up needing.
 * 
 * This class is meant to go hand-in-hand with ToastShowerService,
 * but isn't aware of that class internally.
 * 
 * @author Jake
 *
 */
public class ToastBuilder implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6809785805637813918L;
	
	
	private boolean as_res_id ;
	private int text_res_id ;
	private String text ;
	private int duration ;
	
	public ToastBuilder() {
		as_res_id = true ;
		text = null ;
		duration = -1 ;
	}
	
	
	public Toast build( Context context ) {
		if ( !as_res_id )
			return Toast.makeText(context, text, duration) ;
		else
			return Toast.makeText(context, text_res_id, duration) ;
	}
	
	/**
	 * Upon build(), will return a Toast created using
	 * makeText( context, resid, duration ).
	 * @param resid
	 * @param duration
	 * @return this object, for method chaining
	 */
	public ToastBuilder makeText( int resid, int duration ) {
		as_res_id = true ;
		text_res_id = resid ;
		this.duration = duration ;
		
		return this ;
	}
	
	public ToastBuilder makeText( CharSequence text, int duration ) {
		as_res_id = false ;
		this.text = new StringBuilder().append(text).toString() ;
		this.duration = duration ;
		
		return this ;
	}
	
	
	//////////////////////////////////////////////////////////////////////
	// METHODS FOR SERIALIZING
	//////////////////////////////////////////////////////////////////////
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeBoolean(as_res_id) ;
		stream.writeInt(text_res_id) ;
		stream.writeObject(text) ;
		stream.writeInt(duration) ;
	}
	
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		as_res_id = stream.readBoolean() ;
		text_res_id = stream.readInt();
		text = (String)stream.readObject() ;
		duration = stream.readInt() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required Challenge structure.") ;
	}
	
}
