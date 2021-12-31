package com.peaceray.quantro.view.button.content;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.peaceray.quantro.R;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;

public class QuantroButtonDirectAccess implements QuantroButtonAccess {
	
	@SuppressWarnings("unused")
	private static final String TAG = "QBDirectSetter" ;

	QuantroContentWrappingButton mButton ;
	
	QuantroContentWrappingButton.ContentState mContentState ;
	
	TextView mViewTitle ;
	TextView mViewDescription ;
	TextView mViewLongDescription ;
	
	TextView mViewTimeRemaining ;
	
	View mViewBackground ;
	ImageView mViewImage ;
	ImageView mViewAlert ;
	
	// Special places for votes!
	ImageView mViewImageLocalVote ;
	ImageView [] mViewImageVote ;
	
	// Content of the above views
	CharSequence mTitle = null  ;
	CharSequence mDescription = null ;
	CharSequence mLongDescription = null ;
	
	CharSequence mTimeRemaining = null ;
	
	Drawable mFullBackground = null ;
	
	Drawable mBackground = null ;
	
	Drawable mImage = null ;
	boolean mImageShown = false ;
	int mImageColor ;
	
	Drawable mAlert = null ;
	boolean mAlertShown = false ;
	int mAlertColor ;
	
	Drawable mImageLocalVote = null ;
	Drawable [] mImageVote = null ;
	int mImageLocalVoteColor ;
	int [] mImageVoteColor ;
	boolean mImageLocalVoteShown = false ;
	boolean [] mImageVoteShown = null ;
	
	int mColor = 0 ;
	
	int mContentAlpha = 255 ;
	
	// Lobby detail
	TextView [] mViewLobbyDetail ;
	CharSequence [] mLobbyDetail ;
	
	// Pinwheel
	ProgressBar mPinwheel = null ;
	boolean mPinwheelShown = false ;
	
	public static QuantroButtonAccess wrap( QuantroContentWrappingButton button ) {
		return new QuantroButtonDirectAccess( button ) ;
	}
	
