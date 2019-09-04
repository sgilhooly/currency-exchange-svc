package com.mineraltree.secret;

import com.mineraltree.extern.Environment;
import com.mineraltree.secret.provider.AwsVault;
import com.mineraltree.secret.provider.FileVault;
import com.typesafe.config.Config;

/**
 * Manages access to the secret vault. This vault stores secrets that are required by the service
 * runtime. It only allows reading of the secret values.
 */
public class SecretService {

  private static SecretVault vault;
  static Environment environment = new Environment();
  static Externs externs = new Externs();

  static class Externs {
    AwsVault makeAwsVault(Config config) {
      return new AwsVault(config);
    }

    FileVault makeFileVault(String filePathname) {
      return new FileVault(filePathname);
    }
  }

  private static final String ENVIRONMENT_OVERRIDE_VAR = "MT_SECRET_SOURCE";
  private static final String CONFIG_SETTING_KEY = "secretSource";
  private static final String CONFIG_AWS = "AWS";

  /**
   * Initializes the secret vault retrieval location. This method {@code must} be called once during
   * application startup.
   */
  public static synchronized void initializeVault(Config config) {

    if (null != vault) {
      throw new RuntimeException(
          "SecretService was already initialized. Attempted re-initialization of service not allowed");
    }

    String sourceLocation = environment.getenv(ENVIRONMENT_OVERRIDE_VAR);
    if (null == sourceLocation && config.hasPath(CONFIG_SETTING_KEY)) {
      sourceLocation = config.getString(CONFIG_SETTING_KEY);
    }

    if (null == sourceLocation || sourceLocation.equalsIgnoreCase(CONFIG_AWS)) {
      vault = externs.makeAwsVault(config);
    } else {
      vault = externs.makeFileVault(sourceLocation);
    }
  }

  /** Returns a reference to the vault where secrets are stored. */
  public static SecretVault getVault() {
    if (null == vault) {
      throw new RuntimeException(
          "Attempt to acquire vault reference from un-initialized service. "
              + "SecretService must be initialized before first use");
    }
    return vault;
  }
}
