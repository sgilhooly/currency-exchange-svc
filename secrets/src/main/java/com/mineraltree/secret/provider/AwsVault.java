package com.mineraltree.secret.provider;

import com.mineraltree.secret.SecretVault;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/** Manages secrets stored in a secure vault. */
public class AwsVault implements SecretVault {

  private final SecretsManagerClient secretClient;

  public AwsVault(Config config) {
    Region awsRegion = Region.of(config.getString("aws.region"));
    secretClient = SecretsManagerClient.builder().region(awsRegion).build();
  }

  @Override
  public Config getSecrets(String secretId) {

    GetSecretValueRequest getSecretRequest =
        GetSecretValueRequest.builder().secretId(secretId).build();

    GetSecretValueResponse secretValue = secretClient.getSecretValue(getSecretRequest);
    return ConfigFactory.parseString(secretValue.secretString());
  }
}
