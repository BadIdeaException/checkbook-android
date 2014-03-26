package heger.christian.ledger.providers;

import heger.christian.ledger.db.LedgerDbHelper;
import android.net.Uri;
import android.provider.BaseColumns;

public abstract class CategoryContract implements BaseColumns {
	public static final String TABLE_NAME = "categories";
	public static final Uri CONTENT_URI = LedgerContentProvider.CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();
	public static final String COL_NAME_CAPTION = "caption";

	public static final String MIME_SUBTYPE_SUFFIX = TABLE_NAME;

	protected static String mapToDBContract(String in) {
		if (in.equals(TABLE_NAME))
			return LedgerDbHelper.CategoryContract.TABLE_NAME;
		if (in.equals(_ID))
			return LedgerDbHelper.CategoryContract._ID;
		if (in.equals(COL_NAME_CAPTION))
			return LedgerDbHelper.CategoryContract.COL_NAME_CAPTION;
		return null;
	}

	protected CategoryContract() {
	}
}