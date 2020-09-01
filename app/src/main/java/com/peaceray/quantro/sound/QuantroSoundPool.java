package com.peaceray.quantro.sound;

import java.io.FileDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.peaceray.quantro.content.Music;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;


/**
 * A wrapper for SoundPool and MediaPlayer that allows sound clips for specific game events
 * to be played by various program components without those components monitoring
 * sound IDs and sound files.
 * 
 * A simple wrapper such as this should not be expected to load all of its
 * assets without help.  Its main function is to allow sounds to be played
 * based on their type rather than arbitrary sound IDs; in other words, it
 * handles only soundID and streamID management, and some very basic MediaPlayer
 * functionality.
 * 
 * Usage: load it up with sound and go to town.  Music loops forever,
 * 		other sounds will not loop (play only once).
 * 
 * 		Music can be directly controlled using the play, pause, stop
 * 		commands.  'Play' will resume paused music or start from the beginning
 * 		otherwise.
 * 
 * 		Music should be provided as two pieces, an intro and a looping component.
 * 		The intro will be played first, and only once (replaying if the music
 * 		is stopped, not paused, and then played again), before the player transitions
 * 		to the looping component.  If the looping component is missing, the sound
 * 		pool plays only the intro (once).  If the intro component is missing, the
 * 		sound pool plays only the looping component.
 * 
 * 		Sounds, if loaded, will be played upon cue.
 * 
 * 		Use 'mute' and 'unmute' to silence everything as-needed.
 * 		Muting will automagically pause any music playing and stop
 * 		any sound playback (EXCEPT for the sound of "pausing", so
 * 		feel free to play a pause sound and then mute without delay
 * 		if that's your thing).
 * 
 * 		Calls to play music or sounds will silently (get it?) fail if
 * 		the pool is muted.
 * 
 * 		Unmuting will allow these calls to succeed once again; music
 * 		will be resumed IF it would be playing had 'mute' and 'unmute'
 * 		not been called.
 * 
 * @author Jake
 *
 */
public class QuantroSoundPool implements MediaPlayer.OnCompletionListener {
	
	public static final String TAG = "QuantroSoundPool" ;
	
	// The types of sounds we can play, aside from music, which gets
	// special consideration.
	
	private static int enm = 0 ;
	
	// Player actions: these represent (successful) button-presses by
	// the player.
	public final static int SOUND_TYPE_MOVE_LEFT = 			enm++ ;			// 0
	public final static int SOUND_TYPE_MOVE_RIGHT = 		enm++ ;
	public final static int SOUND_TYPE_MOVE_LEFT_RIGHT = 	enm++ ;
	public final static int SOUND_TYPE_MOVE_LEFT_FAILED = 	enm++ ;
	public final static int SOUND_TYPE_MOVE_RIGHT_FAILED = 	enm++ ;
	public final static int SOUND_TYPE_MOVE_LEFT_RIGHT_FAILED = enm++ ;		// 5
	public final static int SOUND_TYPE_TURN_CCW = 			enm++ ;
	public final static int SOUND_TYPE_TURN_CW = 			enm++ ;
	public final static int SOUND_TYPE_TURN_CCW_CW = 		enm++ ;
	public final static int SOUND_TYPE_FLIP = 				enm++ ;
	public final static int SOUND_TYPE_DROP = 				enm++ ;			// 10
	public final static int SOUND_TYPE_RESERVE = 			enm++ ;
	public final static int SOUND_TYPE_ATTACK = 			enm++ ;			// 12
	// Animation effects: in-game events that the player
	// does not directly control.
	public final static int SOUND_TYPE_LOCK = 				enm++ ;			// 13
	public final static int SOUND_TYPE_LAND = 				enm++ ;
	public final static int SOUND_TYPE_CLEAR_EMPH = 		enm++ ;			// 15
	public final static int SOUND_TYPE_CLEAR = 				enm++ ;
	public final static int SOUND_TYPE_ROW_PUSH_GARBAGE = 	enm++ ;
	public final static int SOUND_TYPE_ROW_PUSH_ADD = 		enm++ ;
	public final static int SOUND_TYPE_ROW_PUSH_SUBTRACT = 	enm++ ;
	public final static int SOUND_TYPE_ROW_PUSH_ZERO = 		enm++ ;			// 20
	public final static int SOUND_TYPE_PENALTY = 			enm++ ;			// 21
	
