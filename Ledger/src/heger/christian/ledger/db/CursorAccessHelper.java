package heger.christian.ledger.db;

import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.provider.BaseColumns;

public class CursorAccessHelper {
	private final Cursor cursor;
	
	public CursorAccessHelper(Cursor cursor) {
		if (cursor == null)
			throw new IllegalArgumentException("Cannot instantiate without a cursor");
		this.cursor = cursor;
	}

	public Cursor getCursor() {
		return cursor;
	}

	
	
	/**
	 * Moves the cursor to the row with the specified id. 
	 * @param cursor - The cursor to move. The cursor must contain an _id column.
	 * @param id - The id to move to
	 * @return The new position of the cursor or -1 if no row with the specified id exists in the cursor.
	 * @throws IllegalStateException - If the cursor does not have an _id column
	 */
	public static int moveToId(Cursor cursor, int id) {
		int columnIndex = cursor.getColumnIndex(BaseColumns._ID);
		if (columnIndex == -1)
			throw new IllegalStateException("Cursor does not have an _id column");
		
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) 
			if (cursor.getInt(columnIndex) == id)
				return cursor.getPosition();
		return -1;
	}

	public int moveToId(int id) {
		return moveToId(cursor,id);
	}
	
public static byte[] getBlob(Cursor cursor, String columnName) {
		return cursor.getBlob(cursor.getColumnIndex(columnName));
	}
	
	public byte[] getBlob(String columnName) {
		return getBlob(cursor,columnName);
	}

	public static String getString(Cursor cursor, String columnName) {
		return cursor.getString(cursor.getColumnIndex(columnName));
	}
	
	public String getString(String columnName) {
		return getString(cursor,columnName);
	}

	public static void copyStringToBuffer(Cursor cursor, String columnName, CharArrayBuffer buffer) {
		cursor.copyStringToBuffer(cursor.getColumnIndex(columnName), buffer);
	}
	
	public void copyStringToBuffer(String columnName, CharArrayBuffer buffer) {
		copyStringToBuffer(cursor, columnName, buffer);
	}

	public static short getShort(Cursor cursor, String columnName) {
		return cursor.getShort(cursor.getColumnIndex(columnName));
	}

	public short getShort(String columnName) {
		return getShort(cursor,columnName);
	}

	public static int getInt(Cursor cursor,String columnName) {
		return cursor.getInt(cursor.getColumnIndex(columnName));
	}
	
	public int getInt(String columnName) {
		return getInt(cursor, columnName);
	}

	public static long getLong(Cursor cursor, String columnName) {
		return cursor.getLong(cursor.getColumnIndex(columnName));
	}

	public long getLong(String columnName) {
		return getLong(cursor, columnName);
	}

	public static float getFloat(Cursor cursor, String columnName) {
		return cursor.getFloat(cursor.getColumnIndex(columnName));
	}

	public float getFloat(String columnName) {
		return getFloat(cursor, columnName);
	}

	public static double getDouble(Cursor cursor, String columnName) {
		return cursor.getDouble(cursor.getColumnIndex(columnName));
	}

	public double getDouble(String columnName) {
		return getDouble(cursor, columnName);
	}

	public static int getType(Cursor cursor, String columnName) {
		return cursor.getType(cursor.getColumnIndex(columnName));
	}

	public int getType(String columnName) {
		return getType(cursor, columnName);
	}

	public static boolean isNull(Cursor cursor, String columnName) {
		return cursor.isNull(cursor.getColumnIndex(columnName));
	}

	public boolean isNull(String columnName) {
		return isNull(cursor, columnName);
	}
}
