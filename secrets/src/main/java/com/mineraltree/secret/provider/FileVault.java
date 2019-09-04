package com.mineraltree.secret.provider;

import com.mineraltree.secret.SecretVault;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A file based secret vault. This vault is intended for testing purposes though it is possible,
 * with sufficient precautions, to use this safely as a primary secret vault.
 */
public class FileVault implements SecretVault {

  private final Config rootSecrets;
  private final Path configFileName;

  public FileVault(String filePathname) {
    configFileName = Paths.get(filePathname);
    if (Files.notExists(configFileName)) {
      throw new IllegalArgumentException(
          "Cannot initialize secret vault from file '" + filePathname + "'. File not found");
    }
    rootSecrets = ConfigFactory.parseFile(configFileName.toFile());
  }

  @Override
  public Config getSecrets(String secretId) {
    if (!rootSecrets.hasPath(secretId)) {
      throw new IllegalArgumentException(
          "Missing Secret with ID=" + secretId + " in configuration file " + configFileName);
    }
    return rootSecrets.getConfig(secretId);
  }
}
