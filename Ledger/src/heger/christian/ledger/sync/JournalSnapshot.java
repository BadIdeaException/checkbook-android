package heger.christian.ledger.sync;

import heger.christian.ledger.providers.Journaler;
import heger.christian.ledger.providers.MetaContentProvider.JournalContract;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.database.Cursor;

/**
 * This class provides easy access to a snapshot of the journal created from a cursor, as retrieving it directly
 * from the cursor may result in a lot of cursor moving if the access pattern is sequential in the
 * cursor.
 * <p>
 * This class relies on the original journal being <i>optimized</i>, that is, for any given combination of
 * table name, row id and column name (including if the column is unspecified), at most one operation must exist in the journal.
 * <p>
 * This class is <i>not</i> intended to be used as an alternative to querying the journal. In particular,
 * it does not update itself to any changes that happen to the journal in storage after construction
 * has taken place. It is intended solely as a helper object for synchronization.
 */
class JournalSnapshot {
	/*
	 * Think of this as a function table -> operation -> row -> [column]
	 */
	private Map<String, Map<String, Map<Long, Set<String>>>> journal = new HashMap<String, Map<String, Map<Long, Set<String>>>>();

	private JournalSnapshot() {}

	public static JournalSnapshot createFromCursor(Cursor journal) {
		final class Columns {
			/* public int colSequenceNumber = -1; */
			public int colTable = -1;
			public int colRow = -1;
			public int colColumn = -1;
			public int colOperation = -1;
			public void indexColumns(Cursor sample) {
				/* colSequenceNumber = sample.getColumnIndex(JournalContract.COL_NAME_SEQUENCE_NUMBER); */
				colTable = sample.getColumnIndex(JournalContract.COL_NAME_TABLE);
				colRow = sample.getColumnIndex(JournalContract.COL_NAME_ROW);
				colColumn = sample.getColumnIndex(JournalContract.COL_NAME_COLUMN);
				colOperation = sample.getColumnIndex(JournalContract.COL_NAME_OPERATION);
			}
		}
		Columns columns = new Columns();
		columns.indexColumns(journal);

		JournalSnapshot result = new JournalSnapshot();
		journal.moveToPosition(-1);
		while (journal.moveToNext()) {
			String table = journal.getString(columns.colTable);
			// The journal for a set table. Think of this as a function operation -> row -> [column].
			// This is the function implied by the field result.journal curried once.
			Map<String, Map<Long, Set<String>>> curried = result.journal.get(table);
			if (curried == null) {
				curried = new HashMap<String,Map<Long, Set<String>>>();
				result.journal.put(table, curried);
			}

			// The journal for a set table and operation. Think of this as a function row -> [column].
			// This is the function implied by the field result.journal curried twice.
			String operation = journal.getString(columns.colOperation);
			Map<Long, Set<String>> curried2 = curried.get(operation);
			if (curried2 == null) {
				curried2 = new HashMap<Long, Set<String>>();
				curried.put(operation, curried2);
			}

			// The journal for a set table, operation and row, that is, the set of columns.
			// This is the function implied by the field result.journal curried three times.
			long row = journal.getLong(columns.colRow);
			Set<String> curried3 = curried2.get(row);
			// Only do something when the column isn't null in the cursor
			if (!journal.isNull(columns.colColumn)) {
				if (curried3 == null) {
					curried3 = new HashSet<String>();
				}
				curried3.add(journal.getString(columns.colColumn));
			}
			curried2.put(row, curried3);
		}
		return result;
	}

	/**
	 * Returns the <i>agenda</i> for the given operation, that is,
	 * a mapping from table names to row ids to which the given operation
	 * type was applied. For example, if the journal contains creation
	 * operations for rows r1 and r2 in table t1 and for row r3 in table t2,
	 * the agenda for operation type creation will contain the following
	 * mappings:
	 * <pre>
	 * t1 -> [r1,r2]
	 * t2 -> [r3]
	 * </pre>
	 * @param operation - The operation type for which to get the agenda.
	 * @return A map from table names to sets of rows
	 */
	public Map<String,Set<Long>> getAgenda(String operation) {
		Map<String, Set<Long>> agenda = new HashMap<String,Set<Long>>();
		for (String table: journal.keySet()) {
			Map<String, Map<Long, Set<String>>> curried = journal.get(table);
			// curried == null means there were no operations at all on that table
			// curried.get(operation) == null means there were no operations of the requested type on that table
			if (curried != null && curried.get(operation) != null) {
					Set<Long> current = new HashSet<Long>();
					current.addAll(curried.get(operation).keySet());
					agenda.put(table, current);
			}
		}
		return agenda;
	}

	/**
	 * Gets all columns for which there are update operations for the specified table and row.
	 * If no updates exist for this combination, <code>null</code> is returned.
	 */
	public Set<String> getColumns(String table, long row) {
		try {
			return journal.get(table).get(Journaler.OP_TYPE_UPDATE).get(row);
		} catch (NullPointerException x) {
			return null;
		}
	}
}
