Elasticsearch-GCE-Discovery
===========================
A plugin for Elasticsearch to enable discovery in the Google Compute Engine, as well as allowing the plugin to automatically set node attributes, similar to the AWS plugin.

Also check out the official GCE cloud plugin, which has better documentation is maintained more actively:
https://github.com/elasticsearch/elasticsearch-cloud-gce

# Building

To build the plugin you need to have maven installed. With that in mind simply check out the project and run "mvn package" in the project directory. The plugin should then be available under target/release as a .zip file.

# Installation

Just copy the .zip file on the elasticsearch server should be using the plugin and run the "plugin" script coming with elasticsearch in the bin folder.

An Exmaple how one would call the plugin script:

	/my/elasticsearch/bin/plugin -install cloud-gce -url file:///path/to/plugin/elasticsearch-gce-discovery.zip

The plugin needs to be installed on all nodes of the ES cluster.
There are two ways to authenticate against Googles services to allow a node to look up information:

1. Using a client app authentication.	

    For this you will need to set set the client_id and client_secret you got from the Google API console. When you launch an Elasticsearch node for the first time, it will output an address that you need to open to authenticate the client with your account. To find the link run Elasticsearch in foreground mode:

        /my/elasticsearch/bin/elasticsearch -f
	
2. Using a service account

    For this you will need to fetch the p12 file and the client_email from the Google API console. Set the client_id to use the client_email and set the location of the p12 file. The file itself can be compiled into the plugin itself and accessed via classpath:... or the file can come from an external source. This method does not need any further interaction when starting up new Elasticsearch nodes.

    Whenever the p12 file cannot be found, the plugin will fall back to using the client app authentication.

# Configuration

	discovery:
	  type: GCE
	  gce:
	    client_id: <MyClientId>
	    client_secret: <MySecret>
	    client_p12_location: can be classpath:..., file://... or just a standard file location
	    credential_location: defaults/to/<home>/.credentials/compute-engine.json
	    project: my:project-id
	    tags: [tags, limit, where, the, plugin, searches, for, nodes]
	    metadata:
	    	limits: to instances with this key:value pair
	    	is_interpreted: as.a.[regex](match)?_eg:
	    	name: p_es_[0-9]*
	    zones: [can, limit, to, zones]
	    network: [or, networks]
	    network_if: defaults to 0
	cloud.node.auto_attributes: <defaults to true>
