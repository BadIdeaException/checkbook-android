package heger.christian.ledger.network;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Constants for URL connections.
 */
public class Endpoints {
	protected static final String HOST = "10.0.2.2"; // Host machine alias from Android
	protected static final String PORT = "3000";
	
	protected static final String BASE = "https://" + HOST + ":" + PORT + "/";
	
	public static final URL URL_AUTH;
	public static final URL URL_KEY_REQUEST;
	
	static {
		try {
			URL_AUTH = new URL(BASE + "oauth/token");
			URL_KEY_REQUEST = new URL(BASE + "sync/key_request");
		} catch (MalformedURLException x) {
			throw new ExceptionInInitializerError(x);
		}
		
	}
}
