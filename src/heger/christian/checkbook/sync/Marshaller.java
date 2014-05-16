package heger.christian.checkbook.sync;

import heger.christian.checkbook.providers.CheckbookContentProvider;
import heger.christian.checkbook.providers.Journaler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderClient;
import android.content.SyncStats;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Class for turning the synchronization data into a JSON representation ("marshalling").
 * @see Unmarshaller
 */
public class Marshaller {
	public static final String JSON_FIELD_ANCHOR = "anchor";
	public static final String JSON_FIELD_CREATED = "created";
	public static final String JSON_FIELD_DELETED = "deleted";
	public static final String JSON_FIELD_UPDATED = "updated";

	private static final String TAG = Marshaller.class.getSimpleName();

	private SyncStats stats;

	private String getFilterString(Set<Long> ids) {
		String result = "";
		for (long id: ids) {
			result += result.isEmpty() ? id : "," + id;
		}
		result = "(" + result + ")";
		return result;
	}

	/**
	 * Tries to move the given <code>cursor</code> to the row with the requested <code>id</code>.
	 * This method assumes that the cursor has an _id column.
	 * <p>
	 * This method works by searching the cursor to its end from its current position, then wrapping
	 * around at the end and searching from the beginning. Therefore, it only produces a relatively
	 * minor performance penalty on sequential calls with ids in the same order as they are in the cursor.
	 * Notice however that going backward in that order requires searching once through the entire cursor.
	 * @param cursor - The cursor to search
	 * @param id - The id to find
	 * @return <code>True</code> if a row with the given id was found, in which case <code>cursor</code> is
	 * now pointing to that row. <code>False</code> if no such row was found.
	 */
	private boolean findRowInCursor(Cursor cursor, long id) {
		int position = cursor.getPosition();
		int index = cursor.getColumnIndex(BaseColumns._ID);

		if (cursor.getCount() == 0)
			return false;

		if (cursor.isBeforeFirst())
			cursor.moveToFirst();

		// Searches forward through the cursor from its current position
		// This is a do-while loop to catch the case where the cursor is already
		// on the requested row quickly rather than having to loop once through
		// the entire cursor
		do {
			if (cursor.getLong(index) == id)
				return true; // Found the correct row => leave the cursor on this position and exit
		} while (cursor.moveToNext());

		// Wrap around and search from beginning to original position
		cursor.moveToPosition(-1);
		while (cursor.moveToNext() && cursor.getPosition() < position) {
			if (cursor.getLong(index) == id)
				return true;
		}
		return false;
	}

