package heger.christian.ledger.providers;

import heger.christian.ledger.db.LedgerDbHelper;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Entry contract for the content provider. It provides some constants and a
 * static method for mapping its constants to those of the database entry
 * contract. <br>
 * <br>
 * Do not confuse this class with the entry contract class contained in the
 * DB helper class. While the two are similar in nature, the one contained
 * in <code>LedgerDbHelper</code> is <i>only</i> guaranteed to provide the
 * valid contract as far as the database is concerned, while this class is
 * likewise <i>only</i> guaranteed to provide the valid contract for the
 * content provider. This is so as to decouple the outward appearance the
 * content provider provides from the actual structure of the database
 * schema.
 * 
 * @author chris
 */
public abstract class EntryContract implements BaseColumns {
	public static final String TABLE_NAME = "entries";
	public static final Uri CONTENT_URI = LedgerContentProvider.CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();
	
	public static final String COL_NAME_DATETIME = "datetime";
	public static final String COL_NAME_CAPTION = "caption";
	public static final String COL_NAME_VALUE = "value";
	public static final String COL_NAME_DETAILS = "details";
	public static final String COL_NAME_CATEGORY = "category";
	
	public static final String MIME_SUBTYPE_SUFFIX = TABLE_NAME;

	protected static String mapToDBContract(String in) {
		if (in.equals(TABLE_NAME))
			return LedgerDbHelper.EntryContract.TABLE_NAME;
		if (in.equals(_ID))
			return LedgerDbHelper.EntryContract._ID;
		if (in.equals(COL_NAME_CAPTION))
			return LedgerDbHelper.EntryContract.COL_NAME_CAPTION;
		if (in.equals(COL_NAME_CATEGORY))
			return LedgerDbHelper.EntryContract.COL_NAME_CATEGORY;
		if (in.equals(COL_NAME_DETAILS))
			return LedgerDbHelper.EntryContract.COL_NAME_DETAILS;
		if (in.equals(COL_NAME_DATETIME))
			return LedgerDbHelper.EntryContract.COL_NAME_DATETIME;
		if (in.equals(COL_NAME_VALUE))
			return LedgerDbHelper.EntryContract.COL_NAME_VALUE;
		return null;
	}

	protected EntryContract() {
	}
}