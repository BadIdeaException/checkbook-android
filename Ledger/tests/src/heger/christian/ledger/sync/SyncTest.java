package heger.christian.checkbook.sync;

import heger.christian.checkbook.providers.CheckbookContentProvider;
import heger.christian.checkbook.sync.SyncAdapter;
import android.test.AndroidTestCase;

public class SyncTest extends AndroidTestCase {
	public void testSync() {
		new SyncAdapter(getContext(), true).onPerformSync(null,
				null,
				CheckbookContentProvider.AUTHORITY,
				getContext().getContentResolver().acquireContentProviderClient(CheckbookContentProvider.AUTHORITY),
				null);

	}
}
