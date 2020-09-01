package com.peaceray.quantro.communications.web;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import com.peaceray.quantro.communications.HttpURLConnectionInterruptThread;
import com.peaceray.quantro.communications.nonce.Nonce;

/**
 * WebQuery provides a standardized template for communication
 * with web-based CGI scripts, assuming a very specific communication style.
 * 
 * A WebQuery instance represents the result of the query; it has a result
 * (probably OK, NO or FAIL) and optionally a list of returned variables.
 * 
 * Assumption:
 * 
 * Queries are made as varA=?&varB=?&...
 * 
 * Response is formatted as:
 * 
 * RESPONSE_TYPE_VARIABLES:
 * 
 * RESULT_CODE
 * AResponseVar:<>
 * BResponseVar:<>
 * C1ResponseVar:<>
 * C2ResponseVar:<>
 * 
 * 
 * RESPONSE_TYPE_TERSE_VARIABLES:
 * 
 * RESULT_CODE
 * A:<>
 * B:<>
 * C1:<>
 * C2:<>
 * 
 * RESPONSE_TYPE_STRING:
 * 
 * RESULT_CODE
 * <a long, potentially newline-including
 * 	string which is returned without any response variables>
 * 
 * 
 * 
 * Individual Queries are Built (using a Builder) and then
 * are immutable once created.  Queries may be used repeatedly,
 * but the same information will be sent each time and the response
 * parsed in the same way.
 * 
 * Response type must be explicitly set.  VARIABLES and STRING do
 * not require additional settings (although VARIABLES will optionally
 * support an explicit terse encoding, converting values from terse-strings
 * to either the full-length string encoding or an associated Object
 * such as an Integer).
 * 
 * TERSE_VARIABLES is different; it requires an explicit list of possible
 * variable names, and a malformed error will result if a variable name
 * appears that cannot be matched to the list.
 * 
 * @author Jake
 *
 */
public class WebQuery {
	
	
	private static final String OK = "OK" ;
	private static final String NO = "NO" ;
	private static final String FAIL = "FAIL" ;
	
	private static final String RESPONSE_VAR_SEPARATOR = ":" ;
		// the first occurrence on a given line separates the variable name from its value.
	
	
	private static final int RESPONSE_TYPE_UNSET = 0 ;
	private static final int RESPONSE_TYPE_STRING = 1 ;
	private static final int RESPONSE_TYPE_VARIABLES = 2 ;
	private static final int RESPONSE_TYPE_TERSE_VARIABLES = 3 ;
	
	/**
	 * Determines the way in which we parse the response.
	 */
	private int mResponseType ;
	
	/**
	 * If non-null, then a given query will return N+1 responses, where N is the number
	 * of times this separator occurs on a line.
	 */
	private String mResponseSeparator ;
	
	/**
	 * The URL for the CGI query.
	 */
	private String mQueryURL ;
	
	/**
	 * If response type is TERSE_VARIABLES, this specifies the 
	 * list of full variable names against which the terse response
	 * will be matched.
	 */
	private ArrayList<String> mTerseVariableNames ;
	
	/**
	 * Any variable can have a set of coded values specified;
	 * if it does, the result is converted from a string
	 * (possibly a terse string) to the inner Object.
	 */
	private Hashtable< String, Hashtable<String, Object> > mVariableCodes ;
		// If the response contains a variable whose name
		// is a key in this hashtable, the value will be converted
		// to one of the inner values.
	
	private ArrayList<String> mSectionHeaders ;
	
	/**
	 * The variables used in our Post.  Valid object types are String,
	 * Integer, Long, Float, Double and Nonce.
	 */
	private Hashtable< String, Object > mPostVariables ;
	
	
	protected WebQuery() {
		mResponseType = RESPONSE_TYPE_UNSET ;
		mResponseSeparator = null ;
		mQueryURL = null ;
		mTerseVariableNames = null ;
		mVariableCodes = null ;
		mPostVariables = null ;
		mSectionHeaders = null ;
	}
	
	protected WebQuery( WebQuery wq ) {
		mResponseType = wq.mResponseType ;
		mResponseSeparator = wq.mResponseSeparator ;
		mQueryURL = wq.mQueryURL ;
		mTerseVariableNames = wq.mTerseVariableNames == null ? null : (ArrayList<String>)wq.mTerseVariableNames.clone() ;
		mVariableCodes = wq.mVariableCodes == null
				? null
				: (Hashtable< String, Hashtable<String, Object> >)wq.mVariableCodes.clone() ;
		mPostVariables = wq.mPostVariables == null ? null : (Hashtable<String, Object>)wq.mPostVariables.clone() ;
		mSectionHeaders = wq.mSectionHeaders == null ? null : (ArrayList<String>)wq.mSectionHeaders.clone() ;
	}
	
