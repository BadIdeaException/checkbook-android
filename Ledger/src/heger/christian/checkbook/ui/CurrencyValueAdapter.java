package heger.christian.checkbook.ui;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class CurrencyValueAdapter {
	private static DecimalFormat nf = (DecimalFormat) NumberFormat.getIntegerInstance();
	static {
		nf.applyLocalizedPattern("0.00\u00A4");
	}
	
	public static String format(int value) {
		return nf.format(value);
	}

}
