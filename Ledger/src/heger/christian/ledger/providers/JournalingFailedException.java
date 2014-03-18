package heger.christian.ledger.providers;

/**
 * Generic exception indicating that there was an error while writing a record of some operation
 * to the journal.
 * When available, the cause of the failure should be indicated by using the 
 * {@link #JournalingFailedException(Throwable)} or {@link #JournalingFailedException(String, Throwable)}
 * constructor.
 */
@SuppressWarnings("serial")
public class JournalingFailedException extends RuntimeException {
	public JournalingFailedException(String message) {
		super(message);
	}
	
	public JournalingFailedException(Throwable cause) {
		super(cause);
	}
	
	public JournalingFailedException(String message, Throwable cause) {
		super(message,cause);
	}
}
