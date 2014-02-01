package org.elasticsearch.discovery.gce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;

import org.apache.lucene.util.IOUtils;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.network.NetworkService.CustomNameResolver;
import org.elasticsearch.common.settings.Settings;

/**
 * Looks up the IP for this instance, using the GCE meta data URL. This class seems only to be called to resolve the nodes
 * own address. This probably also doesn't work on app engine, so that ElasticSearch will have to try and figure this one out
 * on it's own.
 * 
 * @author mallocator
 */
public class GCENameResolver implements CustomNameResolver {
	private final ESLogger	logger	= Loggers.getLogger(getClass());
	private final String	networkInterface;

	public GCENameResolver(final Settings settings) {
		this.networkInterface = settings.get("discovery.get.network_if", "0");
		this.logger.debug("Initialized GCENameResolver");
	}

	@Override
	public InetAddress resolveDefault() {
		return null;
	}

	@Override
	public InetAddress resolveIfPossible(final String value) {
		this.logger.debug("Using GCE Custom Name Resolver to resolve {}", value);
		InputStream in = null;
		try {
			final URL url = new URL("http://metadata/computeMetadata/v1beta1/instance/network-interfaces/" + this.networkInterface + "/ip");
			final URLConnection urlConnection = url.openConnection();
			urlConnection.setConnectTimeout(2000);
			in = urlConnection.getInputStream();
			final BufferedReader urlReader = new BufferedReader(new InputStreamReader(in));
			final String metadataResult = urlReader.readLine();
			if (metadataResult == null || metadataResult.length() == 0) {
				this.logger.error("No GCE metadata found at {}", url);
				return null;
			}
			return InetAddress.getByName(metadataResult);
		} catch (IOException e) {
			this.logger.debug("There was an error retrieving meta data from GCE: " + ExceptionsHelper.detailedMessage(e));
			return null;
		} finally {
			IOUtils.closeWhileHandlingException(in);
		}
	}
}
