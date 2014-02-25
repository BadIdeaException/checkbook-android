package heger.christian.ledger.textwatchers;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.regex.Pattern;

import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;

/** 
 * This class, when attached to an <code>EditView</code>, formats its input to as a localized currency. 
 * Formatting is done on the fly.
 * The <code>EditView</code> is assumed to accept all relevant characters, including non-digit ones. It is recommended 
 * to set its input type to number so as to prevent the user from entering any illegal characters. 
 * @author chris
 *
 */
public class CurrencyFormattingTextWatcher implements TextWatcher {
	private static class FormatResult {
		public final String s;
		public final int cursor;
		public FormatResult(String s, int cursor) {
			this.s = s;
			this.cursor = cursor;
		}
	}
	
	private boolean selfChange = false;
	private static final Pattern NON_DIGITS = Pattern.compile("\\D");
	private DecimalFormat cf;
	private DecimalFormatSymbols symbols;
	
	private String lead, trail;
	
	/**
	 * Create a new instance using the passed <code>DecimalFormat</code> for formatting.
	 * @param cf - The currency formatter to use
	 */
	public CurrencyFormattingTextWatcher(DecimalFormat cf) {
		this.cf = cf;
		symbols = cf.getDecimalFormatSymbols();
		
		// Since there is no easy way to find out if the currency symbol is preceding
		// or succeeding the numeric value when formatting, some work is necessary to
		// extract this information from the formatting pattern
		String pattern = cf.toPattern();
		// Find the position of the currency symbol in the pattern
		int symbol = pattern.indexOf('\u00A4');
		// Find the position of the first digit symbol in the pattern
		int firstDigit = Math.min(pattern.indexOf('0'), pattern.indexOf('#'));
		// Find the position of the last digit symbol in the pattern
		int lastDigit = Math.max(pattern.lastIndexOf('0'), pattern.lastIndexOf('#'));
		// The lead is everything between the symbol and the first digit, 
		// or empty if the symbol comes after the first digit 
		lead = symbol > firstDigit ? "" : pattern.substring(symbol,firstDigit);
		// Replace the generic currency symbol with the actual one in the lead
		lead = lead.replace("\u00A4", symbols.getCurrencySymbol());
		// Analogous to lead
		trail = symbol < lastDigit ? "" : pattern.substring(lastDigit + 1, symbol + 1);
		trail = trail.replace("\u00A4", symbols.getCurrencySymbol());
	}
	
	/**
	 * Create a new <code>CurrencyFormattingTextWatcher</code> using the 
	 * <code>NumberFormat.getCurrencyInstance()</code> as the currency formatter.
	 */
	public CurrencyFormattingTextWatcher() {
		this((DecimalFormat) NumberFormat.getCurrencyInstance());
	}
		
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	public void afterTextChanged(Editable s) {
		if (!selfChange) {
			selfChange = true;
			FormatResult formatted = reformat(s, Selection.getSelectionEnd(s));		
			// Replace the text in s with the formatted version
			s.replace(0, s.length(), formatted.s, 0, formatted.s.length());
			// Set cursor to the position calculated during reformatting
			Selection.setSelection(s, formatted.cursor);
			
			// Adjust cursor to never be within the lead or trail
			int selectionEnd = Selection.getSelectionEnd(s);
			if (selectionEnd > -1 && selectionEnd > s.length() - trail.length())
				Selection.setSelection(s, s.length() - trail.length());
			int selectionStart = Selection.getSelectionStart(s);
			if (selectionStart > -1 && selectionStart < lead.length())
				Selection.setSelection(s, lead.length());
		}
		selfChange = false;
	}

	/**
	 * Helper method to do the actual formatting. 
	 * @param s - The input to format
	 * @param cursor - The position the cursor is currently in
	 * @return A <code>FormatResult</code> consisting of the formatted input and
	 * the recalculated position of the cursor.
	 */
	private FormatResult reformat(CharSequence s, int cursor) {
		// Split input string at the cursor
		String left = s.subSequence(0, cursor).toString();
		String right = s.subSequence(cursor, s.length()).toString();		
		// Create a new string builder, initialized to s stripped to only digits
		// Start with only the part preceding the cursor for this
		StringBuilder builder = new StringBuilder(left.replaceAll(NON_DIGITS.toString(), ""));
		// Move the cursor one to the left for every character that was deleted 
		cursor -= left.length() - builder.length();
		// Append the part after the cursor. No need to shift the cursor here.
		builder.append(right.replaceAll(NON_DIGITS.toString(), ""));
		
		// Pad with leading zeros in case the sequence is shorter than the number of fraction digits 
		// plus one (for the leading zero before the decimal separator)
		for (int i = builder.length(); i < cf.getCurrency().getDefaultFractionDigits() + 1; i++) {
			builder.insert(0, symbols.getZeroDigit());
			// Move the cursor to the right for every padded zero unless it was at the beginning of the string before
			if (cursor > 0) cursor++;
		}
		// Strip all leading zeros except one
		while (builder.length() > cf.getCurrency().getDefaultFractionDigits() + 1 && builder.charAt(0) == symbols.getZeroDigit()) {
			builder.deleteCharAt(0);
			// Move the cursor to the left for every removed zero unless it was at the beginning of the string before
			if (cursor > 0) cursor--;
		}
		
		// Get the length of the integral part of the number
		int integralLength = builder.length() - cf.getCurrency().getDefaultFractionDigits();
		// Insert the decimal separator
		builder.insert(builder.length() - cf.getCurrency().getDefaultFractionDigits(), symbols.getMonetaryDecimalSeparator());
		// Move the cursor to the right for the decimal separator unless it was within the integral part of the entered number before
		if (cursor > integralLength) cursor++;
		
		// Insert grouping separators
		// While the integral length, after subtracting the grouping size from it, is larger than zero, there
		// is at least one more grouping separator to insert (at position integral length - grouping size)
		while ((integralLength -= ((DecimalFormat) cf).getGroupingSize()) > 0) {
			builder.insert(integralLength, symbols.getGroupingSeparator());
			// If the insertion happened to the left of the cursor, move it to the right
			if (cursor > integralLength) cursor++;
		}
		
		// Insert the lead and move the cursor accordingly
		builder.insert(0, lead);
		cursor += lead.length();
		// Insert the trail. No need to move the cursor, since we don't want to end up in the trail 
		builder.append(trail);
		
		return new FormatResult(builder.toString(),cursor);				
	}
}
