package heger.christian.ledger.providers;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Rule contract for the content provider. 
 */
public class RulesContract implements BaseColumns {
	public static final String TABLE_NAME = "rules";
	public static final Uri CONTENT_URI = LedgerContentProvider.CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();
	
	public static final String COL_NAME_ANTECEDENT = "antecedent";
	public static final String COL_NAME_CONSEQUENT = "consequent";
	
	public static final String MIME_SUBTYPE_SUFFIX = TABLE_NAME;

	protected RulesContract() {
	}

}
