package org.elasticsearch.discovery.gce;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.elasticsearch.cloud.gce.GCEService;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.discovery.zen.ping.unicast.UnicastHostsProvider;
import org.elasticsearch.transport.TransportService;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Zone;

/**
 * Is used to register this node and create a list of available nodes in the cluster.
 */
public class GCEUnicastHostsProvider extends AbstractComponent implements UnicastHostsProvider {
	private final TransportService		transportService;
	private final GCEService			gceService;
	private final String				project;
	private final Set<String>			network;
	private final Set<String>			tags;
	private final Map<String, String>	metadata;
	private final Set<String>			zones;

	@Inject
	public GCEUnicastHostsProvider(final Settings settings, final TransportService transportService, final GCEService gceService) {
		super(settings);
		this.transportService = transportService;
		this.gceService = gceService;
		this.project = this.settings.get("discovery.gce.project");
		this.network = new HashSet<>(Arrays.asList(this.settings.getAsArray("discovery.gce.network")));
		this.tags = new HashSet<>(Arrays.asList(this.settings.getAsArray("discovery.gce.tags")));
		this.metadata = this.settings.getByPrefix("discovery.gce.metadata.").getAsMap();
		this.zones = new HashSet<>(Arrays.asList(this.settings.getAsArray("discovery.gce.zones")));
		this.logger.debug("Initialized GCEUnicastHostProvider");
	}

	/**
	 * Connects to google computer engine and goes over list of available instance to check if any match the configuration,
	 * which it then returns as a list ElasticSearch understands.
	 */
	@Override
	public List<DiscoveryNode> buildDynamicNodes() {
		final List<DiscoveryNode> nodes = Lists.newArrayList();

		this.logger.debug("Building dynamic list of discovery nodes");
		final Compute client = this.gceService.client();
		try {
			for (final Zone zone : client.zones().list(this.project).execute().getItems()) {
				if (!checkZone(zone)) {
					continue;
				}
				final List<Instance> iList = client.instances().list(this.project, zone.getName()).execute().getItems();
				if (iList != null) {
					for (final Instance instance : iList) {
						if (!checkState(instance) || !checkTags(instance) || !checkMetaData(instance)) {
							continue;
						}
						for (final NetworkInterface networkInterface : instance.getNetworkInterfaces()) {
							if (this.network.isEmpty() || this.network.contains(networkInterface.getNetwork())) {
								int i = 0;
								for (TransportAddress address : this.transportService.addressesFromString(networkInterface.getNetworkIP())) {
									nodes.add(new DiscoveryNode("#cloud-" + instance.getId() + "-" + networkInterface.getName() + "-" + i++,
										address));
								}
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
		return false;
	}
}
