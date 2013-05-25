package org.elasticsearch.discovery.gce;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.discovery.Discovery;
import org.elasticsearch.discovery.zen.ZenDiscoveryModule;

/**
 * A wrapper to bind the GCE discovery as a singleton to the available discovery modules.
 */
public class GCEDiscoveryModule extends ZenDiscoveryModule {
	private final ESLogger	logger	= Loggers.getLogger(getClass());

	@Override
	protected void bindDiscovery() {
		this.logger.debug("Binding GCEDiscoveryModue as Singleton");
		bind(Discovery.class).to(GCEDiscovery.class).asEagerSingleton();
	}
}
