package heger.christian.ledger.accounts;

import heger.christian.ledger.network.TruststoreException;
import heger.christian.ledger.ui.login.LoginActivity;

import java.io.IOException;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class Authenticator extends AbstractAccountAuthenticator {
	private final Context context;

	public static final String TOKEN_TYPE_ACCESS = "access";
	public static final String TOKEN_TYPE_REFRESH = "refresh";

	public static final String ACCOUNT_TYPE = "heger.christian.ledger";

	public Authenticator(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		throw new UnsupportedOperationException("editProperties is not supported");
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options)
			throws NetworkErrorException {
		final Intent intent = new Intent(context, LoginActivity.class);
		intent.putExtra(LoginActivity.ARG_ADD_ACCOUNT, true);
	    intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, accountType);
	    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
	    final Bundle bundle = new Bundle();
	    bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options)
			throws NetworkErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
			throws NetworkErrorException {

		Bundle bundle = new Bundle();
		AccountManager manager = AccountManager.get(context);

		// Get the currently stored tokens from the account manager, if any
		TokenSet tokens = new TokenSet(manager.peekAuthToken(account, TOKEN_TYPE_ACCESS), manager.peekAuthToken(account, TOKEN_TYPE_REFRESH));

		// A token of the requested type was found, return it
		if (authTokenType.equals(TOKEN_TYPE_ACCESS) && tokens.hasAccess()) {
			bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
			bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
			bundle.putString(AccountManager.KEY_AUTHTOKEN, tokens.access);
		} else if (authTokenType.equals(TOKEN_TYPE_REFRESH) && tokens.hasRefresh()) {
			bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
			bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
			bundle.putString(AccountManager.KEY_AUTHTOKEN, tokens.refresh);
		}

		// There is neither an access nor a refresh token. User has to log in
		if (!tokens.hasAccess() && !tokens.hasRefresh()) {
			Intent intent = new Intent(context, LoginActivity.class);
			intent.putExtra(LoginActivity.ARG_ACCOUNT, account);
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response); // As per AbstractAccountAuthenticator doc
			bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		}

		// An access token is requested. We don't have one, but there is a refresh token
		// Use it to get a new access token
		if (authTokenType.equals(TOKEN_TYPE_ACCESS) && !tokens.hasAccess() && tokens.hasRefresh()) {
			try {
				TokenSet newTokens = new ServerAuthenticator(context).authenticate(tokens.refresh);
				// Got to here: authentication was successful

				// Invalidate the refresh token we just used
				manager.invalidateAuthToken(account.type, tokens.refresh);
				// Populate the manager with the new tokens we just received
				manager.setAuthToken(account, TOKEN_TYPE_ACCESS, newTokens.access);
				if (newTokens.hasRefresh()) {
					manager.setAuthToken(account, TOKEN_TYPE_REFRESH, newTokens.refresh);
				}
				// Prepare result bundle
				bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
				bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
				bundle.putString(AccountManager.KEY_AUTHTOKEN, newTokens.access);
			} catch (AuthenticationFailedException x) {
				// Authentication failed - notify the user, and invalidate the refresh token we tried with
				bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_INVALID_RESPONSE);
				bundle.putString(AccountManager.KEY_ERROR_MESSAGE, x.getMessage());
				manager.invalidateAuthToken(account.type, tokens.refresh);
			} catch (TruststoreException x) {
				// Something went wrong setting up the truststore - notify the user, but keep the refresh token
				bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);
				bundle.putString(AccountManager.KEY_ERROR_MESSAGE, x.getMessage());
			} catch (IOException x) {
				// Network exception - notify the user, but keep the refresh token
				bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_NETWORK_ERROR);
				bundle.putString(AccountManager.KEY_ERROR_MESSAGE, x.getMessage());
			}
		}

		return bundle;
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
			throws NetworkErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features)
			throws NetworkErrorException {
		// TODO Auto-generated method stub
		return null;
	}

}
