package org.elasticsearch.discovery.gce;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.discovery.zen.ping.unicast.UnicastHostsProvider;
import org.elasticsearch.transport.TransportService;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Zone;

/**
 * Is used to register this node and create a list of available nodes in the cluster.
 */
public class GCEUnicastHostsProvider implements UnicastHostsProvider {
	private static final String			APP_NAME	= "ElasticSearch GCE Discovery Plugin";
	private final ESLogger				logger		= Loggers.getLogger(getClass());
	private final TransportService		transportService;
	private final String				clientId;
	private final String				clientSecret;
	private final File					credentials;
	private final String				clientP12;
	private final String				project;
	private final Set<String>			network;
	private final Set<String>			tags;
	private final Map<String, String>	metadata;
	private final Set<String>			zones;
	private final Compute				client;

	public GCEUnicastHostsProvider(final Settings settings, final TransportService transportService) {
		this.transportService = transportService;
		this.project = settings.get("discovery.gce.project");
		this.network = new HashSet<>(Arrays.asList(settings.getAsArray("discovery.gce.network")));
		this.tags = new HashSet<>(Arrays.asList(settings.getAsArray("discovery.gce.tags")));
		this.metadata = settings.getByPrefix("discovery.gce.metadata.").getAsMap();
		this.zones = new HashSet<>(Arrays.asList(settings.getAsArray("discovery.gce.zones")));
		this.clientId = settings.get("discovery.gce.client_id");
		this.clientSecret = settings.get("discovery.gce.client_secret");
		this.clientP12 = settings.get("discovery.gce.client_p12_location");
		this.credentials = new File(settings.get("discovery.gce.credential_location", System.getProperty("user.home")
				+ ".credentials/compute-engine.json"));
		this.client = getClient();
		this.logger.debug("Initialized GCEUnicastHostProvider");
	}

