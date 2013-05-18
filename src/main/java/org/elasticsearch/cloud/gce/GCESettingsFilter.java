package org.elasticsearch.cloud.gce;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.SettingsFilter;

/**
 * Removes settings from the settings filter so that they are not processed by other plugins.
 */
public class GCESettingsFilter implements SettingsFilter.Filter {

	@Override
	public void filter(final ImmutableSettings.Builder settings) {
		settings.remove("cloud.gce.client_id");
		settings.remove("cloud.gce.client_secret");
		settings.remove("cloud.gce.project");
		settings.remove("cloud.gce.network");
		settings.remove("cloud.gce.tags");
		settings.remove("cloud.gce.metadata");
		settings.remove("cloud.gce.zones");
	}
}
