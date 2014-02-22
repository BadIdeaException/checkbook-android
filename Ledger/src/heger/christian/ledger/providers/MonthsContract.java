package heger.christian.ledger.providers;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract class for the months view.
 
 * @author chris
 * 
 */
public class MonthsContract implements BaseColumns {
	public static final String TABLE_NAME = "months";
	public static final Uri CONTENT_URI = LedgerContentProvider.CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();
	public static final String COL_NAME_VALUE = "value";
	public static final String MIME_SUBTYPE_SUFFIX = TABLE_NAME;
	
	// Calculates the elapsed months since January 1970 
	final static String MONTHS_QUERY = "" +
			"SELECT strftime('%m'," + EntryContract.COL_NAME_DATETIME + ")+12*(strftime('%Y'," + EntryContract.COL_NAME_DATETIME + ")-1970) as "
			+ MonthsContract._ID
			+ ",  sum(value) as "
			+ COL_NAME_VALUE
			+ " FROM "
			+ EntryContract.mapToDBContract(EntryContract.TABLE_NAME)
			+ " GROUP BY " + _ID;
}