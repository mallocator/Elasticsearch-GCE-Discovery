package org.elasticsearch.discovery.gce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.apache.lucene.util.IOUtils;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.cluster.node.DiscoveryNodeService;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

/**
 * Looks up the availability zone from gce and sets it as a custom node attribute.
 */
public class GCENodeAttributes implements DiscoveryNodeService.CustomAttributesProvider {
	private final ESLogger	logger	= Loggers.getLogger(getClass());
	private final Settings	settings;

	public GCENodeAttributes(final Settings settings) {
		this.settings = settings;
		this.logger.debug("Initialized GCENodeAttributes");
	}

	@Override
	public Map<String, String> buildAttributes() {
		this.logger.debug("Building node atrtibutes");
		if (!this.settings.getAsBoolean("cloud.node.auto_attributes", true)) {
			return null;
		}
		Map<String, String> gceAttributes = Maps.newHashMap();

		URLConnection urlConnection;
		InputStream in = null;
		try {
			URL url = new URL("http://metadata/computeMetadata/v1beta1/instance/zone");
			this.logger.debug("Retrieving GCE zone from GCE meta data url {}", url);
			urlConnection = url.openConnection();
			urlConnection.setConnectTimeout(2000);
			in = urlConnection.getInputStream();
			BufferedReader urlReader = new BufferedReader(new InputStreamReader(in));

			final String metadataResult = urlReader.readLine();
			if (metadataResult == null || metadataResult.length() == 0) {
				this.logger.error("No GCE meta data returned from {}", url);
				return null;
			}
			gceAttributes.put("gce_zone", metadataResult);
		} catch (IOException e) {
			this.logger.debug("Failed to get metadata: " + ExceptionsHelper.detailedMessage(e));
		} finally {
			IOUtils.closeWhileHandlingException(in);
		}

		return gceAttributes;
	}
}
