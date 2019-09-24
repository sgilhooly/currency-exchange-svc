package com.mineraltree.currency.providers;

import akka.http.javadsl.model.Query;
import akka.japi.Pair;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mineraltree.currency.dto.CurrencyRates.Builder;
import com.mineraltree.http.RequestUtils;
import com.typesafe.config.Config;

public class FixerProvider extends RateMapProvider {

  private final String accessKey;

  public FixerProvider(Config config, RequestUtils requestUtils, Config serviceConfig) {
    super(config, requestUtils);
    accessKey = serviceConfig.getString("fixer.access-key");
  }

  /**
   * Inspects and extracts the results of the returned currency rate information. The response has
   * the form:
   *
   * <pre>
   *   {
   *     "success": true,
   *     "timestamp": 1564111566,
   *     "base": "USD",
   *     "date": "2019-07-26",
   *     "rates": {
   *         "AED": 3.673201,
   *         "AFN": 80.765498,
   *         "ALL": 109.205002,
   *         "AMD": 475.510209,
   *         ...etc...
   *     }
   *   }
   * </pre>
   */
  @Override
  protected void extractRatesFromResponse(Builder builder, ObjectNode responseTree) {
    BooleanNode successNode = getRequiredAttribute(responseTree, "success", BooleanNode.class);
    if (!successNode.booleanValue()) {
      throw new RuntimeException(
          "Response from Fixer returned unsuccessful status: " + responseTree);
    }
    ObjectNode allRates = getRequiredAttribute(responseTree, "rates", ObjectNode.class);
    extractRateMap(builder, allRates);
  }

  @Override
  protected Query getRequestParameters() {
    return Query.create(Pair.create("access_key", accessKey));
  }
}