	private QuantroButtonDirectAccess( QuantroContentWrappingButton button ) {
		this.mButton = button ;
		
		mContentState = mButton.getContentState() ;
		
		View contentView = button.getContentView() ;
		if ( contentView == null )
			throw new IllegalArgumentException("Provided button has no content view!") ;
		View v ;
		// Title?
		v = contentView.findViewById( R.id.button_content_title ) ;
		if ( v != null ) {
			mViewTitle = ((TextView)v) ;
			mTitle = mViewTitle.getText().toString() ;
		}
		// Description?
		v = contentView.findViewById( R.id.button_content_description ) ;
		if ( v != null ) {
			mViewDescription = ((TextView)v) ;
			mDescription = mViewDescription.getText().toString() ;
		}
		// Long Description?
		v = contentView.findViewById( R.id.button_content_long_description ) ;
		if ( v != null ) {
			mViewLongDescription = ((TextView)v) ;
			mLongDescription = mViewLongDescription.getText().toString() ;
		}
		// Time remaining?
		v = contentView.findViewById( R.id.button_content_time_remaining_text ) ;
		if ( v != null ) {
			mViewTimeRemaining = ((TextView)v) ;
			mTimeRemaining = mViewTimeRemaining.getText().toString() ;
		}
		// Full background?
		mFullBackground = mButton.getBackground() ;
		// Background drawable?
		v = contentView.findViewById( R.id.button_content_background_drawable ) ;
		if ( v != null ) {
			mViewBackground = v ;
			mBackground = mViewBackground.getBackground() ;
		}
		// Image drawable?
		v = contentView.findViewById( R.id.button_content_image_drawable ) ;
		if ( v != null ) {
			mViewImage = (ImageView)v ;
			mImage = null ;
			mImageColor = Color.WHITE ;
		}
		// Alert drawable?
		v = contentView.findViewById( R.id.button_content_alert_drawable ) ;
		if ( v != null ) {
			mViewAlert = (ImageView)v ;
			mAlert = null ;
			mAlertColor = Color.WHITE ;
		}
		// Votes?
		v = contentView.findViewById( R.id.button_content_local_vote_image_drawable ) ;
		if ( v != null ) {
			mViewImageLocalVote = (ImageView)v ;
			mImageLocalVote = null ;
			mImageLocalVoteColor = Color.WHITE ;
			mImageLocalVoteShown = false ;

			// assume that this is necessary and sufficient for all other vote drawables.
			mViewImageVote = new ImageView[6] ;
			mImageVote = new Drawable[6] ;
			mImageVoteColor = new int[6] ;
			mImageVoteShown = new boolean[6] ;
			for ( int i = 0; i < 6; i++ ) {
				mImageVote[i] = null ;
				mImageVoteColor[i] = Color.WHITE ;
				mImageVoteShown[i] = false ;
			}
			
			mViewImageVote[0] = (ImageView) contentView.findViewById( R.id.button_content_other_vote_0_image_drawable ) ;
			mViewImageVote[1] = (ImageView) contentView.findViewById( R.id.button_content_other_vote_1_image_drawable ) ;
			mViewImageVote[2] = (ImageView) contentView.findViewById( R.id.button_content_other_vote_2_image_drawable ) ;
			mViewImageVote[3] = (ImageView) contentView.findViewById( R.id.button_content_other_vote_3_image_drawable ) ;
			mViewImageVote[4] = (ImageView) contentView.findViewById( R.id.button_content_other_vote_4_image_drawable ) ;
			mViewImageVote[5] = (ImageView) contentView.findViewById( R.id.button_content_other_vote_5_image_drawable ) ;
		}
		
		// Lobby Detail
		LobbyDetail [] ldValues = LobbyDetail.values() ;
		mViewLobbyDetail = new TextView[ldValues.length] ;
		mLobbyDetail = new CharSequence[ldValues.length] ;
		for ( int i = 0; i < ldValues.length; i++ ) {
			int viewID ;
			LobbyDetail ld = ldValues[i] ;
			switch( ld ) {
			case MEMBERSHIP:
				viewID = R.id.button_content_lobby_population ;
				break ;
			case CREATION:
				viewID = R.id.button_content_lobby_creation_time ;
				break ;
			case HOST:
				viewID = R.id.button_content_lobby_host ;
				break ;
			case ADDRESS_OR_TYPE:
				viewID = R.id.button_content_lobby_address_or_type ;
				break ;
			case SESSION_ID:
				viewID = R.id.button_content_lobby_session_id ;
				break ;
			default:
				throw new IllegalStateException("I don't know what to do with lobby detail " + ld) ;
			}
			
			mViewLobbyDetail[i] = (TextView) contentView.findViewById(viewID) ;
			mLobbyDetail[i] = mViewLobbyDetail[i] == null ? null : mViewLobbyDetail[i].getText() ;
		}
		
		// Pinwheel
		mPinwheel = (ProgressBar) contentView.findViewById(R.id.button_content_pinwheel) ;
		mPinwheelShown = mPinwheel != null && mPinwheel.getVisibility() == View.VISIBLE ;
		
		// base color?
		button.setColor(mColor) ;
	}
	
	
	/**
	 * Sets the ContentState for this button.
	 * @param content
	 * @return
	 */
	public boolean setContentState( QuantroContentWrappingButton.ContentState contentState ) {
		boolean changed = mContentState != contentState ;
		if ( changed ) {
			mButton.setContentState(contentState) ;
			mContentState = contentState ;
		}
		return changed ;
	}
	
	
	public boolean setContentAlpha( int alphaVal ) {
		if ( alphaVal != mContentAlpha ) {
			
			float alpha = alphaVal / 255.0f ;
			
			VersionSafe.setAlpha( mViewTitle,			alpha ) ;
			VersionSafe.setAlpha( mViewDescription,		alpha ) ;
			
			VersionSafe.setAlpha( mViewTimeRemaining,	alpha ) ;
			
			VersionSafe.setAlpha( mViewBackground,		alpha ) ;
			VersionSafe.setAlpha( mViewImage,			alpha ) ;
			VersionSafe.setAlpha( mViewAlert,			alpha ) ;
			
			for ( int i = 0; i < mViewLobbyDetail.length; i++ ) {
				VersionSafe.setAlpha(mViewLobbyDetail[i], 	alpha) ;
			}
			
			mContentAlpha = alphaVal ;
			
			return true ;
		}
		return false ;
	}
	
