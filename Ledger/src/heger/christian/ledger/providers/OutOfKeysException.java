package heger.christian.ledger.providers;

/**
 * Indicates that the key generator has run out of available keys and was therefore unable
 * to satisfy the request for a new key.
 */
@SuppressWarnings("serial")
public class OutOfKeysException extends RuntimeException {
}
