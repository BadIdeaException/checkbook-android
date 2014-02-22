package heger.christian.ledger.control;

/**
 * Helper class to convert month/year combinations to months elapsed since January 1970 and vice versa.
 * @author chris
 *
 */
public class MonthsElapsedCalculator {

	/**
	 * Calculates the number of months that have elapsed since January 1970 (the start of the
	 * Unix epoch) until the given month and year, inclusively, i.e. 
	 * <code>getMonthsElapsed(0,1970)==1</code>.
	 * @param month - Zero-based index of the month to calculate for. The {@link java.util.Calendar#JANUARY}
	 * to  {@link java.util.Calendar#DECEMBER} constants can be used.
	 * @param year - Year to calculate for. 
	 * @return The number of months between January 1970 and the passed month/year
	 */
	public static int getMonthsElapsed(int month, int year) {
		return month + 1 + 12 * (year - 1970);
	}
	
	/**
	 * Given the number of elapsed months since January 1970, calculate the corresponding
	 * month of the year. The result is zero-based. 
	 */
	public static int getMonth(int monthsElapsed) {
		monthsElapsed--; // This value isn't zero-based, so make it zero-based
		return monthsElapsed % 12; 
	}
	
	/**
	 * Given the number of elapsed months since January 1970, calculate the corresponding year.
	 */
	public static int getYear(int monthsElapsed) {
		monthsElapsed--; // This value isn't zero-based, so make it zero-based
		return 1970 + monthsElapsed / 12;
	}
}
