package com.peaceray.quantro.content;

import com.peaceray.quantro.R;
import com.peaceray.quantro.content.Background.Template;
import com.peaceray.quantro.model.modes.GameModes;


/**
 * Analogous to 'Skin' or 'Background', a 'Music' instance represents
 * a single music track.
 * 
 * Not that one Music instance may not correspond to exactly one Music
 * Resource -- some songs, especially game songs, are divided into
 * an 'intro' resource and a 'loop' resource.
 * 
 * A difference between Skin/Background and Music: a Background instance
 * is fully defined by its combination of Template and Shade: one alone
 * is not sufficient to describe a Background.
 * 
 * For Music, 'Track' is sufficient to define the instance.  Other enums,
 * such as 'Setting', describe that instance in various terms.
 * 
 * @author Jake
 *
 */
public class Music {
	
	
	public enum Track {
		/**
		 * The default Quantro music track.  Part of the original 0.8 release.
		 */
		QUANTRO_1,
		
		
		/**
		 * The 2nd, alternative Quantro music track.  Included in 0.9.7.
		 */
		QUANTRO_2,
		
		
		/**
		 * The default Retro music track.  Part of the original 0.8 release.
		 */
		RETRO_1,
		
		/**
		 * The 2nd, alternative Retro music track.  Included in 0.9.7.
		 */
		RETRO_2,
		
		
		/**
		 * The main theme, played in menus.
		 */
		MAIN_THEME,
		
		
		/**
		 * The "lobby" music
		 */
		WAITING,
	}
	
	public enum Setting {
		
		/**
		 * In a Quantro game mode.
		 */
		QUANTRO,
		
		/**
		 * In a Retro game mode.
		 */
		RETRO,
		
		
		/**
		 * In a menu
		 */
		MENU,
		
		
		/**
		 * In a lobby.
		 */
		LOBBY,
	}
	

	
	
	private Track mTrack ;
	private Setting mSetting ;
	
	private boolean mHasIntro ;
	private boolean mHasLoop ;
	
	private int mIntroResourceID ;
	private int mLoopResourceID ;
	
	private String mName ;
	private int mTypicalGameMode ;
	
	private Music( Track track ) {
		mTrack = track ;
		mSetting = Music.getSetting( track ) ;
		
		mHasIntro = Music.getHasIntro( track ) ;
		mHasLoop = Music.getHasLoop( track ) ;
		
		if ( mHasIntro )
			mIntroResourceID = Music.getIntroResourceID( track ) ;
		if ( mHasLoop )
			mLoopResourceID = Music.getLoopResourceID( track ) ;
		
		mName = Music.getName( track ) ;
		mTypicalGameMode = Music.getTypicalGameMode( track ) ;
	}
	
	@Override
	public boolean equals( Object o ) {
		if ( o == this )
			return true ;
		if ( o instanceof Music ) {
			Music m = (Music)o ;
			if ( mTrack != m.mTrack )
				return false ;
			// DETERMINISTIC based on track.
			
			return true ;
		}
		
		return false ;
	}
	
	@Override
	public int hashCode() {
		return mTrack.ordinal() ;
	}
	
	
	public Track getTrack() {
		return mTrack ;
	}
	
	public Setting getSetting() {
		return mSetting ;
	}
	
	public boolean getHasIntro() {
		return mHasIntro ;
	}
	
	public boolean getHasLoop() {
		return mHasLoop ;
	}
	
	public int getIntroResourceID() {
		if ( mHasIntro )
			return mIntroResourceID ;
		throw new IllegalStateException("Track " + mTrack + " has no Intro.") ;
	}
	
	public int getLoopResourceID() {
		if ( mHasLoop )
			return mLoopResourceID ;
		throw new IllegalStateException("Track " + mTrack + " has no Loop.") ;
	}
	
	@Override
	public String toString() {
		return "Music (" + getName() + ")" ;
	}
	
	public String getName() {
		return mName ;
	}
	
