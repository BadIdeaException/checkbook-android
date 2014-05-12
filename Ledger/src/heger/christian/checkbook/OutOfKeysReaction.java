package heger.christian.checkbook;

import heger.christian.checkbook.accounts.Authenticator;
import heger.christian.checkbook.network.CheckbookSSLContextFactory;
import heger.christian.checkbook.network.TruststoreException;
import heger.christian.checkbook.network.UnauthorizedAccessException;
import heger.christian.checkbook.providers.MetaContentProvider.KeyGenerationContract;
import heger.christian.checkbook.sync.KeySeriesRequester;
import heger.christian.checkbook.ui.OutOfKeysDialog;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

/**
 * This class encapsulates the common reaction to a failed insert
 * due to an exhausted key series.
 * <p>
 * It attempts to get a new key series from the
 * server. This may involve asking the user to log in; the appropriate
 * activity will be shown.
 * If a new key series is indeed obtained, it will be written to
 * storage.
 * If it was not possible to get a new key series, a user notification
 * is displayed.
 * Because requesting a key series involves potentially long-running
 * network operations, an overlay with a progress spinner is faded in,
 * and the original content faded out to 33%. On completion,
 * the progress view is faded out again and the original content
 * brought back to full opaqueness.
 * <p>
 * A <code>KeyRequestResultListener</code> can be attached to be
 * notified of the outcome of the request. If present, the listener
 * is always called last.
 * <p>
 * Note: As part of reacting to an exhausted key series, a temporary
 * fragment is attached to the calling activity using this class's
 * canonical name as the tag. This is done to allow a started key
 * series request to persist across configuration changes. On completion,
 * the fragment is automatically removed again.
 * <h2>Internal protocol</h2>
 * Internally, it relies on two separate <code>Handler</code>s (an <i>account handler</i>
 * and a <i>network handler</i>) communicating.
 * This is because the <code>LoginActivity</code> does not get shown if
 * {@link AccountManager#getAuthToken(Account, String, Bundle, Activity, AccountManagerCallback, Handler)}
 * is not called from the main thread.
 * When both handlers are fully set up, the protocol is as follows:
 * <ol>
 * <li>The network handler gets sent a message to start the algorithm. This will make it initialize the
 * key requester and then wait.
 * <li>The main handler gets sent a message to get an authorization token. This may
 * include showing a login form to the user. If a token is successfully obtained, a message
 * indicating so and including the token is sent to the network handler. If no token could be obtained,
 * a CANCEL message is sent to the network handler.
 * <li>The network handler uses the token to make a key series request to the server. If this is successful,
 * the new key series is written to storage, and a message indicating success is sent to the account handler.
 * If a new key series could not be obtained due to bad authorization, a message indicating the invalidity
 * of the used token is sent to the account handler. If a new series could not be obtained for any other reason,
 * a message indicating failure is sent to the account handler.
 * <li>If the account handler receives the success message, it performs a clean up (see below). If a
 * <code>KeyRequestResultListener</code> is attached, its <code>onSuccess()</code> callback is called.
 * <li>If the account handler receives a message indicating an invalid token, it invalidates the token in the
 * <code>AccountManager</code> and sends itself a new message to get an authorization token. The process repeats
 * from step 2.
 * <li>If the account manager gets a message indicating failure of the key request, it performs a clean up (see below).
 * An <code>OutOfKeysDialog</code> is shown to inform the user of the failure. Finally,
 * if a <code>KeyRequestResultListener</code> is attached, its <code>onFailure()</code> callback is called.
 * <li>If the network handler, at any time, receives a request for cancellation, it will tear down the
 * connection and send a message indicating failure to the account handler. It will not interrupt an ongoing
 * request.
 * </ol>
 * "Performing a clean up" in the steps outlined above means
 * <ul>
 * <li>quitting the network handler, thereby allowing it to
 * terminate,
 * <li>fading out the overlay and bringing the original content back to full opaqueness,
 * <li>and removing the temporary fragment from the hosting activity.
 * </ul>
 */
