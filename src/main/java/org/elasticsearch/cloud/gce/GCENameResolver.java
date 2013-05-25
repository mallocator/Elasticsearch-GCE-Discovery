package org.elasticsearch.cloud.gce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;

import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.io.Closeables;
import org.elasticsearch.common.network.NetworkService.CustomNameResolver;
import org.elasticsearch.common.settings.Settings;

/**
 * Looks up the IP for this instance, using the GCE meta data URL. This class seems only to be called to resolve the nodes
 * own address. This probably also doesn't work on app engine, so that ElasticSearch will have to try and figure this one out
 * on it's own.
 * 
 * @author mallocator
 */
public class GCENameResolver extends AbstractComponent implements CustomNameResolver {

	public GCENameResolver(final Settings settings) {
		super(settings);
		this.logger.debug("Initialized GCENameResolver");
	}

	@Override
	public InetAddress resolveDefault() {
		return null;
	}

	@Override
	public InetAddress resolveIfPossible(final String value) {
		this.logger.debug("Using GCE Custom Name Resolver to resolve {}", value);
		URLConnection urlConnection = null;
		InputStream in = null;
		try {
			final URL url = new URL("http://metadata/computeMetadata/v1beta1/instance/network-interfaces/0/ip");
			this.logger.debug("Looking up (internal) ip from GCE meta data url {}", url);
			urlConnection = url.openConnection();
			urlConnection.setConnectTimeout(2000);
			in = urlConnection.getInputStream();
			BufferedReader urlReader = new BufferedReader(new InputStreamReader(in));

			String metadataResult = urlReader.readLine();
			if (metadataResult == null || metadataResult.length() == 0) {
				this.logger.error("No GCE metadata found at {}", url);
				return null;
			}
			return InetAddress.getByName(metadataResult);
		} catch (IOException e) {
			this.logger.debug("There was an error retrieving meta data from GCE: " + ExceptionsHelper.detailedMessage(e));
			return null;
		} finally {
			Closeables.closeQuietly(in);
		}
	}
}
