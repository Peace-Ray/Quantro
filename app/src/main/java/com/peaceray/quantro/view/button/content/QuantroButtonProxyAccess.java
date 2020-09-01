package com.peaceray.quantro.view.button.content;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton.ContentState;


/**
 * QuantroButtonProxyAccess: a way of setting QuantroButtonAccess
 * when the actual button is unavailable or locked.
 * 
 * ProxyAccess has three basic features.  First, it does not make any
 * changes to any QuantroButton.  Second, it has setToButton and
 * getFromButton methods, allowing current content settings to be
 * applied through (or copied from) a DirectAccess object.  And third,
 * it has direct accessors (in addition to direct setters) for content,
 * in case you'd like some other method of use.
 * 
 * @author Jake
 *
 */
public class QuantroButtonProxyAccess implements QuantroButtonAccess {
	
	// Content set directly or copied from a DirectAccess object.
	private ContentState mContentState ;
	private int mContentAlpha ;
	
	private CharSequence mTitle ;
	private CharSequence mDescription ;
	private CharSequence mLongDescription ;
	
	private CharSequence mTimeRemaining ;
	
	private Drawable mFullBackground ;
	private Drawable mBackground ;
	
	private boolean mImageShown ;
	private Drawable mImage ;
	private int mImageColor ;
	
	private boolean mAlertShown ;
	private Drawable mAlert ;
	private int mAlertColor ;
	
	private int mColor ;
	
	private boolean mVoteLocalShown ;
	private Drawable mVoteLocalDrawable ;
	private int mVoteLocalColor ;
	
	private boolean [] mVoteShown ;
	private Drawable [] mVoteDrawable ;
	private int [] mVoteColor ;
	
	private CharSequence [] mLobbyDetail ;
	
	private boolean mPinwheelShown ;
	
	public QuantroButtonProxyAccess( QuantroButtonAccess access ) {
		mVoteShown = new boolean[6] ;
		mVoteDrawable = new Drawable[6] ;
		mVoteColor = new int[6] ;
		mLobbyDetail = new CharSequence[LobbyDetail.values().length] ;
		if ( access instanceof QuantroButtonDirectAccess )
			setFromButton( (QuantroButtonDirectAccess)access ) ;
		else if ( access instanceof QuantroButtonProxyAccess )
			setFromButton( (QuantroButtonProxyAccess)access ) ;
		else
			throw new IllegalArgumentException("What kind of button access is " + access) ;
	}
	
	public QuantroButtonProxyAccess( QuantroButtonDirectAccess access ) {
		mVoteShown = new boolean[6] ;
		mVoteDrawable = new Drawable[6] ;
		mVoteColor = new int[6] ;
		mLobbyDetail = new CharSequence[LobbyDetail.values().length] ;
		setFromButton(access) ;
	}
	
	public QuantroButtonProxyAccess( QuantroButtonProxyAccess access ) {
		mVoteShown = new boolean[6] ;
		mVoteDrawable = new Drawable[6] ;
		mVoteColor = new int[6] ;
		mLobbyDetail = new CharSequence[LobbyDetail.values().length] ;
		setFromButton(access) ;
	}
	
	
	public void setFromButton( QuantroButtonDirectAccess access ) {
		mContentState 		= access.mContentState ;
		mContentAlpha 		= access.mContentAlpha ;
		
		mTitle 				= access.mTitle ;
		mDescription 		= access.mDescription ;
		mLongDescription 	= access.mLongDescription ;
		
		mTimeRemaining 		= access.mTimeRemaining ;
		
		mFullBackground 	= access.mFullBackground ;
		mBackground 		= access.mBackground ;
		
		mImageShown 		= access.mImageShown ;
		mImage 				= access.mImage ;
		mImageColor 		= access.mImageColor ;
		
		mAlertShown 		= access.mAlertShown ;
		mAlert 				= access.mAlert ;
		mAlertColor			= access.mAlertColor ;
		
		mColor 				= access.mColor ;
		
		mVoteLocalShown 	= access.mImageLocalVoteShown ;
		mVoteLocalDrawable 	= access.mImageLocalVote ;
		mVoteLocalColor 	= access.mImageLocalVoteColor ;
		
		for ( int i = 0; i < 6; i++ ) {
			mVoteShown[i] 		= access.mImageVoteShown == null ? false : access.mImageVoteShown[i] ;
			mVoteDrawable[i]	= access.mImageVote == null ? null : access.mImageVote[i] ;
			mVoteColor[i] 		= access.mImageVoteColor == null ? Color.MAGENTA : access.mImageVoteColor[i] ;
		}
		
		LobbyDetail [] ldValues = LobbyDetail.values() ;
		for ( int i = 0; i < ldValues.length; i++ ) {
			LobbyDetail ld = ldValues[i] ;
			mLobbyDetail[ld.ordinal()] = access.getLobbyDetail(ld) ;
		}
		
		mPinwheelShown 		= access.mPinwheelShown ;
	}
	