public class OutOfKeysReaction extends Fragment {
	/**
	 * Start the algorithm.
	 */
	private static final int EVENT_START = 0;
	/*
	 * Get an access token.
	 */
	private static final int EVENT_GET_TOKEN = 1;
	/**
	 * The token was invalid. The invalid token is in the payload.
	 * This will automatically generate an <code>EVENT_GET_TOKEN</code>.
	 */
	private static final int EVENT_INVALID_TOKEN = 2;
	/**
	 * A token is available and attached in the message's <code>WorkerArgs.payload</code>.
	 */
	private static final int EVENT_TOKEN_AVAILABLE = 3;
	/**
	 * The key series request succeeded. The new series has been written to storage.
	 */
	private static final int EVENT_SUCCEEDED = 4;
	/**
	 * The key series request failed. More information might be available
	 * in the <code>WorkerArgs.payload</code>.
	 */
	private static final int EVENT_FAILED = 5;
	/**
	 * Cancel the operation.
	 */
	private static final int EVENT_CANCEL = 6;

	public static final String OUT_OF_KEYS_DIALOG_TAG = OutOfKeysDialog.class.getCanonicalName();

	private HandlerThread networkThread;

	private int duration;
	private FrameLayout overlay;
	private View content;

	public interface KeyRequestResultListener {
		public void onSuccess();
		public void onFailure();
	}
	private KeyRequestResultListener listener;

	protected static class WorkerArgs {
		Handler handler;
		Object payload;
	}
	protected class NetworkHandler extends Handler {
		private KeySeriesRequester requester;
		public NetworkHandler(Looper looper) {
			super(looper);
		}
		/**
		 * Prepare the requester, giving it a SSLSocketFactory for our self-signed certificate, then wait
		 * @throws TruststoreException If setting up the trust store failed
		 */
		protected void prepareRequester() throws TruststoreException {
			requester = new KeySeriesRequester();
			requester.setSSLSocketFactory(new CheckbookSSLContextFactory(getActivity()).createSSLContext().getSocketFactory());
		}
		protected boolean requestKeys(String token) throws IOException {
			// Request new key series from the server using the connection
			Bundle bundle = requester.request(token);

			// If response has a new key series, write it to storage
			if (bundle.containsKey(KeySeriesRequester.KEY_NEXT_KEY) && bundle.containsKey(KeySeriesRequester.KEY_UPPER_BOUND)) {
				ContentValues values = new ContentValues();
				values.put(KeyGenerationContract.COL_NAME_NEXT_KEY, bundle.getLong(KeySeriesRequester.KEY_NEXT_KEY));
				values.put(KeyGenerationContract.COL_NAME_UPPER_BOUND, bundle.getLong(KeySeriesRequester.KEY_UPPER_BOUND));
				Activity activity = getActivity();
				if (activity != null) {
					getActivity().getContentResolver().insert(KeyGenerationContract.CONTENT_URI, values);
					return true;
				} else
					return false;
			} else
				return false;
		}

