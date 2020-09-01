package com.peaceray.quantro.utils;

public class ThreadSafety {

	
	public static void waitForThreadToTerminate( Thread thread ) {
		while (true) {
            try {
        		thread.join( );
        		break ;
            } catch (InterruptedException e) {
            }
        }
	}
	
	public static void waitForThreadToTerminate( Thread thread, long timeout ) {
		if ( timeout == 0 ) {
			waitForThreadToTerminate(thread) ;
			return ;
		}
		
		long timeRemaining = timeout ;
		long timeJoin = 0 ;
		while (timeRemaining > 0) {
            try {
            	if ( timeRemaining > 0 ) {
            		timeJoin = System.currentTimeMillis() ;
            		//System.out.println("ThreadSafety: joining for " + timeRemaining + " millis") ;
            		thread.join( timeRemaining );
            		//System.out.println("ThreadSafety: done, breaking") ;
            	}
            	break ;
            } catch (InterruptedException e) {
            	timeRemaining -= (System.currentTimeMillis() - timeJoin) ;
            	System.out.println("ThreadSafety Interrupted; decremented to " + timeRemaining) ;
            } catch ( Exception e ) {
            	e.printStackTrace() ;
            	System.out.println("THREAD SAFETY: UNCAUGHT EXCEPTION WHEN WAITING FOR THREAD") ;
            }
        }
	}
}
