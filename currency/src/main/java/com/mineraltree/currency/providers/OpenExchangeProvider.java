package com.mineraltree.currency.providers;

import akka.http.javadsl.model.Query;
import akka.japi.Pair;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mineraltree.currency.dto.CurrencyRates.Builder;
import com.mineraltree.http.RequestUtils;
import com.typesafe.config.Config;

public class OpenExchangeProvider extends RateMapProvider {

  private final String openExchangeAppId;

  public OpenExchangeProvider(Config config, RequestUtils requestUtils, Config serviceConfig) {
    super(config, requestUtils);
    openExchangeAppId = serviceConfig.getString("openexchange.app-id");
  }

  @Override
  protected void extractRatesFromResponse(Builder builder, ObjectNode responseTree) {
    ObjectNode rateMap = getRequiredAttribute(responseTree, "rates", ObjectNode.class);
    extractRateMap(builder, rateMap);
  }

  @Override
  protected Query getRequestParameters() {
    return Query.create(Pair.create("app_id", openExchangeAppId));
  }
}
