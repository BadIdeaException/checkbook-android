package heger.christian.ledger.control.rules;

import heger.christian.ledger.model.EntryParameterObject;
import heger.christian.ledger.model.RuleParameterObject;

public class RuleFactory {
	
	/**
	 * Creates a new rule from the passed entry's caption and category.
	 */
	public static RuleParameterObject createRuleFromEntry(EntryParameterObject entry) {
		return new RuleParameterObject(null, entry.caption, entry.category);
	}
}