	public boolean setTitle( CharSequence title ) {
		boolean result = setText( title, this.mTitle, mViewTitle ) ;
		this.mTitle = title ;
		return result ;
	}
	
	public boolean setDescription( CharSequence desc ) {
		boolean result = setText( desc, this.mDescription, mViewDescription ) ;
		this.mDescription = desc ;
		return result ;
	}
	
	public boolean setLongDescription( CharSequence desc ) {
		boolean result = setText( desc, this.mLongDescription, mViewLongDescription ) ;
		this.mLongDescription = desc ;
		return result ;
	}
	
	
	
	public boolean setTimeRemaining( CharSequence timeRemaining ) {
		boolean result = setText( timeRemaining, this.mTimeRemaining, mViewTimeRemaining ) ;
		this.mTimeRemaining = timeRemaining ;
		return result ;
	}
	
	public boolean setFullBackground( Drawable background ) {
		boolean changed = false ;
		if ( background != this.mFullBackground ) {
			VersionSafe.setBackground(mButton, background) ;
			changed = true ;
		}
		this.mFullBackground = background ;
		return changed ;
	}
	
	public boolean setBackground( Drawable background ) {
		boolean changed = false ;
		if ( background != this.mBackground && mViewBackground != null ) {
			VersionSafe.setBackground(mViewBackground, background) ;
			changed = true ;
		}
		this.mBackground = background ;
		return changed ;
	}
	
	public boolean setImageInvisible() {
		return setImage( false, mImage, mImageColor ) ;
	}
	
	public boolean setImage( boolean show, Drawable image ) {
		return setImage( show, image, mImageColor ) ;
	}
	
	public boolean setImage( boolean show, Drawable image, int imageColor ) {
		boolean changed = mViewImage != null && ( image != this.mImage || imageColor != mImageColor || show != mImageShown ) ;
		if ( changed ) {
			if ( !show ) {
				// set the image -- for sizing.  Don't bother setting the color.
				if ( image != this.mImage )
					mViewImage.setImageDrawable(image) ;
				mViewImage.setVisibility(View.INVISIBLE) ;
			} else {
				mViewImage.setImageDrawable(image) ;
				mViewImage.setColorFilter(imageColor, PorterDuff.Mode.MULTIPLY) ;
				mViewImage.setVisibility(View.VISIBLE) ;
			}
		}
		
		this.mImageShown = show ;
		this.mImage = image ;
		this.mImageColor = imageColor ;
		return changed ;
	}
	
	public boolean setAlertInvisible() {
		return setAlert( false, mAlert, mAlertColor ) ;
	}
	
	public boolean setAlert( boolean show, Drawable alert ) {
		return setAlert( show, alert, mAlertColor ) ;
	}
	
	public boolean setAlert( boolean show, Drawable alert, int alertColor ) {
		boolean changed = mViewAlert != null && ( alert != this.mAlert || alertColor != mAlertColor || show != mAlertShown ) ;
		if ( changed ) {
			if ( !show ) {
				// set the alert -- for sizing.  Don't bother setting the color.
				if ( alert != this.mAlert )
					mViewAlert.setImageDrawable(alert) ;
				mViewAlert.setVisibility(View.GONE) ;
			} else {
				mViewAlert.setImageDrawable(alert) ;
				mViewAlert.setColorFilter(alertColor, PorterDuff.Mode.MULTIPLY) ;
				mViewAlert.setVisibility(View.VISIBLE) ;
			}
		}
		
		this.mAlertShown = show ;
		this.mAlert = alert ;
		this.mAlertColor = alertColor ;
		return changed ;
	}
	
