package heger.christian.checkbook.sync;

import heger.christian.checkbook.db.CheckbookDbHelper.CategoryContract;
import heger.christian.checkbook.providers.CheckbookContentProvider;
import heger.christian.checkbook.providers.MetaContentProvider;
import heger.christian.checkbook.providers.MetaContentProvider.RevisionTableContract;
import heger.christian.checkbook.providers.RuleContract;
import heger.christian.checkbook.providers.SharedTransaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.SyncStats;
import android.database.Cursor;
import android.test.ProviderTestCase2;

public class UnmarshallerTest extends ProviderTestCase2<CheckbookContentProvider> {
	private static final long CATEGORY_ID = 1;
	private static final String CATEGORY_CAPTION = "category_caption";
	private static final int CATEGORY_REVISION = 1;

	private static final long CATEGORY_ID2 = 2;
	private static final String CATEGORY_CAPTION2 = CATEGORY_CAPTION + "2";
	private static final int CATEGORY_REVISION2 = 2;

	private static final long RULE_ID = 10;
	private static final String RULE_CAPTION = "rule_caption";
	private static final long RULE_CATEGORY = CATEGORY_ID;
	private static final String RULE_COLUMN = RuleContract.COL_NAME_CONSEQUENT;
	private static final int RULE_REVISION = 2;

	private static final long RULE_ID2 = 11;
	private static final String RULE_CAPTION2 = RULE_CAPTION + "2";
	private static final long RULE_CATEGORY2 = CATEGORY_ID2;

	MetaContentProvider meta;
	JSONObject json;

	public UnmarshallerTest() {
		super(CheckbookContentProvider.class, CheckbookContentProvider.AUTHORITY);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		meta = new MetaContentProvider();
		meta.attachInfo(getMockContext(), null);
		getMockContentResolver().addProvider(MetaContentProvider.AUTHORITY, meta);

		json = new JSONObject();
		final String[] FIELDS = new String[] { Unmarshaller.JSON_FIELD_CREATED, Unmarshaller.JSON_FIELD_UPDATED, Unmarshaller.JSON_FIELD_DELETED };
		for (int i = 0; i < 3; i++) {
			json.put(FIELDS[i], new JSONArray());
		}
		json.put(Unmarshaller.JSON_FIELD_ANCHOR, 0);
	}

	@Override
	protected void tearDown() throws Exception {
		meta.shutdown();
	}

