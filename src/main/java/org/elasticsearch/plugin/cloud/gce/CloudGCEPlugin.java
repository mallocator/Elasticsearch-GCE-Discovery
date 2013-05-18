package org.elasticsearch.plugin.cloud.gce;

import java.util.Collection;

import org.elasticsearch.cloud.gce.GCEModule;
import org.elasticsearch.cloud.gce.GCEService;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

/**
 * Registers the discovery module with elastic search.
 */
public class CloudGCEPlugin extends AbstractPlugin {

	private final Settings	settings;

	public CloudGCEPlugin(final Settings settings) {
		this.settings = settings;
	}

	@Override
	public String name() {
		return "cloud-gce";
	}

	@Override
	public String description() {
		return "Cloud GCE Plugin";
	}

	@Override
	public Collection<Class<? extends Module>> modules() {
		Collection<Class<? extends Module>> modules = Lists.newArrayList();
		if (this.settings.getAsBoolean("cloud.enabled", true)) {
			modules.add(GCEModule.class);
		}
		return modules;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Collection<Class<? extends LifecycleComponent>> services() {
		Collection<Class<? extends LifecycleComponent>> services = Lists.newArrayList();
		if (this.settings.getAsBoolean("cloud.enabled", true)) {
			services.add(GCEService.class);
		}
		return services;
	}
}
