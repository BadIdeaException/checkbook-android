package heger.christian.ledger.test;

import heger.christian.ledger.network.Endpoints;
import heger.christian.ledger.network.SecureConnection;
import heger.christian.ledger.sync.KeySeriesRequester;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.HttpsURLConnection;

import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.test.InstrumentationTestCase;

public class KeySeriesRequesterTest extends InstrumentationTestCase {

	public void testRequest() {
		KeySeriesRequester requester = new KeySeriesRequester();
		
		SecureConnection connection = null;
		try {
			connection = new SecureConnection((HttpsURLConnection) Endpoints.URL_KEY_REQUEST.openConnection());
			connection.loadTruststore(getInstrumentation().getContext().getResources().openRawResource(heger.christian.ledger.test.R.raw.truststore), null);
		} catch (IOException x) {
			fail("IOException during connection setup: " + x.getMessage());
		} catch (NotFoundException x) {
			fail("Truststore not found: " + x.getMessage());
		} catch (GeneralSecurityException x) {
			fail("Error while loading certificate: " + x.getMessage());
		}
		
		String tag = "Request key series: ";
		try {
			Bundle bundle = requester.request(connection);
			assertTrue(tag + "Field KEY_NEXT_KEY is missing", bundle.containsKey(KeySeriesRequester.KEY_NEXT_KEY));
			assertTrue(tag + "Field KEY_NEXT_KEY is missing", bundle.containsKey(KeySeriesRequester.KEY_UPPER_BOUND));
		} catch (IOException x) {
			fail("IOException during communication with the server: " + x.getMessage());
		}
	}
}
