package org.elasticsearch.discovery.gce;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Zone;

/**
 * Is used to register this node and create a list of available nodes in the cluster.
 */
public class GCEUnicastHostsProvider extends AbstractComponent implements UnicastHostsProvider {
	private final TransportService		transportService;
	private final Compute				client;
	private final String				project;
	private final Set<String>			network;
	private final Set<String>			tags;
	private final Map<String, String>	metadata;
	private final Set<String>			zones;

	@Inject
	public GCEUnicastHostsProvider(final Settings settings, final TransportService transportService, final Compute client) {
		super(settings);
		this.transportService = transportService;
		this.client = client;
		this.project = this.componentSettings.get("project");
		this.network = new HashSet<>(Arrays.asList(this.componentSettings.getAsArray("network")));
		this.tags = new HashSet<>(Arrays.asList(this.componentSettings.getAsArray("tags")));
		this.metadata = this.componentSettings.getByPrefix("metadata.").getAsMap();
		this.zones = new HashSet<>(Arrays.asList(this.componentSettings.getAsArray("zones")));
	}

	/**
	 * Connects to google computer engine and goes over list of available instance to check if any match the configuration,
	 * which it then returns as a list ElasticSearch understands.
	 */
	@Override
	public List<DiscoveryNode> buildDynamicNodes() {
		final List<DiscoveryNode> nodes = Lists.newArrayList();

		this.logger.trace("Building dynamic list of discovery nodes");
		try {
			for (final Zone zone : this.client.zones().list(this.project).execute().getItems()) {
				if (!checkZone(zone)) {
					continue;
				}
				for (final Instance instance : this.client.instances().list(this.project, zone.getName()).execute().getItems()) {
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
		} catch (Exception e) {
			this.logger.error("Unable to load google computer client", e);
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
		for (String tag : instance.getTags().getItems()) {
			if (this.tags.contains(tag)) {
				return true;
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
		for (final Entry<String, Object> entry : instance.getMetadata().entrySet()) {
			if (this.metadata.containsKey(entry.getKey()) && this.metadata.get(entry.getKey()).matches((String) entry.getValue())) {
				return true;
			}
		}
		return false;
	}
}
