package heger.christian.ledger.providers;

import heger.christian.ledger.providers.MetaContentProvider.JournalContract;
import heger.christian.ledger.providers.MetaContentProvider.SequenceAnchorContract;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;

/**
 * This class performs translating data operations into journal entries.
 * For update and delete operations, it will also check whether optimizations
 * are possible to the journal as described in
 * <a href="file:../../../../../notes/Sync algorithm.html#client_side_optimizations">.
 * Sequence numbers are automatically determined. Thus, this class registers a
 * <code>ContentObserver</code> on the sequence anchor table's content URI.
 */
public class Journaler {
	protected class JournalerContentObserver extends ContentObserver {
		public JournalerContentObserver(Handler handler) {
			super(handler);
		}
		@Override
		public void onChange(boolean selfChange) {
			if (!selfChange)
				needsUpdate = true;
		}
	}
	private JournalerContentObserver observer = new JournalerContentObserver(null);

	/**
	 * Data describing possible optimizations from an operation.
	 */
	protected static class OptimizationOperations {
		/**
		 * Whether the operation causing the optimization check can be discarded as a result of optimization.
		 */
		public boolean discardCurrent = false;
		/**
		 * A list of <code>ContentProviderOperation</code>s for deleting any existing entries from the journal
		 * that can be optimized away.
		 */
		public List<ContentProviderOperation> operations = new LinkedList<ContentProviderOperation>();
		public OptimizationOperations() {}
	}

	public static final String OP_TYPE_CREATE = "c";
	public static final String OP_TYPE_UPDATE = "u";
	public static final String OP_TYPE_DELETE = "d";

	private ContentResolver resolver;
	private boolean needsUpdate = true;
	private long nextSequenceNumber = 0;

	public Journaler(ContentResolver resolver) {
		this.resolver = resolver;
		resolver.registerContentObserver(SequenceAnchorContract.CONTENT_URI, true, observer);
	}

	/**
	 * Unregisters the content observer.
	 */
	public void shutdown() {
		resolver.unregisterContentObserver(observer);
		resolver = null;
	}

	/**
	 * Updates the next available sequence number to max(sequence anchor, highest sequence number in journal + 1).
	 * This method is thread safe.
	 */
	private synchronized void updateSequenceNumber() {
		Cursor anchorCursor = resolver.query(SequenceAnchorContract.CONTENT_URI,
				new String[] { SequenceAnchorContract.COL_NAME_SEQUENCE_ANCHOR },
				null, null, null);
		Cursor journalCursor = resolver.query(JournalContract.CONTENT_URI,
				new String[] { "max(" + JournalContract.COL_NAME_SEQUENCE_NUMBER + ")" },
				null, null, null);
		if (anchorCursor != null && anchorCursor.moveToFirst()) {
			nextSequenceNumber = anchorCursor.getLong(0);
			needsUpdate = false;
		}
		if (journalCursor != null && journalCursor.moveToFirst()) {
			nextSequenceNumber = Math.max(nextSequenceNumber, journalCursor.getLong(0) + 1);
			needsUpdate = false;
		}
	}

	/**
	 * Returns the next available sequence number, lazily initializing it from storage if needed.
	 * @return The next available sequence number.
	 * @see Journaler#updateSequenceNumber()
	 */
	protected synchronized long getSequenceNumber() {
		if (needsUpdate) updateSequenceNumber();
		return nextSequenceNumber++;
	}

	/**
	 * Tries to hand back the supplied sequence number to the pool of available numbers. A
	 * sequence number can be handed back if and only if
	 * <ol>
	 * <li>it was the last sequence number to be issued
	 * <li>sequence number generating does not need to be initialized on the next call to
	 * <code>getSequenceNumber()</code>
	 * </ol>
	 * @param sqn - The sequence number to hand back
	 * @return <code>True</code> if the sequence number return was accepted, <code>false</code> otherwise.
	 */
	protected synchronized boolean returnSequenceNumber(long sqn) {
		if (!needsUpdate && nextSequenceNumber == sqn + 1) {
			nextSequenceNumber--;
			return true;
		}
		return false;
	}

