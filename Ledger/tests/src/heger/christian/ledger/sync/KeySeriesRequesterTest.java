package heger.christian.ledger.sync;

import heger.christian.ledger.network.LedgerSSLContextFactory;
import heger.christian.ledger.network.TruststoreException;
import heger.christian.ledger.test.R;

import java.io.IOException;

import android.os.Bundle;
import android.test.InstrumentationTestCase;

public class KeySeriesRequesterTest extends InstrumentationTestCase {

	public void testRequest() throws TruststoreException {
		KeySeriesRequester requester = new KeySeriesRequester();

//		SecureConnection connection = null;
//		try {
//			connection = new SecureConnection((HttpsURLConnection) Endpoints.URL_KEY_REQUEST.openConnection());
//			connection.loadTruststore(getInstrumentation().getContext().getResources().openRawResource(heger.christian.ledger.R.raw.truststore), null);
//		} catch (IOException x) {
//			fail("IOException during connection setup: " + x.getMessage());
//		} catch (NotFoundException x) {
//			fail("Truststore not found: " + x.getMessage());
//		} catch (GeneralSecurityException x) {
//			fail("Error while loading certificate: " + x.getMessage());
//		}
//
		LedgerSSLContextFactory factory = new LedgerSSLContextFactory(getInstrumentation().getContext().getResources().openRawResource(R.raw.truststore), "keystore");
		requester.setSSLSocketFactory(factory.createSSLContext().getSocketFactory());
		String tag = "Request key series: ";
		try {
			// The mock server doesn't care for authorization, so let's not jump through that particular hoop here
			Bundle bundle = requester.request("");
			assertTrue(tag + "Field KEY_NEXT_KEY is missing", bundle.containsKey(KeySeriesRequester.KEY_NEXT_KEY));
			assertTrue(tag + "Field KEY_NEXT_KEY is missing", bundle.containsKey(KeySeriesRequester.KEY_UPPER_BOUND));
		} catch (IOException x) {
			fail("IOException during communication with the server: " + x.getMessage());
		}
	}
}
