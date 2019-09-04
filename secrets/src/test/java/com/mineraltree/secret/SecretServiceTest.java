package com.mineraltree.secret;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.mineraltree.extern.Environment;
import com.mineraltree.secret.SecretService.Externs;
import com.mineraltree.secret.provider.AwsVault;
import com.mineraltree.secret.provider.FileVault;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import junit.framework.AssertionFailedError;
import org.joor.Reflect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecretServiceTest {

  private Environment mockEnv = mock(Environment.class);
  private Externs mockExterns = mock(Externs.class);

  @BeforeEach
  void setUpTest() {

    // Force the static 'vault' member to null so we can call initialize
    // lotsa times (for each test case)
    Reflect.on(SecretService.class).set("vault", null);

    SecretService.environment = mockEnv;
    SecretService.externs = mockExterns;
  }

  @Test
  void testDefaultAws() {
    when(mockEnv.getenv(anyString())).thenReturn(null);
    mockCreateAwsVault();

    SecretService.initializeVault(ConfigFactory.empty());

    verify(mockExterns).makeAwsVault(ConfigFactory.empty());
    verifyEnvironmentRead();
  }

  @Test
  void testEnvAws() {
    when(mockEnv.getenv(anyString())).thenReturn("aws");
    mockCreateAwsVault();

    SecretService.initializeVault(ConfigFactory.empty());

    verifyEnvironmentRead();
    verify(mockExterns).makeAwsVault(ConfigFactory.empty());
    verifyNoMoreInteractions(mockExterns);
  }

  @Test
  void testEnvFile() {
    when(mockEnv.getenv(anyString())).thenReturn("a/path/to/a/file");
    mockCreateFileVault();

    SecretService.initializeVault(ConfigFactory.empty());

    verify(mockEnv).getenv("MT_SECRET_SOURCE");
    verifyNoMoreInteractions(mockEnv);
    verifyEnvironmentRead();
  }

  @Test
  void testConfigFile() {
    Config fileConfig = ConfigFactory.parseString("secretSource=a/config/file/path");
    mockCreateFileVault();

    SecretService.initializeVault(fileConfig);

    verify(mockExterns).makeFileVault("a/config/file/path");
    verifyNoMoreInteractions(mockExterns);
  }

  @Test
  void testEnvOverridesConfig() {
    when(mockEnv.getenv(anyString())).thenReturn("aws");
    Config fileConfig = ConfigFactory.parseString("secretSource=a/config/file/path");
    mockCreateAwsVault();

    SecretService.initializeVault(fileConfig);

    verify(mockExterns).makeAwsVault(fileConfig);
    verifyEnvironmentRead();
  }

  private void verifyEnvironmentRead() {
    verify(mockEnv).getenv("MT_SECRET_SOURCE");
    verifyNoMoreInteractions(mockEnv);
  }

  private void mockCreateFileVault() {
    FileVault mockVault = mock(FileVault.class);
    when(mockExterns.makeFileVault(anyString())).thenReturn(mockVault);
    when(mockExterns.makeAwsVault(ConfigFactory.empty()))
        .thenThrow(new AssertionFailedError("Test was not supposed to create an AWS Vault!"));
  }

  private void mockCreateAwsVault() {
    AwsVault mockVault = mock(AwsVault.class);
    when(mockExterns.makeAwsVault(any(Config.class))).thenReturn(mockVault);
    when(mockExterns.makeFileVault(anyString()))
        .thenThrow(new AssertionFailedError("Test was not supposed to create a FileVault"));
  }
}