	public void setFromButton( QuantroButtonProxyAccess access ) {
		mContentState 		= access.mContentState ;
		mContentAlpha 		= access.mContentAlpha ;
		
		mTitle 				= access.mTitle ;
		mDescription 		= access.mDescription ;
		mLongDescription 	= access.mLongDescription ;
		
		mTimeRemaining 		= access.mTimeRemaining ;
		
		mFullBackground		= access.mFullBackground ;
		mBackground 		= access.mBackground ;
		
		mImageShown 		= access.mImageShown ;
		mImage 				= access.mImage ;
		mImageColor 		= access.mImageColor ;
		
		mAlertShown 		= access.mAlertShown ;
		mAlert 				= access.mAlert ;
		mAlertColor			= access.mAlertColor ;
		
		mColor 				= access.mColor ;
		
		mVoteLocalShown 	= access.mVoteLocalShown ;
		mVoteLocalDrawable 	= access.mVoteLocalDrawable ;
		mVoteLocalColor 	= access.mVoteLocalColor ;
		
		for ( int i = 0; i < 6; i++ ) {
			mVoteShown[i] 		= access.mVoteShown[i] ;
			mVoteDrawable[i]	= access.mVoteDrawable[i] ;
			mVoteColor[i] 		= access.mVoteColor[i] ;
		}
		
		LobbyDetail [] ldValues = LobbyDetail.values() ;
		for ( int i = 0; i < ldValues.length; i++ ) {
			LobbyDetail ld = ldValues[i] ;
			mLobbyDetail[ld.ordinal()] = access.getLobbyDetail(ld) ;
		}
		
		mPinwheelShown		= access.mPinwheelShown ;
	}
	
	public void setToButton( QuantroButtonDirectAccess access ) {
		access.setContentState(mContentState) ;
		access.setContentAlpha(mContentAlpha) ;
		
		access.setTitle(mTitle) ;
		access.setDescription(mDescription) ;
		access.setLongDescription(mLongDescription) ;
		
		access.setTimeRemaining(mTimeRemaining) ;
		
		access.setFullBackground(mFullBackground) ;
		access.setBackground(mBackground) ;
		
		access.setImage(mImageShown, mImage, mImageColor) ;
		
		access.setAlert(mAlertShown, mAlert, mAlertColor) ;
		
		access.setColor(mColor) ;

		access.setVoteLocal(mVoteLocalShown, mVoteLocalDrawable, mVoteLocalColor) ;
		
		for ( int i = 0; i < 6; i++ ) {
			access.setVote(i, mVoteShown[i], mVoteDrawable[i], mVoteColor[i]) ;
		}
		
		LobbyDetail [] ldValues = LobbyDetail.values() ;
		for ( int i = 0; i < ldValues.length; i++ ) {
			LobbyDetail ld = ldValues[i] ;
			access.setLobbyDetail(ld, mLobbyDetail[ld.ordinal()]) ;
		}
		
		access.setPinwheel(mPinwheelShown) ;
	}
	

	@Override
	public boolean setContentState(ContentState contentState) {
		mContentState = contentState ;
		return false ;
	}

	@Override
	public boolean setContentAlpha(int alphaVal) {
		mContentAlpha = alphaVal ;
		return false;
	}

	@Override
	public boolean setTitle(CharSequence title) {
		mTitle = title ;
		return false;
	}

