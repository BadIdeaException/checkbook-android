package heger.christian.checkbook.sync;

import heger.christian.checkbook.db.CheckbookDbHelper.KeyGenerationContract;
import heger.christian.checkbook.providers.CategoryContract;
import heger.christian.checkbook.providers.Journaler;
import heger.christian.checkbook.providers.CheckbookContentProvider;
import heger.christian.checkbook.providers.MetaContentProvider;
import heger.christian.checkbook.providers.MetaContentProvider.JournalContract;
import heger.christian.checkbook.providers.MetaContentProvider.RevisionTableContract;
import heger.christian.checkbook.providers.RuleContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.test.ProviderTestCase2;

public class MarshallerTest extends ProviderTestCase2<CheckbookContentProvider> {
	private static final long CATEGORY_ID = 1;
	private static final String CATEGORY_CAPTION = "category_caption";

	private static final long CATEGORY_ID2 = 2;
	private static final String CATEGORY_CAPTION2 = CATEGORY_CAPTION + "2";

	private static final long RULE_ID = 10;
	private static final String RULE_CAPTION = "rule_caption";
	private static final long RULE_CATEGORY = CATEGORY_ID;

	private static final long RULE_ID2 = 11;
	private static final String RULE_CAPTION2 = RULE_CAPTION + "2";
	private static final long RULE_CATEGORY2 = CATEGORY_ID2;

	/**
	 * Mock journaler that won't perform any optimizations.
	 */
	protected class MockJournaler extends Journaler {
		public MockJournaler(ContentResolver resolver) {
			super(resolver);
		}
		@Override
		protected OptimizationOperations getUpdateOptimizations(String table, long id, String column) {
			return new OptimizationOperations();
		}
		@Override
		protected OptimizationOperations getDeleteOptimizations(String table, long id) {
			return new OptimizationOperations();
		}
	}

	private MetaContentProvider metaProvider;
	private ContentValues cat1Values, cat2Values, r1Values, r2Values, r1ValuesUpdated, r2ValuesUpdated;
	@SuppressWarnings("unused")
	private Uri cat1;
	private Uri cat2;
	private Uri r1;
	private Uri r2;

	public MarshallerTest() {
		super(CheckbookContentProvider.class, CheckbookContentProvider.AUTHORITY);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		metaProvider = new MetaContentProvider();
		metaProvider.attachInfo(getMockContext(), null);
		getMockContentResolver().addProvider(MetaContentProvider.AUTHORITY, metaProvider);

		// Set mock journaler to prevent optimizations
		getProvider().setJournaler(new MockJournaler(getMockContentResolver()));

		// Keep key generator from failing
		// This is not strictly necessary as we're inserting with set primary keys
		// But it doesn't hurt, either
		ContentValues values = new ContentValues();
		values.put(KeyGenerationContract.COL_NAME_NEXT_KEY, 0);
		values.put(KeyGenerationContract.COL_NAME_UPPER_BOUND, 100);
	}

	protected void writeCreations() {
		ContentResolver resolver = getMockContentResolver();

		cat1Values = new ContentValues();
		cat1Values.put(CategoryContract._ID, CATEGORY_ID);
		cat1Values.put(CategoryContract.COL_NAME_CAPTION, CATEGORY_CAPTION);
		cat1 = resolver.insert(CategoryContract.CONTENT_URI, cat1Values);

		cat2Values = new ContentValues();
		cat2Values.put(CategoryContract._ID, CATEGORY_ID2);
		cat2Values.put(CategoryContract.COL_NAME_CAPTION, CATEGORY_CAPTION2);
		cat2 = resolver.insert(CategoryContract.CONTENT_URI, cat2Values);

		r1Values = new ContentValues();
		r1Values.put(RuleContract._ID, RULE_ID);
		r1Values.put(RuleContract.COL_NAME_ANTECEDENT, RULE_CAPTION);
		r1Values.put(RuleContract.COL_NAME_CONSEQUENT, RULE_CATEGORY);
		r1 = resolver.insert(RuleContract.CONTENT_URI, r1Values);

		r2Values = new ContentValues();
		r2Values.put(RuleContract._ID, RULE_ID2);
		r2Values.put(RuleContract.COL_NAME_ANTECEDENT, RULE_CAPTION2);
		r2Values.put(RuleContract.COL_NAME_CONSEQUENT, RULE_CATEGORY2);
		r2 = resolver.insert(RuleContract.CONTENT_URI, r2Values);
	}
	protected void writeUpdates() {
		ContentResolver resolver = getMockContentResolver();
		r1ValuesUpdated = new ContentValues();
		r1ValuesUpdated.put(RuleContract.COL_NAME_ANTECEDENT, RULE_CAPTION2);
		resolver.update(r1, r1ValuesUpdated, null, null);

		r2ValuesUpdated = new ContentValues();
		r2ValuesUpdated.put(RuleContract.COL_NAME_ANTECEDENT, RULE_CAPTION);
		r2ValuesUpdated.put(RuleContract.COL_NAME_CONSEQUENT, RULE_CATEGORY);
		resolver.update(r2, r2ValuesUpdated, null, null);
	}
	protected void writeDeletions() {
		ContentResolver resolver = getMockContentResolver();
		resolver.delete(cat2, null, null);
		resolver.delete(r2, null, null);
	}

