package heger.christian.checkbook.sync;

import heger.christian.checkbook.providers.CheckbookContentProvider;
import heger.christian.checkbook.providers.MetaContentProvider.RevisionTableContract;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.net.Uri;

/**
 * Factory to translate JSON encoded operations into <code>ContentProviderOperation</code>s.
 */
public class OperationFactory {
	public static final String JSON_FIELD_TABLE = JSONBuilder.JSON_FIELD_TABLE;
	public static final String JSON_FIELD_ROW = JSONBuilder.JSON_FIELD_ROW;
	public static final String JSON_FIELD_COLUMN = JSONBuilder.JSON_FIELD_COLUMN;
	public static final String JSON_FIELD_DATA = JSONBuilder.JSON_FIELD_DATA;
	// FIXME Rename to "revisions"
	public static final String JSON_FIELD_REVISIONS = JSONBuilder.JSON_FIELD_REVISIONS;

	private Translator translator;

	public OperationFactory(Translator translator) {
		this.translator = translator;
	}

	private Uri getUri(String table, long row) {
		return ContentUris.withAppendedId(CheckbookContentProvider.getUriForTable(table), row);
	}

	public List<ContentProviderOperation> getCreateOperations(JSONObject json) throws JSONException {
		String table = json.getString(JSON_FIELD_TABLE);
		long row = json.getLong(JSON_FIELD_ROW);
		JSONObject data = json.getJSONObject(JSON_FIELD_DATA);
		// FIXME Change from scalar to JSONObject { column: revision }
//		int revision = json.getInt(JSON_FIELD_REVISIONS);
		JSONObject revisions = json.getJSONObject(JSON_FIELD_REVISIONS);

		List<ContentProviderOperation> result = new ArrayList<ContentProviderOperation>(1 + revisions.length());
		ContentProviderOperation dataOperation = ContentProviderOperation.newInsert(getUri(table, row))
				.withValues(translator.translate(data))
				.build();
		result.add(dataOperation);

		// FIXME Iterate over revision columns instead of data.keys
		@SuppressWarnings("unchecked")
//		Iterator<String> iterator = data.keys();
		Iterator<String> iterator = revisions.keys();
		while (iterator.hasNext()) {
			String column = iterator.next();
			Integer revision = revisions.getInt(column);
			ContentProviderOperation revisionOperation = ContentProviderOperation.newInsert(RevisionTableContract.CONTENT_URI)
					.withValue(RevisionTableContract.COL_NAME_TABLE, table)
					.withValue(RevisionTableContract.COL_NAME_ROW, row)
//					.withValue(RevisionTableContract.COL_NAME_COLUMN, iterator.next())
					.withValue(RevisionTableContract.COL_NAME_COLUMN, column)
					.withValue(RevisionTableContract.COL_NAME_REVISION, revision)
					.build();
			result.add(revisionOperation);
		}
		return result;
	}

	public List<ContentProviderOperation> getUpdateOperations(JSONObject json) throws JSONException {
		String table = json.getString(JSON_FIELD_TABLE);
		long row = json.getLong(JSON_FIELD_ROW);
		// Column field is not actually needed, as it is reflected in the data
		// String column = json.getString(JSON_FIELD_COLUMN);
		JSONObject data = json.getJSONObject(JSON_FIELD_DATA);
//		String column = data.names().getString(0);
		// FIXME change from scalar to JSONObject { column: revision }
//		int revision = json.getInt(JSON_FIELD_REVISIONS);
		JSONObject revisions = json.getJSONObject(JSON_FIELD_REVISIONS);

		List<ContentProviderOperation> result = new ArrayList<ContentProviderOperation>(1 + revisions.length());
		ContentProviderOperation dataOperation = ContentProviderOperation.newUpdate(getUri(table, row))
				.withValues(translator.translate(data))
				.build();
		result.add(dataOperation);

		// FIXME Change to iteration over revision object (should atm only have one key, but let's be future proof)
		Iterator<String> iterator = revisions.keys();
		while (iterator.hasNext()) {
			String column = iterator.next();
			Integer revision = revisions.getInt(column);
			ContentProviderOperation revisionOperation = ContentProviderOperation.newUpdate(RevisionTableContract.CONTENT_URI)
					.withSelection(RevisionTableContract.COL_NAME_TABLE + "=? and "
							+ RevisionTableContract.COL_NAME_ROW + "=" + row + " and "
							+ RevisionTableContract.COL_NAME_COLUMN + "=?",
							new String[] { table, column })
					.withValue(RevisionTableContract.COL_NAME_REVISION, revision)
					.build();
			result.add(revisionOperation);
		}
		return result;
	}

	public List<ContentProviderOperation> getDeleteOperations(JSONObject json) throws JSONException {
		String table = json.getString(JSON_FIELD_TABLE);
		long row = json.getLong(JSON_FIELD_ROW);
		// FIXME Change from scalar to JSONObject { column: revision }
//		int revision = json.getInt(JSON_FIELD_REVISIONS);
		JSONObject revisions = json.getJSONObject(JSON_FIELD_REVISIONS);

		List<ContentProviderOperation> result = new ArrayList<ContentProviderOperation>();
		ContentProviderOperation dataOperation = ContentProviderOperation.newDelete(getUri(table, row))
				.build();
		result.add(dataOperation);

		// FIXME Change to iteration over revision object
		Iterator<String> iterator = revisions.keys();
		while (iterator.hasNext()) {
			String column = iterator.next();
			int revision = revisions.getInt(column);
			ContentProviderOperation revisionOperation = ContentProviderOperation.newUpdate(RevisionTableContract.CONTENT_URI)
					.withSelection(RevisionTableContract.COL_NAME_TABLE  + "=? and "
							+ RevisionTableContract.COL_NAME_ROW + "=" + row + " and "
							+ RevisionTableContract.COL_NAME_COLUMN + "=?",
							new String[] { table, column })
					.withValue(RevisionTableContract.COL_NAME_REVISION, revision)
					.build();
			result.add(revisionOperation);
		}
		return result;
	}

	/**
	 * @return the translator
	 */
	public Translator getTranslator() {
		return translator;
	}

	/**
	 * @param translator the translator to set
	 */
	public void setTranslator(Translator translator) {
		this.translator = translator;
	}
}
