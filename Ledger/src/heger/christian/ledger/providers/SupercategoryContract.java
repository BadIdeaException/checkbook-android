package heger.christian.ledger.providers;

import heger.christian.ledger.db.LedgerDbHelper;
import android.net.Uri;
import android.provider.BaseColumns;

public class SupercategoryContract implements BaseColumns {
	public static final String TABLE_NAME = "supercategories";
	public static final Uri CONTENT_URI = LedgerContentProvider.CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();
	public static final String COL_NAME_CAPTION = "caption";

	public static final String MIME_SUBTYPE_SUFFIX = TABLE_NAME;
	
	protected static String mapToDBContract(String in) {
		if (in.equals(TABLE_NAME))
			return LedgerDbHelper.SupercategoryContract.TABLE_NAME;
		if (in.equals(_ID))
			return LedgerDbHelper.SupercategoryContract._ID;
		if (in.equals(COL_NAME_CAPTION))
			return LedgerDbHelper.SupercategoryContract.COL_NAME_CAPTION;
		return null;
	}

	protected SupercategoryContract() {
	}
}