	@Override
	protected void tearDown() throws Exception {
		metaProvider.shutdown();
		super.tearDown();
	}

	// Compare all the values in v1 and v2, but consider Long and Integer to be equal if their values are equal
	private boolean compareContentValues(ContentValues v1, ContentValues v2) {
		if (v1.size() != v2.size())
			return false;
		boolean result = true;
		for (String key: v1.keySet()) {
			Object value1 = v1.get(key);
			Object value2 = v2.get(key);
			if (value1 instanceof Integer && value2 instanceof Long)
				value1 = Long.valueOf((Integer) value1);
			if (value1 instanceof Long && value2 instanceof Integer)
				value2 = Long.valueOf((Integer) value2);
			result &= v1 == null && v2== null || value1.equals(value2);
		}
		return result;
	}

	public void testMarshalCreations() throws JSONException {
		Marshaller marshaller = new Marshaller();
		writeCreations();
		JournalSnapshot journal = JournalSnapshot.createFromCursor(getMockContentResolver().query(JournalContract.CONTENT_URI, null, null, null, null));
		RevisionTableSnapshot revisions = RevisionTableSnapshot.createFromCursor(getMockContentResolver().query(RevisionTableContract.CONTENT_URI, null, null, null, null));
		Translator translator = new Translator();

		ContentProviderClient provider = getMockContentResolver().acquireContentProviderClient(CheckbookContentProvider.AUTHORITY);
		JSONObject json = marshaller.marshal(journal, revisions, provider, 0);

		assertEquals(0, json.getInt(Marshaller.JSON_FIELD_ANCHOR));

		JSONArray array = json.getJSONArray(Marshaller.JSON_FIELD_CREATED);
		assertEquals(4, array.length());
		ContentValues[] values = new ContentValues[] { cat1Values, cat2Values, r1Values, r2Values};
		long[] ids = new long[] { CATEGORY_ID, CATEGORY_ID2, RULE_ID, RULE_ID2 };
		String[] tables = new String[] { CategoryContract.TABLE_NAME, CategoryContract.TABLE_NAME, RuleContract.TABLE_NAME, RuleContract.TABLE_NAME };
		for (int i = 0; i < 4; i++) {
			JSONObject operation = array.getJSONObject(i);
			assertEquals(tables[i], operation.get(JSONBuilder.JSON_FIELD_TABLE));
			assertEquals(ids[i], operation.get(JSONBuilder.JSON_FIELD_ROW));
			assertEquals(0, operation.get(JSONBuilder.JSON_FIELD_REVISION));
			assertTrue(compareContentValues(values[i], translator.translate(operation.getJSONObject(JSONBuilder.JSON_FIELD_DATA))));
		}

		// Check that stats were correctly written
		assertNotNull(marshaller.getStats());
		assertEquals(0, marshaller.getStats().numSkippedEntries);
	}

