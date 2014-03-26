package heger.christian.ledger.control.rules;

import heger.christian.ledger.db.CursorAccessHelper;
import heger.christian.ledger.providers.RuleContract;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.database.Cursor;

/**
 * This class is responsible for matching rules obtained from the content provider against a
 * phrase. A phrase is a series of words. A word is a string that is delimited by any of the 
 * characters defined as {@link WORD_DELIMITERS} or the boundaries of the string.
 * <p>
 * A rule, consequently written in the form "r: a => c", consists of three parts:
 * <ol>
 * <li> The <i>rule name r</i>. This is only provided for ease of reference within this documentation
 * and does not translate directly to the underlying storage model. (Note, however, that rules do 
 * store an _id field).
 * <li> The <i>antecedent a</i>, also referred to as the rule body. This is the string that the phrase 
 * must match to satisfy the rule.
 * <li> The <i>consequent c</i>. This points to the category implied when this rule is satisfied.  
 * </ol> 
 * <p>
 * A rule is considered to apply if its antecedent matches at least one word in the phrase. 
 * @author chris
 *
 */
public class RuleMatcher {
	protected static final String WORD_DELIMITERS = " ";
	
	ContentResolver resolver;
	
	public RuleMatcher(ContentResolver resolver) {
		this.resolver = resolver;
	}

	protected String[] prepare(String phrase) {
		return phrase.split(WORD_DELIMITERS);
	}
	
	@SuppressLint("UseSparseArrays")
	/*
	 * (non-javadoc)
	 * Not using SparseIntArray here because we want access to the keyset
	 */
	private Map<Integer,Integer> buildResultFromCursor(Cursor cursor) {		
		Map<Integer,Integer> result = new HashMap<Integer, Integer>();
		if (cursor.getCount() > 0) {
			cursor.moveToPosition(-1);
			CursorAccessHelper helper = new CursorAccessHelper(cursor);
			
			while (cursor.moveToNext()) {
				int id = helper.getInt(RuleContract.COL_NAME_CONSEQUENT);
				int count = result.containsKey(id) ? result.get(id) : 0;
				result.put(id, count + 1);
			}
		}
		return result;
	}
	
	/**
	 * Performs a <em>strict</em> match of all rules against
	 * the words of this matcher's phrase.
	 * <p>
	 * In a strict match, a rule is considered to apply if
	 * the rule's antecedent is an exact match of the word it is 
	 * compared against. Therefore, in a strict match of the rule
	 * "r:foo => c", r will be considered to match the phrases
	 * "foo" and "foo bar", but not "foobar".
	 * @param phrase - The phrase against which to match the rules
	 * @return A map in which the keys are the antecedents of all
	 * rules that matched the phrase and the corresponding values
	 * are the number of rules with that antecedent that were a 
	 * match. For instance, if two rules "r1: x1 => a" and "r2: x2 => a"
	 * both matched the phrase, the result map would contain the 
	 * mapping "(a,2)" (assuming that no other rules matched a). 
	 */
	public Map<Integer,Integer> matchStrict(String phrase) {
		String[] words = prepare(phrase);
		
		if (words.length == 0)
			return Collections.emptyMap();
		
		String where = "(" + RuleContract.COL_NAME_ANTECEDENT + " like ?)";
		for (int i = 1; i < words.length; i++) {
			where += " OR (" + RuleContract.COL_NAME_ANTECEDENT + "like ?)";
		}
		Cursor cursor = resolver.query(
				RuleContract.CONTENT_URI, 
				new String[] { RuleContract.COL_NAME_CONSEQUENT }, 
				where, 
				words, 
				null);
		return buildResultFromCursor(cursor);
	}
}
