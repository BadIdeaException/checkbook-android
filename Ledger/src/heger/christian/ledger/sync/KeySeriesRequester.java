package heger.christian.ledger.sync;

import heger.christian.ledger.network.SecureConnection;

import java.io.IOException;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

/**
 * This class encapsulates the algorithm to request a new key series from the server. This is done 
 * in the {@link #request(SecureConnection)} method. 
 * <p>
 * Note that this class's responsibility is limited to <i>only</i> key series requests. It does not
 * set up the used connection with respect to certificate handling or authorization (it does, however,
 * disable caching on the connection). Likewise, if a new key series was successfully obtained, it
 * does not write it to persistent storage or perform any other operations on it.  
 */
public class KeySeriesRequester {
	protected static final String JSON_FIELD_NEXT_KEY = "next_key";
	protected static final String JSON_FIELD_UPPER_BOUND = "upper_bound";
	
	public static final String KEY_NEXT_KEY = "next_key";
	public static final String KEY_UPPER_BOUND = "upper_bound";
	public static final String KEY_ERROR_CODE = "error_code";
	public static final String KEY_ERROR_MESSAGE = "error_message";
	
	/**
	 * Makes a request for a new key series to the server. If a series was successfully obtained, they new
	 * next key and upper bound are returned in the result bundle under <code>KEY_NEXT_KEY</code> and 
	 * <code>KEY_UPPER_BOUND</code>, respectively. Otherwise, the result contains an error code (usually
	 * the HTTP response code) and an error message. 
	 * <p>This method will add request headers, so the connection <b>must not</b> have been called. Also,
	 * this method will not disconnect the connection on completion.
	 * @param connection - A {@link SecureConnection} to the key request endpoint on the server, with the 
	 * required certificates (if any) already trusted and authorization set if required for the endpoint. 
	 * The connection object <strong>must not</strong> 
	 * be connected yet, that is, <code>connection.connect()</code> must not have been called. 
	 * @return A <code>Bundle</code> containing 
	 * <ul>
	 * <li>the next key to use under <code>KEY_NEXT_KEY</code> and new upper bound under <code>KEY_UPPER_BOUND</code>, or
	 * <li>an error code under <code>KEY_ERROR_CODE</code> and error message under <code>KEY_ERROR_MESSAGE</code>
	 * @throws IOException - If an IOException occurred during communication with the server
	 *  
	 */
	public Bundle request(SecureConnection connection) throws IOException {
		// Tell the server what we're expecting
		connection.addRequestProperty("Accept", "application/json");
		
		// Disable caching to avoid getting stale responses
		connection.addRequestProperty("Pragme", "no-cache");
		connection.addRequestProperty("Cache-Control","no-cache");
		
		connection.connect();
		
		Bundle result = new Bundle();
		// Check the response code and parse the result
		if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
			String response = connection.read();
			try {
			JSONObject json = new JSONObject(response);
			// Read both values first to keep bundle consistent if getting an exception on the second read
			long nextKey = json.getLong(JSON_FIELD_NEXT_KEY);
			long upperBound = json.getLong(JSON_FIELD_UPPER_BOUND);
			result.putLong(KEY_NEXT_KEY, nextKey);
			result.putLong(KEY_UPPER_BOUND, upperBound);
			} catch (JSONException x) {
				// Invalid response will be handled as an HTTP 500 status
				result.putInt(KEY_ERROR_CODE, HttpsURLConnection.HTTP_INTERNAL_ERROR);
				result.putString(KEY_ERROR_MESSAGE, "Invalid response");
			}
		} else {
			// Error if bad response code or invalid result
			result.putInt(KEY_ERROR_CODE, connection.getResponseCode());
			String errorMessage = connection.getResponseMessage();
			if (errorMessage == null) 
				errorMessage = connection.readError();
			result.putString(KEY_ERROR_MESSAGE, errorMessage);
		}
		return result;
	}
}