	@Override
	public boolean setDescription(CharSequence desc) {
		mDescription = desc ;
		return false;
	}

	@Override
	public boolean setLongDescription(CharSequence desc) {
		mLongDescription = desc ;
		return false;
	}

	@Override
	public boolean setTimeRemaining(CharSequence timeRemaining) {
		mTimeRemaining = timeRemaining ;
		return false;
	}
	
	@Override
	public boolean setFullBackground(Drawable background) {
		mFullBackground = background ;
		return false ;
	}

	@Override
	public boolean setBackground(Drawable background) {
		mBackground = background ;
		return false;
	}

	@Override
	public boolean setImageInvisible() {
		mImageShown = false ;
		return false;
	}

	@Override
	public boolean setImage(boolean show, Drawable image) {
		mImageShown = show ;
		mImage = image ;
		return false;
	}

	@Override
	public boolean setImage(boolean show, Drawable image, int imageColor) {
		mImageShown = show ;
		mImage = image ;
		mImageColor = imageColor ;
		return false;
	}

	@Override
	public boolean setAlertInvisible() {
		mAlertShown = false ;
		return false;
	}

	@Override
	public boolean setAlert(boolean show, Drawable alert) {
		mAlertShown = show ;
		mAlert = alert ;
		return false;
	}

	@Override
	public boolean setAlert(boolean show, Drawable alert, int alertColor) {
		mAlertShown = show ;
		mAlert = alert ;
		mAlertColor = alertColor ;
		return false;
	}

	@Override
	public boolean setColor(int color) {
		mColor = color ;
		return false;
	}

	@Override
	public boolean setVoteLocal(boolean show, Drawable d, int color) {
		mVoteLocalShown = show ;
		mVoteLocalDrawable = d ;
		mVoteLocalColor = color ;
		return false;
	}

	@Override
	public boolean setVote(int number, boolean show, Drawable d, int color) {
		mVoteShown[number] = show ;
		mVoteDrawable[number] = d ;
		mVoteColor[number] = color ;
		return false;
	}
	
	@Override
	public boolean setLobbyDetail( LobbyDetail detail, CharSequence text ) {
		mLobbyDetail[detail.ordinal()] = text ;
		return false ;
	}
	
	@Override
	public boolean setPinwheel( boolean visible ) {
		mPinwheelShown = visible ;
		return false ;
	}
	
	
	public QuantroContentWrappingButton.ContentState getContentState() {
		return mContentState ;
	}

	public int getContentAlpha() {
		return mContentAlpha ;
	}

	public CharSequence getTitle() {
		return mTitle ;
	}

	public CharSequence getDescription() {
		return mDescription ;
	}

	public CharSequence getLongDescription() {
		return mLongDescription ;
	}

	public CharSequence getTimeRemaining() {
		return mTimeRemaining ;
	}

	public Drawable getFullBackground() {
		return mFullBackground ;
	}
	
	public Drawable getBackground() {
		return mBackground ;
	}
	
	public boolean getImageShown() {
		return mImageShown ;
	}

	public Drawable getImage() {
		return mImage ;
	}

	public int getImageColor() {
		return mImageColor ;
	}

	public boolean getAlertShown() {
		return mAlertShown ;
	}

	public Drawable getAlert() {
		return mAlert ;
	}

	public int getAlertColor() {
		return mAlertColor ;
	}

	public int getColor() {
		return mColor ;
	}
	
	public boolean getVoteLocalShown() {
		return mVoteLocalShown ;
	}

	public Drawable getVoteLocal() {
		return mVoteLocalDrawable ;
	}

	public int getVoteLocalColor() {
		return mVoteLocalColor ;
	}

	public boolean getVoteShown( int player ) {
		return mVoteShown[player] ;
	}

	public Drawable getVote( int player ) {
		return mVoteDrawable[player] ;
	}

	public int getVoteColor( int player ) {
		return mVoteColor[player] ;
	}
	
	public CharSequence getLobbyDetail( LobbyDetail detail ) {
		return mLobbyDetail[detail.ordinal()] ;
	}
	
	public boolean getPinwheel() {
		return mPinwheelShown ;
	}

}
