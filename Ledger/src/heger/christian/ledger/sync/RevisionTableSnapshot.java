package heger.christian.ledger.sync;

import heger.christian.ledger.providers.MetaContentProvider.RevisionTableContract;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import android.database.Cursor;

/**
 * This class provides easy access to a snapshot of the revision table created from a cursor, as retrieving it
 * directly from the cursor may result in a lot of cursor moving if the access pattern is sequential in the
 * cursor.
 * <p>
 * Revisions can be queried using {@link #getRevision(String, long, String)}.
 * <p>
 * This class is <i>not</i> intended to be used as an alternative to querying the revision table. In particular,
 * it does not update itself to any changes that happen to the revision table in storage after construction
 * has taken place. It is intended solely as a helper object for synchronization.
 */
class RevisionTableSnapshot {
	/**
	 * Key to map revision number to.
	 */
	private static class Key {
		private final long row;
		private final String table;

		public Key(String table, long row) {
			if (table == null)
				throw new IllegalArgumentException("Table must not be null");
			this.table = table;
			this.row = row;
		}

		/**
		 * Returns true iff <code>o</code> is a <code>Key</code> and
		 * <code>this</code> and <code>o</code> have equal <code>table</code> and
		 * <code>row</code>.
		 */
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Key))
				return false;
			Key k2 = (Key) o;
			boolean result = table.equals(k2.table)
					&& row == k2.row;
			return result;
		}
		/**
		 * Returns a hash code of this key as the
		 * the xor combination of the hash codes of <code>table</code> and <code>row</code>.
		 */
		@Override
		public int hashCode() {
			int result = table.hashCode() ^ Long.valueOf(row).hashCode();
			return result;
		}
		@Override
		public String toString() {
			return "(" + table + "," + row + ")";
		}
	}

	private RevisionTableSnapshot() {}

	/**
	 * Creates and returns a new <code>RevisionTableSnapshot</code> snapshot
	 * constructed from the passed cursor.
	 */
	public static RevisionTableSnapshot createFromCursor(Cursor cursor) {
		/**
		 * Helper class to cache column indices.
		 */
		final class Columns {
			public int colColumn = -1;
			public int colRevision = -1;
			public int colRow = -1;
			public int colTable = -1;

			public void indexColumns(Cursor sample) {
				colTable = sample
						.getColumnIndex(RevisionTableContract.COL_NAME_TABLE);
				colRow = sample.getColumnIndex(RevisionTableContract.COL_NAME_ROW);
				colColumn = sample
						.getColumnIndex(RevisionTableContract.COL_NAME_COLUMN);
				colRevision = sample
						.getColumnIndex(RevisionTableContract.COL_NAME_REVISION);
			}
		}
		Columns columns = new Columns();
		columns.indexColumns(cursor);

		RevisionTableSnapshot result = new RevisionTableSnapshot();
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) {
			String table = cursor.getString(columns.colTable);
			long row = cursor.getLong(columns.colRow);
			String column = cursor.getString(columns.colColumn);
			Key key = new Key(table, row);
			if (!result.revisions.containsKey(key)) {
				result.revisions.put(key, new HashMap<String, Integer>());
			}
			result.revisions.get(key).put(column, cursor.getInt(columns.colRevision));
		}
		return result;
	}

	/*
	 * Think of this as a function (table,row) -> column -> revision
	 */
	private Map<Key, Map<String, Integer>> revisions = new HashMap<Key, Map<String, Integer>>();

	/**
	 * Gets the revision number for the passed table, row and column.
	 * If no such combination exists,
	 * a <code>NoSuchElementException</code> is thrown.
	 */
	public int getRevision(String table, long row, String column) {
		Integer result = null;
		Map<String, Integer> curried = revisions.get(new Key(table, row));
		if (curried != null) {
			result = curried.get(column);
		}

		if (result != null)
			return result;
		else
			throw new NoSuchElementException("No entry for table " + table + ", row " + row + " and column " + column);
	}

	/**
	 * Gets the maximum revision for any column in the specified table for the specified row.
	 * If no entry exists for that combination, a <code>NoSuchElementException</code> is thrown.
	 * @return The highest revision for any column of the specified <code>row</code> in the
	 * specified <code>table</code>.
	 */
	public int getMaxRevision(String table, long row) {
		int result = 0;
		Map<String, Integer> curried = revisions.get(new Key(table, row));
		if (curried == null)
			throw new NoSuchElementException("No entry for table " + table + " and row " + row);

		for (Integer revision: curried.values())
			result = Math.max(result, revision);
		return result;
	}

	/**
	 * Returns a map from column names to their associated revisions for the specified row
	 * in the specified table. If no such combination exists, a <code>NoSuchElementException</code>
	 * is thrown.
	 * @return A map from column names to their revision numbers. Never <code>null</code>.
	 */
	public Map<String, Integer> getRevisionsForRow(String table, long row) {
		Map<String, Integer> curried = revisions.get(new Key(table,row));
		if (curried == null)
			throw new NoSuchElementException("No entry for table " + table + " and row " + row);
		return curried;
	}
}
