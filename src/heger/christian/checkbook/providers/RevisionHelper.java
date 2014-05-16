package heger.christian.checkbook.providers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.database.Cursor;

/**
 * Helper class to facilitate revision keeping. It will cache column names for tables queried in the past
 * so as to speed up future queries.
 */
public class RevisionHelper {

	/**
	 * Map from table names to column names
	 */
	private Map<String, Set<String>> columns = new HashMap<String, Set<String>>();
	private CheckbookContentProvider provider;

	public RevisionHelper(CheckbookContentProvider provider) {
		this.provider = provider;
	}

	/**
	 * Returns a set containing all column names for the specified <code>table</code>.
	 * If this is the first request for column names for that table, a query to the database
	 * will be made using the content provider instance specified during creation. The resulting
	 * set of column names for that table will be cached for speeding up future queries. Otherwise,
	 * if a cached result already exists for that table, it is returned.
	 * @param table - The table for which to get the column names.
	 * @return A set of all column names for that table.
	 */
	public Set<String> getColumns(String table) {
		if (!columns.containsKey(table)) {
			Cursor cursor = provider.query(CheckbookContentProvider.getUriForTable(table), null, null, null, null);
			Set<String> newColumns = new HashSet<String>(cursor.getColumnCount());
			for (int i = 0; i < cursor.getColumnCount(); i++) {
				newColumns.add(cursor.getColumnName(i));
			}
			columns.put(table, newColumns);
		}
		return columns.get(table);
	}
}