	public void testUnmarshalCreations() throws JSONException, OperationApplicationException {
		JSONArray created = json.getJSONArray(Unmarshaller.JSON_FIELD_CREATED);

		JSONObject cat1 = new JSONObject();
		cat1.put(OperationFactory.JSON_FIELD_TABLE, CategoryContract.TABLE_NAME);
		cat1.put(OperationFactory.JSON_FIELD_ROW, CATEGORY_ID);
		Map<String, Integer> cat1Revisions = new HashMap<String, Integer>();
		cat1Revisions.put(heger.christian.checkbook.providers.CategoryContract._ID, CATEGORY_REVISION);
		cat1Revisions.put(heger.christian.checkbook.providers.CategoryContract.COL_NAME_CAPTION, CATEGORY_REVISION);
		cat1.put(OperationFactory.JSON_FIELD_REVISIONS, new JSONObject(cat1Revisions));
		JSONObject cat1Data = new JSONObject();
		cat1Data.put(CategoryContract._ID, CATEGORY_ID);
		cat1Data.put(CategoryContract.COL_NAME_CAPTION, CATEGORY_CAPTION);
		cat1.put(OperationFactory.JSON_FIELD_DATA, cat1Data);
		created.put(cat1);

		JSONObject cat2 = new JSONObject();
		cat2.put(OperationFactory.JSON_FIELD_TABLE, CategoryContract.TABLE_NAME);
		cat2.put(OperationFactory.JSON_FIELD_ROW, CATEGORY_ID2);
		Map<String, Integer> cat2Revisions = new HashMap<String, Integer>();
		cat2Revisions.put(heger.christian.checkbook.providers.CategoryContract._ID, CATEGORY_REVISION2);
		cat2Revisions.put(heger.christian.checkbook.providers.CategoryContract.COL_NAME_CAPTION, CATEGORY_REVISION2);
		cat2.put(OperationFactory.JSON_FIELD_REVISIONS, new JSONObject(cat2Revisions));
//		cat2.put(OperationFactory.JSON_FIELD_REVISION, CATEGORY_REVISION2);
		JSONObject cat2Data = new JSONObject();
		cat2Data.put(CategoryContract._ID, CATEGORY_ID2);
		cat2Data.put(CategoryContract.COL_NAME_CAPTION, CATEGORY_CAPTION2);
		cat2.put(OperationFactory.JSON_FIELD_DATA, cat2Data);
		created.put(cat2);

		Unmarshaller unmarshaller = new Unmarshaller();
		List<ContentProviderOperation> operations = unmarshaller.unmarshal(json);
		// Disable journaling / revision keeping
		((CheckbookContentProvider) getMockContentResolver().acquireContentProviderClient(CheckbookContentProvider.AUTHORITY).getLocalContentProvider()).setJournaling(false);
		SharedTransaction transaction = SharedTransaction.newInstance(getMockContext());
		transaction.applyBatch(operations);

		ContentResolver resolver = getMockContentResolver();
		Cursor cursor = resolver.query(CategoryContract.CONTENT_URI, null, null, null, CategoryContract._ID);
		assertEquals(2, cursor.getCount());
		cursor.moveToFirst();
		assertEquals(CATEGORY_ID, cursor.getLong(cursor.getColumnIndex(CategoryContract._ID)));
		assertEquals(CATEGORY_CAPTION, cursor.getString(cursor.getColumnIndex(CategoryContract.COL_NAME_CAPTION)));

		cursor.moveToNext();
		assertEquals(CATEGORY_ID2, cursor.getLong(cursor.getColumnIndex(CategoryContract._ID)));
		assertEquals(CATEGORY_CAPTION2, cursor.getString(cursor.getColumnIndex(CategoryContract.COL_NAME_CAPTION)));

		cursor = resolver.query(RevisionTableContract.CONTENT_URI, null, null, null, null);
		assertEquals(4, cursor.getCount());

		RevisionTableSnapshot revisionTableSnapshot = RevisionTableSnapshot.createFromCursor(cursor);
		assertEquals(CATEGORY_REVISION, revisionTableSnapshot.getRevision(CategoryContract.TABLE_NAME, CATEGORY_ID, CategoryContract._ID));
		assertEquals(CATEGORY_REVISION, revisionTableSnapshot.getRevision(CategoryContract.TABLE_NAME, CATEGORY_ID, CategoryContract.COL_NAME_CAPTION));
		assertEquals(CATEGORY_REVISION2, revisionTableSnapshot.getRevision(CategoryContract.TABLE_NAME, CATEGORY_ID2, CategoryContract._ID));
		assertEquals(CATEGORY_REVISION2, revisionTableSnapshot.getRevision(CategoryContract.TABLE_NAME, CATEGORY_ID2, CategoryContract.COL_NAME_CAPTION));

		SyncStats stats = unmarshaller.getStats();
		assertEquals(0, stats.numSkippedEntries);
		assertEquals(0, stats.numParseExceptions);
		assertEquals(2, stats.numInserts);
		assertEquals(0, stats.numUpdates);
		assertEquals(0, stats.numDeletes);
	}