	/**
	 * Audits the settings of this WebQuery.  No effect if those settings
	 * are complete and consistent; throws an exception if not.
	 */
	protected void audit() {
		if ( mResponseType == RESPONSE_TYPE_UNSET )
			throw new IllegalStateException("Response type is unset.") ;
		if ( mQueryURL == null )
			throw new IllegalStateException("No query URL set.") ;
		
		// If terse variables, make sure every variable code (if present) is
		// included in the terse list.
		if ( mVariableCodes != null && mTerseVariableNames != null ) {
			Enumeration<String> keys = mVariableCodes.keys() ;
			for ( ; keys.hasMoreElements() ; ) {
				String key = keys.nextElement() ;
				boolean found = false ;
				for ( int i = 0; i < mTerseVariableNames.size(); i++ ) {
					String name = mTerseVariableNames.get(i) ;
					if ( key.equals(name) )
						found = true ;
				}
				if ( !found ) {
					throw new IllegalStateException("Coded variable " + key + " not found in terse list.") ;
				}
			}
		}
		
		if ( mPostVariables == null || mPostVariables.size() == 0 )
			throw new IllegalStateException("No post variables set.") ;
		
		Enumeration<String> keys = mPostVariables.keys() ;
		for ( ; keys.hasMoreElements() ; ) {
			String key = keys.nextElement() ;
			Object value = mPostVariables.get(key) ;
			boolean ok = false ;
			if ( value == null )
				throw new NullPointerException("Null value given for post variable " + key) ;
			
			if ( value instanceof String )
				ok = true ;
			else if ( value instanceof Boolean )
				ok = true ;
			else if ( value instanceof Integer )
				ok = true ;
			else if ( value instanceof Long )
				ok = true ;
			else if ( value instanceof Float )
				ok = true ;
			else if ( value instanceof Double )
				ok = true ;
			else if ( value instanceof Nonce )
				ok = true ;
			
			if ( !ok ) 
				throw new IllegalStateException("Post variable " + key + " is of an unsupported type.") ;
		}
		
		// No null section headers.
		if ( mSectionHeaders != null ) {
			Iterator<String> iter = mSectionHeaders.iterator() ;
			for ( ; iter.hasNext() ; )
				if ( iter.next() == null )
					throw new IllegalStateException("Null section header provided!") ;
		}
	}
	
	
	
