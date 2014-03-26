package heger.christian.ledger.accounts;

import heger.christian.ledger.network.Endpoints;
import heger.christian.ledger.network.LedgerSSLContextFactory;
import heger.christian.ledger.network.TruststoreException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.goebl.david.Response;
import com.goebl.david.Webb;
import com.goebl.david.WebbException;


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

	private static final String TAG = ServerAuthenticator.class.getSimpleName();

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
	 * Handles the actual communication with the server and processing of the result.
	 * The passed <code>payloadBuilder</code> is used to construct the body of the
	 * POST request to be issued.
	 *
	 * All other aspects are considered to be identical across all flow types.
	 * @param payloadBuilder - The <code>StrategyBodyBuilder</code> to use for
	 * constructing the POST body.
	 * @return A token set, or <code>null</code>
	 * @throws TruststoreException - If an error occurred while setting up the trust store. The error is automatically logged.
	 */
	private TokenSet handleAuthentication(StrategyBodyBuilder payloadBuilder) throws AuthenticationFailedException, TruststoreException, IOException {
		TokenSet tokens = null;
		Webb webb = Webb.create();
		try {
			webb.setSSLSocketFactory(new LedgerSSLContextFactory(context).createSSLContext().getSocketFactory());
		} catch (TruststoreException x) {
			Log.e(TAG, "There was an error when setting up the SSL context: \n "
					+ "Error type: + " + x.getCause().getClass().getSimpleName() + "\n"
					+ "Error message: " + x.getCause().getMessage());
			throw x;
		}

		Response<JSONObject> response = null;
		JSONObject json = null;
		try {
			response = webb.post(Endpoints.URL_AUTH)
					.header(Webb.HDR_ACCEPT, Webb.APP_JSON)
					.header(Webb.HDR_CONTENT_TYPE, Webb.APP_FORM)
					.header(Webb.HDR_CONTENT_ENCODING, CHARSET)
					.header("Pragma", "no-cache")
					.header("Cache-Control", "no-cache")
					.body(payloadBuilder.getBody())
					.asJsonObject();

			// Intercept 403 error code since this is a fairly like case
			if (response.getStatusCode() == HttpsURLConnection.HTTP_FORBIDDEN) {
				json = (JSONObject) response.getErrorBody();
				throw new AuthenticationFailedException(
						ErrorParser.parseError(json.optString(JSON_FIELD_ERR_CODE)),
						json.optString(JSON_FIELD_ERR_DESCRIPTION));
			}

			json = response.getBody();
			response.ensureSuccess();
		} catch (WebbException x) {
			// Something went wrong - find out what
			if (x.getCause() instanceof ConnectException) {
				// Connection refused - host unreachable
				throw new IOException(x.getCause());
			} else {
				int errorCode = response.getStatusCode();
				String errorMessage = response.getResponseMessage();
				try {
					json = (JSONObject) response.getErrorBody();
					errorCode = ErrorParser.parseError(json.getString(JSON_FIELD_ERR_CODE));
					errorMessage = json.getString(JSON_FIELD_ERR_DESCRIPTION);
				} catch (ClassCastException x2) {
				} catch (JSONException x2) {}
				throw new AuthenticationFailedException(errorCode, errorMessage);
			}
		}

		tokens = new TokenSet(
				json.optString(JSON_FIELD_ACCESS_TOKEN, null),
				json.optString(JSON_FIELD_REFRESH_TOKEN, null));
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
	public final TokenSet authenticate(String user, String password) throws IOException, TruststoreException, AuthenticationFailedException {
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
	public final TokenSet authenticate(String refreshToken) throws IOException, TruststoreException, AuthenticationFailedException {
		return handleAuthentication(new RefreshBodyBuilder(refreshToken));
	}
}
