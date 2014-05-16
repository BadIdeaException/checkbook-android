package heger.christian.checkbook.sync;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Builder class to construct JSON output for an operation. Different builder types are available for
 * the operations "create", "update" and "delete", which will make sure that only those values that
 * are applicable for the operation type in question are reflected in the built output.
 */
public abstract class JSONBuilder {
	public static final String JSON_FIELD_TABLE = "table_name";
	public static final String JSON_FIELD_ROW = "row";
	public static final String JSON_FIELD_COLUMN = "column_name";
	public static final String JSON_FIELD_DATA = "data";
	// FIXME Rename to "revisions"
	public static final String JSON_FIELD_REVISIONS = "revisions";

	private String table = null;
	private JSONObject data = null;
	private Long row = null;
	private String column = null;
	// FIXME Change to Map column -> revision
//	private Integer revision = null;
	private Map<String, Integer> revisions = new HashMap<String, Integer>();

	/**
	 * Concrete <code>JSONBuilder</code> for a created entry.
	 * The method <code>withColumn(String)</code> is disabled in this implementation,
	 * as creations always refer to the entire row. Even if called, a subsequent call
	 * to <code>build()</code> will produce a result that doesn't have a column.
	 */
	protected static class CreateBuilder extends JSONBuilder {
		@Override
		public JSONBuilder withColumn(String column) { return this; }
	}
	/**
	 * Concrete <code>JSONBuilder</code> for an updated entry.
	 */
	protected static class UpdateBuilder extends JSONBuilder {}
	/**
	 * Concrete <code>JSONBuilder</code> for a deleted entry.
	 * The methods <code>withColumn(String)</code> and <code>withData(JSONObject)</code>
	 * are disabled in this implementation,
	 * as deletions always refer to the entire row and invalidate that rows data.
	 * Even if called, a subsequent call
	 * to <code>build()</code> will produce a result that doesn't have a column or data.
	 */
	protected static class DeleteBuilder extends JSONBuilder {
		@Override
		public JSONBuilder withColumn(String column) { return this; }
		@Override
		public JSONBuilder withData(JSONObject data) { return this; }
	}

	private JSONBuilder() {}

	public JSONBuilder withTable(String table) {
		this.table = table;
		return this;
	}

	public JSONBuilder withData(JSONObject data) {
		this.data = data;
		return this;
	}

	public JSONBuilder withRow(long row) {
		this.row = row;
		return this;
	}

	public JSONBuilder withColumn(String column) {
		this.column = column;
		return this;
	}

	public JSONBuilder withRevisions(Map<String, Integer> revisions) {
		// FIXME Change from scalar to map column -> revision
		this.revisions = revisions;
//		this.revision = revision;
		return this;
	}

	public JSONObject build() throws JSONException {
		JSONObject result = new JSONObject();
		if (table != null)
			result.put(JSON_FIELD_TABLE, table);
		if (row != null)
			result.put(JSON_FIELD_ROW, row);
		if (column != null)
			result.put(JSON_FIELD_COLUMN, column);
		if (data != null)
			result.put(JSON_FIELD_DATA, data);
		if (revisions != null) {
			// FIXME Convert map to JSONObject { column: revision }
			result.put(JSON_FIELD_REVISIONS, new JSONObject(revisions));
//			result.put(JSON_FIELD_REVISIONS, revision);
		}
		return result;
	}

	public static JSONBuilder newCreateBuilder() {
		return new CreateBuilder();
	}

	public static JSONBuilder newUpdateBuilder() {
		return new UpdateBuilder();
	}

	public static JSONBuilder newDeleteBuilder() {
		return new DeleteBuilder();
	}
}
