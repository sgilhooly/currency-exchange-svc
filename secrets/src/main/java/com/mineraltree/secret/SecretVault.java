package com.mineraltree.secret;

import com.typesafe.config.Config;

public interface SecretVault {

  Config getSecrets(String secretId);
}
