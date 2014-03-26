package heger.christian.ledger.accounts;

import heger.christian.ledger.network.TruststoreException;

import java.io.IOException;

import android.test.AndroidTestCase;

/*
 *
 * IMPORTANT:
 *
 * Make sure to run the mock OAuth server under /env/mock-auth-server.js or this test will fail!

 */

public class ServerAuthenticatorTest extends AndroidTestCase {
	ServerAuthenticator authenticator;

	@Override
	public void setUp() {
		authenticator = new ServerAuthenticator(getContext());
	}

	public void testAuthenticateRefresh() throws TruststoreException {
		String tag;
		TokenSet tokens;

		// Using correct refresh token
		tag = "Present correct refresh token: ";
		try {
			tokens = authenticator.authenticate("correct refresh token");
			assertNotNull(tag, tokens);
			assertTrue(tag, tokens.hasAccess());
			assertTrue(tag, tokens.hasRefresh());
		} catch (AuthenticationFailedException x) {
			fail(tag + x.getMessage());
		} catch (IOException x) {
			fail(tag + "IOException " + x.getMessage());
		}

		// Using incorrect credentials
		tag = "Present incorrect refresh token: ";
		try {
			tokens = authenticator.authenticate("incorrect refresh token");
			fail(tag + "Did not throw AuthenticationFailedException");
		} catch (AuthenticationFailedException x) {
			assertEquals(ServerAuthenticator.ERR_INVALID_GRANT, x.getErrorCode());
		} catch (IOException x) {
			fail(tag + "IOException " + x.getMessage());
		}
	}

	public void testAuthenticateResourceOwner() throws TruststoreException {
		String tag;
		TokenSet tokens;

		// Using correct credentials
		tag = "Login with correct credentials: ";
		try {
			tokens = authenticator.authenticate("correct user", "correct password");
			assertNotNull(tag, tokens);
			assertTrue(tag, tokens.hasAccess());
			assertTrue(tag, tokens.hasRefresh());
		} catch (AuthenticationFailedException x) {
			fail(tag + x.getMessage());
		} catch (IOException x) {
			fail(tag + "IOException " + x.getMessage());
		}

		// Using incorrect credentials
		tag = "Login with incorrect credentials: ";
		try {
			tokens = authenticator.authenticate("incorrect user", "incorrect password");
			fail(tag + "Did not throw AuthenticationFailedException");
		} catch (AuthenticationFailedException x) {
			assertEquals(ServerAuthenticator.ERR_INVALID_GRANT, x.getErrorCode());
		} catch (IOException x) {
			fail(tag + "IOException " + x.getMessage());
		}
	}

}
