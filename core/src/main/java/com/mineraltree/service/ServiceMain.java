package com.mineraltree.service;

import static com.mineraltree.utils.Ensure.verifyNotNull;
import static com.mineraltree.utils.Ensure.verifyState;

import akka.actor.ActorSystem;
import com.mineraltree.api.ApiMain;
import com.mineraltree.api.rest.ApiRouter;
import com.mineraltree.config.ConfigurationFetcher;
import com.typesafe.config.Config;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main service entry-point for the login service. Handles all the bootstrapping, configuration, and
 * initialization of the service.
 */
public abstract class ServiceMain {

  private static final Logger log = LoggerFactory.getLogger(ServiceMain.class);

  private final String serviceName;
  private ActorSystem system;
  protected final Config config;

  public ServiceMain(String serviceName) {
    this.serviceName = serviceName;
    config = ConfigurationFetcher.getInstance().getServiceConfigSettings(serviceName);
  }

  /**
   * Creates an API server using the provided router and configuration.
   *
   * @param router a router that defines how to handle incoming REST requests
   * @param serverConfig configuration settings for the REST server endpoint. Specifies the
   *     listening IP and port combination.
   */
  protected void startApiServer(ApiRouter router, Config serverConfig) {
    verifyNotNull(router, "router");
    verifyNotNull(serverConfig, "serverConfig");
    verifyState(system, Objects::nonNull, "Actor system not properly initialized");

    ApiMain apiMain = new ApiMain(router, system);
    apiMain.startApi(
        throwable -> {
          final Logger log = LoggerFactory.getLogger(ServiceMain.class);
          log.error("Unable to initialize API listening service", throwable);
          System.exit(9);
        },
        serverConfig);
  }

  protected abstract void initializeServers(ActorSystem system);

  /** Starts things up */
  public void initialize() {

    try {
      // Start up the actor system. This will start up the background servicing and take over
      // lifecycle
      // ownership of the process
      system =
          ActorSystem.create(serviceName, ConfigurationFetcher.getInstance().getConfigSettings());
      system
          .getWhenTerminated()
          .thenAccept(
              terminated -> {
                final Logger log = LoggerFactory.getLogger(ServiceMain.class);
                log.error("Actor system terminated. System exit: {}", terminated.toString());
                System.exit(0);
              });
      initializeServers(system);
    } catch (RuntimeException ex) {
      final Logger log = LoggerFactory.getLogger(ServiceMain.class);
      log.error("This isn't good. Service initialization failed", ex);
      System.exit(100);
    }
  }
}