		/* (non-Javadoc)
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {
			WorkerArgs args = (WorkerArgs) msg.obj;
			int what;
			try{
				switch(msg.what) {
					case EVENT_START:
						// Prepare the connection and wait
						try {
							prepareRequester();
						} catch (TruststoreException x) {
							what = EVENT_FAILED;
							args.payload = x;
						}
						// Return without sending a reply
						return;
					case EVENT_TOKEN_AVAILABLE:
						// Go to the server with the token and get keys
						String token = (String) args.payload;
						try {
							if (requestKeys(token)) {
								what = EVENT_SUCCEEDED;
							} else
								what = EVENT_FAILED;
						} catch (UnauthorizedAccessException x) {
							// Unauthorized access might mean our token has expired.
							// Send a request to the other thread to invalidate it
							// and come again
							what = EVENT_INVALID_TOKEN;
							args.payload = token;
						}
						break;
					case EVENT_CANCEL:
						// Canceling means the operation failed, send the appropriate reply
						what = EVENT_FAILED;
						break;
					default:
						return;
				}
			} catch (IOException x) {
				what = EVENT_FAILED;
				args.payload = x;
			}

			Message reply = args.handler.obtainMessage(what);
			args.handler = this;
			reply.obj = args;
			reply.sendToTarget();
		}
	}

	protected class AccountHandler extends Handler {
		public AccountHandler(Looper looper) {
			super(looper);
		}
		@Override
		public void handleMessage(Message msg) {
			final WorkerArgs args = (WorkerArgs) msg.obj;
			switch (msg.what) {
				case EVENT_GET_TOKEN: {
					// Get an access token from the AccountManager.
					// This may involve asking the user to log in
					Activity activity = getActivity();
					// If activity has become unavailable, cancel
					if (activity == null) {
						Message reply = args.handler.obtainMessage(EVENT_CANCEL);
						args.handler = this;
						reply.obj = args;
						reply.sendToTarget();
						return;
					}

					AccountManager manager = AccountManager.get(activity);
					Account[] accounts = manager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
					if (accounts.length == 0) throw new IllegalStateException("No accounts available when trying to request new keys.");
					manager.getAuthToken(accounts[0],
							Authenticator.TOKEN_TYPE_ACCESS,
							null,
							activity,
							new AccountManagerCallback<Bundle>() {
								@Override
								public void run(AccountManagerFuture<Bundle> future) {
									Message reply = args.handler.obtainMessage();
									try {
										Bundle bundle = future.getResult();
										if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
											reply.what = EVENT_TOKEN_AVAILABLE;
											args.payload = bundle.getString(AccountManager.KEY_AUTHTOKEN);
										} else
											reply.what = EVENT_CANCEL;
									} catch (OperationCanceledException x) {
										reply.what = EVENT_CANCEL;
									} catch (AuthenticatorException x) {
										reply.what = EVENT_CANCEL;
									} catch (IOException x) {
										reply.what = EVENT_CANCEL;
									}
									args.handler = AccountHandler.this;
									reply.obj = args;
									reply.sendToTarget();
								}
							},
							null);
					break; }
				case EVENT_INVALID_TOKEN: {
					String token = (String) args.payload;
					// If activity has become unavailable, cancel
					Activity activity = getActivity();
					if (activity == null) {
						Message reply = args.handler.obtainMessage(EVENT_CANCEL);
						args.handler = this;
						reply.obj = args;
						reply.sendToTarget();
						return;
					}
					AccountManager.get(activity).invalidateAuthToken(Authenticator.ACCOUNT_TYPE, token);
					Message reply = obtainMessage(EVENT_GET_TOKEN);
					reply.obj = args;
					reply.sendToTarget();
					break; }
				case EVENT_SUCCEEDED:
					onSuccess();
					break;
				case EVENT_FAILED:
					onFailure();
					break;
			}
		}
	}

	public OutOfKeysReaction() {}

	/**
	 * Creates a new handler to run on the main thread for dealing with <code>AccountManager</code>.
	 * This is necessary because apparently if called from another thread, the login activity
	 * will not be displayed. This handler gets sent the <code>EVENT_GET_TOKEN</code> message after
	 * creation.
	 * <p>
	 * Creates a new handler to run on a new thread for running network requests. This handler gets
	 * sent the <code>EVENT_START</code> message after creation.
	 *
	 */
	protected void createAndStartHandlers() {
		Handler accountHandler = new AccountHandler(getActivity().getMainLooper());
		networkThread = new HandlerThread("OutOfKeysNetworkThread");
		networkThread.start();
		Handler networkHandler = new NetworkHandler(networkThread.getLooper());
		// Send EVENT_START to network handler
		Message msg = networkHandler.obtainMessage(EVENT_START);
		WorkerArgs args = new WorkerArgs();
		args.handler = accountHandler;
		msg.obj = args;
		msg.sendToTarget();
		// Send EVENT_GET_TOKEN to account handler
		msg = accountHandler.obtainMessage(EVENT_GET_TOKEN);
		args = new WorkerArgs();
		args.handler = networkHandler;
		msg.obj = args;
		msg.sendToTarget();
	}

