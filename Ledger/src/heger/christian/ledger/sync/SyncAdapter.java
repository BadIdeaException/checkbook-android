package heger.christian.ledger.sync;

import heger.christian.ledger.accounts.Authenticator;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

	Context context;

	/**
	 * Set up the sync adapter
	 */
	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		/*
		 * If your app uses a content resolver, get an instance of it
		 * from the incoming Context
		 */
		this.context = context;
	}

	/**
	 * Set up the sync adapter. This form of the
	 * constructor maintains compatibility with Android 3.0
	 * and later platform versions
	 */
	public SyncAdapter(
			Context context,
			boolean autoInitialize,
			boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);
		this.context = context;
	}

	@SuppressWarnings("deprecation")
	@TargetApi(android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		// TODO Auto-generated method stub
		Log.i("SyncAdapter", "Started sync");
		AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {			
			@Override
			public void run(AccountManagerFuture<Bundle> future) {
				try {
					String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
					if (token == null) token = "";
					Log.i("SyncAdapter", "Token: " + token);
				} catch (OperationCanceledException x) {
					// TODO Auto-generated catch block
					x.printStackTrace();
				} catch (AuthenticatorException x) {
					// TODO Auto-generated catch block
					x.printStackTrace();
				} catch (IOException x) {
					// TODO Auto-generated catch block
					x.printStackTrace();
				}
				
			}
		};
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			AccountManager.get(context).getAuthToken(account, Authenticator.TOKEN_TYPE_ACCESS, null, true, callback, null);
		} else {
			AccountManager.get(context).getAuthToken(account, Authenticator.TOKEN_TYPE_ACCESS, true, callback, null);
		}
	}
}
