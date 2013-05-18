package org.elasticsearch.discovery.gce;

import org.elasticsearch.discovery.Discovery;
import org.elasticsearch.discovery.zen.ZenDiscoveryModule;

/**
 * A wrapper to bind the GCE discovery as a singleton to the available discovery modules.
 */
public class GCEDiscoveryModule extends ZenDiscoveryModule {

	@Override
	protected void bindDiscovery() {
		bind(Discovery.class).to(GCEDiscovery.class).asEagerSingleton();
	}
}
