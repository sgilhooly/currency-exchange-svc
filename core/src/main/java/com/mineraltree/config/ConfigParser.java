package com.mineraltree.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.net.URL;

/**
 * Helper class to handle parsing config files. Extracts static parsing methos into a mockable class
 * enabling the ability to unit test code which requires this functionality.
 */
class ConfigParser {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConfigParser.class);

  Config parseFile(File inputFile) {
    log.debug("Loading configuration from file {}", inputFile);
    Config config = ConfigFactory.parseFile(inputFile);
    return config;
  }

  Config parseResource(String resourcePath) {
    log.debug("Loading configuration from resource {}", resourcePath);
    Config config = ConfigFactory.parseResources(this.getClass(), resourcePath);
    return config;
  }

  Config parseUrl(URL url) {
    log.debug("Loading configuration from URL {}", url);
    return ConfigFactory.parseURL(url);
  }
}
