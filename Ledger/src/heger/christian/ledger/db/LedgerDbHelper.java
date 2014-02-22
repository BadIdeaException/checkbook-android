package heger.christian.ledger.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class LedgerDbHelper extends SQLiteOpenHelper {
	public static final String DEFAULT_PRAGMA_FOREIGN_KEYS = "ON";
	
	public static abstract class MonthsContract extends heger.christian.ledger.providers.MonthsContract {
		protected static final String SQL_CREATE = 
				"CREATE VIEW " + TABLE_NAME + " AS " +
				"SELECT strftime('%m'," + EntryContract.COL_NAME_DATETIME + 
						")+12*(strftime('%Y'," + EntryContract.COL_NAME_DATETIME + ")-1970) AS " + _ID
						+ ",  sum(value) AS " + COL_NAME_VALUE
						+ " FROM " + EntryContract.TABLE_NAME
						+ " GROUP BY " + _ID;
		
		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
		}	
	}
	
	public static abstract class EntryContract extends heger.christian.ledger.providers.EntryContract {
		protected static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE_NAME + " (" +
				BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				COL_NAME_DATETIME + " DATETIME NOT NULL," + 
				COL_NAME_CAPTION + " TEXT," +
				COL_NAME_VALUE + " INTEGER NOT NULL," + 
				COL_NAME_DETAILS + " TEXT," +
				COL_NAME_CATEGORY + " NOT NULL," +
				"FOREIGN KEY (" + COL_NAME_CATEGORY + ") REFERENCES " + 
					CategoryContract.TABLE_NAME + "(" + CategoryContract._ID + ") " +
					"ON UPDATE CASCADE ON DELETE CASCADE);";
		
		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
		}
	}
	
	public static abstract class EntryMetaDataContract extends heger.christian.ledger.providers.EntryMetadataContract {
		protected static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE_NAME + "(" +
				BaseColumns._ID + " INTEGER PRIMARY KEY," + 
				COL_NAME_USER_CHOSEN_CATEGORY + "," + 
				"FOREIGN KEY (" + _ID + ") REFERENCES " +
					EntryContract.TABLE_NAME + "(" + EntryContract._ID + ") " +
					"ON UPDATE CASCADE ON DELETE CASCADE);";
		
		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
		}
	}
	
	public static abstract class CategoryContract extends heger.christian.ledger.providers.CategoryContract {
		protected static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE_NAME + " (" +
				BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				COL_NAME_CAPTION + " TEXT," +
				COL_NAME_SUPERCATEGORY + " NOT NULL," +
				"FOREIGN KEY (" + COL_NAME_SUPERCATEGORY + ") REFERENCES " + 
					SupercategoryContract.TABLE_NAME + "(" + SupercategoryContract._ID + ") " +
					"ON UPDATE CASCADE ON DELETE CASCADE);";

		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
		}
	}
	
	public static abstract class SupercategoryContract extends heger.christian.ledger.providers.SupercategoryContract {
		protected static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE_NAME + " (" +
				BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
				COL_NAME_CAPTION + " TEXT);";
			
		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
		}
	}
	
	public static abstract class RulesContract extends heger.christian.ledger.providers.RulesContract {
		protected static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE_NAME + " (" + 
				BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
				COL_NAME_ANTECEDENT + " TEXT," +
				COL_NAME_CONSEQUENT + " NOT NULL," + 
				"FOREIGN KEY (" + COL_NAME_CONSEQUENT + ") REFERENCES " + 
					CategoryContract.TABLE_NAME + "(" + CategoryContract._ID + ") " +
					"ON UPDATE CASCADE ON DELETE CASCADE);";
		
		public static void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE);
		}
	}

	public static final int DB_VERSION = 1;
	public static final String DB_NAME = "ledger.db";
	
	public LedgerDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {		
		SupercategoryContract.createTable(db);
		CategoryContract.createTable(db);
		EntryContract.createTable(db);
		MonthsContract.createTable(db);
		EntryMetaDataContract.createTable(db);
		RulesContract.createTable(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
	@Override
	public void onOpen(SQLiteDatabase db) {
		db.execSQL("PRAGMA foreign_keys = " + DEFAULT_PRAGMA_FOREIGN_KEYS);
	}
}