	public final static int SOUND_TYPE_DISPLACEMENT_ACCEL = enm++ ;
	public final static int SOUND_TYPE_ENTER_BLOCKS = 		enm++ ;
	public final static int SOUND_TYPE_SPECIAL_UPGRADED	=	enm++ ;			// 24
	public final static int SOUND_TYPE_ATTACKED_BY_BLOCKS =	enm++ ;
	public final static int SOUND_TYPE_COLORBLIND_METAMORPHOSIS = enm++ ;
	public final static int SOUND_TYPE_RISE_AND_FADE = 		enm++ ;			// 27
	// HUZZAH!
	public final static int SOUND_TYPE_COLUMN_UNLOCK = 		enm++ ;
	// win/lose/level?
	public final static int SOUND_TYPE_LEVEL_UP 			= enm++ ;		// 29
	public final static int SOUND_TYPE_GAME_LOSS 			= enm++ ;
	public final static int SOUND_TYPE_GAME_WIN 			= enm++ ;		// 31
	// pause/resume
	public final static int SOUND_TYPE_PAUSE 				= enm++ ;
	public final static int SOUND_TYPE_RESUME 				= enm++ ;
	// get ready!
	public final static int SOUND_TYPE_GET_READY 			= enm++ ;
	
	public final static int NUM_IN_GAME_SOUND_TYPES 		= enm ;			// 35
	
	// Menu button presses
	public final static int SOUND_TYPE_MENU_BUTTON_CLICK 			= enm++ ;	// 36
	public final static int SOUND_TYPE_MENU_BUTTON_HOLD 			= enm++ ;	
	public final static int SOUND_TYPE_MENU_BACK_BUTTON_CLICK 		= enm++ ;	
	public final static int SOUND_TYPE_MENU_BUTTON_FLIP 			= enm++ ;
	// Lobby Sounds!
	// User status changes
	public final static int SOUND_TYPE_LOBBY_USER_JOIN 				= enm++ ;	// 40
	public final static int SOUND_TYPE_LOBBY_USER_QUIT 				= enm++ ;
	public final static int SOUND_TYPE_LOBBY_USER_GO_ACTIVE 		= enm++ ;
	public final static int SOUND_TYPE_LOBBY_USER_GO_INACTIVE 		= enm++ ;
	// Other people status changes
	public final static int SOUND_TYPE_LOBBY_OTHER_JOIN 			= enm++ ;	// 44
	public final static int SOUND_TYPE_LOBBY_OTHER_QUIT 			= enm++ ;	// 45
	public final static int SOUND_TYPE_LOBBY_OTHER_GO_ACTIVE 		= enm++ ;
	public final static int SOUND_TYPE_LOBBY_OTHER_GO_INACTIVE 		= enm++ ;
	// Chatting
	public final static int SOUND_TYPE_LOBBY_USER_CHAT 				= enm++ ;
	public final static int SOUND_TYPE_LOBBY_OTHER_CHAT 			= enm++ ;
	// Game launches!
	public final static int SOUND_TYPE_LOBBY_LAUNCH_COUNTDOWN_START = enm++ ;	// 50
	public final static int SOUND_TYPE_LOBBY_LAUNCH_COUNTDOWN_CANCEL = enm++ ;
	public final static int SOUND_TYPE_LOBBY_LAUNCH_GO = enm++ ;				// 52
	
	
	
