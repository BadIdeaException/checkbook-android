package heger.christian.ledger.model;

import heger.christian.ledger.providers.EntryContract;
import android.database.Cursor;
import android.os.Bundle;

/**
 * Parameter object to hold all values returned from a query to the 
 * <code>LedgerContentProvider</code>. 
 * Note that not all values are required to be filled.
 * @author chris
 *
 */
public class EntryParameterObject {
	public final Integer id;
	public String caption;
	public Integer category;
	public String details;
	public String datetime;
	public Integer value;
	
	public EntryParameterObject(Integer id, String caption, Integer category, String details, String datetime, Integer value) {
		this.id = id;
		this.caption = caption;
		this.category = category;
		this.details = details;
		this.datetime = datetime;
		this.value = value;
	}
	
	/**
	 * Convenience constructor for <code>EntryParameterObject(id,null,null,null,null,null)</code>.
	 */
	public EntryParameterObject(Integer id) {
		this(id,null,null,null,null,null);
	}
	
	/**
	 * Creates a <code>EntryParameterObject</code> object, reading values 
	 * from the passed cursor. Note that not all values are guaranteed to
	 * be filled - only those for which columns are present in the cursor.
	 * For the same reason, no check is performed whether the cursor actually
	 * contains rows from the entry table. 
	 * @param cursor - The cursor from which to read the values.
	 * @return An <code>EntryParameterObject</code> object with its fields
	 * set according to the columns contained in the cursor.
	 */
	public static EntryParameterObject readFromCursor(Cursor cursor) {
		EntryParameterObject result;

		int index = cursor.getColumnIndex(EntryContract._ID);
		result = new EntryParameterObject(index > -1 ? cursor.getInt(index) : null);
		index = cursor.getColumnIndex(EntryContract.COL_NAME_CAPTION); 
		if (index > -1) 
			result.caption = cursor.getString(index);
		index = cursor.getColumnIndex(EntryContract.COL_NAME_CATEGORY);
		if (index > -1) 
			result.category = cursor.getInt(index);
		index = cursor.getColumnIndex(EntryContract.COL_NAME_DATETIME);
		if (index > -1) 
			result.datetime = cursor.getString(index);
		index = cursor.getColumnIndex(EntryContract.COL_NAME_DETAILS);
		if (index > -1) 
			result.details = cursor.getString(index);
		index = cursor.getColumnIndex(EntryContract.COL_NAME_VALUE);
		if (index > -1) 
			result.value = cursor.getInt(index);
		return result;
	}
	
	/**
	 * Creates a <code>EntryParameterObject</code> object, reading values 
	 * from the passed bundle. Note that not all values are guaranteed to
	 * be filled - only those for which mappings are present in the bundle.
	 * Keys are expected to respect the <code>EntryParameterContract</code>.
	 * @param bundle - The bundle from which to read the values.
	 * @return An <code>EntryParameterObject</code> object with its fields
	 * set according to the mappings in the bundle.
	 */
	public static EntryParameterObject readFromBundle(Bundle bundle) {
		EntryParameterObject result;

		result = new EntryParameterObject(bundle.containsKey(EntryContract._ID) ? bundle.getInt(EntryContract._ID) : null);
		result.caption = bundle.getString(EntryContract.COL_NAME_CAPTION, null);
		result.category = bundle.containsKey(EntryContract.COL_NAME_CATEGORY) ? bundle.getInt(EntryContract.COL_NAME_CATEGORY) : null;
		result.datetime = bundle.getString(EntryContract.COL_NAME_DATETIME, null);
		result.details = bundle.getString(EntryContract.COL_NAME_DETAILS, null);
		result.value = bundle.containsKey(EntryContract.COL_NAME_VALUE) ? bundle.getInt(EntryContract.COL_NAME_VALUE) : null;
		return result;
	}
	
	/**
	 * Writes this instance into the passed bundle. 
	 */
	public void writeToBundle(Bundle bundle) {
		if (id != null)
			bundle.putInt(EntryContract._ID, id);
		if (caption != null)
			bundle.putString(EntryContract.COL_NAME_CAPTION, caption);
		if (category != null)
			bundle.putInt(EntryContract.COL_NAME_CATEGORY, category);
		if (datetime != null) 
			bundle.putString(EntryContract.COL_NAME_DATETIME, datetime);
		if (details != null)
			bundle.putString(EntryContract.COL_NAME_DETAILS, details);
		if (value != null) 
			bundle.putInt(EntryContract.COL_NAME_VALUE, value);
	}
}