	public void testMarshalUpdates() throws JSONException {
		Marshaller marshaller = new Marshaller();
		writeCreations();
		writeUpdates();
		JournalSnapshot journal = JournalSnapshot.createFromCursor(getMockContentResolver().query(JournalContract.CONTENT_URI, null, null, null, null));
		RevisionTableSnapshot revisions = RevisionTableSnapshot.createFromCursor(getMockContentResolver().query(RevisionTableContract.CONTENT_URI, null, null, null, null));
		Translator translator = new Translator();

		ContentProviderClient provider = getMockContentResolver().acquireContentProviderClient(CheckbookContentProvider.AUTHORITY);
		JSONObject json = marshaller.marshal(journal, revisions, provider, 0);

		assertEquals(0, json.getInt(Marshaller.JSON_FIELD_ANCHOR));

		JSONArray array = json.getJSONArray(Marshaller.JSON_FIELD_UPDATED);
		assertEquals(3, array.length());
		Object[] values = new Object[] { RULE_CAPTION2, RULE_CAPTION, RULE_CATEGORY };
		long[] ids = new long[] { RULE_ID, RULE_ID2, RULE_ID2 };
		String[] tables = new String[] { RuleContract.TABLE_NAME, RuleContract.TABLE_NAME, RuleContract.TABLE_NAME };
		String[] columns = new String[] { RuleContract.COL_NAME_ANTECEDENT, RuleContract.COL_NAME_ANTECEDENT, RuleContract.COL_NAME_CONSEQUENT };
		for (int i = 0; i < 3; i++) {
			JSONObject operation = array.getJSONObject(i);
			assertEquals(tables[i], operation.get(JSONBuilder.JSON_FIELD_TABLE));
			assertEquals(ids[i], operation.get(JSONBuilder.JSON_FIELD_ROW));
			assertEquals(columns[i], operation.get(JSONBuilder.JSON_FIELD_COLUMN));
			assertEquals(revisions.getRevision(tables[i], ids[i], columns[i]), operation.get(JSONBuilder.JSON_FIELD_REVISION));
			ContentValues data = translator.translate(operation.getJSONObject(JSONBuilder.JSON_FIELD_DATA));
			// Convert to Long to keep from failing the test since ids are small enough that they will be coerced to Integers
			Object dataValue = data.get(columns[i]);
			if (dataValue instanceof Integer)
				dataValue = Long.valueOf((Integer) dataValue);
			assertEquals(values[i], dataValue);
			assertEquals(1, data.size());
		}
		// Check that stats were correctly written
		assertNotNull(marshaller.getStats());
		assertEquals(0, marshaller.getStats().numSkippedEntries);
	}

	public void testMarshalDeletions() throws JSONException {
		Marshaller marshaller = new Marshaller();
		writeCreations();
		writeUpdates();
		writeDeletions();
		JournalSnapshot journal = JournalSnapshot.createFromCursor(getMockContentResolver().query(JournalContract.CONTENT_URI, null, null, null, null));
		RevisionTableSnapshot revisions = RevisionTableSnapshot.createFromCursor(getMockContentResolver().query(RevisionTableContract.CONTENT_URI, null, null, null, null));

		ContentProviderClient provider = getMockContentResolver().acquireContentProviderClient(CheckbookContentProvider.AUTHORITY);
		JSONObject json = marshaller.marshal(journal, revisions, provider, 0);

		assertEquals(0, json.getInt(Marshaller.JSON_FIELD_ANCHOR));

		JSONArray array = json.getJSONArray(Marshaller.JSON_FIELD_DELETED);
		assertEquals(2, array.length());
		long[] ids = new long[] { CATEGORY_ID2, RULE_ID2 };
		String[] tables = new String[] { CategoryContract.TABLE_NAME, RuleContract.TABLE_NAME };
		for (int i = 0; i < 2; i++) {
			JSONObject operation = array.getJSONObject(i);
			assertEquals(tables[i], operation.get(JSONBuilder.JSON_FIELD_TABLE));
			assertEquals(ids[i], operation.get(JSONBuilder.JSON_FIELD_ROW));
			assertEquals(revisions.getMaxRevision(tables[i], ids[i]), operation.get(JSONBuilder.JSON_FIELD_REVISION));
		}
		// Check that stats were correctly written
		// We expect 4 skipped entries
		assertNotNull(marshaller.getStats());
		assertEquals(4, marshaller.getStats().numSkippedEntries);
	}

	public void testGetStats() {
//		fail("Not yet implemented");
	}

}
