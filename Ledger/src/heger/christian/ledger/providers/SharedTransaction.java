package heger.christian.ledger.providers;

import java.util.List;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * This class helps in situations where multiple operations have to executed changing between providers.
 *
 */
public class SharedTransaction {

	private ContentResolver resolver;

	private SharedTransaction(ContentResolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * Creates a new shared transaction. No checking is performed whether another transaction is
	 * already running.
	 */
	public static SharedTransaction newInstance(Context context) {
		SharedTransaction transaction = new SharedTransaction(context.getContentResolver());
		return transaction;
	}

	/**
	 * Executes all <code>ContentProviderOperation</code>s in <code>operations</code> on their respective
	 * <code>ContentProvider</code>s in a shared transaction.
	 * This must be run from the same process as the underlying providers.
	 * @param operations - A list of <code>ContentProviderOperation</code>s to execute
	 * @return An array containing the results of the operations
	 * @throws OperationApplicationException - Handed through from the underlying call to
	 * {@link ContentProviderOperation#apply(android.content.ContentProvider, ContentProviderResult[], int)}
	 */
	public ContentProviderResult[] applyBatch(List<ContentProviderOperation> operations) throws OperationApplicationException {
		ContentProviderResult[] results = new ContentProviderResult[operations.size()];
		ContentProviderClient dataClient = resolver.acquireContentProviderClient(LedgerContentProvider.AUTHORITY);
		LedgerContentProvider data = (LedgerContentProvider) dataClient.getLocalContentProvider();
		ContentProviderClient metaClient = resolver.acquireContentProviderClient(MetaContentProvider.AUTHORITY);
		MetaContentProvider meta = (MetaContentProvider) metaClient.getLocalContentProvider();
		Log.d("",data + "");
		Log.d("",meta + "");
		SQLiteDatabase db = data.getHelper().getWritableDatabase();
		db.beginTransaction();
		try {
			for (int i = 0; i < operations.size(); i++) {
				ContentProviderOperation operation = operations.get(i);
				Uri uri = operation.getUri();
				if (LedgerContentProvider.URI_MATCHER.match(uri) != UriMatcher.NO_MATCH) {
					results[i] = operation.apply(data, results, i);
				} else if (MetaContentProvider.URI_MATCHER.match(uri) != UriMatcher.NO_MATCH) {
					results[i] = operation.apply(meta, results, i);
				} else
					throw new IllegalArgumentException("Unknown URI " + uri);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			dataClient.release();
			metaClient.release();
		}
		return results;
	}
}
