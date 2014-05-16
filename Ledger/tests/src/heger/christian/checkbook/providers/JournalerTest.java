package heger.christian.checkbook.providers;

import heger.christian.checkbook.providers.Journaler;
import heger.christian.checkbook.providers.MetaContentProvider;
import heger.christian.checkbook.providers.MetaContentProvider.JournalContract;
import heger.christian.checkbook.providers.MetaContentProvider.SequenceAnchorContract;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.test.ProviderTestCase2;

public class JournalerTest extends ProviderTestCase2<MetaContentProvider> {
	// Make some of Journaler's methods public for testing accessibility
	private class TestingJournaler extends Journaler {
		public TestingJournaler(ContentResolver resolver) {
			super(resolver);
		}
		@Override
		public synchronized long getSequenceNumber() {
			return super.getSequenceNumber();
		}
		@Override
		public synchronized boolean returnSequenceNumber(long sqn) {
			return super.returnSequenceNumber(sqn);
		}
	}
	private TestingJournaler journaler;
	private static final String TABLE = "table";
	private static final long ROW = 0;
	private static final String COLUMN = "column";

	public JournalerTest() {
		super(MetaContentProvider.class, MetaContentProvider.AUTHORITY);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		journaler = new TestingJournaler(getMockContentResolver());
	}

	@Override
	public void tearDown() {
	}

	public void testGetSequenceNumberReturnSequenceNumber() {
		String tag = "getSequenceNumber: ";
		final long anchor = 20;
		ContentValues values = new ContentValues();
		values.put(SequenceAnchorContract.COL_NAME_SEQUENCE_ANCHOR, anchor);
		getMockContentResolver().insert(SequenceAnchorContract.CONTENT_URI, values);

		long sqn = journaler.getSequenceNumber();
		assertTrue(tag + "sequence number is too low for anchor " + anchor + ": " + sqn, sqn >= anchor);

		tag = "returnSequenceNumber: ";
		assertTrue(tag + "sequence number " + sqn + " was not accepted", journaler.returnSequenceNumber(sqn));
		assertEquals(tag + "subsequent call to getSequenceNumber did not give expected value",sqn, journaler.getSequenceNumber());
	}

	public void testJournalCreate() throws RemoteException, OperationApplicationException {
		String tag = "Writing create: ";
		ArrayList<ContentProviderOperation> operations = journaler.getJournalCreateOperation(TABLE, ROW);
		ContentProviderResult[] results = getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY, operations);
		assertNotNull(tag + " result array is null", results);
		assertTrue(tag + " result array is zero-length", results.length > 0);
		assertNotNull(tag + " result URI is null", results[0].uri);

