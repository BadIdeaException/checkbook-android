package heger.christian.ledger.network;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ProtocolException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import android.text.TextUtils;

/**
 * Thin wrapper around {@link HttpsURLConnection} to facilitate importing the server certificate.
 */
public class SecureConnection  {
	protected static final String DEFAULT_PASSWORD = "keystore";
	
	private final HttpsURLConnection connection;

	public SecureConnection(HttpsURLConnection connection) {
		this.connection = connection;
	}
	
	/**
	 * Loads and sets the trust store from the supplied input stream using the supplied password
	 * for access. If the password is <code>null</code> the <code>DEFAULT_PASSWORD</code> is used.
	 * @param in - The input stream from which to read the trust store
	 * @param password - The password to use for accessing the trust store
	 */
	public void loadTruststore(InputStream in, String password) throws IOException, GeneralSecurityException {
		// Load the truststore that includes self-signed cert as a "trusted" entry.
		KeyStore truststore;
		truststore = KeyStore.getInstance("BKS");
		if (password == null) password = DEFAULT_PASSWORD;
		truststore.load(in, password.toCharArray());

		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(truststore);

		// Create custom SSL context that incorporates that truststore
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

		connection.setSSLSocketFactory(sslContext.getSocketFactory());
	}
	
	/**
	 * Sets this connection's authorization header field using the bearer strategy and the supplied token.
	 * @param token - The token to use for authorization with the server. Must not be <code>null</code>, or 
	 * an <code>IllegalArgumentException</code> will be thrown.
	 */
	public void bear(String token) {
		if (TextUtils.isEmpty(token))
			throw new IllegalArgumentException("Invalid token for authorization header.");
		connection.addRequestProperty("Authorization", "Bearer " + token);
	}
	/**
	 * Write the supplied <code>body</code> to the connection's output stream. 
	 * This operation allocates resources to write to the connection. These are automatically
	 * released at the end, but for performance reasons, frequent calls in close succession
	 * to this method should be avoided in favor of a single call combining all the <code>body</code>s.
	 * @param body - The payload to send
	 * @throws IOException
	 */
	public void write(String body) throws IOException {
		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(new BufferedOutputStream(connection.getOutputStream()));
		writer.write(body);
		writer.flush();
		} finally {
			writer.close();
		}
	}
	
	/**
	 * Reads the entire contents of the supplied <code>InputStream</code> and returns it as a string.
	 * @param in - The <code>InputStream</code> to read from
	 * @return - The contents of the stream as a single string
	 * @throws IOException 
	 */
	protected String internalRead(InputStream in) throws IOException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			return builder.toString();
		} finally {
			reader.close();
		}
	}
	
	protected void safeFail() throws IOException {
		switch (connection.getResponseCode()) {
			case HttpsURLConnection.HTTP_UNAUTHORIZED: throw new UnauthorizedAccessException();
		}
	}
	/**
	 * Returns the entire content of the input stream as a string.
	 * @throws IOException If there was an IO error during communication with the server
	 * @throws UnauthorizedAccessException If the request was denied by the server due to 
	 * bad or missing authorization
	 */
	public String read() throws IOException {
		safeFail();
		return internalRead(getInputStream());
	}

	/**
	 * Returns the entire content of the error stream as a string.
	 */
	public String readError() throws IOException {
		return internalRead(getErrorStream());
	}
	
	/*
	 *
	 * Auto-generated delegate methods 
	 *
	 */
	
	/**
	 * @throws IOException
	 * @see java.net.URLConnection#connect()
	 */
	public void connect() throws IOException {
		connection.connect();
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getAllowUserInteraction()
	 */
	public boolean getAllowUserInteraction() {
		return connection.getAllowUserInteraction();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.net.URLConnection#getContent()
	 */
	public Object getContent() throws IOException {
		return connection.getContent();
	}

	/**
	 * @param o
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		return connection.equals(o);
	}

	/**
	 * @return
	 * @see javax.net.ssl.HttpsURLConnection#getCipherSuite()
	 */
	public String getCipherSuite() {
		return connection.getCipherSuite();
	}

	/**
	 * @param types
	 * @return
	 * @throws IOException
	 * @see java.net.URLConnection#getContent(java.lang.Class[])
	 */
	public Object getContent(@SuppressWarnings("rawtypes") Class[] types) throws IOException {
		return connection.getContent(types);
	}

	/**
	 * @return
	 * @see javax.net.ssl.HttpsURLConnection#getLocalCertificates()
	 */
	public Certificate[] getLocalCertificates() {
		return connection.getLocalCertificates();
	}

	/**
	 * @return
	 * @throws SSLPeerUnverifiedException
	 * @see javax.net.ssl.HttpsURLConnection#getServerCertificates()
	 */
	public Certificate[] getServerCertificates()
			throws SSLPeerUnverifiedException {
		return connection.getServerCertificates();
	}

	/**
	 * @return
	 * @throws SSLPeerUnverifiedException
	 * @see javax.net.ssl.HttpsURLConnection#getPeerPrincipal()
	 */
	public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
		return connection.getPeerPrincipal();
	}

	/**
	 * @return
	 * @see javax.net.ssl.HttpsURLConnection#getLocalPrincipal()
	 */
	public Principal getLocalPrincipal() {
		return connection.getLocalPrincipal();
	}

	/**
	 * @param v
	 * @see javax.net.ssl.HttpsURLConnection#setHostnameVerifier(javax.net.ssl.HostnameVerifier)
	 */
	public void setHostnameVerifier(HostnameVerifier v) {
		connection.setHostnameVerifier(v);
	}

	/**
	 * @return
	 * @see javax.net.ssl.HttpsURLConnection#getHostnameVerifier()
	 */
	public HostnameVerifier getHostnameVerifier() {
		return connection.getHostnameVerifier();
	}

	/**
	 * @param sf
	 * @see javax.net.ssl.HttpsURLConnection#setSSLSocketFactory(javax.net.ssl.SSLSocketFactory)
	 */
	public void setSSLSocketFactory(SSLSocketFactory sf) {
		connection.setSSLSocketFactory(sf);
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return connection.hashCode();
	}

	/**
	 * @return
	 * @see javax.net.ssl.HttpsURLConnection#getSSLSocketFactory()
	 */
	public SSLSocketFactory getSSLSocketFactory() {
		return connection.getSSLSocketFactory();
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getContentLength()
	 */
	public int getContentLength() {
		return connection.getContentLength();
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getContentType()
	 */
	public String getContentType() {
		return connection.getContentType();
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getDate()
	 */
	public long getDate() {
		return connection.getDate();
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getDefaultUseCaches()
	 */
	public boolean getDefaultUseCaches() {
		return connection.getDefaultUseCaches();
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getDoInput()
	 */
	public boolean getDoInput() {
		return connection.getDoInput();
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getDoOutput()
	 */
	public boolean getDoOutput() {
		return connection.getDoOutput();
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getExpiration()
	 */
	public long getExpiration() {
		return connection.getExpiration();
	}

	/**
	 * @param pos
	 * @return
	 * @see java.net.URLConnection#getHeaderField(int)
	 */
	public String getHeaderField(int pos) {
		return connection.getHeaderField(pos);
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getHeaderFields()
	 */
	public Map<String, List<String>> getHeaderFields() {
		return connection.getHeaderFields();
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getRequestProperties()
	 */
	public Map<String, List<String>> getRequestProperties() {
		return connection.getRequestProperties();
	}

	/**
	 * @param field
	 * @param newValue
	 * @see java.net.URLConnection#addRequestProperty(java.lang.String, java.lang.String)
	 */
	public void addRequestProperty(String field, String newValue) {
		connection.addRequestProperty(field, newValue);
	}

	/**
	 * @param key
	 * @return
	 * @see java.net.URLConnection#getHeaderField(java.lang.String)
	 */
	public String getHeaderField(String key) {
		return connection.getHeaderField(key);
	}

	/**
	 * @param field
	 * @param defaultValue
	 * @return
	 * @see java.net.URLConnection#getHeaderFieldInt(java.lang.String, int)
	 */
	public int getHeaderFieldInt(String field, int defaultValue) {
		return connection.getHeaderFieldInt(field, defaultValue);
	}

	/**
	 * 
	 * @see java.net.HttpURLConnection#disconnect()
	 */
	public void disconnect() {
		connection.disconnect();
	}

	/**
	 * @param posn
	 * @return
	 * @see java.net.URLConnection#getHeaderFieldKey(int)
	 */
	public String getHeaderFieldKey(int posn) {
		return connection.getHeaderFieldKey(posn);
	}

	/**
	 * @return
	 * @see java.net.HttpURLConnection#getErrorStream()
	 */
	public InputStream getErrorStream() {
		return connection.getErrorStream();
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getIfModifiedSince()
	 */
	public long getIfModifiedSince() {
		return connection.getIfModifiedSince();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.net.HttpURLConnection#getPermission()
	 */
	public Permission getPermission() throws IOException {
		return connection.getPermission();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.net.URLConnection#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		return connection.getInputStream();
	}

	/**
	 * @return
	 * @see java.net.HttpURLConnection#getRequestMethod()
	 */
	public String getRequestMethod() {
		return connection.getRequestMethod();
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getLastModified()
	 */
	public long getLastModified() {
		return connection.getLastModified();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.net.HttpURLConnection#getResponseCode()
	 */
	public int getResponseCode() throws IOException {
		return connection.getResponseCode();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.net.URLConnection#getOutputStream()
	 */
	public OutputStream getOutputStream() throws IOException {
		return connection.getOutputStream();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see java.net.HttpURLConnection#getResponseMessage()
	 */
	public String getResponseMessage() throws IOException {
		return connection.getResponseMessage();
	}

	/**
	 * @param field
	 * @return
	 * @see java.net.URLConnection#getRequestProperty(java.lang.String)
	 */
	public String getRequestProperty(String field) {
		return connection.getRequestProperty(field);
	}

	/**
	 * @param method
	 * @throws ProtocolException
	 * @see java.net.HttpURLConnection#setRequestMethod(java.lang.String)
	 */
	public void setRequestMethod(String method) throws ProtocolException {
		connection.setRequestMethod(method);
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getURL()
	 */
	public URL getURL() {
		return connection.getURL();
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getUseCaches()
	 */
	public boolean getUseCaches() {
		return connection.getUseCaches();
	}

	/**
	 * @return
	 * @see java.net.HttpURLConnection#usingProxy()
	 */
	public boolean usingProxy() {
		return connection.usingProxy();
	}

	/**
	 * @return
	 * @see java.net.HttpURLConnection#getContentEncoding()
	 */
	public String getContentEncoding() {
		return connection.getContentEncoding();
	}

	/**
	 * @return
	 * @see java.net.HttpURLConnection#getInstanceFollowRedirects()
	 */
	public boolean getInstanceFollowRedirects() {
		return connection.getInstanceFollowRedirects();
	}

	/**
	 * @param followRedirects
	 * @see java.net.HttpURLConnection#setInstanceFollowRedirects(boolean)
	 */
	public void setInstanceFollowRedirects(boolean followRedirects) {
		connection.setInstanceFollowRedirects(followRedirects);
	}

	/**
	 * @param field
	 * @param defaultValue
	 * @return
	 * @see java.net.HttpURLConnection#getHeaderFieldDate(java.lang.String, long)
	 */
	public long getHeaderFieldDate(String field, long defaultValue) {
		return connection.getHeaderFieldDate(field, defaultValue);
	}

	/**
	 * @param contentLength
	 * @see java.net.HttpURLConnection#setFixedLengthStreamingMode(int)
	 */
	public void setFixedLengthStreamingMode(int contentLength) {
		connection.setFixedLengthStreamingMode(contentLength);
	}

	/**
	 * @param chunkLength
	 * @see java.net.HttpURLConnection#setChunkedStreamingMode(int)
	 */
	public void setChunkedStreamingMode(int chunkLength) {
		connection.setChunkedStreamingMode(chunkLength);
	}

	/**
	 * @param newValue
	 * @see java.net.URLConnection#setAllowUserInteraction(boolean)
	 */
	public void setAllowUserInteraction(boolean newValue) {
		connection.setAllowUserInteraction(newValue);
	}

	/**
	 * @param newValue
	 * @see java.net.URLConnection#setDefaultUseCaches(boolean)
	 */
	public void setDefaultUseCaches(boolean newValue) {
		connection.setDefaultUseCaches(newValue);
	}

	/**
	 * @param newValue
	 * @see java.net.URLConnection#setDoInput(boolean)
	 */
	public void setDoInput(boolean newValue) {
		connection.setDoInput(newValue);
	}

	/**
	 * @param newValue
	 * @see java.net.URLConnection#setDoOutput(boolean)
	 */
	public void setDoOutput(boolean newValue) {
		connection.setDoOutput(newValue);
	}

	/**
	 * @param newValue
	 * @see java.net.URLConnection#setIfModifiedSince(long)
	 */
	public void setIfModifiedSince(long newValue) {
		connection.setIfModifiedSince(newValue);
	}

	/**
	 * @param field
	 * @param newValue
	 * @see java.net.URLConnection#setRequestProperty(java.lang.String, java.lang.String)
	 */
	public void setRequestProperty(String field, String newValue) {
		connection.setRequestProperty(field, newValue);
	}

	/**
	 * @param newValue
	 * @see java.net.URLConnection#setUseCaches(boolean)
	 */
	public void setUseCaches(boolean newValue) {
		connection.setUseCaches(newValue);
	}

	/**
	 * @param timeoutMillis
	 * @see java.net.URLConnection#setConnectTimeout(int)
	 */
	public void setConnectTimeout(int timeoutMillis) {
		connection.setConnectTimeout(timeoutMillis);
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getConnectTimeout()
	 */
	public int getConnectTimeout() {
		return connection.getConnectTimeout();
	}

	/**
	 * @param timeoutMillis
	 * @see java.net.URLConnection#setReadTimeout(int)
	 */
	public void setReadTimeout(int timeoutMillis) {
		connection.setReadTimeout(timeoutMillis);
	}

	/**
	 * @return
	 * @see java.net.URLConnection#getReadTimeout()
	 */
	public int getReadTimeout() {
		return connection.getReadTimeout();
	}

	/**
	 * @return
	 * @see java.net.URLConnection#toString()
	 */
	@Override
	public String toString() {
		return connection.toString();
	}
}
