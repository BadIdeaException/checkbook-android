package heger.christian.ledger.network;


/**
 * Catch-all exception to avoid having to deal with the dozens of poorly documented exceptions thrown
 * during the various steps of setting up an SSL context with the self-signed certificate truststore.
 * The underlying exception should be provided as the cause.
 */
@SuppressWarnings("serial")
public class TruststoreException extends Exception {

	public TruststoreException(Throwable cause) {
		super(cause);
	}

}
