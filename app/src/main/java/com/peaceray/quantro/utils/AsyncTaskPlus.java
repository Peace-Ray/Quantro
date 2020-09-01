package com.peaceray.quantro.utils;

import android.os.AsyncTask;

/**
 * AsyncTaskPlus extends Android's AsyncTask with some additional
 * functionality.  For example, we now have executeSynchronously(),
 * which behaves exactly like 'execute' except that the execution
 * happens in-line, before the method returns.
 * 
 * @author Jake
 *
 * @param <Params>
 * @param <Progress>
 * @param <Result>
 */
public abstract class AsyncTaskPlus<Params, Progress, Result>  extends AsyncTask<Params, Progress, Result>  {

	/**
	 * Performs an identical operation to the asynchronous "execute",
	 * but performs in in-line.  After this method returns, all listener
	 * methods will have been called.
	 * 
	 * @return
	 */
	public Result executeSynchronously( Params ... params ) {
		if ( getStatus() != AsyncTask.Status.PENDING )
			throw new IllegalStateException("Current status is not PENDING; instead, " + getStatus()) ;
		
		onPreExecute() ;
		Result result = doInBackground( params ) ;
		onPostExecute( result ) ;
		
		return result ;
	}
	
	
}