	/**
	 * Gets a <code>ContentProviderOperation</code> for writing the creation of the specified row
	 * within the specified table to the journal, using the next available sequence number.
	 * @param table - The name of the table where the creation occurred
	 * @param id - The primary key of the created row
	 * @return A list of <code>ContentProviderOperation</code>s to write the creation to the journal.
	 * Optimizations aren't possible on creations, but for consistency with the other methods, this
	 * will return a one-element <code>ArrayList</code>.
	 */
	public ArrayList<ContentProviderOperation> getJournalCreateOperation(String table, long id) {
		ContentValues values = new ContentValues();
		values.put(JournalContract.COL_NAME_SEQUENCE_NUMBER, getSequenceNumber());
		values.put(JournalContract.COL_NAME_TABLE, table);
		values.put(JournalContract.COL_NAME_ROW, id);
		values.put(JournalContract.COL_NAME_COLUMN, (String) null);
		values.put(JournalContract.COL_NAME_OPERATION, OP_TYPE_CREATE);

		ContentProviderOperation op = ContentProviderOperation.newInsert(JournalContract.CONTENT_URI)
				.withValues(values)
				.build();

		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
		operations.add(op);
		return operations;
	}

	/**
	 * Tries to find an entries within the journal that can be optimized away under the
	 * conditions given in the <a href="file:../../../../../notes/Sync algorithm.html#client_side_optimizations">
	 * Ledger synchronization algorithm outline</a> given the current
	 * update to <code>table</code>, <code>id</code> and <code>column</code>.
	 * @return An <code>OptimizationOperations</code> that contains the optimizations
	 * possible given the current update.
	 */
	protected OptimizationOperations getUpdateOptimizations(String table, long id, String column) {
		// Select all UPDATES for the same table, row and column, or all
		// CREATES for the same table and row that have a
		// sequence number higher than N
		String where = JournalContract.COL_NAME_TABLE + "=? and " +
				JournalContract.COL_NAME_ROW + "=" + id + " and " +
				"(" + JournalContract.COL_NAME_COLUMN + "=? and " +
				JournalContract.COL_NAME_OPERATION + "=\"" + OP_TYPE_UPDATE + "\" or " +
				JournalContract.COL_NAME_OPERATION + "=\"" + OP_TYPE_CREATE + "\") and " +
				JournalContract.COL_NAME_SEQUENCE_NUMBER + " >= (SELECT * FROM " + SequenceAnchorContract.TABLE_NAME + ")";
		String[] args = new String[] { table, column };

		// If any such entry exists, the current update doesn't need to be written to the journal
		Cursor cursor = resolver.query(JournalContract.CONTENT_URI,
				new String[] { JournalContract.COL_NAME_SEQUENCE_NUMBER, JournalContract.COL_NAME_OPERATION },
				where,
				args,
				null);
		OptimizationOperations optimizations = new OptimizationOperations();
		if (cursor != null && cursor.moveToFirst()) {
			optimizations.discardCurrent = true;
		}
		return optimizations;
	}

	/**
	 * Gets a <code>ContentProviderOperation</code> for writing the update of the specified row and column
	 * within the specified table to the journal, using the next available sequence number.
	 * If optimizations to the journal are possible from this update, they will be included. Note that this
	 * might cause the entry for the current update to be optimized away.
	 * @param table - The name of the table where the update occurred
	 * @param id - The primary key of the updated row
	 * @param column - The name of the column that was updated
	 * @return A list of <code>ContentProviderOperation</code>s to write the update to the journal,
	 * including any optimizations possible.
	 * @see #getUpdateOptimizations(String, long, String)
	 */
	public ArrayList<ContentProviderOperation> getJournalUpdateOperation(String table, long id, String column) {
		ContentValues values = new ContentValues();
		long sqn = getSequenceNumber();
		values.put(JournalContract.COL_NAME_SEQUENCE_NUMBER, sqn);
		values.put(JournalContract.COL_NAME_TABLE, table);
		values.put(JournalContract.COL_NAME_ROW, id);
		values.put(JournalContract.COL_NAME_COLUMN, column);
		values.put(JournalContract.COL_NAME_OPERATION, OP_TYPE_UPDATE);
		ContentProviderOperation op = ContentProviderOperation.newInsert(JournalContract.CONTENT_URI)
				.withValues(values)
				.build();

		OptimizationOperations optimizations = getUpdateOptimizations(table, id, column);

		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
		if (optimizations != null)
			operations.addAll(optimizations.operations);
		if (!optimizations.discardCurrent) {
			operations.add(op);
		} else {
			returnSequenceNumber(sqn);
		}
		return operations;
	}

