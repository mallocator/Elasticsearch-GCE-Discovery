package org.elasticsearch.plugin.cloud.gce;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

/**
 * Registers the discovery module with elastic search.
 */
public class CloudGCEPlugin extends AbstractPlugin {
	private final ESLogger	logger	= Loggers.getLogger(getClass());

	public CloudGCEPlugin(final Settings settings) {
		this.logger.debug("Initialized CloudGCEPlugin");
	}

	@Override
	public String name() {
		return "cloud-gce";
	}

	@Override
	public String description() {
		return "Cloud GCE Plugin";
	}
}
