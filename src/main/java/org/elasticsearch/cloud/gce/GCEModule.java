package org.elasticsearch.cloud.gce;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

/**
 * Binds the GCE service as a module that is available to other modules.
 */
public class GCEModule extends AbstractModule {
	private final ESLogger	logger	= Loggers.getLogger(getClass());

	@Override
	protected void configure() {
		this.logger.debug("Binding GCEService as Singleton");
		bind(GCEService.class).asEagerSingleton();
	}
}
