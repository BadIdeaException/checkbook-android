package heger.christian.ledger.providers;

import heger.christian.ledger.db.LedgerDbHelper;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

/**
 * This content provider provides access to the metadata associated with the database, as there are
 * <ul>
 * <li>the <i>key_generation</i> table that holds data associated with the key generation algorithm
 * <li>the <i>journal</i> table that holds data necessary for syncing
 * </ul>
 * <p>
 * Internally, this data is stored in the same database file along with the actual application data to
 * make sure no inconsistencies arise between application data and metadata. However, this metadata
 * is only accessible through this class, allowing it to be hidden from the higher level application 
 * logic accessible from {@link LedgerContentProvider}. 
 * <p>
 * Queries to the key generation table must not contain an id fragment, as this table can never have 
 * more than one row. Queries made to the key generation table path will always return just that row.
 * <p><i>Note: The single-row property of that table is not enforced by this content provider. It is the
 * responsibility of the underlying storage mechanism to enforce it.</i>
 */
public class MetaContentProvider extends ContentProvider {
	public static final String AUTHORITY = "heger.christian.ledger.meta";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	public static class KeyGenerationContract {
		public static final String TABLE_NAME = "key_generation";
		public static final Uri CONTENT_URI = MetaContentProvider.CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();
		public static final String COL_NAME_NEXT_KEY = "next_key";
		public static final String COL_NAME_UPPER_BOUND = "upper_bound";

		public static final String MIME_SUBTYPE_SUFFIX = TABLE_NAME;
	}

	public static final int URI_KEY_GENERATION = 10;

	public static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI(AUTHORITY, KeyGenerationContract.TABLE_NAME, URI_KEY_GENERATION);
	}
	
	public static final String MIME_TYPE = "vnd.android.cursor";
	public static final String MIME_SUBTYPE = "vnd.heger.christian.ledger.provider";
	
	private SQLiteOpenHelper dbHelper;
	
	@Override
	public boolean onCreate() {
		dbHelper = new LedgerDbHelper(getContext());
		return true;
	}
	
	@Override
	public String getType(Uri uri) {
		String typeSuffix;
		String subtypeSuffix;
		switch (URI_MATCHER.match(uri)) {
			case URI_KEY_GENERATION:
				typeSuffix = "item";
				subtypeSuffix = KeyGenerationContract.MIME_SUBTYPE_SUFFIX;
				break;
			default:
				throw new IllegalArgumentException("Could not match passed URI to a known path: " + uri);
		}
		return MIME_TYPE + typeSuffix + "/" + MIME_SUBTYPE + subtypeSuffix;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		String tables = "";
		String groupBy = null;
		String having = null;
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		
		switch(URI_MATCHER.match(uri)) {
			case URI_KEY_GENERATION: 
				tables = KeyGenerationContract.TABLE_NAME;
				break;
			default:
				throw new IllegalArgumentException(
						"Could not match passed URI to a known path: " + uri);
		}
		Cursor cursor = db.query(tables, projection, selection, selectionArgs, groupBy,
				having, sortOrder, null);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String table;
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (URI_MATCHER.match(uri)) {
			case URI_KEY_GENERATION:
				table = KeyGenerationContract.TABLE_NAME;
				break;
			default:
				throw new IllegalArgumentException("Could not match passed URI to a known path: " + uri);
		}
		long rowID = db.insertOrThrow(table, null, values);;
		if (rowID > -1)
			getContext().getContentResolver().notifyChange(uri, null);
		// If we get to here, the insertion was successful
		return uri;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		String table;
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (URI_MATCHER.match(uri)) {
			case URI_KEY_GENERATION:
				table = KeyGenerationContract.TABLE_NAME;
				break;
			default:
				throw new IllegalArgumentException("Could not match passed URI to a known path: " + uri);
		}
		int result = db.delete(table, selection, selectionArgs);
		if (result > 0)
			getContext().getContentResolver().notifyChange(uri, null);
		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		String table;
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (URI_MATCHER.match(uri)) {
			case URI_KEY_GENERATION:
				table = KeyGenerationContract.TABLE_NAME;
				break;
			default:
				throw new IllegalArgumentException("Could not match passed URI to a known path: " + uri);
		}
		int result = db.update(table, values, selection, selectionArgs);
		if (result > 0)
			getContext().getContentResolver().notifyChange(uri, null);
		return result;
	}
}
