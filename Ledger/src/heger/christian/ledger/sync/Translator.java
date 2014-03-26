package heger.christian.ledger.sync;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * This class can translate between storage representation and JSON objects for arbitrary
 * objects.
 */
public class Translator {

	/**
	 * Translates the current row of the passed cursor into a JSON representation
	 * of the form <code>column: value</code> for the columns contained in the cursor.
	 * If the cursor is before the first or after the last row, the return value is
	 * <code>null</code>.
	 * It is possible to specify which columns are to be included in the result, by
	 * passing their names in the <code>projection</code> parameter.
	 * <p>As neither cursor column types as returned by <code>cursor.getType(int)</code>
	 * nor the JSON have a notion of single or double precision floating point numbers,
	 * this method will <i>always</i> return a double for columns for which
	 * <code>getType</code> is <code>FIELD_TYPE_FLOAT</code>.
	 * @param cursor - The cursor from which to read. It may be empty, but must not
	 * be <code>null</code>.
	 * @param columns - The names of the columns to include in the result. If this
	 * is <code>null</code>, all columns in the cursor will be included.
	 * @return A <code>JSONObject</code> with the values of the cursor's
	 * current row, or <code>null</code> if the cursor is before its first
	 * or after its last row.
	 * @throws JSONException
	 */
	public JSONObject translate(Cursor cursor, String[] projection) throws JSONException {
		if (cursor == null)
			throw new NullPointerException("Cannot translate from a null cursor");

		if (cursor.isBeforeFirst() || cursor.isAfterLast())
			return null;

		// Turn the array into a set for easier use
		Set<String> columns = null;
		if (projection != null) {
			columns = new HashSet<String>();
			Collections.addAll(columns, projection);
		}

		JSONObject result = new JSONObject();
		for (int i = 0; i < cursor.getColumnCount(); i++) {
			if (columns == null || columns.contains(cursor.getColumnName(i))) {
				Object value;
				switch (cursor.getType(i)) {
					case Cursor.FIELD_TYPE_BLOB: value =
							cursor.getBlob(i);
					break;
					case Cursor.FIELD_TYPE_FLOAT:
						// It is not differentiated here between single and double precision
						// So just get the more accurate one here for safety
						value = cursor.getDouble(i);
						break;
					case Cursor.FIELD_TYPE_INTEGER:
						// It is not differentiated here between 32 and 64 bit integers
						value = cursor.getLong(i);
						if (Integer.MIN_VALUE <= (Long) value && Integer.MAX_VALUE >= (Long) value) {
							value = Integer.valueOf(((Long) value).intValue());
						}
						break;
					case Cursor.FIELD_TYPE_STRING:
						value = cursor.getString(i);
						break;
					case Cursor.FIELD_TYPE_NULL:
						value = JSONObject.NULL;
						break;
					default:
						throw new IllegalStateException("Unrecognized column type for column " + cursor.getColumnName(i));
				}
				result.put(cursor.getColumnName(i), value);
			}
		}
		return result;
	}

	/**
	 * Translates the passed JSON object <code>json</code> into a <code>ContentValues</code> object
	 * such that for each <code>name: value</code> mapping in <code>json</code>, there is a
	 * corresponding mapping from <code>name</code> to <code>value</code> in the result. The type
	 * of the mapped value is inferred from the coercion performed by <code>json</code>.
	 * <code>json</code> must not contain nested <code>JSONObject</code>s or <code>JSONArray</code>s.
	 * Parsing won't fail if it does, but any nested values will be ignored.
	 * @param json - The <code>JSONObject</code> to translate. May be empty, but must not be <code>null</code>.
	 * @return A <code>ContentValues</code> with the same <code>name: value</code> mappings as in <code>json</code>.
	 * @throws JSONException If <code>json</code> is not a legal JSON object
	 */
	public ContentValues translate(JSONObject json) throws JSONException {
		if (json == null)
			throw new IllegalArgumentException("Cannot translate from a null JSON object");

		ContentValues result = new ContentValues();
		JSONArray names = json.names();
		if (names != null) {
			for (int i = 0; i < names.length(); i++) {
				String name = names.getString(i);
				Object value = json.get(name);
				if (value instanceof String) {
					result.put(name, (String) value);
				} else if (value instanceof Boolean) {
					result.put(name, (Boolean) value);
				} else if (value instanceof Integer) {
					result.put(name, (Integer) value);
				} else if (value instanceof Long) {
					result.put(name, (Long) value);
				} else if (value instanceof Double) {
					result.put(name, (Double) value);
				} else if (value == null) {
					result.putNull(name);
				} else if (value instanceof JSONObject) {
					continue;
				} else if (value instanceof JSONArray) {
					continue;
				}
			}
		}
		return result;
	}
}
