package com.mineraltree.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.net.URL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConfigurationFetcherTest {

  private final SystemResolver resolver = mock(SystemResolver.class);
  private final ConfigParser parser = mock(ConfigParser.class);

  private void runBasic(String appLocation, String secretLocation) {

    when(resolver.resolveProperty(ConfigInput.APPLICATION)).thenReturn(appLocation);
    when(resolver.resolveProperty(ConfigInput.SECRET)).thenReturn(secretLocation);

    ConfigurationFetcher fetcher = new ConfigurationFetcher(resolver, parser);
    Config configSettings = fetcher.getConfigSettings();

    verify(resolver).resolveProperty(ConfigInput.APPLICATION);
    verify(resolver).resolveProperty(ConfigInput.SECRET);

    Assertions.assertEquals("true", configSettings.getString("test"));
    Assertions.assertEquals("another", configSettings.getString("mineraltree.second"));
  }

  @Test
  void testDefaultConfig() {
    when(parser.parseFile(any(File.class)))
        .thenReturn(ConfigFactory.parseString("test=\"true\""))
        .thenReturn(ConfigFactory.parseString("second=\"another\""));

    runBasic("file:///etc/file.conf", "file:///etc/secretfile.conf");

    verify(parser).parseFile(new File("/etc/file.conf"));
    verify(parser).parseFile(new File("/etc/secretfile.conf"));
  }

  @Test
  void testUrlAndClasspath() throws Exception {
    when(parser.parseUrl(any(URL.class))).thenReturn(ConfigFactory.parseString("test=\"true\""));
    when(parser.parseResource(anyString()))
        .thenReturn(ConfigFactory.parseString("second=\"another\""));
    runBasic("http://somehost/my/config/app.conf", "classpath:/web-inf/secret.conf");

    // Verify the URL with a captor since otherwise the 'equals' operator will make networks calls
    ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);
    verify(parser).parseUrl(urlCaptor.capture());
    Assertions.assertEquals("somehost", urlCaptor.getValue().getHost());
    Assertions.assertEquals("/my/config/app.conf", urlCaptor.getValue().getPath());
    verify(parser).parseResource("/web-inf/secret.conf");
  }

  @Test
  void testBadConfig() {
    when(resolver.resolveProperty(ConfigInput.APPLICATION)).thenReturn("invalid stuff");
    when(resolver.resolveProperty(ConfigInput.SECRET)).thenReturn("file:///valid/enough.conf");

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          ConfigurationFetcher fetcher = new ConfigurationFetcher(resolver, parser);
          fetcher.getConfigSettings();
        });
  }

  /**
   * Tests that properties defined within a sub-section can properly override values stored at the
   * root level.
   */
  @Test
  void testLiftService() {
    when(parser.parseUrl(any(URL.class)))
        .thenReturn(
            ConfigFactory.parseString(
                "mineraltree.debugLevel = 10\n"
                    + "mineraltree.watermark=3\n"
                    + "mineraltree.inner {"
                    + "   debugLevel = 4\n"
                    + "}"));
    when(parser.parseResource(anyString()))
        .thenReturn(ConfigFactory.parseString("inner.newprop=\"innerprop\""));

    when(resolver.resolveProperty(ConfigInput.APPLICATION)).thenReturn("http://somehost/path");
    when(resolver.resolveProperty(ConfigInput.SECRET)).thenReturn("classpath:/prop");

    ConfigurationFetcher fetcher = new ConfigurationFetcher(resolver, parser);
    Config configSettings = fetcher.getServiceConfigSettings("inner");

    Assertions.assertEquals(3, configSettings.getInt("watermark"));
    Assertions.assertEquals(4, configSettings.getInt("debugLevel"));
    Assertions.assertEquals("innerprop", configSettings.getString("newprop"));
  }

  /**
   * Tests that when a service config is requested and that service does not have any overrides that
   * need to be "lifted", it will properly accept all the defaults.
   */
  @Test
  void testLiftDefaultService() {
    when(parser.parseUrl(any(URL.class)))
        .thenReturn(
            ConfigFactory.parseString(
                "mineraltree.debugLevel = 10\n" + "mineraltree.watermark=3\n"));
    when(parser.parseResource(anyString()))
        .thenReturn(ConfigFactory.parseString("newprop=\"otherprop\""));

    when(resolver.resolveProperty(ConfigInput.APPLICATION)).thenReturn("http://somehost/path");
    when(resolver.resolveProperty(ConfigInput.SECRET)).thenReturn("classpath:/prop");

    ConfigurationFetcher fetcher = new ConfigurationFetcher(resolver, parser);
    Config configSettings = fetcher.getServiceConfigSettings("inner");

    Assertions.assertEquals(3, configSettings.getInt("watermark"));
    Assertions.assertEquals(10, configSettings.getInt("debugLevel"));
    Assertions.assertEquals("otherprop", configSettings.getString("newprop"));
  }
}
