package com.couchbase.client.util;

import com.couchbase.client.InMemoryRequestTracer;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

public class CouchbaseResources {
  private static final Logger logger = LoggerFactory.getLogger(CouchbaseResources.class);
  private final Cluster cluster;
  private final Bucket bucket;

  public CouchbaseResources(InMemoryRequestTracer tracer) {
    Properties properties = loadProperties();
    cluster = Cluster.connect(properties.getProperty("cluster.hostname"),
      ClusterOptions.clusterOptions(properties.getProperty("cluster.username"), properties.getProperty("cluster.password"))
        .environment(env -> env.requestTracer(tracer)));
    bucket = cluster.bucket(properties.getProperty("cluster.bucket.name"));
  }

  public Cluster cluster() {
    return cluster;
  }

  public Bucket bucket() {
    return bucket;
  }

  public void close() {
    cluster.close();
  }

  private static Properties loadProperties() {
    try {
      Properties defaults = new Properties();

      // This file is unversioned.  Good practice is to copy integration.properties to this and make changes to this.
      URL url = CouchbaseResources.class.getResource("/com/couchbase/client/integration.local.properties");
      if (url != null) {
        logger.info("Found test config file {}", url.getPath());
        defaults.load(url.openStream());
      } else {
        url = CouchbaseResources.class.getResource("/com/couchbase/client/integration.properties");
        if (url != null) {
          logger.info("Found test config file {}", url.getPath());
          defaults.load(url.openStream());
        } else {
          throw new RuntimeException("Could not locate integration.properties file");
        }
      }

      Properties all = new Properties(System.getProperties());
      for (Map.Entry<Object, Object> property : defaults.entrySet()) {
        if (all.getProperty(property.getKey().toString()) == null) {
          all.put(property.getKey(), property.getValue());
        }
      }

      return all;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
