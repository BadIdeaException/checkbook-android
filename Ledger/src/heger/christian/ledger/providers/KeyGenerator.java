package heger.christian.ledger.providers;

public class KeyGenerator {

	private long next;
	private long max;
	
	public synchronized long generateKey(String table) {
		if (next > max) throw new IllegalStateException("Out of keys");
		return next++;		
	}
}