		Cursor cursor = getMockContentResolver().query(results[0].uri, null, null, null, null);
		assertNotNull(tag + " cursor is null", cursor);
		assertTrue(tag + " cursor is empty", cursor.moveToFirst());
		assertEquals(tag + " inserted entry has wrong table", TABLE, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_TABLE)));
		assertEquals(tag + " inserted entry has wrong row", ROW, cursor.getLong(cursor.getColumnIndex(JournalContract.COL_NAME_ROW)));
		assertEquals(tag + " inserted entry has wrong type", Journaler.OP_TYPE_CREATE, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_OPERATION)));
	}

	private void populate() {
		String[] ops = { Journaler.OP_TYPE_CREATE, Journaler.OP_TYPE_UPDATE, Journaler.OP_TYPE_DELETE };
		ContentResolver resolver = getMockContentResolver();
		long i = 0;
		// Put in a create entry for same table but different row
		ContentValues values = new ContentValues();
		values.put(JournalContract.COL_NAME_SEQUENCE_NUMBER, i++);
		values.put(JournalContract.COL_NAME_TABLE, TABLE);
		values.put(JournalContract.COL_NAME_ROW, ROW + 1);
		values.put(JournalContract.COL_NAME_OPERATION, Journaler.OP_TYPE_CREATE);
		resolver.insert(JournalContract.CONTENT_URI, values);
		// Put in a create entry for same table but different row
		values = new ContentValues();
		values.put(JournalContract.COL_NAME_SEQUENCE_NUMBER, i++);
		values.put(JournalContract.COL_NAME_TABLE, TABLE);
		values.put(JournalContract.COL_NAME_ROW, ROW + 1);
		values.put(JournalContract.COL_NAME_OPERATION, Journaler.OP_TYPE_CREATE);
		resolver.insert(JournalContract.CONTENT_URI, values);
		// Put in an update entry for same table but different row
		values = new ContentValues();
		values.put(JournalContract.COL_NAME_SEQUENCE_NUMBER, i++);
		values.put(JournalContract.COL_NAME_TABLE, TABLE);
		values.put(JournalContract.COL_NAME_ROW, ROW + 2);
		values.put(JournalContract.COL_NAME_COLUMN, COLUMN);
		values.put(JournalContract.COL_NAME_OPERATION, Journaler.OP_TYPE_UPDATE);
		resolver.insert(JournalContract.CONTENT_URI, values);
		// Put in a delete entry for same table but different row
		values = new ContentValues();
		values.put(JournalContract.COL_NAME_SEQUENCE_NUMBER, i++);
		values.put(JournalContract.COL_NAME_TABLE, TABLE);
		values.put(JournalContract.COL_NAME_ROW, ROW + 1);
		values.put(JournalContract.COL_NAME_OPERATION, Journaler.OP_TYPE_CREATE);
		resolver.insert(JournalContract.CONTENT_URI, values);

		// Put in values for same row and column but different table
		for (String op: ops) {
			values = new ContentValues();
			values.put(JournalContract.COL_NAME_SEQUENCE_NUMBER, i++);
			values.put(JournalContract.COL_NAME_TABLE, "other table");
			values.put(JournalContract.COL_NAME_ROW, ROW);
			values.put(JournalContract.COL_NAME_OPERATION, op);
			resolver.insert(JournalContract.CONTENT_URI, values);
		}
	}

	private boolean verifyExistence() {
		boolean result = true;
		Cursor cursor;
		for (int i = 0; i <= 6; i++) {
			cursor = getMockContentResolver().query(ContentUris.withAppendedId(JournalContract.CONTENT_URI, 0), null, null, null, null);
			result &= cursor.moveToFirst();
		}
		return result;
	}

	/**
	 * Test just a regular update. Expect to see it entered in the journal with correct values, and not messing with the existing values.
	 */
	public void testJournalUpdate() throws RemoteException, OperationApplicationException {
		populate();
		String tag = "Writing simple update: ";
		ArrayList<ContentProviderOperation> operations = journaler.getJournalUpdateOperation(TABLE, ROW, COLUMN);
		ContentProviderResult[] results = getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY, operations);
		assertNotNull(tag + " result array is null", results);
		assertTrue(tag + " result array is zero-length", results.length > 0);
		assertNotNull(tag + " result URI is null", results[0].uri);

		assertTrue(tag + " verify pre-existing values" , verifyExistence());
		Cursor cursor = getMockContentResolver().query(results[0].uri, null, null, null, null);
		assertNotNull(tag + " cursor is null", cursor);
		assertTrue(tag + " cursor is empty", cursor.moveToFirst());
		assertEquals(tag + " inserted entry has wrong table", TABLE, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_TABLE)));
		assertEquals(tag + " inserted entry has wrong row", ROW, cursor.getLong(cursor.getColumnIndex(JournalContract.COL_NAME_ROW)));
		assertEquals(tag + " inserted entry has wrong column", COLUMN, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_COLUMN)));
		assertEquals(tag + " inserted entry has wrong type", Journaler.OP_TYPE_UPDATE, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_OPERATION)));
	}

	/**
	 * Test an update where there is an existing update that could be optimized away, except the value
	 * of the sequence anchor prevents this. Expect to see both entries there.
	 */
	public void testJournalUpdateSequenceAnchorPreventsOptimization() throws RemoteException, OperationApplicationException {
		populate();
		String tag = "Writing update with sequence anchor preventing possible update/update optimization: ";
		ArrayList<ContentProviderOperation> operations = journaler.getJournalUpdateOperation(TABLE, ROW, COLUMN);
		ContentProviderResult[] results = getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY, operations);

		// Set anchor to the new entry we're just about to create
		// Setting it to an arbitrary higher number won't work because the MockContentResolver mocks out calls to its observers
		long anchor = ContentUris.parseId(results[0].uri) + 1;
		ContentValues values = new ContentValues();
		values.put(SequenceAnchorContract.COL_NAME_SEQUENCE_ANCHOR, anchor);
		getMockContentResolver().insert(SequenceAnchorContract.CONTENT_URI, values);
		results = getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY,
				journaler.getJournalUpdateOperation(TABLE, ROW, COLUMN));
		Cursor cursor = getMockContentResolver().query(JournalContract.CONTENT_URI,
				null,
				JournalContract.COL_NAME_TABLE + "=? and " + JournalContract.COL_NAME_ROW + "=" + ROW + " and " + JournalContract.COL_NAME_COLUMN + "=?",
				new String[] { TABLE, COLUMN },
				null);
		assertTrue(tag + " verify pre-existing values",verifyExistence());
		assertEquals(tag + " wrong number of entries", cursor.getCount(), 2);
	}

	/**
	 * Test an update where there is an existing update that could be optimized away.
	 * Expect to see only one entry there.
	 */
	public void testJournalUpdateWithUpdateOptimizations() throws RemoteException, OperationApplicationException {
		populate();
		String tag = "Writing update with possible update/update optimization: ";
		ArrayList<ContentProviderOperation> operations = journaler.getJournalUpdateOperation(TABLE, ROW, COLUMN);
		getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY, operations);

		getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY,
				journaler.getJournalUpdateOperation(TABLE, ROW, COLUMN));
		Cursor cursor = getMockContentResolver().query(JournalContract.CONTENT_URI,
				null,
				JournalContract.COL_NAME_TABLE + "=? and " + JournalContract.COL_NAME_ROW + "=" + ROW + " and " + JournalContract.COL_NAME_COLUMN + "=?",
				new String[] { TABLE, COLUMN },
				null);
		assertTrue(tag + " verify pre-existing values",verifyExistence());
		assertEquals(tag + " wrong number of entries", cursor.getCount(), 1);
	}

	/**
	 * Test an update when with a possible update/create optimization.
	 * Expect to see only the create in the journal.
	 */
	public void testJournalUpdateWithCreateOptimizations() throws RemoteException, OperationApplicationException {
		populate();
		String tag = "Writing update with possible update/create optimization: ";
		ArrayList<ContentProviderOperation> operations = journaler.getJournalCreateOperation(TABLE, ROW);
		getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY, operations);

		getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY,
				journaler.getJournalUpdateOperation(TABLE, ROW, COLUMN));
		Cursor cursor = getMockContentResolver().query(JournalContract.CONTENT_URI,
				null,
				JournalContract.COL_NAME_TABLE + "=? and " + JournalContract.COL_NAME_ROW + "=" + ROW,
				new String[] { TABLE },
				null);
		assertTrue(tag + " verify pre-existing values",verifyExistence());
		assertEquals(tag + " wrong number of entries", cursor.getCount(), 1);
		cursor.moveToFirst();
		assertEquals(tag + " not a create operation", cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_OPERATION)), Journaler.OP_TYPE_CREATE);
	}

	/**
	 * Test a simple delete operation.
	 * Expect to see it in the journal.
	 */
	public void testJournalDelete() throws RemoteException, OperationApplicationException {
		populate();
		String tag = "Writing simple delete: ";
		ArrayList<ContentProviderOperation> operations = journaler.getJournalDeleteOperation(TABLE, ROW);
		ContentProviderResult[] results = getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY, operations);

		assertNotNull(tag + " result array is null", results);
		assertTrue(tag + " result array is zero-length", results.length > 0);
		assertNotNull(tag + " result URI is null", results[0].uri);

		assertTrue(tag + " verify pre-existing values" , verifyExistence());
		Cursor cursor = getMockContentResolver().query(results[0].uri, null, null, null, null);
		assertNotNull(tag + " cursor is null", cursor);
		assertTrue(tag + " cursor is empty", cursor.moveToFirst());
		assertEquals(tag + " inserted entry has wrong table", TABLE, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_TABLE)));
		assertEquals(tag + " inserted entry has wrong row", ROW, cursor.getLong(cursor.getColumnIndex(JournalContract.COL_NAME_ROW)));
		assertEquals(tag + " inserted entry has wrong type", Journaler.OP_TYPE_DELETE, cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_OPERATION)));
	}

	/**
	 * Test a delete operation with the current sequence anchor value preventing optimizations.
	 * Expect to see all entries.
	 */
	public void testJournalDeleteWithSequenceAnchorPreventingOptimizations() throws RemoteException, OperationApplicationException {
		populate();
		String tag = "Writing delete with sequence anchor preventing possible delete optimization: ";
		ArrayList<ContentProviderOperation> operations = journaler.getJournalCreateOperation(TABLE, ROW);
		operations.addAll(journaler.getJournalUpdateOperation(TABLE, ROW, COLUMN));
		operations.addAll(journaler.getJournalUpdateOperation(TABLE, ROW, COLUMN + COLUMN));
		ContentProviderResult[] results = getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY, operations);

		// Set anchor to the new entry we're just about to create
		// Setting it to an arbitrary higher number won't work because the MockContentResolver mocks out calls to its observers
		long anchor = ContentUris.parseId(results[results.length - 1].uri) + 1;
		ContentValues values = new ContentValues();
		values.put(SequenceAnchorContract.COL_NAME_SEQUENCE_ANCHOR, anchor);
		getMockContentResolver().insert(SequenceAnchorContract.CONTENT_URI, values);
		results = getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY,
				journaler.getJournalDeleteOperation(TABLE, ROW));
		Cursor cursor = getMockContentResolver().query(JournalContract.CONTENT_URI,
				null,
				JournalContract.COL_NAME_TABLE + "=? and " + JournalContract.COL_NAME_ROW + "=" + ROW,
				new String[] { TABLE },
				null);
		assertTrue(tag + " verify pre-existing values",verifyExistence());
		assertEquals(tag + " wrong number of entries", cursor.getCount(), 4);
	}

	/**
	 * Test a delete with update entries eligible for optimization already present.
	 * Expect to see only the delete
	 */
	public void testJournalDeleteWithUpdateOptimization() throws RemoteException, OperationApplicationException {
		populate();
		String tag = "Writing delete with possible delete/update optimization: ";
		ArrayList<ContentProviderOperation> operations = journaler.getJournalUpdateOperation(TABLE, ROW, COLUMN);
		operations.addAll(journaler.getJournalUpdateOperation(TABLE, ROW, COLUMN + COLUMN));
		getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY, operations);

		getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY,
				journaler.getJournalDeleteOperation(TABLE, ROW));
		Cursor cursor = getMockContentResolver().query(JournalContract.CONTENT_URI,
				null,
				JournalContract.COL_NAME_TABLE + "=? and " + JournalContract.COL_NAME_ROW + "=" + ROW,
				new String[] { TABLE },
				null);
		assertTrue(tag + " verify pre-existing values",verifyExistence());
		assertEquals(tag + " wrong number of entries", cursor.getCount(), 1);
		cursor.moveToFirst();
		assertEquals(tag + " not a delete operation", cursor.getString(cursor.getColumnIndex(JournalContract.COL_NAME_OPERATION)), Journaler.OP_TYPE_DELETE);
	}

	/**
	 * Test a delete operation with possible delete/create optimization.
	 * Expect to see no operations in the journal (both should be optimized away)
	 */
	public void testJournalDeleteWithCreateOptimization() throws RemoteException, OperationApplicationException {
		populate();
		String tag = "Writing delete with possible delete/create optimization: ";
		ArrayList<ContentProviderOperation> operations = journaler.getJournalCreateOperation(TABLE, ROW);
		getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY, operations);

		getMockContentResolver().applyBatch(MetaContentProvider.AUTHORITY,
				journaler.getJournalDeleteOperation(TABLE, ROW));
		Cursor cursor = getMockContentResolver().query(JournalContract.CONTENT_URI,
				null,
				JournalContract.COL_NAME_TABLE + "=? and " + JournalContract.COL_NAME_ROW + "=" + ROW,
				new String[] { TABLE },
				null);
		assertTrue(tag + " verify pre-existing values",verifyExistence());
		assertEquals(tag + " wrong number of entries", cursor.getCount(), 0);
	}
}
