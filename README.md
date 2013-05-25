Elasticsearch-GCE-Discovery
===========================

A plugin for Elasticsearch to enable discovery in the Google Compute Engine, as well as allowing the plugin to automatically set node attributes, similar to the AWS plugin.

# Building

To build the plugin you need to have maven installed. With that in mind simply check out the project and run "mvn package" in the project directory. The plugin should then be available under target/release as a .zip file.

# Installation

Just copy the .zip file on the elasticsearch server should be using the plugin and run the "plugin" script coming with elasticsearch in the bin folder.

An Exmaple how one would call the plugin script:

	/my/elasticsearch/bin/plugin install cloud-gce -url file:///path/to/plugin/elasticsearch-gce-discovery.zip

The plugin needs to be installed on all nodes of the ES cluster.

Unfortunately the plugin requires you to manually go through the OAuth process and open a URL. Once you've followed this procedure on each instance, they should store the credentials on a local file.

# Configuration

	discovery:
	  type: GCE
	  gce:
	    client_id: <MyClientId>
	    client_secret: <MySecret>
	    credential_location: defaults/to/<home>/.credentials/compute-engine.json
	    project: my:project-id
	    tags: [tags, limit, where, the, plugin, searches, for, nodes]
	    metadata:
	    	limits: to instances with this key:value pair
	    	is_interpreted: as.a.[regex](match)?_eg:
	    	name: p_es_[0-9]*
	    zones: [can, limit, to, zones]
	    network: [or, networks]
	cloud.node.auto_attributes: <defaults to true>