	public final static int NUM_SOUND_TYPES = enm ;		// 52
	
	
	// Here's where we store our stuff.
	private Music mMusic ;
	private MediaPlayer mMusicMPIntro ;
	private MediaPlayer mMusicMPLoop ;
	private boolean mMusicManualTransition ;
	
	private float mMusicVolume ;
	private boolean mMusicPlaying ;		// necessary to resume after mute, or not.
	private boolean mMusicIntroFinished ;
	
	// For now, completely generic, w.r.t. QO or #rows, etc.?
	private int [] mSoundResID ;		// If set by Res ID, we recall which was used.  Otherwise is 0.
	private int [] mSoundID ;
	private int [] mSoundPriority ;
	private int [] mSoundStreamID ;		// used only for the "most recent" stream,
										// so this may fail if multiple sounds of
										// the same type are played.
	private float mInGameSoundVolume ;
	private float mOutOfGameSoundVolume ;
	
	private SoundPool mSoundPool ;
	private AudioManager mAudioManager ;
	
	
	private boolean mSilent ;
	private boolean mMuteWithRinger ;
	
	
	public QuantroSoundPool( Context context, int maxStreams ) {
		if ( maxStreams < 0 )
			maxStreams = 0 ;
		mSoundPool = new SoundPool( maxStreams, AudioManager.STREAM_MUSIC, 0 ) ;
		mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE) ;
		
		mMusicMPIntro = null ;
		mMusicMPLoop = null ;
		mMusicPlaying = false ;
		mMusicIntroFinished = false ;
		mMusicManualTransition = true ;		// we manually transition from Intro to Loop.
		
		// allocate
		mMusicVolume = 1.0f ;
		mInGameSoundVolume = 1.0f ;
		mOutOfGameSoundVolume = 1.0f ;
		
		mSoundResID = new int[NUM_SOUND_TYPES] ;
		mSoundID = new int[NUM_SOUND_TYPES] ;
		mSoundPriority = new int[NUM_SOUND_TYPES] ;
		mSoundStreamID = new int[NUM_SOUND_TYPES] ;
		
