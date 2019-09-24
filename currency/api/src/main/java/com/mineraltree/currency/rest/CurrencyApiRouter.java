package com.mineraltree.currency.rest;

import static akka.event.Logging.DebugLevel;
import static akka.http.javadsl.server.Directives.complete;
import static akka.http.javadsl.server.Directives.get;
import static akka.http.javadsl.server.Directives.handleExceptions;
import static akka.http.javadsl.server.Directives.logRequest;
import static akka.http.javadsl.server.Directives.onSuccess;
import static akka.http.javadsl.server.Directives.parameter;
import static akka.http.javadsl.server.Directives.pathPrefix;
import static com.mineraltree.utils.Ensure.verifyNotEmpty;
import static com.mineraltree.utils.Ensure.verifyNotNull;

import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.ExceptionHandler;
import akka.http.javadsl.server.Route;
import com.mineraltree.api.marshal.Marshal;
import com.mineraltree.api.rest.ApiRouter;
import com.mineraltree.currency.api.CurrencyProcessor;

public class CurrencyApiRouter implements ApiRouter {

  private final CurrencyProcessor currency;

  /** The router which handles currency requests */
  private CurrencyApiRouter(Builder builder) {
    this.currency = builder.currency;
    verifyNotEmpty(currency, "CurrencyProcessor");
  }

  @Override
  public Route getRouter() {
    return handleExceptions(
        getExceptionHandler(),
        () -> get(() -> logRequest("Currency Api Marker", DebugLevel(), this::handleConverter)));
  }

  private ExceptionHandler getExceptionHandler() {
    return ExceptionHandler.newBuilder()
        .match(
            IllegalArgumentException.class, e -> complete(StatusCodes.BAD_REQUEST, e.getMessage()))
        .matchAny(e -> complete(StatusCodes.INTERNAL_SERVER_ERROR, "Server Error"))
        .build();
  }

  private Route handleConverter() {
    return logRequest(
        "currencyConverter",
        DebugLevel(),
        () -> pathPrefix("currencyConverter", this::handleConversionRates));
  }

  private Route handleConversionRates() {
    return logRequest(
        "allConversionRates",
        DebugLevel(),
        () -> pathPrefix("allConversionRates", this::handleBase));
  }

  private Route handleBase() {
    return parameter(
        "base",
        base ->
            (onSuccess(
                currency.getCurrencyRates(base),
                currencyObj -> complete(StatusCodes.OK, currencyObj, Marshal.marshaller()))));
  }

  /**
   * Builder class for constructing the router. This ensures that all required arguments to the
   * router are provided during construction.
   */
  public static class Builder {
    private CurrencyProcessor currency = null;

    public Builder setCurrencyProcessor(CurrencyProcessor newCurrency) {
      this.currency = verifyNotNull(newCurrency, "Currency Processor");
      return this;
    }

    /**
     * Creates the API router using the input provided by the various {@code set...} methods.
     *
     * @return CurrencyApiRouter
     */
    public CurrencyApiRouter build() {
      return new CurrencyApiRouter(this);
    }
  }
}
