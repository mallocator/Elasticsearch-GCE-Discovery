package org.elasticsearch.cloud.gce;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.cluster.node.DiscoveryNodeService;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.MemoryCredentialStore;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;

/**
 * This service finds the other nodes of the cluster.
 */
public class GCEService extends AbstractLifecycleComponent<GCEService> {
	private static final String	APP_NAME	= "ElasticSearch GCE Discovery Plugin";
	private Compute				client;
	private final String		clientId;
	private final String		clientSecret;

	@Inject
	public GCEService(final Settings settings, final SettingsFilter settingsFilter, final NetworkService networkService,
			final DiscoveryNodeService discoveryNodeService) {
		super(settings);
		settingsFilter.addFilter(new GCESettingsFilter());
		networkService.addCustomNameResolver(new GCENameResolver(settings));
		discoveryNodeService.addCustomAttributeProvider(new GCENodeAttributes(settings));
		this.clientId = this.settings.get("discovery.gce.client_id");
		this.clientSecret = this.settings.get("discovery.gce.client_secret");
		this.logger.debug("Initialized GCEService with client_id {} and client_secret {}", this.clientId, this.clientSecret.length());
	}

	/**
	 * Creates a new authorized compute engine client, or returns the one that already exists.
	 * 
	 * @return
	 */
	public synchronized Compute client() {
		this.logger.trace("Retrieving GCE Client");
		if (this.client != null) {
			return this.client;
		}

		this.logger.debug("Setting up GCE Client");
		try {
			final HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			final JsonFactory jsonFactory = new JacksonFactory();
			final Details details = new Details().setClientId(this.clientId).setClientSecret(this.clientSecret);
			final GoogleClientSecrets secrets = new GoogleClientSecrets().setInstalled(details);
			final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport,
				jsonFactory,
				secrets,
				Arrays.asList(ComputeScopes.COMPUTE_READONLY)).setCredentialStore(new MemoryCredentialStore()).build();
			final Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("compute.readonly");
			this.client = new Compute.Builder(httpTransport, jsonFactory, credential).setApplicationName(APP_NAME).build();
		} catch (GeneralSecurityException | IOException e) {
			this.logger.error("There was an error creating the GCE compute client", e);
		}

		return this.client;

	}

	@Override
	protected void doStart() throws ElasticSearchException {}

	@Override
	protected void doStop() throws ElasticSearchException {}

	@Override
	protected void doClose() throws ElasticSearchException {}
}
