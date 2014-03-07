package heger.christian.ledger.accounts;

import android.text.TextUtils;

/**
 * Represents a set of tokens as issued by the authorization server. It consists of an access token and an
 * refresh token, although neither one need be present (for instance, the access token may have expired, but a
 * refresh token is still present).
 * 
 * Instances of this class are immutable. This class is final.
 */
public final class TokenSet {
	public final String access;
	public final String refresh;
	
	/**
	 * Constructs a new token set with the supplied access and refresh token.
	 * @param access - The access token to use. 
	 * @param refresh - The refresh token. This may be <code>null</code>.
	 */
	public TokenSet(String access, String refresh) {
		this.access = access;
		this.refresh = refresh;
	}
	
	public boolean hasAccess() {
		return !TextUtils.isEmpty(access);
	}
	
	public boolean hasRefresh() {
		return !TextUtils.isEmpty(refresh);
	}
	
	@Override
	public String toString() {
		return "Access token: " + access + "; refresh token: " + refresh;
	}
}