	/**
	 * Performs the query.  Returns a 'Response' object if successful, even if
	 * the CGI script failed to perform our action (for example, if it sends the
	 * response "FAIL").
	 * 
	 * If a problem occurs - if we cannot connect, there is no response before the
	 * timeout, the result cannot be parsed according to our settings, etc. -
	 * then an Exception will be thrown.  The type of Exception should given an
	 * idea of the type of error to occur.
	 * 
	 * @param timeout How long should we wait for a response before giving up?
	 * @return
	 * @throws SocketTimeoutException 
	 */
	public Response [] query( int timeout ) throws SocketTimeoutException {
		
		// Query comes in three parts.  First, we package up our variables as a
		// String, String hash table.
		Hashtable<String, Object> postVars = new Hashtable<String, Object>(mPostVariables.size()) ;
		Enumeration<String> keys = mPostVariables.keys() ;
		for ( ; keys.hasMoreElements() ; ) {
			String key = keys.nextElement() ;
			postVars.put( key, mPostVariables.get(key).toString() ) ;
		}
		
		// Second, we send this and get a response - formatted as an array
		// of text lines.
		String [] responseLines = getResponse( postVars, timeout ) ;
		if ( responseLines == null || responseLines.length == 0 )
			return new Response[0] ;
		
		// Third, we parse this into response objects according to our response
		// type and other settings.
		
		// How many do we need?
		int numResponseSeparators = 0 ;
		if ( mResponseSeparator != null ) {
			for ( int i = 0; i < responseLines.length; i++ ) {
				if ( responseLines[i].equalsIgnoreCase(mResponseSeparator) )
					numResponseSeparators++ ;
			}
		}
		
		Response [] responses = new Response[numResponseSeparators + 1] ;
		
		int firstLine = 0 ;
		for ( int i = 0; i < numResponseSeparators+1; i++ ) {
			if ( firstLine >= responseLines.length )
				throw new IllegalArgumentException("Indexing error finding the limits of multiple responses") ;
			int lastLine = firstLine ;
			while ( lastLine < responseLines.length  && !responseLines[lastLine].equalsIgnoreCase(mResponseSeparator) )
				lastLine++ ;
			if ( lastLine +1 == responseLines.length )
				lastLine++ ;
				// following convention, lastLine is the index just after the last line to include.
				// it is either the end of the whole mess, or the separator line.
			
			String [] localResponseLines = new String[lastLine - firstLine] ;
			for ( int j = firstLine; j < lastLine; j++ )
				localResponseLines[j - firstLine] = responseLines[j] ;
			
			responses[i] = parseResponse( localResponseLines ) ;
			
			firstLine = lastLine + 1 ;
		}
		
		
		return responses ;
	}
	
	
	private Response parseResponse( String [] responseLines ) {
		Response response = new Response() ;
		
		// The first line MUST be a valid response code.
		if ( responseLines.length == 0 )
			throw new NullPointerException("Response is blank") ;
		String code = responseLines[0].trim() ;
		if ( !code.equals( OK ) && !code.equals( NO ) && !code.equals( FAIL ) ) {
			throw new RuntimeException("Response code is invalid: "+ code) ;
		}
		response.mResultCode = code ;
		
		// Set the "full response" and the various string responses.
		StringBuilder sb_full = new StringBuilder() ;
		StringBuilder sb_response = new StringBuilder() ;
		StringBuilder sb_response_noBreaks = new StringBuilder() ;
		for ( int i = 0; i < responseLines.length; i++ ) {
			String l = responseLines[i] ;
			
			sb_full.append(l) ;
			if ( i > 0 ) {
				sb_response.append(l) ;
				sb_response_noBreaks.append(l) ;
			}
			
			// line breaks?
			if ( i < responseLines.length -1 ) {
				// there is another line after this.
				sb_full.append("\n") ;
				if ( i > 0 )
					sb_response.append("\n") ;
			}
		}
		response.mFullResponse = sb_full.toString() ;
		response.mResponse = sb_response.toString() ;
		response.mResponseNoLineBreaks = sb_response_noBreaks.toString() ;
		
		// If we are processing this as a simple string response, we're done.
		if ( this.mResponseType == RESPONSE_TYPE_STRING )
			return response ;
		
		// Finally, process the response lines for variables.
		// Do this in two steps.  First, do a naive String-based implementation;
		// take exactly and only the variable names appearing in the response.
		// Get the un-sectioned.
		response.mResponseVars = getKeyedValues(responseLines, null, 0) ;
		// Make the sections as length-zero array lists..
		response.mSectionResponseVars = new Hashtable< String, ArrayList<Hashtable<String, String>>>() ;
		if ( mSectionHeaders != null ) {
			for ( int i = 0; i < mSectionHeaders.size(); i++ ) {
				response.mSectionResponseVars.put(
						mSectionHeaders.get(i),
						new ArrayList<Hashtable<String, String>>()) ;
			}
			// Fill them.
			for ( int i = 0; i < mSectionHeaders.size(); i++ ) {
				String header = mSectionHeaders.get(i) ;
				boolean keepGoing = true ;
				for ( int j = 0; keepGoing; j++ ) {
					Hashtable<String, String> sectionVars = getKeyedValues(responseLines, header, j) ;
					if ( sectionVars == null )
						keepGoing = false ;
					else
						response.mSectionResponseVars.get(header).add(sectionVars) ;
				}
			}
		}
		
		// Second, IF we have TERSE_VARIABLES, convert them to their full-length
		// form.
		// Do this for EVERY variable set, including sections.
		if ( mResponseType == RESPONSE_TYPE_TERSE_VARIABLES ) {
			Hashtable<String, String> vars ;
			
			vars = response.mResponseVars ;
			expandVariableNames( vars ) ;
			if ( mSectionHeaders != null ) {
				for ( int i = 0; i < mSectionHeaders.size(); i++ ) {
					ArrayList< Hashtable<String, String>> sectionVars
							= response.mSectionResponseVars.get(mSectionHeaders.get(i)) ;
					for ( int j = 0; j < sectionVars.size(); j++ ) {
						vars = sectionVars.get(j) ;
						expandVariableNames( vars ) ;
					}
				}
			}
		}
		
		// Finally, check the coding for any coded variables provided.
		encodeVariables( response.mResponseVars, response.mResponseVarCodes ) ;
		response.mSectionResponseVarCodes
				= new Hashtable<String, ArrayList< Hashtable<String, Object>>>() ;
		if ( mSectionHeaders != null ) {
			for ( int i = 0; i < mSectionHeaders.size(); i++ ) {
				String header = mSectionHeaders.get(i) ;
				response.mSectionResponseVarCodes.put(
						header,
						new ArrayList<Hashtable<String, Object>>()) ;
				ArrayList< Hashtable<String, String>> sectionVars
						= response.mSectionResponseVars.get(mSectionHeaders.get(i)) ;
				ArrayList< Hashtable<String, Object>> sectionVarCodes
						= new ArrayList<Hashtable<String, Object>>() ;
		
				for ( int j = 0; j < sectionVars.size(); j++ ) {
					Hashtable<String, String> vars = sectionVars.get(j) ;
					Hashtable<String, Object> varCodes = new Hashtable<String, Object>() ;
					encodeVariables( vars, varCodes ) ;
					sectionVarCodes.add(varCodes) ;
				}
				
				response.mSectionResponseVarCodes.put(
						header, sectionVarCodes ) ;
			}
		}
		
		
		// That's it, we're done.
		return response ;
	}
	
	
	/**
	 * Sends a quantro_mp_web request using the provided keyed values.  Returns
	 * an un-examined response, except that the content provided has been divided
	 * up line-by-line and is provided in-order in the returned array.
	 * 
	 * Will return 'null' upon a complete failure to communicate.  Otherwise, by
	 * convention, the first entry will be one of {OK, NO, FAIL} indicating the result
	 * as described by the web script.
	 * 
	 * e.g.:
	 * called with keyedValues["action"] = request
	 * will probably return
	 * {"OK", "<nonce as string>"}
	 */
	private String [] getResponse( Hashtable<String, Object> keyedValues, int timeout ) throws SocketTimeoutException {
		// Construct data to post a status message
		Enumeration<String> keys = keyedValues.keys() ;
		String data = null ;
		for ( ; keys.hasMoreElements(); ) {
			String key = keys.nextElement() ;
			Object value = keyedValues.get(key) ;
			String keyValuePair ;
			try {
				keyValuePair = URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value.toString(), "UTF-8") ;
			} catch ( UnsupportedEncodingException uee ) {
				throw new RuntimeException("UnsupportedEncoding?? " + uee.getMessage()) ;
			}
			data = data == null ? keyValuePair : data + "&" + keyValuePair ;
		}
		
