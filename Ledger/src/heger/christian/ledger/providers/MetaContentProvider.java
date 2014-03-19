package heger.christian.ledger.providers;

import heger.christian.ledger.db.LedgerDbHelper;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * This content provider provides access to the metadata associated with the database, as there are
 * <ul>
 * <li>the <i>key_generation</i> table that holds data associated with the key generation algorithm
 * <li>the <i>journal</i> table that holds data necessary for syncing
 * <li>the <i>revision table</i> holding revision numbers for all application data
 * <li>the <i>sequence_anchor</i> table which holds a single cell with the
 * sequence anchor (the next sequence number to be synchronized)
 * </ul>
 * <p>
 * Internally, this data is stored in the same database file along with the actual application data to
 * make sure no inconsistencies arise between application data and metadata. However, this metadata
 * is only accessible through this class, allowing it to be hidden from the higher level application
 * logic accessible from {@link LedgerContentProvider}.
 * <p>
 * Queries to the key generation table must not contain an id fragment, as this table can never have
 * more than one row. Queries made to the key generation table path will always return just that row.
 * Likewise, queries to the sequence anchor must not contain an id fragment.
 * <p><i>Note: The single-row property of that table is not enforced by this content provider. It is the
 * responsibility of the underlying storage mechanism to enforce it.</i>
 *
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

	public static class JournalContract {
		public static final String TABLE_NAME = "journal";
		public static final Uri CONTENT_URI = MetaContentProvider.CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();
		public static final String COL_NAME_SEQUENCE_NUMBER = "sqn";
		public static final String COL_NAME_TABLE = "table_name";
		public static final String COL_NAME_ROW = "row";
		public static final String COL_NAME_COLUMN = "column_name";
		public static final String COL_NAME_OPERATION = "operation";
		public static final String MIME_SUBTYPE_SUFFIX = TABLE_NAME;
	}

	public static class RevisionTableContract {
		public static final String TABLE_NAME = "revision_table";
		public static final Uri CONTENT_URI = MetaContentProvider.CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();
		public static final String COL_NAME_TABLE = "table_name";
		public static final String COL_NAME_ROW = "row";
		public static final String COL_NAME_COLUMN = "column_name";
		public static final String COL_NAME_REVISION = "revision";
		public static final String MIME_SUBTYPE_SUFFIX = TABLE_NAME;
	}

	public static class SequenceAnchorContract {
		public static final String TABLE_NAME = "sequence_anchor";
		public static final Uri CONTENT_URI = MetaContentProvider.CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build();
		public static final String COL_NAME_SEQUENCE_ANCHOR = "sequence_anchor";
		public static final String MIME_SUBTYPE_SUFFIX = TABLE_NAME;
	}

	public static final int URI_KEY_GENERATION = 10;
	public static final int URI_JOURNAL = 20;
	public static final int URI_JOURNAL_ID = 21;
	public static final int URI_REVISION_TABLE = 30;
	public static final int URI_SEQUENCE_ANCHOR = 40;

	public static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI(AUTHORITY, KeyGenerationContract.TABLE_NAME, URI_KEY_GENERATION);
		URI_MATCHER.addURI(AUTHORITY, JournalContract.TABLE_NAME, URI_JOURNAL);
		URI_MATCHER.addURI(AUTHORITY, JournalContract.TABLE_NAME + "/#", URI_JOURNAL_ID);
		URI_MATCHER.addURI(AUTHORITY, RevisionTableContract.TABLE_NAME, URI_REVISION_TABLE);
		URI_MATCHER.addURI(AUTHORITY, SequenceAnchorContract.TABLE_NAME, URI_SEQUENCE_ANCHOR);
	}

	public static final String MIME_TYPE = "vnd.android.cursor";
	public static final String MIME_SUBTYPE = "vnd.heger.christian.ledger.provider";

	/* package private */ SQLiteOpenHelper dbHelper;

	protected SQLiteOpenHelper getHelper() {
		String TAG = "MetaContentProvider";
		Log.d(TAG, "Getting helper for context " + getContext());
		if (dbHelper == null) {
			try {
				LedgerContentProvider buddy = (LedgerContentProvider) getContext().getContentResolver().acquireContentProviderClient(LedgerContentProvider.AUTHORITY).getLocalContentProvider();
				Log.d(TAG, "Found buddy " + buddy);
				if (buddy.dbHelper != null) {
					dbHelper = buddy.dbHelper;
					Log.d(TAG, "Found dbHelper from buddy " + dbHelper);
				}
			} catch (ClassCastException x /* Wasn't a MetaContentProvider */) {
			} catch (NullPointerException x /* One of the involved objects is unavailable */) {
			}
		}
		if (dbHelper == null) {
			Log.d(TAG, "Helper unavailable, creating new");
			dbHelper = new LedgerDbHelper(getContext());
		}
		return dbHelper;
	}

	@Override
	public boolean onCreate() {
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
			case URI_JOURNAL:
				typeSuffix = "dir";
				subtypeSuffix = JournalContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_JOURNAL_ID:
				typeSuffix = "item";
				subtypeSuffix = JournalContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_REVISION_TABLE:
				typeSuffix = "dir";
				subtypeSuffix = RevisionTableContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_SEQUENCE_ANCHOR:
				typeSuffix = "item";
				subtypeSuffix = SequenceAnchorContract.MIME_SUBTYPE_SUFFIX;
				break;
			default:
				throw new IllegalArgumentException("Could not match passed URI to a known path: " + uri);
		}
		return MIME_TYPE + typeSuffix + "/" + MIME_SUBTYPE + subtypeSuffix;
	}

	private String getTableNameFromURI(Uri uri) {
		String table;
		switch(URI_MATCHER.match(uri)) {
			case URI_KEY_GENERATION:
				table = KeyGenerationContract.TABLE_NAME;
				break;
			case URI_JOURNAL: // $FALL-THROUGH$
			case URI_JOURNAL_ID:
				table = JournalContract.TABLE_NAME;
				break;
			case URI_REVISION_TABLE:
				table = RevisionTableContract.TABLE_NAME;
				break;
			case URI_SEQUENCE_ANCHOR:
				table = SequenceAnchorContract.TABLE_NAME;
				break;
			default:
				throw new IllegalArgumentException(
						"Could not match passed URI to a known path: " + uri);
		}
		return table;
	}

	@Override
	public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
		SQLiteDatabase db = getHelper().getWritableDatabase();
		db.beginTransaction();
		try {
			ContentProviderResult[] results = super.applyBatch(operations);
			db.setTransactionSuccessful();
			return results;
		} finally {
			db.endTransaction();
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		String tables = getTableNameFromURI(uri);
		String groupBy = null;
		String having = null;
		SQLiteDatabase db = getHelper().getReadableDatabase();
		switch (URI_MATCHER.match(uri)) {
			case URI_JOURNAL_ID:
				if (TextUtils.isEmpty(selection))
					selection = JournalContract.COL_NAME_SEQUENCE_NUMBER + "=" + uri.getLastPathSegment();
				else
					selection = "(" + selection + ") and " + JournalContract.COL_NAME_SEQUENCE_NUMBER + "=" + uri.getLastPathSegment();
		}
		Cursor cursor = db.query(tables, projection, selection, selectionArgs, groupBy,
				having, sortOrder, null);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String table = getTableNameFromURI(uri);
		SQLiteDatabase db = getHelper().getWritableDatabase();
		long rowID = db.insertOrThrow(table, null, values);
		uri = ContentUris.withAppendedId(uri, rowID);
		if (rowID > -1)
			getContext().getContentResolver().notifyChange(uri, null);
		// If we get to here, the insertion was successful
		return uri;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		String table = getTableNameFromURI(uri);
		SQLiteDatabase db = getHelper().getWritableDatabase();
		switch (URI_MATCHER.match(uri)) {
			case URI_JOURNAL_ID:
				if (TextUtils.isEmpty(selection))
					selection = JournalContract.COL_NAME_SEQUENCE_NUMBER + "=" + uri.getLastPathSegment();
				else
					selection = "(" + selection + ") and " + JournalContract.COL_NAME_SEQUENCE_NUMBER + "=" + uri.getLastPathSegment();
		}
		int result = db.delete(table, selection, selectionArgs);
		if (result > 0)
			getContext().getContentResolver().notifyChange(uri, null);
		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		String table = getTableNameFromURI(uri);
		SQLiteDatabase db = getHelper().getWritableDatabase();
		switch (URI_MATCHER.match(uri)) {
			case URI_JOURNAL_ID:
				if (TextUtils.isEmpty(selection))
					selection = JournalContract.COL_NAME_SEQUENCE_NUMBER + "=" + uri.getLastPathSegment();
				else
					selection = "(" + selection + ") and " + JournalContract.COL_NAME_SEQUENCE_NUMBER + "=" + uri.getLastPathSegment();
		}
		int result = db.update(table, values, selection, selectionArgs);
		if (result > 0)
			getContext().getContentResolver().notifyChange(uri, null);
		return result;
	}
}