	/**
	 * Tries to find an entries within the journal that can be optimized away under the
	 * conditions given in the <a href="file:../../../../../notes/Sync algorithm.html#client_side_optimizations">
	 * Ledger synchronization algorithm outline</a> given the current
	 * deletion from <code>table</code> and <code>id</code>.
	 * @return An <code>OptimizationOperations</code> that contains the optimizations
	 * possible given the current deletion.
	 */
	protected OptimizationOperations getDeleteOptimizations(String table, long id) {
		// Select all updates or creates for the same table and row that have a
		// sequence number higher than N
		String where = JournalContract.COL_NAME_TABLE + "=? and " +
				JournalContract.COL_NAME_ROW + "=" + id + " and " +
				"(" + JournalContract.COL_NAME_OPERATION + "=\"" + OP_TYPE_UPDATE + "\" or " +
				JournalContract.COL_NAME_OPERATION + "=\"" + OP_TYPE_CREATE + "\") and " +
				JournalContract.COL_NAME_SEQUENCE_NUMBER + " >= (SELECT * FROM " + SequenceAnchorContract.TABLE_NAME + ")";
		String[] args = new String[] { table };
		Cursor cursor = resolver.query(JournalContract.CONTENT_URI, null, where, args, null);

		// If any such entries exist, put delete operations for them into the optimizations
		// and check if the current delete can be discarded
		OptimizationOperations optimizations = new OptimizationOperations();
		if (cursor != null) {
			cursor.moveToPosition(-1);
			int sequenceNumberIndex = cursor.getColumnIndex(JournalContract.COL_NAME_SEQUENCE_NUMBER);
			int operationIndex = cursor.getColumnIndex(JournalContract.COL_NAME_OPERATION);
			while (cursor.moveToNext()) {
				Uri uri = ContentUris.withAppendedId(JournalContract.CONTENT_URI, cursor.getLong(sequenceNumberIndex));
				optimizations.operations.add(ContentProviderOperation.newDelete(uri).build());
				if (cursor.getString(operationIndex).equals(OP_TYPE_CREATE))
					optimizations.discardCurrent = true;
			}
		}
		return optimizations;
	}


	/**
	 * Gets a <code>ContentProviderOperation</code> for writing the deletion of the specified row
	 * within the specified table to the journal, using the next available sequence number.
	 * If optimizations to the journal are possible from this deletion, they will be included in the result. Note that this
	 * might cause the entry for the current delete to be optimized away.
	 * @param table - The name of the table where the deletion occurred
	 * @param id - The primary key of the deleted row
	 * @return A list of <code>ContentProviderOperation</code>s to write the deletion to the journal,
	 * including any optimizations possible.
	 * @see #getDeleteOptimizations(String, long)
	 */
	public ArrayList<ContentProviderOperation> getJournalDeleteOperation(String table, long id) {
		ContentValues values = new ContentValues();
		long sqn = getSequenceNumber();
		values.put(JournalContract.COL_NAME_SEQUENCE_NUMBER, sqn);
		values.put(JournalContract.COL_NAME_TABLE, table);
		values.put(JournalContract.COL_NAME_ROW, id);
		values.put(JournalContract.COL_NAME_COLUMN, (String) null);
		values.put(JournalContract.COL_NAME_OPERATION, OP_TYPE_DELETE);
		ContentProviderOperation op = ContentProviderOperation.newInsert(JournalContract.CONTENT_URI)
				.withValues(values).build();

		OptimizationOperations optimizations = getDeleteOptimizations(table, id);

		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
		operations.addAll(optimizations.operations);
		if (!optimizations.discardCurrent) {
			operations.add(op);
		} else {
			returnSequenceNumber(sqn);
		}
		return operations;
	}

	/**
	 * Convenience method for running
	 * <code>applyBatch(getJournalCreateOperation(table,id)</code> on the content resolver supplied
	 * to the constructor.
	 */
	public void journalCreate(String table, long id) throws RemoteException, OperationApplicationException {
		resolver.applyBatch(MetaContentProvider.AUTHORITY, getJournalCreateOperation(table, id));
	}

	/**
	 * Convenience method for running
	 * <code>applyBatch(getJournalUpdateOperation(table,id,column)</code> on the content resolver supplied
	 * to the constructor.
	 */
	public void journalUpdate(String table, long id, String column) throws RemoteException, OperationApplicationException {
		resolver.applyBatch(MetaContentProvider.AUTHORITY, getJournalUpdateOperation(table, id, column));
	}

	/**
	 * Convenience method for running
	 * <code>applyBatch(getJournalDeleteOperation(table,id)</code> on the content resolver supplied
	 * to the constructor.
	 */
	public void journalDelete(String table, long id) throws RemoteException, OperationApplicationException {
		resolver.applyBatch(MetaContentProvider.AUTHORITY, getJournalDeleteOperation(table, id));
	}
}
