package heger.christian.checkbook.accounts;

/**
 * Represents an error returned from the OAuth server during authentication. In addition to the message 
 * inherited from <code>Exception</code> it also has an error code.
 * 
 * Note that this does <i>not</i> include network, socket or protocol errors that occurred during the
 * authentication process. Rather, it applies to those cases where the OAuth protocol completed 
 * correctly, but the actual authentication failed, by means of the server returning an error code and
 * a message rather than the requested tokens.
 */
@SuppressWarnings("serial")
public class AuthenticationFailedException extends Exception {
	public static final int ERR_UNKNOWN = 0;
	
	private final int errorCode;
	
	public AuthenticationFailedException() {
		this(ERR_UNKNOWN, null, null);
	}
	
	public AuthenticationFailedException(int errorCode, String message, Throwable cause) {
		super(message,cause);
		this.errorCode = errorCode;
	}
	
	public AuthenticationFailedException(int errorCode, String message) {
		this(errorCode, message, null);
	}
	
	public AuthenticationFailedException(String message) {
		this(ERR_UNKNOWN, message);
	}
	
	public int getErrorCode() { return errorCode; }
	
	@Override
	public String getMessage() {
		String message = errorCode != ERR_UNKNOWN ? String.valueOf(errorCode) : "";
		message += super.getMessage();
		return message;
	}
}
