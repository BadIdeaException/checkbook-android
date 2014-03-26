package heger.christian.ledger.network;


/**
 * Constants for URL connections.
 */
public class Endpoints {
	protected static final String HOST = "10.0.2.2"; // Host machine alias from Android
	protected static final String PORT = "3000";

	protected static final String BASE = "https://" + HOST + ":" + PORT + "/";

	public static final String URL_AUTH = BASE + "oauth/token";
	public static final String URL_KEY_REQUEST = BASE + "sync/key_request";
	public static final String URL_SYNC = BASE + "sync";
}