	public int getTypicalGameMode() {
		return mTypicalGameMode ;
	}
	
	
	public static final boolean equals( Music m1, Music m2 ) {
		if ( (m1 == null) != (m2 == null) )
			return false ;
		if ( m1 != null )
			return m1.equals(m2) ;
		
		// otherwise, both null.
		return true ;
	}
	
	private static final Music [] CACHED_MUSIC = new Music[Track.values().length] ;
	private static final Music [][] CACHED_MUSIC_BY_SETTING = new Music[Setting.values().length][] ;
	
	
	public static final Music get( Track track ) {
		int t = track.ordinal() ;
		
		if ( CACHED_MUSIC[t] == null ) {
			CACHED_MUSIC[t] = new Music(track) ;
		}
		
		return CACHED_MUSIC[t] ;
	}
	
	
	public static final Music [] getMusics() {
		Track [] tracks = Track.values() ;
		Music [] musics = new Music[tracks.length] ;
		for ( int i = 0; i < tracks.length; i++ )
			musics[i] = Music.get( tracks[i] ) ;
		return musics ;
	}
	
	public static final Music [] getMusics( Setting setting ) {
		int s = setting.ordinal() ;
		
		if ( CACHED_MUSIC_BY_SETTING[s] == null ) {
			Track [] tracks = Track.values() ;
			int count = 0 ;
			for ( int i = 0; i < tracks.length; i++ ) {
				if ( Music.getSetting(tracks[i]) == setting )
					count++ ;
			}
			
			CACHED_MUSIC_BY_SETTING[s] = new Music[count] ;
			count = 0 ;
			for ( int i = 0; i < tracks.length; i++ ) {
				if ( Music.getSetting(tracks[i]) == setting )
					CACHED_MUSIC_BY_SETTING[s][count++] = Music.get(tracks[i]) ;
			}
		}
		
		return CACHED_MUSIC_BY_SETTING[s].clone() ;
	}
	
	
	public static final Setting getSetting( Track track ) {
		switch( track ) {
		case QUANTRO_1:
		case QUANTRO_2:
			return Setting.QUANTRO ;
			
		case RETRO_1:
		case RETRO_2:
			return Setting.RETRO ;
			
		case MAIN_THEME:
			return Setting.MENU ;
			
		case WAITING:
			return Setting.LOBBY ;
		}
		
		return null ;
	}
	
	public static final boolean getHasIntro( Track track ) {
		switch( track ) {
		case QUANTRO_1:
		case QUANTRO_2:
		case RETRO_1:	
		case RETRO_2:
			return true ;
		}
		
		return false ;
	}
	
	public static final boolean getHasLoop( Track track ) {
		switch( track ) {
		case QUANTRO_1:
		case QUANTRO_2:
		case RETRO_1:	
		case RETRO_2:
		case MAIN_THEME:
		case WAITING:
			return true ;
		}
		
		return false ;
	}
	
	
	public static final int getIntroResourceID( Track track ) {
		switch( track ) {
		case QUANTRO_1:
			return R.raw.quantro1final_intro ;
		case QUANTRO_2:
			return R.raw.quantro2final_intro ;
		case RETRO_1:
			return R.raw.retro1final_intro ;
		case RETRO_2:
			return R.raw.retro2final_intro ;
		}
		
		throw new IllegalArgumentException("Track " + track + " has no intro.") ;
	}
	
	
	public static final int getLoopResourceID( Track track ) {
		switch( track ) {
		case QUANTRO_1:
			return R.raw.quantro1final_loop ;
		case QUANTRO_2:
			return R.raw.quantro2final_loop ;
		case RETRO_1:
			return R.raw.retro1final_loop ;
		case RETRO_2:
			return R.raw.retro2final_loop ;
		case MAIN_THEME:
			return R.raw.menufinal_loop ;
		case WAITING:
			return R.raw.lobbyfinal_loop ;
		}
		
		throw new IllegalArgumentException("Track " + track + " has no loop.") ;
	}
	
