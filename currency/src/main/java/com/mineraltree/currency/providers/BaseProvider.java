package com.mineraltree.currency.providers;

import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.Query;
import akka.http.javadsl.model.Uri;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mineraltree.currency.api.CurrencyProcessor;
import com.mineraltree.currency.dto.CurrencyRates;
import com.mineraltree.currency.dto.CurrencyRates.Builder;
import com.mineraltree.http.RequestUtils;
import com.mineraltree.http.WrappedResponse;
import com.typesafe.config.Config;
import java.util.concurrent.CompletionStage;

public abstract class BaseProvider implements CurrencyProcessor {
  private RequestUtils requestUtils;

  private final Uri baseUri;
  private final String providerName;

  public BaseProvider(Config config, RequestUtils requestUtils) {
    this.requestUtils = requestUtils;
    this.baseUri = Uri.create(config.getString("endpoint"));
    this.providerName = config.getString("type");
  }

  @Override
  public CompletionStage<CurrencyRates> getCurrencyRates(String base) {
    HttpRequest request = createRequest(base);
    return requestUtils
        .singleRequest(request)
        .thenApply(WrappedResponse::assertStatusSuccess)
        .thenCompose(r -> r.extractPayload(ObjectNode.class))
        .thenApply(r -> readResponseJson(base, r));
  }

  public String getProviderName() {
    return providerName;
  }

  private CurrencyRates readResponseJson(String baseCurrency, ObjectNode responseTree) {
    try {
      CurrencyRates.Builder rateBuilder = new Builder();
      rateBuilder.setBaseCurrency(baseCurrency);
      rateBuilder.setProvider(providerName);
      extractRatesFromResponse(rateBuilder, responseTree);
      return rateBuilder.build();
    } catch (RuntimeException ex) {
      throw new RuntimeException(
          "Failed to read JSON response from " + baseUri + " with base currency " + baseCurrency,
          ex);
    }
  }

  protected abstract void extractRatesFromResponse(
      CurrencyRates.Builder builder, ObjectNode responseTree);

  protected <N extends JsonNode> N getRequiredAttribute(
      ObjectNode root, String keyName, Class<N> clazz) {
    if (!root.has(keyName)) {
      throw new RuntimeException("Response missing expected attribute '" + keyName + "'");
    }
    JsonNode value = root.get(keyName);
    if (!clazz.isAssignableFrom(value.getClass())) {
      throw new RuntimeException(
          "Expected '"
              + keyName
              + "' field to be of type "
              + clazz.getSimpleName()
              + " but value contained node of type "
              + root.getNodeType());
    }
    return clazz.cast(value);
  }

  protected abstract Query getRequestParameters();

  private HttpRequest createRequest(String base) {
    Query params = getRequestParameters();
    Uri requestUri = baseUri.query(params.withParam("base", base));
    return HttpRequest.GET(requestUri.toString())
        .addHeader(HttpHeader.parse("Accept", ContentTypes.APPLICATION_JSON.toString()));
  }
}
