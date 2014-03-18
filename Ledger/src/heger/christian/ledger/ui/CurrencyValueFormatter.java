package heger.christian.ledger.ui;

import java.text.DecimalFormat;
import java.text.NumberFormat;


public class CurrencyValueFormatter {
	private static DecimalFormat nf = (DecimalFormat) NumberFormat.getCurrencyInstance();
	private static double fraction = Math.pow(10f, nf.getCurrency().getDefaultFractionDigits());
	
	public static String format(int value) {
		return nf.format(((double) value) / fraction);
	}

	/**
	 * Does a very lenient attempt to parse the passed string into an integer.
	 * It does not perform any checking for input validity. As long as the 
	 * input contains any digits at all, a result will be returned.0
	 * @param string - The input to parse
	 * @return The input parsed into an integer
	 */
	public static int parseLenient(String string) {
		// Want to keep only digits
		String pattern = "\\D"; 
		// Remove anything that doesn't match that
		string = string.replaceAll(pattern, "");
		return Integer.valueOf(string);
	}

}