	/**
	 * Do some clean up:
	 * <ul>
	 * <li>Stop the network thread
	 * <li>Hide the overlay and show the original content
	 * <li>Removes this <code>OutOfKeysReaction</code> from its activity
	 * </ul>
	 */
	protected void cleanUp() {
		// Stop the thread
		networkThread.quit();
		// Hide the overlay and show the original content
		hideOverlay();
		// Remove ourselves from the activity
		FragmentManager fragmentManager = getActivity().getFragmentManager();
		fragmentManager.beginTransaction().remove(this).commitAllowingStateLoss();
	}

	/**
	 * Adds to the view tree and fades in an overlay to show while trying to get more keys from the server,
	 * and fades the original content to 33%.
	 */
	public void showOverlay() {
		duration = getActivity().getResources().getInteger(android.R.integer.config_shortAnimTime);

		// Show an overlay with a progress spinner
		overlay = new FrameLayout(getActivity());
		overlay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {} // Swallow clicks
		});
		getActivity().getLayoutInflater().inflate(R.layout.layout_requesting_keys, overlay);
		content = getActivity().findViewById(android.R.id.content);
		// Navigate down one level
		// If we don't do this, the original content won't be visible anymore
		if (content instanceof ViewGroup) content = ((ViewGroup) content).getChildAt(0);

		overlay.setAlpha(0);

		((ViewGroup) content.getParent()).addView(overlay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		overlay.animate().setDuration(duration).alpha(1);
		content.animate().setDuration(duration).alpha(0.33f);
	}

	/**
	 * Fades out the overlay and then removes it from the view tree, while fading the original
	 * content back to 100%.
	 */
	public void hideOverlay() {
		overlay.animate().setDuration(duration).alpha(0).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animator) {
				((ViewGroup) overlay.getParent()).removeView(overlay);
			}
		});
		content.animate().setDuration(duration).alpha(1);
	}

	protected void onSuccess() {
		cleanUp();
		if (listener != null) listener.onSuccess();
	}

	protected void onFailure() {
		cleanUp();
		// Inform the user
		final DialogFragment dialog = OutOfKeysDialog.newInstance();
		// Cannot use dialog.show here because we need to commit allowing state loss
		// Otherwise this will crash if the user cancels out of the LoginActivity
		getActivity().getFragmentManager().beginTransaction().add(dialog, OUT_OF_KEYS_DIALOG_TAG).commitAllowingStateLoss();
		if (listener != null) listener.onFailure();
	}

	public void handleOutOfKeys() {
		showOverlay();
		createAndStartHandlers();
	}

	public static OutOfKeysReaction newInstance(Activity activity) {
		OutOfKeysReaction reaction = new OutOfKeysReaction();
		reaction.setRetainInstance(true);
		FragmentManager fragmentManager = activity.getFragmentManager();
		// Need to commit allowing state loss here or this will crash when the
		// user cancels out of the LoginActivity
		fragmentManager.beginTransaction().add(reaction, reaction.getClass().getCanonicalName()).commitAllowingStateLoss();
		fragmentManager.executePendingTransactions();
		return reaction;
	}

	/**
	 * @return the listener
	 */
	public KeyRequestResultListener getResultListener() {
		return listener;
	}

	/**
	 * @param listener the listener to set
	 */
	public void setResultListener(KeyRequestResultListener listener) {
		this.listener = listener;
	}
}
