package heger.christian.ledger.providers;

import heger.christian.ledger.db.LedgerDbHelper;
import heger.christian.ledger.providers.MetaContentProvider.RevisionTableContract;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

/**
 * Content provider for accessing the application logic level data (as opposed to the metadata accessible
 * through {@link MetaContentProvider}.
 * <p>
 * When inserting, primary keys are derived from the attached <code>KeyGenerator</code>. If the generator
 * finds itself unable to satisfy the key request, it throws an <code>OutOfKeysException</code>, which this
 * content provider will propagate. It does not attempt to acquire a new series for the generator.
 * Therefore, calls to {@link #insert(Uri, ContentValues)} should always be prepared to handle
 * this exception to prevent data loss (as the insertion will not have been made) and ungraceful failure.
 * <p>
 * In addition, insertions, updates and deletions are automatically written to the journal by the attached
 * <code>Journaler</code>. If journaling fails for whatever reason, the database operation causing the
 * journaling will be rolled back as well, and a <code>JournalingFailedException</code> will be thrown.
 * Therefore, any calls to <code>insert(Uri, ContentValues)</code>, {@link #update(Uri, ContentValues, String, String[])},
 * {@link #delete(Uri, String, String[])} should always be prepared to handle this exception.
 */
public class LedgerContentProvider extends ContentProvider {
	public static final String AUTHORITY = "heger.christian.ledger.providers.ledgercontentprovider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	public static final int URI_ENTRIES = 10;
	public static final int URI_ENTRIES_ID = 11;
	public static final int URI_CATEGORIES = 20;
	public static final int URI_CATEGORIES_ID = 21;
	public static final int URI_CATEGORIES_SUBTOTALS = 25;
	public static final int URI_CATEGORIES_SUBTOTALS_ID = 26;
	public static final int URI_MONTHS = 40;
	public static final int URI_MONTHS_ID = 41;
	public static final int URI_RULES = 50;
	public static final int URI_RULES_ID = 51;
	public static final int URI_ENTRY_METADATA = 60;
	public static final int URI_ENTRY_METADATA_ID = 61;