		// Post the message.
		String [] responseLines = null ;
		HttpURLConnection conn = null ;
		OutputStreamWriter wr = null ;
		InputStreamReader isr = null ;
		try {
			URL url = new URL(mQueryURL);
			conn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
		    conn.setConnectTimeout(timeout) ;
		    conn.setReadTimeout(timeout) ;
		    conn.setDoOutput(true);
		    // try request properties...
		    try {
		    	conn.setRequestProperty("Connection", "close") ;
		    	System.setProperty("java.net.preferIPv4Stack" , "true");
		    } catch ( Exception e ) {
		    	//System.err.println("Problems setting request property...") ;
		    	//e.printStackTrace() ;
		    }
		    conn.connect() ;
		    // wrap a interrupt thread
		    new HttpURLConnectionInterruptThread( conn, timeout ).start() ;
		    wr = new OutputStreamWriter(conn.getOutputStream());
		    wr.write(data);
		    wr.flush();
		    
		    //System.err.println("WebQuery.getResponse: trace is ") ;
		    //new Exception("").printStackTrace() ;
		    //System.err.println("WebQuery.getResponse: response is ") ;
		    //System.err.println(data) ;
	
		    // Get the response
		    isr = new InputStreamReader(conn.getInputStream()) ;
		    StringBuilder sb = new StringBuilder() ;
		    
		    char[] b = new char[2048] ;
		    for ( int n; (n = isr.read(b)) != -1; sb.append(b, 0, n) ) ;
		    wr.close() ;
		    isr.close() ;
		    responseLines = sb.toString().split("[\\r\\n]+") ;
		} catch ( IOException e ) {
			//e.printStackTrace() ;
			throw new SocketTimeoutException("Could not read response in time from " + mQueryURL + " with data " + data) ;
		} finally {
			try {
				conn.disconnect() ;
			} catch ( Exception e ) { }
			try {
				wr.close() ;
			} catch ( Exception e ) { }
			try {
				isr.close() ;
			} catch ( Exception e ) { }
			
		}
	    