	private Compute getClient() {
		this.logger.debug("Setting up GCE Client");
		try {
			final HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			final JsonFactory jsonFactory = new JacksonFactory();
			final PrivateKey p12 = getP12File();
			Credential credential;
			if (p12 != null) {
				credential = new GoogleCredential.Builder().setTransport(httpTransport)
					.setJsonFactory(jsonFactory)
					.setServiceAccountId(this.clientId)
					.setServiceAccountScopes(Arrays.asList(ComputeScopes.COMPUTE_READONLY))
					.setServiceAccountPrivateKey(p12)
					.build();
			}
			else {
				final Details details = new Details().setClientId(this.clientId).setClientSecret(this.clientSecret);
				final GoogleClientSecrets secrets = new GoogleClientSecrets().setInstalled(details);
				final DataStore<StoredCredential> credentialsStore = new FileDataStoreFactory(this.credentials.getParentFile()).getDataStore(this.credentials.getName());
				final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport,
					jsonFactory,
					secrets,
					Arrays.asList(ComputeScopes.COMPUTE_READONLY)).setCredentialDataStore(credentialsStore).build();
				credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize(this.clientId);
			}
			return new Compute.Builder(httpTransport, jsonFactory, credential).setApplicationName(APP_NAME).build();
		} catch (Exception e) {
			this.logger.error("There was an error creating the GCE compute client", e);
			throw new RuntimeException(e);
		}
	}

	private PrivateKey getP12File() {
		if (this.clientP12 != null) {
			try {
				if (this.clientP12.startsWith("classpath:")) {
					return SecurityUtils.loadPrivateKeyFromKeyStore(SecurityUtils.getPkcs12KeyStore(), getClass().getClassLoader()
						.getResourceAsStream(this.clientP12.substring(10)), "notasecret", "privatekey", "notasecret");
				}
				return SecurityUtils.loadPrivateKeyFromKeyStore(SecurityUtils.getPkcs12KeyStore(),
					new FileInputStream(this.clientP12.replaceFirst("file:/?/?", "")),
					"notasecret",
					"privatekey",
					"notasecret");
			} catch (IOException | GeneralSecurityException e) {
				this.logger.warn("Unable to load p12 key", e);
			}
		}
		return null;
	}

	/**
	 * Connects to google computer engine and goes over list of available instance to check if any match the configuration,
	 * which it then returns as a list ElasticSearch understands.
	 */
	@Override
	public List<DiscoveryNode> buildDynamicNodes() {
		final List<DiscoveryNode> nodes = Lists.newArrayList();

		this.logger.debug("Building dynamic list of discovery nodes");
		try {
			final List<Zone> items = this.client.zones().list(this.project).execute().getItems();
			if (items == null || items.isEmpty()) {
				logger.warn("Compute client didn't report any zones. Not adding any discovery nodes.");
			}
			else {
				for (final Zone zone : items) {
					if (!checkZone(zone)) {
						logger.debug("Skipping zone {} as it doesn't match the configured zone", zone.getName());
						continue;
					}
					final List<Instance> instances = this.client.instances().list(this.project, zone.getName()).execute().getItems();
					if (instances == null || instances.isEmpty()) {
						logger.debug("Skipping zone {} as it doesn't seem to have any instances", zone.getName());
						continue;
					}
					for (final Instance instance : instances) {
						if (!checkState(instance) || !checkTags(instance) || !checkMetaData(instance)) {
							logger.debug("Skipping instance {} as it doesn't match the configured filters", instance.getName());
							continue;
						}
						for (final NetworkInterface networkInterface : instance.getNetworkInterfaces()) {
							if (this.network.isEmpty() || this.network.contains(networkInterface.getNetwork())) {
								int i = 0;
								for (TransportAddress address : this.transportService.addressesFromString(networkInterface.getNetworkIP())) {
									logger.trace("Adding instance {} to list of discovery nodes", instance.getName());
									nodes.add(new DiscoveryNode("#cloud-" + instance.getId() + "-" + networkInterface.getName() + "-" + i++,
										address,
										Version.CURRENT));
								}
							}
							else {
								logger.debug("Skipping any instance {} because they are not going over the configured network interface {}",
									instance.getName(),
									this.network);
							}
						}
					}

				}
			}
		} catch (Exception e) {
			this.logger.error("Unable to load google computer gceService", e);
		}

		this.logger.debug("Using dynamic discovery nodes {}", nodes);
		return nodes;
	}

	/**
	 * Verifies if the zone matches up, if any has been configured.
	 * 
	 * @param zone
	 * @return
	 */
	private boolean checkZone(final Zone zone) {
		if (this.zones.isEmpty()) {
			logger.trace("Not limiting to any zones");
			return true;
		}
		return this.zones.contains(zone.getName());
	}

	/**
	 * Check if this instance if running before we allow it to be checked for an active ElasticSearch node.
	 * 
	 * @param instance
	 * @return
	 */
	private boolean checkState(final Instance instance) {
		if (instance.getStatus().equalsIgnoreCase("RUNNING")) {
			return true;
		}
		if (instance.getStatus().equalsIgnoreCase("STAGING")) {
			return true;
		}
		logger.trace("Skipping instance {} because its state is {}", instance.getName(), instance.getStatus());
		return false;
	}

	/**
	 * Verifies if the tags of the instance match up, if any tags have been configured.
	 * 
	 * @param instance
	 * @return
	 */
	private boolean checkTags(final Instance instance) {
		if (this.tags.isEmpty()) {
			return true;
		}
		final List<String> tags = instance.getTags().getItems();
		if (tags != null) {
			for (final String tag : tags) {
				if (this.tags.contains(tag)) {
					return true;
				}
			}
		}
		logger.trace("Skipping instance {} because it doesn't match any of the configured tags {}", instance.getName(), this.tags);
		return false;
	}

	/**
	 * Verifies if the meta data of the instance matches the given regex, if any meta data regexes have been given.
	 * 
	 * @param instance
	 * @return
	 */
	private boolean checkMetaData(final Instance instance) {
		if (this.metadata.isEmpty()) {
			return true;
		}
		final Metadata metadata = instance.getMetadata();
		if (metadata != null) {
			for (final Entry<String, Object> entry : metadata.entrySet()) {
				if (this.metadata.containsKey(entry.getKey()) && this.metadata.get(entry.getKey()).matches((String) entry.getValue())) {
					return true;
				}
			}
		}
		logger.trace("Skipping instance {} because it doesn't match any of the configured metadata {}", instance.getName(), this.metadata);
		return false;
	}
}
