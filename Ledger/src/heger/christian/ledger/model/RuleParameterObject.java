package heger.christian.ledger.model;

public class RuleParameterObject {
	public final Long id;
	public String antecedent;
	public Integer consequent;
	
	public RuleParameterObject(Long id, String antecedent, Integer consequent) {
		this.id = id;
		this.antecedent = antecedent;
		this.consequent = consequent; 
	}
}
