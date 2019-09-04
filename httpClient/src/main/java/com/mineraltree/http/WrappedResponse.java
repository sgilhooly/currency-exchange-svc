package com.mineraltree.http;

import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.ContentType;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCode;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.stream.Materializer;
import com.mineraltree.utils.HttpHeaderKey;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WrappedResponse {
  private static final Logger log = LoggerFactory.getLogger(WrappedResponse.class);

  private final HttpResponse realResponse;
  private final Materializer materializer;

  public WrappedResponse(HttpResponse realResponse, Materializer materializer) {
    this.realResponse = realResponse;
    this.materializer = materializer;
  }

  /**
   * Ensures that the response status code matches the code expected
   *
   * @param expected the status code the response is expected to have
   * @return returns the wrapped response ({@code this}) passed through to allow this method to be
   *     used in stream operations
   */
  public WrappedResponse assertStatus(StatusCode expected) {
    if (realResponse.status() != expected) {
      throw new RuntimeException(
          "HTTP response code "
              + realResponse.status()
              + " does not match expected status "
              + expected);
    }
    return this;
  }

  /**
   * Ensures that the response status code matches a successful code. A "successful" code is any in
   * the 1xx or 2xx ranges. Note that a redirection (a 3xx status) is <b>not</b> considered
   * successful.
   *
   * @return returns the wrapped response ({@code this}) passed through to allow this method to be
   *     used in stream operations
   */
  public WrappedResponse assertStatusSuccess() {
    if (!realResponse.status().isSuccess()) {

      String responseBody = extractRawEntity();

      throw new RuntimeException(
          "HTTP response code "
              + realResponse.status()
              + " is not a successful status: "
              + responseBody);
    }
    return this;
  }

  private String extractRawEntity() {
    try {
      CompletionStage<String> messageBody =
          Unmarshaller.entityToString().unmarshal(realResponse.entity(), materializer);
      return messageBody.toCompletableFuture().get(5, TimeUnit.SECONDS);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted", ie);
    } catch (TimeoutException ex) {
      return "Cannot read response body. Timed out.";
    } catch (ExecutionException ex) {
      throw new RuntimeException("Unable to extract message body from response: " + realResponse);
    }
  }

  /**
   * Ensures taht the response status code is not a failure code. A "failure" status code is any
   * code in the 4xx or 5xx ranges. Note that a redirection (a 3xx status) is <b>not</b> considered
   * failure.
   *
   * @return returns the wrapped response ({@code this}) passed through to allow this method to be
   *     used in stream operations
   */
  public WrappedResponse assertStatusNotFailed() {
    if (realResponse.status().isFailure()) {
      realResponse.discardEntityBytes(materializer);
      throw new RuntimeException(
          "HTTP response code " + realResponse.status() + " is a failure status");
    }
    return this;
  }

  /**
   * Ensures that the response content type matches the content type expected.
   *
   * @param expectedType the content type expected in the response
   * @return returns the wrapped response ({@code this}) passed through to allow this method to be
   *     used in stream operations
   */
  public WrappedResponse assertContentType(ContentType expectedType) {

    realResponse
        .getHeader(akka.http.javadsl.model.headers.ContentType.class)
        .map(
            t -> {
              if (!t.contentType().equals(expectedType)) {
                realResponse.discardEntityBytes(materializer);
                log.error("Content type BAD!");
                throw new RuntimeException(
                    "HTTP response has unexpected content-type \""
                        + t
                        + "\". Expected: "
                        + expectedType);
              }
              return t;
            })
        .orElseThrow(
            () -> {
              realResponse.discardEntityBytes(materializer);
              return new RuntimeException(
                  "HTTP response is missing expected \"Content-Type\" header: " + realResponse);
            });

    return this;
  }

  /**
   * Unmarshals the response body to the given class type.
   *
   * @param payloadType the data type to unmarshal the response body to
   * @return a completion stage which will resolve to the unmarshalled object
   */
  public <T> CompletionStage<T> extractPayload(Class<T> payloadType) {
    return Jackson.unmarshaller(payloadType).unmarshal(realResponse.entity(), materializer);
  }

  public String getResponseHeader(HttpHeaderKey headerName) {
    return realResponse
        .getHeader(headerName.getKey())
        .orElseThrow(
            () -> new RuntimeException("Response did not contain expected header: " + headerName))
        .value();
  }
}
