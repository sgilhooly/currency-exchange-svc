package com.mineraltree.api;

import static com.mineraltree.utils.Ensure.verify;
import static com.mineraltree.utils.Ensure.verifyNotNull;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import com.mineraltree.api.rest.ApiRouter;
import com.typesafe.config.Config;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Initializes the API handler service. The API module accepts incoming HTTP requests, and sends
 * them through to the "routes". The routes figure out what to do with the request and return a
 * corresponding response. Here, this simply initializes the system to get it connected to the
 * listening port and the actor system.
 */
public class ApiMain {

  /** The router which defines how to handle the incoming requests */
  private final ApiRouter router;
  /** The actor system that the routes execute within */
  private final ActorSystem system;

  public ApiMain(ApiRouter apiRouter, ActorSystem system) {
    this.router = verifyNotNull(apiRouter, "apiRouter");
    this.system = verifyNotNull(system, "actorSystem");
  }

  /**
   * Starts the API server. Call this once from the beginning of the program {@code main}. It will
   * create the listening port and ready the service to accept requests.
   *
   * @param onError a callback function to invoke if an error occurs trying to bind to the listening
   *     port
   * @param serverConfig a configuration object containing 'listen' and 'port' fields indicating
   *     what interface:port this server should listen on.
   */
  public void startApi(Consumer<Throwable> onError, Config serverConfig) {
    verifyNotNull(system, "actorSystem");
    verify(
        serverConfig,
        c -> c.hasPath("listen"),
        "serverConfig",
        "Configuration is missing property 'listen'");
    verify(
        serverConfig,
        c -> c.hasPath("port"),
        "serverConfig",
        "Configuration is missing property 'port'");
    final ActorMaterializer materializer = ActorMaterializer.create(system);

    final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow =
        router.getRouter().flow(system, materializer);
    final Http http = Http.get(system);
    final CompletionStage<ServerBinding> binding =
        http.bindAndHandle(
            routeFlow,
            ConnectHttp.toHost(serverConfig.getString("listen"), serverConfig.getInt("port")),
            materializer);

    binding.exceptionally(
        throwable -> {
          onError.accept(throwable);
          throw new RuntimeException(
              "Port binding to ip:port '"
                  + serverConfig.getString("listen")
                  + ":"
                  + serverConfig.getInt("port")
                  + "' failed",
              throwable);
        });
  }
}
