package com.mineraltree.config;

import com.typesafe.config.Config;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Retrieves configuration files needed by the application. This class manages access to the
 * configuration required by the application. Configuration is loaded from multiple sources
 * identified by the {@link ConfigInput} values.
 */
public final class ConfigurationFetcher {

  /** Singleton instance */
  private static final ConfigurationFetcher FETCHER = new ConfigurationFetcher();

  private static final String FILE_MARKER_PREFIX = "file://";
  private static final String CLASSPATH_MARKER_PREFIX = "classpath:";
  private static final String APPLICATION_CONFIG_GROUP = "mineraltree";

  /** Returns the singleton configuration fetcher instance */
  public static ConfigurationFetcher getInstance() {
    return FETCHER;
  }

  private final SystemResolver resolver;
  private final ConfigParser configParser;
  private Config contents;

  /** Constructor. Don't use this directly. Use the {@link #getInstance()} method instead. */
  private ConfigurationFetcher() {
    this(new SystemResolver(), new ConfigParser());
  }

  /** Constructor for testing */
  ConfigurationFetcher(SystemResolver resolver, ConfigParser configParser) {
    this.resolver = resolver;
    this.configParser = configParser;
  }

  /**
   * Retrieves an individual configuration file. Given the name of a configuration document,
   * retrieves and returns the contents of the file stored with that name.
   *
   * @return the contents of the configuration file with the given name
   */
  public Config getConfigSettings() {

    if (null == contents) {
      loadConfig();
    }
    return contents;
  }

  /**
   * Returns the configuration settings configured for a particular service. The configuration can
   * include overrides for services. This method will return a configuration set which has the
   * overrides applied. For example with the configuration:
   *
   * <pre>
   *     debugLevel = 3
   *     watermarkLevel = 10
   *     special {
   *         debugLevel = 9
   *     }
   * </pre>
   *
   * If this method is called as: {@code getServiceConfigSettings("special")} then calling {@code
   * config.getInt("debugLevel")} would return 9 and {@code config.getInt("watermarkLevel")} would
   * return 10.
   */
  public Config getServiceConfigSettings(String serviceName) {
    if (null == contents) {
      loadConfig();
    }
    final String servicePath = APPLICATION_CONFIG_GROUP + "." + serviceName;
    if (contents.hasPath(servicePath)) {
      return contents
          .getConfig(servicePath)
          .withFallback(contents.getConfig(APPLICATION_CONFIG_GROUP));
    }
    return contents.getConfig(APPLICATION_CONFIG_GROUP);
  }

  /** Loads the configuration if necessary and returns the parsed and merged values. */
  private synchronized void loadConfig() {
    if (null != contents) {
      return;
    }
    String appLocation = resolver.resolveProperty(ConfigInput.APPLICATION);
    String secretLocation = resolver.resolveProperty(ConfigInput.SECRET);

    Config config = parseConfigFile(appLocation);
    Config secretsConfig = parseConfigFile(secretLocation);

    contents = config.withFallback(secretsConfig.atPath(APPLICATION_CONFIG_GROUP));
  }

  /**
   * Parses an individual configuration file. Given the location of the configuration file, this
   * will parse it into a typesafe configuration object and return it.
   */
  private Config parseConfigFile(String location) {
    if (location.startsWith("http")) {
      try {
        return configParser.parseUrl(new URL(location));
      } catch (MalformedURLException ex) {
        throw new RuntimeException(
            "Unable to load configuration from " + location + ". Invalid URL", ex);
      }
    }
    if (location.startsWith(FILE_MARKER_PREFIX)) {
      return configParser.parseFile(new File(location.substring(FILE_MARKER_PREFIX.length())));
    }
    if (location.startsWith(CLASSPATH_MARKER_PREFIX)) {
      return configParser.parseResource(location.substring(CLASSPATH_MARKER_PREFIX.length()));
    }
    throw new IllegalArgumentException(
        "Invalid configuration location \""
            + location
            + "\". "
            + "Locations must start with 'http', 'file://', or 'classpath:'.");
  }
}
