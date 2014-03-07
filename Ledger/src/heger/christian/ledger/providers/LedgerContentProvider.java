package heger.christian.ledger.providers;

import heger.christian.ledger.db.LedgerDbHelper;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class LedgerContentProvider extends ContentProvider {
	public static final String AUTHORITY = "heger.christian.ledger.providers.ledgercontentprovider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	
	public static final int URI_ENTRIES = 10;
	public static final int URI_ENTRIES_ID = 11;
	public static final int URI_CATEGORIES = 20;
	public static final int URI_CATEGORIES_ID = 21;
	public static final int URI_CATEGORIES_SUBTOTALS = 25;
	public static final int URI_CATEGORIES_SUBTOTALS_ID = 26;
	public static final int URI_SUPERCATEGORIES = 30;
	public static final int URI_SUPERCATEGORIES_ID = 31;
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
		URI_MATCHER.addURI(AUTHORITY, SupercategoryContract.TABLE_NAME, URI_SUPERCATEGORIES);
		URI_MATCHER.addURI(AUTHORITY, SupercategoryContract.TABLE_NAME + "/#", URI_SUPERCATEGORIES_ID);
		URI_MATCHER.addURI(AUTHORITY, MonthsContract.TABLE_NAME, URI_MONTHS);
		URI_MATCHER.addURI(AUTHORITY, MonthsContract.TABLE_NAME + "/#", URI_MONTHS_ID);
		URI_MATCHER.addURI(AUTHORITY, RulesContract.TABLE_NAME, URI_RULES);
		URI_MATCHER.addURI(AUTHORITY, RulesContract.TABLE_NAME + "/#", URI_RULES_ID);
		URI_MATCHER.addURI(AUTHORITY, EntryMetadataContract.TABLE_NAME, URI_ENTRY_METADATA);
		URI_MATCHER.addURI(AUTHORITY, EntryMetadataContract.TABLE_NAME + "/#", URI_ENTRY_METADATA_ID);
	}
	public static final String MIME_TYPE = "vnd.android.cursor";
	public static final String MIME_SUBTYPE = "vnd.heger.christian.ledger.provider";

	private SQLiteOpenHelper dbHelper;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		String table;
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (URI_MATCHER.match(uri)) {
			case URI_ENTRIES:
				table = EntryContract.mapToDBContract(EntryContract.TABLE_NAME);
				break;
			case URI_ENTRIES_ID:
				table = EntryContract.mapToDBContract(EntryContract.TABLE_NAME);
				selection = EntryContract.mapToDBContract(EntryContract._ID) + "=" + uri.getLastPathSegment();
				break;
			case URI_CATEGORIES:
				table = CategoryContract.mapToDBContract(CategoryContract.TABLE_NAME);
				break;
			case URI_CATEGORIES_ID:
				table = CategoryContract.mapToDBContract(CategoryContract.TABLE_NAME);
				selection = CategoryContract.mapToDBContract(CategoryContract._ID) + "=" + uri.getLastPathSegment();
				break;
			case URI_CATEGORIES_SUBTOTALS: //$FALL-THROUGH$
			case URI_CATEGORIES_SUBTOTALS_ID:
				table = CategorySubtotalsContract.TABLE_NAME;
				throw new UnsupportedOperationException("Unsupported operation: DELETE in table " + table);
			case URI_SUPERCATEGORIES:
				table = SupercategoryContract.mapToDBContract(SupercategoryContract.TABLE_NAME);
				break;
			case URI_SUPERCATEGORIES_ID:
				table = SupercategoryContract.mapToDBContract(SupercategoryContract.TABLE_NAME);
				selection = SupercategoryContract.mapToDBContract(SupercategoryContract._ID) + "=" + uri.getLastPathSegment();
				break;
			case URI_MONTHS:
				throw new UnsupportedOperationException("Write access to view " + MonthsContract.TABLE_NAME + " is not supported.");
			case URI_MONTHS_ID:
				throw new UnsupportedOperationException("Write access to view " + MonthsContract.TABLE_NAME + " is not supported.");
			case URI_RULES:
				table = RulesContract.TABLE_NAME;
				break;
			case URI_RULES_ID:
				table = RulesContract.TABLE_NAME;
				selection = RulesContract._ID + "=" + uri.getLastPathSegment();
				break;
			case URI_ENTRY_METADATA:
				table = EntryMetadataContract.TABLE_NAME;
				break;
			case URI_ENTRY_METADATA_ID:
				table = EntryMetadataContract.TABLE_NAME;
				selection = EntryMetadataContract._ID + "=" + uri.getLastPathSegment();
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
			case URI_SUPERCATEGORIES:
				typeSuffix = "dir";
				subtypeSuffix = SupercategoryContract.MIME_SUBTYPE_SUFFIX;
				break;
			case URI_SUPERCATEGORIES_ID:
				typeSuffix = "item";
				subtypeSuffix = SupercategoryContract.MIME_SUBTYPE_SUFFIX;
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

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String table;
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (URI_MATCHER.match(uri)) {
			case URI_ENTRIES:
				table = EntryContract.mapToDBContract(EntryContract.TABLE_NAME);
				break;
			case URI_ENTRIES_ID:
				throw new IllegalArgumentException("Illegal URI for insertion: " + uri + ". Must not contain a row id.");
			case URI_CATEGORIES:
				table = CategoryContract.mapToDBContract(CategoryContract.TABLE_NAME);
				break;
			case URI_CATEGORIES_ID:
				throw new IllegalArgumentException("Illegal URI for insertion: " + uri + ". Must not contain a row id.");
			case URI_CATEGORIES_SUBTOTALS: //$FALL-THROUGH$
			case URI_CATEGORIES_SUBTOTALS_ID:
				table = CategorySubtotalsContract.TABLE_NAME;
				throw new UnsupportedOperationException("Unsupported operation: INSERT in table " + table);
			case URI_SUPERCATEGORIES:
				table = SupercategoryContract.mapToDBContract(SupercategoryContract.TABLE_NAME);
				break;
			case URI_SUPERCATEGORIES_ID:
				throw new IllegalArgumentException("Illegal URI for insertion: " + uri + ". Must not contain a row id.");
			case URI_MONTHS:
				throw new UnsupportedOperationException("Write access to view " + MonthsContract.TABLE_NAME + " is not supported.");
			case URI_MONTHS_ID:
				throw new UnsupportedOperationException("Write access to view " + MonthsContract.TABLE_NAME + " is not supported.");
			case URI_RULES:
				table = RulesContract.TABLE_NAME;
				break;
			case URI_RULES_ID:
				throw new IllegalArgumentException("Illegal URI for insertion: " + uri + ". Must not contain a row id.");
			case URI_ENTRY_METADATA:
				table = EntryMetadataContract.TABLE_NAME;
				break;
			case URI_ENTRY_METADATA_ID:
				throw new IllegalArgumentException("Illegal URI for insertion: " + uri + ". Must not contain a row id.");
			default:
				throw new IllegalArgumentException("Could not match passed URI to a known path: " + uri);
		}
		long rowID = db.insertOrThrow(table, null, values);
		uri = ContentUris.withAppendedId(uri, rowID);
		if (rowID > -1)
			getContext().getContentResolver().notifyChange(uri, null);
		// If we get to here, the insertion was successful
		return uri;
	}

	@Override
	public boolean onCreate() {
		/*
		 * Creates a new helper object. This method always returns quickly.
		 * Notice that the database itself isn't created or opened until
		 * dbHelper.getWritableDatabase is called
		 */
		dbHelper = new LedgerDbHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		String tables = "";
		String groupBy = null;
		String having = null;
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		// Most queries, i.e. the ones corresponding directly to database tables,
		// are simply handed through to the database.
		// The same applies to queries to the above mentioned databases
		// when an ID is appended to the uri, in which case the query is
		// handed through, but the selection is modified to select the
		// appropriate row.
		switch (URI_MATCHER.match(uri)) {
			case URI_ENTRIES:
				tables = EntryContract
						.mapToDBContract(EntryContract.TABLE_NAME);
				break;
			case URI_ENTRIES_ID:
				tables = EntryContract
						.mapToDBContract(EntryContract.TABLE_NAME);
				selection = EntryContract.mapToDBContract(EntryContract._ID)
						+ " = " + uri.getLastPathSegment();
				break;
			case URI_CATEGORIES:
				tables = CategoryContract
						.mapToDBContract(CategoryContract.TABLE_NAME);
				break;
			case URI_CATEGORIES_ID:
				tables = CategoryContract
						.mapToDBContract(CategoryContract.TABLE_NAME);
				selection = CategoryContract
						.mapToDBContract(CategoryContract._ID)
						+ "="
						+ uri.getLastPathSegment();
				break;
			case URI_CATEGORIES_SUBTOTALS: {
				String month = uri.getQueryParameter(CategorySubtotalsContract.QUERY_ARG_MONTH);
				tables = CategorySubtotalsContract.generateSQL(month);
				break; }
			case URI_CATEGORIES_SUBTOTALS_ID: {
				String month = uri.getQueryParameter(CategorySubtotalsContract.QUERY_ARG_MONTH);
				tables = CategorySubtotalsContract.generateSQL(month);
				selection = CategorySubtotalsContract._ID + "=" + uri.getLastPathSegment();
				break; }
			case URI_SUPERCATEGORIES:
				tables = SupercategoryContract
						.mapToDBContract(SupercategoryContract.TABLE_NAME);
				break;
			case URI_SUPERCATEGORIES_ID:
				tables = SupercategoryContract
						.mapToDBContract(SupercategoryContract.TABLE_NAME);
				selection = SupercategoryContract
						.mapToDBContract(SupercategoryContract._ID)
						+ "="
						+ uri.getLastPathSegment();
				break;
			case URI_MONTHS:
				tables = MonthsContract.TABLE_NAME;
				break;
			case URI_MONTHS_ID:
				tables = MonthsContract.TABLE_NAME;
				selection = MonthsContract._ID + "=" + uri.getLastPathSegment();
				break;
			case URI_RULES:
				tables = RulesContract.TABLE_NAME;
				break;
			case URI_RULES_ID:
				tables = RulesContract.TABLE_NAME;
				selection = RulesContract._ID + "=" + uri.getLastPathSegment();
				break;
			case URI_ENTRY_METADATA:
				tables = EntryMetadataContract.TABLE_NAME;
				break;
			case URI_ENTRY_METADATA_ID:
				tables = EntryMetadataContract.TABLE_NAME;
				selection = EntryMetadataContract._ID + "=" + uri.getLastPathSegment();
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
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		String table;
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (URI_MATCHER.match(uri)) {
			case URI_ENTRIES:
				table = EntryContract.mapToDBContract(EntryContract.TABLE_NAME);
				break;
			case URI_ENTRIES_ID:
				table = EntryContract.mapToDBContract(EntryContract.TABLE_NAME);
				selection = EntryContract.mapToDBContract(EntryContract._ID)
						+ "=" + uri.getLastPathSegment();
				break;
			case URI_CATEGORIES:
				table = CategoryContract
						.mapToDBContract(CategoryContract.TABLE_NAME);
				break;
			case URI_CATEGORIES_ID:
				table = CategoryContract
						.mapToDBContract(CategoryContract.TABLE_NAME);
				selection = CategoryContract
						.mapToDBContract(CategoryContract._ID)
						+ "="
						+ uri.getLastPathSegment();
				break;
			case URI_CATEGORIES_SUBTOTALS: //$FALL-THROUGH$
			case URI_CATEGORIES_SUBTOTALS_ID:
				table = CategorySubtotalsContract.TABLE_NAME;
				throw new UnsupportedOperationException("Unsupported operation: DELETE in table " + table);
			case URI_SUPERCATEGORIES:
				table = SupercategoryContract.mapToDBContract(SupercategoryContract.TABLE_NAME);
				break;
			case URI_SUPERCATEGORIES_ID:
				table = SupercategoryContract.mapToDBContract(SupercategoryContract.TABLE_NAME);
				selection = SupercategoryContract.mapToDBContract(SupercategoryContract._ID)
						+ "="
						+ uri.getLastPathSegment();
				break;
			case URI_MONTHS:
				throw new UnsupportedOperationException(
						"Write access to view " + MonthsContract.TABLE_NAME + " is not supported.");
			case URI_MONTHS_ID:
				throw new UnsupportedOperationException(
						"Write access to view " + MonthsContract.TABLE_NAME + " is not supported.");
			case URI_RULES:
				table = RulesContract.TABLE_NAME;
				break;
			case URI_RULES_ID:
				table = RulesContract.TABLE_NAME;
				selection = RulesContract._ID + "=" + uri.getLastPathSegment();
				break;
			case URI_ENTRY_METADATA:
				table = EntryMetadataContract.TABLE_NAME;
				break;
			case URI_ENTRY_METADATA_ID:
				table = EntryMetadataContract.TABLE_NAME;
				selection = EntryMetadataContract._ID + "=" + uri.getLastPathSegment();
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
