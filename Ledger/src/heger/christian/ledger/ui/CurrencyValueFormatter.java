package heger.christian.ledger.ui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;


public class CurrencyValueFormatter {
	private static DecimalFormat nf = (DecimalFormat) NumberFormat.getCurrencyInstance();
	
	public static String format(int value) {
		return nf.format(((float) value)/100.0f);
	}

	public static int parse(String string) throws ParseException {
		return (int) (100.0f * nf.parse(string).floatValue());
	}
	
	/**
	 * Does a very lenient attempt to parse the passed string into an integer.
	 * It does not perform any checking for input validity. As long as the 
	 * input contains any digits at all, a result will be returned.0
	 * @param string - The input to parse
	 * @return The input parsed into an integer
	 */
	public static int parseLenient(String string) {
		DecimalFormatSymbols symbols = nf.getDecimalFormatSymbols();
		// Want to keep only digits and the monetary decimal separator sign
		String pattern = "[^\\d\\" + symbols.getMonetaryDecimalSeparator() + "]"; 
		// Remove anything that doesn't match that
		string = string.replaceAll(pattern, "");
		// Replace the locale-dependent monetary decimal separator by a dot
		string = string.replace(symbols.getMonetaryDecimalSeparator(), '.');
		// Parse into a double and cast for result
		return (int) (Double.valueOf(string) * 100.0f);		
	}

}
