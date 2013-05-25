package org.elasticsearch.discovery.gce;

import org.elasticsearch.cloud.gce.GCEService;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.node.DiscoveryNodeService;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.discovery.zen.ZenDiscovery;
import org.elasticsearch.discovery.zen.ping.ZenPing;
import org.elasticsearch.discovery.zen.ping.ZenPingService;
import org.elasticsearch.discovery.zen.ping.unicast.UnicastZenPing;
import org.elasticsearch.node.settings.NodeSettingsService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

/**
 * Registers GCE discovery as a ping resolver.
 */
public class GCEDiscovery extends ZenDiscovery {

	@Inject
	public GCEDiscovery(final Settings settings, final ClusterName clusterName, final ThreadPool threadPool,
			final TransportService transportService, final ClusterService clusterService, final NodeSettingsService nodeSettingsService,
			final ZenPingService pingService, final DiscoveryNodeService discoveryNodeService, final GCEService gceService) {
		super(settings, clusterName, threadPool, transportService, clusterService, nodeSettingsService, discoveryNodeService, pingService);

		this.logger.info("Setting up GCE Discovery");
		if (settings.getAsBoolean("discovery.gce.enabled", true)) {
			ImmutableList<? extends ZenPing> zenPings = pingService.zenPings();
			UnicastZenPing unicastZenPing = null;
			for (ZenPing zenPing : zenPings) {
				if (zenPing instanceof UnicastZenPing) {
					unicastZenPing = (UnicastZenPing) zenPing;
					break;
				}
			}

			if (unicastZenPing != null) {
				this.logger.debug("Adding GCE UnicastHostsProvider to zen pings");
				unicastZenPing.addHostsProvider(new GCEUnicastHostsProvider(settings, transportService, gceService));
				pingService.zenPings(ImmutableList.of(unicastZenPing));
				this.logger.info("Added GCE UnicastHostsProvider to zen pings");
			}
			else {
				this.logger.warn("Failed GCE unicast discovery, no unicast ping found");
			}
		}
	}
}
