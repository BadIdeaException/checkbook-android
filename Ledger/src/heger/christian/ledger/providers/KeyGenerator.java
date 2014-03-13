package heger.christian.ledger.providers;

import heger.christian.ledger.providers.MetaContentProvider.KeyGenerationContract;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.os.Handler;

/**
 * Key generator based loosely on the high-low algorithm. Keys are generated with calls to {@link #generateKey(String)}.   
 * 
 */
public class KeyGenerator {
	protected class ContentObserver extends android.database.ContentObserver {
		boolean active = true;
		public ContentObserver(Handler handler) {
			super(handler);
		}
		@Override
		public void onChange(boolean selfChange) {
			if (active && !selfChange)
				initialized = false;
		}
	}
	private ContentObserver observer = new ContentObserver(null);
	
	/** Next key to be issued by {@link #generateKey(String)} */
	private long nextKey;
	/** Upper bound (inclusively) of the current series */
	private long upperBound;
	private boolean initialized = false;
	
	private final ContentResolver resolver;
	
	public KeyGenerator(ContentResolver resolver) {
		this.resolver = resolver;
		resolver.registerContentObserver(KeyGenerationContract.CONTENT_URI, true, observer);
	}
	
	/**
	 * Loads the initial values for the next key and the upper bound of the current series from
	 * storage.
	 * <p>
	 * <strong>Storage access is blocking.</strong> If necessary, it is the caller's responsibility
	 * to call this method asynchronously. However, in most cases, this will be invoked as part of
	 * an insertion into the application logic content provider, which in itself should already be
	 * performed asynchronously, thereby keeping this method from blocking the UI thread.
	 * @throws OutOfKeysException If the values could not be loaded from storage. Note that if values
	 * could be loaded, but those values do not allow for further key requests, this method will not
	 * throw an exception.
	 */
	protected void initialize() throws OutOfKeysException {
		initialized = true;
		Cursor cursor = resolver.query(KeyGenerationContract.CONTENT_URI, null, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			nextKey = cursor.getLong(cursor.getColumnIndex(KeyGenerationContract.COL_NAME_NEXT_KEY));
			upperBound = cursor.getLong(cursor.getColumnIndex(KeyGenerationContract.COL_NAME_UPPER_BOUND));
		} else {
			throw new OutOfKeysException();
		}
	}
	
	/**
	 * Checks if there are keys available. If this method returns <code>false</code>, subsequent calls
	 * to {@link #generateKey(String)} will throw an <code>OutOfKeysException</code>.
	 * @return - <code>True</code> if this key generator has at least one more key in its
	 * current series, or <code>false</code> if the supply is exhausted. 
	 */
	public boolean hasKey() {
		if (!initialized) initialize();
		
		return nextKey <= upperBound;
	}
	
	/**
	 * Generates a new key from the current series. 
	 * <p>
	 * The generated key will immediately be written through to storage, so key generation must be considered a
	 * long-running operations Consequently, it should always be called asynchronously, either directly or 
	 * indirectly by performing the storage insertion requesting the key asynchronously (this should be the more common case).
	 * <p>
	 * This method is thread safe. 
	 * @param table - The table for which a new primary key is requested
	 * @return A new primary key from the current series
	 * @throws OutOfKeysException - If the current series is exhausted.
	 */
	public synchronized long generateKey(String table) throws OutOfKeysException {
		// Load nextKey and upperBound lazily
		if (!initialized) initialize();
		
		if (nextKey > upperBound) throw new OutOfKeysException();

		long key = nextKey++;

		ContentValues values = new ContentValues();
		values.put(KeyGenerationContract.COL_NAME_NEXT_KEY, nextKey);
		values.put(KeyGenerationContract.COL_NAME_UPPER_BOUND, upperBound);
		try {
			// Avoid re-reading the values we're inserting from storage
			observer.active = false;
			if (resolver.insert(KeyGenerationContract.CONTENT_URI, values) == null)
				throw new IllegalStateException("Could not mark issued key " + key + " as used");
		} finally {
			observer.active = true;
		}
		
		return key++;		
	}	
}