	public static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI(AUTHORITY, EntryContract.TABLE_NAME, URI_ENTRIES);
		URI_MATCHER.addURI(AUTHORITY, EntryContract.TABLE_NAME + "/#", URI_ENTRIES_ID);
		URI_MATCHER.addURI(AUTHORITY, CategoryContract.TABLE_NAME, URI_CATEGORIES);
		URI_MATCHER.addURI(AUTHORITY, CategoryContract.TABLE_NAME + "/#", URI_CATEGORIES_ID);
		URI_MATCHER.addURI(AUTHORITY, CategorySubtotalsContract.TABLE_NAME, URI_CATEGORIES_SUBTOTALS);
		URI_MATCHER.addURI(AUTHORITY, CategorySubtotalsContract.TABLE_NAME + "/#", URI_CATEGORIES_SUBTOTALS_ID);
		URI_MATCHER.addURI(AUTHORITY, MonthsContract.TABLE_NAME, URI_MONTHS);
		URI_MATCHER.addURI(AUTHORITY, MonthsContract.TABLE_NAME + "/#", URI_MONTHS_ID);
		URI_MATCHER.addURI(AUTHORITY, RulesContract.TABLE_NAME, URI_RULES);
		URI_MATCHER.addURI(AUTHORITY, RulesContract.TABLE_NAME + "/#", URI_RULES_ID);
		URI_MATCHER.addURI(AUTHORITY, EntryMetadataContract.TABLE_NAME, URI_ENTRY_METADATA);
		URI_MATCHER.addURI(AUTHORITY, EntryMetadataContract.TABLE_NAME + "/#", URI_ENTRY_METADATA_ID);
	}
	public static final String MIME_TYPE = "vnd.android.cursor";
	public static final String MIME_SUBTYPE = "vnd.heger.christian.ledger.provider";

	/* package private */ SQLiteOpenHelper dbHelper;
	private KeyGenerator keyGenerator;
	private Journaler journaler;

	@Override
	public boolean onCreate() {
		keyGenerator = new KeyGenerator(getContext().getContentResolver());
		journaler = new Journaler(getContext().getContentResolver());
		return true;
	}

	protected SQLiteOpenHelper getHelper() {
		String TAG = "LedgerContentProvider";
		Log.d(TAG, "Getting helper for context " + getContext());
		if (dbHelper == null) {
			ContentProviderClient client = getContext().getContentResolver().acquireContentProviderClient(MetaContentProvider.AUTHORITY);
			try {
				MetaContentProvider buddy = (MetaContentProvider) client.getLocalContentProvider();
				Log.d(TAG, "Found buddy " + buddy);
				if (buddy.dbHelper != null) {
					dbHelper = buddy.dbHelper;
					Log.d(TAG, "Found dbHelper from buddy " + dbHelper);
				}
			} catch (ClassCastException x /* Wasn't a MetaContentProvider */) {
			} catch (NullPointerException x /* MetaContentProvider is unavailable (because it's client is null) */) {
			} finally {
				client.release();
			}
		}
		if (dbHelper == null) {
			Log.d(TAG, "Helper unavailable, creating new");
			dbHelper = new LedgerDbHelper(getContext());
		}
		return dbHelper;
	}

	protected String getTableFromUri(Uri uri) {
		String table;
		switch (URI_MATCHER.match(uri)) {
			case URI_ENTRIES: //$FALL-THROUGH$
			case URI_ENTRIES_ID:
				table = EntryContract.mapToDBContract(EntryContract.TABLE_NAME);
				break;
			case URI_CATEGORIES: //$FALL-THROUGH$
			case URI_CATEGORIES_ID:
				table = CategoryContract.mapToDBContract(CategoryContract.TABLE_NAME);
				break;
			case URI_CATEGORIES_SUBTOTALS: //$FALL-THROUGH$
			case URI_CATEGORIES_SUBTOTALS_ID:
				String month = uri.getQueryParameter(CategorySubtotalsContract.QUERY_ARG_MONTH);
				table = CategorySubtotalsContract.generateSQL(month);
				break;
			case URI_MONTHS: //$FALL_THROUGH$
			case URI_MONTHS_ID:
				table = MonthsContract.TABLE_NAME;
				break;
			case URI_RULES: //$FALL-THROUGH$
			case URI_RULES_ID:
				table = RulesContract.TABLE_NAME;
				break;
			case URI_ENTRY_METADATA: //$FALL-THROUGH$
			case URI_ENTRY_METADATA_ID:
				table = EntryMetadataContract.TABLE_NAME;
				break;
			default:
				throw new IllegalArgumentException("Could not match passed URI to a known path: " + uri);
		}
		return table;
	}


	@Override
	public String getType(Uri uri) {
		String typeSuffix;
		String subtypeSuffix;
		switch (URI_MATCHER.match(uri)) {
			case URI_ENTRIES:
				typeSuffix = "dir";
				subtypeSuffix = EntryContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_ENTRIES_ID:
				typeSuffix = "item";
				subtypeSuffix = EntryContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_CATEGORIES:
				typeSuffix = "dir";
				subtypeSuffix = CategoryContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_CATEGORIES_ID:
				typeSuffix = "item";
				subtypeSuffix = CategoryContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_CATEGORIES_SUBTOTALS:
				typeSuffix = "dir";
				subtypeSuffix = CategorySubtotalsContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_CATEGORIES_SUBTOTALS_ID:
				typeSuffix = "dir";
				subtypeSuffix = CategorySubtotalsContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_MONTHS:
				typeSuffix = "dir";
				subtypeSuffix = MonthsContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_MONTHS_ID:
				typeSuffix = "item";
				subtypeSuffix = MonthsContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_RULES:
				typeSuffix = "dir";
				subtypeSuffix = RulesContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_RULES_ID:
				typeSuffix = "item";
				subtypeSuffix = RulesContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_ENTRY_METADATA:
				typeSuffix = "dir";
				subtypeSuffix = EntryMetadataContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_ENTRY_METADATA_ID:
				typeSuffix = "item";
				subtypeSuffix = EntryMetadataContract.MIME_SUBTYPE_SUFFIX;
				break;
			default:
				throw new IllegalArgumentException("Could not match passed URI to a known path: " + uri);
		}
		return MIME_TYPE + typeSuffix + "/" + MIME_SUBTYPE + subtypeSuffix;
	}

	protected long generateKey(String table) throws OutOfKeysException {
		return keyGenerator.generateKey(table);
	}

	/**
	 * Inserts the supplied <code>values</code> under the supplied <code>uri</code>.
	 * If <code>values</code> contains an id (under <code>BaseColumns._ID</code>), it will be used,
	 * otherwise, an id will be generated. If key generation fails due to an exhausted key supply,
	 * this method will throw an <code>OutOfKeysException</code>.
	 * <p>
	 * The insert will be automatically written to the journal. If journaling fails for whatever
	 * reason, the insert will be rolled back as well, and a <code>JournalingFailedException</code>
	 * is thrown.
	 * <p>
	 * A revision number of 0 will be automatically written to the revision table for the inserted row.
	 * @throws OutOfKeysException - If the key generator could not generate a key
	 * @throws JournalingFailedException - If a record of the insert could not be written to the journal
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) throws OutOfKeysException, JournalingFailedException {
		String table = getTableFromUri(uri);
		SQLiteDatabase db = getHelper().getWritableDatabase();
		switch (URI_MATCHER.match(uri)) {
			case URI_ENTRIES_ID: 				//$FALL-THROUGH$
			case URI_CATEGORIES_ID: 			//$FALL-THROUGH$
			case URI_RULES_ID: 					//$FALL-THROUGH$
			case URI_ENTRY_METADATA_ID:
				values.put(BaseColumns._ID, uri.getLastPathSegment());
				break;
			case URI_CATEGORIES_SUBTOTALS: 		//$FALL-THROUGH$
			case URI_CATEGORIES_SUBTOTALS_ID: 	//$FALL-THROUGH$
			case URI_MONTHS: 					//$FALL_THROUGH$
			case URI_MONTHS_ID:
				throw new UnsupportedOperationException("Unsupported operation: INSERT into " + table);
		}

		// Generate a primary key for insertion using the key generator
		boolean checkKey = false;
		long key = 0;
		if (!values.containsKey(BaseColumns._ID)) {
			checkKey = true;
			key = generateKey(table);
			values.put(BaseColumns._ID, key);
		}

		// Wrap the insert and the subsequent journaling in a transaction: If journaling fails,
		// the changes will be rolled back
		db.beginTransaction();
		try {
			long rowID = db.insertOrThrow(table, null, values);
			// If no primary key had been supplied: check if insertion really happened with the generated key
			if (checkKey && rowID != key) {
				throw new IllegalStateException("Generated key was " + key + " but database inserted as " + rowID + " in table " + table);
			}
			uri = ContentUris.withAppendedId(uri, rowID);
			if (rowID > -1) {
				// Notify content observers
				getContext().getContentResolver().notifyChange(uri, null);

				// Put the insertion in the journal
				try {
					journaler.journalCreate(table, rowID);
				} catch (RemoteException x) {
					throw new JournalingFailedException(x);
				} catch (OperationApplicationException x) {
					throw new JournalingFailedException(x);
				} catch (SQLiteConstraintException x) {
					throw new JournalingFailedException(x);
				}

				// Write a revision number of 0 to the revision table for the inserted row
				ContentValues revisionValues = new ContentValues();
				revisionValues.put(RevisionTableContract.COL_NAME_TABLE, table);
				revisionValues.put(RevisionTableContract.COL_NAME_ROW, rowID);
				revisionValues.put(RevisionTableContract.COL_NAME_REVISION, 0);
				getContext().getContentResolver().insert(RevisionTableContract.CONTENT_URI, revisionValues);

				// If journal and revision table were written without error, mark transaction as a success
				db.setTransactionSuccessful();
			} else
				db.setTransactionSuccessful(); // Nothing was inserted, so nothing could have gone wrong

			return uri;
		} finally {
			db.endTransaction();
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		String table = getTableFromUri(uri);
		String groupBy = null;
		String having = null;
		SQLiteDatabase db = getHelper().getReadableDatabase();
		// If an id was supplied in the uri fragment, modify the selection accordingly
		switch (URI_MATCHER.match(uri)) {
			case URI_ENTRIES_ID:					// $FALL-THROUGH$
			case URI_CATEGORIES_ID:					// $FALL-THROUGH$
			case URI_CATEGORIES_SUBTOTALS_ID: 		// $FALL-THROUGH$
			case URI_MONTHS_ID:						// $FALL-THROUGH$
			case URI_RULES_ID:						// $FALL-THROUGH$
			case URI_ENTRY_METADATA_ID:
				if (TextUtils.isEmpty(selection))
					selection = BaseColumns._ID + "=" + uri.getLastPathSegment();
				else
					selection = "(" + selection + ") and " + BaseColumns._ID + "=" + uri.getLastPathSegment();
		}
		Cursor cursor = db.query(table, projection, selection, selectionArgs, groupBy,
				having, sortOrder, null);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	/**
	 * Performs an update using the supplied values.
	 * The update will be automatically written to the journal, entering updates for those columns
	 * that were actually changed only. If journaling fails for whatever
	 * reason, the update will be rolled back as well, and a <code>JournalingFailedException</code>
	 * is thrown.
	 * @throws JournalingFailedException - If a record of the update could not be written to the journal
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) throws JournalingFailedException {
		String table = getTableFromUri(uri);
		SQLiteDatabase db = getHelper().getWritableDatabase();
		// If an id was supplied in the uri fragment, modify the selection accordingly
		switch (URI_MATCHER.match(uri)) {
			case URI_ENTRIES_ID:					// $FALL-THROUGH$
			case URI_CATEGORIES_ID:					// $FALL-THROUGH$
			case URI_RULES_ID:						// $FALL-THROUGH$
			case URI_ENTRY_METADATA_ID:
				if (TextUtils.isEmpty(selection))
					selection = BaseColumns._ID + "=" + uri.getLastPathSegment();
				else
					selection = "(" + selection + ") and " + BaseColumns._ID + "=" + uri.getLastPathSegment();
				break;
			case URI_CATEGORIES_SUBTOTALS:			// $FALL-THROUGH$
			case URI_CATEGORIES_SUBTOTALS_ID: 		// $FALL-THROUGH$
			case URI_MONTHS:						// $FALL-THROUGH$
			case URI_MONTHS_ID:
				throw new UnsupportedOperationException("Unsupported operation: UPDATE on " + table);
		}

		// Make new ArrayList for the columns. If _id column was already included in the ContentValues
		// (which for an update should really never be the case), initialize to values.size(), otherwise
		// make one column extra for the _id
		List<String> projection = values.containsKey(BaseColumns._ID) ? new ArrayList<String>(values.size()) : new ArrayList<String>(values.size() + 1);
		// Add _id column as first column
		projection.add(0, BaseColumns._ID);
		// For every column for which there is a value present in values, add a column of the form
		// "(column_name != value) as column_name" to the projection
		for (String key: values.keySet()) {
			if (!key.equals(BaseColumns._ID))
				projection.add("(" + key + "!= \"" + values.getAsString(key) + "\") as " + key);
		}
		// Get cursor of all rows that will be affected by the update. This will give us two things:
		// The ids of all rows that will be touched by the update, we need those to pass them into the journaler
		// A table detailing whether a column was changed within a row or not.
		// Say we had this table t:
		// _id | data
		//  1  | foo
		//  2  | bar
		// Executing the query "select _id, (data != "bar") as data from t;" would give
		// _id | data
		//  1  |  1
		//  2  |  0
		// We will later use this knowledge to pass into the journaler only those columns that were actually changed by the update
		Cursor affected = db.query(table, projection.toArray(new String[0]), selection, selectionArgs, null, null, null);
		// Initialize the cursor, it seems that otherwise the update may get executed before the query (weird)
		affected.moveToFirst();

		// Wrap the update and subsequent journaling in a single transaction. That way, if the journaling fails,
		// the update will be rolled back
		db.beginTransaction();
		try {
			// Do the actual update
			int result = db.update(table, values, selection, selectionArgs);
			// Make sure there is no discrepancy between the expected and actual number of updated rows
			if (result != affected.getCount())
				throw new IllegalStateException("Expected to affect " + affected.getCount() + " rows, but found actually " + result + " rows were affected.");
			// Iterate over the cursor and write every update to the journal that actually changed the value
			affected.moveToPosition(-1);
			ArrayList<ContentProviderOperation> journalOperations = new ArrayList<ContentProviderOperation>();
			while (affected.moveToNext()) {
				long id = affected.getLong(0); // We made the _id column the first in the projection
				for (int i = 1; i < affected.getColumnCount(); i++) {
					if (affected.getInt(i) != 0) {
						journalOperations.addAll(journaler.getJournalUpdateOperation(table, id, affected.getColumnName(i)));
					}
				}
			}
			try {
				getContext().getContentResolver().applyBatch(MetaContentProvider.AUTHORITY, journalOperations);
				// If we get to here, journaling competed without errors - mark the entire transaction as successful
				db.setTransactionSuccessful();
			} catch (RemoteException x) {
				throw new JournalingFailedException(x);
			} catch (OperationApplicationException x) {
				throw new JournalingFailedException(x);
			} catch (SQLiteConstraintException x) {
				throw new JournalingFailedException(x);
			}
			if (result > 0)
				getContext().getContentResolver().notifyChange(uri, null);
			return result;
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * Performs a delete using the supplied values.
	 * The delete will be automatically written to the journal, entering updates for those columns
	 * that were actually changed only. If journaling fails for whatever
	 * reason, the delete will be rolled back as well, and a <code>JournalingFailedException</code>
	 * is thrown.
	 * @throws JournalingFailedException - If a record of the delete could not be written to the journal
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) throws JournalingFailedException {
		String table = getTableFromUri(uri);
		// If an exact row was identified through the URI fragment, this will hold its id
		Long id = null;
		SQLiteDatabase db = getHelper().getWritableDatabase();
		// If an id was supplied in the URI fragment, modify the selection accordingly
		switch (URI_MATCHER.match(uri)) {
			case URI_ENTRIES_ID:					// $FALL-THROUGH$
			case URI_CATEGORIES_ID:					// $FALL-THROUGH$
			case URI_RULES_ID:						// $FALL-THROUGH$
			case URI_ENTRY_METADATA_ID:
				id = Long.valueOf(uri.getLastPathSegment());
				if (TextUtils.isEmpty(selection))
					selection = BaseColumns._ID + "=" + id;
				else
					selection = "(" + selection + ") and " + BaseColumns._ID + "=" + id;
				break;
			case URI_CATEGORIES_SUBTOTALS:			// $FALL-THROUGH$
			case URI_CATEGORIES_SUBTOTALS_ID: 		// $FALL-THROUGH$
			case URI_MONTHS:						// $FALL-THROUGH$
			case URI_MONTHS_ID:
				throw new UnsupportedOperationException("Unsupported operation: DELETE from " + table);
		}

		List<Long> ids = new LinkedList<Long>();
		int result;

		// Wrap the delete and the subsequent journaling in a single transaction. That way, if journaling fails,
		// the delete will be rolled back
		db.beginTransaction();
		try {
			// Was this a simple delete of one row identified by its id? If yes, that makes our life easier
			// While not strictly necessary, this is probably the most frequent use case, so for performance reasons,
			// it gets an optimized code path working on scalars instead of lists.
			if (id != null) {
				// Perform the delete...
				result = db.delete(table, selection, selectionArgs);
				// ...and if it went through, put the id in the list of affected ids
				if (result > 0) {
					ids.add(id);
				}
			} else {
				// There wasn't one precisely identified row by id, get a cursor of all the rows that will be
				// affected by running a query with the same selection
				Cursor affected = query(uri, new String[] { BaseColumns._ID }, selection, selectionArgs, null);
				result = db.delete(table, selection, selectionArgs);
				// Make sure there is no discrepancy between the expected and actual number of deleted rows
				if (result != affected.getCount())
					throw new IllegalStateException("Expected to affect " + affected.getCount() + " rows, but found actually " + result + " rows were affected.");
				// Add the id of all affected rows to ids
				affected.moveToPosition(-1);
				while (affected.moveToNext())
					ids.add(affected.getLong(0)); // We projected to only the _id column
			}
			if (result > 0) {
				getContext().getContentResolver().notifyChange(uri, null);
				// Journal deletions for all affected rows
				ArrayList<ContentProviderOperation> journalOperations = new ArrayList<ContentProviderOperation>();
				for (Long i: ids)
					journalOperations.addAll(journaler.getJournalDeleteOperation(table, i));

				try {
					getContext().getContentResolver().applyBatch(MetaContentProvider.AUTHORITY, journalOperations);
					db.setTransactionSuccessful();
				} catch (RemoteException x) {
					throw new JournalingFailedException(x);
				} catch (OperationApplicationException x) {
					throw new JournalingFailedException(x);
				} catch (SQLiteConstraintException x) {
					throw new JournalingFailedException(x);
				}
			} else
				db.setTransactionSuccessful(); // Nothing was done, so nothing could have gone wrong
			return result;
		} finally {
				db.endTransaction();
		}
	}
}
