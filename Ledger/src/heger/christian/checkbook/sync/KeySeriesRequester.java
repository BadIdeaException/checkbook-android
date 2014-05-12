package heger.christian.checkbook.sync;

import heger.christian.checkbook.network.Endpoints;
import heger.christian.checkbook.network.SecureConnection;
import heger.christian.checkbook.network.UnauthorizedAccessException;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.goebl.david.Response;
import com.goebl.david.Webb;
import com.goebl.david.WebbException;

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

	private SSLSocketFactory factory;

	/**
	 * Makes a request for a new key series to the server. If a series was successfully obtained, they new
	 * next key and upper bound are returned in the result bundle under <code>KEY_NEXT_KEY</code> and
	 * <code>KEY_UPPER_BOUND</code>, respectively. Otherwise, the result contains an error code (usually
	 * the HTTP response code) and an error message.
	 * @param token - The access token to present to the server
	 * @return A <code>Bundle</code> containing
	 * <ul>
	 * <li>the next key to use under <code>KEY_NEXT_KEY</code> and new upper bound under <code>KEY_UPPER_BOUND</code>, or
	 * <li>an error code under <code>KEY_ERROR_CODE</code> and error message under <code>KEY_ERROR_MESSAGE</code>
	 * @throws IOException If an IOException occurred during communication with the server
	 * @throws UnauthorizedAccessException - If the request was rejected by the server due to an invalid token
	 */
	public Bundle request(String token) throws IOException {
		Webb webb = Webb.create();
		webb.setSSLSocketFactory(factory);
		Response<JSONObject> response;
		try {
			response = webb.post(Endpoints.URL_KEY_REQUEST)
				.header(Webb.HDR_ACCEPT, Webb.APP_JSON)
				.header(Webb.HDR_AUTHORIZATION, "Bearer " + token)
				.header("Pragma", "no-cache")
				.header("Cache-Control", "no-cache")
				.ensureSuccess()
				.asJsonObject();
		} catch (WebbException x) {
			if (x.getResponse() != null && x.getResponse().getStatusCode() == HttpURLConnection.HTTP_FORBIDDEN) {
				throw new UnauthorizedAccessException();
			} else
				throw new IOException(x);
		}
		JSONObject json = response.getBody();

		Bundle result = new Bundle();
		// Check the response code and parse the result
		try {
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
		return result;
	}

	public SSLSocketFactory getSSLSocketFactory() {
		return factory;
	}

	public void setSSLSocketFactory(SSLSocketFactory factory) {
		this.factory = factory;
	}
}
