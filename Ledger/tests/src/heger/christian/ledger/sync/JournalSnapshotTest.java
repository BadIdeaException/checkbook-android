package heger.christian.ledger.sync;

import heger.christian.ledger.providers.Journaler;
import heger.christian.ledger.providers.MetaContentProvider.JournalContract;

import java.util.Map;
import java.util.Set;

import android.database.MatrixCursor;
import android.test.AndroidTestCase;

public class JournalSnapshotTest extends AndroidTestCase {
	private static final String TABLE = "table";
	private static final String TABLE2 = TABLE + "2";
	private static final String TABLE3 = TABLE + "3";
	private static final long ROW = 1;
	private static final long ROW2 = ROW + 1;
	private static final String COLUMN = "column";
	private static final String COLUMN2 = COLUMN + "2";

	MatrixCursor cursor;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		cursor = new MatrixCursor(new String[] {
				JournalContract.COL_NAME_SEQUENCE_NUMBER,
				JournalContract.COL_NAME_TABLE,
				JournalContract.COL_NAME_ROW,
				JournalContract.COL_NAME_COLUMN,
				JournalContract.COL_NAME_OPERATION
		});
	}

	public void testGetColumns() {
		cursor.addRow(new Object[] { 1, TABLE, ROW, COLUMN, Journaler.OP_TYPE_UPDATE });
		cursor.addRow(new Object[] { 2, TABLE, ROW, COLUMN2, Journaler.OP_TYPE_UPDATE });
		cursor.addRow(new Object[] { 3, TABLE, ROW2, COLUMN, Journaler.OP_TYPE_UPDATE });
		cursor.addRow(new Object[] { 4, TABLE2, ROW, COLUMN, Journaler.OP_TYPE_UPDATE });
		cursor.addRow(new Object[] { 5, TABLE, ROW, null, Journaler.OP_TYPE_CREATE });
		cursor.addRow(new Object[] { 6, TABLE3, ROW, COLUMN, Journaler.OP_TYPE_CREATE });

		JournalSnapshot snapshot = JournalSnapshot.createFromCursor(cursor);
		Set<String> columns = snapshot.getColumns(TABLE, ROW);
		assertEquals(2, columns.size());
		assertTrue(columns.contains(COLUMN));
		assertTrue(columns.contains(COLUMN2));

		columns = snapshot.getColumns(TABLE3, ROW);
		assertNull(columns);
	}

	public void testGetAgenda() {
		cursor.addRow(new Object[] { 1, TABLE, ROW, COLUMN, Journaler.OP_TYPE_UPDATE });
		cursor.addRow(new Object[] { 2, TABLE, ROW, COLUMN2, Journaler.OP_TYPE_UPDATE });
		cursor.addRow(new Object[] { 3, TABLE, ROW2, COLUMN, Journaler.OP_TYPE_UPDATE });
		cursor.addRow(new Object[] { 4, TABLE2, ROW, COLUMN, Journaler.OP_TYPE_UPDATE });
		cursor.addRow(new Object[] { 5, TABLE, ROW, null, Journaler.OP_TYPE_CREATE });
		cursor.addRow(new Object[] { 6, TABLE3, ROW, null, Journaler.OP_TYPE_CREATE });
		cursor.addRow(new Object[] { 7, TABLE3, ROW2, null, Journaler.OP_TYPE_CREATE });
		cursor.addRow(new Object[] { 8, TABLE3, ROW2, null, Journaler.OP_TYPE_DELETE });


		JournalSnapshot snapshot = JournalSnapshot.createFromCursor(cursor);

		/*
		 * Check creations
		 */
		Map<String, Set<Long>> agenda = snapshot.getAgenda(Journaler.OP_TYPE_CREATE);
		assertEquals(2, agenda.keySet().size()); // Two tables with create operations
		assertTrue(agenda.keySet().contains(TABLE));
		assertTrue(agenda.keySet().contains(TABLE3));

		assertEquals(1, agenda.get(TABLE).size()); // One row was inserted into TABLE
		assertTrue(agenda.get(TABLE).contains(ROW));

		assertNull(agenda.get(TABLE2)); // No rows were inserted into TABLE2

		assertEquals(2, agenda.get(TABLE3).size()); // Two rows were insert into TABLE3
		assertTrue(agenda.get(TABLE3).contains(ROW));
		assertTrue(agenda.get(TABLE3).contains(ROW2));

		/*
		 * Check updates
		 */
		agenda = snapshot.getAgenda(Journaler.OP_TYPE_UPDATE);
		assertEquals(2, agenda.keySet().size()); // Two tables were updated
		assertTrue(agenda.keySet().contains(TABLE));
		assertTrue(agenda.keySet().contains(TABLE2));

		assertEquals(2, agenda.get(TABLE).size()); // Two rows were updated in TABLE
		assertTrue(agenda.get(TABLE).contains(ROW));
		assertTrue(agenda.get(TABLE).contains(ROW2));

		assertEquals(1, agenda.get(TABLE2).size()); // One row was updated in TABLE2
		assertTrue(agenda.get(TABLE2).contains(ROW));

		/*
		 * Check deletions
		 */
		agenda = snapshot.getAgenda(Journaler.OP_TYPE_DELETE);
		assertEquals(1, agenda.keySet().size()); // One table had deletions on it
		assertTrue(agenda.keySet().contains(TABLE3));

		assertEquals(1, agenda.get(TABLE3).size()); // One row was deleted from TABLE3
		assertTrue(agenda.get(TABLE3).contains(ROW2));
	}
}
