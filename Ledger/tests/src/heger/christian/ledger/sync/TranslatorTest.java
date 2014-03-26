package heger.christian.ledger.sync;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.MatrixCursor;
import android.test.AndroidTestCase;

public class TranslatorTest extends AndroidTestCase {
	private Translator translator;

	private static final String COL_STRING = "a_string";
	private static final String COL_INT = "an_int";
	private static final String COL_LONG = "a_long";
	// No floats since they aren't supported neither by cursor nor JSON (see documentation for Translator.translate(Cursor, String[]))
	private static final String COL_DOUBLE = "a_double";
	private static final String COL_NULL = "a_null";

	private static final String A_STRING = "a string";
	private static final int AN_INT = Integer.MAX_VALUE;
	private static final long A_LONG = Long.MAX_VALUE;
	private static final double A_DOUBLE = Double.MAX_VALUE / 2;
	private static final Object A_NULL = null;

	@Override
	public void setUp() {
		translator = new Translator();
	}

	public void testTranslateToJson() throws JSONException {
		MatrixCursor cursor = new MatrixCursor(new String[] {
				COL_STRING, COL_INT, COL_LONG, COL_DOUBLE, COL_NULL
		});
		cursor.addRow(new Object[] {
				A_STRING, AN_INT, A_LONG, A_DOUBLE, A_NULL
		});

		// Should be null when cursor is before first row
		assertNull(translator.translate(cursor, null));
		cursor.moveToFirst();

		JSONObject json = translator.translate(cursor, null);
		assertEquals(json.get(COL_STRING), A_STRING);
		json.get(COL_INT).equals(AN_INT);
		assertEquals(json.get(COL_INT), AN_INT);
		assertEquals(json.get(COL_LONG), A_LONG);
		assertEquals(json.get(COL_DOUBLE), A_DOUBLE);
		assertTrue(json.isNull(COL_NULL));
	}

	public void testTranslateToJsonWithProjection() throws JSONException {
		MatrixCursor cursor = new MatrixCursor(new String[] {
				COL_STRING, COL_INT, COL_LONG, COL_DOUBLE, COL_NULL
		});
		cursor.addRow(new Object[] {
				A_STRING, AN_INT, A_LONG, A_DOUBLE, A_NULL
		});

		JSONObject json = null;
		// Should be null when cursor is before first row
		assertNull(translator.translate(cursor, null));
		cursor.moveToFirst();

		json = translator.translate(cursor, new String[] { COL_STRING, COL_LONG, COL_DOUBLE });
		assertEquals(json.get(COL_STRING), A_STRING);
		assertEquals(json.get(COL_LONG), A_LONG);
		assertEquals(json.get(COL_DOUBLE), A_DOUBLE);
		assertFalse(json.has(COL_INT));
		assertFalse(json.has(COL_NULL));
	}

	public void testTranslateFromJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put(COL_STRING, A_STRING);
		json.put(COL_INT, AN_INT);
		json.put(COL_LONG, A_LONG);
		json.put(COL_DOUBLE, A_DOUBLE);
		json.putOpt(COL_NULL, JSONObject.NULL);

		ContentValues values = translator.translate(json);
		assertEquals(values.get(COL_STRING), A_STRING);
		assertEquals(values.get(COL_INT), AN_INT);
		assertEquals(values.get(COL_LONG), A_LONG);
		assertEquals(values.get(COL_DOUBLE), A_DOUBLE);
		assertEquals(values.get(COL_NULL), A_NULL);

		json = new JSONObject("{}");
		values = translator.translate(json);
		assertTrue(values.size() == 0);

		json = new JSONObject();
		json.put(COL_STRING, new JSONObject());
		assertFalse(translator.translate(json).containsKey(COL_STRING));
	}
}
