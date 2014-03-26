package heger.christian.ledger.sync;

import heger.christian.ledger.providers.LedgerContentProvider;
import heger.christian.ledger.sync.SyncAdapter;
import android.test.AndroidTestCase;

public class SyncTest extends AndroidTestCase {
	public void testSync() {
		new SyncAdapter(getContext(), true).onPerformSync(null,
				null,
				LedgerContentProvider.AUTHORITY,
				getContext().getContentResolver().acquireContentProviderClient(LedgerContentProvider.AUTHORITY),
				null);

	}
}
