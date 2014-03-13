package heger.christian.ledger.accounts;

import heger.christian.ledger.R;
import heger.christian.ledger.network.Endpoints;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;


/**
 * This class handles the authentication of the app or its sync adapter against the webservice's OAuth 2.0 endpoint. It supports the 
 * Resource Owner Password Credentials flow and the refresh token flow as defined in the <a href="tools.ietf.org/html/rfc6749">IETF's RFC 6749</a>. 
 * 
 * The following assumptions are made:
 * <ul>
 * <li> The server speaks HTTPS
 * <li> The server answers OAuth authentication attempts under an endpoint /oauth/token 
 * and understands UTF-8 encoding of the payload on that endpoint
 * <li> The server serves tokens in JSON containing at least the field access_token, possibly also refresh_token if one is issued
 * </ul>
 * @author chris
 *
 */
public class ServerAuthenticator {
	public static final int ERR_INVALID_REQUEST = 1;
	public static final int ERR_INVALID_CLIENT = 2;
	public static final int ERR_INVALID_GRANT = 3;
	public static final int ERR_UNAUTHORIZED_CLIENT = 4;
	public static final int ERR_UNSUPPORTED_GRANT_TYPE = 5;
	public static final int ERR_INVALID_SCOPE = 6;

	private static class ErrorParser {
		private static final String ERR_CODE_INVALID_REQUEST = "invalid_request";
		private static final String ERR_CODE_INVALID_CLIENT = "invalid_client";
		private static final String ERR_CODE_INVALID_GRANT = "invalid_grant";
		private static final String ERR_CODE_UNAUTHORIZED_CLIENT = "unauthorized_client";
		private static final String ERR_CODE_UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
		private static final String ERR_CODE_INVALID_SCOPE = "invalid_scope";
		public static int parseError(String error) {
			if (error.equals(ERR_CODE_INVALID_REQUEST)) return ERR_INVALID_REQUEST;
			if (error.equals(ERR_CODE_INVALID_CLIENT)) return ERR_INVALID_CLIENT;
			if (error.equals(ERR_CODE_INVALID_GRANT)) return ERR_INVALID_GRANT;
			if (error.equals(ERR_CODE_UNAUTHORIZED_CLIENT)) return ERR_UNAUTHORIZED_CLIENT;
			if (error.equals(ERR_CODE_UNSUPPORTED_GRANT_TYPE)) return ERR_UNSUPPORTED_GRANT_TYPE;
			if (error.equals(ERR_CODE_INVALID_SCOPE)) return ERR_INVALID_SCOPE;
			throw new IllegalArgumentException("Unrecognized error code: " + error);
		}
	}
	private static final String CHARSET = "UTF-8";
	/**
	 * Strategy interface to build the payload for posting a token request to the server.
	 */
	private interface StrategyBodyBuilder {
		public String getBody();
	}
	/**
	 * Implementation for constructing the payload for the Resource Owner Password Credentials strategy.
	 */
	private static class PasswordBodyBuilder implements StrategyBodyBuilder {
		private static final String BODY_TEMPLATE = "grant_type=password&username=%s&password=%s";
		private final String user; private final String password;
		public PasswordBodyBuilder(String user, String password) { this.user = user; this.password = password; }
		@Override
		public String getBody() {
			try {
				return String.format(BODY_TEMPLATE, URLEncoder.encode(user, CHARSET), URLEncoder.encode(password, CHARSET));
			} catch (UnsupportedEncodingException x) {
				// This should never happen
				throw new Error("UTF-8 not supported");
			}
		}
	}
	/**
	 * Implementation for constructing the payload for the refresh token strategy.
	 * @author chris
	 */
	private static class RefreshBodyBuilder implements StrategyBodyBuilder {
		private static final String BODY_TEMPLATE = "grant_type=refresh_token&refresh_token=%s";
		private final String refreshToken;
		public RefreshBodyBuilder(String refreshToken) { this.refreshToken = refreshToken; }
		@Override
		public String getBody() {
			try {
				return String.format(BODY_TEMPLATE, URLEncoder.encode(refreshToken, CHARSET));
			} catch (UnsupportedEncodingException x) {
				// This should never happen
				throw new Error("UTF-8 not supported");
			}
		}
	}
	
	private final Context context;
		
	private static final String CLIENT_ID = "Ledger Android app";
	
	
	private static final String JSON_FIELD_ACCESS_TOKEN = "access_token";
	private static final String JSON_FIELD_REFRESH_TOKEN = "refresh_token";
	private static final String JSON_FIELD_ERR_CODE = "error";
	private static final String JSON_FIELD_ERR_DESCRIPTION = "error_description";
	
	public ServerAuthenticator(Context context) {
		this.context = context;
	}
	
//	/**
//	 * Default constructor using 10.0.2.2:3000
//	 */
//	public ServerAuthenticator(Context context) {
//		this(context, Endpoints.HOST, Endpoints.PORT);
//	}
	