	public boolean setColor( int color ) {
		boolean changed = false ;
		if ( color != this.mColor ) {
			mButton.setColor(color) ;
			changed = true ;
		}
		this.mColor = color ;
		return changed ;
	}
	
	
	public boolean setLobbyDetail( LobbyDetail detail, CharSequence text ) {
		int ldIndex = detail.ordinal() ;
		boolean result = setText( text, mLobbyDetail[ldIndex], mViewLobbyDetail[ldIndex] ) ;
		mLobbyDetail[ldIndex] = text ;
		return result ;
	}
	
	
	public boolean setVoteLocal( boolean show, Drawable d, int color ) {
		boolean changed = false ;
		if ( ( d != mImageLocalVote || color != mImageLocalVoteColor || show != this.mImageLocalVoteShown ) && mViewImageLocalVote != null ) {
			// visibility check.
			if ( !show ) {
				changed = changed || mViewImageLocalVote.getVisibility() != View.INVISIBLE ;
				mImageLocalVoteShown = false ;
				if ( d != mImageLocalVote || mImageLocalVoteColor != color ) {
					mImageLocalVote = d ;
					mImageLocalVoteColor = color ;
					mViewImageLocalVote.setImageDrawable(d) ;
					mViewImageLocalVote.setColorFilter(color, PorterDuff.Mode.MULTIPLY) ;
				}
				mImageLocalVoteShown = false ;
				mViewImageLocalVote.setVisibility(View.INVISIBLE) ;
			} else {
				if ( d != mImageLocalVote || mImageLocalVoteColor != color ) {
					mViewImageLocalVote.setImageDrawable(d) ;
					mViewImageLocalVote.setColorFilter(color, PorterDuff.Mode.MULTIPLY) ;
					changed = true ;
				}
				mViewImageLocalVote.setVisibility(View.VISIBLE) ;
				mImageLocalVote = d ;
				mImageLocalVoteColor = color ;
				mImageLocalVoteShown = true ;
				changed = true ;
			}
		}
		
		return changed ;
	}
	
	public boolean setVote( int number, boolean show, Drawable d, int color ) {
		boolean changed = false ;
		
		if ( mViewImageVote != null && number < mViewImageVote.length && mViewImageVote[number] != null
				&& ( d != mImageVote[number] || color != mImageVoteColor[number] || show != this.mImageVoteShown[number] ) ) {
			// visibility check.
			if ( !show ) {
				changed = changed || mViewImageVote[number].getVisibility() != View.INVISIBLE ;
				mImageVoteShown[number] = false ;
				if ( d != mImageVote[number] || mImageVoteColor[number] != color ) {
					mImageVote[number] = d ;
					mImageVoteColor[number] = color ;
					mViewImageVote[number].setImageDrawable(d) ;
					mViewImageVote[number].setColorFilter(color, PorterDuff.Mode.MULTIPLY) ;
				}
				mImageVoteShown[number] = false ;
				mViewImageVote[number].setVisibility(View.INVISIBLE) ;
			} else {
				if ( d != mImageVote[number] || mImageVoteColor[number] != color ) {
					mViewImageVote[number].setImageDrawable(d) ;
					mViewImageVote[number].setColorFilter(color, PorterDuff.Mode.MULTIPLY) ;
					changed = true ;
				}
				mViewImageVote[number].setVisibility(View.VISIBLE) ;
				mImageVote[number] = d ;
				mImageVoteColor[number] = color ;
				mImageVoteShown[number] = true ;
				changed = true ;
			}
		}
		
		return changed ;
	}
	
	public boolean setPinwheel( boolean visible ) {
		boolean change = visible != mPinwheelShown && mPinwheel != null ;
		if ( change ) {
			mPinwheel.setVisibility(visible ? View.VISIBLE : View.INVISIBLE) ;
		}
		mPinwheelShown = visible ;
		return change ;
	}
	
	
	private boolean setText( CharSequence text, CharSequence prevText, TextView tv ) {
		if ( tv != null && ( (text == null) != (prevText == null) || (text != null && !text.equals(prevText) ) ) ) {
			tv.setText(text) ;
			return true ;
		}
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
		return mImageLocalVoteShown ;
	}

	public Drawable getVoteLocal() {
		return mImageLocalVote ;
	}

	public int getVoteLocalColor() {
		return mImageLocalVoteColor ;
	}

	public boolean getVoteShown( int player ) {
		return mImageVoteShown[player] ;
	}

	public Drawable getVote( int player ) {
		return mImageVote[player] ;
	}

	public int getVoteColor( int player ) {
		return mImageVoteColor[player] ;
	}
	
	public CharSequence getLobbyDetail( LobbyDetail detail ) {
		return mLobbyDetail[detail.ordinal()] ;
	}
	
	public boolean getPinwheel() {
		return mPinwheelShown ;
	}
	
}
