package heger.christian.checkbook.network;


/**
 * Constants for URL connections.
 */
public class Endpoints {
	protected static final String HOST = "checkbook.dlinkddns.com";
	protected static final String PORT = "443";

	protected static final String BASE = "https://" + HOST + ":" + PORT + "/";

	public static final String URL_AUTH = BASE + "oauth/token";
	public static final String URL_KEY_REQUEST = BASE + "sync/key_request";
	public static final String URL_SYNC = BASE + "sync";
}