		mSilent = false ;
		mMuteWithRinger = false ;
	}
	
	
	synchronized public Music getMusic() {
		return mMusic ;
	}
	
	synchronized public void loadMusic( Context context, Music music ) {
		if ( !Music.equals(mMusic, music) ) {
			boolean playing = this.isPlayingMusic() ;
			if ( playing ) {
				stopMusic() ;
			}
			if ( music != null && music.getHasIntro() )
				loadMusicIntro( context, music.getIntroResourceID() ) ;
			else
				unloadMusicIntro() ;
			
			if ( music != null && music.getHasLoop() )
				loadMusicLoop( context, music.getLoopResourceID() ) ;
			else
				unloadMusicLoop() ;
			
			mMusic = music ;
			if ( playing ) {
				playMusic() ;
			}
		}
	}
	
	// set music!
	synchronized private void loadMusicIntro( Context context, int resid ) {
		unloadMusicIntro() ;
		mMusicMPIntro = MediaPlayer.create(context, resid) ;
		mMusicMPIntro.setVolume(mMusicVolume, mMusicVolume) ;
		mMusicMPIntro.setOnCompletionListener(this) ;
		mMusicMPIntro.setLooping(false) ;
		
		setGaplessTransition() ;
	}
	
	synchronized private void loadMusicLoop( Context context, int resid ) {
		unloadMusicLoop() ;
		mMusicMPLoop = MediaPlayer.create(context, resid) ;
		mMusicMPLoop.setVolume(mMusicVolume, mMusicVolume) ;
		mMusicMPLoop.setLooping(true) ;
		
		setGaplessTransition() ;
	}
	
	private void setGaplessTransition() {
		mMusicManualTransition = true ;
		if ( mMusicMPIntro == null )
			return ;
		
		// attempt to call 'setNextMediaPlayer.'  This is
		// an Android 4.1 (API 16) call, so use reflection.
		try {
			Method setNext = MediaPlayer.class.getMethod("setNextMediaPlayer", MediaPlayer.class) ;
			setNext.invoke(mMusicMPIntro, mMusicMPLoop) ;
			mMusicManualTransition = false ;
		} catch (SecurityException e) {
			// nothing
		} catch (NoSuchMethodException e) {
			// nothing
		} catch (IllegalArgumentException e) {
			// nothing
		} catch (IllegalAccessException e) {
			// nothing
		} catch (InvocationTargetException e) {
			// nothing
		}
	}
	
	synchronized public void unloadMusic() {
		unloadMusicIntro() ;
		unloadMusicLoop() ;
		mMusic = null ;
	}
	
	synchronized private void unloadMusicIntro() {
		if ( mMusicMPIntro != null ) {
			if ( mMusicMPIntro.isPlaying() )
				mMusicMPIntro.stop() ;
			if ( !mMusicIntroFinished )
				mMusicPlaying = false ;
			mMusicMPIntro.release() ;
			mMusicMPIntro = null ;
		}
	}
	
	synchronized private void unloadMusicLoop() {
		if ( mMusicMPLoop != null ) {
			if ( mMusicMPLoop.isPlaying() )
				mMusicMPLoop.stop() ;
			if ( mMusicPlaying && mMusicIntroFinished )
				mMusicPlaying = false ;
			mMusicIntroFinished = false ;
			mMusicMPLoop.release() ;
			mMusicMPLoop = null ;
		}
	}
	
	// load!
	synchronized public void loadSound( int soundType, int priority, AssetFileDescriptor afd ) {
		unloadSound( soundType ) ;
		mSoundID[soundType] = mSoundPool.load(afd, 1) ;
		mSoundPriority[soundType] = priority ;
		mSoundStreamID[soundType] = 0 ;
		mSoundResID[soundType] = 0 ;
	}
	
	synchronized public void loadSound( int soundType, int priority, Context context, int resId) {
		unloadSound( soundType ) ;
		// attempt to avoid re-loading the sound: look for a sound with the same ResID.
		boolean set = false ;
		for ( int i = 0; i < NUM_SOUND_TYPES && !set; i++ ) {
			if ( i != soundType && mSoundResID[i] == resId ) {
				mSoundID[soundType] = mSoundID[i] ;
				set = true ;
			}
		}
		if ( !set )
			mSoundID[soundType] = mSoundPool.load(context, resId, 1) ;
		mSoundPriority[soundType] = priority ;
		mSoundStreamID[soundType] = 0 ;
		mSoundResID[soundType] = resId ;
	}
	
	synchronized public void loadSound( int soundType, int priority, String path ) {
		unloadSound( soundType ) ;
		mSoundID[soundType] = mSoundPool.load(path, 1) ;
		mSoundPriority[soundType] = priority ;
		mSoundStreamID[soundType] = 0 ;
		mSoundResID[soundType] = 0 ;
	}
	
	synchronized public void loadSound( int soundType, int priority, FileDescriptor fd, long offset, long length ) {
		unloadSound( soundType ) ;
		mSoundID[soundType] = mSoundPool.load(fd, offset, length, 1) ;
		mSoundPriority[soundType] = priority ;
		mSoundStreamID[soundType] = 0 ;
		mSoundResID[soundType] = 0 ;
	}
	
	synchronized public int getSoundResID( int soundType ) {
		return mSoundResID[soundType] ;
	}
	
	synchronized public void unloadSound( int soundType ) {
		if ( mSoundID[soundType] > 0 ) {
			mSoundPool.stop(mSoundStreamID[soundType]) ;
			mSoundPool.unload(mSoundID[soundType]) ;
			mSoundStreamID[soundType] = 0 ;
			mSoundID[soundType] = 0 ;
			mSoundResID[soundType] = 0 ;
		}
	}
	
	synchronized public void setMusicVolume(float vol) {
		mMusicVolume = vol ;
		if ( mMusicMPIntro != null )
			mMusicMPIntro.setVolume(vol, vol) ;
		if ( mMusicMPLoop != null )
			mMusicMPLoop.setVolume(vol, vol) ;
	}
	
	
	synchronized public void setInGameSoundVolume(float vol) {
		mInGameSoundVolume = vol ;
		for ( int i = 0; i < NUM_IN_GAME_SOUND_TYPES; i++ )
			mSoundPool.setVolume(mSoundStreamID[i], vol, vol) ;
		// We play menu and lobby sounds at full volume, regardless of setting.
	}
	
	synchronized public void setOutOfGameSoundVolume(float vol) {
		mOutOfGameSoundVolume = vol ;
		for ( int i = NUM_IN_GAME_SOUND_TYPES; i <  NUM_SOUND_TYPES; i++ )
			mSoundPool.setVolume(mSoundStreamID[i], vol, vol) ;
		// We play menu and lobby sounds at full volume, regardless of setting.
	}
	
	
	synchronized public boolean isPlayingMusic() {
		return mMusicPlaying ;
	}
	
	synchronized public void playMusic() {
		// try starting a MediaPlayer depending on settings.
		if ( mMusic != null ) {
			mMusicPlaying = true ;
			if ( !isMutedByAnything() ) {
				if ( ( !mMusicIntroFinished || mMusicMPLoop == null ) && mMusicMPIntro != null )
					mMusicMPIntro.start() ;
				else if ( mMusicMPLoop != null ) {
					mMusicIntroFinished = true ;
					mMusicMPLoop.start() ;
				}
			}
		}
	}
	
	
	synchronized public void pauseMusic() {
		// media player allows pause/resume using "start()."
		mMusicPlaying = false ;
		if ( mMusicIntroFinished && mMusicMPLoop != null && mMusicMPLoop.isPlaying() )
			mMusicMPLoop.pause() ;
		else if ( mMusicMPIntro != null && mMusicMPIntro.isPlaying() )
			mMusicMPIntro.pause() ;
	}
	
	synchronized public void stopMusic() {
		// media player allows "stop()" followed by "start()"
		mMusicPlaying = false ;
		if ( mMusicIntroFinished && mMusicMPLoop != null && mMusicMPLoop.isPlaying() ) {
			mMusicMPLoop.stop() ;
			mMusicIntroFinished = false ;
		}
		else if ( mMusicMPIntro != null && mMusicMPIntro .isPlaying() )
			mMusicMPIntro.stop() ;
	}
	
	
	synchronized public void moveLeft( int pieceType ) {
		if ( mSoundID[SOUND_TYPE_MOVE_LEFT_RIGHT] != 0 )
			playSound( SOUND_TYPE_MOVE_LEFT_RIGHT ) ;
		else
			playSound( SOUND_TYPE_MOVE_LEFT ) ;
	}
	
	synchronized public void moveRight( int pieceType ) {
		if ( mSoundID[SOUND_TYPE_MOVE_LEFT_RIGHT] != 0 )
			playSound( SOUND_TYPE_MOVE_LEFT_RIGHT ) ;
		else
			playSound( SOUND_TYPE_MOVE_RIGHT ) ;
	}
	
	synchronized public void moveLeftFailed( int pieceType ) {
		if ( mSoundID[SOUND_TYPE_MOVE_LEFT_RIGHT_FAILED] != 0 )
			playSound( SOUND_TYPE_MOVE_LEFT_RIGHT_FAILED ) ;
		else
			playSound( SOUND_TYPE_MOVE_LEFT_FAILED ) ;
	}
	
	synchronized public void moveRightFailed( int pieceType ) {
		if ( mSoundID[SOUND_TYPE_MOVE_LEFT_RIGHT_FAILED] != 0 )
			playSound( SOUND_TYPE_MOVE_LEFT_RIGHT_FAILED ) ;
		else
			playSound( SOUND_TYPE_MOVE_RIGHT_FAILED ) ;
	}
	
	synchronized public void turnCCW( int pieceType ) {
		if ( mSoundID[SOUND_TYPE_TURN_CCW_CW] != 0 )
			playSound( SOUND_TYPE_TURN_CCW_CW ) ;
		else
			playSound( SOUND_TYPE_TURN_CCW ) ;
	}
	
	synchronized public void turnCW( int pieceType ) {
		if ( mSoundID[SOUND_TYPE_TURN_CCW_CW] != 0 )
			playSound( SOUND_TYPE_TURN_CCW_CW ) ;
		else
			playSound( SOUND_TYPE_TURN_CW ) ;
	}
	
	synchronized public void flip( int pieceType ) {
		playSound( SOUND_TYPE_FLIP ) ;
	}
	
	synchronized public void drop( int pieceType ) {
		playSound( SOUND_TYPE_DROP ) ;
	}
	
	synchronized public void reserve( int oldPiece, int newPiece ) {
		playSound( SOUND_TYPE_RESERVE ) ;
	}
	
	synchronized public void attack( int attackPreviewPiece ) {
		playSound( SOUND_TYPE_ATTACK ) ;
	}
	
	
	synchronized public void land( int pieceType, boolean isPieceComponent, int qc ) {
		playSound( SOUND_TYPE_LAND ) ;
	}
	
	synchronized public void lock( int pieceType, boolean isPieceComponent, int qc ) {
		playSound( SOUND_TYPE_LOCK ) ;
	}
	
	synchronized public void clearEmph( int pieceType, int cascadeNumber, int [] clears, boolean [] monoClears ) {
		playSound( SOUND_TYPE_CLEAR_EMPH ) ;
	}
	
	/**
	 * 
	 * @param pieceType
	 * @param cascadeNumber	The number of 'cascades' to reach this clear.  0 is
	 * 		the first clear for a piece, 1 the cascade immediately after the first clear, etc.
	 * @param clears
	 * @param monoClears
	 */
	synchronized public void clear( int pieceType, int cascadeNumber, int [] clears, boolean [] monoClears ) {
		playSound( SOUND_TYPE_CLEAR ) ;
	}
	
	synchronized public void garbageRows( int numRows ) {
		playSound( SOUND_TYPE_ROW_PUSH_GARBAGE ) ;
	}
	
	synchronized public void pushRows( int numRows ) {
		if ( numRows > 0 )
			playSound( SOUND_TYPE_ROW_PUSH_ADD ) ;
		else if ( numRows < 0 )
			playSound( SOUND_TYPE_ROW_PUSH_SUBTRACT );
		else
			playSound( SOUND_TYPE_ROW_PUSH_ZERO ) ;
	}
	
	synchronized public void penalty( int qCombination ) {
		playSound( SOUND_TYPE_PENALTY ) ;
	}
	
	synchronized public void displacementAccel( int numRows ) {
		playSound( SOUND_TYPE_DISPLACEMENT_ACCEL ) ;
	}
	
	synchronized public void enter( int qc ) {
		playSound( SOUND_TYPE_ENTER_BLOCKS ) ;
	}
	
	synchronized public void specialUpgraded( int piecePreviewPrev, int piecePreviewCurrent ) {
		playSound( SOUND_TYPE_SPECIAL_UPGRADED ) ;
	}
	
	synchronized public void attackedByBlocks( int numBlocks ) {
		playSound( SOUND_TYPE_ATTACKED_BY_BLOCKS ) ;
	}
	
	synchronized public void metamorphosis( int qcFrom, int qcTo ) {
		playSound( SOUND_TYPE_COLORBLIND_METAMORPHOSIS ) ;
	}
	
	synchronized public void pieceRiseFade( int pieceType ) {
		playSound( SOUND_TYPE_RISE_AND_FADE ) ;
	}
	
	synchronized public void columnUnlocked( int pieceType ) {
		playSound( SOUND_TYPE_COLUMN_UNLOCK ) ;
	}
	
	synchronized public void levelUp( int newLevel ) {
		playSound( SOUND_TYPE_LEVEL_UP ) ;
	}
	
	synchronized public void gameLoss() {
		playSound( SOUND_TYPE_GAME_LOSS ) ;
	}
	
	synchronized public void gameWin() {
		playSound( SOUND_TYPE_GAME_WIN ) ;
	}
	
	
	synchronized public void playPauseSound() {
		playSound( SOUND_TYPE_PAUSE ) ;
	}
	
	synchronized public void playResumeSound() {
		playSound( SOUND_TYPE_RESUME ) ;
	}
	
	synchronized public void playGetReadySound() {
		playSound( SOUND_TYPE_GET_READY ) ;
	}
		
	
	synchronized public void menuButtonClick() {
		playSound( SOUND_TYPE_MENU_BUTTON_CLICK ) ;
	}
	
	synchronized public void menuButtonHold() {
		playSound( SOUND_TYPE_MENU_BUTTON_HOLD ) ;
	}
	
	synchronized public void menuButtonBack() {
		playSound( SOUND_TYPE_MENU_BACK_BUTTON_CLICK ) ;
	}
	
	synchronized public void menuButtonFlip() {
		playSound( SOUND_TYPE_MENU_BUTTON_FLIP ) ;
	}
	
	synchronized public void lobbyUserJoin() {
		playSound( SOUND_TYPE_LOBBY_USER_JOIN ) ;
	}
	
	synchronized public void lobbyUserQuit() {
		playSound( SOUND_TYPE_LOBBY_USER_QUIT ) ;
	}
	
	synchronized public void lobbyUserGoActive() {
		playSound( SOUND_TYPE_LOBBY_USER_GO_ACTIVE ) ;
	}
	
	synchronized public void lobbyUserGoInactive() {
		playSound( SOUND_TYPE_LOBBY_USER_GO_INACTIVE ) ;
	}
	
	synchronized public void lobbyOtherJoin() {
		playSound( SOUND_TYPE_LOBBY_OTHER_JOIN ) ;
	}
	
	synchronized public void lobbyOtherQuit() {
		playSound( SOUND_TYPE_LOBBY_OTHER_QUIT ) ;
	}
	
	synchronized public void lobbyOtherGoActive() {
		playSound( SOUND_TYPE_LOBBY_OTHER_GO_ACTIVE ) ;
	}
	
	synchronized public void lobbyOtherGoInactive() {
		playSound( SOUND_TYPE_LOBBY_OTHER_GO_INACTIVE ) ;
	}
	
	synchronized public void lobbyUserChat() {
		playSound( SOUND_TYPE_LOBBY_USER_CHAT ) ;
	}
	
	synchronized public void lobbyOtherChat() {
		playSound( SOUND_TYPE_LOBBY_OTHER_CHAT ) ;
	}

	synchronized public void lobbyLaunchCountdownStart() {
		playSound( SOUND_TYPE_LOBBY_LAUNCH_COUNTDOWN_START ) ;
	}

	synchronized public void lobbyLaunchCountdownCancel() {
		playSound( SOUND_TYPE_LOBBY_LAUNCH_COUNTDOWN_CANCEL ) ;
	}

	synchronized public void lobbyLaunch() {
		playSound( SOUND_TYPE_LOBBY_LAUNCH_GO ) ;
	}
	
	// stopping / starting sounds.  "On" and "Off" will stop playback and
	// prevent any further sounds from being played.  It will resume/pause
	// music, if the music is playing.  All sound effects will be immediately
	// stopped.  There are two exceptions:
	//
	// because we store only the most recent StreamID for 
	// any given sound effect, we can only stop the most recent
	// version of that sound.  So, e.g., if multiple LOCK sounds
	// are playing, only the most recent will be silenced; the rest
	// will play to completion.
	//
	// although turning the pool 'off' will prevent any further sounds
	// from playing, including the Pause sound, it will NOT truncate
	// a pause sound currently playing.  You're welcome!
	
	synchronized public void unmute() {
		mSilent = false ;
		if ( mMusicPlaying && !isMutedByAnything() )
			playMusic() ;
	}
	
	synchronized public void mute() {
		mSilent = true ;
		if ( mMusicMPIntro != null && mMusicMPIntro.isPlaying() )
			mMusicMPIntro.pause() ;
		if ( mMusicMPLoop != null && mMusicMPLoop.isPlaying() )
			mMusicMPLoop.pause() ;
		for ( int i = 0; i < NUM_SOUND_TYPES; i++ )
			if ( i != SOUND_TYPE_PAUSE )
				mSoundPool.stop(mSoundStreamID[i]) ;
	}
	
	synchronized public boolean isMuted() {
		return mSilent ;
	}
	
	synchronized public void unmuteWithRinger() {
		mMuteWithRinger = false ;
		if ( mMusicPlaying && !isMutedByAnything() )
			playMusic() ;
	}

	synchronized public void muteWithRinger() {
		mMuteWithRinger = true ;
		if ( isMutedByRinger() ) {
			if ( mMusicMPIntro != null && mMusicMPIntro.isPlaying() )
				mMusicMPIntro.pause() ;
			if ( mMusicMPLoop != null && mMusicMPLoop.isPlaying() )
				mMusicMPLoop.pause() ;
			for ( int i = 0; i < NUM_SOUND_TYPES; i++ )
				if ( i != SOUND_TYPE_PAUSE )
					mSoundPool.stop(mSoundStreamID[i]) ;
		}
	}
	
	synchronized public void ringerDidChangeMode() {
		if ( isMutedByRinger() )
			muteWithRinger() ;		// call to stop music and such.
	}
	
	synchronized public boolean isMutedByRinger() {
		int ringerMode = mAudioManager.getRingerMode() ;
		return mMuteWithRinger && ringerMode != AudioManager.RINGER_MODE_NORMAL ;
	}
	
	synchronized public boolean isMutedByAnything() {
		return mSilent || isMutedByRinger() ;
	}
	
	synchronized private void playSound( int type ) {
		//Log.d(TAG, "playSound: " + type + " id " + mSoundID[type] + " silent " + mSilent) ;
		if ( !isMutedByAnything() && mSoundID[type] > 0 ) {
			if ( type < NUM_IN_GAME_SOUND_TYPES )
				mSoundStreamID[type] = mSoundPool.play(mSoundID[type], mInGameSoundVolume, mInGameSoundVolume, mSoundPriority[type], 0, 1) ;
			else
				mSoundStreamID[type] = mSoundPool.play(mSoundID[type], mOutOfGameSoundVolume, mOutOfGameSoundVolume, mSoundPriority[type], 0, 1) ;
		}
	}

	@Override
	synchronized public void onCompletion(MediaPlayer mp) {
		// this method is used to transition from "intro" to "loop."
		if ( mp == mMusicMPIntro ) {
			// if we have a loop, transition to playing it - and start it
			// up if we are not silent.  If we have no loop, just shut
			// things down.
			if ( !isMutedByAnything() && mMusicPlaying ) {
				// try transitioning to Loop.
				if ( mMusicMPLoop != null ) {
					if ( mMusicManualTransition ) {
						Log.d(TAG, "Manual music transition") ;
						mMusicMPLoop.start() ;
					} else {
						Log.d(TAG, "Automatic music transition") ;
					}
					mMusicIntroFinished = true ;
				}
				else
					mMusicPlaying = false ;
			}
			mMusicIntroFinished = true ;
		}
	}
}
