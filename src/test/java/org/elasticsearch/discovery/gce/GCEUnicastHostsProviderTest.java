package org.elasticsearch.discovery.gce;

import java.io.File;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialStore;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.FileCredentialStore;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.ComputeScopes;

@SuppressWarnings("deprecation")
public class GCEUnicastHostsProviderTest {
	private String	clientSecret;
	private String	clientId;
	private String	credentials;

	/**
	 * Set class fields to valid values to make test possible to complete.
	 * 
	 * @throws Throwable
	 */
	@Test(enabled = false)
	public void connectionTest() throws Throwable {
		final HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		final JsonFactory jsonFactory = new JacksonFactory();
		final Details details = new Details().setClientId(this.clientId).setClientSecret(this.clientSecret);
		final GoogleClientSecrets secrets = new GoogleClientSecrets().setInstalled(details);
		final CredentialStore credentialsStore = new FileCredentialStore(new File(this.credentials), jsonFactory);
		final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport,
			jsonFactory,
			secrets,
			Arrays.asList(ComputeScopes.COMPUTE_READONLY)).setCredentialStore(credentialsStore).build();
		final Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize(this.clientId);
		Assert.assertTrue(credential.getExpiresInSeconds() > 0);
	}
}
