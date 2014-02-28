package heger.christian.ledger.providers;

import android.net.Uri;
import android.provider.BaseColumns;

public abstract class CategorySubtotalsContract implements BaseColumns {
	public static final String TABLE_NAME = "categories_subtotals";
	public static final Uri CONTENT_URI = LedgerContentProvider.CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();
	public static final String COL_NAME_CAPTION = CategoryContract.COL_NAME_CAPTION;
	public static final String COL_NAME_SUPERCATEGORY = CategoryContract.COL_NAME_SUPERCATEGORY;
	public static final String COL_NAME_VALUE = "value";
	public static final String COL_NAME_MONTH = "month";
	
	public static final String MIME_SUBTYPE_SUFFIX = TABLE_NAME;
	public static final String QUERY_ARG_MONTH = "month";
	
	static String generateSQL(String month) {
		return "categories	left join (" +
				"select sum(" + EntryContract.COL_NAME_VALUE + ") as " + COL_NAME_VALUE + 
				", " + EntryContract.COL_NAME_CATEGORY + 
				", strftime('%m',datetime) + 12*(strftime('%Y',datetime)-1970) as " + COL_NAME_MONTH +
				" from " + EntryContract.TABLE_NAME + 
				" group by " + EntryContract.COL_NAME_CATEGORY + "," + COL_NAME_MONTH +
				(month != null ? " having " + COL_NAME_MONTH + " is null or " + COL_NAME_MONTH + "=" + month : "") +
				") on " + CategoryContract._ID + "=" + EntryContract.COL_NAME_CATEGORY;
	}
	protected static String mapToDBContract(String in) {
//		if (in.equals(TABLE_NAME))
//			return LedgerDbHelper.CategoryContract.TABLE_NAME;
//		if (in.equals(_ID))
//			return LedgerDbHelper.CategoryContract._ID;
//		if (in.equals(COL_NAME_CAPTION))
//			return LedgerDbHelper.CategoryContract.COL_NAME_CAPTION;
//		if (in.equals(COL_NAME_SUPERCATEGORY))
//			return LedgerDbHelper.CategoryContract.COL_NAME_SUPERCATEGORY;
		return null;
	}

	protected CategorySubtotalsContract() {
	}
}