	public void testUnmarshalUpdates() throws JSONException, OperationApplicationException {
		ContentResolver resolver = getMockContentResolver();
		{
			ContentValues cat1 = new ContentValues();
			cat1.put(CategoryContract._ID, CATEGORY_ID);
			cat1.put(CategoryContract.COL_NAME_CAPTION, CATEGORY_CAPTION);
			resolver.insert(CategoryContract.CONTENT_URI, cat1);
			ContentValues cat2 = new ContentValues();
			cat2.put(CategoryContract._ID, CATEGORY_ID2);
			cat2.put(CategoryContract.COL_NAME_CAPTION, CATEGORY_CAPTION2);
			resolver.insert(CategoryContract.CONTENT_URI, cat2);
			ContentValues r1 = new ContentValues();
			r1.put(RuleContract._ID, RULE_ID);
			r1.put(RuleContract.COL_NAME_ANTECEDENT, RULE_CAPTION);
			r1.put(RuleContract.COL_NAME_CONSEQUENT, RULE_CATEGORY);
			resolver.insert(RuleContract.CONTENT_URI, r1);
		}

		JSONArray updated = json.getJSONArray(Unmarshaller.JSON_FIELD_UPDATED);
		JSONObject r1 = new JSONObject();
		r1.put(OperationFactory.JSON_FIELD_TABLE, RuleContract.TABLE_NAME);
		r1.put(OperationFactory.JSON_FIELD_ROW, RULE_ID);
		r1.put(OperationFactory.JSON_FIELD_COLUMN, RuleContract.COL_NAME_CONSEQUENT);

		Map<String, Integer> r1Revisions = new HashMap<String,Integer>();
		r1Revisions.put(RuleContract.COL_NAME_CONSEQUENT, RULE_REVISION);
		r1.put(OperationFactory.JSON_FIELD_REVISIONS, new JSONObject(r1Revisions));
//		r1.put(OperationFactory.JSON_FIELD_REVISION, RULE_REVISION);
		JSONObject r1Data = new JSONObject();
		r1Data.put(RuleContract.COL_NAME_CONSEQUENT, CATEGORY_ID2);
		r1.put(OperationFactory.JSON_FIELD_DATA, r1Data);
		updated.put(r1);

		Unmarshaller unmarshaller = new Unmarshaller();
		List<ContentProviderOperation> operations = unmarshaller.unmarshal(json);
		// Disable journaling / revision keeping
		((CheckbookContentProvider) resolver.acquireContentProviderClient(CheckbookContentProvider.AUTHORITY).getLocalContentProvider()).setJournaling(false);
		SharedTransaction transaction = SharedTransaction.newInstance(getMockContext());
		transaction.applyBatch(operations);

		Cursor cursor = resolver.query(RuleContract.CONTENT_URI, null, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.moveToFirst();
		assertEquals(RULE_ID, cursor.getLong(cursor.getColumnIndex(RuleContract._ID)));
		assertEquals(RULE_CAPTION, cursor.getString(cursor.getColumnIndex(RuleContract.COL_NAME_ANTECEDENT)));
		assertEquals(RULE_CATEGORY2, cursor.getLong(cursor.getColumnIndex(RuleContract.COL_NAME_CONSEQUENT)));

		cursor = resolver.query(RevisionTableContract.CONTENT_URI,
				null,
				RevisionTableContract.COL_NAME_TABLE + "=?",
				new String[] { RuleContract.TABLE_NAME },
				null);

		assertEquals(3, cursor.getCount());
		RevisionTableSnapshot revisionTableSnapshot = RevisionTableSnapshot.createFromCursor(cursor);
		assertEquals(0, revisionTableSnapshot.getRevision(RuleContract.TABLE_NAME, RULE_ID, RuleContract._ID));
		assertEquals(0, revisionTableSnapshot.getRevision(RuleContract.TABLE_NAME, RULE_ID, RuleContract.COL_NAME_ANTECEDENT));
		assertEquals(RULE_REVISION, revisionTableSnapshot.getRevision(RuleContract.TABLE_NAME, RULE_ID, RuleContract.COL_NAME_CONSEQUENT));

		SyncStats stats = unmarshaller.getStats();
		assertEquals(0, stats.numSkippedEntries);
		assertEquals(0, stats.numParseExceptions);
		assertEquals(0, stats.numInserts);
		assertEquals(1, stats.numUpdates);
		assertEquals(0, stats.numDeletes);
	}

	public void testUnmarshalDeletions() throws JSONException, OperationApplicationException {
		ContentResolver resolver = getMockContentResolver();
		JSONArray deleted = json.getJSONArray(Unmarshaller.JSON_FIELD_DELETED);

		{
			ContentValues values = new ContentValues();
			values.put(CategoryContract._ID, CATEGORY_ID);
			values.put(CategoryContract.COL_NAME_CAPTION, CATEGORY_CAPTION);
			resolver.insert(CategoryContract.CONTENT_URI, values);

			values = new ContentValues();
			values.put(CategoryContract._ID, CATEGORY_ID2);
			values.put(CategoryContract.COL_NAME_CAPTION, CATEGORY_CAPTION2);
			resolver.insert(CategoryContract.CONTENT_URI, values);
		}

		JSONObject cat1 = new JSONObject();
		cat1.put(OperationFactory.JSON_FIELD_TABLE, CategoryContract.TABLE_NAME);
		cat1.put(OperationFactory.JSON_FIELD_ROW, CATEGORY_ID2);
		Map<String, Integer> cat1Revisions = new HashMap<String,Integer>();
		cat1Revisions.put(heger.christian.checkbook.providers.CategoryContract._ID, CATEGORY_REVISION2);
		cat1Revisions.put(heger.christian.checkbook.providers.CategoryContract.COL_NAME_CAPTION, CATEGORY_REVISION2);
		cat1.put(OperationFactory.JSON_FIELD_REVISIONS, new JSONObject(cat1Revisions));
//		cat1.put(OperationFactory.JSON_FIELD_REVISION, CATEGORY_REVISION2);
		deleted.put(cat1);

		Unmarshaller unmarshaller = new Unmarshaller();
		List<ContentProviderOperation> operations = unmarshaller.unmarshal(json);
		// Disable journaling / revision keeping
		((CheckbookContentProvider) resolver.acquireContentProviderClient(CheckbookContentProvider.AUTHORITY).getLocalContentProvider()).setJournaling(false);
		SharedTransaction transaction = SharedTransaction.newInstance(getMockContext());
		transaction.applyBatch(operations);

		// Only one category should be left now
		Cursor cursor = resolver.query(CategoryContract.CONTENT_URI, null, null, null, CategoryContract._ID);
		assertEquals(1, cursor.getCount());
		cursor.moveToFirst();
		assertEquals(CATEGORY_ID, cursor.getLong(cursor.getColumnIndex(CategoryContract._ID)));
		assertEquals(CATEGORY_CAPTION, cursor.getString(cursor.getColumnIndex(CategoryContract.COL_NAME_CAPTION)));

		cursor = resolver.query(RevisionTableContract.CONTENT_URI,
				null,
				null,
				null,
				null);
		assertEquals(4, cursor.getCount());

		RevisionTableSnapshot revisions = RevisionTableSnapshot.createFromCursor(cursor);
		assertEquals(0, revisions.getRevision(CategoryContract.TABLE_NAME, CATEGORY_ID, CategoryContract._ID));
		assertEquals(0, revisions.getRevision(CategoryContract.TABLE_NAME, CATEGORY_ID, CategoryContract.COL_NAME_CAPTION));
		assertEquals(CATEGORY_REVISION2, revisions.getRevision(CategoryContract.TABLE_NAME, CATEGORY_ID2, CategoryContract._ID));
		assertEquals(CATEGORY_REVISION2, revisions.getRevision(CategoryContract.TABLE_NAME, CATEGORY_ID2, CategoryContract.COL_NAME_CAPTION));

		SyncStats stats = unmarshaller.getStats();
		assertEquals(0, stats.numSkippedEntries);
		assertEquals(0, stats.numParseExceptions);
		assertEquals(0, stats.numInserts);
		assertEquals(0, stats.numUpdates);
		assertEquals(1, stats.numDeletes);
	}
}