	/**
	 * Prepares an HTTPS connection that incorporates the truststore under res/raw/auth_server_truststore
	 * @return The prepared HTTPS connection. The return value has not been connected yet, so further configuration,
	 * for instance with regard to the headers, can be done
	 * @throws IOException - If obtaining the connection failed, or importing the certificate trust store failed
	 */
	private final HttpsURLConnection getConnection() throws IOException {
		// Load the truststore that includes self-signed cert as a "trusted" entry.
		KeyStore truststore;
		try {
			truststore = KeyStore.getInstance("BKS");
			truststore.load(context.getResources().openRawResource(R.raw.truststore), "keystore".toCharArray());

			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(truststore);

			// Create custom SSL context that incorporates that truststore
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

			HttpsURLConnection connection = (HttpsURLConnection) Endpoints.URL_AUTH.openConnection();
			connection.setSSLSocketFactory(sslContext.getSocketFactory());
			
			// Disable caching
			connection.addRequestProperty("Pragma", "no-cache");
			connection.addRequestProperty("Cache-Control", "no-cache");
			
			return connection;
		} catch (KeyStoreException x) {
			throw new IOException("Could not open connection to server",x);
		} catch (NoSuchAlgorithmException x) {
			// Note: This should really never happen with the default algorithm
			throw new IOException("Could not open connection to server",x);
		} catch (CertificateException x) {
			throw new IOException("Could not open connection to server",x);
		} catch (NotFoundException x) {
			throw new IOException("Could not open connection to server",x);
		} catch (KeyManagementException x) {
			throw new IOException("Could not open connection to server",x);
		}
	}
	
	/**
	 * Handles the actual communication with the server and processing of the result. 
	 * The passed <code>payloadBuilder</code> is used to construct the body of the 
	 * POST request to be issued.
	 * 
	 * All other aspects are considered to be identical across all flow types.
	 * @param payloadBuilder - The <code>StrategyBodyBuilder</code> to use for 
	 * constructing the POST body.
	 * @return A token set, or <code>null</code>
	 */
	private TokenSet handleAuthentication(StrategyBodyBuilder payloadBuilder) throws IOException, AuthenticationFailedException {
		TokenSet tokens = null;
		HttpsURLConnection connection = null;
		OutputStreamWriter writer = null;
		BufferedReader reader = null;
		try {
			// Establish connection
			connection = getConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
			connection.connect();
			
			// Send authentication request to server 
			writer = new OutputStreamWriter(new BufferedOutputStream(connection.getOutputStream()), CHARSET);
			writer.write(payloadBuilder.getBody());
			writer.flush();
			
			// Authentication was successful if the response has a 200 HTTP status code.
			int response = connection.getResponseCode();
			// Read result from response and parse into JSON object
			// using input stream if login was successful (HTTP status 200) or
			// error stream if it wasn't
			reader = new BufferedReader(new InputStreamReader(
					connection.getResponseCode() == HttpURLConnection.HTTP_OK ? connection.getInputStream() : connection.getErrorStream(), 
					CHARSET));
			StringBuilder builder = new StringBuilder();
			for (String s = reader.readLine(); s != null; s = reader.readLine()) {
				builder.append(s);
			}
			JSONObject json = new JSONObject(builder.toString());
			
			if (response == HttpURLConnection.HTTP_OK) {			
				// Return result token set. Note that access token cannot be null at this point, 
				// otherwise we wouldn't have seen a 200 status code.
				tokens = new TokenSet(json.optString(JSON_FIELD_ACCESS_TOKEN, null), json.optString(JSON_FIELD_REFRESH_TOKEN, null));
			} else {
				// If authentication was unsuccessful, try to parse the error cause from the JSON
				// and construct an AuthenticationFailedException from it
				String errorCode = json.getString(JSON_FIELD_ERR_CODE);
				String errorMessage = json.optString(JSON_FIELD_ERR_DESCRIPTION);
				if (json.has(JSON_FIELD_ERR_CODE)) {
					throw new AuthenticationFailedException(ErrorParser.parseError(errorCode), errorMessage);
				} else {
					throw new AuthenticationFailedException(errorMessage);
				}
			}
		} catch (JSONException x) {
			x.printStackTrace();
		} finally {
			if (connection != null) connection.disconnect();
			try {
				if (writer != null) writer.close();
				if (reader != null) reader.close();
			} catch (IOException x) {
				Log.e(this.getClass().getCanonicalName(), "An IOException occurred while closing the HTTP handling streams.");
				x.printStackTrace();
			}
		}
		return tokens;
	}
	/**
	 * Authenticates against the server using the Resource Owner Password Credentials flow. If successful, a <code>TokenSet</code> 
	 * containing at least an access token and possibly a refresh token is returned.
	 * Whether or not a refresh token is issued depends on the server implementation of the OAuth 2 protocol.
	 * @param user - The user name to use for authentication
	 * @param password - The user's password
	 * @return A set of tokens if authentication was successful.
	 * @throws AuthenticationFailedException - If the authentication attempt with the server yielded anything other than
	 * a token set
	 * @throws IOException - If the connection with the server broke 
	 * @see TokenSet
	 */
	public final TokenSet authenticate(String user, String password) throws IOException, AuthenticationFailedException {
		return handleAuthentication(new PasswordBodyBuilder(user, password));
	}
	
	/**
	 * Authenticates against the server using the passed refresh token. If successful, a <code>TokenSet</code> 
	 * containing at least an access token and possibly a refresh token is returned.
	 * Whether or not a refresh token is issued depends on the server implementation of the OAuth 2 protocol.
	 * @param refreshToken - The refresh token to present to the server
	 * @return A set of tokens if authentication was successful.
	 * @throws AuthenticationFailedException - If the authentication attempt with the server yielded anything other than
	 * a token set
	 * @throws IOException - If the connection with the server broke 
	 * @see TokenSet
	 */
	public final TokenSet authenticate(String refreshToken) throws IOException, AuthenticationFailedException {
		return handleAuthentication(new RefreshBodyBuilder(refreshToken));
	}
}
