package com.peaceray.quantro.view.options;

import java.util.Collection;
import java.util.Map;

import com.peaceray.quantro.content.Music;

public interface OptionsSoundMusicView {
	
	public interface Delegate {
		

		/**
		 * The user is attempting to set the provided Music as the current
		 * music used for gameplay.  The OptionsSoundMusicView (provided) will
		 * not take any action unless instructed to by the delegate.
		 * 
		 * One example for behavior is to call osv.setCurrentMusic( music ),
		 * which informs the view that the user selection was successful.
		 * 
		 * @param view
		 * @param skin
		 * @param availability As a reminder, this is the currently set
		 * 			OptionAvailability for this music.
		 * @return
		 */
		public void osmv_userSetCurrentMusic( OptionsSoundMusicView osmv, Music music, OptionAvailability availability ) ;
		
		/**
		 * The user is attempting to set the provided Music as the DEFAULT music
		 * for this gametype.  It's up to you to determine what that means exactly.
		 * 
		 * @param osmv
		 * @param music
		 * @param availability
		 */
		public void osmv_userSetDefaultMusic( OptionsSoundMusicView osmv, Music music, OptionAvailability availability ) ;
		
		/**
		 * The user wants to turn sound on or off.  We have made no internal changes.
		 * 
		 * It's up to the Delegate to determine how to perform this change -- unmute?
		 * Deactivate Mute By Ringer?
		 * 
		 * @param osmv
		 * @param on
		 */
		public void osmv_userSetSoundOn( OptionsSoundMusicView osmv, boolean on ) ;
		
		
		/**
		 * The user has set music volume to the specified level.  We have made
		 * no internal changes.
		 * 
		 * @param osmv
		 * @param volPercent
		 * @return Whether we should perform whatever (minimal) updates are necessary to
		 * 		reflect the new volume level.
		 */
		public boolean osmv_userSetMusicVolumePercent( OptionsSoundMusicView osmv, int volPercent ) ;
		
		
		/**
		 * The user has set sound volume to the specified level.  We have made no
		 * internal changes.
		 * 
		 * @param osmv
		 * @param volPercent
		 * @return Whether we should perform whatever (minimal) updates are necessary to
		 * 		reflect the new volume level.
		 */
		public boolean osmv_userSetSoundVolumePercent( OptionsSoundMusicView osmv, int volPercent ) ;
		
		
		/**
		 * The user wants to perform advance configuration.  We have made no internal
		 * changes.
		 * 
		 * @param osmv
		 */
		public void osmv_userAdvancedConfiguration( OptionsSoundMusicView osmv ) ;
		
	}
	
	
	/**
	 * Sets this View's Delegate.  May be called with 'null'.
	 * 
	 * @param delegate An implementation of Delegate.
	 */
	public void setDelegate( Delegate delegate ) ;
	
	
	/**
	 * Clear all Musics set in this view.
	 */
	public void clearMusic() ;

	
	/**
	 * Adds the specified Music to those displayed by the Options view.
	 * 'unlocked' indicates whether this music is available.
	 * 
	 * @param music
	 * @param unlocked
	 */
	public void addMusic( Music music, OptionAvailability availability ) ;
	
	
	/**
	 * Sets the specified musics and availability as the set displayed by this
	 * Options view.
	 * 
	 * Equivalent to calling clearMusics(), then addMusic() in succession.
	 * 
	 * @param musics
	 * @param availability
	 */
	public void setMusics( Collection<Music> musics, Map<Music, OptionAvailability> availability ) ;
	
	
	/**
	 * Sets the availability of the specified music, which must have been 
	 * previously added.
	 * 
	 * @param music
	 * @param availability
	 */
	public void setMusicAvailability( Music music, OptionAvailability availability ) ;
	
	
	/**
	 * Sets the availability of the provided musics, which must have been
	 * previously added.
	 * 
	 * @param availability
	 */
	public void setMusicAvailability( Map<Music, OptionAvailability> availability ) ;
	
	
	/**
	 * Sets the music currently selected by the user, either in response to
	 * user actions or via loading parameters.  Note that this OptionsView
	 * is meant to allow users to select options; it does not perform any
	 * underlying changes.  That happens through the Delegate, which (one
	 * presumes) is the same entity calling this method.
	 * 
	 * @param music
	 */
	public void setCurrentMusic( Music music ) ;
	
	
	/**
	 * Sets the current 'sound-on' status.  Has no effect other than
	 * what the user sees and the options available to ver.
	 * 
	 * @param on
	 * @param isMutedByRinger
	 */
	public void setSoundOn( boolean on, boolean isMutedByRinger ) ;
	
	
	/**
	 * Sets the current displayed sound volume.  Has no effect other than
	 * what the user sees and the options available to ver.
	 * 
	 * @param vol
	 */
	public void setSoundVolumePercent( int vol ) ;
	
	/**
	 * Sets the current displayed music volume.  Has no effect other than
	 * what the user sees and the options available to ver.
	 * 
	 * @param vol
	 */
	public void setMusicVolumePercent( int vol ) ;

}
