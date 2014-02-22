package heger.christian.ledger.providers;

import android.net.Uri;
import android.provider.BaseColumns;

public abstract class EntryMetadataContract implements BaseColumns {
	public static final String TABLE_NAME = "entry_metadata";
	public static final Uri CONTENT_URI = LedgerContentProvider.CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();
	
	public static final String COL_NAME_USER_CHOSEN_CATEGORY = "user_chosen_category";
	
	public static final String MIME_SUBTYPE_SUFFIX = TABLE_NAME;
	
	protected EntryMetadataContract() {}
}
