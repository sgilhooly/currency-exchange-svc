package com.mineraltree.currency.dto;

import static com.mineraltree.utils.Ensure.verifyNotEmpty;

import com.google.common.collect.ImmutableSortedMap;
import com.mineraltree.api.dto.ApiDto;
import java.util.Map;
import java.util.TreeMap;

/** Represents the currency rates against a common (base) value. */
public class CurrencyRates implements ApiDto {

  private final String baseCurrency;
  private final String provider;
  private final Map<String, Double> rates;

  public CurrencyRates(Builder build) {
    this.baseCurrency = verifyNotEmpty(build.baseCurrency, "baseCurrency");
    this.provider = verifyNotEmpty(build.provider, "provider");
    this.rates = ImmutableSortedMap.copyOf(verifyNotEmpty(build.rates, "rates"));
  }

  public String getBaseCurrency() {
    return baseCurrency;
  }

  public String getProvider() {
    return provider;
  }

  @Override
  public void validate() {
    verifyNotEmpty(baseCurrency, "baseCurrency");
    verifyNotEmpty(provider, "provider");
    verifyNotEmpty(rates, "rates");
  }

  public static class Builder {

    private String baseCurrency;
    private String provider;
    private Map<String, Double> rates = new TreeMap<>();

    public void setBaseCurrency(String baseCurrency) {
      this.baseCurrency = verifyNotEmpty(baseCurrency, "baseCurrency");
    }

    public void setProvider(String provider) {
      this.provider = verifyNotEmpty(provider, "provider");
    }

    public void addRate(String key, Double value) {
      rates.put(key, value);
    }

    public void addAllRates(Map<String, Double> values) {
      rates.putAll(values);
    }

    public CurrencyRates build() {
      return new CurrencyRates(this);
    }
  }
}
