package heger.christian.checkbook.network;

import heger.christian.checkbook.R;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import android.content.Context;
import android.content.res.Resources.NotFoundException;

public class CheckbookSSLContextFactory  {
	private static final String PASSWORD = "keystore";

	private TrustManagerFactory trustManagerFactory;

	/**
	 * Standard constructor. It will read the truststore to use from the resources associated with the passed context.
	 * @param context - The context from which to take the resources
	 * @throws TruststoreException If setting up the truststore failed
	 */
	public CheckbookSSLContextFactory(Context context) throws TruststoreException {
		this(context.getResources().openRawResource(R.raw.truststore), PASSWORD);
	}

	/**
	 * @hide
	 * Constructor to directly provide an input stream and password for the truststore.
	 * This exists mainly for testing.
	 * @param in - An input stream to the truststore
	 * @param password - The password for the truststore
	 * @throws TruststoreException If setting up the truststore failed
	 */
	public CheckbookSSLContextFactory(InputStream in, String password) throws TruststoreException {
		loadTruststore(in, password);
	}

	protected void loadTruststore(InputStream in, String password) throws TruststoreException {
		// Load the truststore that includes self-signed cert as a "trusted" entry.
		KeyStore truststore;
		try {
			truststore = KeyStore.getInstance("BKS");
			truststore.load(in, password.toCharArray());

			trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(truststore);
		} catch (NoSuchAlgorithmException x) {
			throw new TruststoreException(x);
		} catch (KeyStoreException x) {
			throw new TruststoreException(x);
		} catch (CertificateException x) {
			throw new TruststoreException(x);
		} catch (NotFoundException x) {
			throw new TruststoreException(x);
		} catch (IOException x) {
			throw new TruststoreException(x);
		}
	}

	public SSLContext createSSLContext() throws TruststoreException  {
		// Create custom SSL context that incorporates the truststore
		SSLContext sslContext;
		try {
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
		} catch (KeyManagementException x) {
			throw new TruststoreException(x);
		} catch (NoSuchAlgorithmException x) {
			throw new TruststoreException(x);
		}
		return sslContext;
	}
}
