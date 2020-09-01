package com.peaceray.quantro.model.modes;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

public class CustomGameModeSettings implements Cloneable, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1288557068827282027L;
	
	private static final int VERSION = 1 ;
	// VERSION 0: initial
	// VERSION 1: adds 'mNumberQPanes.' to allow Quantro / Retro custom games.

	private int mID ;
	
	private String mName ;
	private String mSummary ;
	private String mDescription ;
	
	private int mNumberQPanes ;

	private int mRows ;
	private int mCols ;
	
	private boolean mHasTrominoes ;
	private boolean mHasTetrominoes ;
	private boolean mHasPentominoes ;
	
	private boolean mHasRotation ;
	private boolean mHasReflection ;
	
	private boolean mAllowMultiplayer ;
	
	
	CustomGameModeSettings( int ID ) {
		mID = ID ;
		
		mName = null ;
		mSummary = null ;
		mDescription = null ;
		
		mNumberQPanes = 1 ;
		
		mRows = 0 ;
		mCols = 0 ;
		
		mHasTrominoes = false ;
		mHasTetrominoes = false ;
		mHasPentominoes = false ;
		
		mHasRotation = false ;
		mHasReflection = false ;
		
		mAllowMultiplayer = false ;
	}
	
	@Override
	protected Object clone() {
		CustomGameModeSettings c = new CustomGameModeSettings( 0 ) ;
		
		c.mID = mID ;
		
		c.mName = mName ;
		c.mSummary = mSummary ;
		c.mDescription = mDescription ;
		
		c.mNumberQPanes = mNumberQPanes ;
		
		c.mRows = mRows ;
		c.mCols = mCols ;
		
		c.mHasTrominoes = mHasTrominoes ;
		c.mHasTetrominoes = mHasTetrominoes ;
		c.mHasPentominoes = mHasPentominoes ;
		
		c.mHasRotation = mHasRotation ;
		c.mHasReflection = mHasReflection ;
		
		c.mAllowMultiplayer = mAllowMultiplayer ;
		
		return c ;
	}
	
	/**
	 * Returns whether the two CustomGameModeSettings are equivalent
	 * after renaming (i.e., aside from name, summary and description, the
	 * two objects are identical).
	 * @param cgms
	 * @return
	 */
	public boolean isEquivalent( CustomGameModeSettings cgms ) {
		boolean same = true ;
		
		same = same && mID == cgms.mID ;
		
		same = same && mNumberQPanes == cgms.mNumberQPanes ;
		
		same = same && mRows == cgms.mRows ;
		same = same && mCols == cgms.mCols ;
		
		same = same && mHasTrominoes == cgms.mHasTrominoes ;
		same = same && mHasTetrominoes == cgms.mHasTetrominoes ;
		same = same && mHasPentominoes == cgms.mHasPentominoes ;
		
		same = same && mHasRotation == cgms.mHasRotation ;
		same = same && mHasReflection == cgms.mHasReflection ;
		
		same = same && mAllowMultiplayer == cgms.mAllowMultiplayer ;
		
		return same ;
	}
	
	
	@Override
	public boolean equals( Object o ) {
		if ( o instanceof CustomGameModeSettings ) {
			CustomGameModeSettings cgms = (CustomGameModeSettings) o ;
			boolean same = true ;
			same = same && isEquivalent( cgms ) ;
			same = same && mName == null ? cgms.getName() == null : mName.equals(cgms.getName()) ;
			same = same && mSummary == null ? cgms.getSummary() == null : mSummary.equals(cgms.getSummary()) ;
			same = same && mDescription == null ? cgms.getDescription() == null : mDescription.equals(cgms.getDescription()) ;
			
			return same ;
		}
		
		return false ;
	}
	
	public int getID() {
		return mID ;
	}
	
	public String getName() {
		return mName ;
	}
	
	public String getSummary() {
		return mSummary ;
	}
	
	public String getDescription() {
		return mDescription ;
	}
	
	public int getNumberQPanes() {
		return mNumberQPanes ;
	}
	
	public int getRows() {
		return mRows ;
	}
	
	public int getCols() {
		return mCols ;
	}
	
	public boolean getHasTrominoes() {
		return mHasTrominoes ;
	}
	
	public boolean getHasTetrominoes() {
		return mHasTetrominoes ;
	}
	
	public boolean getHasPentominoes() {
		return mHasPentominoes ;
	}
	
	public boolean getHasRotation() {
		return mHasRotation ;
	}
	
	public boolean getHasReflection() {
		return mHasReflection ;
	}
	
	public boolean getAllowMultiplayer() {
		return mAllowMultiplayer ;
	}
	
	
	
	////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE 
	//
	// These methods provide the implementation of the Serializable
	// interface.
	//
	////////////////////////////////////////////////////////////////
	
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		// write version
		stream.writeInt(VERSION) ;
		
		stream.writeInt(mID) ;
		
		stream.writeObject(mName) ;
		stream.writeObject(mSummary) ;
		stream.writeObject(mDescription) ;
		
		stream.writeInt(mNumberQPanes) ;

		stream.writeInt(mRows) ;
		stream.writeInt(mCols) ;
		
		stream.writeBoolean(mHasTrominoes) ;
		stream.writeBoolean(mHasTetrominoes) ;
		stream.writeBoolean(mHasPentominoes) ;
		
		stream.writeBoolean(mHasRotation) ;
		stream.writeBoolean(mHasReflection) ;
		
		stream.writeBoolean(mAllowMultiplayer) ;
		
		// write boolean: has more
		stream.writeBoolean(false) ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int version = stream.readInt() ;
		// should be 1.  If 0, we have no numberQPanes.
		
		mID = stream.readInt() ;
		
		mName = (String)stream.readObject() ;
		mSummary = (String)stream.readObject() ;
		mDescription = (String)stream.readObject() ;
		
		if ( version >= 1 )
			mNumberQPanes = stream.readInt() ;
		else
			mNumberQPanes = 1 ;

		mRows = stream.readInt() ;
		mCols  = stream.readInt() ;
		
		mHasTrominoes = stream.readBoolean() ;
		mHasTetrominoes = stream.readBoolean() ;
		mHasPentominoes = stream.readBoolean() ;
		
		mHasRotation = stream.readBoolean() ;
		mHasReflection = stream.readBoolean() ;
		
		mAllowMultiplayer = stream.readBoolean() ;
		
		// read boolean: finished
		stream.readBoolean();
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
	}
	
	
	////////////////////////////////////////////////////////////////
	//
	// BUILDER 
	//
	// CGMS objects are immutable, so use a Builder to construct or
	// modify them.
	//
	////////////////////////////////////////////////////////////////
	
	
	
	public static class Builder {
		
		CustomGameModeSettings mTemplate ;
		
		public Builder( int ID ) {
			mTemplate = new CustomGameModeSettings( ID ) ;
		}
		
		public Builder( CustomGameModeSettings cgms ) {
			mTemplate = (CustomGameModeSettings)cgms.clone() ;
		}
		
		public CustomGameModeSettings build() {
			throwIfNotBuildable() ;
			return (CustomGameModeSettings)mTemplate.clone() ;
		}
		
		
		public boolean buildable() {
			try {
				throwIfNotBuildable() ;
				return true ;
			} catch( IllegalStateException ise ) {
				return false ;
			}
		}
		
		private void throwIfNotBuildable() {
			if ( mTemplate.mName == null )
				throw new IllegalStateException("Custom settings do not have a name") ;
			if ( mTemplate.mSummary == null )
				throw new IllegalStateException("Custom settings do not have a summary") ;
			if ( mTemplate.mDescription == null )
				throw new IllegalStateException("Custom settings do not have a description") ;
			
			if ( mTemplate.mNumberQPanes < 1 || mTemplate.mNumberQPanes > 2 )
				throw new IllegalStateException("Custom settings has an invalid number of QPanes") ;
			
			if ( !mTemplate.mHasTrominoes
					&& !mTemplate.mHasTetrominoes
					&& !mTemplate.mHasPentominoes )
				throw new IllegalStateException("Custom settings do not include any pieces") ;
			
			if ( mTemplate.mHasTrominoes && mTemplate.mCols < 3 )
				throw new IllegalStateException("Custom settings not wide enough for trionimos") ;
			if ( mTemplate.mHasTetrominoes && mTemplate.mCols < 4 )
				throw new IllegalStateException("Custom settings not wide enough for tetrominos") ;
			if ( mTemplate.mHasPentominoes && mTemplate.mCols < 5 )
				throw new IllegalStateException("Custom settings not wide enough for pentominos") ;
			
			if ( mTemplate.mHasTrominoes && mTemplate.mRows < 3 )
				throw new IllegalStateException("Custom settings not tall enough for trionimos") ;
			if ( mTemplate.mHasTetrominoes && mTemplate.mRows < 4 )
				throw new IllegalStateException("Custom settings not tall enough for tetrominos") ;
			if ( mTemplate.mHasPentominoes && mTemplate.mRows < 5 )
				throw new IllegalStateException("Custom settings not tall enough for pentominos") ;
		}
		
		
		public Builder setName( String name ) {
			mTemplate.mName = name ;
			return this ;
		}
		
		public Builder setSummary( String sum ) {
			mTemplate.mSummary = sum ;
			return this ;
		}
		
		public Builder setDescription( String desc ) {
			mTemplate.mDescription = desc ;
			return this ;
		}
		
		public Builder setNumberQPanes( int number ) {
			mTemplate.mNumberQPanes = number ;
			return this ;
		}
		
		public Builder setHasTrominoes( boolean has ) {
			mTemplate.mHasTrominoes = has ;
			return this ;
		}
		
		public Builder setHasTetrominoes( boolean has ) {
			mTemplate.mHasTetrominoes = has ;
			return this ;
		}
		
		public Builder setHasPentominoes( boolean has ) {
			mTemplate.mHasPentominoes = has ;
			return this ;
		}
		
		public Builder setHasRotation( boolean has ) {
			mTemplate.mHasRotation = has ;
			return this ;
		}
		
		public Builder setHasReflection( boolean has ) {
			mTemplate.mHasReflection = has ;
			return this ;
		}
		
		public Builder setRows( int rows ) {
			mTemplate.mRows = rows ;
			return this ;
		}
		
		public Builder setCols( int cols ) {
			mTemplate.mCols = cols ;
			return this ;
		}
		
		public Builder setAllowMultiplayer( boolean allow ) {
			mTemplate.mAllowMultiplayer = allow ;
			return this ;
		}
		
	}
}
