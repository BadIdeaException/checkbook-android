package heger.christian.ledger.test;

import heger.christian.ledger.accounts.Authenticator;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import junit.framework.AssertionFailedError;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.AndroidTestCase;

/*
 * 
 * IMPORTANT:
 * 
 * This class requires that the authorization server found in /env/mock-auth-server is running, or this test will fail.
 * 
 */
public class AuthenticatorTest extends AndroidTestCase {
	/**
	 * Helper callback to declutter the testing code from exceptions possible when accessing the 
	 * <code>AccountManagerFuture</code> while testing the results of <code>getAuthToken</code>, specifically
	 * <code>OperationCanceledException</code>, <code>AuthenticatorException</code>, and <code>IOException</code>.
	 * Any of these will be caught, but will result in test failures.
	 * <p>
	 * Put the actual test code in {@link #test(AccountManagerFuture)}. 
	 */
	private abstract class AbstractAuthenticatorTestCallback implements AccountManagerCallback<Bundle> {
		private final String tag;
		public AbstractAuthenticatorTestCallback(String tag) { this.tag = tag; }
		public abstract void test(AccountManagerFuture<Bundle> future) throws OperationCanceledException, AuthenticatorException, IOException;
		@Override
		public void run(AccountManagerFuture<Bundle> future) {
			try {
				test(future);
				// Convert exceptions into test failures	
			} catch (OperationCanceledException x) {
				fail(getTag() + x.getMessage());
			} catch (AuthenticatorException x) {
				fail(getTag() + x.getMessage());
			} catch (IOException x) {
				fail(getTag() + x.getMessage());
			} 
		};
		protected String getTag() { return tag; }
	}

	/**
	 * Helper class to do a version-aware, blocking call to the account manager's <code>getAuthToken</code> method.	 
	 *  
	 * <p><i>Version aware</i> means that depending on the SDK version of the execution environment, either
	 * {@link AccountManager#getAuthToken(Account, String, boolean, AccountManagerCallback, Handler)} is invoked (pre-ICS) or
	 * {@link AccountManager#getAuthToken(Account, String, Bundle, boolean, AccountManagerCallback, Handler)} (ICS and later).
	 * 
	 * <p><i>Blocking</i> means that the current thread will be suspended after the call to the account manager has been made. 
	 * Any supplied callbacks are run on an extra thread. Upon completion of the callback, the suspended thread is automatically
	 * resumed. Any failed assertions in the callback will automatically be propagated to the test case.
	 */
	private class AuthTokenGetter {
		/**
		 * Semaphore to block main thread until completion of callback
		 */
		private Semaphore semaphore;
		/**
		 * Thread to run callback on
		 */
		private HandlerThread thread;
		/**
		 * Raised assertion errors from the callback go here 
		 */
		private AssertionFailedError error = null;
		
		/**
		 * Helper callback to facilitate resuming the main thread and propagating assertion errors.
		 */
		private class ReleasingCallback implements AccountManagerCallback<Bundle> {
			private final AccountManagerCallback<Bundle> callback;
			public ReleasingCallback(AccountManagerCallback<Bundle> callback) {
				this.callback = callback;
			}
			@Override
			public void run(AccountManagerFuture<Bundle> future) {
				try	 {
					if (callback != null) callback.run(future);
				} catch (AssertionFailedError e) {
					error = e;
				} finally {
					semaphore.release();
				}
			}			
		}
		
		/**
		 * Does a version-aware blocking call to <code>AccountManager.getAuthToken</code>.
		 */
		@SuppressWarnings("deprecation")
		@TargetApi(android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		private AccountManagerFuture<Bundle> getAuthToken(Account account, String authTokenType, Bundle options, boolean notify, AccountManagerCallback<Bundle> callback) {
			// Initialize semaphore and callback thread
			thread = new HandlerThread(this.toString());
			thread.start();
			Handler handler = new Handler(thread.getLooper());
			semaphore = new Semaphore(0); // Will block on the first acquisition
			
			AccountManagerFuture<Bundle> future = null;
			try {
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
					future = manager.getAuthToken(account, authTokenType, options, notify, new ReleasingCallback(callback), handler);
				} else {
					future = manager.getAuthToken(account, authTokenType, notify, new ReleasingCallback(callback), handler);
				}
				semaphore.acquire();
			} catch (InterruptedException x) {
				fail("Concurrency error");
			} finally {
				thread.quit();
			}
			if (error != null) {
				throw error;
			}
			return future;
		}		
	}
	private static final String DUMMY_ACCOUNT_NAME = "heger.christian.ledger.tests.dummy_account";

	private static final String ACCESS_TOKEN = "correct access token";
	private static final String REFRESH_TOKEN = "correct refresh token";
	
	private AccountManager manager;
	private Account account;
		
