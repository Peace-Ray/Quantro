package com.peaceray.quantro.view.controls;

public interface InvisibleButtonInterface {
	
	public interface Delegate {
		public void buttonPressed( InvisibleButtonInterface ibi, int numPreviousTaps ) ;
		public void buttonReleased( InvisibleButtonInterface ibi, int numPreviousTaps ) ;
	}

	// Can hide and show
	public void hide() ;
	public void show() ;
	
	public void setEnabled( boolean enabled ) ;
	public void setTouchable( boolean touchable ) ;
	public void setUseAlt( boolean useAlt ) ;
	
	public boolean isTouchable() ;
	
	// General methods for operation
	public void press( boolean tellDelegate ) ;
	public void press( boolean tellDelegate, boolean forceResetTaps ) ;
	public void release( boolean tellDelegate ) ;
	
	// Can query whether shown
	public boolean shown() ;
	public boolean pressed() ;
	
	// Can get/set name and text
	public String name() ;
	public String name( int taps ) ;
	public String text() ;
	public Delegate delegate() ;
	public void setName( String n ) ;
	public void setText( String t ) ;
	public void setDelegate( Delegate invControls ) ;
	
	
	// Do we respond directly to touches?
	public void setCaptureTouches( boolean on ) ;
	public void setShowWhenPressed( boolean on ) ;
	public void setShowWhenPressedDefault() ;
}
