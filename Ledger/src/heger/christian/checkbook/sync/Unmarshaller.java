package heger.christian.checkbook.sync;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.SyncStats;
import android.util.Log;

public class Unmarshaller {
	public static final String JSON_FIELD_ANCHOR = Marshaller.JSON_FIELD_ANCHOR;
	public static final String JSON_FIELD_CREATED = Marshaller.JSON_FIELD_CREATED;
	public static final String JSON_FIELD_DELETED = Marshaller.JSON_FIELD_DELETED;
	public static final String JSON_FIELD_UPDATED = Marshaller.JSON_FIELD_UPDATED;

	private static final String TAG = Unmarshaller.class.getSimpleName();

	private SyncStats stats;

	public List<ContentProviderOperation> unmarshal(JSONObject json) {
		stats = new SyncStats();
		final short CREATIONS = 0;
		final short UPDATES = 1;
		final short DELETIONS = 2;

		List<ContentProviderOperation> result = new LinkedList<ContentProviderOperation>();
		JSONArray[] array = new JSONArray[3];

		Translator translator = new Translator();
		OperationFactory factory = new OperationFactory(translator);

		final String FIELDS[] = new String[] { JSON_FIELD_CREATED, JSON_FIELD_UPDATED, JSON_FIELD_DELETED };
		for (byte i = CREATIONS; i <= DELETIONS; i++) {
			try {
				array[i] = json.getJSONArray(FIELDS[i]);
			} catch (JSONException x) {
				stats.numParseExceptions++;
			}
		}

		for (byte i = CREATIONS; i <= DELETIONS; i++) {
			for (int j = 0; j < array[i].length(); j++) {
				JSONObject entry = null;
				try {
					entry = array[i].getJSONObject(j);
					List<ContentProviderOperation> operations = null;
					switch (i) {
						case CREATIONS: operations = factory.getCreateOperations(entry); stats.numInserts++; break;
						case UPDATES:   operations = factory.getUpdateOperations(entry); stats.numUpdates++; break;
						case DELETIONS: operations = factory.getDeleteOperations(entry); stats.numDeletes++; break;
					}
					result.addAll(operations);
				} catch (JSONException x) {
					error(entry, x);
					stats.numSkippedEntries++;
				}
			}
		}
		return result;
	}

	private static void error(JSONObject json, Throwable cause) {
		Log.e(TAG, "An error occurred while trying to parse a JSON object \n"
				+ "  JSON: " + (json != null ? json.toString() : "unknown")
				+ "  Cause: " + cause.getClass().getSimpleName() + " with message " + cause.getMessage());
	}

	/**
	 * Gets sync stats for the last unmarshalling run. The following stats are kept:
	 * <ul>
	 * <li> numInserts
	 * <li> numUpdates
	 * <li> numDeletes
	 * <li> numParseExceptions
	 * <li> numSkippedEntries
	 * </ul>
	 * @return Sync stats about the last call to {@link #marshal(JournalSnapshot, RevisionTableSnapshot, ContentProviderClient, long)}. If
	 * <code>marshal</code> has never been called, this will be <code>null</code>.
	 */
	public SyncStats getStats() {
		return stats;
	}
}