	@Override
	public void setUp() {
		// Set up a dummy account
		manager = AccountManager.get(getContext());
		
		Account[] accounts = manager.getAccountsByType("heger.christian.ledger");
		for (Account acc: accounts) {
			if (acc.name.equals(DUMMY_ACCOUNT_NAME))
				throw new IllegalStateException("Could not create dummy account: An account with this name already exists (" + DUMMY_ACCOUNT_NAME + ")");
		}
		account = new Account(DUMMY_ACCOUNT_NAME, "heger.christian.ledger");
		manager.addAccountExplicitly(account, null, null);
	}
		
	/**
	 * Clears all tokens in the account manager for the dummy account
	 */
	private void clearTokens() {
		manager.invalidateAuthToken(account.type, manager.peekAuthToken(account, Authenticator.TOKEN_TYPE_ACCESS));
		manager.invalidateAuthToken(account.type, manager.peekAuthToken(account, Authenticator.TOKEN_TYPE_REFRESH));
	}
	
	public void testGetAuthToken() {	
		// Set an access and a refresh token
		clearTokens();
		manager.setAuthToken(account, Authenticator.TOKEN_TYPE_ACCESS, ACCESS_TOKEN);
		manager.setAuthToken(account, Authenticator.TOKEN_TYPE_REFRESH, REFRESH_TOKEN);
		
		String tag;
		AccountManagerCallback<Bundle> callback;
		
		/*
		 *  There is an access and a refresh token. Expect to see them returned, and no intent 
		 */
		tag = "Access and refresh token available:";
		callback = new AbstractAuthenticatorTestCallback(tag) {
			@Override
			public void test(AccountManagerFuture<Bundle> future) throws OperationCanceledException, AuthenticatorException, IOException {
				// Expect to see token, but no intent 
				String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
				Intent intent = (Intent) future.getResult().get(AccountManager.KEY_INTENT);
				assertNotNull(getTag() + "No token present", token);
				assertNull(getTag() + "Unexpected intent", intent);
			}
		};
		new AuthTokenGetter().getAuthToken(account, Authenticator.TOKEN_TYPE_ACCESS, null, false, callback);
		new AuthTokenGetter().getAuthToken(account, Authenticator.TOKEN_TYPE_REFRESH, null, false, callback);		

		/*
		 *  There is no access token but a refresh token. Expect to see a new access and a refresh token returned, and no intent 
		 */
		clearTokens();
		manager.setAuthToken(account, Authenticator.TOKEN_TYPE_REFRESH, REFRESH_TOKEN);
		tag = "Only refresh token available:";
		callback = new AbstractAuthenticatorTestCallback(tag) {
			@Override
			public void test(AccountManagerFuture<Bundle> future) throws OperationCanceledException, AuthenticatorException, IOException {
				// Expect to see token, but no intent 
				String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
				Intent intent = (Intent) future.getResult().get(AccountManager.KEY_INTENT);
				assertNotNull(getTag() + "No token present", token);
				assertNull(getTag() + "Unexpected intent", intent);
			}
		};
		new AuthTokenGetter().getAuthToken(account, Authenticator.TOKEN_TYPE_ACCESS, null, false, callback);		
		new AuthTokenGetter().getAuthToken(account, Authenticator.TOKEN_TYPE_REFRESH, null, false, callback);		
		
		/*
		 *  There are no tokens available. Expect to see no tokens, but an intent
		 */
		clearTokens();
		tag = "No tokens available: ";
		callback = new AbstractAuthenticatorTestCallback(tag) {
			@Override
			public void test(AccountManagerFuture<Bundle> future) throws OperationCanceledException, AuthenticatorException, IOException {
				// Expect to see no token, but an intent 
				String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
				Intent intent = (Intent) future.getResult().get(AccountManager.KEY_INTENT);
				assertNull(getTag() + "Unexpected token " + token, token);
				assertNotNull(getTag() + "No intent present", intent);
			}
		};
		new AuthTokenGetter().getAuthToken(account, Authenticator.TOKEN_TYPE_ACCESS, null, false, callback);		
		new AuthTokenGetter().getAuthToken(account, Authenticator.TOKEN_TYPE_REFRESH, null, false, callback);		
	}
	
	@Override
	public void tearDown() throws InterruptedException {
		// Remove dummy account
		final Semaphore semaphore = new Semaphore(0);
		HandlerThread thread = new HandlerThread("Account removal thread");
		thread.start();
		manager.removeAccount(account, new AccountManagerCallback<Boolean>() {
			@Override
			public void run(AccountManagerFuture<Boolean> future) {
				try {
					if (future.getResult())
						return;
				} catch (OperationCanceledException x) {
				} catch (AuthenticatorException x) {
				} catch (IOException x) {
				} finally {
					semaphore.release();
				}
				throw new IllegalStateException("Could not remove dummy account");
			}
		}, new Handler(thread.getLooper()));
		semaphore.acquire();
	}
}
