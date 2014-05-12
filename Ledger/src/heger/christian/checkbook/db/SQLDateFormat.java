package heger.christian.checkbook.db;

import java.text.SimpleDateFormat;
import java.util.Locale;

@SuppressWarnings("serial")
public class SQLDateFormat extends SimpleDateFormat {
	public SQLDateFormat() {
		super("yyyy-MM-dd HH:mm:ss", Locale.US); // Locale.US to ensure we get ASCII characters (see SimpleDateFormat javadoc)
	}
}
