package heger.christian.ledger.ui.login;

import heger.christian.ledger.R;
import heger.christian.ledger.accounts.AuthenticationFailedException;
import heger.christian.ledger.accounts.Authenticator;
import heger.christian.ledger.accounts.ServerAuthenticator;
import heger.christian.ledger.accounts.TokenSet;
import heger.christian.ledger.network.TruststoreException;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends AccountAuthenticatorActivity {
	private static final String LOG_TAG = LoginActivity.class.getSimpleName();

	public static final String ARG_ADD_ACCOUNT = "add_account";
	public static final String ARG_ACCOUNT = "account";
	public static final String ARG_ACCOUNT_TYPE = AccountManager.KEY_ACCOUNT_TYPE;

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginTask authTask = null;

	// Values for email and password at the time of the login attempt.
	private String username;
	private String password;

	// UI references.
	private EditText editUser;
	private EditText editPassword;
	private TextView txtStatus;
	private View viewLoginForm;
	private View viewLoginStatus;
	private TextView txtLoginStatusMessage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.actvy_login);

		// Set up the login form
		editUser = (EditText) findViewById(R.id.edit_user);
		editPassword = (EditText) findViewById(R.id.edit_password);
		editPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.ime_enter || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});

		txtStatus = (TextView) findViewById(R.id.txt_status);

		if (!getIntent().getBooleanExtra(ARG_ADD_ACCOUNT, false)) {
			Account account = getIntent().getParcelableExtra(ARG_ACCOUNT);
			username = account.name;
			editUser.setText(username);
		} else {
			if (AccountManager.get(this).getAccountsByType(getIntent().getStringExtra(ARG_ACCOUNT_TYPE)).length > 0) {
				editUser.setEnabled(false);
				editPassword.setEnabled(false);
				findViewById(R.id.btn_sign_in).setEnabled(false);
				txtStatus.setText(R.string.err_too_many_accounts);
				txtStatus.setVisibility(View.VISIBLE);
			}
		}
		viewLoginForm = findViewById(R.id.login_form);
		viewLoginStatus = findViewById(R.id.login_status);
		txtLoginStatusMessage = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.btn_sign_in).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (authTask != null) {
			return;
		}

		// Reset errors.
		txtStatus.setVisibility(View.GONE);

		// Store values at the time of the login attempt.
		username = editUser.getText().toString();
		password = editPassword.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check that required fields are filled in
		if (TextUtils.isEmpty(username) && TextUtils.isEmpty(password)) {
			txtStatus.setText(R.string.err_username_and_password_required);
			focusView = editUser;
			cancel = true;
		} else if (TextUtils.isEmpty(username)) {
			txtStatus.setText(R.string.err_username_required);
			focusView = editUser;
			cancel = true;
		} else if (TextUtils.isEmpty(password)) {
			txtStatus.setText(getString(R.string.err_password_required));
			focusView = editPassword;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error. Show error text.
			txtStatus.setVisibility(View.VISIBLE);
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			txtLoginStatusMessage.setText(R.string.login_progress_signing_in);
			showProgress(true);
			authTask = new UserLoginTask();
			authTask.execute((Void) null);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			viewLoginStatus.setVisibility(View.VISIBLE);
			viewLoginStatus.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							viewLoginStatus.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			viewLoginForm.setVisibility(View.VISIBLE);
			viewLoginForm.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							viewLoginForm.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			viewLoginStatus.setVisibility(show ? View.VISIBLE : View.GONE);
			viewLoginForm.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	private void failLogin(Exception x) {
		Bundle result = new Bundle();
		if (x instanceof IOException) {
			result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_NETWORK_ERROR);
		}
		setAccountAuthenticatorResult(result);
		setResult(RESULT_OK);
		finish();
	}

    private void finishLogin(TokenSet tokens) {
     	Bundle extras = getIntent().getExtras();
		final AccountManager manager = AccountManager.get(this);
    	final Account account;
		if (extras.getBoolean(ARG_ADD_ACCOUNT, false)) {
    		account = new Account(username, extras.getString(AccountManager.KEY_ACCOUNT_TYPE));
    		manager.addAccountExplicitly(account, null, null);
    	} else {
    		account = extras.getParcelable(ARG_ACCOUNT);
    	}

		// Store access token if one was provided. Note that this should always be the case anyway,
		// otherwise the login would have been unsuccessful
		if (tokens.hasAccess())
			manager.setAuthToken(account, Authenticator.TOKEN_TYPE_ACCESS, tokens.access);
		// Store refresh token if one was provided.
		if (tokens.hasRefresh())
			manager.setAuthToken(account, Authenticator.TOKEN_TYPE_REFRESH, tokens.refresh);

    	final Intent intent = new Intent();
    	if (tokens.hasAccess()) {
    		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
    		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);
    		intent.putExtra(AccountManager.KEY_AUTHTOKEN, tokens.access);
//    		intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, true);
    	}

    	setAccountAuthenticatorResult(intent.getExtras());
    	setResult(Activity.RESULT_OK, intent);
    	finish();
    }

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, Object> {
		/**
		 * Will return the token set obtained from authentication, or the exception
		 * if there was one.
		 */
		@Override
		protected Object doInBackground(Void... params) {
			ServerAuthenticator serverAuthenticator = new ServerAuthenticator(LoginActivity.this);
			try {
				return serverAuthenticator.authenticate(username, password);
			} catch (IOException x) {
				return x;
			} catch (AuthenticationFailedException x) {
				return x;
			} catch (TruststoreException x) {
				return x;
			}
		}

		@Override
		protected void onPostExecute(final Object result) {
			authTask = null;
			showProgress(false);

			if (result instanceof TokenSet) {
				finishLogin((TokenSet) result);
			} else if (result instanceof Exception) {
				if (result instanceof AuthenticationFailedException) {
					// Authentication incorrect
					if (((AuthenticationFailedException) result).getErrorCode() == ServerAuthenticator.ERR_INVALID_GRANT) {
						txtStatus.setText(R.string.err_incorrect_credentials);
						txtStatus.setVisibility(View.VISIBLE);
						editPassword.requestFocus();
					}
				} else if (result instanceof IOException) {
					String message = getResources().getString(R.string.err_network_error);
					// Log message as error
					Log.e(LOG_TAG, ((Exception) result).getLocalizedMessage());
					txtStatus.setText(message);
					txtStatus.setVisibility(View.VISIBLE);
//					failLogin((Exception) result);
				} else {
					failLogin((Exception) result);
				}
			}
		}

		@Override
		protected void onCancelled() {
			authTask = null;
			showProgress(false);
		}
	}
}
