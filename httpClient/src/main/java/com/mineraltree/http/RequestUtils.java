package com.mineraltree.http;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mineraltree.api.marshal.Marshal;
import java.util.concurrent.CompletionStage;

/**
 * Helper utilities for dealing with HTTP request/response actions. Provides some syntactic sugar
 * for dealing with the asynchronous HTTP requests and their associated responses.
 */
public class RequestUtils {

  private final Http http;
  private final Materializer materializer;

  public RequestUtils(ActorSystem system) {
    materializer = ActorMaterializer.create(system);
    http = Http.get(system);
  }

  public CompletionStage<WrappedResponse> singleRequest(HttpRequest request) {
    return http.singleRequest(request).thenApply(r -> new WrappedResponse(r, materializer));
  }

  public ObjectNode makeObjectNode() {
    return Marshal.MAPPER.createObjectNode();
  }

  public String marshalToJson(Object payload) {
    try {
      return Marshal.MAPPER.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException("Failed to convert object payload to JSON", ex);
    }
  }
}
