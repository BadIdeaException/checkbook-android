package heger.christian.checkbook.sync;

import heger.christian.checkbook.providers.MetaContentProvider.RevisionTableContract;

import java.util.NoSuchElementException;

import android.database.MatrixCursor;
import android.test.AndroidTestCase;

public class RevisionTableSnapshotTest extends AndroidTestCase {
	private static final String TABLE = "table";
	private static final long ROW = 1;
	private static final String COLUMN = "column";
	private static final int REVISION = 1;

	MatrixCursor cursor;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		cursor = new MatrixCursor(new String[] {
 				RevisionTableContract.COL_NAME_TABLE,
				RevisionTableContract.COL_NAME_ROW,
				RevisionTableContract.COL_NAME_COLUMN,
				RevisionTableContract.COL_NAME_REVISION
		});
	}

	public void testLookup() {
		cursor.addRow(new Object[] { TABLE, ROW, COLUMN, REVISION });
		RevisionTableSnapshot snapshot = RevisionTableSnapshot.createFromCursor(cursor);
		assertEquals(snapshot.getRevision(TABLE, ROW, COLUMN), REVISION);

		// Test nothing is returned that doesn't exist
		try {
			snapshot.getRevision(TABLE + "2", ROW, COLUMN);
			fail();
		} catch (NoSuchElementException x) {}

		try {
			snapshot.getRevision(TABLE, ROW + 1, COLUMN);
			fail();
		} catch (NoSuchElementException x) {}

		try {
			snapshot.getRevision(TABLE, ROW, COLUMN + "2");
			fail();
		} catch (NoSuchElementException x) {}
	}

	public void testMaxLookup() {
		cursor.addRow(new Object[] { TABLE, ROW, COLUMN, 1 });
		cursor.addRow(new Object[] { TABLE, ROW, COLUMN + "2", 2 });
		cursor.addRow(new Object[] { TABLE, ROW, COLUMN + "3", 5 });

		RevisionTableSnapshot snapshot = RevisionTableSnapshot.createFromCursor(cursor);
		assertEquals(5, snapshot.getMaxRevision(TABLE, ROW));
	}
}
