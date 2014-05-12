package heger.christian.checkbook.providers;

import heger.christian.checkbook.db.CheckbookDbHelper.KeyGenerationContract;
import heger.christian.checkbook.providers.MetaContentProvider.JournalContract;
import heger.christian.checkbook.providers.MetaContentProvider.RevisionTableContract;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.test.ProviderTestCase2;


public class CheckbookContentProviderTest extends ProviderTestCase2<CheckbookContentProvider> {

	public CheckbookContentProviderTest() {
		super(CheckbookContentProvider.class, CheckbookContentProvider.AUTHORITY);
	}

//	private CheckbookContentProvider dataProvider;
	private MetaContentProvider metaProvider;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		metaProvider = new MetaContentProvider();
		metaProvider.attachInfo(getMockContext(), null);
		getMockContentResolver().addProvider(MetaContentProvider.AUTHORITY, metaProvider);
		// Keep key generator from failing
		ContentValues values = new ContentValues();
		values.put(KeyGenerationContract.COL_NAME_NEXT_KEY, 0);
		values.put(KeyGenerationContract.COL_NAME_UPPER_BOUND, 100);
		getMockContentResolver().insert(KeyGenerationContract.CONTENT_URI, values);
	}

	@Override
	public void tearDown() {
		metaProvider.shutdown();
	}

	public void testInsert() {
		ContentResolver resolver = getMockContentResolver();
		ContentValues values = new ContentValues();
		values.put(CategoryContract.COL_NAME_CAPTION,CategoryContract.COL_NAME_CAPTION);
		long id = ContentUris.parseId(getMockContentResolver().insert(CategoryContract.CONTENT_URI, values));

		Cursor cursor = getMockContentResolver().query(CategoryContract.CONTENT_URI, null, null, null, null);
		assertEquals("Category was not added", 1, cursor.getCount());

		cursor = resolver.query(JournalContract.CONTENT_URI, null, null, null, null);
		cursor.moveToFirst();
		assertEquals("Journal was not written", 1, cursor.getCount());
		assertEquals("Incorrect table", CategoryContract.TABLE_NAME, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_TABLE)));
		assertEquals("Incorrect row", id, cursor.getLong(cursor.getColumnIndex(JournalContract.COL_NAME_ROW)));
		assertEquals("Incorrect operation type", Journaler.OP_TYPE_CREATE, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_OPERATION)));

		cursor = resolver.query(RevisionTableContract.CONTENT_URI,
				null,
				RevisionTableContract.COL_NAME_TABLE + "=? and " + RevisionTableContract.COL_NAME_ROW + "=" + id,
				new String[] { CategoryContract.TABLE_NAME },
				null);
		assertEquals("Wrong number of revision table entries", 2, cursor.getCount());
		cursor.moveToPosition(-1);
		List<String> columns = new LinkedList<String>(Arrays.asList(new String[] { CategoryContract._ID, CategoryContract.COL_NAME_CAPTION }));
		while (cursor.moveToNext()) {
			assertEquals("Incorrect table", CategoryContract.TABLE_NAME, cursor.getString(cursor.getColumnIndex(RevisionTableContract.COL_NAME_TABLE)));
			assertEquals("Incorrect row", id, cursor.getLong(cursor.getColumnIndex(RevisionTableContract.COL_NAME_ROW)));
			int j = columns.indexOf(cursor.getString(cursor.getColumnIndex(RevisionTableContract.COL_NAME_COLUMN)));
			assertTrue(j != -1);
			assertEquals("Incorrect revision number", 0, cursor.getLong(cursor.getColumnIndex(RevisionTableContract.COL_NAME_REVISION)));
			columns.remove(j);
		}
	}

	public void testUpdate() {
		ContentResolver resolver = getMockContentResolver();

		ContentValues values = new ContentValues();
		values.put(CategoryContract.COL_NAME_CAPTION, CategoryContract.COL_NAME_CAPTION);
		long category = ContentUris.parseId(resolver.insert(CategoryContract.CONTENT_URI, values));

		// For this test, do something with multiple columns
		values = new ContentValues();
		values.put(RuleContract.COL_NAME_ANTECEDENT, RuleContract.COL_NAME_ANTECEDENT);
		values.put(RuleContract.COL_NAME_CONSEQUENT, category);
		long id = ContentUris.parseId(resolver.insert(RuleContract.CONTENT_URI, values));

		// Clear journal to prevent optimizations
		resolver.delete(JournalContract.CONTENT_URI, null, null);

		values = new ContentValues();
		values.put(RuleContract.COL_NAME_ANTECEDENT, RuleContract.COL_NAME_ANTECEDENT + RuleContract.COL_NAME_ANTECEDENT);
		int count = resolver.update(ContentUris.withAppendedId(RuleContract.CONTENT_URI, id), values, null, null);
		assertEquals("Wrong number of rows modified", 1, count);

		Cursor cursor = resolver.query(ContentUris.withAppendedId(RuleContract.CONTENT_URI, id), null, null, null, null);
		assertTrue("Row not present",cursor.moveToFirst());
		assertEquals("Column " + RuleContract.COL_NAME_ANTECEDENT + " was not updated", RuleContract.COL_NAME_ANTECEDENT + RuleContract.COL_NAME_ANTECEDENT, cursor.getString(cursor.getColumnIndex(RuleContract.COL_NAME_ANTECEDENT)));
		assertEquals("Column " + RuleContract.COL_NAME_CONSEQUENT + " was updated when it shouldn't have been", category, cursor.getLong(cursor.getColumnIndex(RuleContract.COL_NAME_CONSEQUENT)));

		// Journal was empty before update, so there should only be one entry now
		cursor = resolver.query(JournalContract.CONTENT_URI, null, null, null, null);
		cursor.moveToFirst();
		assertEquals("Journal was not written", 1, cursor.getCount());
		assertEquals("Incorrect table", RuleContract.TABLE_NAME, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_TABLE)));
		assertEquals("Incorrect row", id, cursor.getLong(cursor.getColumnIndex(JournalContract.COL_NAME_ROW)));
		assertEquals("Incorrect column", RuleContract.COL_NAME_ANTECEDENT, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_COLUMN)));
		assertEquals("Incorrect operation type", Journaler.OP_TYPE_UPDATE, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_OPERATION)));

		cursor = resolver.query(RevisionTableContract.CONTENT_URI,
				null,
				RevisionTableContract.COL_NAME_TABLE + "=? and " + RevisionTableContract.COL_NAME_ROW + "=" + id,
				new String[] { RuleContract.TABLE_NAME },
				null);
		assertEquals("Wrong number of revision table entries", 3, cursor.getCount());
		cursor.moveToPosition(-1);
		List<String> columns = new LinkedList<String>(Arrays.asList(new String[] { RuleContract._ID, RuleContract.COL_NAME_ANTECEDENT, RuleContract.COL_NAME_CONSEQUENT }));
		while (cursor.moveToNext()) {
			assertEquals("Incorrect table", RuleContract.TABLE_NAME, cursor.getString(cursor.getColumnIndex(RevisionTableContract.COL_NAME_TABLE)));
			assertEquals("Incorrect row", id, cursor.getLong(cursor.getColumnIndex(RevisionTableContract.COL_NAME_ROW)));
			int j = columns.indexOf(cursor.getString(cursor.getColumnIndex(RevisionTableContract.COL_NAME_COLUMN)));
			assertTrue(j != -1);
			assertEquals("Incorrect revision number", 0, cursor.getLong(cursor.getColumnIndex(RevisionTableContract.COL_NAME_REVISION)));
			columns.remove(j);
		}
	}

	public void testDelete() {
		ContentResolver resolver = getMockContentResolver();

		ContentValues values = new ContentValues();
		values.put(CategoryContract.COL_NAME_CAPTION,CategoryContract.COL_NAME_CAPTION);
		long id = ContentUris.parseId(resolver.insert(CategoryContract.CONTENT_URI, values));

		// Clear journal to prevent optimizations
		resolver.delete(JournalContract.CONTENT_URI, null, null);

		int count = resolver.delete(ContentUris.withAppendedId(CategoryContract.CONTENT_URI, id), null, null);
		assertEquals("Wrong number of rows affected", 1, count);

		Cursor cursor = resolver.query(CategoryContract.CONTENT_URI, null, null, null, null);
		cursor.moveToFirst();
		assertEquals("Row was not deleted", 0, cursor.getCount());

		// Journal was empty before delete, so there should only be one entry now
		cursor = resolver.query(JournalContract.CONTENT_URI, null, null, null, null);
		cursor.moveToFirst();
		assertEquals("Journal was not written", 1, cursor.getCount());
		assertEquals("Incorrect table", CategoryContract.TABLE_NAME, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_TABLE)));
		assertEquals("Incorrect row", id, cursor.getLong(cursor.getColumnIndex(JournalContract.COL_NAME_ROW)));
		assertEquals("Incorrect operation type", Journaler.OP_TYPE_DELETE, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_OPERATION)));

		cursor = resolver.query(RevisionTableContract.CONTENT_URI,
				null,
				RevisionTableContract.COL_NAME_TABLE + "=? and " + RevisionTableContract.COL_NAME_ROW + "=" + id,
				new String[] { CategoryContract.TABLE_NAME },
				null);
		assertEquals("Wrong number of revision table entries", 2, cursor.getCount());
		cursor.moveToPosition(-1);
		List<String> columns = new LinkedList<String>(Arrays.asList(new String[] { CategoryContract._ID, CategoryContract.COL_NAME_CAPTION }));
		while (cursor.moveToNext()) {
			assertEquals("Incorrect table", CategoryContract.TABLE_NAME, cursor.getString(cursor.getColumnIndex(RevisionTableContract.COL_NAME_TABLE)));
			assertEquals("Incorrect row", id, cursor.getLong(cursor.getColumnIndex(RevisionTableContract.COL_NAME_ROW)));
			int j = columns.indexOf(cursor.getString(cursor.getColumnIndex(RevisionTableContract.COL_NAME_COLUMN)));
			assertTrue(j != -1);
			assertEquals("Incorrect revision number", 0, cursor.getLong(cursor.getColumnIndex(RevisionTableContract.COL_NAME_REVISION)));
			columns.remove(j);
		}
	}
}