	public static final String getName( Track track ) {
		switch( track ) {
		case QUANTRO_1:
			return "Quantro 1" ;
		case QUANTRO_2:
			return "Quantro 2" ;
		case RETRO_1:
			return "Retro 1" ;
		case RETRO_2:
			return "Retro 2" ;
		case MAIN_THEME:
			return "Main Theme" ;
		case WAITING:
			return "Waiting" ;
		}
		
		throw new IllegalArgumentException("Track " + track + " has no name.") ;
	}

	
	/**
	 * Music tracks might be associated with a 'typical game mode'.
	 * This does not mean this game mode must play this music, or
	 * that this music will only play during this game mode; rather,
	 * it means the returned game mode is "typical" of this track.
	 * 
	 * One use: for UI purposes, getting a standard color to represent
	 * the Music track.
	 * 
	 * @param track
	 * @return
	 */
	public static final int getTypicalGameMode( Track track ) {
		switch( track ) {
		case QUANTRO_1:
			return GameModes.GAME_MODE_SP_QUANTRO_A ;
		case QUANTRO_2:
			return GameModes.GAME_MODE_SP_QUANTRO_B ;
		case RETRO_1:
			return GameModes.GAME_MODE_SP_RETRO_A ;
		case RETRO_2:
			return GameModes.GAME_MODE_SP_RETRO_B ;
		case MAIN_THEME:
			return GameModes.GAME_MODE_SP_QUANTRO_C ;
		case WAITING:
			return GameModes.GAME_MODE_SP_RETRO_C ;
		}
		
		throw new IllegalArgumentException("Track " + track + " has no typical game mode.") ;
	}
	
	
	/**
	 * Game modes might be associated with a "default music track."
	 * This is the music that we recommend playing for the game mode,
	 * although user preferences might select a different track.
	 * 
	 * We guarantee to always return a Music object, unless the specified
	 * game mode does not exist.
	 * 
	 * @param gameMode
	 * @return
	 */
	public static final Music getDefaultTrackForGameMode( int gameMode ) {
		
		if ( !GameModes.has(gameMode) )
			throw new IllegalArgumentException("Game mode "+ gameMode + " does not exist.") ;
		
		Track track = null ;
		
		// explicit game mode links...
		switch( gameMode ) {
		case GameModes.GAME_MODE_SP_QUANTRO_A:
		case GameModes.GAME_MODE_1V1_QUANTRO_A:
		case GameModes.GAME_MODE_1V1_QUANTRO_BITTER_PILL:
			track = Track.QUANTRO_1 ;
			break ;
		case GameModes.GAME_MODE_SP_QUANTRO_B:
		case GameModes.GAME_MODE_SP_QUANTRO_C:
		case GameModes.GAME_MODE_1V1_QUANTRO_C:
			track = Track.QUANTRO_2 ;
			break ;
		
		case GameModes.GAME_MODE_SP_RETRO_A:
		case GameModes.GAME_MODE_1V1_RETRO_A:
		case GameModes.GAME_MODE_1V1_RETRO_GRAVITY:
			track = Track.RETRO_1 ;
			break ;
		case GameModes.GAME_MODE_SP_RETRO_B:
		case GameModes.GAME_MODE_SP_RETRO_C:
		case GameModes.GAME_MODE_1V1_RETRO_C:
			track = Track.RETRO_2 ;
			break ;
		}
		
		if ( track == null ) {
			// by qPanes and class code.
			int qPanes = GameModes.numberQPanes(gameMode) ;
			int classCode = GameModes.classCode(gameMode) ;
			switch( classCode ) {
			case GameModes.CLASS_CODE_ENDURANCE:
			case GameModes.CLASS_CODE_SPECIAL:
				track = qPanes == 2 ? Track.QUANTRO_1 : Track.RETRO_1 ;
				break ;
			case GameModes.CLASS_CODE_PROGRESSION:
			case GameModes.CLASS_CODE_FLOOD:
				track = qPanes == 2 ? Track.QUANTRO_2 : Track.RETRO_2 ;
				break ;
			}
		}
		
		return Music.get(track) ;
		
	}
	
	
	private static final String ENCODED_PREFIX = "M" ;
	private static final String ENCODED_OPEN = "(" ;
	private static final String ENCODED_CLOSE = ")" ;
	
