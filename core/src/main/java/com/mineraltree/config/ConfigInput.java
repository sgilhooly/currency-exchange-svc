package com.mineraltree.config;

/**
 * Represents a configuration file. Any files used to configure the service are identified by this
 * class. Ideally this class will remain small since we don't want to have a lot of different files
 * involved in configuring the services.
 *
 * <p>Each file can be specified by either an environment variable or a system property; if not a
 * default location.
 */
enum ConfigInput {
  APPLICATION("MT_APP_URL", "mt.app.url", "classpath:/application.conf"),
  SECRET("MT_SECRETS", "mt.secrets", "file:////etc/mineraltree/.config/secrets.conf");

  private final String environmentName;
  private final String sysPropertyName;
  private final String defaultUrl;

  ConfigInput(String environmentName, String sysPropertyName, String defaultUrl) {
    this.environmentName = environmentName;
    this.sysPropertyName = sysPropertyName;
    this.defaultUrl = defaultUrl;
  }

  /**
   * Returns the name of the environment variable used to identify this configuration file location
   */
  public String getEnvironmentName() {
    return environmentName;
  }

  /** Returns the name of the systme property used to identify this configuration file location */
  public String getSysPropertyName() {
    return sysPropertyName;
  }

  /**
   * Returns the default location of this configuration file. Use this value only if the environment
   * variable and system property is not set in the current runtime environment.
   */
  public String getDefaultUrl() {
    return defaultUrl;
  }
}
