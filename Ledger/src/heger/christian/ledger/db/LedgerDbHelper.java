package heger.christian.ledger.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * <code>SQLiteOpenHelper</code> for the Ledger database. It uses the singleton pattern to support
 * multiple content providers accessing the same database.
 */
public class LedgerDbHelper extends SQLiteOpenHelper {
	public static final String DEFAULT_PRAGMA_FOREIGN_KEYS = "ON";

	public static abstract class MonthContract extends heger.christian.ledger.providers.MonthContract {
		protected static final String SQL_CREATE =
				"create view " + TABLE_NAME + " as " +
				"select strftime('%m'," + EntryContract.COL_NAME_DATETIME +
						")+12*(strftime('%Y'," + EntryContract.COL_NAME_DATETIME + ")-1970) as " + _ID
						+ ",  sum(value) as " + COL_NAME_VALUE
						+ " from " + EntryContract.TABLE_NAME
						+ " group by " + _ID;

		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
		}
	}

	public static abstract class EntryContract extends heger.christian.ledger.providers.EntryContract {
		protected static final String SQL_CREATE =
				"create table " + TABLE_NAME + " (" +
				BaseColumns._ID + " integer primary key," +
				COL_NAME_DATETIME + " datetime not null," +
				COL_NAME_CAPTION + " text," +
				COL_NAME_VALUE + " integer not null," +
				COL_NAME_DETAILS + " text," +
				COL_NAME_CATEGORY + " not null," +
				"foreign key (" + COL_NAME_CATEGORY + ") references " +
					CategoryContract.TABLE_NAME + "(" + CategoryContract._ID + ") " +
					"on update cascade on delete cascade);";

		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
		}
	}

	public static abstract class EntryMetaDataContract extends heger.christian.ledger.providers.EntryMetadataContract {
		protected static final String SQL_CREATE =
				"create table " + TABLE_NAME + "(" +
				BaseColumns._ID + " integer primary key," +
				COL_NAME_USER_CHOSEN_CATEGORY + "," +
				"foreign key (" + _ID + ") references " +
					EntryContract.TABLE_NAME + "(" + EntryContract._ID + ") " +
					"on update cascade on delete cascade);";

		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
		}
	}

	public static abstract class CategoryContract extends heger.christian.ledger.providers.CategoryContract {
		protected static final String SQL_CREATE =
				"create table " + TABLE_NAME + " (" +
				BaseColumns._ID + " integer primary key," +
				COL_NAME_CAPTION + " text);";

		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
		}
	}

	public static abstract class RuleContract extends heger.christian.ledger.providers.RuleContract {
		protected static final String SQL_CREATE =
				"create table " + TABLE_NAME + " (" +
				BaseColumns._ID + " integer primary key," +
				COL_NAME_ANTECEDENT + " text," +
				COL_NAME_CONSEQUENT + " not null," +
				"foreign key (" + COL_NAME_CONSEQUENT + ") references " +
					CategoryContract.TABLE_NAME + "(" + CategoryContract._ID + ") " +
					"on update cascade on delete cascade);";

		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
		}
	}

	public static abstract class KeyGenerationContract extends heger.christian.ledger.providers.MetaContentProvider.KeyGenerationContract {
		protected static final String SQL_CREATE =
				"create table " + TABLE_NAME + " (" +
				COL_NAME_NEXT_KEY + " integer not null, " +
				COL_NAME_UPPER_BOUND + " integer not null); ";
		protected static final String SQL_CREATE_TRIGGER =
				// Create trigger that will ensure not more than one row is ever present
				"create trigger " + KeyGenerationContract.TABLE_NAME + "_insert before insert on " + TABLE_NAME +
				" for each row begin delete from " + TABLE_NAME + "; end; ";
		protected static final String SQL_POPULATE =
				// Insert initial values that will force acquisition of new series from server
				"insert into " + TABLE_NAME + " values (1,0);";

		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
			db.execSQL(SQL_CREATE_TRIGGER);
			db.execSQL(SQL_POPULATE);
		}
	}

	public static abstract class JournalContract extends heger.christian.ledger.providers.MetaContentProvider.JournalContract {
		protected static final String SQL_CREATE =
				"create table " + TABLE_NAME + " (" +
				// Sequence number, must not be null and must be unique - primary key
				COL_NAME_SEQUENCE_NUMBER + " integer primary key, " +
				// Table name, must not be null
				COL_NAME_TABLE + " text not null, " +
				// Target row primary key, target row existence cannot be formulated here
				COL_NAME_ROW + " integer not null, " +
				// Target column, MAY BE EMPTY for creation and deletion operations
				COL_NAME_COLUMN + " text, " +
				// Operation type, may be one of C, U, D
				COL_NAME_OPERATION + " char not null check (lower(" + COL_NAME_OPERATION + ") = \"c\"" +
				"or lower(" + COL_NAME_OPERATION + ") = \"u\" or lower(" + COL_NAME_OPERATION + ") = \"d\"));";
		protected static final String SQL_CREATE_TRIGGER =
				// Need to check AFTER the insert - otherwise the sequence number won't be available yet
				"create trigger ensure_valid_sequence_number after insert on " + TABLE_NAME + " " +
				"for each row when (new." + COL_NAME_SEQUENCE_NUMBER + " < " +
				 "(select max(" + SequenceAnchorContract.COL_NAME_SEQUENCE_ANCHOR + ") " +
				"from " + SequenceAnchorContract.TABLE_NAME + ")) " +
				 // Delete the illegal entry again
				 "begin delete from " + TABLE_NAME + " where " + COL_NAME_SEQUENCE_NUMBER + "=new." + COL_NAME_SEQUENCE_NUMBER + "; " +
				 "select raise(fail,\"sequence number too low\"); end;";
;
		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
			db.execSQL(SQL_CREATE_TRIGGER);
		}
	}

	public static abstract class RevisionTableContract extends heger.christian.ledger.providers.MetaContentProvider.RevisionTableContract {
		protected static final String SQL_CREATE =
			"create table " + TABLE_NAME + " (" +
			COL_NAME_TABLE + " text not null, " +
			COL_NAME_ROW + " integer not null, " +
			COL_NAME_COLUMN + " text not null, " +
			COL_NAME_REVISION + " integer not null, " +
			"primary key (" + COL_NAME_TABLE + "," + COL_NAME_ROW + "," + COL_NAME_COLUMN + ") " +
			"on conflict replace)";
		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
		}
	}

	public static abstract class SequenceAnchorContract extends heger.christian.ledger.providers.MetaContentProvider.SequenceAnchorContract {
		protected static final String SQL_CREATE =
				"create table " + TABLE_NAME + " (" +
				COL_NAME_SEQUENCE_ANCHOR + " integer not null);";
		protected static final String SQL_CREATE_TRIGGER =
				// Create trigger that will ensure not more than one row is ever present
				"create trigger " + SequenceAnchorContract.TABLE_NAME + "_insert before insert on " + TABLE_NAME +
				" for each row begin delete from " + TABLE_NAME + "; end; ";
		protected static final String SQL_POPULATE =
				// Insert initial value: First synchronization to start from sequence number 0
				"insert into " + TABLE_NAME + " values (0);";

		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
			db.execSQL(SQL_CREATE_TRIGGER);
			db.execSQL(SQL_POPULATE);
		}
	}

	public static final int DB_VERSION = 1;
	public static final String DB_NAME = "ledger.db";

	public LedgerDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		CategoryContract.createTable(db);
		EntryContract.createTable(db);
		MonthContract.createTable(db);
		EntryMetaDataContract.createTable(db);
		RuleContract.createTable(db);
		// Metadata tables:
		KeyGenerationContract.createTable(db);
		SequenceAnchorContract.createTable(db);
		JournalContract.createTable(db);
		RevisionTableContract.createTable(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		db.execSQL("PRAGMA foreign_keys = " + DEFAULT_PRAGMA_FOREIGN_KEYS);
	}
}
