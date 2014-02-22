package heger.christian.ledger.model;

import heger.christian.ledger.providers.CategoryContract;
import android.database.Cursor;

/**
 * Parameter object to hold all values returned from a query to the 
 * <code>LedgerContentProvider</code>.
 * Note that not all values are required to be filled.
 * @author chris
 *
 */
public class CategoryParameterObject {
	public final Integer id;
	public final String caption;
	public final Integer supercategory;
	
	public CategoryParameterObject(Integer id, String caption, Integer supercategory) {
		this.id = id;
		this.caption = caption;
		this.supercategory = supercategory;
	}
	
	/**
	 * Creates a <code>CategoryParameterObject</code> object, reading values 
	 * from the passed cursor. Note that not all values are guaranteed to
	 * be filled - only those for which columns are present in the cursor.
	 * For the same reason, no check is performed whether the cursor actually
	 * contains rows from the category table. 
	 * @param cursor - The cursor from which to read the values.
	 * @return An <code>CategoryParameterObject</code> object with its fields
	 * set according to the columns contained in the cursor.
	 */
	public static CategoryParameterObject readFromCursor(Cursor cursor) {
		Integer id = null;
		String caption = null;
		Integer supercategory = null;
		int index = cursor.getColumnIndex(CategoryContract._ID);
		if (index > -1) 
			id = cursor.getInt(index);
		index = cursor.getColumnIndex(CategoryContract.COL_NAME_CAPTION); 
		if (index > -1) 
			caption = cursor.getString(index);
		index = cursor.getColumnIndex(CategoryContract.COL_NAME_SUPERCATEGORY);
			supercategory = cursor.getInt(index);
		return new CategoryParameterObject(id, caption, supercategory);
	}
}