	public JSONObject marshal(JournalSnapshot journalSnapshot, RevisionTableSnapshot revisionTableSnapshot, ContentProviderClient provider, long anchor) {
		final byte CREATIONS = 0;
		final byte UPDATES = 1;
		final byte DELETIONS = 2;

		stats = new SyncStats();

		// Create agenda (map from table names to rows for that table)
		@SuppressWarnings("unchecked")
		Map<String, Set<Long>>[] agenda = new Map[] {
				journalSnapshot.getAgenda(Journaler.OP_TYPE_CREATE),
				journalSnapshot.getAgenda(Journaler.OP_TYPE_UPDATE),
				journalSnapshot.getAgenda(Journaler.OP_TYPE_DELETE)
			};

		// Array of three JSONArrays for creations, updates and deletions
		JSONArray[] array = { new JSONArray(), new JSONArray(), new JSONArray() };

		// The translator to use for dealing with data rows
		Translator translator = new Translator();

		// Iterate over operation types
		for (byte i = CREATIONS; i <= DELETIONS; i++) {
			// Iterate over all tables in the agenda of the current operation type
			for (String table: agenda[i].keySet()) {
				Set<Long> ids = agenda[i].get(table);
				// Prepare a data cursor unless we're dealing with deletions
				Cursor data = null;
//				if (i != DELETIONS) {
					try {
						data = provider.query(CheckbookContentProvider.getUriForTable(table),
								null,
								BaseColumns._ID + " in " + getFilterString(ids),
								null,
								null);
					} catch (RemoteException x) {
						error(table, null, null, x);
						// TODO Shouldn't this be stats.numSkippedEntries += ids.size()
						stats.numSkippedEntries++;
						continue;
					}
//				} else {
//					data = new MatrixCursor(new String[] { BaseColumns._ID });
//				}

				// Iterate over all the rows in the current table
				for (long id: ids) {
					// Get array of columns if this is an update, and bogus one-element array otherwise
					String[] columns;
					if (i == UPDATES)
						columns = journalSnapshot.getColumns(table, id).toArray(new String[] {});
					else
						columns = data.getColumnNames();

					if (i != DELETIONS && !findRowInCursor(data, id)) {
						error(table, id, null, new IllegalStateException("No data found"));
						stats.numSkippedEntries += i == UPDATES ? columns.length : 1;
						continue;
					}

					// FIXME Change from scalar to Map column -> revision
					int iterations = i == UPDATES ? columns.length : 1;
					for (int j = 0; j < iterations; j++) {
					// Iterate over all the columns for the current row.
//					for (String column: columns) {
						// Create the appropriate builder
						JSONBuilder builder = null;
//						int revision = 0;
						Map<String, Integer> revisions = new HashMap<String, Integer>();
						switch (i) {
							case CREATIONS:
								builder = JSONBuilder.newCreateBuilder();
								// FIXME Fill revision map with 0 for all columns
								for (String column: columns) {
									revisions.put(column, 0);
								}
//								revision = 0;
								break;
							case UPDATES:
								builder = JSONBuilder.newUpdateBuilder();
								// FIXME Turn into single entry map
								// Because there will be further iteration cycles for each update operation, we do not
								// need to put all column revisions into the map at this point
								// I.e. no loop for column:columns
								revisions.put(columns[j], revisionTableSnapshot.getRevision(table, id, columns[j]));
//								revision = revisionTableSnapshot.getRevision(table, id, columns[j]);
								break;
							case DELETIONS:
								builder = JSONBuilder.newDeleteBuilder();
								// FIXME Fill map with revisions for all columns
								for (String column: columns) {
									revisions.put(column, revisionTableSnapshot.getRevision(table, id, column));
								}
//								revision = revisionTableSnapshot.getMaxRevision(table, id);
								break;
						}
						try {
							array[i].put(builder.withTable(table)
									.withRow(id)
									.withColumn(columns[j])
									.withData(translator.translate(data, i == UPDATES ? new String[] { columns[j] } : null))
									.withRevisions(revisions)
									.build());
						} catch (JSONException x) {
							error(table, id, i == UPDATES ? columns[j] : "n/a", x);
							stats.numSkippedEntries++;
						}
					}
				}
			}
		}

		try {
			JSONObject json = new JSONObject();
			json.put(JSON_FIELD_CREATED, array[CREATIONS]);
			json.put(JSON_FIELD_UPDATED, array[UPDATES]);
			json.put(JSON_FIELD_DELETED, array[DELETIONS]);
			json.put(JSON_FIELD_ANCHOR, anchor);
			return json;
		} catch (JSONException x) {
			/*
			 * Note: None of the above should throw a JSONException, because we're not putting in any floats
			 * that could be NaN/Infinite and only using constants as keys.
			 */
			error(null, null, null, x);
			return null;
		}
	}

	/**
	 * Gets sync stats for the last marshalling run. The following stats are kept:
	 * <ul>
	 * <li> numSkippedEntries
	 * </ul>
	 * @return Sync stats about the last call to {@link #marshal(JournalSnapshot, RevisionTableSnapshot, ContentProviderClient, long)}. If
	 * <code>marshal</code> has never been called, this will be <code>null</code>.
	 */
	public SyncStats getStats() {
		return stats;
	}

	private static void error(String message) {
		Log.e(TAG, message);
	}
	private static void error(String table, Long row, String column, Throwable cause) {
		error("An error occurred while creating the JSON representation. \n"
				+ "  Table: " + table + "\n"
				+ "  Row: " + row + "\n"
				+ "  Column: " + column + "\n"
				+ "  Cause: " + (cause != null ? cause.getClass().getSimpleName() + " with message " + cause.getMessage() : "unknown"));
	}
}
