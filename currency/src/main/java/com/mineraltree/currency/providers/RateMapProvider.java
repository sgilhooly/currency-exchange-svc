package com.mineraltree.currency.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mineraltree.currency.dto.CurrencyRates.Builder;
import com.mineraltree.currency.providers.BaseProvider;
import com.mineraltree.http.RequestUtils;
import com.typesafe.config.Config;
import java.util.Iterator;
import java.util.Map.Entry;

public abstract class RateMapProvider extends BaseProvider {

  public RateMapProvider(Config config, RequestUtils requestUtils) {
    super(config, requestUtils);
  }

  /**
   * Given a Json ObjectNode containing all fields which map to double values, reads in the map
   * entries as the currency rates.
   */
  protected void extractRateMap(Builder builder, ObjectNode rateNode) {
    Iterator<Entry<String, JsonNode>> it = rateNode.fields();
    while (it.hasNext()) {
      Entry<String, JsonNode> rate = it.next();
      builder.addRate(rate.getKey(), rate.getValue().doubleValue());
    }
  }
}