	    return responseLines == null ? new String[0] : responseLines ;
	}
	
	
	/**
	 * Finds the provided sectionHeader within the response lines, and reads from there
	 * until the next section header (or the end of the response lines).  For each line,
	 * splits on RESULT_KEY_VALUE_SEPARATOR, using "key:value" as the assumed format,
	 * placing them in a new Hashtable (which is returned).  Hash table keys are Integers,
	 * containing the RESULT_KEY_CODE_* for the key provided (which, remember, may be
	 * only a prefix of an actual RESULT_KEY_*).
	 * 
	 * The section examined will be the 'sectionHeaderNumber'th section with the header
	 * provided, counting up from 0.  If sectionHeader is null, this method will return
	 * the "un-headered" challenge values, ignoring the success indicator.
	 * 
	 * Typically, you will call this with
	 * getKeyedValues( lines, null, 0 )
	 * getKeyedValues( lines, RESULT_HEADER_LOBBY, 0 )
	 * getKeyedValues( lines, RESULT_HEADER_GAME, 0 )
	 * 
	 * but we leave open the possibility of multiple lobbies or games in a single response.
	 * 
	 * Will return 'null' if the provided section/sectionNumber is not found.  Will return
	 * an empty Hashtable if the section was found but contained no content.  Will throw
	 * an exception if the section contained malformed information.
	 * 
	 * @param responseLines
	 * @param sectionHeader
	 * @param sectionHeaderNumber
	 * @return
	 */
	private Hashtable<String, String> getKeyedValues( String [] responseLines, String sectionHeader, int sectionHeaderNumber ) {
		if ( sectionHeader == null && sectionHeaderNumber != 0 )
			return null ;
		
		int firstLine = -1 ;
		
		// Find the header / count.
		if ( sectionHeader == null )
			firstLine = 1 ;		// skip response status
		else {
			int numHeaderSeen = 0 ;		// how many sectionHeaders have we seen?
			for ( int i = 0; i < responseLines.length; i++ ) {
				if ( responseLines[i].equalsIgnoreCase(sectionHeader) ) {
					if ( numHeaderSeen == sectionHeaderNumber ) {
						firstLine = i+1 ;
						break ;
					}
					else
						numHeaderSeen++ ;
				}
			}
		}
		
		if ( firstLine == -1 )
			return null ;
		
		// Examine!
		Hashtable<String, String> res = new Hashtable<String, String>() ;
		for ( int i = firstLine; i < responseLines.length; i++ ) {
			String line = responseLines[i] ;
			
			// If a section header, terminate.
			boolean isHeader = false ;
			for ( int j = 0; mSectionHeaders != null && j < mSectionHeaders.size(); j++ )
				if ( line.equalsIgnoreCase( mSectionHeaders.get(j) ) )
					isHeader = true ;
			
			if ( isHeader )
				break ;
			
			// Otherwise, split and parse.
			String [] items = line.split(RESPONSE_VAR_SEPARATOR, 2) ;
			res.put( items[0], items[1]) ;
		}
		
		return res ;
	}
	
	
	/**
	 * Provided with a set of variables (in name / value format), compares
	 * the variable names against those set ahead of time.  Removes each entry
	 * in vars, replacing it with the canonical variable name (holding the same
	 * value as before).
	 * 
	 * If an entry cannot be canonically matched, throws an IllegalArgumentException.
	 * In this event, the provided 'vars' Hashtable is unchanged.
	 * 
	 * @param vars
	 */
	private void expandVariableNames( Hashtable<String, String> vars ) {
		// we perform the changes on a new object, finally replacing
		// all the content in vars after the operation is complete.
		Hashtable<String, String> canonVars = new Hashtable<String, String>() ;
		Enumeration<String> keys = vars.keys() ;
		for ( ; keys.hasMoreElements() ; ) {
			// attempt to match this key to a canonical one.
			String key = keys.nextElement() ;
			String canonKey = expandVariableName( key ) ;
			if ( canonKey == null )
				throw new IllegalArgumentException("Response variable name " + key + " cannot be uniquely expanded") ;
			if ( canonVars.containsKey(canonKey ) )
				throw new IllegalArgumentException("Terse variable name " + canonKey + " appears more than once in response") ;
			canonVars.put(canonKey, vars.get(key)) ;
		}
		
		// copy over.
		vars.clear() ;
		keys = canonVars.keys() ;
		for ( ; keys.hasMoreElements() ; ) {
			String key = keys.nextElement() ;
			vars.put(key, canonVars.get(key)) ;
		}
	}
	
	/**
	 * Attempts to expand the provided key on the assumption that it is a
	 * truncation of one of mTerseVariableNames.
	 * 
	 * If no match can be found, returns 'null.'
	 * 
	 * @param key
	 * @return
	 */
	private String expandVariableName( String terseName ) {
		if ( terseName == null )
			throw new NullPointerException("Cannot expand a null variable name!") ;
		
		int match = -1 ;
		for ( int i = 0; i < mTerseVariableNames.size(); i++ ) {
			String name = mTerseVariableNames.get(i) ;
			if ( name.indexOf(terseName) == 0 ) {
				if ( match != -1 )
					return null ;		// is ambiguous.
				match = i ;
			}
		}
		
		if ( match > -1 )
			return mTerseVariableNames.get(match) ;
		return null ;
	}
	
	
	/**
	 * Examines the variables in the provided hashtable 'vars'; if a variable
	 * is specified in mVariableCodes, we attempt to find the correct encoding
	 * and place that object in varCodes.  If a variable is specified, but we
	 * don't have the right encoding for it, we throw an exception.  There is
	 * no "anything else" encoding.
	 * 
	 * @param vars
	 * @param varCodes
	 */
	private void encodeVariables( Hashtable<String, String> vars, Hashtable<String, Object> varCodes ) {
		if ( mVariableCodes == null )
			return ;
		
		Enumeration<String> keys = vars.keys() ;
		for ( ; keys.hasMoreElements() ; ) {
			String key = keys.nextElement() ;
			if ( mVariableCodes.containsKey(key) ) {
				String value = vars.get(key) ;
				String matchCode = null ;
				// Attempt to find a match here!
				Enumeration<String> codedStrings = mVariableCodes.get(key).keys() ;
				// we compare using ignoresCase.
				for ( ; codedStrings.hasMoreElements() ; ) {
					String coded = codedStrings.nextElement() ;
					if ( coded.indexOf(value) == 0 ) {
						 // a match!
						if ( matchCode != null )
							throw new IllegalArgumentException("Variable value " + value + " is ambiguously coded") ;
						matchCode = coded ;
					}
				}
				if ( matchCode != null )
					varCodes.put(key, mVariableCodes.get(key).get(matchCode)) ;
				else
					throw new IllegalArgumentException("Variable name " + key + " matches coded vars, but value " + vars.get(key) + " does not") ;
			}
		}
	}
	
	
	
	public static class Builder {
		
		private WebQuery mTemplate ;
		
		public Builder() {
			mTemplate = new WebQuery() ;
		}
		
		public WebQuery build() {
			mTemplate.audit() ;
			return new WebQuery( mTemplate ) ;
		}
		
		/**
		 * Sets the cgi URL used for this query.
		 * @param url
		 * @return
		 */
		public Builder setURL( String url ) {
			mTemplate.mQueryURL = url ;
			return this ;
		}
		
		/**
		 * Sets the string used to separate distinct responses, each of which
		 * will be returned as a distinct object.
		 * 
		 * @param sep
		 * @return
		 */
		public Builder setResponseSeparator( String sep ) {
			mTemplate.mResponseSeparator = sep ;
			return this ;
		}
		
		/**
		 * Sets the post variables for this web query.
		 * @param vars
		 * @return
		 */
		public Builder setPostVariables( Hashtable<String, Object> vars ) {
			if ( vars == null || vars.size() == 0 )
				mTemplate.mPostVariables = null ;
			else {
				mTemplate.mPostVariables = (Hashtable<String, Object>)vars.clone() ;
			}
			
			return this ;
		}
		
		
		/**
		 * Adds the specified post variables to those we will post.
		 * @param vars
		 * @return This instance.
		 */
		public Builder addPostVariables( Hashtable<String, Object> vars ) {
			if ( vars == null || vars.size() == 0 )
				return this ;
			
			if ( mTemplate.mPostVariables == null )
				mTemplate.mPostVariables = (Hashtable<String, Object>)vars.clone() ;
			else {
				Enumeration<String> keys = vars.keys() ;
				for ( ; keys.hasMoreElements() ; ) {
					String key = keys.nextElement() ;
					Object val = vars.get(key) ;
					
					mTemplate.mPostVariables.put(key, val) ;
				}
			}
			
			return this ;
		}
		
		public Builder addPostVariable( String var, Object value ) {
			if ( mTemplate.mPostVariables == null )
				mTemplate.mPostVariables = new Hashtable<String, Object>() ;
			
			mTemplate.mPostVariables.put( var, value ) ;
			
			return this ;
		}
		
		
		/**
		 * Sets the expected response type to an unformatted string.
		 * @return This instance, for chaining.
		 */
		public Builder setResponseTypeString() {
			if ( mTemplate.mResponseType != RESPONSE_TYPE_UNSET )
				throw new IllegalStateException("Response type is already set.") ;
			if ( mTemplate.mVariableCodes != null )
				throw new IllegalStateException("Variable codes have been set; you cannot process the response as a string.") ;
			if ( mTemplate.mSectionHeaders != null )
				throw new IllegalStateException("Section headers have been set; you cannot process the response as a string.") ;
			mTemplate.mResponseType = RESPONSE_TYPE_STRING ;
			mTemplate.mTerseVariableNames = null ;
			return this ;
		}
		
		/**
		 * Sets the expected response type to a list of variables with
		 * full-length names.
		 * @return This instance, for chaining.
		 */
		public Builder setResponseTypeVariableList() {
			if ( mTemplate.mResponseType != RESPONSE_TYPE_UNSET )
				throw new IllegalStateException("Response type is already set.") ;
			mTemplate.mResponseType = RESPONSE_TYPE_VARIABLES ;
			mTemplate.mTerseVariableNames = null ;
			return this ;
		}
		
		/**
		 * Sets the expected response type to a list of variables with
		 * potentially "terse" names.  A terse name is one representing
		 * a prefix of a variable name, long enough to uniquely identify
		 * the name among those provided.
		 * 
		 * @param variableNames
		 * @return
		 */
		public Builder setResponseTypeTerseVariableList( ArrayList<String> variableNames ) {
			if ( mTemplate.mResponseType != RESPONSE_TYPE_UNSET )
				throw new IllegalStateException("Response type is already set.") ;
			mTemplate.mResponseType = RESPONSE_TYPE_TERSE_VARIABLES ;
			mTemplate.mTerseVariableNames = new ArrayList<String>(variableNames.size()) ;
			for ( int i = 0; i < variableNames.size(); i++ )
				mTemplate.mTerseVariableNames.add(variableNames.get(i)) ;
			return this ;
		}
		
		
		/**
		 * Sets the sections headers for this query.  If the response could
		 * possibly be divided into sections, make sure this is set; otherwise
		 * parsing will fail.
		 * 
		 * @param headers
		 * @return
		 */
		public Builder setSectionHeaders( ArrayList<String> headers ) {
			if ( mTemplate.mResponseType == RESPONSE_TYPE_STRING )
				throw new IllegalStateException("Response type is STRING; cannot set section headers, since they may occur in the string.") ;
			if ( headers == null || headers.size() == 0 )
				mTemplate.mSectionHeaders = null ;
			else {
				mTemplate.mSectionHeaders = (ArrayList<String>)headers.clone() ;
			}
			
			return this ;
		}
		
		
		public Builder addSectionHeaders( ArrayList<String> headers ) {
			if ( mTemplate.mResponseType == RESPONSE_TYPE_STRING )
				throw new IllegalStateException("Response type is STRING; cannot add section headers") ;
			
			if ( headers == null || headers.size() == 0 )
				return this ;
			
			if ( mTemplate.mSectionHeaders == null )
				mTemplate.mSectionHeaders = (ArrayList<String>)headers.clone() ;
			else {
				for ( int i = 0; i < headers.size(); i++ ) {
					String h = headers.get(i) ;
					// make sure is not in the headers already...
					boolean found = false ;
					for ( int j = 0; j < mTemplate.mSectionHeaders.size(); j++ )
						if ( mTemplate.mSectionHeaders.get(j).equals(h) )
							found = true ;
					if ( !found )
						mTemplate.mSectionHeaders.add(h) ;
				}
			}
			
			return this ;
		}
		
		
		public Builder addSectionHeader( String header ) {
			if ( mTemplate.mResponseType == RESPONSE_TYPE_STRING )
				throw new IllegalStateException("Response type is STRING; cannot add section headers") ;
			
			if ( mTemplate.mSectionHeaders == null )
				mTemplate.mSectionHeaders = new ArrayList<String>() ;
			boolean found = false ;
			for ( int j = 0; j < mTemplate.mSectionHeaders.size(); j++ )
				if ( mTemplate.mSectionHeaders.get(j).equals(header) )
					found = true ;
			if ( !found )
				mTemplate.mSectionHeaders.add(header) ;
			
			return this ;
		}
		
		
		public Builder addVariableCode( String varName, Hashtable<String, Object> varCode ) {
			if ( mTemplate.mResponseType == RESPONSE_TYPE_STRING )
				throw new IllegalStateException("Response type was set as String; no variable codes allowed") ;
			
			if ( mTemplate.mVariableCodes == null )
				mTemplate.mVariableCodes = new Hashtable<String, Hashtable<String, Object>>() ;
			
			Hashtable<String, Object> code = new Hashtable<String, Object>() ;
			
			Enumeration<String> enumeration = varCode.keys() ;
			for ( ; enumeration.hasMoreElements() ; ) {
				String key = enumeration.nextElement() ;
				code.put(key, varCode.get(key)) ;
			}
			
			mTemplate.mVariableCodes.put(varName, code) ;
			
			return this ;
		}
		
		
		
	}
	
	
	/**
	 * A Response represents the reply sent by the CGI script to our query.  The
	 * only way to create an instance of Response is to call webQuery.query( timeout ).
	 * 
	 * A Response object represents a well-formed response that conforms to our
	 * expectations, including terse variable names and codes, if specified.
	 * 
	 * @author Jake
	 *
	 */
	public class Response {
		
		private String mFullResponse ;
		private String mResultCode ;	// OK, NO, FAIL, or something else.
		
		private String mResponse ;	// does not include the result code.
		private String mResponseNoLineBreaks ; 		// does not include the result code.
		
		private Hashtable<String, String> mResponseVars ;
		private Hashtable<String, Object> mResponseVarCodes ;	// sparse; only those which are coded.
		
		private Hashtable<String, ArrayList<Hashtable<String, String>>> mSectionResponseVars ;
																// first index is by section header, second by occurence number.
		private Hashtable<String, ArrayList<Hashtable<String, Object>>> mSectionResponseVarCodes ;
																// first index is by section header, second by occurence number.

		
		protected Response() {
			mFullResponse = null ;
			mResultCode = null ;
			mResponse = null ;
			mResponseNoLineBreaks = null ;
			mResponseVars = new Hashtable<String, String>() ;
			mResponseVarCodes = new Hashtable<String, Object>() ;
			
			mSectionResponseVars = null ;
			mSectionResponseVarCodes = null ;
		}
		
		
		public boolean isOK() {
			return mResultCode.equals(OK) ;
		}
		
		public boolean isNO() {
			return mResultCode.equals(NO) ;
		}
		
		public boolean isFAIL() {
			return mResultCode.equals(FAIL) ;
		}
		
		public String getFullResponseString() {
			return mFullResponse ;
		}
		
		public String getResultCode() {
			return mResultCode ;
		}
		
		public String getResponseString() {
			return mResponse ;
		}
		
		public String getResponseStringNoLineBreaks() {
			return mResponseNoLineBreaks ;
		}
		
		public Enumeration<String> getVariables() {
			return mResponseVars.keys() ;
		}
		
		public Enumeration<String> getVariables( String header, int headerNumber ) {
			if ( header == null && headerNumber != 0 )
				throw new IllegalArgumentException("The default (null) header must be numbered '0'") ;
			
			if ( header == null )
				return getVariables() ;
			
			return mSectionResponseVars.get(header).get(headerNumber).keys() ;
		}
		
		public String getResponseVariableString( String variable ) {
			if ( mResponseVarCodes.containsKey(variable) )
				return mResponseVarCodes.get(variable).toString() ;
			return mResponseVars.get(variable) ;
		}
		
		public Boolean getResponseVariableBoolean( String variable ) {
			if ( mResponseVarCodes.containsKey(variable) )
				return (Boolean)mResponseVarCodes.get(variable) ;
			if ( !mResponseVars.containsKey(variable) )
				return null ;
			return new Boolean( mResponseVars.get(variable) ) ;
		}
		
		public Integer getResponseVariableInteger( String variable ) {
			if ( mResponseVarCodes.containsKey(variable) )
				return (Integer)mResponseVarCodes.get(variable) ;
			if ( !mResponseVars.containsKey(variable) )
				return null ;
			return new Integer( mResponseVars.get(variable) ) ;
		}
		
		public Long getResponseVariableLong( String variable ) {
			if ( mResponseVarCodes.containsKey(variable) )
				return (Long)mResponseVarCodes.get(variable) ;
			if ( !mResponseVars.containsKey(variable) )
				return null ;
			return new Long( mResponseVars.get(variable) ) ;
		}
		
		public Float getResponseVariableFloat( String variable ) {
			if ( mResponseVarCodes.containsKey(variable) )
				return (Float)mResponseVarCodes.get(variable) ;
			if ( !mResponseVars.containsKey(variable) )
				return null ;
			return new Float( mResponseVars.get(variable) ) ;
		}
		
		public Double getResponseVariableDouble( String variable ) {
			if ( mResponseVarCodes.containsKey(variable) )
				return (Double)mResponseVarCodes.get(variable) ;
			if ( !mResponseVars.containsKey(variable) )
				return null ;
			return new Double( mResponseVars.get(variable) ) ;
		}
		
		
		public Nonce getResponseVariableNonce( String variable ) throws IOException {
			if ( mResponseVarCodes.containsKey(variable) )
				return (Nonce)mResponseVarCodes.get(variable) ;
			if ( !mResponseVars.containsKey(variable) )
				return null ;
			return new Nonce( mResponseVars.get(variable) ) ;
		}
		
		public int getNumberOfSections( String header ) {
			if ( header == null ) 
				return 1 ;	// always a default section.
			if ( mSectionResponseVars == null || !mSectionResponseVars.containsKey(header) )
				return 0 ;
			return mSectionResponseVars.get(header).size() ;
		}
		
		public String getResponseVariableString( String variable, String header, int headerNumber ) {
			if ( header == null && headerNumber != 0 )
				throw new IllegalArgumentException("The default (null) header must be numbered '0'") ;
			
			if ( header == null )
				return getResponseVariableString( variable ) ;
			
			Hashtable<String, Object> varCodes = mSectionResponseVarCodes.get(header).get(headerNumber) ;
			Hashtable<String, String> vars = mSectionResponseVars.get(header).get(headerNumber) ;
			if ( varCodes.containsKey(variable) )
				return varCodes.get(variable).toString() ;
			return vars.get(variable) ;
		}
		
		public Boolean getResponseVariableBoolean( String variable, String header, int headerNumber ) {
			if ( header == null && headerNumber != 0 )
				throw new IllegalArgumentException("The default (null) header must be numbered '0'") ;
			
			if ( header == null )
				return getResponseVariableBoolean( variable ) ;
			
			Hashtable<String, Object> varCodes = mSectionResponseVarCodes.get(header).get(headerNumber) ;
			Hashtable<String, String> vars = mSectionResponseVars.get(header).get(headerNumber) ;
			if ( varCodes.containsKey(variable) )
				return (Boolean)varCodes.get(variable) ;
			if ( !mResponseVars.containsKey(variable) )
				return null ;
			return new Boolean( vars.get(variable) ) ;
		}
		
		public Integer getResponseVariableInteger( String variable, String header, int headerNumber ) {
			if ( header == null && headerNumber != 0 )
				throw new IllegalArgumentException("The default (null) header must be numbered '0'") ;
			
			if ( header == null )
				return getResponseVariableInteger( variable ) ;
			
			Hashtable<String, Object> varCodes = mSectionResponseVarCodes.get(header).get(headerNumber) ;
			Hashtable<String, String> vars = mSectionResponseVars.get(header).get(headerNumber) ;
			if ( varCodes.containsKey(variable) )
				return (Integer)varCodes.get(variable) ;
			if ( !mResponseVars.containsKey(variable) )
				return null ;
			return new Integer( vars.get(variable) ) ;
		}
		
		public Long getResponseVariableLong( String variable, String header, int headerNumber ) {
			if ( header == null && headerNumber != 0 )
				throw new IllegalArgumentException("The default (null) header must be numbered '0'") ;
			
			if ( header == null )
				return getResponseVariableLong( variable ) ;
			
			Hashtable<String, Object> varCodes = mSectionResponseVarCodes.get(header).get(headerNumber) ;
			Hashtable<String, String> vars = mSectionResponseVars.get(header).get(headerNumber) ;
			if ( varCodes.containsKey(variable) )
				return (Long)varCodes.get(variable) ;
			if ( !mResponseVars.containsKey(variable) )
				return null ;
			return new Long( vars.get(variable) ) ;
		}
		
		public Float getResponseVariableFloat( String variable, String header, int headerNumber ) {
			if ( header == null && headerNumber != 0 )
				throw new IllegalArgumentException("The default (null) header must be numbered '0'") ;
			
			if ( header == null )
				return getResponseVariableFloat( variable ) ;
			
			Hashtable<String, Object> varCodes = mSectionResponseVarCodes.get(header).get(headerNumber) ;
			Hashtable<String, String> vars = mSectionResponseVars.get(header).get(headerNumber) ;
			if ( varCodes.containsKey(variable) )
				return (Float)varCodes.get(variable) ;
			if ( !mResponseVars.containsKey(variable) )
				return null ;
			return new Float( vars.get(variable) ) ;
		}
		
		public Double getResponseVariableDouble( String variable, String header, int headerNumber ) {
			if ( header == null && headerNumber != 0 )
				throw new IllegalArgumentException("The default (null) header must be numbered '0'") ;
			
			if ( header == null )
				return getResponseVariableDouble( variable ) ;
			
			Hashtable<String, Object> varCodes = mSectionResponseVarCodes.get(header).get(headerNumber) ;
			Hashtable<String, String> vars = mSectionResponseVars.get(header).get(headerNumber) ;
			if ( varCodes.containsKey(variable) )
				return (Double)varCodes.get(variable) ;
			if ( !mResponseVars.containsKey(variable) )
				return null ;
			return new Double( vars.get(variable) ) ;
		}
		
		public Nonce getResponseVariableNonce( String variable, String header, int headerNumber ) throws IOException {
			if ( header == null && headerNumber != 0 )
				throw new IllegalArgumentException("The default (null) header must be numbered '0'") ;
			
			if ( header == null )
				return getResponseVariableNonce( variable ) ;
			
			Hashtable<String, Object> varCodes = mSectionResponseVarCodes.get(header).get(headerNumber) ;
			Hashtable<String, String> vars = mSectionResponseVars.get(header).get(headerNumber) ;
			if ( varCodes.containsKey(variable) )
				return (Nonce)varCodes.get(variable) ;
			if ( !vars.containsKey(variable) )
				return null ;
			return new Nonce( vars.get(variable) ) ;
		}
	}
	

}