	private static final String [] ENCODED_TRACK = new String[Template.values().length] ;
	
	static {
		ENCODED_TRACK[Track.QUANTRO_1.ordinal()] 		= "q1" ;
		ENCODED_TRACK[Track.QUANTRO_2.ordinal()] 		= "q2" ;
		ENCODED_TRACK[Track.RETRO_1.ordinal()] 			= "r1" ;
		ENCODED_TRACK[Track.RETRO_2.ordinal()] 			= "r2" ;
		ENCODED_TRACK[Track.MAIN_THEME.ordinal()] 		= "m1" ;
		ENCODED_TRACK[Track.WAITING.ordinal()] 			= "w1" ;
	}
	
	
	/**
	 * Returns a "string encoded" version of this Music, designed to be
	 * as lightweight as possible.
	 * 
	 * One possible use: storing, in SharedPreferences, a "StringSet" for the
	 * currently shuffled musics.
	 * 
	 * Encoding: uses the 26 alphabetical characters, in upper- and lower-case,
	 * parentheses "(" and ")", and the comma ",".
	 * 
	 * @param b
	 * @return
	 */
	public static final String toStringEncoding( Music m ) {
		return toStringEncoding( m.getTrack() ) ;
	}
	
	/**
	 * Returns a "string encoded" version of this Music, designed to be
	 * as lightweight as possible.
	 * 
	 * One possible use: storing, in SharedPreferences, a "StringSet" for the
	 * currently shuffled musics.
	 * 
	 * Encoding: uses the 26 alphabetical characters, in upper- and lower-case,
	 * parentheses "(" and ")", and the comma ",".
	 * 
	 * @param t
	 * @param c
	 * @return
	 */
	public static final String toStringEncoding( Track t ) {
		String tStr = ENCODED_TRACK[t.ordinal()] ;
		
		if ( tStr == null )
			throw new IllegalArgumentException("Don't have an encoding for Track " + t) ;
		
		StringBuilder sb = new StringBuilder() ;
		sb.append(ENCODED_PREFIX) ;
		sb.append(ENCODED_OPEN) ;
		sb.append(tStr) ;
		sb.append(ENCODED_CLOSE) ;
		
		return sb.toString() ;
	}
	
	/**
	 * Returns a Music instance, representing the Music object
	 * encoding in the provided string.
	 * 
	 * The provided string must be an encoding string as returned
	 * by toStringEncoding( ).  For convenience, leading and trailing
	 * whitespace is allowed, at least that supported by 'trim()'.
	 * NO OTHER STRING CONTENT IS ALLOWED.
	 * 
	 * @param str
	 * @return
	 */
	public static final Music fromStringEncoding( String str ) {
		str = str.trim() ;
		
		int index_bg = str.indexOf(ENCODED_PREFIX) ;
		int index_op = str.indexOf(ENCODED_OPEN) ;
		int index_cp = str.indexOf(ENCODED_CLOSE) ;
		
		// verify
		if ( index_bg == -1 || index_op == -1 || index_cp == -1 )
			throw new IllegalArgumentException("Encoding " + str + " does not match expected format.") ;
		if ( index_bg > index_op || index_op > index_cp )
			throw new IllegalArgumentException("Encoding " + str + " does not match expected format.") ;
		
		String tStr = str.substring(index_op+1, index_cp) ;
		
		// get the template and shade
		int t = -1 ;
		for ( int i = 0; i < ENCODED_TRACK.length; i++ ) {
			if ( tStr.equals( ENCODED_TRACK[i] ) )
				t = i ;
		}
		
		return Music.get(Track.values()[t]) ;
	}

}
