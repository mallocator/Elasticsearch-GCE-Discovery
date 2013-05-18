package org.elasticsearch.cloud.gce;

import org.elasticsearch.common.inject.AbstractModule;

/**
 * Binds the GCE service as a module that is available to other modules.
 */
public class GCEModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(GCEService.class).asEagerSingleton();
	}
}
