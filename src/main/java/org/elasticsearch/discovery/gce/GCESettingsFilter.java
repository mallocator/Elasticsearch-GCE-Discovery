package org.elasticsearch.discovery.gce;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.SettingsFilter;

/**
 * Removes settings via the settings filter so that they are not processed by other plugins.
 */
public class GCESettingsFilter implements SettingsFilter.Filter {
	private final ESLogger	logger	= Loggers.getLogger(getClass());

	@Override
	public void filter(final ImmutableSettings.Builder settings) {
		this.logger.debug("Filtering GCE Settings");
		settings.remove("discovery.gce.client_id");
		settings.remove("discovery.gce.client_secret");
	}
}
