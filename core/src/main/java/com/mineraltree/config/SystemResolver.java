package com.mineraltree.config;

import com.mineraltree.utils.ObjectUtil;

/**
 * Helper class to resolve the location of a configuration file. Since locating a file involves a
 * lot of static system methods, making it difficult to test, this class extracts that functionality
 * into an isolated routine. This makes the functionality mockable enabling the code using it
 * testable.
 */
class SystemResolver {

  /**
   * Returns the configured location for the requested property. Checks the environment variable,
   * then the system property for a configuration file name. If neither are found, returns the
   * default location for the setting.
   *
   * @param input the setting to return the desired location of
   */
  String resolveProperty(ConfigInput input) {
    final String envLocation = System.getenv(input.getEnvironmentName());
    final String propLocation =
        System.getProperty(input.getSysPropertyName(), input.getDefaultUrl());
    return ObjectUtil.firstNonNull(envLocation, propLocation);
  }
}
